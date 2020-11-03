package fi.thl.koronahaavi.exposure

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import java.time.ZonedDateTime

class ExposureStateLiveData(
    private val hasExposures: LiveData<Boolean>,
    private val lastCheck: LiveData<ZonedDateTime?>,
    private val isLocked: LiveData<Boolean>
) : MediatorLiveData<ExposureState?>() {

    init {
        addSource(hasExposures) { updateExposureState() }
        addSource(lastCheck) { updateExposureState() }
        addSource(isLocked) { updateExposureState() }
    }

    private fun updateExposureState() {
        value = when {
            (hasExposures.value == null) -> null  // need to know this to get state
            (hasExposures.value == true) -> ExposureState.HasExposures
            (isLastCheckOld()) -> ExposureState.Pending(lastCheck.value)
            else -> ExposureState.Clear(lastCheck.value)
        }
    }

    private fun isLastCheckOld(): Boolean {
        if (isLocked.value == true) {
            return false // when locked, exposure checks are not done so they are not pending
        }

        // null value returns false here, so its considered as ExposureState.Clear,
        // because that is the state right after onboarding until worker executed
        val limit = ZonedDateTime.now().minusDays(1)
        return lastCheck.value?.isBefore(limit) == true
    }
}

sealed class ExposureState {
    data class Clear(val lastCheck: ZonedDateTime?): ExposureState()
    object HasExposures: ExposureState()
    data class Pending(val lastCheck: ZonedDateTime?): ExposureState()
}
