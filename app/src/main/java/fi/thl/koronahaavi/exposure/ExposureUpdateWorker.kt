package fi.thl.koronahaavi.exposure

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.*
import com.google.android.gms.nearby.exposurenotification.DailySummariesConfig
import fi.thl.koronahaavi.data.*
import fi.thl.koronahaavi.service.ExposureNotificationService
import timber.log.Timber

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
        val dailySummaries = exposureNotificationService.getDailySummaries(config)
                .also { Timber.d("Daily summaries $it") }


        //val windows = exposureNotificationService.getExposureWindows()
        //        .also { it.forEach { w -> Timber.d("$w") }  }

        /*
        var exposures: List<Exposure>? = null
        if (summary.matchedKeyCount > 0) {
            val config = settingsRepository.requireExposureConfiguration()
            val checker = ExposureSummaryChecker(summary, config)

            Timber.d("Configuration minimum risk score ${config.minimumRiskScore}")

            if (checker.hasHighRisk()) {
                Timber.d("High risk detected")

                // this call will trigger EN system notifications
                exposures = exposureNotificationService.getExposureDetails(token).let {
                    checker.filterExposures(it)
                }

                exposures.forEach { exposureRepository.saveExposure(it) }
            }
        }
        else {
            Timber.i("No exposure matches found")
        }

        exposureRepository.saveKeyGroupToken(createKeyGroupToken(token, summary, exposures))
         */

        return Result.success()
    }

    /*
    private fun createKeyGroupToken(token: String, summary: ExposureSummary, exposures: List<Exposure>?) =
            KeyGroupToken(
                token = token,
                matchedKeyCount = summary.matchedKeyCount,
                maximumRiskScore = summary.maximumRiskScore,
                exposureCount = exposures?.size,
                latestExposureDate = exposures?.map { it.detectedDate }?.max()
            )

     */

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