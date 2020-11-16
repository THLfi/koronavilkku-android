@file:Suppress("DEPRECATION")

package fi.thl.koronahaavi.service

import android.app.Activity
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
import fi.thl.koronahaavi.service.ExposureNotificationService.ResolvableResult
import fi.thl.koronahaavi.BuildConfig
import fi.thl.koronahaavi.data.Exposure
import fi.thl.koronahaavi.service.ExposureNotificationService.EnableStep
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.io.File
import java.time.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.min

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

    override suspend fun getExposureDetails(token: String): List<Exposure> {
        val createdDate = ZonedDateTime.now()

        return engine.getContactDetail(token).await()
                .map { info ->
                    Timber.d(info.toString())
                    info.toExposure(createdDate)
                }
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
                .setRollingStartIntervalNumber(
                    convertToDayFirstInterval(it.periodicKeyValidTime.toInt())
                )
                .setRollingPeriod(
                    convertToRollingPeriod(it.periodicKeyValidTime.toInt(), it.periodicKeyLifeTime.toInt())
                )
                .setTransmissionRiskLevel(it.initialRiskLevel)
                .build()
        }
    }

    // convert to beginning of day in UTC, to match Google implementation
    private fun convertToDayFirstInterval(periodicKeyValidTime: Int) =
        periodicKeyValidTime / ROLLING_INTERVALS_IN_DAY * ROLLING_INTERVALS_IN_DAY

    // convert to match Google implementation
    private fun convertToRollingPeriod(periodicKeyValidTime: Int, periodicKeyLifeTime: Int): Int {
        val dayFirstInterval = convertToDayFirstInterval(periodicKeyValidTime)

        return min(periodicKeyValidTime + periodicKeyLifeTime - dayFirstInterval, ROLLING_INTERVALS_IN_DAY)
    }

    override fun deviceSupportsLocationlessScanning() = false

    override fun getAvailabilityResolver() = HuaweiAvailabilityResolver()

    private suspend fun <T> resultFromRunning(block: suspend () -> T): ResolvableResult<T> {
        return try {
            ResolvableResult.Success(block())
        }
        catch (exception: ApiException) {
            Timber.e(exception, "HMS API call failed")
            when (exception.statusCode) {
                ExposureNotificationStatusCodes.FAILED_NOT_SUPPORTED -> {
                    ResolvableResult.MissingCapability(
                        exception.localizedMessage
                    )
                }
                StatusCode.STATUS_INTERNAL_ERROR -> ResolvableResult.HmsCanceled(EnableStep.LocationPermission)
                StatusCode.STATUS_FAILURE -> ResolvableResult.HmsCanceled(EnableStep.UserConsent)
                else -> {
                    ResolvableResult.Failed(
                        apiErrorCode = exception.statusCode,
                        connectionErrorCode = 0,
                        error = exception.localizedMessage
                    )
                }
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

    companion object {
        const val ROLLING_INTERVALS_IN_DAY = 144
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

private fun ContactDetail.toExposure(
    createdDate: ZonedDateTime = ZonedDateTime.now()
) = Exposure(
    detectedDate = LocalDate.ofEpochDay(dayNumber).atTime(0, 0).atZone(ZoneOffset.UTC),
    totalRiskScore = totalRiskValue,
    createdDate = createdDate
)

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