package fi.thl.koronahaavi.exposure

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

/**
 * True when manual exposure check function should be shown in UI
 */
class ManualCheckShownLiveData(
    private val enEnabled: LiveData<Boolean?>,
    private val exposureState: LiveData<ExposureState>,
    private val checkInProgress: LiveData<Boolean>
) : MediatorLiveData<Boolean>() {

    init {
        addSource(enEnabled) { updateValue() }
        addSource(exposureState) { updateValue() }
        addSource(checkInProgress) { updateValue() }
    }

    private fun updateValue() {
        // Always show while check is in progress, so button disappears only when check is done.
        // For example if EN is disabled while check in progress, check button should still be visible until done.
        // Button is disabled through other live data while in progress
        value = (checkInProgress.value == true) ||
                (exposureState.value is ExposureState.Pending) && (enEnabled.value == true)
    }
}

