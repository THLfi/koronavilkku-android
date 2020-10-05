@file:Suppress("DEPRECATION")

package fi.thl.koronahaavi.exposure

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.*
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.ExposureRepository
import fi.thl.koronahaavi.data.KeyGroupToken
import fi.thl.koronahaavi.data.SettingsRepository
import fi.thl.koronahaavi.service.ExposureNotificationService
import timber.log.Timber

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
        Timber.d(summary.toString())

        if (summary.matchedKeyCount > 0) {
            Timber.i("Exposure matches found: ${summary.matchedKeyCount}")
            val config = settingsRepository.requireExposureConfiguration()

            if (summary.maximumRiskScore >= config.minimumRiskScore) {
                Timber.i("Maximum risk score ${summary.maximumRiskScore} over minimum ${config.minimumRiskScore}, saving detailed exposure data")

                // this call will trigger EN system notifications (only on real use)
                exposureNotificationService.getExposureDetails(token)
                    .forEach { exposureRepository.saveExposure(it) }
            }
            else {
                Timber.d("Maximum risk score ${summary.maximumRiskScore} less than minimum ${config.minimumRiskScore}, ignoring")
            }
        }
        else {
            Timber.i("No exposure matches found")
        }

        exposureRepository.saveKeyGroupToken(KeyGroupToken(
            token = token,
            matchedKeyCount = summary.matchedKeyCount,
            maximumRiskScore = summary.maximumRiskScore
        ))

        return Result.success()
    }

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