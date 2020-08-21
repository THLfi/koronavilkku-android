package fi.thl.koronahaavi.onboarding

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.*
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import fi.thl.koronahaavi.*
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.di.AppModule
import fi.thl.koronahaavi.di.DatabaseModule
import fi.thl.koronahaavi.di.ExposureNotificationModule
import fi.thl.koronahaavi.di.NetworkModule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@UninstallModules(AppModule::class, DatabaseModule::class, NetworkModule::class, ExposureNotificationModule::class)
@HiltAndroidTest
class AcceptTermsFragmentTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var appStateRepository: AppStateRepository

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun checkboxesRequiredToEnable() {
        launchFragmentInHiltContainer<AcceptTermsFragment>(themeResId = R.style.Theme_Vilkku_NoActionBar)
        onView(withId(R.id.button_accept_terms_scroll)).perform(click())

        onView(withId(R.id.button_enable_service)).checkIsDisabled()

        onView(withId(R.id.tos_checkbox)).perform(click())
        onView(withId(R.id.checkbox_voluntary_activation)).perform(click())

        onView(withId(R.id.button_enable_service)).checkIsDisplayed()
        onView(withId(R.id.button_enable_service)).checkIsEnabled()
    }
}