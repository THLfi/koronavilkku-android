package fi.thl.koronahaavi.service

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.*
import fi.thl.koronahaavi.data.AppStateRepository
import timber.log.Timber
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

class DiagnosisKeySendTrafficCoverWorker @WorkerInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val exposureNotificationService: ExposureNotificationService,
    private val appStateRepository: AppStateRepository,
    private val diagnosisKeyService: DiagnosisKeyService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("Starting")

        if (shouldSend()) {
            Timber.d("Sending fake keys")
            diagnosisKeyService.sendExposureKeys(
                authCode = DiagnosisKeyService.FAKE_TOKEN,
                keyHistory = listOf(),
                visitedCountryCodes = listOf(),
                consentToShare = true,
                isFake = true
            )
        }

        schedule(context, reconfigure = true)
        return Result.success()
    }

    private suspend fun shouldSend(): Boolean {
        if (!exposureNotificationService.isEnabled()) {
            Timber.d("Exposure notifications disabled, not sending")
            return false
        }

        if (appStateRepository.lockedAfterDiagnosis().value) {
            Timber.d("App locked, not sending")
            return false
        }

        return true
    }

    companion object {
        const val SEND_TRAFFIC_COVER_WORKER_NAME = "DiagnosisKeySendTrafficCoverWorker"
        private val SECURE_RANDOM = SecureRandom()

        fun schedule(context: Context, reconfigure: Boolean = false) {

            // Start with random initial delay (0-24hrs) to produce traffic without any recognizable
            // patterns. Worker reschedules itself on every execution so actually this is
            // in effect a one time worker, but periodic worker is used to guarantee that the worker
            // repeats even if it is aborted before it can reschedule itself

            val delaySeconds = SECURE_RANDOM.nextInt(TimeUnit.HOURS.toSeconds(24).toInt())
            val policy = if (reconfigure) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP

            Timber.d("Scheduling, policy $policy, delay ${TimeUnit.SECONDS.toMinutes(delaySeconds.toLong())} minutes")

            val request = PeriodicWorkRequestBuilder<DiagnosisKeySendTrafficCoverWorker>(
                repeatInterval = 12,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            ).setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            ).setInitialDelay(delaySeconds.toLong(), TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SEND_TRAFFIC_COVER_WORKER_NAME, policy, request
            )
        }
    }
}