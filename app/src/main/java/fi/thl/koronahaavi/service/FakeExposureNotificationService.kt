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
import fi.thl.koronahaavi.data.DailyExposure
import fi.thl.koronahaavi.data.Exposure
import fi.thl.koronahaavi.exposure.ExposureStateUpdatedReceiver
import fi.thl.koronahaavi.service.ExposureNotificationService.ResolvableResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.io.File
import java.time.Instant
import java.time.LocalDate
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

    override suspend fun getDailyExposures(config: ExposureConfigurationData): List<DailyExposure> {
        return List(Random.nextInt(1,3)) { index ->
            val day = LocalDate.now().minusDays(index.plus(2L))
            Timber.d("Creating fake exposure for $day")
            DailyExposure(day, 1000)
        }
    }

    override suspend fun getExposureWindows(): List<ExposureWindow> = listOf()

    override suspend fun provideDiagnosisKeyFiles(config: ExposureConfigurationData, files: List<File>): ResolvableResult<Unit> {
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