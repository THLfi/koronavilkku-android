package fi.thl.koronahaavi.data

import fi.thl.koronahaavi.utils.TestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAmount

class DefaultExposureRepositoryTest {
    private lateinit var repository: DefaultExposureRepository
    private lateinit var keyGroupTokenDao: KeyGroupTokenDao
    private lateinit var exposureDao: ExposureDao
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun init() {
        keyGroupTokenDao = mockk(relaxed = true)
        exposureDao = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        every { settingsRepository.appConfiguration } returns TestData.appConfig

        repository = DefaultExposureRepository(keyGroupTokenDao, exposureDao, settingsRepository)
    }

    @Test
    fun flowExposureNotifications() {
        every { exposureDao.flowAll() } returns flowOf(listOf(
            createExposure(Duration.ofDays(2)),
            createExposure(Duration.ofDays(2).plusSeconds(1)),
            createExposure(Duration.ofDays(2).minusSeconds(1)),
            createExposure(Duration.ofDays(3)),
            createExposure(Duration.ofDays(3)),
            createExposure(Duration.ofDays(4))
        ))

        runBlocking {
            repository.flowExposureNotifications().collectLatest {
                assertEquals(3, it.size)
                assertTrue(it.any { n -> n.createdDate == baseCreatedDate.minus(Duration.ofDays(2)) })
                assertTrue(it.any { n -> n.createdDate == baseCreatedDate.minus(Duration.ofDays(3)) })
                assertTrue(it.any { n -> n.createdDate == baseCreatedDate.minus(Duration.ofDays(4)) })
            }
        }
    }

    @Test
    fun deletesExpiredExposures() {
        val now = ZonedDateTime.now()
        val ttlDays = TestData.appConfig.exposureValidDays + 1

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
        val ttlDays = TestData.appConfig.exposureValidDays + 1

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

    private val baseCreatedDate = ZonedDateTime.of(2020, 1, 1, 1, 1, 1, 0, ZoneId.of("Z"))

    private fun createExposure(age: TemporalAmount)
            = Exposure(1, ZonedDateTime.now(), baseCreatedDate.minus(age), 0)
}