package fi.thl.koronahaavi.device

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.service.ExposureNotificationService
import fi.thl.koronahaavi.service.NotificationService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemStateProvider @Inject constructor(
    exposureNotificationService: ExposureNotificationService,
    appStateRepository: AppStateRepository,
    deviceStateRepository: DeviceStateRepository,
    notificationService: NotificationService
) {
    private val isENEnabled = exposureNotificationService.isEnabledFlow().asLiveData()
    private val isAppLocked = appStateRepository.lockedAfterDiagnosis().asLiveData()
    private val isBluetoothOn = deviceStateRepository.bluetoothOn()
    private val isLocationOn = deviceStateRepository.locationOn()
    private val isNotificationsEnabled = notificationService.isEnabled().asLiveData()

    private val systemState = SystemStateLiveData(
        isENEnabled,
        isBluetoothOn,
        isLocationOn,
        isAppLocked,
        isNotificationsEnabled
    )

    fun systemState(): LiveData<SystemState?> = systemState.distinctUntilChanged()
}