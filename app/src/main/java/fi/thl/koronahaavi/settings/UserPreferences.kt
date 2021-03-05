package fi.thl.koronahaavi.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.thl.koronahaavi.common.setBoolean
import fi.thl.koronahaavi.common.tryBoolean
import javax.inject.Inject
import javax.inject.Singleton

// Wrapper for accessing user preferences. Uses a standard non-encrypted SharedPreferences as
// the encrypted version can be problematic in some cases.
class UserPreferences(
    context: Context,
    preferencesName: String = "fi.thl.koronavilkku.user_prefs"
) {
    private val keyLanguage = "language"
    private val keyPowerOptimizationDisabled = "power_optimization_disabled"

    private val prefs = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    var language: String?
        get() = prefs.getString(keyLanguage, null)
        set(value) = prefs.edit { putString(keyLanguage, value) }

    var powerOptimizationDisableAllowed: Boolean?
        get() = prefs.tryBoolean(keyPowerOptimizationDisabled)
        set(value) = prefs.setBoolean(keyPowerOptimizationDisabled, value)
}
