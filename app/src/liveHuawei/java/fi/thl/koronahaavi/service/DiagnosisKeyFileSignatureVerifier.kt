package fi.thl.koronahaavi.service

import fi.thl.koronahaavi.proto.TEKSignatureList
import java.io.File
import java.io.InputStream
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.zip.ZipInputStream

class DiagnosisKeyFileSignatureVerifier {

    suspend fun verify(files: List<File>, keyBytes: ByteArray): Boolean {

        files.forEach { f ->
            f.inputStream().use { input ->
                val entries = input.unzip() ?: return false

                val tekSignatureList = TEKSignatureList.parseFrom(entries.signatureBytes)
                if (tekSignatureList.signaturesCount != 1) return false
                val tekSignature = tekSignatureList.getSignatures(0)

                val publicKey = KeyFactory.getInstance("EC")
                        .generatePublic(X509EncodedKeySpec(keyBytes))

                val sig = Signature.getInstance(
                        algorithmOidToName[tekSignature.signatureInfo.signatureAlgorithm]
                )

                sig.initVerify(publicKey)
                sig.update(entries.exportBinaryBytes)
                return sig.verify(tekSignature.signature.toByteArray())
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