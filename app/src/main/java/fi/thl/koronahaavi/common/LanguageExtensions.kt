package fi.thl.koronahaavi.common

import android.content.Context
import android.content.res.Resources
import android.os.Build
import fi.thl.koronahaavi.settings.UserPreferences
import java.util.*

/**
 * Use this function from attachBaseContext(). Injection doesn't seem to work in
 * attachBaseContext so create a separate instance to access the language setting.
 */
fun Context.withSavedLanguage(): Context {
    return withLanguage(UserPreferences(this).language)
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

fun UserPreferences.changeLanguage(language: String?) {

    if (language == null) {
        // Restore system primary locale as Context.withSavedLanguage() doesn't do anything
        // if saved language is null. Without this the previously set locale (fi/sv/en)
        // would be used (for formatting - the UI language would be correct).
        Locale.setDefault(Resources.getSystem().primaryLocale())
    }

    this.language = language
}

@Suppress("DEPRECATION")
fun Resources.primaryLocale(): Locale {
    return configuration.let { cfg ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cfg.locales.get(0)
        } else {
            cfg.locale
        }
    }
}
