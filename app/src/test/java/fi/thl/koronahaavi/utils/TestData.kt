package fi.thl.koronahaavi.utils

import fi.thl.koronahaavi.data.Exposure
import fi.thl.koronahaavi.service.AppConfiguration
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAmount

object TestData {
    private val baseCreatedDate = ZonedDateTime.of(2020, 11, 2, 13, 10, 5, 0, ZoneId.of("Z"))

    fun exposure(age: TemporalAmount = Duration.ofDays(2))
            = Exposure(1, ZonedDateTime.now(), baseCreatedDate.minus(age), 0)

    val appConfig = AppConfiguration(
        version = 0,
        diagnosisKeysPerSubmit = 18,
        pollingIntervalMinutes = 240,
        tokenLength = 12,
        exposureValidDays = 10
    )
}
