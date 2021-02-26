@file:Suppress("DEPRECATION")

package fi.thl.koronahaavi.exposure

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import fi.thl.koronahaavi.data.*
import fi.thl.koronahaavi.service.ExposureNotificationService
import timber.log.Timber

@HiltWorker
class ExposureUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val exposureNotificationService: ExposureNotificationService,
    private val exposureRepository: ExposureRepository,
    private val appStateRepository: AppStateRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.i("Starting")

        val token = inputData.getString(TOKEN_KEY)
        if (token == null) {
            Timber.e("No input token")
            return Result.failure()
        }

        if (appStateRepository.lockedAfterDiagnosis().value) {
            Timber.i("App locked, ignoring exposure update")
            return Result.success()
        }

        val summary = exposureNotificationService.getExposureSummary(token)
                .also { Timber.d(it.toString()) }
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
        return Result.success()
    }

    private fun createKeyGroupToken(token: String, summary: ExposureSummary, exposures: List<Exposure>?) =
            KeyGroupToken(
                token = token,
                matchedKeyCount = summary.matchedKeyCount,
                maximumRiskScore = summary.maximumRiskScore,
                exposureCount = exposures?.size,
                latestExposureDate = exposures?.map { it.detectedDate }?.max()
            )

    companion object {
        const val TOKEN_KEY = "token_key"
        const val TAG = "fi.thl.koronahaavi.exposure.ExposureUpdateWorker"

        fun start(context: Context, token: String) {
            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequest.Builder(ExposureUpdateWorker::class.java)
                    .setInputData(
                        Data.Builder()
                            .putString(TOKEN_KEY, token)
                            .build()
                    )
                    .addTag(TAG)
                    .build()
            )
        }
    }
}