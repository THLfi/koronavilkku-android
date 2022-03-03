package fi.thl.koronahaavi

import android.content.Intent
import android.net.Uri
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.ExposureRepository
import fi.thl.koronahaavi.data.LocaleString
import fi.thl.koronahaavi.data.SettingsRepository
import fi.thl.koronahaavi.di.AppModule
import fi.thl.koronahaavi.di.DatabaseModule
import fi.thl.koronahaavi.di.ExposureNotificationModule
import fi.thl.koronahaavi.di.NetworkModule
import fi.thl.koronahaavi.service.BackendService
import fi.thl.koronahaavi.service.ExposureConfigurationData
import fi.thl.koronahaavi.service.ExposureNotificationService
import fi.thl.koronahaavi.service.LabeledStringValue
import fi.thl.koronahaavi.settings.UserPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
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

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var backendService: BackendService

    private val endOfLifeStatistics = listOf(
        LabeledStringValue(
            label = LocaleString("test 1", en = null, sv = null),
            value = "234"
        ),
        LabeledStringValue(
            label = LocaleString("test 2", en = null, sv = null),
            value = "567"
        )
    )

    @Before
    fun setup() {
        hiltRule.inject()

        WorkManagerTestInitHelper.initializeTestWorkManager(
            InstrumentationRegistry.getInstrumentation().targetContext,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        )

        // disable power disable prompts
        userPreferences.powerOptimizationDisableAllowed = false

        appStateRepository.setAppShutdown(false)
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

            onView(withId(R.id.text_choice_footer)).checkHasText(R.string.share_consent_code_saved)
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

            onView(withId(R.id.text_choice_footer)).checkHasText(R.string.share_consent_code_saved)
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

    @Test
    fun shareDiagnosisFlow() {
        appStateRepository.setDiagnosisKeysSubmitted(false)
        appStateRepository.setOnboardingComplete(true)

        runBlocking {
            exposureNotificationService.enable()

            // get config from fake backend, since required for country list
            settingsRepository.updateExposureConfiguration(backendService.getConfiguration())

            activityRule.launchActivity(null)

            // select diagnosis tab
            onView(allOf(
                withText(R.string.diagnosis_title),
                isDescendantOfA(withId(R.id.nav_view)),
                isDisplayed()
            )).perform(click())

            onView(withId(R.id.text_diagnosis_title)).checkIsDisplayed()

            // start share diagnosis
            onView(withId(R.id.button_diagnosis_start)).perform(scrollTo(), click())
            onView(withId(R.id.text_choice_header)).checkHasText(R.string.share_consent_header)

            // select EU share and continue
            onView(withId(R.id.radio_1)).perform(click())
            onView(withId(R.id.button_choice_continue)).perform(click())
            onView(withId(R.id.text_choice_header)).checkHasText(R.string.travel_disclosure_header)

            // select travel and continue
            onView(withId(R.id.radio_2)).perform(click())
            onView(withId(R.id.button_choice_continue)).perform(click())
            onView(withId(R.id.recyclerview_country_selection)).checkIsDisplayed()

            // select two countries
            onView(withId(R.id.recyclerview_country_selection)).perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()),
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(2, click())
            )
            onView(withId(R.id.button_country_selection_continue)).perform(
                NestedScrollViewAction(scrollTo()),
                click()
            )
            onView(withId(R.id.layout_summary_consent_countries)).checkIsDisplayed()

            // check the boxes and continue
            onView(withText(R.string.summary_consent_accept_use)).perform(scrollTo(), click())
            onView(withText(R.string.summary_consent_accept_share)).perform(scrollTo(), click())
            onView(withId(R.id.button_summary_consent_continue)).perform(scrollTo(), click())
            onView(withId(R.id.text_input_edit_code_entry)).checkIsDisplayed()

            // code required to send
            onView(withId(R.id.button_code_entry_submit)).checkIsDisabled()
            onView(withId(R.id.text_input_edit_code_entry)).perform(typeText("1234"))
            onView(withId(R.id.button_code_entry_submit)).checkIsEnabled()

            // send
            onView(withId(R.id.button_code_entry_submit)).perform(click())
            onView(withId(R.id.text_diagnosis_complete_title)).checkIsDisplayed()

            // close final screen
            onView(withId(R.id.button_diagnosis_complete_continue)).perform(scrollTo(), click())
            onView(withId(R.id.image_home_app_status)).checkIsDisplayed()
        }
    }

    @Test
    fun shareDiagnosisFlowNoCountryData() {
        appStateRepository.setDiagnosisKeysSubmitted(false)
        appStateRepository.setOnboardingComplete(true)

        runBlocking {
            exposureNotificationService.enable()

            // clear participating countries to simulate no data
            settingsRepository.updateExposureConfiguration(backendService.getConfiguration().copy(
                availableCountries = null
            ))

            activityRule.launchActivity(null)

            // select diagnosis tab
            onView(allOf(
                    withText(R.string.diagnosis_title),
                    isDescendantOfA(withId(R.id.nav_view)),
                    isDisplayed()
            )).perform(click())

            onView(withId(R.id.text_diagnosis_title)).checkIsDisplayed()

            // start share diagnosis
            onView(withId(R.id.button_diagnosis_start)).perform(scrollTo(), click())
            onView(withId(R.id.text_choice_header)).checkHasText(R.string.share_consent_header)

            // select EU share and continue
            onView(withId(R.id.radio_1)).perform(click())
            onView(withId(R.id.button_choice_continue)).perform(click())

            // verify that navigated directly to summary
            onView(withText(R.string.summary_consent_header)).checkIsDisplayed()
            onView(withText(R.string.summary_consent_travel_header)).checkIsGone()
            onView(withId(R.id.layout_summary_consent_countries)).checkIsGone()
        }
    }

    @Test
    fun showsShutdownAtStart() {
        appStateRepository.setOnboardingComplete(true)
        setAppShutdownConfig()

        activityRule.launchActivity(null)

        onView(withText(endOfLifeStatistics[0].value)).checkIsDisplayed()
    }

    @Test
    fun showsShutdownAtStartDeeplink() {
        appStateRepository.setOnboardingComplete(true)
        setAppShutdownConfig()

        activityRule.launchActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://koronavilkku/i?12345"))
        )

        onView(withText(endOfLifeStatistics[0].value)).checkIsDisplayed()
    }

    @Test
    fun showsShutdownFromMain() {
        appStateRepository.setOnboardingComplete(true)

        activityRule.launchActivity(null)
        onView(withId(R.id.image_home_app_status)).checkIsDisplayed()

        setAppShutdownConfig()

        onView(withText(endOfLifeStatistics[0].value)).checkIsDisplayed()
    }

    private fun setAppShutdownConfig() {
        runBlocking {
            settingsRepository.updateExposureConfiguration(backendService.getConfiguration().copy(
                endOfLifeStatistics = endOfLifeStatistics
            ))
        }
        appStateRepository.setAppShutdown(true)
    }
}