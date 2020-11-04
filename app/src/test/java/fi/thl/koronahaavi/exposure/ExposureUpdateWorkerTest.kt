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
import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.ExposureRepository
import fi.thl.koronahaavi.data.SettingsRepository
import fi.thl.koronahaavi.service.ExposureConfigurationData
import fi.thl.koronahaavi.service.ExposureNotificationService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class ExposureUpdateWorkerTest {
    private lateinit var context: Context

    private lateinit var exposureNotificationService: ExposureNotificationService
    private lateinit var exposureRepository: ExposureRepository
    private lateinit var appStateRepository: AppStateRepository
    private lateinit var settingsRepository: SettingsRepository
    val lockedFlow = MutableStateFlow(false)

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

        worker = TestListenableWorkerBuilder<ExposureUpdateWorker>(context)
            .setWorkerFactory(fakeFactory)
            .setInputData(Data.Builder()
                .putString(ExposureUpdateWorker.TOKEN_KEY, "token")
                .build()
            )
            .build()
    }

    @Test
    fun successWhenLocked() {
        coEvery { exposureNotificationService.isEnabled() } returns true
        lockedFlow.value = true
        runBlocking {
            val result = worker.startWork().get()
            Assert.assertEquals(Result.success(), result)
            coVerify(exactly = 0) { exposureNotificationService.getExposureSummary(any()) }
        }
    }

    @Test
    fun successWhenNoMatches() {
        coEvery { exposureNotificationService.isEnabled() } returns true
        coEvery { exposureNotificationService.getExposureSummary(any()) } returns
                ExposureSummary.ExposureSummaryBuilder().setMatchedKeyCount(0).build()

        runBlocking {
            val result = worker.startWork().get()
            Assert.assertEquals(Result.success(), result)
            coVerify(exactly = 0) { settingsRepository.requireExposureConfiguration() }
        }
    }

    @Test
    fun successWhenHighRisk() {
        coEvery { exposureNotificationService.isEnabled() } returns true
        coEvery { exposureNotificationService.getExposureSummary(any()) } returns
                ExposureSummary.ExposureSummaryBuilder().setMatchedKeyCount(1).setMaximumRiskScore(200).build()

        coEvery { exposureNotificationService.getExposureDetails(any()) } returns listOf(
            ExposureInformation.ExposureInformationBuilder().setTotalRiskScore(200).build(),
            ExposureInformation.ExposureInformationBuilder().setTotalRiskScore(160).build()
        )

        runBlocking {
            val result = worker.startWork().get()
            Assert.assertEquals(Result.success(), result)
            coVerify(exactly = 1) { exposureNotificationService.getExposureDetails(any()) }
            coVerify(exactly = 2) { exposureRepository.saveExposure(any()) }
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