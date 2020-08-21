package fi.thl.koronahaavi.settings

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.device.DeviceStateRepository
import fi.thl.koronahaavi.device.SystemState
import fi.thl.koronahaavi.device.SystemStateLiveData
import fi.thl.koronahaavi.service.ExposureNotificationService

class SettingsViewModel @ViewModelInject constructor(
    exposureNotificationService: ExposureNotificationService,
    appStateRepository: AppStateRepository,
    deviceStateRepository: DeviceStateRepository
) : ViewModel() {

    private val isENEnabled = exposureNotificationService.isEnabledFlow().asLiveData()
    private val isAppLocked = appStateRepository.lockedAfterDiagnosis().asLiveData()
    private val isBluetoothOn = deviceStateRepository.bluetoothOn()
    private val isLocationOn = deviceStateRepository.locationOn()

    private val systemState = SystemStateLiveData(isENEnabled, isBluetoothOn, isLocationOn,  isAppLocked)

    fun systemState(): LiveData<SystemState?> = systemState.distinctUntilChanged()
}