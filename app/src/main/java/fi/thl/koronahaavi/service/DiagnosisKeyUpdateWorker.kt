package fi.thl.koronahaavi.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.lifecycle.LiveData
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.ExposureRepository
import fi.thl.koronahaavi.data.KeyGroupToken
import fi.thl.koronahaavi.service.ExposureNotificationService.ResolvableResult
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.net.HttpURLConnection
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit

@HiltWorker
class DiagnosisKeyUpdateWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val exposureNotificationService: ExposureNotificationService,
    private val exposureRepository: ExposureRepository,
    private val appStateRepository: AppStateRepository,
    private val diagnosisKeyService: DiagnosisKeyService

) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.v("begin, attempt=$runAttemptCount")

        if (isDisabled()) {
            return Result.success()
        }

        // download diagnosis key files from backend and save to local files
        val downloadResult = try {
            diagnosisKeyService.downloadDiagnosisKeyFiles()
        } catch (e: Exception) {
            Timber.e(e, "Failed to download diagnosis keys")
            return resultForError(e)
        }

        appStateRepository.setLastExposureCheckTime(ZonedDateTime.now())

        if (downloadResult.files.isEmpty()) {
            Timber.i("Key files up to date, nothing downloaded")
            return Result.success()
        }

        return if (processFiles(downloadResult)) {
            appStateRepository.setDiagnosisKeyBatchId(downloadResult.lastSuccessfulBatchId)
            Result.success()
        }
        else {
            Result.failure()
        }
    }

    private suspend fun isDisabled(): Boolean {
        if (!exposureNotificationService.isEnabled()) {
            Timber.i("Exposure notifications disabled, skipping download")
            return true
        }

        if (appStateRepository.lockedAfterDiagnosis().value) {
            // this should not happen, but check just in case work was not successfully canceled
            // when submitting diagnosis... this is also checked in main activity oncreate where worker
            // is verified to be canceled
            Timber.i("App locked, skipping download")
            return true
        }

        return false
    }

    /**
     * Provides key files to EN api and deletes the files after done
     * Returns true when successful
     */
    private suspend fun processFiles(downloadResult: DownloadResult): Boolean {
        Timber.i("Processing ${downloadResult.files.size} key files")
        // give files to exposure system to process, and save token so it can be retrieved
        // when processing is done and exposure receiver is called
        val token = UUID.randomUUID().toString()
        exposureRepository.saveKeyGroupToken(KeyGroupToken((token)))

        val processResult = when (
            val result = exposureNotificationService.provideDiagnosisKeyFiles(
                token,
                downloadResult.files,
                downloadResult.exposureConfig
            )) {

            is ResolvableResult.Success -> {
                true
            }
            is ResolvableResult.Failed -> {
                Timber.e("provideDiagnosisKeys call failed, error: %s", result.error)
                false
            }
            is ResolvableResult.MissingCapability -> {
                Timber.e("provideDiagnosisKeys call failed with missing capability, error: %s", result.error)
                false
            }
            is ResolvableResult.ApiNotSupported -> {
                Timber.e("provideDiagnosisKeys call failed with api not supported")
                false
            }
            is ResolvableResult.ResolutionRequired -> {
                // this should not occur from provideDiagnosisKeys API call but handling it anyway
                Timber.e("provideDiagnosisKeys call failed requiring resolution")
                false
            }
            is ResolvableResult.HmsCanceled -> false
        }

        // remove temp key files since EN api has read and processed them
        downloadResult.files.forEach { it.delete() }

        return processResult
    }

    private fun resultForError(e: Exception): Result {
        if (allowedToRetry()) {
            // OkHttp by default silently retries in case of certain errors (see OkHttpClient.Builder.retryOnConnectionFailure).
            if (e is IOException) {
                return Result.retry()

            } else if (e is HttpException && e.shouldRetry()) {
                return Result.retry()
            }
        }

        return Result.failure()
    }

    // manual one-time update disables retries
    private fun allowedToRetry(): Boolean =
        (runAttemptCount < (MAX_RETRIES - 1)) && !inputData.getBoolean(RETRY_DISABLED_KEY, false)

    private fun HttpException.shouldRetry(): Boolean {

        if (code() == HttpURLConnection.HTTP_UNAVAILABLE) {
            // Retry if the backoff policy (together with the current attempt number) defines a
            // a delay greater than or equal to the one defined by Retry-After header.
            // Only support header value that defines the number of seconds after which one can retry.
            // If Retry-After isn't defined or it defines an absolute time, then always retry.
            val retryAfterSeconds = response()?.headers()?.get("Retry-After")?.toIntOrNull()
            val backoffDelay = backoffDelayMillis(runAttemptCount) / 1000L

            if (retryAfterSeconds == null || backoffDelay >= retryAfterSeconds) {
                return true
            }
        }

        return false
    }

    /**
     * Returns the minimum backoff delay after the specified number of run attempts.
     */
    private fun backoffDelayMillis(runAttemptCount: Int): Long {
        return Math.scalb(WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS.toDouble(), runAttemptCount).toLong()
    }

    companion object {
        const val MAX_RETRIES = 3
        const val KEY_UPDATER_NAME = "DiagnosisKeyUpdate"
        const val RETRY_DISABLED_KEY = "retry_disabled"

        fun schedule(context: Context, cfg: AppConfiguration, reconfigure: Boolean = false) {
            // start polling for backend diagnosis keys
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // KEEP policy is used normally so that previously configured (and possibly running)
            // work is not canceled when app starts
            val policy = if (reconfigure) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP
            Timber.d("Enqueue download worker, reconfigure=$reconfigure")

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                KEY_UPDATER_NAME,
                policy,
                PeriodicWorkRequestBuilder<DiagnosisKeyUpdateWorker>(
                    cfg.pollingIntervalMinutes, TimeUnit.MINUTES)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS,
                        TimeUnit.MILLISECONDS)
                    .setConstraints(constraints)
                    .build()
            )
        }

        fun runOnce(context: Context): LiveData<WorkInfo> {
            // just run once, no constraints or retries
            val request = OneTimeWorkRequestBuilder<DiagnosisKeyUpdateWorker>()
                .setInputData(
                    Data.Builder().putBoolean(RETRY_DISABLED_KEY, true).build()
                )
                .build()

            with (WorkManager.getInstance(context)) {
                enqueue(request)
                return getWorkInfoByIdLiveData(request.id)
            }
        }
    }
}
