package fi.thl.koronahaavi.settings

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.common.api.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import fi.thl.koronahaavi.common.Event
import fi.thl.koronahaavi.device.DeviceStateRepository
import fi.thl.koronahaavi.service.ExposureNotificationService
import fi.thl.koronahaavi.service.ExposureNotificationService.*
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EnableSystemViewModel @Inject constructor(
    private val exposureNotificationService: ExposureNotificationService,
    deviceStateRepository: DeviceStateRepository
) : ViewModel() {

    private val enableResolutionRequiredEvent = MutableLiveData<Event<ApiErrorResolver>>()
    fun enableResolutionRequired(): LiveData<Event<ApiErrorResolver>> = enableResolutionRequiredEvent

    private val enableENErrorEvent = MutableLiveData<Event<EnableENError>>()
    fun enableErrorEvent(): LiveData<Event<EnableENError>> = enableENErrorEvent

    private val isBluetoothOn = deviceStateRepository.bluetoothOn()
    private val isLocationOn = deviceStateRepository.locationOn()
    private val requiredServices = MediatorLiveData<Boolean>().apply {
        addSource(isBluetoothOn) { updateRequiredServices() }
        addSource(isLocationOn) { updateRequiredServices() }
    }

    private fun updateRequiredServices() {
        requiredServices.value = isBluetoothOn.value==true && isLocationOn.value==true
    }

    fun getSystemAvailability(): ExposureNotificationService.AvailabilityResolver =
        exposureNotificationService.getAvailabilityResolver()

    suspend fun enableSystem(): Boolean {
        // we cannot directly enable bluetooth/location, but disabling/enabling EN will also enable
        // bluetooth/location.. this has no effect if EN was already disabled
        if (requiredServices.value != true) {
            exposureNotificationService.disable()
        }

        return when (val result = exposureNotificationService.enable()) {
            is ResolvableResult.Success -> true
            is ResolvableResult.ResolutionRequired -> {
                enableResolutionRequiredEvent.postValue(Event(result.errorResolver))
                false
            }
            is ResolvableResult.Failed -> {
                enableENErrorEvent.postValue(Event(
                    EnableENError.Failed(
                        code = result.apiErrorCode,
                        connectionErrorCode = result.connectionErrorCode
                    )
                ))
                false
            }
            is ResolvableResult.MissingCapability -> {
                enableENErrorEvent.postValue(Event(
                    EnableENError.MissingCapability
                ))
                false
            }
            is ResolvableResult.ApiNotSupported -> {
                enableENErrorEvent.postValue(Event(
                    EnableENError.ApiNotSupported(result.connectionError?.toENApiError())
                ))
                false
            }
            is ResolvableResult.HmsCanceled -> {
                Timber.d("got ResolvableResult.Canceled")
                enableENErrorEvent.postValue(Event(
                    EnableENError.UserCanceled(
                        result.step.toUserEnableStep(enableENErrorEvent)
                    )
                ))
                false
            }
        }
    }

    suspend fun disableSystem() {
        exposureNotificationService.disable()
    }
}

fun ConnectionError.toENApiError(): ENApiError =
    when (this) {
        is ConnectionError.DeviceNotSupported -> ENApiError.DeviceNotSupported
        is ConnectionError.UserIsNotOwner -> ENApiError.UserIsNotOwner
        is ConnectionError.ClientNotAuthorized -> ENApiError.AppNotAuthorized
        is ConnectionError.Failed -> ENApiError.Failed(errorCode)
    }

sealed class EnableENError() {
    data class UserCanceled(val step: UserEnableStep) : EnableENError()
    object MissingCapability : EnableENError()
    data class ApiNotSupported(val enApiError: ENApiError? = null): EnableENError()
    data class Failed(val code: Int? = null, val connectionErrorCode: Int? = null, val error: String? = null): EnableENError()
}

sealed class ENApiError {
    object DeviceNotSupported: ENApiError()
    object UserIsNotOwner: ENApiError()
    object AppNotAuthorized: ENApiError()
    data class Failed(val errorCode: Int?): ENApiError()
}

sealed class UserEnableStep() {
    object UserConsent: UserEnableStep()
    object LocationPermission: UserEnableStep()
    data class UpdateRequired(val retry: suspend (activity: Activity) -> Boolean): UserEnableStep()
}

fun EnableStep.toUserEnableStep(errorEvent: MutableLiveData<Event<EnableENError>>): UserEnableStep =
    when(this) {
        is EnableStep.UserConsent -> UserEnableStep.UserConsent
        is EnableStep.LocationPermission -> UserEnableStep.LocationPermission
        is EnableStep.UpdateRequired -> UserEnableStep.UpdateRequired {
            when (retry(it)) {
                is ResolvableResult.Success -> true
                else -> {
                    Timber.d("retry with activity failed")
                    errorEvent.postValue(Event(
                        EnableENError.UserCanceled(UserEnableStep.UserConsent)
                    ))
                    false
                }
            }
        }
    }
