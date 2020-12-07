package fi.thl.koronahaavi.service

import fi.thl.koronahaavi.proto.TEKSignatureList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.zip.ZipInputStream

class ExposureKeyFileSignatureVerifier {
    /**
     * Verify each file signature and return true if all signatures are valid
     */
    suspend fun verify(files: List<File>, keyBytes: ByteArray) = withContext(Dispatchers.Default) {
        val publicKey = KeyFactory.getInstance(KEY_ALGORITHM_NAME)
            .generatePublic(X509EncodedKeySpec(keyBytes))

        files.all { verifyFile(it, publicKey) }
    }

    private suspend fun verifyFile(file: File, publicKey: PublicKey): Boolean {
        val entries = withContext(Dispatchers.IO) { unzip(file) }
            ?: throw Exception("Invalid exposure archive file")

        return verifySignature(entries, publicKey)
    }

    private fun verifySignature(entries: ExposureFileEntries, publicKey: PublicKey): Boolean {
        // assume only one signature
        val tekSignatureList = TEKSignatureList.parseFrom(entries.signatureBytes)
        if (tekSignatureList.signaturesCount != 1) {
            Timber.e("Invalid signatures count: ${tekSignatureList.signaturesCount}")
            return false
        }

        val tekSignature = tekSignatureList.getSignatures(0)

        val algorithmName = algorithmOidToName[tekSignature.signatureInfo.signatureAlgorithm]
            ?: throw Exception("Invalid algorithm: ${tekSignature.signatureInfo.signatureAlgorithm}")

        val signatureAlgorithm = Signature.getInstance(algorithmName).apply {
            initVerify(publicKey)
            update(entries.exportBinaryBytes)
        }

        return signatureAlgorithm.verify(tekSignature.signature.toByteArray())
    }

    private fun unzip(file: File): ExposureFileEntries? {
        file.inputStream().use { input ->
            var exportBinaryBytes: ByteArray? = null
            var signatureBytes: ByteArray? = null

            ZipInputStream(input).use { zipIn ->
                do {
                    val entry = zipIn.nextEntry
                    when (entry?.name) {
                        EXPORT_BINARY_NAME -> exportBinaryBytes = zipIn.readBytes()
                        EXPORT_SIGNATURE_NAME -> signatureBytes = zipIn.readBytes()
                    }
                    zipIn.closeEntry()
                } while (entry != null)
            }

            return exportBinaryBytes?.let { bin ->
                signatureBytes?.let { sig ->
                    ExposureFileEntries(exportBinaryBytes = bin, signatureBytes = sig)
                }
            }
        }
    }

    private val algorithmOidToName = mapOf(
            "1.2.840.10045.4.3.2"  to "SHA256withECDSA",
            "1.2.840.10045.4.3.4"  to "SHA512withECDSA"
    )

    companion object {
        const val EXPORT_BINARY_NAME = "export.bin"
        const val EXPORT_SIGNATURE_NAME = "export.sig"
        const val KEY_ALGORITHM_NAME = "EC"
    }
}

class ExposureFileEntries(
    val exportBinaryBytes: ByteArray,
    val signatureBytes: ByteArray
)