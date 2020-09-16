package fi.thl.koronahaavi.data

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fi.thl.koronahaavi.service.BackendService
import fi.thl.koronahaavi.service.BatchId
import fi.thl.koronahaavi.service.WorkDispatcher
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
    private val workDispatcher: WorkDispatcher
) {
    private val onBoardingCompleteKey = "onboarding_complete"
    private val lastBatchIdKey = "last_batch_id"
    private val lastExposureCheckTimeKey = "last_exposure_check"
    private val diagnosisKeysSubmittedKey = "diagnosis_keys_submitted"
    private val powerOptimizationDisabledKey = "power_optimization_disabled"

    private val keysSubmitted = MutableStateFlow(
        prefs.getBoolean(diagnosisKeysSubmittedKey, false)
    )
    fun lockedAfterDiagnosis(): StateFlow<Boolean> = keysSubmitted

    private val lastExposureCheckTime = MutableLiveData<ZonedDateTime?>(null)
    fun lastExposureCheckTime(): LiveData<ZonedDateTime?> = lastExposureCheckTime

    init {
        updateLastExposureCheckTime()
    }

    fun setPowerOptimizationDisableAllowed(isAllowed: Boolean) {
        prefs.edit().putBoolean(powerOptimizationDisabledKey, isAllowed).apply()
    }

    fun isPowerOptimizationDisableAllowed(): Boolean? =
        if (prefs.contains(powerOptimizationDisabledKey)) {
            prefs.getBoolean(powerOptimizationDisabledKey, false)
        }
        else {
            null
        }

    fun setDiagnosisKeysSubmitted(submitted: Boolean) {
        keysSubmitted.value = submitted
        prefs.edit().putBoolean(diagnosisKeysSubmittedKey, submitted).apply()

        if (submitted) {
            workDispatcher.cancelWorkersAfterLock()
        }
    }

     private fun updateLastExposureCheckTime() {
        lastExposureCheckTime.postValue(
            if (prefs.contains(lastExposureCheckTimeKey)) {
                val epochSec = prefs.getLong(lastExposureCheckTimeKey, 0)
                ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSec), ZoneId.systemDefault())
            }
            else {
                null
            }
        )
    }

    fun setLastExposureCheckTime(time: ZonedDateTime) {
        prefs.edit().putLong(lastExposureCheckTimeKey, time.toEpochSecond()).apply()
        updateLastExposureCheckTime()
    }

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

    private suspend fun getInitialBatchId() : BatchId
            = backendService.getInitialBatchId().current.also { setDiagnosisKeyBatchId(it) }
}