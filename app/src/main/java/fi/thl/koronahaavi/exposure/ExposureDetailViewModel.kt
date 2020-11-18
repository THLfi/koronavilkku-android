package fi.thl.koronahaavi.exposure

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import fi.thl.koronahaavi.common.Event
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.ExposureNotification
import fi.thl.koronahaavi.data.ExposureRepository
import fi.thl.koronahaavi.data.SettingsRepository
import fi.thl.koronahaavi.service.ExposureNotificationService
import fi.thl.koronahaavi.service.WorkDispatcher
import fi.thl.koronahaavi.service.WorkState
import java.time.ZonedDateTime

class ExposureDetailViewModel @ViewModelInject constructor(
    exposureRepository: ExposureRepository,
    appStateRepository: AppStateRepository,
    exposureNotificationService: ExposureNotificationService,
    private val workDispatcher: WorkDispatcher,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val exposureNotifications = exposureRepository.getExposureNotificationsFlow().asLiveData()
    val hasExposures = exposureRepository.getIsExposedFlow().asLiveData()
    val notificationCount = exposureNotifications.map { it.size }
    val showNotifications = exposureNotifications.map { it.isNotEmpty() }

    val notifications = exposureNotifications.map {
        it.map(this::createNotificationData)
    }

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

    private fun createNotificationData(notification: ExposureNotification): NotificationData {
        val rangeDays = settingsRepository.appConfiguration.exposureValidDays.toLong()

        return NotificationData(
            dateTime = notification.createdDate,
            exposureRangeStart = notification.createdDate.minusDays(rangeDays),
            exposureRangeEnd = notification.createdDate.minusDays(1),
            exposureCount = notification.exposureCount
        )
    }
}

data class NotificationData(
    val dateTime: ZonedDateTime,
    val exposureRangeStart: ZonedDateTime,
    val exposureRangeEnd: ZonedDateTime,
    val exposureCount: Int
)
