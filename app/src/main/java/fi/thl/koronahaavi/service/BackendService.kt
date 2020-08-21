package fi.thl.koronahaavi.service

import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody
import retrofit2.http.*

typealias BatchId = String

/**
 * Backend server API for retrieving configuration and diagnosis key files,
 * and submitting temporary exposure keys from this device
 */
interface BackendService {

    @GET("/exposure/configuration/v1")
    suspend fun getConfiguration(): ExposureConfigurationData

    @GET("/diagnosis/v1/current")
    suspend fun getInitialBatchId(): InitialBatchId

    @GET("/diagnosis/v1/list")
    suspend fun listDiagnosisKeyBatches(@Query("previous") previous: BatchId): DiagnosisKeyBatches

    /*
    @GET("/diagnosis/v1/status")
    suspend fun getStatus(
        @Query("batch") previousBatchId: BatchId?,
        @Query("exposure-config") previousExposureConfigurationVersion: Int?,
        @Query("app-config") previousAppConfigurationVersion: Int?
    ): StatusData
     */

    @Streaming
    @GET("/diagnosis/v1/batch/{batch_id}")
    suspend fun getDiagnosisKeyFile(@Path("batch_id") batchId: BatchId): ResponseBody

    @POST("/diagnosis/v1")
    suspend fun sendKeys(
        @Header("KV-Publish-Token") token: String,
        @Body data: DiagnosisKeyList,
        @Header("KV-Fake-Request") type: RequestType = RequestType.REAL
    )

    enum class RequestType(val code: Int) {
        REAL(0),
        FAKE(1);

        override fun toString(): String {
            return code.toString()
        }
    }
}


/*
@JsonClass(generateAdapter = true)
data class StatusData (
    val startBatch: BatchId,
    val batches: List<BatchId>,
    val exposureConfig: ExposureConfigurationData?
    val appConfig: AppConfiguration?
)
 */

@JsonClass(generateAdapter = true)
data class DiagnosisKey (
    val keyData: String,
    val transmissionRiskLevel: Int,
    val rollingStartIntervalNumber: Int,
    val rollingPeriod: Int
)

@JsonClass(generateAdapter = true)
data class DiagnosisKeyList (
    val keys: List<DiagnosisKey>
)

@JsonClass(generateAdapter = true)
data class ExposureConfigurationData (
    val version: Int,
    val minimumRiskScore: Int,
    val attenuationScores: List<Int>,
    val daysSinceLastExposureScores: List<Int>,
    val durationScores: List<Int>,
    val transmissionRiskScoresAndroid: List<Int>, // <- note specific to android
    val durationAtAttenuationThresholds: List<Int>
)

@JsonClass(generateAdapter = true)
data class AppConfiguration(
    val version: Int,
    val diagnosisKeysPerSubmit: Int,
    val pollingIntervalMinutes: Long,
    val tokenLength: Int
)

data class InitialBatchId(val current: BatchId)

data class DiagnosisKeyBatches(val batches: List<BatchId>)
