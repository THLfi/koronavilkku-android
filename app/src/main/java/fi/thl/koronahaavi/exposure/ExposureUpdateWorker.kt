package fi.thl.koronahaavi.exposure

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
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
@HiltWorker
class ExposureUpdateWorker @AssistedInject constructor(
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

        if (appStateRepository.lockedAfterDiagnosis().value ||
            appStateRepository.appShutdown().value) {
            Timber.i("App not active, ignoring exposure update")
            return Result.success()
        }

        // configuration is loaded during diagnosis key file download, which must happen
        // before this worker can be started, so we can rely on config being available
        val config = settingsRepository.requireExposureConfiguration()

        // due to legacy implementation, current exposures can contain each individual exposure, or new daily exposure
        val latestExposureDate = exposureRepository.getAllExposures().maxOfOrNull {
            it.detectedDate.toLocalDate()
        }

        // compare to existing and find new days with exposure
        val newExposures = exposureNotificationService.getDailyExposures(config)
            .filter { it.score >= config.minimumDailyScore }
            .filter { latestExposureDate?.isBefore(it.day) ?: true }
            .map { it.toExposure() }

        newExposures.forEach {
            Timber.d("New daily exposure ${it.detectedDate.toLocalDate()}")
            exposureRepository.saveExposure(it)
        }

        if (newExposures.isNotEmpty()) {
            exposureRepository.saveKeyGroupToken(
                createKeyGroupToken(newExposures)
            )
            notificationService.notifyExposure()
        }

        return Result.success()
    }

    // UTC timezone is used to match EN api v1 exposure detail, and also Converters.longToZonedDateTime
    private fun DailyExposure.toExposure() =
        Exposure(
            createdDate = ZonedDateTime.now(),
            detectedDate = day.atStartOfDay(ZoneId.of("Z")),
            totalRiskScore = score
        )

    private fun createKeyGroupToken(exposures: List<Exposure>?) =
            KeyGroupToken(
                token = UUID.randomUUID().toString(), // legacy db schema support
                dayCount = exposures?.size,
                latestExposureDate = exposures?.map { it.detectedDate }?.maxOrNull()
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