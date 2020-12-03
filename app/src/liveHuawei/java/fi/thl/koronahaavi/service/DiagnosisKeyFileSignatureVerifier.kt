package fi.thl.koronahaavi.service

import fi.thl.koronahaavi.proto.TEKSignatureList
import java.io.File
import java.io.InputStream
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.*
import java.util.zip.ZipInputStream

class DiagnosisKeyFileSignatureVerifier {

    suspend fun verify(files: List<File>): Boolean {

        files.forEach { f ->

            f.inputStream().use { input ->
                val entries = input.unzip() ?: return false

                val payloadBytes = entries.exportBinaryBytes.copyOfRange(16, entries.exportBinaryBytes.size)

                val signatureList = TEKSignatureList.parseFrom(entries.signatureBytes)
                val signature = signatureList.getSignatures(0)

                val sig = Signature.getInstance(
                        algorithmOidToName[signature.signatureInfo.signatureAlgorithm]
                )

                /*
                val decoded = Base64.getDecoder().decode("")
                val publicKey = KeyFactory.getInstance("EC")
                        .generatePublic(X509EncodedKeySpec(decoded))

                sig.initVerify(publicKey)
                sig.update(payloadBytes)
                return sig.verify(signature.signature.toByteArray())

                 */

            }
        }
        return true
    }

    private fun InputStream.unzip(): ExposureFileEntries? {
        var exportBinaryBytes: ByteArray? = null
        var signatureBytes: ByteArray? = null

        ZipInputStream(this).use { zipIn ->
            do {
                val entry = zipIn.nextEntry
                when (entry?.name) {
                    "export.bin" -> exportBinaryBytes = zipIn.readBytes()
                    "export.sig" -> signatureBytes = zipIn.readBytes()
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

    private val algorithmOidToName = mapOf(
            "1.2.840.10045.4.3.2"  to "SHA256withECDSA",
            "1.2.840.10045.4.3.4"  to "SHA512withECDSA"
    )
}

data class ExposureFileEntries(
    val exportBinaryBytes: ByteArray,
    val signatureBytes: ByteArray
)