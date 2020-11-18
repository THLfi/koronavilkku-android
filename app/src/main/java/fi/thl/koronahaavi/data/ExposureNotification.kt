package fi.thl.koronahaavi.data

import java.time.ZonedDateTime

data class ExposureNotification(
    val createdDate: ZonedDateTime,
    val exposureCount: Int
)