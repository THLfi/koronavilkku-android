package fi.thl.koronahaavi.service

import fi.thl.koronahaavi.di.AppModule
import fi.thl.koronahaavi.di.NetworkModule
import fi.thl.koronahaavi.service.BackendService.NumericBoolean
import fi.thl.koronahaavi.utils.TestData
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackendServiceTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var backendService: BackendService

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()

        // creating instance through DI modules to verify their setup
        val httpClient = AppModule.provideHttpClient()
        val retrofit = AppModule.provideRetrofit(httpClient, mockServer.url("/").toString())
        backendService = NetworkModule.provideBackendService(retrofit)
    }

    @After
    fun teardown() {
        mockServer.shutdown()
    }

    @Test
    fun testSendKeys() {
        val payload = DiagnosisKeyList(
                keys = listOf(
                        TestData.diagnosisKey(),
                        TestData.diagnosisKey()
                ),
                visitedCountries = mapOf(
                        "aa" to NumericBoolean.FALSE,
                        "bb" to NumericBoolean.FALSE,
                        "cc" to NumericBoolean.TRUE
                ),
                consentToShareWithEfgs = NumericBoolean.FALSE
        )

        mockServer.enqueue(MockResponse())

        runBlocking {
            backendService.sendKeys(
                    token = "test_token",
                    data = payload,
                    isFake = NumericBoolean.FALSE
            )

            val req = mockServer.takeRequest()
            val body = req.body.readUtf8()

            assertTrue(body.contains("\"consentToShareWithEfgs\":0"))
            assertTrue(body.contains("\"aa\":0,\"bb\":0,\"cc\":1"))
            assertEquals("0", req.getHeader("KV-Fake-Request"))
            assertEquals("test_token", req.getHeader("KV-Publish-Token"))

            // fake request modifies header
            mockServer.enqueue(MockResponse())
            backendService.sendKeys(
                token = "test_token",
                data = payload,
                isFake = NumericBoolean.TRUE
            )

            val fakeReq = mockServer.takeRequest()
            assertEquals("1", fakeReq.getHeader("KV-Fake-Request"))
        }
    }
}