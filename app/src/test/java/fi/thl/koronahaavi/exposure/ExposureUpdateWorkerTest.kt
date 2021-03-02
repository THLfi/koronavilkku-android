
package fi.thl.koronahaavi.exposure

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import fi.thl.koronahaavi.data.*
import fi.thl.koronahaavi.service.ExposureNotificationService
import fi.thl.koronahaavi.service.NotificationService
import fi.thl.koronahaavi.utils.TestData
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class ExposureUpdateWorkerTest {
    private lateinit var context: Context

    private lateinit var exposureNotificationService: ExposureNotificationService
    private lateinit var exposureRepository: ExposureRepository
    private lateinit var appStateRepository: AppStateRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var notificationService: NotificationService
    val lockedFlow = MutableStateFlow(false)

    lateinit var worker: ListenableWorker

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        exposureNotificationService = mockk(relaxed = true)
        exposureRepository = mockk(relaxed = true)
        appStateRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        notificationService = mockk(relaxed = true)

        every { appStateRepository.lockedAfterDiagnosis() } returns lockedFlow
        every { settingsRepository.requireExposureConfiguration() } returns TestData.exposureConfiguration()
        coEvery { exposureRepository.getAllExposures() } returns listOf()
        coEvery { exposureNotificationService.isEnabled() } returns true

        worker = TestListenableWorkerBuilder<ExposureUpdateWorker>(context)
            .setWorkerFactory(fakeFactory)
            .build()
    }

    @Test
    fun successWhenLocked() {
        lockedFlow.value = true
        runBlocking {
            val result = worker.startWork().get()
            assertEquals(Result.success(), result)
            coVerify(exactly = 0) { exposureNotificationService.getDailyExposures(any()) }
        }
    }

    @Test
    fun newExposureNoPrevious() {
        val exposure = TestData.dailyExposure()
        coEvery { exposureNotificationService.getDailyExposures(any()) } returns listOf(exposure)

        runBlocking {
            val result = worker.startWork().get()
            assertEquals(Result.success(), result)

            val savedExposure = slot<Exposure>()
            coVerify(exactly = 1) { exposureRepository.saveExposure(capture(savedExposure)) }
            assertEquals(exposure.day, savedExposure.captured.detectedDate.toLocalDate())

            val savedToken = slot<KeyGroupToken>()
            coVerify(exactly = 1) { exposureRepository.saveKeyGroupToken(capture(savedToken)) }
            assertEquals(1, savedToken.captured.exposureCount)

            coVerify(exactly = 1) { notificationService.notifyExposure() }
        }
    }

    @Test
    fun newExposureOnEarlierDay() {
        coEvery { exposureRepository.getAllExposures() } returns listOf(
                TestData.exposure(age = Duration.ZERO, dayOffset = 3).copy(totalRiskScore = 900)
        )

        coEvery { exposureNotificationService.getDailyExposures(any()) } returns listOf(
                TestData.dailyExposure(dayOffset = 3),
                TestData.dailyExposure(dayOffset = 4) // new
        )

        runBlocking {
            val result = worker.startWork().get()
            assertEquals(Result.success(), result)

            coVerify(exactly = 1) { exposureRepository.saveExposure(any()) }
            coVerify(exactly = 1) { notificationService.notifyExposure() }
        }
    }

    @Test
    fun newExposureOnLaterDay() {
        coEvery { exposureRepository.getAllExposures() } returns listOf(
                TestData.exposure(age = Duration.ZERO, dayOffset = 3).copy(totalRiskScore = 900)
        )

        coEvery { exposureNotificationService.getDailyExposures(any()) } returns listOf(
                TestData.dailyExposure(dayOffset = 3),
                TestData.dailyExposure(dayOffset = 2) // new
        )

        runBlocking {
            val result = worker.startWork().get()
            assertEquals(Result.success(), result)

            coVerify(exactly = 1) { exposureRepository.saveExposure(any()) }
            coVerify(exactly = 1) { notificationService.notifyExposure() }
        }
    }

    @Test
    fun lowExposure() {
        coEvery { exposureNotificationService.getDailyExposures(any()) } returns listOf(
            TestData.dailyExposure().copy(score = 10)
        )

        runBlocking {
            val result = worker.startWork().get()
            assertEquals(Result.success(), result)
            coVerify(exactly = 0) { exposureRepository.saveExposure(any()) }
            coVerify(exactly = 0) { notificationService.notifyExposure() }
        }
    }

    @Test
    fun increasedExposureForExisting() {
        coEvery { exposureRepository.getAllExposures() } returns listOf(
                TestData.exposure(age = Duration.ZERO, dayOffset = 3).copy(totalRiskScore = 900)
        )

        coEvery { exposureNotificationService.getDailyExposures(any()) } returns listOf(
                TestData.dailyExposure(dayOffset = 3).copy(score = 1800)
        )

        runBlocking {
            val result = worker.startWork().get()
            assertEquals(Result.success(), result)
            coVerify(exactly = 0) { exposureRepository.saveExposure(any()) }
            coVerify(exactly = 0) { notificationService.notifyExposure() }
        }
    }

    private val fakeFactory = object : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? {
            return ExposureUpdateWorker(appContext, workerParameters, exposureNotificationService,
                exposureRepository, appStateRepository, settingsRepository, notificationService)
        }
    }
}