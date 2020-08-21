package fi.thl.koronahaavi.diagnosis

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.service.ExposureNotificationService

class DiagnosisViewModel @ViewModelInject constructor(
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