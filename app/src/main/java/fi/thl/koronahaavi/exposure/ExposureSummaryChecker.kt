@file:Suppress("DEPRECATION")
package fi.thl.koronahaavi.exposure

import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import fi.thl.koronahaavi.service.ExposureConfigurationData

class ExposureSummaryChecker(
    summary: ExposureSummary,
    private val config: ExposureConfigurationData
) {
    private val result = when {
        summary.isRiskHigh() -> Result.RISK_HIGH
        summary.isAttenuationDurationLong() -> Result.DURATION_LONG
        else -> Result.NO_RISK
    }

    fun hasHighRisk() =
        result == Result.RISK_HIGH || result == Result.DURATION_LONG

    fun filterExposures(exposures: List<ExposureInformation>): List<ExposureInformation> {
        return if (result == Result.DURATION_LONG) {
            // only include latest since exposure was accumulated and is therefore
            // represented better as a single exposure
            exposures.sortedBy { it.dateMillisSinceEpoch }.takeLast(1)
        } else {
            exposures
        }
    }

    private fun ExposureSummary.isRiskHigh(): Boolean =
        maximumRiskScore >= config.minimumRiskScore

    private fun ExposureSummary.isAttenuationDurationLong(): Boolean =
        attenuationDurationsInMinutes[0] + 0.5 * attenuationDurationsInMinutes[1] >= 15

    private enum class Result {RISK_HIGH, DURATION_LONG, NO_RISK}
}