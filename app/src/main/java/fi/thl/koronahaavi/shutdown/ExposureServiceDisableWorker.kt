package fi.thl.koronahaavi.shutdown

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.service.ExposureNotificationService
import fi.thl.koronahaavi.service.NotificationService

@HiltWorker
class ExposureServiceDisableWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val exposureNotificationService: ExposureNotificationService,
    private val notificationService: NotificationService,
    private val appStateRepository: AppStateRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (appStateRepository.appShutdown().value) {
            notificationService.notifyShutdown()
            exposureNotificationService.disable()
        }
        return Result.success()
    }

    companion object {
        fun start(context: Context) {
            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequest.Builder(ExposureServiceDisableWorker::class.java)
                    .build()
            )
        }
    }
}
