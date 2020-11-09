package fi.thl.koronahaavi.utils

import fi.thl.koronahaavi.data.Exposure
import fi.thl.koronahaavi.data.OmaoloFeatures
import fi.thl.koronahaavi.data.ServiceLanguages
import fi.thl.koronahaavi.exposure.Municipality
import fi.thl.koronahaavi.service.AppConfiguration
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
}
