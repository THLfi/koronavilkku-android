package fi.thl.koronahaavi.exposure

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.Exposure
import fi.thl.koronahaavi.data.ExposureRepository
import fi.thl.koronahaavi.data.SettingsRepository
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
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class ClearExpiredExposuresWorkerTest {
    private lateinit var context: Context
    private lateinit var exposureRepository: ExposureRepository
    lateinit var worker: ListenableWorker

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        exposureRepository = mockk(relaxed = true)

        worker = TestListenableWorkerBuilder<ClearExpiredExposuresWorker>(context)
            .setWorkerFactory(fakeFactory)
            .build()
    }

    @Test
    fun deletesExpired() {
        val now = ZonedDateTime.now()
        val ttlDays = ClearExpiredExposuresWorker.EXPOSURE_TTL_SINCE_DETECTION_DAYS

        coEvery { exposureRepository.getAllExposures() } returns listOf(
            Exposure(1, now.minusDays(ttlDays - 10L), now, 0),
            Exposure(2, now.minusDays(ttlDays - 1L), now, 0),
            Exposure(3, now.minusDays(15).minusHours(1), now, 0)
        )

        runBlocking {
            val result = worker.startWork().get()
            Assert.assertEquals(Result.success(), result)
            coVerify(exactly = 0) { exposureRepository.deleteExposure(1) }
            coVerify(exactly = 0) { exposureRepository.deleteExposure(2) }
            coVerify(exactly = 1) { exposureRepository.deleteExposure(3) }
        }
    }


    private val fakeFactory = object : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? {
            return ClearExpiredExposuresWorker(appContext, workerParameters, exposureRepository)
        }
    }

}