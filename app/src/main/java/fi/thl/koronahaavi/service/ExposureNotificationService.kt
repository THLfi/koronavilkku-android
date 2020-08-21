@file:Suppress("DEPRECATION")

package fi.thl.koronahaavi.service

import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import fi.thl.koronahaavi.data.Exposure
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

interface ExposureNotificationService {
    suspend fun enable(): ResolvableResult<Unit>
    suspend fun disable(): ResolvableResult<Unit>
    suspend fun isEnabled(): Boolean
    suspend fun getExposureSummary(token: String): ExposureSummary
    suspend fun getExposureDetails(token: String): List<ExposureInformation>
    suspend fun provideDiagnosisKeyFiles(token: String, files: List<File>, config: ExposureConfigurationData): ResolvableResult<Unit>
    suspend fun getTemporaryExposureKeys(): ResolvableResult<List<TemporaryExposureKey>>

    fun isEnabledFlow() : StateFlow<Boolean?>
    suspend fun refreshEnabledFlow()

    // represents different EN API method results: success, needs user resolution or failed
    sealed class ResolvableResult<T> {
        data class ResolutionRequired<T>(val status: Status) : ResolvableResult<T>()
        data class Success<T>(val data: T) : ResolvableResult<T>()
        data class MissingCapability<T>(val error: String?) : ResolvableResult<T>()
        data class Failed<T>(val apiErrorCode: Int? = null, val error: String?) : ResolvableResult<T>()
    }
}

fun ExposureInformation.toExposure(): Exposure {
    Timber.d(this.toString())

    return Exposure(
        detectedDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(this.dateMillisSinceEpoch), ZoneOffset.UTC),
        totalRiskScore = this.totalRiskScore,
        createdDate = ZonedDateTime.now()
    )
}
