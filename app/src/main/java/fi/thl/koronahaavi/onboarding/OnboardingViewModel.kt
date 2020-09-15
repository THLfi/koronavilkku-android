package fi.thl.koronahaavi.onboarding

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import fi.thl.koronahaavi.device.DeviceStateRepository

class OnboardingViewModel @ViewModelInject constructor(
    deviceStateRepository: DeviceStateRepository
) : ViewModel() {

    val voluntaryActivationChecked = MutableLiveData<Boolean>(false)
    val termsAcceptedChecked = MutableLiveData<Boolean>(false)
    val enableInProgress = MutableLiveData<Boolean>(false)

    private val enableAllowed = MediatorLiveData<Boolean>().apply {
        addSource(voluntaryActivationChecked) { updateEnableAllowed() }
        addSource(termsAcceptedChecked) { updateEnableAllowed() }
        addSource(enableInProgress) { updateEnableAllowed() }
    }
    fun enableAllowed(): LiveData<Boolean> = enableAllowed.distinctUntilChanged()

    private fun updateEnableAllowed() {
        enableAllowed.value =
            voluntaryActivationChecked.value==true
            && termsAcceptedChecked.value==true
            && enableInProgress.value==false
    }

    val isLocationOn = deviceStateRepository.locationOn()
}
