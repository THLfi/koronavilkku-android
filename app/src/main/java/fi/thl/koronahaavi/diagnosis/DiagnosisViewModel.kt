package fi.thl.koronahaavi.diagnosis

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.service.ExposureNotificationService
import javax.inject.Inject

@HiltViewModel
class DiagnosisViewModel @Inject constructor(
    appStateRepository: AppStateRepository,
    exposureNotificationService: ExposureNotificationService
) : ViewModel() {

    private val enEnabledFlow = exposureNotificationService.isEnabledFlow().asLiveData()

    val showLocked: LiveData<Boolean> = appStateRepository.lockedAfterDiagnosis().asLiveData()

    val showDisabled = MediatorLiveData<Boolean>().apply {
        addSource(showLocked) { updateShowDisabled() }
        addSource(enEnabledFlow) { updateShowDisabled() }
    }

    val allowStart = MediatorLiveData<Boolean>().apply {
        addSource(showLocked) { updateAllowStart() }
        addSource(showDisabled) { updateAllowStart() }
    }

    private fun updateShowDisabled() {
        // locked has priority over disabled
        showDisabled.value = showLocked.value == false && enEnabledFlow.value == false
    }

    private fun updateAllowStart() {
        allowStart.value = showLocked.value == false && showDisabled.value == false
    }
}