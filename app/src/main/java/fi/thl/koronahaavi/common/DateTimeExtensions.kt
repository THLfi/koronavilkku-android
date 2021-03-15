package fi.thl.koronahaavi.common

import java.time.Instant
import java.time.temporal.ChronoUnit

fun Instant.isLessThanWeekOld() = isAfter(Instant.now().minus(7, ChronoUnit.DAYS))
