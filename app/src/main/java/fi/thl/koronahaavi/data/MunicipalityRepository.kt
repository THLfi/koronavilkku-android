package fi.thl.koronahaavi.data

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import fi.thl.koronahaavi.BuildConfig
import fi.thl.koronahaavi.exposure.Municipality
import fi.thl.koronahaavi.exposure.MunicipalityContact
import fi.thl.koronahaavi.service.SystemOperations
import fi.thl.koronahaavi.service.MunicipalityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class MunicipalityRepository @Inject constructor (
    private val municipalityService: MunicipalityService,
    private val systemOperations: SystemOperations
) {
    private val municipalityFilename = "municipalities.json"

    /**
     * Returns municipalities from a local json file, and if
     * reading from file fails for some reason, attempts to download
     * a new copy
     */
    suspend fun loadAll(): List<Municipality>? =
        tryLoadFromFile() ?: tryDownloadMunicipalities()

    private suspend fun tryDownloadMunicipalities(): List<Municipality>? {
        return try {
            reloadMunicipalities()
            tryLoadFromFile()
        }
        catch (e: Throwable) {
            Timber.e(e, "Failed to download municipalities")
            null
        }
    }

    suspend fun reloadMunicipalities() {
        Timber.d("Reloading municipalities file")
        withContext(Dispatchers.IO) {
            systemOperations.createFileInPersistedStorage(municipalityFilename).let { file ->
                file.outputStream().use { output ->
                    municipalityService.getMunicipalities(BuildConfig.MUNICIPALITY_URL).byteStream().use { input ->
                        Timber.d("Copying municipality data into file ${file.absolutePath}")
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    private suspend fun tryLoadFromFile(): List<Municipality>? {
        if (!systemOperations.fileExistsInPersistedStorage(municipalityFilename)) {
            Timber.d("$municipalityFilename file does not exist")
            return null
        }

        Timber.d("Returning municipalities from $municipalityFilename")

        return try {
            val persistedList = withContext(Dispatchers.IO) {
                systemOperations.createFileInPersistedStorage(municipalityFilename).let { file ->
                    file.inputStream().use { input ->
                        createListAdapter().fromJson(
                            input.source().buffer()
                        )
                    }
                }
            }

            persistedList?.map { it.toMunicipality() }?.sortedBy { it.name }
        }
        catch (e: Throwable) {
            Timber.e(e, "Failed to load municipalities from cache file")
            null
        }
    }

    private fun createListAdapter(): JsonAdapter<List<NetworkMunicipality>> {
        val moshi = Moshi.Builder().build()

        val listType = Types.newParameterizedType(
            MutableList::class.java,
            NetworkMunicipality::class.java
        )
        return moshi.adapter(listType)
    }

    private fun NetworkMunicipality.toMunicipality() =
        Municipality(
            code = this.code,
            name = this.name.getLocal(),
            omaoloFeatures = this.omaolo,
            contacts = this.contact.map { it.toMunicipalityContact() }
        )

    private fun NetworkContact.toMunicipalityContact() =
        MunicipalityContact(
            title = this.title.getLocal(),
            phoneNumber = this.phoneNumber,
            info = this.info.getLocal()
        )
}

@JsonClass(generateAdapter = true)
data class NetworkMunicipality(
    val code: String,
    val name: LocaleString,
    val omaolo: OmaoloFeatures,
    val contact: List<NetworkContact>
)

@JsonClass(generateAdapter = true)
data class NetworkContact(
    val title: LocaleString,
    val phoneNumber: String,
    val info: LocaleString
)

@JsonClass(generateAdapter = true)
data class OmaoloFeatures(
    val available: Boolean,
    val serviceLanguages: ServiceLanguages?
)

@JsonClass(generateAdapter = true)
data class ServiceLanguages (
    val fi: Boolean,
    val sv: Boolean,
    val en: Boolean?
)

/**
 * Strings for currently supported locales, default Finnish
 *
 * If new localizations are added to the app, this should be
 * updated as well.
 */
@JsonClass(generateAdapter = true)
data class LocaleString(
    val fi: String,
    val sv: String?,
    val en: String?
) {
    fun getLocal(): String = when (Locale.getDefault().language) {
        Locale("fi").language -> fi
        Locale("sv").language -> sv ?: fi
        else -> en ?: fi
    }
}
