@file:Suppress("DEPRECATION")

package fi.thl.koronahaavi.service

import android.app.Activity
import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import com.google.android.gms.nearby.exposurenotification.ExposureSummary.ExposureSummaryBuilder
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.huawei.hms.api.HuaweiApiAvailability
import com.huawei.hms.common.ApiException
import com.huawei.hms.contactshield.*
import com.huawei.hms.support.api.entity.core.CommonCode
import fi.thl.koronahaavi.service.ExposureNotificationService.ResolvableResult
import fi.thl.koronahaavi.BuildConfig
import fi.thl.koronahaavi.data.Exposure
import fi.thl.koronahaavi.exposure.ExposureUpdateWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HuaweiContactShieldService(
    private val context: Context
) : ExposureNotificationService {

    private val engine by lazy {
        ContactShield.getContactShieldEngine(context)
    }

    private val isEnabledFlow = MutableStateFlow<Boolean?>(null)
    override fun isEnabledFlow(): StateFlow<Boolean?> = isEnabledFlow

    override suspend fun refreshEnabledFlow() {
        isEnabledFlow.value = isEnabled()
    }

    override suspend fun enable() = resultFromRunning {
        engine.startContactShield(ContactShieldSetting.DEFAULT).await()
        isEnabledFlow.value = true
    }

    override suspend fun disable() = resultFromRunning {
        engine.stopContactShield().await()
        isEnabledFlow.value = false
    }

    override suspend fun isEnabled(): Boolean =
        engine.isContactShieldRunning.await()

    override suspend fun getExposureSummary(token: String): ExposureSummary =
        engine.getContactSketch(token).await().let {
            ExposureSummaryBuilder()
                .setDaysSinceLastExposure(it.daysSinceLastHit)
                .setMatchedKeyCount(it.numberOfHits)
                .setMaximumRiskScore(it.maxRiskValue)
                .setSummationRiskScore(it.summationRiskValue)
                .setAttenuationDurations(it.attenuationDurations)
                .build()
        }

    override suspend fun getExposureDetails(token: String) =
        engine.getContactDetail(token).await()
            .map {
                Exposure(
                    detectedDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(it.dayNumber), ZoneOffset.UTC),
                    totalRiskScore = it.totalRiskValue,
                    createdDate = ZonedDateTime.now()
                )
            }

    override suspend fun provideDiagnosisKeyFiles(
        token: String,
        files: List<File>,
        config: ExposureConfigurationData
    ): ResolvableResult<Unit> {

        val intent = PendingIntent.getService(
            context,
            0,
            Intent(context, ContactShieldIntentService::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        return resultFromRunning<Unit> {
            engine.putSharedKeyFiles(intent, files, config.toDiagnosisConfiguration(), token).await()
        }
    }

    override suspend fun getTemporaryExposureKeys() = resultFromRunning {
        engine.periodicKey.await().map {
            TemporaryExposureKey.TemporaryExposureKeyBuilder()
                .setKeyData(it.content)
                .setRollingStartIntervalNumber(it.periodicKeyValidTime.toInt())
                .setRollingPeriod(it.periodicKeyLifeTime.toInt())
                .setTransmissionRiskLevel(it.initialRiskLevel)
                .build()
        }
    }

    override fun deviceSupportsLocationlessScanning() = false

    override fun getAvailabilityResolver() = HuaweiAvailabilityResolver()

    private suspend fun <T> resultFromRunning(block: suspend () -> T): ResolvableResult<T> {
        return try {
            ResolvableResult.Success(block())
        }
        catch (exception: ApiException) {
            Timber.e(exception, "Exposure notification API call failed")
            if (exception.statusCode == ExposureNotificationStatusCodes.FAILED_NOT_SUPPORTED) {
                ResolvableResult.MissingCapability(
                    exception.localizedMessage
                )
            }
            else {
                ResolvableResult.Failed(
                    apiErrorCode = exception.statusCode,
                    connectionErrorCode = 0,
                    error = exception.localizedMessage
                )
            }
        }
        catch (exception: Exception) {
            Timber.e(exception, "Contact shield API call failed")
            ResolvableResult.Failed(error = exception.localizedMessage)
        }
    }

    // this maps backend json object to google EN api configuration
    private fun ExposureConfigurationData.toDiagnosisConfiguration(): DiagnosisConfiguration {

        // when test UI is enabled, override minimum risk score so that we get the calculated risk score
        // from EN api, otherwise it is zeroed out and test UI is not useful
        val minRiskScore = if (BuildConfig.ENABLE_TEST_UI) 1 else minimumRiskScore

        return DiagnosisConfiguration.Builder()
            .setMinimumRiskValueThreshold(minRiskScore)
            .setAttenuationRiskValues(*attenuationScores.toIntArray())
            .setDaysAfterContactedRiskValues(*daysSinceLastExposureScores.toIntArray())
            .setDurationRiskValues(*durationScores.toIntArray())
            .setInitialRiskLevelRiskValues(*transmissionRiskScoresAndroid.toIntArray())
            .setAttenuationDurationThresholds(*durationAtAttenuationThresholds.toIntArray())
            .build()
    }
}



/**
 * Awaits for completion of the task without blocking a thread.
 *
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * stops waiting for the completion stage and immediately resumes with [CancellationException].
 */
suspend fun <T> com.huawei.hmf.tasks.Task<T>.await(): T {
    // fast path
    if (isComplete) {
        val e = exception
        return if (e == null) {
            if (isCanceled) {
                throw CancellationException("Task $this was cancelled normally.")
            } else {
                @Suppress("UNCHECKED_CAST")
                result as T
            }
        } else {
            throw e
        }
    }

    return suspendCancellableCoroutine { cont ->
        addOnCompleteListener {
            val e = exception
            if (e == null) {
                @Suppress("UNCHECKED_CAST")
                if (isCanceled) cont.cancel() else cont.resume(result as T)
            } else {
                cont.resumeWithException(e)
            }
        }
    }
}

class HuaweiAvailabilityResolver : ExposureNotificationService.AvailabilityResolver {
    private val apiAvailability = HuaweiApiAvailability.getInstance()

    override fun isSystemAvailable(context: Context): Int =
        apiAvailability.isHuaweiMobileServicesAvailable(context)

    override fun isUserResolvableError(errorCode: Int) =
        apiAvailability.isUserResolvableError(errorCode)

    override fun showErrorDialogFragment(activity: Activity, errorCode: Int, requestCode: Int,
                                         cancelListener: (dialog: DialogInterface) -> Unit) =
        apiAvailability.showErrorDialogFragment(activity, errorCode, requestCode, cancelListener)

}