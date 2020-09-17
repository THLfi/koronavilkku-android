@file:Suppress("DEPRECATION")

package fi.thl.koronahaavi.service

import android.content.ComponentName
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey.TemporaryExposureKeyBuilder
import fi.thl.koronahaavi.exposure.ExposureStateUpdatedReceiver
import fi.thl.koronahaavi.service.ExposureNotificationService.ResolvableResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.io.File
import java.time.Instant
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

    override suspend fun getExposureSummary(token: String): ExposureSummary {
        return ExposureSummary.ExposureSummaryBuilder()
            .setMatchedKeyCount(1)
            .setMaximumRiskScore(200)
            .build()
    }

    override suspend fun getExposureDetails(token: String): List<ExposureInformation> {
        return listOf(
            ExposureInformation.ExposureInformationBuilder()
                .setDateMillisSinceEpoch(ZonedDateTime.now().minusDays(2).toInstant().toEpochMilli())
                .build()
        )
    }

    override suspend fun provideDiagnosisKeyFiles(token: String, files: List<File>, config: ExposureConfigurationData)
            : ResolvableResult<Unit> {
        Timber.d("Got %d files", files.size)

        // actual system would save and process the files, but here we only send
        // a broadcast that triggers receiver to call getExposureSummary
        val intent = Intent().apply {
            component = ComponentName(context, ExposureStateUpdatedReceiver::class.java)
            action = ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED
            putExtra(ExposureNotificationClient.EXTRA_TOKEN, token)
        }
        context.sendBroadcast(intent)
        return ResolvableResult.Success(Unit)
    }

    override suspend fun getTemporaryExposureKeys(): ResolvableResult<List<TemporaryExposureKey>> {
        delay(2000) // simulate some processing
        return ResolvableResult.Success(listOf(
            fakeExposureKey(),
            fakeExposureKey()
        ))
    }

    override fun deviceSupportsLocationlessScanning() = false

    private fun fakeExposureKey() = TemporaryExposureKeyBuilder()
        .setKeyData(Random.nextBytes(16))
        .setRollingPeriod(144)
        .setRollingStartIntervalNumber((Instant.now().epochSecond / 600).toInt())
        .build()

    companion object {
        const val EN_ENABLED_KEY = "en_enabled"
    }
}