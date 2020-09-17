package fi.thl.koronahaavi.service

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.ExposureRepository
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import retrofit2.HttpException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.*
import org.robolectric.annotation.Config
import retrofit2.Response
import java.io.File
import java.io.IOException

/**
 * This runs as local JVM unit test with Robolectric
 */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class DiagnosisKeyUpdateWorkerTest {
    private lateinit var context: Context

    private lateinit var exposureNotificationService: ExposureNotificationService
    private lateinit var exposureRepository: ExposureRepository
    private lateinit var appStateRepository: AppStateRepository
    private lateinit var diagnosisKeyService: DiagnosisKeyService
    val lockedFlow = MutableStateFlow(false)

    lateinit var worker: ListenableWorker

    @get:Rule
    val folder = TemporaryFolder()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        exposureNotificationService = mockk(relaxed = true)
        exposureRepository = mockk(relaxed = true)
        appStateRepository = mockk(relaxed = true)
        diagnosisKeyService = mockk(relaxed = true)

        coEvery { exposureNotificationService.isEnabled() } returns true
        coEvery { appStateRepository.getDiagnosisKeyBatchId() } returns "test"
        every { appStateRepository.lockedAfterDiagnosis() } returns lockedFlow

        worker = TestListenableWorkerBuilder<DiagnosisKeyUpdateWorker>(context)
            .setWorkerFactory(fakeFactory)
            .build()
    }

    @Test
    fun successWhenNotEnabled() {
        coEvery { exposureNotificationService.isEnabled() } returns false
        runBlocking {
            val result = worker.startWork().get()

            assertEquals(Result.success(), result)
            coVerify(exactly = 0) { diagnosisKeyService.downloadDiagnosisKeyFiles() }
        }
    }

    @Test
    fun successWhenLocked() {
        lockedFlow.value = true
        runBlocking {
            val result = worker.startWork().get()

            assertEquals(Result.success(), result)
            coVerify(exactly = 0) { diagnosisKeyService.downloadDiagnosisKeyFiles() }
        }
    }

    private fun downloadResultWithFiles(files: List<File>) =
        DownloadResult("last_batch", files, exposureConfig)

    @Test
    fun successWhenNoFiles() {
        coEvery { diagnosisKeyService.downloadDiagnosisKeyFiles() } returns downloadResultWithFiles(listOf())

        runBlocking {
            val result = worker.startWork().get()

            assertEquals(Result.success(), result)
            coVerify(exactly = 0) { exposureNotificationService.provideDiagnosisKeyFiles(any(), any(), eq(exposureConfig)) }
        }
    }

    @Test
    fun failureWhenProvideDiagnosisKeysFails() {
        coEvery { diagnosisKeyService.downloadDiagnosisKeyFiles() } returns
                downloadResultWithFiles(listOf(File("")))

        coEvery { exposureNotificationService.provideDiagnosisKeyFiles(any(), any(), any()) } returns
                ExposureNotificationService.ResolvableResult.Failed(1, 1, "error")

        runBlocking {
            val result = worker.startWork().get()
            assertEquals(Result.failure(), result)
        }
    }

    @Test
    fun successWithFiles() {
        coEvery { diagnosisKeyService.downloadDiagnosisKeyFiles() } returns
                downloadResultWithFiles(listOf(File("")))

        coEvery { exposureNotificationService.provideDiagnosisKeyFiles(any(), any(), any()) } returns
                ExposureNotificationService.ResolvableResult.Success(Unit)

        runBlocking {
            val result = worker.startWork().get()
            assertEquals(Result.success(), result)
        }
    }

    @Test
    fun deletesFilesAfter() {
        val testFile = folder.newFile()
        coEvery { diagnosisKeyService.downloadDiagnosisKeyFiles() } returns
                downloadResultWithFiles(listOf(testFile))

        coEvery { exposureNotificationService.provideDiagnosisKeyFiles(any(), any(), any()) } returns
                ExposureNotificationService.ResolvableResult.Success(Unit)

        runBlocking {
            val result = worker.startWork().get()
            assertEquals(Result.success(), result)
            assertFalse(testFile.exists())
        }
    }

    @Test
    fun retryNetworkError() {
        coEvery { diagnosisKeyService.downloadDiagnosisKeyFiles() } throws IOException()

        runBlocking {
            val result = worker.startWork().get()
            assertEquals(Result.retry(), result)
        }
    }

    @Test
    fun retryHttpTimeout() {
        coEvery { diagnosisKeyService.downloadDiagnosisKeyFiles() } throws
                HttpException(Response.error<String>(503, "".toResponseBody() ) )

        runBlocking {
            val result = worker.startWork().get()
            assertEquals(Result.retry(), result)
        }
    }

    @Test
    fun failureHttpError() {
        coEvery { diagnosisKeyService.downloadDiagnosisKeyFiles() } throws
                HttpException(Response.error<String>(404, "".toResponseBody() ) )

        runBlocking {
            val result = worker.startWork().get()
            assertEquals(Result.failure(), result)
        }
    }

    private val fakeFactory = object : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? {
            return DiagnosisKeyUpdateWorker(appContext, workerParameters, exposureNotificationService,
                exposureRepository, appStateRepository, diagnosisKeyService)
        }
    }

    private val exposureConfig = ExposureConfigurationData(
        version = 1,
        minimumRiskScore = 0,
        attenuationScores = listOf(),
        daysSinceLastExposureScores = listOf(),
        durationScores = listOf(),
        transmissionRiskScoresAndroid = listOf(),
        durationAtAttenuationThresholds = listOf()
    )
}