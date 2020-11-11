package fi.thl.koronahaavi.exposure

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import java.time.ZonedDateTime

class ExposureStateLiveData(
    private val hasExposures: LiveData<Boolean>,
    private val lastCheck: LiveData<ZonedDateTime?>,
    private val isLocked: LiveData<Boolean>,
    private val enEnabled: LiveData<Boolean?>
) : MediatorLiveData<ExposureState?>() {

    init {
        addSource(hasExposures) { updateExposureState() }
        addSource(lastCheck) { updateExposureState() }
        addSource(isLocked) { updateExposureState() }
        addSource(enEnabled) { updateExposureState() }
    }

    private fun updateExposureState() {
        value = when {
            (hasExposures.value == null) -> null  // need to know this to get state
            (hasExposures.value == true) -> ExposureState.HasExposures
            (enEnabled.value == false) -> ExposureState.Clear.Disabled(lastCheck.value)
            (isLastCheckOld()) -> ExposureState.Clear.Pending(lastCheck.value)
            else -> ExposureState.Clear.Updated(lastCheck.value)
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
    object HasExposures: ExposureState()

    // Clear sub-states represent situations where there are no known exposures but for example
    // updates have been delayed
    sealed class Clear(open val lastCheck: ZonedDateTime?): ExposureState() {
        data class Updated(override val lastCheck: ZonedDateTime?): ExposureState.Clear(lastCheck)
        data class Pending(override val lastCheck: ZonedDateTime?): ExposureState.Clear(lastCheck)
        data class Disabled(override val lastCheck: ZonedDateTime?): ExposureState.Clear(lastCheck)
    }
}
