package fi.thl.koronahaavi.guide

import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import fi.thl.koronahaavi.*
import fi.thl.koronahaavi.di.AppModule
import fi.thl.koronahaavi.di.DatabaseModule
import fi.thl.koronahaavi.di.ExposureNotificationModule
import fi.thl.koronahaavi.di.NetworkModule
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@UninstallModules(AppModule::class, DatabaseModule::class, NetworkModule::class, ExposureNotificationModule::class)
@HiltAndroidTest
class NotificationGuideFragmentTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()

        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            // this calls lifecycle methods that require main thread
            navController.setGraph(R.navigation.main_navigation)
        }

        launchFragmentInHiltContainer<NotificationGuideFragment>(navController = navController)
    }

    @Test
    fun initialState() {
        onView(withId(R.id.text_notification_guide_page_title))
            .checkHasText(R.string.notification_guide_title)

        onView(withId(R.id.page_indicator_notification_guide))
            .check(ViewAssertions.matches(ViewMatchers.hasMinimumChildCount(5)))

        onView(withId(R.id.button_notification_guide_previous)).checkIsGone()
        onView(withId(R.id.button_notification_guide_next)).checkIsVisible()

        onView(allOf(
            withId(R.id.text_notification_guide_page_number), withText("1")
        )).checkIsVisible()
    }

    @Test
    fun moveToNext() {
        onView(withId(R.id.button_notification_guide_next)).perform(click())

        onView(allOf(
            withId(R.id.text_notification_guide_page_number), withText("2")
        )).checkIsVisible()
    }

    @Test
    fun moveToPrevious() {
        onView(withId(R.id.button_notification_guide_next)).perform(click())

        onView(allOf(
            withId(R.id.text_notification_guide_page_number), withText("2")
        )).checkIsVisible()

        onView(withId(R.id.button_notification_guide_previous)).perform(click())

        onView(allOf(
            withId(R.id.text_notification_guide_page_number), withText("1")
        )).checkIsVisible()
    }

    @Test
    fun swipeToNext() {
        onView(withId(R.id.viewpager_notification_guide)).perform(swipeLeft())

        onView(allOf(
            withId(R.id.text_notification_guide_page_number), withText("2")
        )).checkIsVisible()
    }
}