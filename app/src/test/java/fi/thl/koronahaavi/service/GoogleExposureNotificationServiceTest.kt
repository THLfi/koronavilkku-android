package fi.thl.koronahaavi.service

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.exposurenotification.*
import com.google.android.gms.tasks.Tasks
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.utils.TestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class GoogleExposureNotificationServiceTest {
    private lateinit var service: GoogleExposureNotificationService
    private lateinit var client: ExposureNotificationClient
    private lateinit var appStateRepository: AppStateRepository

    @Before
    fun init() {
        client = mockk(relaxed = true)
        appStateRepository = mockk(relaxed = true)

        every { client.diagnosisKeysDataMapping } returns Tasks.forResult(
                TestData.exposureConfiguration().toDiagnosisKeysDataMapping()
        )

        every { client.setDiagnosisKeysDataMapping(any()) } returns Tasks.forResult(null)
        every { client.getDailySummaries(any()) } returns Tasks.forResult(listOf())
        every { client.status } returns Tasks.forResult(setOf(ExposureNotificationStatus.ACTIVATED))
        every { client.provideDiagnosisKeys(any<List<File>>()) } returns Tasks.forResult(null)

        service = GoogleExposureNotificationService(client, appStateRepository)
    }

    @Test
    fun dailyExposuresMapped() {
        val exposureDate = LocalDate.of(2021, 1, 15)
        val exposureScore = 1200

        val fakeDaySummary: DailySummary = mockk(relaxed = true)
        val fakeSummaryData: DailySummary.ExposureSummaryData = mockk(relaxed = true)
        every { fakeDaySummary.daysSinceEpoch } returns exposureDate.toEpochDay().toInt()
        every { fakeDaySummary.summaryData } returns fakeSummaryData
        every { fakeSummaryData.scoreSum } returns exposureScore.toDouble()

        every { client.getDailySummaries(any()) } returns Tasks.forResult(listOf(
            fakeDaySummary
        ))

        runBlocking {
            val dailyExposures = service.getDailyExposures(TestData.exposureConfiguration())

            assertEquals(exposureDate, dailyExposures[0].day)
            assertEquals(exposureScore, dailyExposures[0].score)
        }
    }

    @Test
    fun keyMappingNoChanges() {
        val config = TestData.exposureConfiguration()
        every { client.diagnosisKeysDataMapping } returns Tasks.forResult(config.toDiagnosisKeysDataMapping())

        runBlocking {
            service.getDailyExposures(config)
            verify(exactly = 0) { client.setDiagnosisKeysDataMapping(any()) }
        }
    }

    @Test
    fun keyMappingReportTypeChanged() {
        val config = TestData.exposureConfiguration()
        val mapping = config.toDiagnosisKeysDataMapping()

        every { client.diagnosisKeysDataMapping } returns Tasks.forResult(
                mapping.cloneBuilder().setReportTypeWhenMissing(ReportType.SELF_REPORT).build()
        )

        runBlocking {
            service.getDailyExposures(config)
            verify(exactly = 1) { client.setDiagnosisKeysDataMapping(any()) }
        }
    }

    @Test
    fun keyMappingDefaultInfectiousnessChanged() {
        val config = TestData.exposureConfiguration()
        val mapping = config.toDiagnosisKeysDataMapping()

        every { client.diagnosisKeysDataMapping } returns Tasks.forResult(
                mapping.cloneBuilder().setInfectiousnessWhenDaysSinceOnsetMissing(Infectiousness.HIGH).build()
        )

        runBlocking {
            service.getDailyExposures(config)
            verify(exactly = 1) { client.setDiagnosisKeysDataMapping(any()) }
        }
    }

    @Test
    fun keyMappingInfectiousnessMapChanged() {
        val config = TestData.exposureConfiguration()
        val mapping = config.toDiagnosisKeysDataMapping()

        val modifiedMap = mapOf(-1 to Infectiousness.STANDARD, 1 to Infectiousness.HIGH)

        every { client.diagnosisKeysDataMapping } returns Tasks.forResult(
                mapping.cloneBuilder().setDaysSinceOnsetToInfectiousness(modifiedMap).build()
        )

        runBlocking {
            service.getDailyExposures(config)
            verify(exactly = 1) { client.setDiagnosisKeysDataMapping(any()) }
        }
    }

    @Test
    fun provideFilesFailsForOldVersionException() {
        every { client.version } returns Tasks.forException(
                ApiException(Status(CommonStatusCodes.API_NOT_CONNECTED))
        )

        runBlocking {
            val result = service.provideDiagnosisKeyFiles(TestData.exposureConfiguration(), listOf())
            assertTrue(result is ExposureNotificationService.ResolvableResult.ApiNotSupported)
        }
    }

    @Test
    fun provideFilesFailsForOldVersion() {
        every { client.version } returns Tasks.forResult(15000000L)

        runBlocking {
            val result = service.provideDiagnosisKeyFiles(TestData.exposureConfiguration(), listOf())
            assertTrue(result is ExposureNotificationService.ResolvableResult.Failed)
        }
    }

    @Test
    fun provideFilesVersionOk() {
        every { client.version } returns Tasks.forResult(16000000L)

        runBlocking {
            val result = service.provideDiagnosisKeyFiles(TestData.exposureConfiguration(), listOf())
            assertTrue(result is ExposureNotificationService.ResolvableResult.Success)
        }
    }

    @Test
    fun keyMappingQuotaLimitAsExpected() {
        val config = TestData.exposureConfiguration()
        val mapping = config.toDiagnosisKeysDataMapping()

        every { client.diagnosisKeysDataMapping } returns Tasks.forResult(
                mapping.cloneBuilder().setReportTypeWhenMissing(ReportType.SELF_REPORT).build()
        )
        every { client.setDiagnosisKeysDataMapping(any()) } throws
                ApiException(Status(ExposureNotificationStatusCodes.FAILED_RATE_LIMITED))

        every { appStateRepository.getLastExposureKeyMappingUpdate() } returns
                Instant.now().minus(2, ChronoUnit.DAYS)

        runBlocking {
            service.getDailyExposures(config)
            verify(exactly = 1) { client.setDiagnosisKeysDataMapping(any()) }
            verify(exactly = 1) { client.getDailySummaries(any()) }
        }
    }

    @Test(expected = ApiException::class)
    fun keyMappingQuotaLimitError() {
        val config = TestData.exposureConfiguration()
        val mapping = config.toDiagnosisKeysDataMapping()

        // trying to update mapping when previous change over a week ago, error critical

        every { client.diagnosisKeysDataMapping } returns Tasks.forResult(
                mapping.cloneBuilder().setReportTypeWhenMissing(ReportType.SELF_REPORT).build()
        )
        every { client.setDiagnosisKeysDataMapping(any()) } throws
                ApiException(Status(ExposureNotificationStatusCodes.FAILED_RATE_LIMITED))

        every { appStateRepository.getLastExposureKeyMappingUpdate() } returns
                Instant.now().minus(8, ChronoUnit.DAYS)

        runBlocking {
            service.getDailyExposures(config)
        }
    }

    @Test(expected = ApiException::class)
    fun keyMappingOtherApiError() {
        val config = TestData.exposureConfiguration()
        val mapping = config.toDiagnosisKeysDataMapping()

        // trying to update mapping when previous change over a week ago, error critical

        every { client.diagnosisKeysDataMapping } returns Tasks.forResult(
                mapping.cloneBuilder().setReportTypeWhenMissing(ReportType.SELF_REPORT).build()
        )
        every { client.setDiagnosisKeysDataMapping(any()) } throws
                ApiException(Status(ExposureNotificationStatusCodes.ERROR))

        runBlocking {
            service.getDailyExposures(config)
        }
    }

    private fun DiagnosisKeysDataMapping.cloneBuilder() =
            DiagnosisKeysDataMapping.DiagnosisKeysDataMappingBuilder()
                .setReportTypeWhenMissing(reportTypeWhenMissing)
                .setInfectiousnessWhenDaysSinceOnsetMissing(infectiousnessWhenDaysSinceOnsetMissing)
                .setDaysSinceOnsetToInfectiousness(daysSinceOnsetToInfectiousness)

}