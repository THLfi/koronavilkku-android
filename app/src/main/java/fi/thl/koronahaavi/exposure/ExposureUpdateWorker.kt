package fi.thl.koronahaavi.exposure

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.*
import fi.thl.koronahaavi.data.*
import fi.thl.koronahaavi.service.ExposureNotificationService
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
    private val settingsRepository: SettingsRepository
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
        val currentExposures = exposureNotificationService.getDailyExposures(config)
            .filter { it.score > 900 }
            .map { it.toExposure() }

        val previousExposures = exposureRepository.getAllExposures()
            .groupBy { it.detectedDate }

        var newExposureCount = 0
        currentExposures.forEach { e ->
            if (!previousExposures.containsKey(e.detectedDate)) {
                Timber.d("New daily exposure ${e.detectedDate.toLocalDate()}")
                exposureRepository.saveExposure(e)
                newExposureCount++
            }
            else {
                // todo old version total risk scores are lower than exposure window scores.. should we add a new field?
                val previousScore = previousExposures[e.detectedDate]?.sumBy { it.totalRiskScore } ?: 0
                if (e.totalRiskScore - previousScore > 900) {
                    Timber.d("Increased daily exposure ${e.detectedDate.toLocalDate()}, from $previousScore to ${e.totalRiskScore}")
                    // todo which exposure to update?
                    //exposureRepository.saveExposure(dailyExposure)
                    newExposureCount++
                }
            }
        }

        if (newExposureCount > 0) {
            exposureRepository.saveKeyGroupToken(
                createKeyGroupToken(newExposureCount)
            )

            // send notifications
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