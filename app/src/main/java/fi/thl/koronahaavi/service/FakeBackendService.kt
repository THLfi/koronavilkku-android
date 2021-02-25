package fi.thl.koronahaavi.service

import fi.thl.koronahaavi.service.InfectiousnessLevel.*
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import timber.log.Timber

// not used at the moment, but maybe for standalone demo build or testing
class FakeBackendService : BackendService {

    override suspend fun sendKeys(token: String, data: DiagnosisKeyList, isFake: BackendService.NumericBoolean) {
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
            attenuationBucketThresholdDb = listOf(30, 50, 60),
            attenuationBucketWeights = listOf(1.5, 1.0, 0.5, 0.0),
            daysSinceExposureThreshold = 10,
            daysSinceOnsetToInfectiousness = mapOf(
                "-14" to NONE,
                "-13" to NONE,
                "-12" to NONE,
                "-11" to NONE,
                "-10" to NONE,
                "-9" to NONE,
                "-8" to NONE,
                "-7" to NONE,
                "-6" to NONE,
                "-5" to NONE,
                "-4" to NONE,
                "-3" to NONE,
                "-2" to STANDARD,
                "-1" to HIGH,
                "0" to HIGH,
                "1" to HIGH,
                "2" to HIGH,
                "3" to HIGH,
                "4" to STANDARD,
                "5" to STANDARD,
                "6" to STANDARD,
                "7" to STANDARD,
                "8" to STANDARD,
                "9" to STANDARD,
                "10" to STANDARD,
                "11" to NONE,
                "12" to NONE,
                "13" to NONE,
                "14" to NONE
            ),
            infectiousnessWeightHigh = 1.5,
            infectiousnessWeightStandard = 1.0,
            infectiousnessWhenDaysSinceOnsetMissing = STANDARD,
            minimumDailyScore = 900,
            minimumWindowScore = 1.0,
            reportTypeWeightConfirmedClinicalDiagnosis = 0.0,
            reportTypeWeightConfirmedTest = 1.0,
            reportTypeWeightRecursive = 0.0,
            reportTypeWeightSelfReport = 0.0,
            availableCountries = listOf("DK", "DE", "IE", "IT", "LV", "ES")
        )
    }

    override suspend fun getInitialBatchId(): InitialBatchId {
        return InitialBatchId("fake_initial_batch_id")
    }
}