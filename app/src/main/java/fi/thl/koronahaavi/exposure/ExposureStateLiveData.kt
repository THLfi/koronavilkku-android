package fi.thl.koronahaavi.exposure

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import java.time.ZonedDateTime

class ExposureStateLiveData(
    private val hasExposures: LiveData<Boolean>,
    private val lastCheck: LiveData<ZonedDateTime?>
) : MediatorLiveData<ExposureState>() {

    init {
        addSource(hasExposures) { updateExposureState( )}
        addSource(lastCheck) { updateExposureState() }
    }

    private fun updateExposureState() {
        value = when {
            (hasExposures.value == true) -> ExposureState.HasExposures
            (isLastCheckOld()) -> ExposureState.Pending(lastCheck.value)
            else -> ExposureState.Clear(lastCheck.value)
        }
    }

    private fun isLastCheckOld(): Boolean {
        // null value returns false here, so its considered as ExposureState.Clear,
        // because thats the state right after onboarding until worker executed
        val limit = ZonedDateTime.now().minusHours(8)
        return lastCheck.value?.isBefore(limit) == true
    }
}

sealed class ExposureState {
    data class Clear(val lastCheck: ZonedDateTime?): ExposureState()
    object HasExposures: ExposureState()
    data class Pending(val lastCheck: ZonedDateTime?): ExposureState()
}
