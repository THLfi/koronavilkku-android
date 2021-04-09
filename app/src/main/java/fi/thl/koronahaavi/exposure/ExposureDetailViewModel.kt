package fi.thl.koronahaavi.exposure

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import fi.thl.koronahaavi.common.Event
import fi.thl.koronahaavi.data.*
import fi.thl.koronahaavi.service.ExposureNotificationService
import fi.thl.koronahaavi.service.WorkDispatcher
import fi.thl.koronahaavi.service.WorkState
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class ExposureDetailViewModel @Inject constructor(
    exposureRepository: ExposureRepository,
    appStateRepository: AppStateRepository,
    exposureNotificationService: ExposureNotificationService,
    private val workDispatcher: WorkDispatcher
) : ViewModel() {
    val notifications = exposureRepository.getExposureNotificationsFlow().asLiveData()
    val hasExposures = exposureRepository.getIsExposedFlow().asLiveData()
    val notificationCount = notifications.map { it.size }
    val showNotifications = notifications.map { it.isNotEmpty() }

    val lastCheckTime = appStateRepository.getLastExposureCheckTimeLive()

    val checkInProgress = MutableLiveData<Boolean>(false)

    private val isLocked = appStateRepository.lockedAfterDiagnosis().asLiveData()
    private val isENEnabled = exposureNotificationService.isEnabledFlow().asLiveData()
    private val exposureState = ExposureStateLiveData(hasExposures, lastCheckTime, isLocked, isENEnabled)

    private val showManualCheck = ManualCheckShownLiveData(exposureState, checkInProgress)
    fun showManualCheck(): LiveData<Boolean> = showManualCheck.distinctUntilChanged()

    private val newExposureCheckEvent = MutableLiveData<Event<Any>>()

    val exposureCheckState: LiveData<Event<WorkState>> = newExposureCheckEvent.switchMap {
        workDispatcher.runUpdateWorker()
    }

    fun startExposureCheck() {
        checkInProgress.postValue(true)
        newExposureCheckEvent.postValue(Event(Unit))
    }
}
