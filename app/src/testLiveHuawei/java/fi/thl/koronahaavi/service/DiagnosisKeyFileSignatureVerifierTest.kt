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
        val keyFile = File(javaClass.getResource("/batch_test.zip")!!.file)

        val keyBytes = Base64.getDecoder().decode(
                "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAENp3lLVciCeIYsZCKc0Ip7cfkWbY12T/cQRRE5l2LSD70JKaFnl3QdyFpImzW/Uv1Jw0KwAp/6BQ6mQ3w3GSFiA=="
        )

        runBlocking {
            assertTrue(verifier.verify(listOf(keyFile), keyBytes))
        }
    }

}