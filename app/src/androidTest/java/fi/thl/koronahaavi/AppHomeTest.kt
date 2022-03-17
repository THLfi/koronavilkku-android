package fi.thl.koronahaavi

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import fi.thl.koronahaavi.data.*
import fi.thl.koronahaavi.di.AppModule
import fi.thl.koronahaavi.di.ExposureNotificationModule
import fi.thl.koronahaavi.di.NetworkModule
import fi.thl.koronahaavi.settings.UserPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.ZonedDateTime
import java.util.*
import javax.inject.Inject

/**
 * Test app home screen with database and repository
 */
@UninstallModules(AppModule::class, NetworkModule::class, ExposureNotificationModule::class, TestRepositoryModule::class)
@HiltAndroidTest
class AppHomeTest {
    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = ActivityTestRule(MainActivity::class.java, false, false)

    @Inject
    lateinit var appStateRepository: AppStateRepository

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var exposureDao: ExposureDao

    @Inject
    lateinit var keyGroupTokenDao: KeyGroupTokenDao

    @Before
    fun setup() {
        hiltRule.inject()

        WorkManagerTestInitHelper.initializeTestWorkManager(
            InstrumentationRegistry.getInstrumentation().targetContext,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        )

        // disable power disable prompts
        userPreferences.powerOptimizationDisableAllowed = false

        runBlocking {
            exposureDao.deleteAll()
            keyGroupTokenDao.deleteAll()
        }

        appStateRepository.setDiagnosisKeysSubmitted(false)
        appStateRepository.setOnboardingComplete(true)
        appStateRepository.setAppShutdown(false)
    }

    @Test
    fun legacyExposure() {
        activityRule.launchActivity(null)
        onView(withId(R.id.image_home_app_status)).checkIsDisplayed()
        onView(withId(R.id.button_home_exposure_instructions)).checkIsGone()

        runBlocking {
            val token = "test"
            val exposure = insertTokenAndExposure(token)

            // this update is done when exposure data received
            keyGroupTokenDao.insert(KeyGroupToken(
                    token = token,
                    matchedKeyCount = 1,
                    maximumRiskScore = exposure.totalRiskScore
            ))

            delay(1000) // data binding
            onView(withId(R.id.button_home_exposure_instructions)).checkIsDisplayed()
            onView(withId(R.id.text_home_exposure_notification_count)).checkIsDisplayed()
            onView(withId(R.id.text_home_exposure_notification_count)).checkHasText(R.string.home_notifications_unknown)
        }
    }

    @Test
    fun singleExposureNotification() {
        activityRule.launchActivity(null)
        onView(withId(R.id.image_home_app_status)).checkIsDisplayed()
        onView(withId(R.id.button_home_exposure_instructions)).checkIsGone()

        runBlocking {
            val token = "test"
            val exposure = insertTokenAndExposure(token)

            keyGroupTokenDao.insert(KeyGroupToken(
                    token = token,
                    matchedKeyCount = 1,
                    maximumRiskScore = exposure.totalRiskScore,
                    exposureCount = 1,
                    latestExposureDate = exposure.detectedDate
            ))

            delay(1000) // data binding
            onView(withId(R.id.button_home_exposure_instructions)).checkIsDisplayed()
            onView(withId(R.id.text_home_exposure_notification_count)).checkIsDisplayed()
            onView(withId(R.id.text_home_exposure_notification_count)).checkHasText("1")
        }
    }

    @Test
    fun multipleExposureNotifications() {
        activityRule.launchActivity(null)
        onView(withId(R.id.image_home_app_status)).checkIsDisplayed()
        onView(withId(R.id.button_home_exposure_instructions)).checkIsGone()

        runBlocking {
            repeat(2) {
                val token = UUID.randomUUID().toString()
                val exposure = insertTokenAndExposure(token)

                keyGroupTokenDao.insert(KeyGroupToken(
                        token = token,
                        matchedKeyCount = 1,
                        maximumRiskScore = exposure.totalRiskScore,
                        exposureCount = 1,
                        latestExposureDate = exposure.detectedDate
                ))
            }

            delay(1000) // data binding
            onView(withId(R.id.button_home_exposure_instructions)).checkIsDisplayed()
            onView(withId(R.id.text_home_exposure_notification_count)).checkIsDisplayed()
            onView(withId(R.id.text_home_exposure_notification_count)).checkHasText("2")
        }
    }

    @Test
    fun dayExposureNotification() {
        activityRule.launchActivity(null)
        onView(withId(R.id.image_home_app_status)).checkIsDisplayed()
        onView(withId(R.id.button_home_exposure_instructions)).checkIsGone()

        runBlocking {
            val detected = ZonedDateTime.now().minusDays(4)
            exposureDao.insert(Exposure(
                    detectedDate = detected,
                    createdDate = ZonedDateTime.now(),
                    totalRiskScore = 1000
            ))

            keyGroupTokenDao.insert(KeyGroupToken(
                    token = UUID.randomUUID().toString(),
                    dayCount = 1,
                    latestExposureDate = detected
            ))

            delay(1000) // data binding
            onView(withId(R.id.button_home_exposure_instructions)).checkIsDisplayed()
            onView(withId(R.id.text_home_exposure_notification_count)).checkIsDisplayed()
            onView(withId(R.id.text_home_exposure_notification_count)).checkHasText("1")
        }
    }

    // insert the data that is created when diagnosis file is downloaded and exposure detected
    private suspend fun insertTokenAndExposure(token: String): Exposure {
        keyGroupTokenDao.insert(KeyGroupToken(token))

        val exposure = Exposure(
                detectedDate = ZonedDateTime.now().minusDays(4),
                createdDate = ZonedDateTime.now(),
                totalRiskScore = 200
        )
        exposureDao.insert(exposure)
        return exposure
    }
}