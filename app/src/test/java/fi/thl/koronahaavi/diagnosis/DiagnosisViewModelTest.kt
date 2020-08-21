package fi.thl.koronahaavi.diagnosis

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.jraska.livedata.test
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.service.DiagnosisKeyService
import fi.thl.koronahaavi.service.ExposureNotificationService
import fi.thl.koronahaavi.service.ExposureNotificationService.ResolvableResult.ResolutionRequired
import fi.thl.koronahaavi.service.ExposureNotificationService.ResolvableResult.Success
import fi.thl.koronahaavi.utils.MainCoroutineScopeRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DiagnosisViewModelTest {
    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()
    @get:Rule
    val testRule = InstantTaskExecutorRule()

    private lateinit var viewModel: DiagnosisViewModel
    private lateinit var appStateRepository: AppStateRepository
    private lateinit var exposureNotificationService: ExposureNotificationService

    val lockedFlow = MutableStateFlow(false)
    val enEnabledFlow = MutableStateFlow<Boolean?>(null)

    @Before
    fun init() {
        appStateRepository = mockk(relaxed = true)
        exposureNotificationService = mockk(relaxed = true)
        coEvery { appStateRepository.lockedAfterDiagnosis() } returns lockedFlow
        every { exposureNotificationService.isEnabledFlow() } returns enEnabledFlow

        viewModel = DiagnosisViewModel(appStateRepository, exposureNotificationService)
    }

    @Test
    fun showLockedENEnabled() {
        lockedFlow.value = true
        enEnabledFlow.value = true
        viewModel.showLocked.test().assertValue(true)
    }

    @Test
    fun showLockedENDisabled() {
        lockedFlow.value = true
        enEnabledFlow.value = false
        viewModel.showLocked.test().assertValue(true)
    }

    @Test
    fun showDisabled() {
        lockedFlow.value = false
        enEnabledFlow.value = false
        viewModel.showDisabled.test().assertValue(true)
    }

    @Test
    fun allowStart() {
        lockedFlow.value = false
        enEnabledFlow.value = true
        viewModel.allowStart.test().assertValue(true)
    }


}