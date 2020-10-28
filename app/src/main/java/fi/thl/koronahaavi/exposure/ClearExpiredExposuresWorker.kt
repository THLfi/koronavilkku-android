package fi.thl.koronahaavi.exposure

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.*
import fi.thl.koronahaavi.data.ExposureRepository
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Removes expired exposures from local database
 * This worker is not canceled when app is locked after sending diagnosis, because
 * this does not send any network communication and also possible exposures should be
 * cleaned up even after the app is locked
 */
class ClearExpiredExposuresWorker @WorkerInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val exposureRepository: ExposureRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("Starting")
        exposureRepository.deleteExpiredExposuresAndTokens()
        return Result.success()
    }

    companion object {
        // 11 day ttl gives the user 24hrs to access the app in case notification is
        // received for a 10 day old exposure
        const val EXPOSURE_TTL_SINCE_DETECTION_DAYS = 11
        const val CLEAR_EXPIRED_WORKER_NAME = "ClearExpiredExposuresWorker"

        fun schedule(context: Context, reconfigure: Boolean = false) {
            val request = PeriodicWorkRequestBuilder<ClearExpiredExposuresWorker>(12, TimeUnit.HOURS)
                .build()

            val policy = if (reconfigure) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                CLEAR_EXPIRED_WORKER_NAME, policy, request
            )
        }
    }
}