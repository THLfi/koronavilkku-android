package fi.thl.koronahaavi.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.jraska.livedata.test
import fi.thl.koronahaavi.common.Event
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.ExposureCount
import fi.thl.koronahaavi.data.ExposureNotification
import fi.thl.koronahaavi.data.ExposureRepository
import fi.thl.koronahaavi.device.DeviceStateRepository
import fi.thl.koronahaavi.device.SystemState
import fi.thl.koronahaavi.service.ExposureNotificationService
import fi.thl.koronahaavi.service.NotificationService
import fi.thl.koronahaavi.service.WorkDispatcher
import fi.thl.koronahaavi.service.WorkState
import fi.thl.koronahaavi.utils.MainCoroutineScopeRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.ZonedDateTime

class HomeViewModelTest {
    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()
    @get:Rule
    val testRule = InstantTaskExecutorRule()

    private lateinit var viewModel: HomeViewModel
    private lateinit var exposureRepository: ExposureRepository
    private lateinit var deviceStateRepository: DeviceStateRepository
    private lateinit var exposureNotificationService: ExposureNotificationService
    private lateinit var appStateRepository: AppStateRepository
    private lateinit var workDispatcher: WorkDispatcher
    private lateinit var notificationService: NotificationService

    val bluetoothOn = MutableLiveData<Boolean>()
    val locationOn = MutableLiveData<Boolean>()
    val enEnabledFlow = MutableStateFlow<Boolean?>(null)
    val lastCheckTime = MutableLiveData<ZonedDateTime?>()
    val notificationsEnabledFlow = MutableStateFlow<Boolean?>(true)
    val appShutdown = MutableStateFlow(false)

    @Before
    fun init() {
        exposureRepository = mockk(relaxed = true)
        deviceStateRepository = mockk(relaxed = true)
        exposureNotificationService = mockk(relaxed = true)
        appStateRepository = mockk(relaxed = true)
        workDispatcher = mockk(relaxed = true)
        notificationService = mockk(relaxed = true)

        every { deviceStateRepository.bluetoothOn() } returns bluetoothOn
        every { deviceStateRepository.locationOn() } returns locationOn
        every { exposureNotificationService.isEnabledFlow() } returns enEnabledFlow
        every { notificationService.isEnabled() } returns notificationsEnabledFlow
        every { exposureRepository.getExposureNotificationsFlow() } returns flowOf(listOf())
        every { exposureRepository.getIsExposedFlow() } returns flowOf(false)
        every { appStateRepository.getLastExposureCheckTimeLive() } returns lastCheckTime
        every { appStateRepository.appShutdown() } returns appShutdown

        viewModel = createViewModel()
    }

    @Test
    fun systemDisabledWhenAllAreNotOn() {

        for (i in 0..6) {
            enEnabledFlow.value = i and 1 > 0
            bluetoothOn.value = i and 2 > 0
            locationOn.value = i and 4 > 0
            viewModel.systemState().test().assertValue(SystemState.Off)
        }
    }

    @Test
    fun systemEnabled() {
        setSystemOn()
        viewModel.systemState().test().assertValue(SystemState.On)
    }

    @Test
    fun systemEnabledNullAtFirst() {
        viewModel.systemState().test().assertNullValue()
    }

    @Test
    fun systemStateNotificationsBlocked() {
        setSystemOn()
        notificationsEnabledFlow.value = false
        viewModel.systemState().test().assertValue(SystemState.NotificationsBlocked)
    }

    @Test
    fun systemStateAppShutdown() {
        val observer = viewModel.systemState().test()

        setSystemOn()
        appShutdown.value = true
        enEnabledFlow.value = false // this is ignored by system state because shutdown

        observer.assertValue(SystemState.On)
    }

    @Test
    fun manualCheckShownWhenOld() {
        setSystemOn()
        lastCheckTime.value = ZonedDateTime.now().minusDays(2)

        viewModel.showManualCheck().test().assertValue(true)
    }

    @Test
    fun manualCheckHiddenWhenUpToDate() {
        setSystemOn()
        lastCheckTime.value = ZonedDateTime.now().minusHours(4)

        viewModel.showManualCheck().test().assertValue(false)
    }

    @Test
    fun manualCheckShownAlwaysWhenInProgress() {
        viewModel.checkInProgress.value = true
        viewModel.showManualCheck().test().assertValue(true)
    }

    @Test
    fun manualCheckRunsWorker() {
        every { workDispatcher.runUpdateWorker() } returns MutableLiveData(Event(WorkState.InProgress))
        viewModel.startExposureCheck()
        viewModel.exposureCheckState.test().assertValue() { it.getContentIfNotHandled() == WorkState.InProgress }
    }

    @Test
    fun exposureSubLabelShown() {
        viewModel.showExposureSubLabel().test().assertValue(false)
    }

    @Test
    fun exposureSubLabelWhenPendingBlocked() {
        lastCheckTime.value = ZonedDateTime.now().minusDays(2)
        enEnabledFlow.value = false

        viewModel.showExposureSubLabel().test().assertValue(false)
    }

    @Test
    fun notificationCountNotAvailable() {
        viewModel.notificationCount.test().assertNullValue()
    }

    @Test
    fun notificationCountAvailable() {
        val notificationCreatedDate = ZonedDateTime.now()
        every { exposureRepository.getExposureNotificationsFlow() } returns flowOf(listOf(
            ExposureNotification(
                createdDate = notificationCreatedDate,
                exposureRangeStart = notificationCreatedDate.minusDays(14),
                exposureRangeEnd = notificationCreatedDate.minusDays(1),
                exposureCount = ExposureCount.ForDetailExposures(2)
            )
        ))
        viewModel = createViewModel()

        viewModel.notificationCount.test().assertValue { it == "1"}
    }

    private fun createViewModel() =
        HomeViewModel(exposureRepository, deviceStateRepository, appStateRepository, notificationService,
                exposureNotificationService, workDispatcher)

    private fun setSystemOn() {
        enEnabledFlow.value = true
        bluetoothOn.value = true
        locationOn.value = true
    }
}