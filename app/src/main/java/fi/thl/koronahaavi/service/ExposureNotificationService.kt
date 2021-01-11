@file:Suppress("DEPRECATION")

package fi.thl.koronahaavi.service

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
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
    fun getAvailabilityResolver(): AvailabilityResolver

    fun isEnabledFlow() : StateFlow<Boolean?>
    suspend fun refreshEnabledFlow()

    // represents different EN API method results: success, needs user resolution or failed
    sealed class ResolvableResult<T> {
        data class HmsCanceled<T>(val step: EnableStep) : ResolvableResult<T>()
        data class ResolutionRequired<T>(val errorResolver: ApiErrorResolver) : ResolvableResult<T>()
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

    // Specifies what was canceled when enable result is Canceled (these are only used for Huawei Contact Shield)
    sealed class EnableStep {
        object UserConsent: EnableStep()
        object LocationPermission: EnableStep()
        data class UpdateRequired(val retry: suspend (activity: Activity) -> ResolvableResult<Unit>): EnableStep()
    }

    // Provides methods to check and resolve exposure system availability in the UI
    interface AvailabilityResolver {
        fun isSystemAvailable(context: Context): Int
        fun isUserResolvableError(errorCode: Int): Boolean
        fun showErrorDialogFragment(activity: Activity, errorCode: Int, requestCode: Int, cancelListener: (dialog: DialogInterface) -> Unit): Boolean
    }

    interface ApiErrorResolver {
        fun startResolutionForResult(activity: Activity, resultCode: Int)
    }
}
