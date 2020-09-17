package fi.thl.koronahaavi.home

import androidx.lifecycle.*

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import com.google.android.gms.common.api.Status
import fi.thl.koronahaavi.BuildConfig
import fi.thl.koronahaavi.common.Event
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.ExposureRepository
import fi.thl.koronahaavi.device.DeviceStateRepository
import fi.thl.koronahaavi.device.SystemState
import fi.thl.koronahaavi.device.SystemStateLiveData
import fi.thl.koronahaavi.service.ExposureNotificationService
import fi.thl.koronahaavi.service.ExposureNotificationService.ResolvableResult
import fi.thl.koronahaavi.settings.EnableENError
import fi.thl.koronahaavi.settings.toENApiError
import kotlinx.coroutines.launch

class HomeViewModel @ViewModelInject constructor(
    exposureRepository: ExposureRepository,
    deviceStateRepository: DeviceStateRepository,
    appStateRepository: AppStateRepository,
    private val exposureNotificationService: ExposureNotificationService
) : ViewModel() {

    private val isLocked = appStateRepository.lockedAfterDiagnosis().asLiveData()

    private val enableResolutionRequiredEvent = MutableLiveData<Event<Status>>()
    fun enableResolutionRequired(): LiveData<Event<Status>> = enableResolutionRequiredEvent

    private val enableENErrorEvent = MutableLiveData<Event<EnableENError>>()
    fun enableErrorEvent(): LiveData<Event<EnableENError>> = enableENErrorEvent

    val hasExposures = exposureRepository.flowHasExposures().asLiveData()

    private val isENEnabled = exposureNotificationService.isEnabledFlow().asLiveData()
    private val isBluetoothOn = deviceStateRepository.bluetoothOn()
    private val isLocationOn = deviceStateRepository.locationOn()

    private val systemState = SystemStateLiveData(isENEnabled, isBluetoothOn, isLocationOn, isLocked)
    fun systemState(): LiveData<SystemState?> = systemState.distinctUntilChanged()

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
                    enableResolutionRequiredEvent.postValue(Event(result.status))
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
            }
        }
    }
}