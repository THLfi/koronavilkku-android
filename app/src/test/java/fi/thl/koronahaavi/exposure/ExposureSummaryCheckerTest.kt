@file:Suppress("DEPRECATION")
package fi.thl.koronahaavi.exposure

import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import fi.thl.koronahaavi.data.Exposure
import fi.thl.koronahaavi.utils.TestData
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ExposureSummaryCheckerTest {

    @Test
    fun noRisk() {
        val checker = ExposureSummaryChecker(
            summary().setMaximumRiskScore(0).setAttenuationDurations(intArrayOf(0,0,0)).build(),
            TestData.exposureConfiguration()
        )
        assertFalse(checker.hasHighRisk())
    }

    @Test
    fun lowRisk() {
        val checker = ExposureSummaryChecker(
            summary().setMaximumRiskScore(10).setAttenuationDurations(intArrayOf(9,11,10)).build(),
            TestData.exposureConfiguration()
        )
        assertFalse(checker.hasHighRisk())
    }

    @Test
    fun highRiskScore() {
        val checker = ExposureSummaryChecker(
            summary().setMaximumRiskScore(200).build(),
            TestData.exposureConfiguration()
        )
        assertTrue(checker.hasHighRisk())
    }

    @Test
    fun longDurationFirst() {
        val checker = ExposureSummaryChecker(
            summary().setMaximumRiskScore(10).setAttenuationDurations(intArrayOf(30,0,0)).build(),
            TestData.exposureConfiguration()
        )
        assertTrue(checker.hasHighRisk())
    }

    @Test
    fun longDurationSecond() {
        val checker = ExposureSummaryChecker(
            summary().setMaximumRiskScore(10).setAttenuationDurations(intArrayOf(0,30,0)).build(),
            TestData.exposureConfiguration()
        )
        assertTrue(checker.hasHighRisk())
    }

    @Test
    fun longDurationThreshold() {
        val checker = ExposureSummaryChecker(
            summary().setMaximumRiskScore(10).setAttenuationDurations(intArrayOf(10,10,0)).build(),
            TestData.exposureConfiguration()
        )
        assertTrue(checker.hasHighRisk())
    }

    @Test
    fun unexpectedWeightArray() {
        val checker = ExposureSummaryChecker(
            summary().build(),
            TestData.exposureConfiguration().copy(durationAtAttenuationWeights = listOf(1.0f))
        )
        assertTrue(checker.hasHighRisk())
    }

    @Test
    fun selectsLatestForDuration() {
        val checker = ExposureSummaryChecker(
            summary().setMaximumRiskScore(10).setAttenuationDurations(intArrayOf(30,30,0)).build(),
            TestData.exposureConfiguration()
        )

        val created = ZonedDateTime.now()
        val latest = LocalDateTime.of(2020, Month.NOVEMBER, 3, 0, 0).atZone(ZoneId.of("Z"))

        val result = checker.filterExposures(listOf(
            Exposure(1, latest.minus(Duration.ofDays(1)), created, 0),
            Exposure(2, latest, created, 0),
            Exposure(3, latest.minus(Duration.ofDays(2)), created, 0)
        ))

        assertEquals(1, result.size)
        assertEquals(latest, result[0].detectedDate)
    }

    private fun summary() = ExposureSummary.ExposureSummaryBuilder()
        .setMatchedKeyCount(1)
        .setMaximumRiskScore(200)
        .setAttenuationDurations(intArrayOf(20,10,0))
}