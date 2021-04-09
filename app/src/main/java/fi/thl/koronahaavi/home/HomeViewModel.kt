package fi.thl.koronahaavi.home

import androidx.lifecycle.*

import androidx.lifecycle.ViewModel
import com.google.android.gms.common.api.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import fi.thl.koronahaavi.BuildConfig
import fi.thl.koronahaavi.common.Event
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.ExposureRepository
import fi.thl.koronahaavi.device.DeviceStateRepository
import fi.thl.koronahaavi.device.SystemState
import fi.thl.koronahaavi.device.SystemStateLiveData
import fi.thl.koronahaavi.exposure.ExposureState
import fi.thl.koronahaavi.exposure.ExposureStateLiveData
import fi.thl.koronahaavi.exposure.ManualCheckShownLiveData
import fi.thl.koronahaavi.service.ExposureNotificationService
import fi.thl.koronahaavi.service.ExposureNotificationService.ApiErrorResolver
import fi.thl.koronahaavi.service.ExposureNotificationService.ResolvableResult
import fi.thl.koronahaavi.service.NotificationService
import fi.thl.koronahaavi.service.WorkDispatcher
import fi.thl.koronahaavi.service.WorkState
import fi.thl.koronahaavi.settings.EnableENError
import fi.thl.koronahaavi.settings.toENApiError
import fi.thl.koronahaavi.settings.toUserEnableStep
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    exposureRepository: ExposureRepository,
    deviceStateRepository: DeviceStateRepository,
    appStateRepository: AppStateRepository,
    notificationService: NotificationService,
    private val exposureNotificationService: ExposureNotificationService,
    private val workDispatcher: WorkDispatcher
) : ViewModel() {

    val isLocked = appStateRepository.lockedAfterDiagnosis().asLiveData()

    private val enableResolutionRequiredEvent = MutableLiveData<Event<ApiErrorResolver>>()
    fun enableResolutionRequired(): LiveData<Event<ApiErrorResolver>> = enableResolutionRequiredEvent

    private val enableENErrorEvent = MutableLiveData<Event<EnableENError>>()
    fun enableErrorEvent(): LiveData<Event<EnableENError>> = enableENErrorEvent

    val checkInProgress = MutableLiveData<Boolean>(false)

    private val exposureNotifications = exposureRepository.getExposureNotificationsFlow().asLiveData()
    val hasExposures = exposureRepository.getIsExposedFlow().asLiveData()
    private val lastCheckTime = appStateRepository.getLastExposureCheckTimeLive()
    private val isENEnabled = exposureNotificationService.isEnabledFlow().asLiveData()
    private val isBluetoothOn = deviceStateRepository.bluetoothOn()
    private val isLocationOn = deviceStateRepository.locationOn()
    private val isNotificationsEnabled = notificationService.isEnabled().asLiveData()
    private val systemState = SystemStateLiveData(isENEnabled, isBluetoothOn, isLocationOn, isLocked, isNotificationsEnabled)
    private val exposureState = ExposureStateLiveData(hasExposures, lastCheckTime, isLocked, isENEnabled)
    private val showManualCheck = ManualCheckShownLiveData(exposureState, checkInProgress)
    private val newExposureCheckEvent = MutableLiveData<Event<Any>>()

    fun systemState(): LiveData<SystemState?> = systemState.distinctUntilChanged()
    fun currentSystemState(): SystemState? = systemState.value
    fun exposureState(): LiveData<ExposureState?> = exposureState.distinctUntilChanged()
    fun showManualCheck(): LiveData<Boolean> = showManualCheck.distinctUntilChanged()

    fun showExposureSubLabel(): LiveData<Boolean> = exposureState.map {
        when (it) {
            is ExposureState.Clear.Disabled, is ExposureState.Clear.Updated -> false
            else -> true
        }
    }

    fun showNotificationGuide(): LiveData<Boolean> = exposureState.map { it is ExposureState.Clear.Updated }

    val notificationCount: LiveData<String?> = exposureNotifications.map {
        if (it.isEmpty()) null else it.size.toString()
    }

    val exposureCheckState: LiveData<Event<WorkState>> = newExposureCheckEvent.switchMap {
        workDispatcher.runUpdateWorker()
    }

    val showTestButton = BuildConfig.ENABLE_TEST_UI


    fun enableSystem() {
        viewModelScope.launch {
            // we cannot directly enable bluetooth/location, but disabling/enabling EN will also enable
            // bluetooth/location.. this has no effect if EN was already disabled
            if (isBluetoothOn.value == false || isLocationOn.value == false) {
                exposureNotificationService.disable()
            }

            when (val result = exposureNotificationService.enable()) {
                is ResolvableResult.ResolutionRequired -> {
                    // fragment needs to trigger the flow to resolve request
                    enableResolutionRequiredEvent.postValue(Event(result.errorResolver))
                }
                is ResolvableResult.Failed -> {
                    enableENErrorEvent.postValue(Event(
                        EnableENError.Failed(code = result.apiErrorCode, connectionErrorCode = result.connectionErrorCode)
                    ))
                }
                is ResolvableResult.MissingCapability -> {
                    enableENErrorEvent.postValue(Event(
                        EnableENError.MissingCapability
                    ))
                }
                is ResolvableResult.ApiNotSupported -> {
                    enableENErrorEvent.postValue(Event(
                        EnableENError.ApiNotSupported(result.connectionError?.toENApiError())
                    ))
                }
                is ResolvableResult.HmsCanceled -> {
                    enableENErrorEvent.postValue(Event(
                        EnableENError.UserCanceled(
                            result.step.toUserEnableStep(enableENErrorEvent)
                        )
                    ))
                }
            }
        }
    }

    fun startExposureCheck() {
        checkInProgress.postValue(true)
        newExposureCheckEvent.postValue(Event(Unit))  // triggers switchmap
    }
}
