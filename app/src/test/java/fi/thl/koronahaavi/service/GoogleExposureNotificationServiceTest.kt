package fi.thl.koronahaavi.service

import com.google.android.gms.nearby.exposurenotification.*
import com.google.android.gms.tasks.Tasks
import fi.thl.koronahaavi.utils.TestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class GoogleExposureNotificationServiceTest {
    private lateinit var service: GoogleExposureNotificationService
    private lateinit var client: ExposureNotificationClient

    @Before
    fun init() {
        client = mockk(relaxed = true)

        every { client.diagnosisKeysDataMapping } returns Tasks.forResult(
            DiagnosisKeysDataMapping.DiagnosisKeysDataMappingBuilder()
                .setDaysSinceOnsetToInfectiousness(mapOf())
                .setInfectiousnessWhenDaysSinceOnsetMissing(Infectiousness.STANDARD)
                .setReportTypeWhenMissing(ReportType.CONFIRMED_TEST)
                .build()
        )

        every { client.setDiagnosisKeysDataMapping(any()) } returns Tasks.forResult(null)

        every { client.getDailySummaries(any()) } returns Tasks.forResult(listOf())

        service = GoogleExposureNotificationService(client)
    }

    @Test
    fun test() {
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
}