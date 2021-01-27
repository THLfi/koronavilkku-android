package fi.thl.koronahaavi.data

import android.content.SharedPreferences
import android.util.Base64
import com.squareup.moshi.Moshi
import fi.thl.koronahaavi.service.AppConfiguration
import fi.thl.koronahaavi.service.ExposureConfigurationData
import timber.log.Timber
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor (
    private val prefs: SharedPreferences
) {
    private val moshi: Moshi by lazy { Moshi.Builder().build() }

    private val defaultAppConfig = AppConfiguration(
        version = 0,
        diagnosisKeysPerSubmit = 14,
        pollingIntervalMinutes = 240,
        tokenLength = 12,
        exposureValidDays = 14
    )

    var appConfiguration: AppConfiguration
        private set

    private var exposureConfiguration: ExposureConfigurationData? = null

    init {
        appConfiguration = defaultAppConfig
    }

    fun getExposureConfiguration(): ExposureConfigurationData? =
        exposureConfiguration ?: loadExposureConfig()

    fun requireExposureConfiguration() : ExposureConfigurationData =
        exposureConfiguration ?: loadExposureConfig() ?:
        throw Exception("exposure configuration not available")

    fun updateExposureConfiguration(config: ExposureConfigurationData) {
        saveExposureConfig(config)
        this.exposureConfiguration = config
    }

    private fun exposureConfigAdapter() = moshi.adapter(ExposureConfigurationData::class.java)

    private fun saveExposureConfig(config: ExposureConfigurationData) {
        val json = exposureConfigAdapter().toJson(config)
        prefs.edit().putString(EXPOSURE_CONFIG_KEY, json).apply()
    }

    private fun loadExposureConfig() =
        prefs.getString(EXPOSURE_CONFIG_KEY, null)?.let {
            Timber.d("loading exposure config from prefs")
            exposureConfigAdapter().fromJson(it)
        }

    companion object {
        const val EXPOSURE_CONFIG_KEY = "exposure_configuration_version_2" // non-null fields added
        const val DB_PASSWORD_BYTES = 32
        const val DB_PASSWORD_KEY = "db_password"
    }

    fun getOrCreateDatabasePassword(): ByteArray =
        getDatabasePassword() ?: createDatabasePassword()

    private fun getDatabasePassword() =
        prefs.getString(DB_PASSWORD_KEY, null)?.let {
            Base64.decode(it, Base64.NO_WRAP)
        }

    private fun createDatabasePassword(): ByteArray {
        val passwordBytes = ByteArray(DB_PASSWORD_BYTES).apply {
            SecureRandom().nextBytes(this)
        }

        // prefs is an instance of EncryptedSharedPreferences, see AppModule
        prefs.edit()
            .putString(DB_PASSWORD_KEY, Base64.encodeToString(passwordBytes, Base64.NO_WRAP))
            .apply()

        return passwordBytes
    }
}