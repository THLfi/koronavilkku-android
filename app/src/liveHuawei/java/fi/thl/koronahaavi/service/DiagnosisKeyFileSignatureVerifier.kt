package fi.thl.koronahaavi.service

import fi.thl.koronahaavi.proto.TEKSignatureList
import java.io.File
import java.io.InputStream
import java.security.Signature
import java.util.zip.ZipInputStream

class DiagnosisKeyFileSignatureVerifier {

    suspend fun verify(files: List<File>) {

        files.forEach { f ->

            f.inputStream().use { input ->
                val entries = input.unzip()

                val payloadBytes = entries.exportBinaryBytes.copyOfRange(16, entries.exportBinaryBytes.size)

                val signatureList = TEKSignatureList.parseFrom(entries.signatureBytes)
                val signature = signatureList.getSignatures(0)

                val sig = Signature.getInstance(
                        algorithmOidToName[signature.signatureInfo.signatureAlgorithm]
                )

                // todo need key
                // sig.initVerify()
                //sig.update(payloadBytes)
                //sig.verify(signature.signature.toByteArray())
            }
        }

        /*
                //if (!String(exportBinaryBytes.copyOfRange(0, 16)).startsWith("EK Export v1")) return false

                //val export = TemporaryExposureKeyExport.parseFrom(payloadBytes)
                //if (export.signatureInfosCount != 1) return false
                //val signatureInfo = export.getSignatureInfos(0)
         */
    }

    private fun InputStream.unzip(): ExposureFileEntries {
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

            exportBinaryBytes?.let { bin ->
                signatureBytes?.let { sig ->
                    return ExposureFileEntries(bin, sig)
                }
            } ?:
            throw Exception("Could not find entries from export archive")
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