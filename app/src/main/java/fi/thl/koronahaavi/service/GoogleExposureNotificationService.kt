@file:Suppress("DEPRECATION")

package fi.thl.koronahaavi.service

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.os.Process
import android.os.UserManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import fi.thl.koronahaavi.BuildConfig
import fi.thl.koronahaavi.data.Exposure
import fi.thl.koronahaavi.service.ExposureNotificationService.ConnectionError
import fi.thl.koronahaavi.service.ExposureNotificationService.ResolvableResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

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
        }
        isEnabledFlow.value = true
        // Always set the value in case isEnabledFlow.value was out of sync.
    }

    override suspend fun disable() = resultFromRunning {
        if (isEnabled()) {
            client.stop().await()
        }
        isEnabledFlow.value = false
    }

    override suspend fun isEnabled(): Boolean {
        return client.isEnabled.await()
    }

    override suspend fun getExposureSummary(token: String): ExposureSummary {
        return client.getExposureSummary(token).await()
    }

    override suspend fun getExposureDetails(token: String): List<Exposure> {
        // this call will show a system notification to user
        return client.getExposureInformation(token).await()
            .map(ExposureInformation::toExposure)
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

    override fun deviceSupportsLocationlessScanning() = client.deviceSupportsLocationlessScanning()

    override fun getAvailabilityResolver() = GoogleAvailabilityResolver()

    /**
     * check if device owner based on https://stackoverflow.com/a/15448131/1467657
     */
    private fun isOwnerUserProfile(): Boolean =
        (context.getSystemService(Context.USER_SERVICE) as? UserManager)?.let {
            it.getSerialNumberForUser(Process.myUserHandle()) == 0L
        } ?: false

    private suspend fun <T> resultFromRunning(block: suspend () -> T): ResolvableResult<T> {
        try {
            return ResolvableResult.Success(block())
        }
        catch (exception: ApiException) {

            if (exception.status.hasResolution()) {
                Timber.d("Got ApiException with a resolution")
                return ResolvableResult.ResolutionRequired(exception.status)
            }

            Timber.e(exception, "Exposure notification API call failed")

            if (exception.statusCode == ExposureNotificationStatusCodes.FAILED_NOT_SUPPORTED) {
                return ResolvableResult.MissingCapability(exception.localizedMessage)
            }

            if (exception.statusCode == CommonStatusCodes.API_NOT_CONNECTED) {
                // Google Play Services exposure notification module is not available
                // because device not supported, app not authorized to use exposure notifications
                // or other reason as defined by status.connectionResult
                return ResolvableResult.ApiNotSupported(
                    connectionError = connectionErrorFrom(exception.status.connectionResult)
                )
            }

            return ResolvableResult.Failed(
                apiErrorCode = exception.statusCode,
                connectionErrorCode = exception.status.connectionResult?.errorCode,
                error = exception.localizedMessage
            )
        }
        catch (exception: Exception) {
            Timber.e(exception, "Exposure notification API call failed")
            return ResolvableResult.Failed(error = exception.localizedMessage)
        }
    }

    private fun connectionErrorFrom(connectionResult: ConnectionResult?) =
        when (connectionResult?.errorCode) {
            ExposureNotificationStatusCodes.FAILED_NOT_SUPPORTED -> {
                // FAILED_NOT_SUPPORTED occurs for both unsupported device and when user profile is not device owner
                if (isOwnerUserProfile())
                    ConnectionError.DeviceNotSupported
                else
                    ConnectionError.UserIsNotOwner
            }
            ExposureNotificationStatusCodes.FAILED_UNAUTHORIZED -> ConnectionError.ClientNotAuthorized
            else -> ConnectionError.Failed(connectionResult?.errorCode)
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

private fun ExposureInformation.toExposure(): Exposure {
    Timber.d(this.toString())

    return Exposure(
        detectedDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(this.dateMillisSinceEpoch), ZoneOffset.UTC),
        totalRiskScore = this.totalRiskScore,
        createdDate = ZonedDateTime.now()
    )
}

class GoogleAvailabilityResolver : ExposureNotificationService.AvailabilityResolver {
    private val apiAvailability = GoogleApiAvailability.getInstance()

    override fun isSystemAvailable(context: Context): Int =
        apiAvailability.isGooglePlayServicesAvailable(context, MIN_GOOGLE_PLAY_VERSION)

    override fun isUserResolvableError(errorCode: Int) =
        apiAvailability.isUserResolvableError(errorCode)

    override fun showErrorDialogFragment(activity: Activity, errorCode: Int, requestCode: Int,
                                         cancelListener: (dialog: DialogInterface) -> Unit) =
        apiAvailability.showErrorDialogFragment(activity, errorCode, requestCode, cancelListener)

    companion object {
        const val MIN_GOOGLE_PLAY_VERSION = 201813000   // v20.18.13
    }
}