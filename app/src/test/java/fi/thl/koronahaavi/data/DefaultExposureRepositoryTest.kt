package fi.thl.koronahaavi.data

import androidx.work.ListenableWorker
import fi.thl.koronahaavi.exposure.ClearExpiredExposuresWorker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime

class DefaultExposureRepositoryTest {
    private lateinit var repository: DefaultExposureRepository
    private lateinit var keyGroupTokenDao: KeyGroupTokenDao
    private lateinit var exposureDao: ExposureDao

    @Before
    fun init() {
        keyGroupTokenDao = mockk(relaxed = true)
        exposureDao = mockk(relaxed = true)

        repository = DefaultExposureRepository(keyGroupTokenDao, exposureDao)
    }

    @Test
    fun flowExposureNotifications() {
        every { exposureDao.flowAll() } returns flowOf(listOf(
            Exposure(1, ZonedDateTime.now().minusDays(3), ZonedDateTime.now().minusDays(2), 0),
            Exposure(2, ZonedDateTime.now().minusDays(4), ZonedDateTime.now().minusDays(2).minusSeconds(1), 0)
        ))

        runBlocking {
            repository.flowExposureNotifications().collectLatest {
                assertEquals(1, it.size)
            }
        }
    }

    @Test
    fun deletesExpiredExposures() {
        val now = ZonedDateTime.now()
        val ttlDays = DefaultExposureRepository.EXPOSURE_VALID_SINCE_DETECTION_DAYS

        coEvery { exposureDao.getAll() } returns listOf(
            Exposure(1, now.minusDays(ttlDays - 10L), now, 0),
            Exposure(2, now.minusDays(ttlDays - 1L), now, 0),
            Exposure(3, now.minusDays(15).minusHours(1), now, 0)
        )

        coEvery { keyGroupTokenDao.getAll() } returns listOf()

        runBlocking {
            repository.deleteExpiredExposuresAndTokens()

            coVerify(exactly = 0) { exposureDao.delete(1) }
            coVerify(exactly = 0) { exposureDao.delete(2) }
            coVerify(exactly = 1) { exposureDao.delete(3) }
        }
    }

    @Test
    fun deletesExpiredTokens() {
        val now = ZonedDateTime.now()
        val ttlDays = DefaultExposureRepository.EXPOSURE_VALID_SINCE_DETECTION_DAYS

        coEvery { exposureDao.getAll() } returns listOf()

        val tokenA = KeyGroupToken("a", now.minusDays(ttlDays - 10L))
        val tokenB = KeyGroupToken("b", now.minusDays(ttlDays - 1L))
        val tokenC = KeyGroupToken("c", now.minusDays(15).minusHours(1))

        coEvery { keyGroupTokenDao.getAll() } returns
                listOf(tokenA, tokenB, tokenC)

        runBlocking {
            repository.deleteExpiredExposuresAndTokens()

            coVerify(exactly = 0) { keyGroupTokenDao.delete(tokenA) }
            coVerify(exactly = 0) { keyGroupTokenDao.delete(tokenB) }
            coVerify(exactly = 1) { keyGroupTokenDao.delete(tokenC) }
        }
    }

}