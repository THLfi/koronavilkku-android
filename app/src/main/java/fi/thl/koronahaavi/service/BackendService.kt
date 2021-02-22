package fi.thl.koronahaavi.service

import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson
import okhttp3.ResponseBody
import retrofit2.http.*

typealias BatchId = String

/**
 * Backend server API for retrieving configuration and diagnosis key files,
 * and submitting temporary exposure keys from this device
 */
interface BackendService {

    @GET("exposure/configuration/v1")
    suspend fun getConfiguration(): ExposureConfigurationData

    @GET("diagnosis/v1/current?en-api-version=2")
    suspend fun getInitialBatchId(): InitialBatchId

    @GET("diagnosis/v1/list?en-api-version=2")
    suspend fun listDiagnosisKeyBatches(@Query("previous") previous: BatchId): DiagnosisKeyBatches

    @Streaming
    @GET("diagnosis/v1/batch/{batch_id}")
    suspend fun getDiagnosisKeyFile(@Path("batch_id") batchId: BatchId): ResponseBody

    @POST("diagnosis/v1")
    suspend fun sendKeys(
        @Header("KV-Publish-Token") token: String,
        @Body data: DiagnosisKeyList,
        @Header("KV-Fake-Request") isFake: NumericBoolean = NumericBoolean.FALSE
    )

    // This class is used to serialize boolean data with numbers 1 and 0, so that message
    // byte length is the same regardless of value
    enum class NumericBoolean(val code: Int) {
        FALSE(0),
        TRUE(1);

        override fun toString(): String {
            return code.toString()
        }

        companion object {
            fun from(value: Boolean) = if (value) TRUE else FALSE
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
    val keys: List<DiagnosisKey>,
    val visitedCountries: Map<String, BackendService.NumericBoolean>,
    val consentToShareWithEfgs: BackendService.NumericBoolean
)

@JsonClass(generateAdapter = true)
data class ExposureConfigurationData (
    val version: Int,
    val minimumRiskScore: Int,
    val attenuationScores: List<Int>,
    val daysSinceLastExposureScores: List<Int>,
    val durationScores: List<Int>,
    val transmissionRiskScoresAndroid: List<Int>, // <- note specific to android
    val durationAtAttenuationThresholds: List<Int>,
    val durationAtAttenuationWeights: List<Float>, // decimal weights for attenuation buckets in summary
    val exposureRiskDuration: Int,
    val participatingCountries: List<String>? // country codes for EU interoperability
)

@JsonClass(generateAdapter = true)
data class AppConfiguration(
    val version: Int,
    val diagnosisKeysPerSubmit: Int,
    val pollingIntervalMinutes: Long,
    val tokenLength: Int,
    val exposureValidDays: Int
)

data class InitialBatchId(val current: BatchId)

data class DiagnosisKeyBatches(val batches: List<BatchId>)

class NumericBooleanAdapter{
    @ToJson
    fun toJson(obj: BackendService.NumericBoolean): Int = obj.code

    // this is a one-way adapter, and does not have a fromJson method
    // since we do not need to read these values from json
}