@file:Suppress("DEPRECATION")

package fi.thl.koronahaavi.exposure

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.ExposureRepository
import fi.thl.koronahaavi.data.KeyGroupToken
import fi.thl.koronahaavi.data.SettingsRepository
import fi.thl.koronahaavi.service.ExposureConfigurationData
import fi.thl.koronahaavi.service.ExposureNotificationService
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
    val lockedFlow = MutableStateFlow(false)
    val groupToken = "test_token"

    lateinit var worker: ListenableWorker

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        exposureNotificationService = mockk(relaxed = true)
        exposureRepository = mockk(relaxed = true)
        appStateRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        every { appStateRepository.lockedAfterDiagnosis() } returns lockedFlow
        every { settingsRepository.requireExposureConfiguration() } returns configuration
        coEvery { exposureNotificationService.isEnabled() } returns true

        worker = TestListenableWorkerBuilder<ExposureUpdateWorker>(context)
            .setWorkerFactory(fakeFactory)
            .setInputData(Data.Builder()
                .putString(ExposureUpdateWorker.TOKEN_KEY, groupToken)
                .build()
            )
            .build()
    }

    @Test
    fun successWhenLocked() {
        lockedFlow.value = true
        runBlocking {
            val result = worker.startWork().get()
            assertEquals(Result.success(), result)
            coVerify(exactly = 0) { exposureNotificationService.getExposureSummary(any()) }
        }
    }

    @Test
    fun successWhenNoMatches() {
        coEvery { exposureNotificationService.getExposureSummary(any()) } returns
                ExposureSummary.ExposureSummaryBuilder().setMatchedKeyCount(0).build()

        runBlocking {
            val result = worker.startWork().get()
            assertEquals(Result.success(), result)
            coVerify(exactly = 0) { settingsRepository.requireExposureConfiguration() }
        }
    }

    @Test
    fun successWhenHighRisk() {
        coEvery { exposureNotificationService.getExposureSummary(any()) } returns
                ExposureSummary.ExposureSummaryBuilder().setMatchedKeyCount(1).setMaximumRiskScore(200).build()

        coEvery { exposureNotificationService.getExposureDetails(any()) } returns listOf(
            TestData.exposure().copy(totalRiskScore = 200),
            TestData.exposure().copy(totalRiskScore = 160)
        )

        runBlocking {
            val result = worker.startWork().get()
            assertEquals(Result.success(), result)
            coVerify(exactly = 1) { exposureNotificationService.getExposureDetails(any()) }
            coVerify(exactly = 2) { exposureRepository.saveExposure(any()) }
        }
    }

    @Test
    fun keyGroupTokenUpdated() {
        coEvery { exposureNotificationService.getExposureSummary(any()) } returns
                ExposureSummary.ExposureSummaryBuilder().setMatchedKeyCount(1).setMaximumRiskScore(200).build()

        val latestExposure = TestData.exposure(Duration.ofDays(1)).copy(totalRiskScore = 200)

        coEvery { exposureNotificationService.getExposureDetails(any()) } returns listOf(
            TestData.exposure(Duration.ofDays(2)).copy(totalRiskScore = 200),
            latestExposure,
            TestData.exposure(Duration.ofDays(3)).copy(totalRiskScore = 200)
        )

        runBlocking {
            worker.startWork().get()

            val savedToken = slot<KeyGroupToken>()
            coVerify(exactly = 1) { exposureRepository.saveKeyGroupToken(capture(savedToken)) }

            assertEquals(3, savedToken.captured.exposureCount)
            assertEquals(latestExposure.detectedDate, savedToken.captured.latestExposureDate)
        }
    }

    @Test
    fun keyGroupTokenUpdatedFiltered() {
        coEvery { exposureNotificationService.getExposureSummary(any()) } returns
                ExposureSummary.ExposureSummaryBuilder()
                    .setMatchedKeyCount(2)
                    .setMaximumRiskScore(10)
                    .setAttenuationDurations(intArrayOf(30,0,0))
                    .build()

        val latestExposure = TestData.exposure(Duration.ofDays(1)).copy(totalRiskScore = 10)

        coEvery { exposureNotificationService.getExposureDetails(any()) } returns listOf(
            TestData.exposure(Duration.ofDays(2)).copy(totalRiskScore = 10),
            latestExposure,
            TestData.exposure(Duration.ofDays(3)).copy(totalRiskScore = 10)
        )

        runBlocking {
            worker.startWork().get()

            val savedToken = slot<KeyGroupToken>()
            coVerify(exactly = 1) { exposureRepository.saveKeyGroupToken(capture(savedToken)) }

            // only the latest is returned since filterd by ExposureSummaryChecker
            assertEquals(1, savedToken.captured.exposureCount)
            assertEquals(latestExposure.detectedDate, savedToken.captured.latestExposureDate)
        }
    }

    private val fakeFactory = object : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? {
            return ExposureUpdateWorker(appContext, workerParameters, exposureNotificationService,
                exposureRepository, appStateRepository, settingsRepository)
        }
    }

    private val configuration = ExposureConfigurationData(
        version = 1,
        minimumRiskScore = 100,
        attenuationScores = listOf(),
        daysSinceLastExposureScores = listOf(),
        durationScores = listOf(),
        transmissionRiskScoresAndroid = listOf(),
        durationAtAttenuationThresholds = listOf(),
        durationAtAttenuationWeights = listOf(1.0f, 0.5f, 0.0f),
        exposureRiskDuration = 15
    )
}