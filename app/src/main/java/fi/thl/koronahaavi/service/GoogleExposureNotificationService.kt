@file:Suppress("DEPRECATION")

package fi.thl.koronahaavi.service

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import fi.thl.koronahaavi.BuildConfig
import fi.thl.koronahaavi.service.ExposureNotificationService.ResolvableResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File

class GoogleExposureNotificationService(
    private val context: Context
) : ExposureNotificationService {

    private val client by lazy {
        Nearby.getExposureNotificationClient(context)
    }

    private val isEnabledFlow = MutableStateFlow<Boolean?>(null)
    override fun isEnabledFlow(): StateFlow<Boolean?> = isEnabledFlow

    override suspend fun refreshEnabledFlow() {
        isEnabledFlow.value = isEnabled()
    }

    override suspend fun enable() = resultFromRunning {
        if (!isEnabled()) {
            client.start().await()
            isEnabledFlow.value = true
        }
    }

    override suspend fun disable() = resultFromRunning {
        if (isEnabled()) {
            client.stop().await()
            isEnabledFlow.value = false
        }
    }

    override suspend fun isEnabled(): Boolean {
        return client.isEnabled.await()
    }

    override suspend fun getExposureSummary(token: String): ExposureSummary {
        return client.getExposureSummary(token).await()
    }

    override suspend fun getExposureDetails(token: String): List<ExposureInformation> {
        // this call will show a system notification to user
        return client.getExposureInformation(token).await()
    }

    override suspend fun provideDiagnosisKeyFiles(token: String, files: List<File>, config: ExposureConfigurationData)
            : ResolvableResult<Unit> {
        Timber.d("Token $token, config: ${config.toExposureConfiguration()}")
        Timber.d("Providing %d key files", files.size)

        return resultFromRunning<Unit> {
            client.provideDiagnosisKeys(files, config.toExposureConfiguration(), token).await()
        }
    }

    override suspend fun getTemporaryExposureKeys() = resultFromRunning {
        client.temporaryExposureKeyHistory.await()
    }

    private suspend fun <T> resultFromRunning(block: suspend () -> T): ResolvableResult<T> {
        try {
            return ResolvableResult.Success(block())
        }
        catch (exception: ApiException) {

            if (exception.status.hasResolution()) {
                Timber.d("Got ApiException with a resolution")
                return ResolvableResult.ResolutionRequired(exception.status)
            }

            if (exception.statusCode == NOT_SUPPORTED_API_ERROR_STATUS) {
                return ResolvableResult.MissingCapability(exception.localizedMessage)
            }

            Timber.e(exception, "Exposure notification API call failed")
            return ResolvableResult.Failed(exception.statusCode, exception.localizedMessage)
        }
        catch (exception: Exception) {
            Timber.e(exception, "Exposure notification API call failed")
            return ResolvableResult.Failed(error = exception.localizedMessage)
        }
    }

    companion object {
        private const val NOT_SUPPORTED_API_ERROR_STATUS = 39501
    }

    // this maps backend json object to google EN api configuration
    private fun ExposureConfigurationData.toExposureConfiguration(): ExposureConfiguration {

        // when test UI is enabled, override minimum risk score so that we get the calculated risk score
        // from EN api, otherwise it is zeroed out and test UI is not useful
        val minRiskScore = if (BuildConfig.ENABLE_TEST_UI) 1 else minimumRiskScore

        return ExposureConfiguration.ExposureConfigurationBuilder()
            .setMinimumRiskScore(minRiskScore)
            .setAttenuationScores(*attenuationScores.toIntArray())
            .setDaysSinceLastExposureScores(*daysSinceLastExposureScores.toIntArray())
            .setDurationScores(*durationScores.toIntArray())
            .setTransmissionRiskScores(*transmissionRiskScoresAndroid.toIntArray())
            .setDurationAtAttenuationThresholds(*durationAtAttenuationThresholds.toIntArray())
            .build()
    }
}

/* EN api error codes for reference.. currently only handling not_supported 39501

/** The operation failed, without any more information.  */
private const val FAILED = CommonStatusCodes.ERROR // 13

/** The app was already in the requested state so the call did nothing.  */
private const val FAILED_ALREADY_STARTED = 39500

/** The hardware capability of the device was not supported.  */
private const val FAILED_NOT_SUPPORTED = 39501

/** The user rejected the opt-in state.  */
private const val FAILED_REJECTED_OPT_IN = 39502

/** The functionality was disabled by the user or the phone.  */
private const val FAILED_SERVICE_DISABLED = 39503

/** The bluetooth was powered off.  */
private const val FAILED_BLUETOOTH_DISABLED = 39504

/** The service was disabled for some reasons temporarily.  */
private const val FAILED_TEMPORARILY_DISABLED = 39505

/** The operation failed during a disk read/write.  */
private const val FAILED_DISK_IO = 39506

/** The client is unauthorized to access the APIs.  */
private const val FAILED_UNAUTHORIZED = 39507

/** The client has been rate limited for access to this API.  */
private const val FAILED_RATE_LIMITED = 39508

 */
