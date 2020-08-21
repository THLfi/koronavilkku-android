package fi.thl.koronahaavi.onboarding

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.rule.ActivityTestRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.checkHasText
import fi.thl.koronahaavi.di.AppModule
import fi.thl.koronahaavi.di.DatabaseModule
import fi.thl.koronahaavi.di.ExposureNotificationModule
import fi.thl.koronahaavi.di.NetworkModule
import fi.thl.koronahaavi.service.ExposureNotificationService
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@UninstallModules(AppModule::class, DatabaseModule::class, NetworkModule::class, ExposureNotificationModule::class)
@HiltAndroidTest
class OnboardingActivityTest {
    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = ActivityTestRule(OnboardingActivity::class.java, false, false)

    @Inject
    lateinit var exposureNotificationService: ExposureNotificationService

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun showsIntro() {
        activityRule.launchActivity(null)

        onView(withId(R.id.text_onboarding_header_title)).checkHasText(R.string.intro_header_title)
    }

}