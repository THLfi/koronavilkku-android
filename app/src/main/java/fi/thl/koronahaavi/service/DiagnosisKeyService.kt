package fi.thl.koronahaavi.service

import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.SettingsRepository
import fi.thl.koronahaavi.service.BackendService.NumericBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class DiagnosisKeyService @Inject constructor (
    private val backendService: BackendService,
    private val appStateRepository: AppStateRepository,
    private val settingsRepository: SettingsRepository,
    private val systemOperations: SystemOperations
) {
    suspend fun sendExposureKeys(authCode: String,
                                 keyHistory: List<TemporaryExposureKey>,
                                 visitedCountryCodes: List<String>,
                                 consentToShare: Boolean,
                                 isFake: Boolean = false
    ): SendKeysResult {
        return try {
            val numKeys = settingsRepository.appConfiguration.diagnosisKeysPerSubmit

            // Sort latest first, so that if API returns more keys than max count, then oldest keys are discarded
            val sortedKeys = keyHistory.sortedByDescending { it.rollingStartIntervalNumber }

            val payload = DiagnosisKeyList(
                keys = List(numKeys) { index ->
                    sortedKeys.getOrNull(index)?.toDiagnosisKey()
                        ?: createFakeKey(index) // padding
                },
                visitedCountries = getVisitedCountries(visitedCountryCodes),
                consentToShareWithEfgs = NumericBoolean.from(consentToShare)
            )

            withContext(Dispatchers.IO) {
                val isFakeRequest = if (isFake) NumericBoolean.TRUE else NumericBoolean.FALSE
                backendService.sendKeys(authCode, payload, isFakeRequest)
            }
            SendKeysResult.Success
        }
        catch (throwable: Throwable) {
            if (throwable is HttpException && AUTH_HTTP_ERRORS.contains(throwable.code())) {
                Timber.e(throwable, "got ${throwable.code()}")
                SendKeysResult.Unauthorized
            }
            else {
                Timber.e(throwable, "request failed")
                SendKeysResult.Failed
            }
        }
    }

    /**
     * Creates map of country code to 1 or 0, required for json serialization
     */
    private fun getVisitedCountries(selectedCodes: List<String>): Map<String, NumericBoolean> =
        settingsRepository.getExposureConfiguration()?.availableCountries?.associateWith {
            NumericBoolean.from(selectedCodes.contains(it))
        } ?: mapOf()

    private fun TemporaryExposureKey.toDiagnosisKey() =
        DiagnosisKey(
            keyData = systemOperations.encodeToBase64(keyData),
            transmissionRiskLevel = transmissionRiskLevel,
            rollingPeriod = rollingPeriod,
            rollingStartIntervalNumber = rollingStartIntervalNumber
        )

    private fun createFakeKey(index: Int) =
        DiagnosisKey(
            keyData = systemOperations.encodeToBase64(Random.nextBytes(16)),
            transmissionRiskLevel = index % 7,
            rollingPeriod = 144,
            rollingStartIntervalNumber = FAKE_INTERVAL_NUM
        )

    /**
     * Download key files using previously stored state, and latest instructions
     * from backend. Files are downloaded sequentially and if any download fails
     * the return value will reflect the last successful batch id.
     *
     * @throws Exception if fails to get initial batch id, configuration or list of batches
     */
    suspend fun downloadDiagnosisKeyFiles(): DownloadResult {
        var downloadedBatchId = ""
        val keyFiles = mutableListOf<File>()

        return withContext(Dispatchers.IO) {
            val lastBatchId = appStateRepository.getDiagnosisKeyBatchId()
            Timber.d("Checking for new key files, last batch id: %s", lastBatchId)

            val listBatchesResult = getDiagnosisKeyBatches(lastBatchId)
            Timber.d("List batches returned ${listBatchesResult.batches.size} batches to download")

            listBatchesResult.batches.forEach { batchId ->
                createTempLocalFile().let { file ->
                    file.outputStream().use { output ->
                        Timber.d("Downloading batch $batchId to file ${file.absolutePath}")
                        if (safeFileDownload(batchId, output)) {
                            keyFiles.add(file)
                            downloadedBatchId = batchId
                        }
                        else {
                            Timber.d("Download for $batchId failed, skipping rest")
                            return@forEach
                        }
                    }
                }
            }

            DownloadResult(
                lastSuccessfulBatchId = downloadedBatchId,
                files = keyFiles,
                exposureConfig = reloadExposureConfig()
            )
        }
    }

    private suspend fun getDiagnosisKeyBatches(lastBatchId: BatchId): DiagnosisKeyBatches {
        try {
            return backendService.listDiagnosisKeyBatches(lastBatchId)
        }
        catch (exception: HttpException) {
            // if stored batch id has been corrupted somehow, list endpoint returns 400, and we should try
            // to recover by resetting stored batch id
            if (exception.code() == HttpURLConnection.HTTP_BAD_REQUEST) {
                Timber.e(exception, "listDiagnosisKeyBatches returned 400, clearing batch id for next attempt")
                appStateRepository.resetDiagnosisKeyBatchId()
            }
            throw exception
        }
    }


    private suspend fun reloadExposureConfig(): ExposureConfigurationData {
        val config = backendService.getConfiguration()

        // Exclude invalid country codes as additional security measure
        val validatedCountries = config.availableCountries?.filter { it.isValidCountryCode() }

        val validatedConfig = config.copy(
            availableCountries = validatedCountries
        )
        settingsRepository.updateExposureConfiguration(validatedConfig)
        return validatedConfig
    }

    private fun String.isValidCountryCode() = Locale.getISOCountries().contains(this) && this != FINLAND_CODE

    private fun createTempLocalFile(): File {
        val filename = "${UUID.randomUUID()}.tmp"
        return systemOperations.createFileInCache(filename)
    }

    private suspend fun safeFileDownload(batchId: BatchId, out: FileOutputStream): Boolean {
        return try {
            backendService.getDiagnosisKeyFile(batchId).byteStream().use { input ->
                input.copyTo(out)
            }
            true
        }
        catch (e: Throwable) {
            Timber.e(e, "File download failed")
            false
        }
    }

    companion object {
        const val FAKE_INTERVAL_NUM = 2650847
        const val FAKE_TOKEN = "000000000000"
        private val AUTH_HTTP_ERRORS = setOf(400, 403)
        const val FINLAND_CODE = "FI"
    }
}

sealed class SendKeysResult {
    object Success: SendKeysResult()
    object Unauthorized: SendKeysResult()
    object Failed: SendKeysResult()
}

data class DownloadResult (
    val lastSuccessfulBatchId: BatchId,
    val files: List<File>,
    val exposureConfig: ExposureConfigurationData
)
