package fi.thl.koronahaavi

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.*
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.jraska.livedata.test
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.di.AppModule
import fi.thl.koronahaavi.di.ExposureNotificationModule
import fi.thl.koronahaavi.di.NetworkModule
import fi.thl.koronahaavi.service.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

@UninstallModules(AppModule::class, NetworkModule::class, ExposureNotificationModule::class, TestRepositoryModule::class)
@HiltAndroidTest
class AppShutdownWorkerTest {
    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val taskExecutorRule = InstantTaskExecutorRule()

    @Inject
    lateinit var backendService: BackendService

    @Inject
    lateinit var workDispatcher: WorkDispatcher

    @Inject
    lateinit var appStateRepository: AppStateRepository

    @Inject
    lateinit var exposureNotificationService: ExposureNotificationService

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Before
    fun setup() {
        hiltRule.inject()

        val config = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .setWorkerFactory(workerFactory)
            .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(
            InstrumentationRegistry.getInstrumentation().targetContext,
            config
        )

        appStateRepository.setAppShutdown(false)
    }

    @Test
    fun workerDetectsShutdown() {
        runBlocking {
            exposureNotificationService.enable()
        }
        (backendService as? FakeBackendService)?.endOfLifeReached = true

        val workLiveData = DiagnosisKeyUpdateWorker.runOnce(
            InstrumentationRegistry.getInstrumentation().targetContext
        )

        workLiveData.test()
            .awaitValue(10, SECONDS).assertValue {
                it.state == WorkInfo.State.RUNNING
            }
            .awaitNextValue(10, SECONDS).assertValue {
                it.state == WorkInfo.State.SUCCEEDED
            }

        assertTrue(appStateRepository.appShutdown().value)
    }

    @Test
    fun workerDetectsShutdownWhenENDisabled() {
        runBlocking {
            exposureNotificationService.disable()
        }
        (backendService as? FakeBackendService)?.endOfLifeReached = true

        val workLiveData = DiagnosisKeyUpdateWorker.runOnce(
            InstrumentationRegistry.getInstrumentation().targetContext
        )

        workLiveData.test()
            .awaitValue(10, SECONDS).assertValue {
                it.state == WorkInfo.State.RUNNING
            }
            .awaitNextValue(10, SECONDS).assertValue {
                it.state == WorkInfo.State.SUCCEEDED
            }

        assertTrue(appStateRepository.appShutdown().value)
    }
}