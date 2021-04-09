package fi.thl.koronahaavi.data

import fi.thl.koronahaavi.utils.TestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAmount
import java.util.*

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
    fun clearWhenNoExposures() {
        every { exposureDao.flowAll() } returns flowOf(listOf())

        runBlocking {
            repository.getIsExposedFlow().collectLatest {
                assertFalse(it)
            }
        }
    }

    @Test
    fun clearWhenExposuresOld() {
        val limitDays = TestData.appConfig.exposureValidDays + 1L

        every { exposureDao.flowAll() } returns flowOf(listOf(
            createExposure().copy(detectedDate = ZonedDateTime.now().minusDays(limitDays + 1L)),
            createExposure().copy(detectedDate = ZonedDateTime.now().minusDays(limitDays + 2L))
        ))

        runBlocking {
            repository.getIsExposedFlow().collectLatest {
                assertFalse(it)
            }
        }
    }

    @Test
    fun isExposed() {
        every { exposureDao.flowAll() } returns flowOf(listOf(
            createExposure().copy(
                detectedDate = ZonedDateTime.now().minusDays(TestData.appConfig.exposureValidDays.toLong())
            )
        ))

        runBlocking {
            repository.getIsExposedFlow().collectLatest {
                assertTrue(it)
            }
        }
    }

    @Test
    fun notificationsEmptyWithNoData() {
        every { keyGroupTokenDao.getExposureTokensFlow() } returns flowOf(listOf())

        runBlocking {
            repository.getExposureNotificationsFlow().collectLatest {
                assertTrue(it.isEmpty())
            }
        }
    }

    @Test
    fun notificationsEmptyWithLegacyData() {
        every { keyGroupTokenDao.getExposureTokensFlow() } returns flowOf(listOf(
            createLegacyKeyGroupToken()
        ))

        runBlocking {
            repository.getExposureNotificationsFlow().collectLatest {
                assertTrue(it.isEmpty())
            }
        }
    }

    @Test
    fun notificationsWithLegacyData() {
        every { keyGroupTokenDao.getExposureTokensFlow() } returns flowOf(listOf(
            createLegacyKeyGroupToken(),
            createKeyGroupToken(),
            createKeyGroupToken()
        ))

        runBlocking {
            repository.getExposureNotificationsFlow().collectLatest {
                assertEquals(2, it.size)
            }
        }
    }

    @Test
    fun notificationsEmptyWithMissingCount() {
        every { keyGroupTokenDao.getExposureTokensFlow() } returns flowOf(listOf(
            createKeyGroupToken().copy(exposureCount = null)
        ))

        runBlocking {
            repository.getExposureNotificationsFlow().collectLatest {
                assertTrue(it.isEmpty())
            }
        }
    }

    @Test
    fun notificationsWithOldData() {
        val limitDays = TestData.appConfig.exposureValidDays + 1L

        every { keyGroupTokenDao.getExposureTokensFlow() } returns flowOf(listOf(
            createKeyGroupToken().copy(latestExposureDate = ZonedDateTime.now().minusDays(limitDays + 1L)), // expired
            createKeyGroupToken().copy(latestExposureDate = ZonedDateTime.now().minusDays(limitDays - 1L)),
            createKeyGroupToken()
        ))

        runBlocking {
            repository.getExposureNotificationsFlow().collectLatest {
                assertEquals(2, it.size)
                assertTrue(it.all { n ->
                    n.exposureRangeStart == n.createdDate.minusDays(TestData.appConfig.exposureValidDays.toLong()) &&
                    n.exposureRangeEnd == n.createdDate.minusDays(1)
                })
            }
        }
    }

    @Test
    fun notificationsWithDayExposures() {
        every { keyGroupTokenDao.getExposureTokensFlow() } returns flowOf(listOf(
            createDayExposureKeyGroupToken()
        ))

        runBlocking {
            repository.getExposureNotificationsFlow().collectLatest {
                assertTrue(it.all { n ->
                    n.exposureRangeStart == n.createdDate.minusDays(TestData.appConfig.exposureValidDays.toLong()) &&
                    n.exposureRangeEnd == n.createdDate
                })
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

        // legacy
        val tokenA = KeyGroupToken("a", now.minusDays(ttlDays - 10L))
        val tokenB = KeyGroupToken("b", now.minusDays(ttlDays - 1L))
        val tokenC = KeyGroupToken("c", now.minusDays(15).minusHours(1))

        // updated with exposure date
        val tokenD = createKeyGroupToken().copy(latestExposureDate = now.minusDays(ttlDays + 1L))
        val tokenE = createKeyGroupToken().copy(latestExposureDate = now.minusDays(ttlDays - 1L))

        coEvery { keyGroupTokenDao.getAll() } returns
                listOf(tokenA, tokenB, tokenC, tokenD, tokenE)

        runBlocking {
            repository.deleteExpiredExposuresAndTokens()

            coVerify(exactly = 0) { keyGroupTokenDao.delete(tokenA) }
            coVerify(exactly = 0) { keyGroupTokenDao.delete(tokenB) }
            coVerify(exactly = 1) { keyGroupTokenDao.delete(tokenC) }
            coVerify(exactly = 1) { keyGroupTokenDao.delete(tokenD) }
            coVerify(exactly = 0) { keyGroupTokenDao.delete(tokenE) }
        }
    }

    private val baseCreatedDate = ZonedDateTime.of(2020, 1, 1, 1, 1, 1, 0, ZoneId.of("Z"))

    private fun createExposure(age: TemporalAmount = Duration.ofDays(1))
            = Exposure(1, ZonedDateTime.now(), baseCreatedDate.minus(age), 0)

    private fun createKeyGroupToken() =
        KeyGroupToken(
            token = UUID.randomUUID().toString(),
            updatedDate = ZonedDateTime.now(),
            matchedKeyCount = 1,
            maximumRiskScore = 100,
            exposureCount = 1,
            latestExposureDate = ZonedDateTime.now().minusDays(2)
        )

    private fun createLegacyKeyGroupToken() =
        createKeyGroupToken().copy(exposureCount = null, latestExposureDate = null)

    private fun createDayExposureKeyGroupToken() =
        createKeyGroupToken().copy(exposureCount = null, dayCount = 1)
}