package fi.thl.koronahaavi.service

import fi.thl.koronahaavi.utils.MainCoroutineScopeRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class DiagnosisKeyFileSignatureVerifierTest {
    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    private lateinit var verifier: DiagnosisKeyFileSignatureVerifier

    @Before
    fun init() {
        verifier = DiagnosisKeyFileSignatureVerifier()
    }

    @Test
    fun test() {
        val keyFile = File(javaClass.getResource("/batch_test.zip")!!.file)

        runBlocking {
            assertTrue(verifier.verify(listOf(keyFile)))
        }
    }

}