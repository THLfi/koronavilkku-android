package fi.thl.koronahaavi.common

import android.content.SharedPreferences
import androidx.core.content.edit
import java.time.Instant

fun SharedPreferences.tryBoolean(key: String): Boolean? {
    return if (contains(key)) {
        getBoolean(key, false)
    } else {
        null
    }
}

fun SharedPreferences.setBoolean(key: String, value: Boolean?) {
    edit {
        if (value != null) {
            putBoolean(key, value)
        } else {
            remove(key)
        }
    }
}

fun SharedPreferences.getInstant(key: String): Instant? =
    getLong(key, 0)
        .takeIf { it > 0 }
        ?.let { Instant.ofEpochSecond(it) }


fun SharedPreferences.setInstant(key: String, value: Instant) =
    edit { putLong(key, value.epochSecond) }
