package fi.thl.koronahaavi.data

import java.time.LocalDate

data class DailyExposure(
    val day: LocalDate,
    val score: Int
)