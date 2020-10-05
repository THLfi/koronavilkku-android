@file:Suppress("DEPRECATION")

package fi.thl.koronahaavi.service

import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import fi.thl.koronahaavi.data.Exposure
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface ExposureNotificationService {
    suspend fun enable(): ResolvableResult<Unit>
    suspend fun disable(): ResolvableResult<Unit>
    suspend fun isEnabled(): Boolean
    suspend fun getExposureSummary(token: String): ExposureSummary
    suspend fun getExposureDetails(token: String): List<Exposure>
    suspend fun provideDiagnosisKeyFiles(token: String, files: List<File>, config: ExposureConfigurationData): ResolvableResult<Unit>
    suspend fun getTemporaryExposureKeys(): ResolvableResult<List<TemporaryExposureKey>>
    fun deviceSupportsLocationlessScanning(): Boolean

    fun isEnabledFlow() : StateFlow<Boolean?>
    suspend fun refreshEnabledFlow()

    // represents different EN API method results: success, needs user resolution or failed
    sealed class ResolvableResult<T> {
        data class ResolutionRequired<T>(val status: Status) : ResolvableResult<T>()
        data class Success<T>(val data: T) : ResolvableResult<T>()
        data class MissingCapability<T>(val error: String?) : ResolvableResult<T>()
        data class ApiNotSupported<T>(val connectionError: ConnectionError?) : ResolvableResult<T>()
        data class Failed<T>(val apiErrorCode: Int? = null, val connectionErrorCode: Int? = null, val error: String?) : ResolvableResult<T>()
    }

    // These map to ExposureNotificationStatusCodes, but only the ones have been defined which
    // we need to handle in user interface to show specific error
    sealed class ConnectionError {
        object DeviceNotSupported: ConnectionError()  // 39501
        object UserIsNotOwner: ConnectionError()      // special case for 39501
        object ClientNotAuthorized: ConnectionError()  // 39507
        data class Failed(val errorCode: Int?): ConnectionError()
    }
}
