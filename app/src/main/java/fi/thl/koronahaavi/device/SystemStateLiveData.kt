package fi.thl.koronahaavi.device

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

class SystemStateLiveData (
    private val isENEnabled: LiveData<Boolean?>,
    private val isBluetoothOn: LiveData<Boolean>,
    private val isLocationOn: LiveData<Boolean>,
    private val isLocked: LiveData<Boolean>,
    private val isNotificationsEnabled: LiveData<Boolean?>,
    private val appShutdown: LiveData<Boolean>
) : MediatorLiveData<SystemState?>() {

    init {
        addSource(isENEnabled) { updateSystemState() }
        addSource(isBluetoothOn) { updateSystemState() }
        addSource(isLocationOn) { updateSystemState() }
        addSource(isLocked) { updateSystemState() }
        addSource(isNotificationsEnabled) { updateSystemState() }
        addSource(appShutdown) { }
    }

    private fun updateSystemState() {
        // stop updates when shutdown in progress to avoid UI flicker when EN is disabled
        if (appShutdown.value != true) {
            value = when {
                (isLocked.value == true) -> SystemState.Locked
                (isENEnabled.value == false || isBluetoothOn.value == false || isLocationOn.value == false) -> SystemState.Off
                (isSystemOn() && isNotificationsEnabled.value == false) -> SystemState.NotificationsBlocked
                (isSystemOn()) -> SystemState.On
                else -> null
            }
        }
    }

    private fun isSystemOn() = (isENEnabled.value == true && isBluetoothOn.value == true && isLocationOn.value == true)
}

sealed class SystemState {
    object On: SystemState()
    object Off: SystemState()
    object Locked: SystemState()
    object NotificationsBlocked: SystemState()
}
