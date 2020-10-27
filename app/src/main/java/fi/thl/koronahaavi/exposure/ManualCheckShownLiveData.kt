package fi.thl.koronahaavi.exposure

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import fi.thl.koronahaavi.device.SystemState

/**
 * True when manual exposure check function should be shown in UI
 */
class ManualCheckShownLiveData(
    private val systemState: LiveData<SystemState?>,
    private val exposureState: LiveData<ExposureState>,
    private val checkInProgress: LiveData<Boolean>
) : MediatorLiveData<Boolean>() {

    init {
        addSource(systemState) { updateValue() }
        addSource(exposureState) { updateValue() }
        addSource(checkInProgress) { updateValue() }
    }

    private fun updateValue() {
        // Always show while check is in progress, so button disappears only when check is done
        // Button is disabled through other live data while in progress
        value = (checkInProgress.value == true) ||
                (exposureState.value is ExposureState.Pending) && (systemState.value == SystemState.On)
    }
}

