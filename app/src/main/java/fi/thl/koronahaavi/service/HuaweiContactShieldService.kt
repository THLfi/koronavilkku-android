@file:Suppress("DEPRECATION")

package fi.thl.koronahaavi.service

import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import com.google.android.gms.nearby.exposurenotification.ExposureSummary.ExposureSummaryBuilder
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import fi.thl.koronahaavi.service.ExposureNotificationService.ResolvableResult
import com.huawei.hms.contactshield.ContactShield
import com.huawei.hms.contactshield.ContactShieldCallback
import com.huawei.hms.contactshield.ContactShieldSetting
import com.huawei.hms.contactshield.DiagnosisConfiguration
import fi.thl.koronahaavi.BuildConfig
import fi.thl.koronahaavi.data.Exposure
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
        // todo how is user consent obtained? is there an equivalent to google ApiException

        val intent = PendingIntent.getService(
            context,
            0,
            Intent(context, BackgroundContactCheckingIntentService::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        Timber.d("startContactShield")
        engine.startContactShield(intent, ContactShieldSetting.DEFAULT).await()

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

        return resultFromRunning<Unit> {
            engine.putSharedKeyFiles(files, config.toDiagnosisConfiguration(), token).await()
        }
    }

    override suspend fun getTemporaryExposureKeys(): ResolvableResult<List<TemporaryExposureKey>> {
        TODO("Not yet implemented")
    }

    // todo is location required?
    override fun deviceSupportsLocationlessScanning() = true

    private suspend fun <T> resultFromRunning(block: suspend () -> T): ResolvableResult<T> {
        return try {
            ResolvableResult.Success(block())
        } catch (exception: Exception) {
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

class BackgroundContactCheckingIntentService : IntentService("ContactShield_BackgroundContactCheckingIntentService") {
    private val engine by lazy {
        ContactShield.getContactShieldEngine(this)
    }

    override fun onHandleIntent(intent: Intent?) {
        Timber.d("onHandleIntent $intent")

        intent?.let {
            engine.handleIntent(it, object : ContactShieldCallback {
                override fun onHasContact(p0: String?) {
                    Timber.d("onHasContact")
                }

                override fun onNoContact(p0: String?) {
                    Timber.d("onNoContact")
                }
            })
        }
    }
}