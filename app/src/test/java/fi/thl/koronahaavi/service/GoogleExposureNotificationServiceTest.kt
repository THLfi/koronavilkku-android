package fi.thl.koronahaavi.service

import com.google.android.gms.nearby.exposurenotification.*
import com.google.android.gms.tasks.Tasks
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.utils.TestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

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

    private fun DiagnosisKeysDataMapping.cloneBuilder() =
            DiagnosisKeysDataMapping.DiagnosisKeysDataMappingBuilder()
                .setReportTypeWhenMissing(reportTypeWhenMissing)
                .setInfectiousnessWhenDaysSinceOnsetMissing(infectiousnessWhenDaysSinceOnsetMissing)
                .setDaysSinceOnsetToInfectiousness(daysSinceOnsetToInfectiousness)

}