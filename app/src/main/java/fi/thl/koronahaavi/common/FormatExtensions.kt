package fi.thl.koronahaavi.common

import android.content.Context
import fi.thl.koronahaavi.R
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object FormatExtensions {
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yyyy")
    private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("H.mm")

    fun Context.formatLastCheckTime(dateTime: ZonedDateTime?): String =
        if (dateTime != null) {
            getString(R.string.exposure_detail_last_check,
                formatRelativeDateTime(dateTime)
            )
        } else {
            getString(R.string.exposure_detail_no_last_check)
        }

    fun formatDate(dateTime: ZonedDateTime): String = DATE_FORMATTER.format(dateTime)

    /**
     * Format a string with relative date part and normal time with a preposition
     * today at 9:30am
     * yesterday at 11:15pm
     * 12.9.2020 at 5:34pm
     */
    private fun Context.formatRelativeDateTime(dateTime: ZonedDateTime): String =
        getString(R.string.all_date_and_time,
            formatRelativeDate(dateTime),
            TIME_FORMATTER.format(dateTime)
        )

    private fun Context.formatRelativeDate(dateTime: ZonedDateTime): String = when {
        dateTime.toLocalDate() == LocalDate.now() -> getString(R.string.all_today)
        dateTime.plusDays(1).toLocalDate() == LocalDate.now() -> getString(R.string.all_yesterday)
        else -> formatDate(dateTime)
    }

}