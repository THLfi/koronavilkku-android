package fi.thl.koronahaavi.service

import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import timber.log.Timber

// not used at the moment, but maybe for standalone demo build or testing
class FakeBackendService : BackendService {

    override suspend fun sendKeys(token: String, data: DiagnosisKeyList, type: BackendService.RequestType) {
        Timber.d("Sending keys")
    }

    override suspend fun listDiagnosisKeyBatches(previous: BatchId): DiagnosisKeyBatches {
        return DiagnosisKeyBatches(listOf("batch_id"))
    }

    override suspend fun getDiagnosisKeyFile(batchId: BatchId): ResponseBody {
        return "test".toResponseBody()
    }

    override suspend fun getConfiguration(): ExposureConfigurationData {
        return ExposureConfigurationData(
            version = 1,
            minimumRiskScore = 0,
            attenuationScores = listOf(),
            daysSinceLastExposureScores = listOf(),
            durationScores = listOf(),
            transmissionRiskScoresAndroid = listOf(),
            durationAtAttenuationThresholds = listOf(),
            durationAtAttenuationWeights = listOf(1.0f, 0.5f, 0.0f),
            exposureRiskDuration = 15
        )
    }

    override suspend fun getInitialBatchId(): InitialBatchId {
        return InitialBatchId("fake_initial_batch_id")
    }

    /*
    override suspend fun getStatus(previousBatchId: BatchId?,
                                   previousExposureConfigurationVersion: Int?,
                                   previousAppConfigurationVersion: Int?
    ): StatusData {
        return StatusData(
            startBatch = "start_id",
            batches = listOf("batch_id"),
            exposureConfig = exposureConfig,
            appConfig = appConfig
        )
    }

    private val appConfig = AppConfiguration(
        version = 1,
        pollingIntervalMinutes = 120,
        diagnosisKeysPerSubmit = 14,
        notificationMinDurationHours = 6,
        tokenLength = 12
    )

    private val exposureConfig = ExposureConfigurationData(
        minimumRiskScore = 1,
        attenuationScores = listOf(),
        daysSinceLastExposureScores = listOf(),
        durationScores = listOf(),
        transmissionRiskScores = listOf(),
        durationAtAttenuationThresholds = listOf()
    )
     */
}