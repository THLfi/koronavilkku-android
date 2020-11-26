package fi.thl.koronahaavi.diagnosis

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.jraska.livedata.test
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.SettingsRepository
import fi.thl.koronahaavi.service.DiagnosisKeyService
import fi.thl.koronahaavi.service.ExposureNotificationService
import fi.thl.koronahaavi.service.ExposureNotificationService.ResolvableResult.ResolutionRequired
import fi.thl.koronahaavi.service.ExposureNotificationService.ResolvableResult.Success
import fi.thl.koronahaavi.service.SendKeysResult
import fi.thl.koronahaavi.service.WorkDispatcher
import fi.thl.koronahaavi.utils.MainCoroutineScopeRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CodeEntryViewModelTest {
    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()
    @get:Rule
    val testRule = InstantTaskExecutorRule()

    private lateinit var viewModel: CodeEntryViewModel
    private lateinit var diagnosisKeyService: DiagnosisKeyService
    private lateinit var exposureNotificationService: ExposureNotificationService
    private lateinit var appStateRepository: AppStateRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var workDispatcher: WorkDispatcher

    val enEnabledFlow = MutableStateFlow(false)

    @Before
    fun init() {
        diagnosisKeyService = mockk(relaxed = true)
        exposureNotificationService = mockk(relaxed = true)
        appStateRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        workDispatcher = mockk(relaxed = true)

        coEvery { exposureNotificationService.isEnabledFlow() } returns enEnabledFlow
        coEvery { exposureNotificationService.getTemporaryExposureKeys() } returns Success(listOf())
        coEvery { diagnosisKeyService.sendExposureKeys(any(), any(), any(), any()) } returns SendKeysResult.Success

        viewModel = CodeEntryViewModel(exposureNotificationService, diagnosisKeyService, appStateRepository, workDispatcher, settingsRepository)
    }

    @Test
    fun submitAllowed() {
        enEnabledFlow.value = true
        viewModel.code.postValue("1234")
        viewModel.submitAllowed().test().assertValue(true)
    }

    @Test
    fun submitNotAllowedWhenENDisabled() {
        enEnabledFlow.value = false
        viewModel.code.postValue("1234")
        viewModel.submitAllowed().test().assertValue(false)
    }

    @Test
    fun submitNotAllowedWhenNoCode() {
        enEnabledFlow.value = false
        viewModel.submitAllowed().test().assertValue(false)
    }

    @Test
    fun resolutionOnSubmit() {
        coEvery { exposureNotificationService.getTemporaryExposureKeys() } returns
                ResolutionRequired(Status(CommonStatusCodes.RESOLUTION_REQUIRED))

        runBlocking {
            viewModel.code.postValue("1234")
            viewModel.submit()

            viewModel.keyHistoryResolutionEvent().test().assertHasValue()
            coVerify(exactly = 0) { diagnosisKeyService.sendExposureKeys(any(), any(), any(), any()) }
        }
    }

    @Test
    fun normalSubmit() {
        runBlocking {
            viewModel.code.postValue("1234")
            viewModel.submit()

            coVerify { diagnosisKeyService.sendExposureKeys(any(), any(), any(), any()) }
            coVerify { appStateRepository.setDiagnosisKeysSubmitted(true) }
        }
    }

}