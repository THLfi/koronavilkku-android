package fi.thl.koronahaavi.service

import android.annotation.SuppressLint
import fi.thl.koronahaavi.utils.MainCoroutineScopeRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.*

class ExposureKeyFileSignatureVerifierTest {
    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    private lateinit var verifier: ExposureKeyFileSignatureVerifier

    @Before
    fun init() {
        verifier = ExposureKeyFileSignatureVerifier()
    }

    @Test
    fun validFiles() {
        val keyFiles = listOf(
            File(javaClass.getResource("/batch_test.zip")!!.file),
            File(javaClass.getResource("/batch_test_2.zip")!!.file)
        )

        runBlocking {
            assertTrue(verifier.verify(keyFiles, getTestKey()))
        }
    }

    @Test
    fun invalidKey() {
        val keyFiles = listOf(
            File(javaClass.getResource("/batch_test.zip")!!.file),
            File(javaClass.getResource("/batch_test_invalid_key.zip")!!.file)
        )

        runBlocking {
            assertFalse(verifier.verify(keyFiles, getTestKey()))
        }
    }

    @SuppressLint("NewApi")
    private fun getTestKey() = Base64.getDecoder().decode(
        "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAENp3lLVciCeIYsZCKc0Ip7cfkWbY12T/cQRRE5l2LSD70JKaFnl3QdyFpImzW/Uv1Jw0KwAp/6BQ6mQ3w3GSFiA=="
    )
}