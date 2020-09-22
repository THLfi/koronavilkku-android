package fi.thl.koronahaavi.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Wrapper for accessing user preferences. Uses a standard non-encrypted SharedPreferences as
// the encrypted version can be problematic in some cases.
@Singleton
class UserPreferences @Inject constructor (@ApplicationContext context: Context) {

    private val keyLanguage = "language"
    private val keyPowerOptimizationDisabled = "power_optimization_disabled"

    private val prefs = context.getSharedPreferences("fi.thl.koronavilkku.user_prefs", Context.MODE_PRIVATE)

    var language: String?
        get() = prefs.getString(keyLanguage, null)
        set(value) = prefs.edit { putString(keyLanguage, value) }

    var powerOptimizationDisableAllowed: Boolean?
        get() = prefs.tryBoolean(keyPowerOptimizationDisabled)
        set(value) = prefs.setBoolean(keyPowerOptimizationDisabled, value)
}

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