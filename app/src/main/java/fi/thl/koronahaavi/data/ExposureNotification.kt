package fi.thl.koronahaavi.data

import java.time.ZonedDateTime

data class ExposureNotification(
    val createdDate: ZonedDateTime,
    val exposureRangeStart: ZonedDateTime,
    val exposureRangeEnd: ZonedDateTime,
    val exposureCount: ExposureCount
)

sealed class ExposureCount(open val value: Int) {
    data class ForDetailExposures(override val value: Int) : ExposureCount(value)
    data class ForDays(override val value: Int) : ExposureCount(value)
}