package fi.thl.koronahaavi.onboarding

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import fi.thl.koronahaavi.device.DeviceStateRepository
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
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
