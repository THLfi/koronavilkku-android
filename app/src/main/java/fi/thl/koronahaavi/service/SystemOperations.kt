package fi.thl.koronahaavi.service

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * Wrapper for operations requiring context to help with
 * test mocking
 */
class SystemOperations @Inject constructor (
    @ApplicationContext private val context: Context
) {
    fun createFileInCache(filename: String) =
        File(context.cacheDir, filename).apply {
            createNewFile() // create if does not exist
        }

    fun createFileInPersistedStorage(filename: String) =
        File(context.filesDir, filename).apply {
            createNewFile()
        }

    fun fileExistsInPersistedStorage(filename: String): Boolean =
        File(context.filesDir, filename).exists()

    fun encodeToBase64(data: ByteArray): String =
        Base64.encodeToString(data, Base64.NO_WRAP)
}