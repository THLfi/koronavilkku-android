package fi.thl.koronahaavi.service

import android.annotation.SuppressLint
import fi.thl.koronahaavi.utils.MainCoroutineScopeRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.*

class DiagnosisKeyFileSignatureVerifierTest {
    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    private lateinit var verifier: DiagnosisKeyFileSignatureVerifier

    @Before
    fun init() {
        verifier = DiagnosisKeyFileSignatureVerifier()
    }

    @SuppressLint("NewApi")
    @Test
    fun test() {
        val keyFiles = listOf(
            File(javaClass.getResource("/batch_test.zip")!!.file),
            File(javaClass.getResource("/batch_test_2.zip")!!.file)
        )

        val testKey = "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAENp3lLVciCeIYsZCKc0Ip7cfkWbY12T/cQRRE5l2LSD70JKaFnl3QdyFpImzW/Uv1Jw0KwAp/6BQ6mQ3w3GSFiA=="
        val keyBytes = Base64.getDecoder().decode(testKey)

        runBlocking {
            assertTrue(verifier.verify(keyFiles, keyBytes))
        }
    }

}