package fi.thl.koronahaavi.exposure

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.ExposureRepository

class ExposureDetailViewModel @ViewModelInject constructor(
    exposureRepository: ExposureRepository,
    appStateRepository: AppStateRepository
) : ViewModel() {
    val hasExposures = exposureRepository.flowHasExposures().asLiveData()
    val lastCheckTime = appStateRepository.lastExposureCheckTime()
}

