package fi.thl.koronahaavi.common

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Build
import java.util.*

private const val PREF_KEY_LANGUAGE = "language"

fun Context.withSavedLanguage(): Context {
    return withLanguage(getSavedLanguage())
}

fun Context.withLanguage(language: String?): Context {

    if (language == null || Locale.getDefault().language == language) {
        return this
    }

    val locale = Locale(language)
    Locale.setDefault(locale)

    val cfg = android.content.res.Configuration()
    cfg.updateFrom(resources.configuration)
    cfg.setLocale(locale)

    return createConfigurationContext(cfg)
}

fun Context.getSavedLanguage(): String? {
    return languagePreferences().getString(PREF_KEY_LANGUAGE, null)
}

fun Context.setSavedLanguage(language: String?) {

    if (language == null) {
        // Restore system primary locale as Context.withSavedLanguage() doesn't do anything
        // if saved language is null. Without this the previously set locale (fi/sv/en)
        // would be used (for formatting - the UI language would be correct).
        Locale.setDefault(Resources.getSystem().primaryLocale())
    }

    languagePreferences().edit()
        .putString(PREF_KEY_LANGUAGE, language)
        .apply()
}

private fun Context.languagePreferences(): SharedPreferences {
    // No encryption is needed for language preference, and not sure if SettingsRepository could
    // be accessed early enough (attachBaseContext) => use a separate SharedPreferences.
    return getSharedPreferences("language", Context.MODE_PRIVATE)
}

fun Resources.primaryLocale(): Locale {
    return configuration.let { cfg ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cfg.locales.get(0)
        } else {
            cfg.locale
        }
    }
}
