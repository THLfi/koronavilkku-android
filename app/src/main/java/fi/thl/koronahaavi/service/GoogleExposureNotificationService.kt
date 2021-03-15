@file:Suppress("DEPRECATION")

package fi.thl.koronahaavi.service

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.exposurenotification.*
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes.FAILED_RATE_LIMITED
import fi.thl.koronahaavi.common.isLessThanWeekOld
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.DailyExposure
import fi.thl.koronahaavi.service.ExposureNotificationService.ConnectionError
import fi.thl.koronahaavi.service.ExposureNotificationService.ResolvableResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

class GoogleExposureNotificationService(
    private val client: ExposureNotificationClient,
    private val appStateRepository: AppStateRepository
) : ExposureNotificationService {

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

    override suspend fun isEnabled(): Boolean =
        when (val result = resultFromRunning { client.isEnabled.await() }) {
            is ResolvableResult.Success -> result.data
            else -> false
        }

    override suspend fun getDailyExposures(config: ExposureConfigurationData): List<DailyExposure> {
        updateKeyDataMapping(config)

        return client.getDailySummaries(config.toDailySummariesConfig())
            .await()
            .map { summary ->
                Timber.d(summary.toString())
                DailyExposure(
                    day = LocalDate.ofEpochDay(summary.daysSinceEpoch.toLong()),
                    score = summary.summaryData.scoreSum.roundToInt()
                )
            }
    }

    private suspend fun updateKeyDataMapping(config: ExposureConfigurationData) {
        // data mapping should only be updated if actually changed, since calls limited to 2 per week
        val currentDataMapping = client.diagnosisKeysDataMapping.await()
        val newDataMapping = config.toDiagnosisKeysDataMapping()

        if (newDataMapping != currentDataMapping) {
            Timber.d("Setting new diagnosis keys data mapping")
            try {
                client.setDiagnosisKeysDataMapping(newDataMapping).await()
                appStateRepository.setLastExposureKeyMappingUpdate(Instant.now())
            }
            catch (e: ApiException) {
                // setDiagnosisKeysDataMapping is rate limited to two per week, so ignore rate limit errors unless
                // it has been over a week since last successful call.
                // This makes sure the app does not just silently ignore setDiagnosisKeysDataMapping errors
                // and continue for over a week with outdated parameters
                if (e.statusCode == FAILED_RATE_LIMITED) {
                    val lastUpdate = appStateRepository.getLastExposureKeyMappingUpdate()
                    if (lastUpdate?.isLessThanWeekOld() == true) {
                        Timber.d("Ignoring rate limiting for setDiagnosisKeysDataMapping")
                    }
                    else {
                        Timber.e("Incorrect rate limiting for setDiagnosisKeysDataMapping, previous update $lastUpdate")
                        throw e
                    }
                }
                else {
                    throw e
                }
            }
        }
    }

    override suspend fun provideDiagnosisKeyFiles(files: List<File>): ResolvableResult<Unit> {
        Timber.d("Providing %d key files", files.size)

        return resultFromRunning {
            verifyENVersion()
            client.provideDiagnosisKeys(files).await()
        }
    }

    private suspend fun verifyENVersion() {
        // EN module version 1.6 is required for getDailySummaries and getVersion methods,
        // so calling getVersion successfully verifies that we have at least 1.6
        // Otherwise the method should throw API_NOT_CONNECTED error, but also check returned
        // version number to make sure it works as expected
        val fullVersion = client.version.await()
        Timber.d("EN module version $fullVersion")

        if (fullVersion.toENVersion()?.let { it < MIN_EN_VERSION } == true) {
            throw Exception("Unsupported EN module version $fullVersion")
        }
    }

    override suspend fun getTemporaryExposureKeys() = resultFromRunning {
        client.temporaryExposureKeyHistory.await()
    }

    override fun deviceSupportsLocationlessScanning() = client.deviceSupportsLocationlessScanning()

    override fun getAvailabilityResolver() = GoogleAvailabilityResolver()

    private suspend fun isOwnerUserProfile(): Boolean =
        ExposureNotificationStatus.USER_PROFILE_NOT_SUPPORT !in client.status.await()

    private suspend fun <T> resultFromRunning(block: suspend () -> T): ResolvableResult<T> {
        try {
            return ResolvableResult.Success(block())
        }
        catch (exception: ApiException) {

            if (exception.status.hasResolution()) {
                Timber.d("Got ApiException with a resolution")
                return ResolvableResult.ResolutionRequired(
                    GoogleApiErrorResolver(exception.status)
                )
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

    private suspend fun connectionErrorFrom(connectionResult: ConnectionResult?) =
        when (connectionResult?.errorCode) {
            ExposureNotificationStatusCodes.FAILED_NOT_SUPPORTED -> ConnectionError.DeviceNotSupported
            ExposureNotificationStatusCodes.FAILED_UNAUTHORIZED -> ConnectionError.ClientNotAuthorized
            else -> {
                if (!isOwnerUserProfile())
                    ConnectionError.UserIsNotOwner
                else
                    ConnectionError.Failed(connectionResult?.errorCode)
            }
        }

    private fun Long.toENVersion(): Long? =
        try {
            // Parse out first two numbers of full version which are the major and minor
            // to ignore variance in full version number length and allow for easy comparison
            // This approach is copied from Google reference implementation
            this.toString().substring(0, 2).toLong()
        }
        catch (t: Throwable) {
            Timber.e(t)
            null
        }

    companion object {
        const val MIN_EN_VERSION = 16L   // v1.6 for getDailySummaries
    }
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

class GoogleApiErrorResolver(private val status: Status) : ExposureNotificationService.ApiErrorResolver {
    override fun startResolutionForResult(activity: Activity, resultCode: Int) {
        status.startResolutionForResult(activity, resultCode)
    }
}

// this maps backend json config object to google EN api daily configuration
fun ExposureConfigurationData.toDailySummariesConfig(): DailySummariesConfig =
        DailySummariesConfig.DailySummariesConfigBuilder()
                .setAttenuationBuckets(attenuationBucketThresholdDb, attenuationBucketWeights)
                .setInfectiousnessWeight(Infectiousness.STANDARD, infectiousnessWeightStandard)
                .setInfectiousnessWeight(Infectiousness.HIGH, infectiousnessWeightHigh)
                .setReportTypeWeight(ReportType.CONFIRMED_CLINICAL_DIAGNOSIS, reportTypeWeightConfirmedClinicalDiagnosis)
                .setReportTypeWeight(ReportType.CONFIRMED_TEST, reportTypeWeightConfirmedTest)
                .setReportTypeWeight(ReportType.SELF_REPORT, reportTypeWeightSelfReport)
                .setReportTypeWeight(ReportType.RECURSIVE, reportTypeWeightRecursive)
                .setMinimumWindowScore(minimumWindowScore)
                .setDaysSinceExposureThreshold(daysSinceExposureThreshold)
                .build()

fun ExposureConfigurationData.toDiagnosisKeysDataMapping(): DiagnosisKeysDataMapping =
        DiagnosisKeysDataMapping.DiagnosisKeysDataMappingBuilder()
                .setReportTypeWhenMissing(ReportType.CONFIRMED_TEST)
                .setInfectiousnessWhenDaysSinceOnsetMissing(infectiousnessWhenDaysSinceOnsetMissing.toInfectiousness())
                .setDaysSinceOnsetToInfectiousness(daysSinceOnsetToInfectiousness.entries.associate {
                    it.key.toInt() to it.value.toInfectiousness()
                })
                .build()

fun InfectiousnessLevel.toInfectiousness() = when (this) {
    InfectiousnessLevel.HIGH -> Infectiousness.HIGH
    InfectiousnessLevel.STANDARD -> Infectiousness.STANDARD
    InfectiousnessLevel.NONE -> Infectiousness.NONE
}