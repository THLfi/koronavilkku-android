package fi.thl.koronahaavi.exposure

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.*
import fi.thl.koronahaavi.data.*
import fi.thl.koronahaavi.service.ExposureNotificationService
import fi.thl.koronahaavi.service.NotificationService
import timber.log.Timber
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

/*
Synchronizes exposure state from ENS daily summaries into local database
Database entities for exposure and key group token are maintained to keep UX
the same as with original exposure summary and details API
*/
class ExposureUpdateWorker @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val exposureNotificationService: ExposureNotificationService,
    private val exposureRepository: ExposureRepository,
    private val appStateRepository: AppStateRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationService: NotificationService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.i("Starting")

        if (appStateRepository.lockedAfterDiagnosis().value) {
            Timber.i("App locked, ignoring exposure update")
            return Result.success()
        }

        // todo get config from backend
        val config = settingsRepository.requireExposureConfiguration()

        // todo wip code to experiment with v2 api

        // due to legacy implementation, current exposures can contain each individual exposure, or new daily exposure
        val currentExposures = exposureRepository.getAllExposures()

        // compare to existing and find new days with exposure
        // todo need to detect increase to existing days?
        val newExposures = exposureNotificationService.getDailyExposures(config)
            .filter { it.score > 900 }
            .filter { currentExposures.none { c -> c.detectedDate.toLocalDate() == it.day } } // nothing for this day
            .map { it.toExposure() }

        newExposures.forEach {
            Timber.d("New daily exposure ${it.detectedDate.toLocalDate()}")
            exposureRepository.saveExposure(it)
        }

        if (newExposures.isNotEmpty()) {
            exposureRepository.saveKeyGroupToken(
                createKeyGroupToken(newExposures.size)
            )
            notificationService.notifyExposure()
        }

        return Result.success()
    }

    private fun DailyExposure.toExposure() =
        Exposure(
            createdDate = ZonedDateTime.now(),
            detectedDate = day.atStartOfDay(ZoneId.of("Z")),
            totalRiskScore = score
        )

    private fun createKeyGroupToken(exposureCount: Int) =
            KeyGroupToken(
                token = UUID.randomUUID().toString(), // legacy db schema support
                matchedKeyCount = exposureCount,
                exposureCount = exposureCount
            )

    companion object {
        const val TAG = "fi.thl.koronahaavi.exposure.ExposureUpdateWorker"

        fun start(context: Context) {
            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequest.Builder(ExposureUpdateWorker::class.java)
                    .addTag(TAG)
                    .build()
            )
        }
    }
}