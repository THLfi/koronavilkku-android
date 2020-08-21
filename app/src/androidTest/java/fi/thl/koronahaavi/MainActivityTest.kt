package fi.thl.koronahaavi

import android.content.Intent
import android.net.Uri
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.ExposureRepository
import fi.thl.koronahaavi.di.AppModule
import fi.thl.koronahaavi.di.DatabaseModule
import fi.thl.koronahaavi.di.ExposureNotificationModule
import fi.thl.koronahaavi.di.NetworkModule
import fi.thl.koronahaavi.service.ExposureNotificationService
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import javax.inject.Singleton

@UninstallModules(AppModule::class, DatabaseModule::class, NetworkModule::class, ExposureNotificationModule::class)
@HiltAndroidTest
class MainActivityTest {
    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = ActivityTestRule(MainActivity::class.java, false, false)

    @Inject
    lateinit var appStateRepository: AppStateRepository

    @Inject
    lateinit var exposureNotificationService: ExposureNotificationService

    @Before
    fun setup() {
        hiltRule.inject()

        WorkManagerTestInitHelper.initializeTestWorkManager(
            InstrumentationRegistry.getInstrumentation().targetContext,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        )
    }

    @Test
    fun showsOnboarding() {
        appStateRepository.setOnboardingComplete(false)
        activityRule.launchActivity(null)
        onView(withId(R.id.image_onboarding_header_image)).checkIsDisplayed()
    }

    @Test
    fun showsMain() {
        appStateRepository.setOnboardingComplete(true)
        activityRule.launchActivity(null)
        onView(withId(R.id.image_home_app_status)).checkIsDisplayed()
    }

    @Test
    fun deepLinkToCode() {
        appStateRepository.setDiagnosisKeysSubmitted(false)
        appStateRepository.setOnboardingComplete(true)

        runBlocking {
            exposureNotificationService.enable()

            val code = "456789"
            activityRule.launchActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://koronavilkku/i?$code"))
            )
            onView(withId(R.id.text_input_edit_code_entry)).checkHasText(code)
        }
    }

    @Test
    fun deepLinkENDisabled() {
        appStateRepository.setDiagnosisKeysSubmitted(false)
        appStateRepository.setOnboardingComplete(true)

        runBlocking {
            exposureNotificationService.disable()

            activityRule.launchActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://koronavilkku/i?456789"))
            )
            onView(withId(R.id.text_diagnosis_en_disabled)).checkIsDisplayed()
        }
    }

    @Test
    fun deepLinkAppLocked() {
        appStateRepository.setDiagnosisKeysSubmitted(true)
        appStateRepository.setOnboardingComplete(true)

        runBlocking {
            exposureNotificationService.enable()

            activityRule.launchActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://koronavilkku/i?456789"))
            )
            onView(withId(R.id.text_diagnosis_locked_title)).checkIsDisplayed()
        }
    }

    @Test
    fun deepLinkIgnoresValue() {
        appStateRepository.setDiagnosisKeysSubmitted(false)
        appStateRepository.setOnboardingComplete(true)

        runBlocking {
            exposureNotificationService.enable()

            val code = "456789"
            activityRule.launchActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://koronavilkku/i?$code=should_be_ignored"))
            )
            onView(withId(R.id.text_input_edit_code_entry)).checkHasText(code)
        }
    }

    @Test
    fun deepLinkOnboardingNotDone() {
        appStateRepository.setOnboardingComplete(false)

        runBlocking {
            activityRule.launchActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://koronavilkku/i?12345"))
            )
            onView(withId(R.id.image_onboarding_header_image)).checkIsDisplayed()
        }
    }
}