package fi.thl.koronahaavi.exposure

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import fi.thl.koronahaavi.device.SystemState

class ManualCheckAllowedLiveData(
    private val systemState: LiveData<SystemState?>,
    private val exposureState: LiveData<ExposureState>,
    private val checkInProgress: LiveData<Boolean>
) : MediatorLiveData<Boolean>() {

    init {
        addSource(systemState) { updateManualCheckAllowed() }
        addSource(exposureState) { updateManualCheckAllowed() }
        addSource(checkInProgress) { updateManualCheckAllowed() }
    }

    private fun updateManualCheckAllowed() {
        // sync showManualCheck value with checkInProgress so that button and progress indicator disappear at the same time
        value = (checkInProgress.value == true) ||
                (exposureState.value is ExposureState.Pending) && (systemState.value == SystemState.On)
    }
}

