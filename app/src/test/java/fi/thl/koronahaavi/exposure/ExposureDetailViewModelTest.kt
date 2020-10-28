package fi.thl.koronahaavi.exposure

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.jraska.livedata.test
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.ExposureNotification
import fi.thl.koronahaavi.data.ExposureRepository
import fi.thl.koronahaavi.service.ExposureNotificationService
import fi.thl.koronahaavi.service.WorkDispatcher
import fi.thl.koronahaavi.utils.MainCoroutineScopeRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.ZonedDateTime

class ExposureDetailViewModelTest {
    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()
    @get:Rule
    val testRule = InstantTaskExecutorRule()

    private lateinit var viewModel: ExposureDetailViewModel
    private lateinit var exposureRepository: ExposureRepository
    private lateinit var appStateRepository: AppStateRepository
    private lateinit var exposureNotificationService: ExposureNotificationService
    private lateinit var workDispatcher: WorkDispatcher

    @Before
    fun init() {
        exposureRepository = mockk(relaxed = true)
        appStateRepository = mockk(relaxed = true)
        exposureNotificationService = mockk(relaxed = true)
        workDispatcher = mockk(relaxed = true)
        every { exposureRepository.flowExposureNotifications() } returns flowOf(listOf(
            ExposureNotification(ZonedDateTime.now(), listOf())
        ))

        viewModel = ExposureDetailViewModel(exposureRepository, appStateRepository, exposureNotificationService, workDispatcher)
    }

    @Test
    fun test() {
        viewModel.hasExposures.test().assertValue(true)
    }
}