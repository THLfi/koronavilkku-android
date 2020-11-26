package fi.thl.koronahaavi.utils

import fi.thl.koronahaavi.data.Exposure
import fi.thl.koronahaavi.data.OmaoloFeatures
import fi.thl.koronahaavi.data.ServiceLanguages
import fi.thl.koronahaavi.exposure.Municipality
import fi.thl.koronahaavi.service.AppConfiguration
import fi.thl.koronahaavi.service.DiagnosisKey
import fi.thl.koronahaavi.service.ExposureConfigurationData
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAmount
import java.util.*
import kotlin.random.Random

object TestData {
    private val baseCreatedDate = ZonedDateTime.of(2020, 11, 2, 13, 10, 5, 0, ZoneId.of("Z"))

    fun exposure(age: TemporalAmount = Duration.ofDays(2)) = Exposure(
        id = Random.nextLong(),
        detectedDate = baseCreatedDate.minus(age).minusDays(3),
        createdDate = baseCreatedDate.minus(age),
        totalRiskScore = 0
    )

    val appConfig = AppConfiguration(
        version = 0,
        diagnosisKeysPerSubmit = 18,
        pollingIntervalMinutes = 240,
        tokenLength = 12,
        exposureValidDays = 10
    )

    fun municipality() = Municipality(
        code = UUID.randomUUID().toString(),
        name = "name",
        omaoloFeatures = omaoloFeatures(),
        contacts = listOf()
    )

    fun omaoloFeatures() = OmaoloFeatures(
        available = true,
        serviceLanguages = ServiceLanguages(fi = true, sv = true, en = null),
        symptomAssessmentOnly = null
    )

    fun exposureConfiguration() = ExposureConfigurationData(
        version = 1,
        minimumRiskScore = 100,
        attenuationScores = listOf(),
        daysSinceLastExposureScores = listOf(),
        durationScores = listOf(),
        transmissionRiskScoresAndroid = listOf(),
        durationAtAttenuationThresholds = listOf(),
        durationAtAttenuationWeights = listOf(1.0f, 0.5f, 0.0f),
        exposureRiskDuration = 15,
        participatingCountries = listOf()
    )

    fun diagnosisKey() = DiagnosisKey(
            keyData = "xAKM/2Mlz2RvfO/wKFAzpg==",
            transmissionRiskLevel = 5,
            rollingPeriod = 144,
            rollingStartIntervalNumber = 2650847
    )
}
