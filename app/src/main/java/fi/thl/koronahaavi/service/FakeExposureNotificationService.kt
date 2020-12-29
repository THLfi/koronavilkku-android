@file:Suppress("DEPRECATION")

package fi.thl.koronahaavi.service

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.DialogInterface
import android.content.Intent
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.nearby.exposurenotification.*
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey.TemporaryExposureKeyBuilder
import fi.thl.koronahaavi.data.Exposure
import fi.thl.koronahaavi.exposure.ExposureStateUpdatedReceiver
import fi.thl.koronahaavi.service.ExposureNotificationService.ResolvableResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.random.Random

/**
 * Fake service that allows to use app without google exposure api
 */
class FakeExposureNotificationService(
    private val context: Context
) : ExposureNotificationService {

    private val isEnabledFlow = MutableStateFlow<Boolean?>(null)
    override fun isEnabledFlow(): StateFlow<Boolean?> = isEnabledFlow

    override suspend fun refreshEnabledFlow() {
        isEnabledFlow.value = isEnabled
    }

    private val prefs =
        context.getSharedPreferences("fi.thl.koronavilkku.sim.prefs", MODE_PRIVATE)

    private var isEnabled =
        prefs.getBoolean(EN_ENABLED_KEY, false)

    override suspend fun enable(): ResolvableResult<Unit> {
        isEnabled = true
        isEnabledFlow.value = true
        prefs.edit().putBoolean(EN_ENABLED_KEY, true).apply()
        return ResolvableResult.Success(Unit)
    }

    override suspend fun disable(): ResolvableResult<Unit> {
        isEnabled = false
        isEnabledFlow.value = false
        prefs.edit().putBoolean(EN_ENABLED_KEY, false).apply()
        return ResolvableResult.Success(Unit)
    }

    override suspend fun isEnabled() = isEnabled

    /*
    override suspend fun getExposureSummary(token: String): ExposureSummary {
        return ExposureSummary.ExposureSummaryBuilder()
            .setMatchedKeyCount(Random.nextInt(1,4))
            .setMaximumRiskScore(200)
            .build()
    }

    override suspend fun getExposureDetails(token: String): List<Exposure> {
        // return a few high risk exposures and one low risk, that should be filtered out
        // simulating EN behavior where it zeroes risk score

        return List(Random.nextInt(1,3)) {
            Exposure(
                detectedDate = ZonedDateTime.now().minusDays(Random.nextLong(2,6)),
                totalRiskScore = 200,
                createdDate = ZonedDateTime.now()
            )
        }.plus(Exposure(
            detectedDate = ZonedDateTime.now().minusDays(Random.nextLong(2,6)),
            totalRiskScore = 0,
            createdDate = ZonedDateTime.now()
        )).also { list ->
            list.forEach { Timber.d(it.toString()) }
        }
    }
     */

    override suspend fun getDailySummaries(config: ExposureConfigurationData): List<DailySummary> {
        return listOf()
    }

    override suspend fun getExposureWindows(): List<ExposureWindow> {
        return listOf()
    }

    override suspend fun provideDiagnosisKeyFiles(files: List<File>): ResolvableResult<Unit> {
        Timber.d("Got %d files", files.size)

        // actual system would save and process the files, but here we only send
        // a broadcast that triggers receiver to call getExposureSummary
        val intent = Intent().apply {
            component = ComponentName(context, ExposureStateUpdatedReceiver::class.java)
            action = ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED
        }
        context.sendBroadcast(intent)
        return ResolvableResult.Success(Unit)
    }

    override suspend fun getTemporaryExposureKeys(): ResolvableResult<List<TemporaryExposureKey>> {
        return ResolvableResult.Success(listOf(
            fakeExposureKey(),
            fakeExposureKey()
        ))
    }

    override fun deviceSupportsLocationlessScanning() = false

    override fun getAvailabilityResolver() = FakeAvailabilityResolver()

    private fun fakeExposureKey() = TemporaryExposureKeyBuilder()
        .setKeyData(Random.nextBytes(16))
        .setRollingPeriod(144)
        .setRollingStartIntervalNumber((Instant.now().epochSecond / 600).toInt())
        .build()

    companion object {
        const val EN_ENABLED_KEY = "en_enabled"
    }
}

class FakeAvailabilityResolver : ExposureNotificationService.AvailabilityResolver {
    override fun isSystemAvailable(context: Context): Int = ConnectionResult.SUCCESS

    override fun isUserResolvableError(errorCode: Int) = false

    override fun showErrorDialogFragment(activity: Activity, errorCode: Int, requestCode: Int,
                                         cancelListener: (dialog: DialogInterface) -> Unit) = false
}