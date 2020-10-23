package fi.thl.koronahaavi.exposure

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.ExposureRepository
import fi.thl.koronahaavi.device.SystemStateProvider
import fi.thl.koronahaavi.service.WorkDispatcher
import fi.thl.koronahaavi.service.WorkState

class ExposureDetailViewModel @ViewModelInject constructor(
    exposureRepository: ExposureRepository,
    appStateRepository: AppStateRepository,
    systemStateProvider: SystemStateProvider,
    private val workDispatcher: WorkDispatcher
) : ViewModel() {
    val hasExposures = exposureRepository.flowHasExposures().asLiveData()
    val lastCheckTime = appStateRepository.lastExposureCheckTime()

    val checkInProgress = MutableLiveData<Boolean>(false)

    private val exposureState = ExposureStateLiveData(hasExposures, lastCheckTime)
    private val systemState = systemStateProvider.systemState()

    private val showManualCheck = ManualCheckAllowedLiveData(systemState, exposureState, checkInProgress)
    fun showManualCheck(): LiveData<Boolean> = showManualCheck.distinctUntilChanged()

    fun startExposureCheck(): LiveData<WorkState> {
        checkInProgress.postValue(true)
        return workDispatcher.runUpdateWorker()
    }
}

