package fi.thl.koronahaavi.data

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.thl.koronahaavi.common.getInstant
import fi.thl.koronahaavi.common.setInstant
import fi.thl.koronahaavi.di.AppStatePreferencesName
import fi.thl.koronahaavi.service.BackendService
import fi.thl.koronahaavi.service.BatchId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppStateRepository @Inject constructor (
        private val prefs: SharedPreferences,
        private val backendService: BackendService,
        @ApplicationContext context: Context,
        @AppStatePreferencesName preferencesName: String
) {
    private val onBoardingCompleteKey = "onboarding_complete"
    private val lastBatchIdKey = "last_batch_id"
    private val lastExposureCheckTimeKey = "last_exposure_check"
    private val lastExposureKeyMappingUpdateKey = "last_exposure_key_mapping_update"
    private val diagnosisKeysSubmittedKey = "diagnosis_keys_submitted"
    private val appShutdownKey = "app_shutdown"

    // this is an unencrypted shared preferences instance which is used to avoid further
    // load to unstable encrypted shared preferences, for data that is not sensitive
    private val appSharedPreferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    private val keysSubmitted = MutableStateFlow(
        prefs.getBoolean(diagnosisKeysSubmittedKey, false)
    )
    fun lockedAfterDiagnosis(): StateFlow<Boolean> = keysSubmitted

    private val appShutdown = MutableStateFlow(
        prefs.getBoolean(appShutdownKey, false)
    )
    fun appShutdown(): StateFlow<Boolean> = appShutdown

    private val lastExposureCheckTime = MutableLiveData<ZonedDateTime?>(null)
    fun getLastExposureCheckTimeLive(): LiveData<ZonedDateTime?> {
        if (lastExposureCheckTime.value == null) {
            updateLastExposureCheckTime() // initial value set when livedata used
        }
        return lastExposureCheckTime
    }

    fun getLastExposureCheckTime(): ZonedDateTime? =
        if (prefs.contains(lastExposureCheckTimeKey)) {
            val epochSec = prefs.getLong(lastExposureCheckTimeKey, 0)
            ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSec), ZoneId.systemDefault())
        }
        else {
            null
        }

    fun setDiagnosisKeysSubmitted(submitted: Boolean) {
        keysSubmitted.value = submitted
        prefs.edit().putBoolean(diagnosisKeysSubmittedKey, submitted).apply()
    }

    fun setAppShutdown(shutdown: Boolean) {
        appShutdown.value = shutdown
        prefs.edit().putBoolean(appShutdownKey, shutdown).apply()
    }

     private fun updateLastExposureCheckTime() {
        lastExposureCheckTime.postValue(getLastExposureCheckTime())
    }

    fun setLastExposureCheckTime(time: ZonedDateTime) {
        prefs.edit().putLong(lastExposureCheckTimeKey, time.toEpochSecond()).apply()
        updateLastExposureCheckTime()
    }

    fun setLastExposureCheckTimeToNow() = setLastExposureCheckTime(ZonedDateTime.now())

    fun isOnboardingComplete() = prefs.getBoolean(onBoardingCompleteKey, false)

    fun setOnboardingComplete(complete: Boolean) {
        prefs.edit().putBoolean(onBoardingCompleteKey, complete).apply()
    }

    suspend fun getDiagnosisKeyBatchId(): BatchId = prefs.getString(lastBatchIdKey, null) ?: getInitialBatchId()

    fun setDiagnosisKeyBatchId(id: BatchId) {
        prefs.edit().putString(lastBatchIdKey, id).apply()
    }

    fun resetDiagnosisKeyBatchId() {
        prefs.edit().remove(lastBatchIdKey).apply()
    }

    fun getLastExposureKeyMappingUpdate(): Instant? = appSharedPreferences.getInstant(lastExposureKeyMappingUpdateKey)

    fun setLastExposureKeyMappingUpdate(time: Instant) = appSharedPreferences.setInstant(lastExposureKeyMappingUpdateKey, time)

    private suspend fun getInitialBatchId() : BatchId
            = backendService.getInitialBatchId().current.also { setDiagnosisKeyBatchId(it) }
}