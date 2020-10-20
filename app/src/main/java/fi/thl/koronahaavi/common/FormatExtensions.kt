package fi.thl.koronahaavi.common

import android.content.Context
import fi.thl.koronahaavi.R
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object FormatExtensions {
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yyyy")

    fun ZonedDateTime.formatRelativeDate(context: Context) = when {
        toLocalDate() == LocalDate.now() -> context.getString(R.string.all_today)
        plusDays(1).toLocalDate() == LocalDate.now() -> context.getString(R.string.all_yesterday)
        else -> DATE_FORMATTER.format(this)
    }

}