package fi.thl.koronahaavi.common

import android.content.Context
import fi.thl.koronahaavi.R
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object FormatExtensions {
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yyyy")
    private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("H.mm")

    /**
     * Format a string with relative date part and normal time with a preposition
     * today at 9:30am
     * yesterday at 11:15pm
     * 12.9.2020 at 5:34pm
     */
    fun ZonedDateTime.formatRelativeDateTime(context: Context): String =
        context.getString(R.string.all_date_and_time,
            formatRelativeDate(context),
            TIME_FORMATTER.format(this)
        )

    private fun ZonedDateTime.formatRelativeDate(context: Context): String = when {
        toLocalDate() == LocalDate.now() -> context.getString(R.string.all_today)
        plusDays(1).toLocalDate() == LocalDate.now() -> context.getString(R.string.all_yesterday)
        else -> DATE_FORMATTER.format(this)
    }

}