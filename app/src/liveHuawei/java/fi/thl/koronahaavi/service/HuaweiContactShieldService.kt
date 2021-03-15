@file:Suppress("DEPRECATION")

package fi.thl.koronahaavi.service

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import com.google.android.gms.nearby.exposurenotification.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.huawei.hms.api.HuaweiApiAvailability
import com.huawei.hms.common.ApiException
import com.huawei.hms.common.ResolvableApiException
import com.huawei.hms.contactshield.*
import com.huawei.hms.contactshield.StatusCode.STATUS_APP_QUOTA_LIMITED
import com.huawei.hms.utils.HMSPackageManager
import fi.thl.koronahaavi.BuildConfig
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.isLessThanWeekOld
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.DailyExposure
import fi.thl.koronahaavi.data.Exposure
import fi.thl.koronahaavi.service.ExposureNotificationService.EnableStep
import fi.thl.koronahaavi.service.ExposureNotificationService.ResolvableResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.min
import kotlin.math.roundToInt

class HuaweiContactShieldService(
    private val context: Context,
    private val engine: ContactShieldEngine,
    private val appStateRepository: AppStateRepository
) : ExposureNotificationService {

    private val isEnabledFlow = MutableStateFlow<Boolean?>(null)
    override fun isEnabledFlow(): StateFlow<Boolean?> = isEnabledFlow

    override suspend fun refreshEnabledFlow() {
        isEnabledFlow.value = isEnabled()
    }

    override suspend fun enable(): ResolvableResult<Unit> {
        // First, test if HMS update required by calling some other method than start, because
        // startContactShield does not return update required error code
        val result = resultFromRunning<Unit> {
            engine.isContactShieldRunning.await()
        }

        return if (result is ResolvableResult.ResolutionRequired) {
            Timber.d("Detected resolution required when starting")
            result
        } else {
            resultFromRunning {
                engine.startContactShield(ContactShieldSetting.DEFAULT).await()
                isEnabledFlow.value = true
            }
        }
    }

    override suspend fun disable() = resultFromRunning {
        Timber.d("calling stopContactShield")
        engine.stopContactShield().await()
        isEnabledFlow.value = false
    }

    override suspend fun isEnabled(): Boolean  =
        when (val result = resultFromRunning { engine.isContactShieldRunning.await() }) {
            is ResolvableResult.Success -> result.data
            else -> false
        }

    override suspend fun getDailyExposures(config: ExposureConfigurationData): List<DailyExposure> {
        updateKeyDataMapping(config)

        return engine.getDailySketch(config.toDailySketchConfiguration())
            .await()
            .map { sketch ->
                Timber.d(sketch.toString())
                DailyExposure(
                    day = LocalDate.ofEpochDay(sketch.daysSinceEpoch.toLong()),
                    score = sketch.sketchData.scoreSum.roundToInt()
                )
            }
    }

    private suspend fun updateKeyDataMapping(config: ExposureConfigurationData) {
        // data mapping should only be updated if actually changed, since calls limited to one per week
        val currentDataMapping: SharedKeysDataMapping? = engine.sharedKeysDataMapping.await()
        val newDataMapping = config.toSharedKeysDataMapping()

        if (currentDataMapping?.isEqualTo(newDataMapping) != true) {
            Timber.d("Setting diagnosis keys data mapping")
            try {
                engine.setSharedKeysDataMapping(newDataMapping).await()
                appStateRepository.setLastExposureKeyMappingUpdate(Instant.now())
            }
            catch (e: ApiException) {
                // setSharedKeysDataMapping is rate limited to one per week, so ignore rate limit errors unless
                // it has been over a week since last successful call.
                // This makes sure the app does not just silently ignore setSharedKeysDataMapping errors
                // and continue for over a week with outdated parameters
                if (e.statusCode == STATUS_APP_QUOTA_LIMITED) {
                    val lastUpdate = appStateRepository.getLastExposureKeyMappingUpdate()
                    if (lastUpdate?.isLessThanWeekOld() == true) {
                        Timber.d("Ignoring rate limiting for setSharedKeysDataMapping")
                    }
                    else {
                        Timber.e("Incorrect rate limiting for setSharedKeysDataMapping, previous update $lastUpdate")
                        throw e
                    }
                }
                else {
                    throw e
                }
            }
        }
    }

    override suspend fun provideDiagnosisKeyFiles(files: List<File>): ResolvableResult<Unit> = resultFromRunning {

        val intent = PendingIntent.getService(
                context,
                0,
                Intent(context, ContactShieldIntentService::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val task = if (BuildConfig.SKIP_EXPOSURE_FILE_VERIFICATION) {
            // use this for local backend when you don't have the signature public key
            Timber.d("Skipping exposure key file signature verification")
            engine.putSharedKeyFiles(intent, SharedKeyFileProvider(files))
        }
        else {
            val publicKeys = listOf(BuildConfig.EXPOSURE_FILE_PUBLIC_KEY)
            Timber.d("putSharedKeyFiles with $publicKeys")
            // todo how to provide public keys
            engine.putSharedKeyFiles(intent, SharedKeyFileProvider(files))
        }

        task.await()
    }

    override suspend fun getTemporaryExposureKeys() = resultFromRunning {
        engine.periodicKey.await().map {
            TemporaryExposureKey.TemporaryExposureKeyBuilder()
                .setKeyData(it.content)
                .setRollingStartIntervalNumber(
                    convertToDayFirstInterval(it.periodicKeyValidTime.toInt())
                )
                .setRollingPeriod(
                    convertToRollingPeriod(it.periodicKeyValidTime.toInt(), it.periodicKeyLifeTime.toInt())
                )
                .setTransmissionRiskLevel(it.initialRiskLevel)
                .build()
        }
    }

    // convert to beginning of day in UTC, to match Google implementation
    private fun convertToDayFirstInterval(periodicKeyValidTime: Int) =
        periodicKeyValidTime / ROLLING_INTERVALS_IN_DAY * ROLLING_INTERVALS_IN_DAY

    // convert to match Google implementation
    private fun convertToRollingPeriod(periodicKeyValidTime: Int, periodicKeyLifeTime: Int): Int {
        val dayFirstInterval = convertToDayFirstInterval(periodicKeyValidTime)

        return min(periodicKeyValidTime + periodicKeyLifeTime - dayFirstInterval, ROLLING_INTERVALS_IN_DAY)
    }

    override fun deviceSupportsLocationlessScanning() = false

    override fun getAvailabilityResolver() = HuaweiAvailabilityResolver()

    private suspend fun <T> resultFromRunning(block: suspend () -> T): ResolvableResult<T> {
        return try {
            ResolvableResult.Success(block())
        }
        catch (resolvable: ResolvableApiException) {
            Timber.e("HMS API call failed with resolvable exception, message: ${resolvable.localizedMessage}")
            ResolvableResult.ResolutionRequired(
                HuaweiApiErrorResolver(resolvable)
            )
        }
        catch (exception: ApiException) {
            Timber.e(exception, "HMS API call failed, status code ${exception.statusCode}")
            when (exception.statusCode) {
                ExposureNotificationStatusCodes.FAILED_NOT_SUPPORTED -> ResolvableResult.MissingCapability(exception.localizedMessage)
                StatusCode.STATUS_MISSING_PERMISSION_LOCATION -> ResolvableResult.HmsCanceled(EnableStep.LocationPermission)
                StatusCode.STATUS_UNAUTHORIZED -> ResolvableResult.HmsCanceled(EnableStep.UserConsent)
                else -> {
                    ResolvableResult.Failed(
                        apiErrorCode = exception.statusCode,
                        connectionErrorCode = 0,
                        error = exception.localizedMessage
                    )
                }
            }
        }
        catch (exception: Exception) {
            Timber.e(exception, "Contact shield API call failed")
            ResolvableResult.Failed(error = exception.localizedMessage)
        }
    }

    // need to compare fields since SharedKeysDataMapping does not define equals
    private fun SharedKeysDataMapping.isEqualTo(other: SharedKeysDataMapping) =
        defaultReportType == other.defaultReportType &&
        defaultContagiousness == other.defaultContagiousness &&
        daysSinceCreationToContagiousness == other.daysSinceCreationToContagiousness

    companion object {
        const val ROLLING_INTERVALS_IN_DAY = 144
    }
}



/**
 * Awaits for completion of the task without blocking a thread.
 *
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * stops waiting for the completion stage and immediately resumes with [CancellationException].
 */
suspend fun <T> com.huawei.hmf.tasks.Task<T>.await(): T {
    // fast path
    if (isComplete) {
        val e = exception
        return if (e == null) {
            if (isCanceled) {
                throw CancellationException("Task $this was cancelled normally.")
            } else {
                @Suppress("UNCHECKED_CAST")
                result as T
            }
        } else {
            throw e
        }
    }

    return suspendCancellableCoroutine { cont ->
        addOnCompleteListener {
            val e = exception
            if (e == null) {
                @Suppress("UNCHECKED_CAST")
                if (isCanceled) cont.cancel() else cont.resume(result as T)
            } else {
                cont.resumeWithException(e)
            }
        }
    }
}

private fun ContactDetail.toExposure(
    createdDate: ZonedDateTime = ZonedDateTime.now()
) = Exposure(
    detectedDate = LocalDate.ofEpochDay(dayNumber).atTime(0, 0).atZone(ZoneOffset.UTC),
    totalRiskScore = totalRiskValue,
    createdDate = createdDate
)

class HuaweiAvailabilityResolver : ExposureNotificationService.AvailabilityResolver {
    private val apiAvailability = HuaweiApiAvailability.getInstance()

    override fun isSystemAvailable(context: Context): Int =
        apiAvailability.isHuaweiMobileServicesAvailable(context, MIN_HMS_CORE_VERSION)

    override fun isUserResolvableError(errorCode: Int) =
        apiAvailability.isUserResolvableError(errorCode)

    override fun showErrorDialogFragment(activity: Activity, errorCode: Int, requestCode: Int,
                                         cancelListener: (dialog: DialogInterface) -> Unit): Boolean {
        // apiAvailability.showErrorDialogFragment does not work with the latest sdk, so we are
        // only displaying a message to update hms core manually
        MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.enable_err_title)
                .setMessage(R.string.enable_err_hms_update_required)
                .setOnCancelListener(cancelListener)
                .setPositiveButton(R.string.enable_err_hms_update) { dialog: DialogInterface, _ ->
                    tryStartAppGallery(activity)
                    cancelListener.invoke(dialog)
                }
                .show()
        return true
    }

    /**
     * Start app gallery activity for HMS Core so that user can easily update
     */
    private fun tryStartAppGallery(activity: Activity) {
        try {
            val hmsPackageName = HMSPackageManager.getInstance(activity).hmsPackageName
            val intent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                data = Uri.parse("appmarket://details?id=$hmsPackageName")
            }

            if (intent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(intent)
            }
            else {
                Timber.e("Could not resolve activity for ${intent.dataString}")
            }
        }
        catch (t: Throwable) {
            Timber.e(t, "Failed to start app gallery activity")
        }
    }

    companion object {
        // 5.0.5.300 required due to Contact Shield incompatibility with earlier HMS core versions
        const val MIN_HMS_CORE_VERSION = 50005300
    }
}

//
// https://developer.huawei.com/consumer/en/doc/development/HMSCore-Guides-V5/contactshield-faq-0000001059216978-V5#EN-US_TOPIC_0000001059216978__section185841314165113
//
class HuaweiApiErrorResolver(private val resolvable: ResolvableApiException) : ExposureNotificationService.ApiErrorResolver {
    override fun startResolutionForResult(activity: Activity, resultCode: Int) {
        try {
            resolvable.startResolutionForResult(activity, resultCode)
        }
        catch (e: IntentSender.SendIntentException) {
            Timber.e(e, "Failed to start resolution")
        }
    }
}

fun ExposureConfigurationData.toDailySketchConfiguration(): DailySketchConfiguration =
        DailySketchConfiguration.Builder()
                .setThresholdsOfAttenuationInDb(attenuationBucketThresholdDb, attenuationBucketWeights)
                .setWeightOfContagiousness(Contagiousness.STANDARD, infectiousnessWeightStandard)
                .setWeightOfContagiousness(Contagiousness.HIGH, infectiousnessWeightHigh)
                .setWeightOfReportType(ReportType.CONFIRMED_CLINICAL_DIAGNOSIS, reportTypeWeightConfirmedClinicalDiagnosis)
                .setWeightOfReportType(ReportType.CONFIRMED_TEST, reportTypeWeightConfirmedTest)
                .setWeightOfReportType(ReportType.SELF_REPORT, reportTypeWeightSelfReport)
                .setWeightOfReportType(ReportType.RECURSIVE, reportTypeWeightRecursive)
                .setMinWindowScore(minimumWindowScore)
                .setThresholdOfDaysSinceHit(daysSinceExposureThreshold)
                .build()

// backend api uses the term infectiousness to match google EN implementation
// but contact shield uses term contagiousness for the same data
fun ExposureConfigurationData.toSharedKeysDataMapping(): SharedKeysDataMapping =
        SharedKeysDataMapping.Builder()
                .setDefaultReportType(ReportType.CONFIRMED_TEST)
                .setDefaultContagiousness(infectiousnessWhenDaysSinceOnsetMissing.toContagiousness())
                .setDaysSinceCreationToContagiousness(daysSinceOnsetToInfectiousness.entries.associate {
                    it.key.toInt() to it.value.toContagiousness()
                })
                .build()

fun InfectiousnessLevel.toContagiousness() = when (this) {
    InfectiousnessLevel.HIGH -> Contagiousness.HIGH
    InfectiousnessLevel.STANDARD -> Contagiousness.STANDARD
    InfectiousnessLevel.NONE -> Contagiousness.NONE
}
