package fi.thl.koronahaavi.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.common.internal.constants.ListAppsActivityContract
import com.jraska.livedata.test
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.ExposureRepository
import fi.thl.koronahaavi.device.DeviceStateRepository
import fi.thl.koronahaavi.device.SystemState
import fi.thl.koronahaavi.service.ExposureNotificationService
import fi.thl.koronahaavi.utils.MainCoroutineScopeRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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

    val bluetoothOn = MutableLiveData<Boolean>()
    val locationOn = MutableLiveData<Boolean>()
    val enEnabledFlow = MutableStateFlow<Boolean?>(null)

    @Before
    fun init() {
        exposureRepository = mockk(relaxed = true)
        deviceStateRepository = mockk(relaxed = true)
        exposureNotificationService = mockk(relaxed = true)
        appStateRepository = mockk(relaxed = true)

        every { deviceStateRepository.bluetoothOn() } returns bluetoothOn
        every { deviceStateRepository.locationOn() } returns locationOn
        every { exposureNotificationService.isEnabledFlow() } returns enEnabledFlow

        viewModel = HomeViewModel(exposureRepository, deviceStateRepository, appStateRepository, exposureNotificationService)
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
        enEnabledFlow.value = true
        bluetoothOn.value = true
        locationOn.value = true
        viewModel.systemState().test().assertValue(SystemState.On)
    }

    @Test
    fun systemEnabledNullAtFirst() {
        viewModel.systemState().test().assertNullValue()
    }
}