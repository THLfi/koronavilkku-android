package fi.thl.koronahaavi.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.common.internal.constants.ListAppsActivityContract
import com.jraska.livedata.test
import fi.thl.koronahaavi.common.Event
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.ExposureRepository
import fi.thl.koronahaavi.device.DeviceStateRepository
import fi.thl.koronahaavi.device.SystemState
import fi.thl.koronahaavi.service.ExposureNotificationService
import fi.thl.koronahaavi.service.WorkDispatcher
import fi.thl.koronahaavi.service.WorkState
import fi.thl.koronahaavi.utils.MainCoroutineScopeRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
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

    val bluetoothOn = MutableLiveData<Boolean>()
    val locationOn = MutableLiveData<Boolean>()
    val enEnabledFlow = MutableStateFlow<Boolean?>(null)
    val lastCheckTime = MutableLiveData<ZonedDateTime?>()

    @Before
    fun init() {
        exposureRepository = mockk(relaxed = true)
        deviceStateRepository = mockk(relaxed = true)
        exposureNotificationService = mockk(relaxed = true)
        appStateRepository = mockk(relaxed = true)
        workDispatcher = mockk(relaxed = true)

        every { deviceStateRepository.bluetoothOn() } returns bluetoothOn
        every { deviceStateRepository.locationOn() } returns locationOn
        every { exposureNotificationService.isEnabledFlow() } returns enEnabledFlow
        every { exposureRepository.flowExposureNotifications() } returns flowOf(listOf())
        every { appStateRepository.getLastExposureCheckTimeLive() } returns lastCheckTime

        viewModel = HomeViewModel(exposureRepository, deviceStateRepository, appStateRepository, exposureNotificationService, workDispatcher)
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
        viewModel.hideExposureSubLabel().test().assertValue(false)
    }

    @Test
    fun exposureSubLabelWhenPendingBlocked() {
        lastCheckTime.value = ZonedDateTime.now().minusDays(2)
        enEnabledFlow.value = false

        viewModel.hideExposureSubLabel().test().assertValue(true)
    }

    private fun setSystemOn() {
        enEnabledFlow.value = true
        bluetoothOn.value = true
        locationOn.value = true
    }
}