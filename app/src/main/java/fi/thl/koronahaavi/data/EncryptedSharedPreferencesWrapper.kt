package fi.thl.koronahaavi.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Forwards all calls to an instance of EncryptedSharedPreferences
 * and recreates the instance and retries for security exceptions
 *
 * Fix attempt for https://issuetracker.google.com/issues/158234058
 */
class EncryptedSharedPreferencesWrapper(
    private val context: Context
) : SharedPreferences {

    private var prefs = createPreferences()

    private fun createPreferences() = EncryptedSharedPreferences.create(
        SHARED_PREFERENCES_NAME,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC), // gets encryption key alias from keystore
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

    override fun getAll(): MutableMap<String, *> = callWithRetry {
        prefs.all
    }

    override fun getString(key: String?, defValue: String?): String? = callWithRetry {
        prefs.getString(key, defValue)
    }

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = callWithRetry {
        prefs.getStringSet(key, defValues)
    }

    override fun getInt(key: String?, defValue: Int): Int = callWithRetry {
        prefs.getInt(key, defValue)
    }

    override fun getLong(key: String?, defValue: Long): Long = callWithRetry {
        prefs.getLong(key, defValue)
    }

    override fun getFloat(key: String?, defValue: Float): Float = callWithRetry {
        prefs.getFloat(key, defValue)
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean = callWithRetry {
        prefs.getBoolean(key, defValue)
    }

    override fun contains(key: String?): Boolean = callWithRetry {
        prefs.contains(key)
    }

    override fun edit(): SharedPreferences.Editor = callWithRetry {
        prefs.edit()
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?)
            = prefs.registerOnSharedPreferenceChangeListener(listener)

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?)
            = prefs.unregisterOnSharedPreferenceChangeListener(listener)

    // Synchronized since EncryptedSharedPreferences methods not thread-safe
    // see https://developer.android.com/topic/security/data#classes-in-library
    // Also synchronizing at method level to prevent other threads from accessing
    // preferences instance during recovery attempts
    @Synchronized
    private fun <T> callWithRetry(retryDelay: Long = 100, block: () -> T): T {
        return try {
            block()
        }
        catch (exception: SecurityException) {
            // This occurs very rarely and is possibly caused by android keystore being busy, but exact reason unclear
            // See https://issuetracker.google.com/issues/158234058
            // Attempt to recover few times by recreating shared preferences and retrying operation as suggested in issue

            if (retryDelay > 800) {
                throw EncryptedSharedPreferencesException(exception)
            } else {
                Thread.sleep(retryDelay)
                prefs = createPreferences()
                callWithRetry(retryDelay * 2, block)
            }
        }
    }

    companion object {
        const val SHARED_PREFERENCES_NAME = "fi.thl.koronavilkku.prefs"
    }
}

class EncryptedSharedPreferencesException(cause: Throwable?) : Throwable(cause)
