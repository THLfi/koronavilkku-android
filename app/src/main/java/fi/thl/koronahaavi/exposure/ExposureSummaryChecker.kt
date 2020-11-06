@file:Suppress("DEPRECATION")
package fi.thl.koronahaavi.exposure

import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import fi.thl.koronahaavi.data.Exposure
import fi.thl.koronahaavi.service.ExposureConfigurationData
import timber.log.Timber

class ExposureSummaryChecker(
    summary: ExposureSummary,
    private val config: ExposureConfigurationData
) {
    private val result = when {
        summary.isRiskHigh() -> {
            Timber.d("High risk: maximumRiskScore ${summary.maximumRiskScore} is over minimumRiskScore ${config.minimumRiskScore}")
            Result.RISK_HIGH
        }
        summary.isAttenuationDurationLong() -> {
            Timber.d("Long duration: attenuationDurationsInMinutes ${summary.attenuationDurationsInMinutes.joinToString()}")
            Result.DURATION_LONG
        }
        else -> {
            Timber.d("No risk: maximumRiskScore ${summary.maximumRiskScore}, attenuationDurationsInMinutes ${summary.attenuationDurationsInMinutes.joinToString()}")
            Result.NO_RISK
        }
    }

    fun hasHighRisk() =
        result == Result.RISK_HIGH || result == Result.DURATION_LONG

    fun filterExposures(exposures: List<Exposure>): List<Exposure> {
        return if (result == Result.DURATION_LONG) {
            // only include latest since exposure was accumulated and is therefore
            // represented better as a single exposure
            exposures.sortedBy { it.detectedDate }.takeLast(1)
        } else {
            exposures
        }
    }

    private fun ExposureSummary.isRiskHigh(): Boolean =
        maximumRiskScore >= config.minimumRiskScore

    private fun ExposureSummary.isAttenuationDurationLong() =
        getWeightedAttenuationDurationSum() >= config.exposureRiskDuration.toFloat()

    private fun ExposureSummary.getWeightedAttenuationDurationSum() =
        attenuationDurationsInMinutes.foldIndexed(0.0f) { i, sum, duration ->
            // weights should be same length as durations, but make sure we don't get index out of bounds
            val weight = config.durationAtAttenuationWeights.getOrElse(i) { 0.0f }
            sum + weight * duration
        }

    private enum class Result {RISK_HIGH, DURATION_LONG, NO_RISK}
}