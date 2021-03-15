package fi.thl.koronahaavi.service

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.nearby.exposurenotification.ReportType
import com.huawei.hmf.tasks.Tasks
import com.huawei.hms.common.ApiException
import com.huawei.hms.contactshield.ContactShieldEngine
import com.huawei.hms.contactshield.Contagiousness
import com.huawei.hms.contactshield.SharedKeysDataMapping
import com.huawei.hms.contactshield.StatusCode
import com.huawei.hms.support.api.client.Status
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.utils.TestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.temporal.ChronoUnit

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class HuaweiContactShieldServiceTest {
    private lateinit var engine: ContactShieldEngine
    private lateinit var appStateRepository: AppStateRepository
    private lateinit var service: HuaweiContactShieldService

    @Before
    fun init() {
        engine = mockk(relaxed = true)
        appStateRepository = mockk(relaxed = true)
        val context: Context = ApplicationProvider.getApplicationContext()

        every { engine.setSharedKeysDataMapping(any()) } returns Tasks.fromResult(null)
        every { engine.getDailySketch(any()) } returns Tasks.fromResult(listOf())

        service = HuaweiContactShieldService(context, engine, appStateRepository)
    }

    @Test
    fun keyMappingNoChanges() {
        val config = TestData.exposureConfiguration()
        every { engine.sharedKeysDataMapping } returns Tasks.fromResult(config.toSharedKeysDataMapping())

        runBlocking {
            service.getDailyExposures(config)
            verify(exactly = 0) { engine.setSharedKeysDataMapping(any()) }
        }
    }

    @Test
    fun keyMappingReportTypeChanged() {
        val config = TestData.exposureConfiguration()
        val mapping = config.toSharedKeysDataMapping()

        every { engine.sharedKeysDataMapping } returns Tasks.fromResult(
                mapping.cloneBuilder().setDefaultReportType(ReportType.SELF_REPORT).build()
        )

        runBlocking {
            service.getDailyExposures(config)
            verify(exactly = 1) { engine.setSharedKeysDataMapping(any()) }
        }
    }

    @Test
    fun keyMappingContagiousnessChanged() {
        val config = TestData.exposureConfiguration()
        val mapping = config.toSharedKeysDataMapping()

        every { engine.sharedKeysDataMapping } returns Tasks.fromResult(
                mapping.cloneBuilder().setDefaultContagiousness(Contagiousness.NONE).build()
        )

        runBlocking {
            service.getDailyExposures(config)
            verify(exactly = 1) { engine.setSharedKeysDataMapping(any()) }
        }
    }

    @Test
    fun keyMappingDaysSinceCreationChanged() {
        val config = TestData.exposureConfiguration()
        val mapping = config.toSharedKeysDataMapping()

        val modifiedMap = mapOf(1 to Contagiousness.HIGH, -1 to Contagiousness.NONE)

        every { engine.sharedKeysDataMapping } returns Tasks.fromResult(
                mapping.cloneBuilder().setDaysSinceCreationToContagiousness(modifiedMap).build()
        )

        runBlocking {
            service.getDailyExposures(config)
            verify(exactly = 1) { engine.setSharedKeysDataMapping(any()) }
        }
    }

    @Test
    fun keyMappingQuotaLimitAsExpected() {
        val config = TestData.exposureConfiguration()
        val mapping = config.toSharedKeysDataMapping()

        // trying to update mapping twice within a week, error ignored
        // since mapping would be applied correctly in future

        every { engine.sharedKeysDataMapping } returns Tasks.fromResult(
                mapping.cloneBuilder().setDefaultContagiousness(Contagiousness.NONE).build()
        )
        every { engine.setSharedKeysDataMapping(any()) } throws
                ApiException(Status(StatusCode.STATUS_APP_QUOTA_LIMITED))

        every { appStateRepository.getLastExposureKeyMappingUpdate() } returns
                Instant.now().minus(2, ChronoUnit.DAYS)

        runBlocking {
            service.getDailyExposures(config)
            verify(exactly = 1) { engine.setSharedKeysDataMapping(any()) }
            verify(exactly = 1) { engine.getDailySketch(any()) }
        }
    }

    @Test(expected = ApiException::class)
    fun keyMappingQuotaLimitError() {
        val config = TestData.exposureConfiguration()
        val mapping = config.toSharedKeysDataMapping()

        // trying to update mapping when previous change over a week ago, error critical

        every { engine.sharedKeysDataMapping } returns Tasks.fromResult(
                mapping.cloneBuilder().setDefaultContagiousness(Contagiousness.NONE).build()
        )
        every { engine.setSharedKeysDataMapping(any()) } throws
                ApiException(Status(StatusCode.STATUS_APP_QUOTA_LIMITED))

        every { appStateRepository.getLastExposureKeyMappingUpdate() } returns
                Instant.now().minus(8, ChronoUnit.DAYS)

        runBlocking {
            service.getDailyExposures(config)
        }
    }

    @Test(expected = ApiException::class)
    fun keyMappingOtherApiError() {
        val config = TestData.exposureConfiguration()
        val mapping = config.toSharedKeysDataMapping()

        every { engine.sharedKeysDataMapping } returns Tasks.fromResult(
                mapping.cloneBuilder().setDefaultContagiousness(Contagiousness.NONE).build()
        )
        every { engine.setSharedKeysDataMapping(any()) } throws
                ApiException(Status(StatusCode.STATUS_INTERNAL_ERROR))

        runBlocking {
            service.getDailyExposures(config)
        }
    }

    private fun SharedKeysDataMapping.cloneBuilder() =
            SharedKeysDataMapping.Builder()
                    .setDefaultReportType(defaultReportType)
                    .setDefaultContagiousness(defaultContagiousness)
                    .setDaysSinceCreationToContagiousness(daysSinceCreationToContagiousness.entries.associate {
                        it.key - 14 to it.value  // map is returned with zero based keys, so need to convert back
                    })
}
