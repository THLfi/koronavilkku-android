package fi.thl.koronahaavi.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import fi.thl.koronahaavi.di.AppModule
import java.security.GeneralSecurityException

/**
 * wrapper around encrypted shared preferences to catch and retry for security exceptions
 */
class VilkkuSharedPreferences(context: Context) : SharedPreferences {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        SHARED_PREFERENCES_NAME,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC), // gets encryption key alias from keystore
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

    override fun getAll(): MutableMap<String, *>
            = prefs.all

    override fun getString(key: String?, defValue: String?): String?
            = prefs.getString(key, defValue)

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>?
            = prefs.getStringSet(key, defValues)

    override fun getInt(key: String?, defValue: Int): Int
            = prefs.getInt(key, defValue)

    override fun getLong(key: String?, defValue: Long): Long
            = prefs.getLong(key, defValue)

    override fun getFloat(key: String?, defValue: Float): Float
            = prefs.getFloat(key, defValue)

    override fun getBoolean(key: String?, defValue: Boolean): Boolean = callWithRetry {
        prefs.getBoolean(key, defValue)
    }

    override fun contains(key: String?): Boolean
            = prefs.contains(key)

    override fun edit(): SharedPreferences.Editor
            = prefs.edit()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?)
            = prefs.registerOnSharedPreferenceChangeListener(listener)

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?)
            = prefs.unregisterOnSharedPreferenceChangeListener(listener)

    private fun <T> callWithRetry(retryDelay: Long = 100, block: () -> T): T {
        try {
            return block()
        }
        catch (exception: SecurityException) {
            if (retryDelay > 800) {
                throw VilkkuSharedPreferencesException(exception)
            }
            else {
                Thread.sleep(retryDelay)
                return callWithRetry(retryDelay * 2, block)
            }
        }
    }

    companion object {
        const val SHARED_PREFERENCES_NAME = "fi.thl.koronavilkku.prefs"
    }
}

class VilkkuSharedPreferencesException(cause: Throwable?) : Throwable(cause)
