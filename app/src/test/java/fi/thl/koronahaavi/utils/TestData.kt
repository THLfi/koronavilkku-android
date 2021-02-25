package fi.thl.koronahaavi.utils

import fi.thl.koronahaavi.data.Exposure
import fi.thl.koronahaavi.data.OmaoloFeatures
import fi.thl.koronahaavi.data.ServiceLanguages
import fi.thl.koronahaavi.exposure.Municipality
import fi.thl.koronahaavi.service.AppConfiguration
import fi.thl.koronahaavi.service.DiagnosisKey
import fi.thl.koronahaavi.service.ExposureConfigurationData
import fi.thl.koronahaavi.service.InfectiousnessLevel
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
            attenuationBucketThresholdDb = listOf(10, 20, 30),
            attenuationBucketWeights = listOf(2.0, 1.0, 0.0, 0.0),
            daysSinceExposureThreshold = 10,
            daysSinceOnsetToInfectiousness = daysSinceOnsetToInfectiousness(),
            infectiousnessWeightHigh = 1.5,
            infectiousnessWeightStandard = 1.0,
            infectiousnessWhenDaysSinceOnsetMissing = InfectiousnessLevel.STANDARD,
            minimumDailyScore = 900,
            minimumWindowScore = 1.0,
            reportTypeWeightConfirmedClinicalDiagnosis = 0.0,
            reportTypeWeightConfirmedTest = 1.0,
            reportTypeWeightRecursive = 0.0,
            reportTypeWeightSelfReport = 0.0,
            availableCountries = listOf()
    )

    fun diagnosisKey() = DiagnosisKey(
            keyData = "xAKM/2Mlz2RvfO/wKFAzpg==",
            transmissionRiskLevel = 5,
            rollingPeriod = 144,
            rollingStartIntervalNumber = 2650847
    )

    fun daysSinceOnsetToInfectiousness() = mapOf(
        "-14" to InfectiousnessLevel.NONE,
        "-13" to InfectiousnessLevel.NONE,
        "-12" to InfectiousnessLevel.NONE,
        "-11" to InfectiousnessLevel.NONE,
        "-10" to InfectiousnessLevel.NONE,
        "-9" to InfectiousnessLevel.NONE,
        "-8" to InfectiousnessLevel.NONE,
        "-7" to InfectiousnessLevel.NONE,
        "-6" to InfectiousnessLevel.NONE,
        "-5" to InfectiousnessLevel.NONE,
        "-4" to InfectiousnessLevel.NONE,
        "-3" to InfectiousnessLevel.NONE,
        "-2" to InfectiousnessLevel.STANDARD,
        "-1" to InfectiousnessLevel.HIGH,
        "0" to InfectiousnessLevel.HIGH,
        "1" to InfectiousnessLevel.HIGH,
        "2" to InfectiousnessLevel.HIGH,
        "3" to InfectiousnessLevel.HIGH,
        "4" to InfectiousnessLevel.STANDARD,
        "5" to InfectiousnessLevel.STANDARD,
        "6" to InfectiousnessLevel.STANDARD,
        "7" to InfectiousnessLevel.STANDARD,
        "8" to InfectiousnessLevel.STANDARD,
        "9" to InfectiousnessLevel.STANDARD,
        "10" to InfectiousnessLevel.STANDARD,
        "11" to InfectiousnessLevel.NONE,
        "12" to InfectiousnessLevel.NONE,
        "13" to InfectiousnessLevel.NONE,
        "14" to InfectiousnessLevel.NONE
    )
}
