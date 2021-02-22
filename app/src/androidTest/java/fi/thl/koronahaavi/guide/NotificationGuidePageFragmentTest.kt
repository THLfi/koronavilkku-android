package fi.thl.koronahaavi.guide

import androidx.core.os.bundleOf
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.checkHasText
import fi.thl.koronahaavi.checkIsGone
import fi.thl.koronahaavi.di.AppModule
import fi.thl.koronahaavi.di.DatabaseModule
import fi.thl.koronahaavi.di.ExposureNotificationModule
import fi.thl.koronahaavi.di.NetworkModule
import fi.thl.koronahaavi.launchFragmentInHiltContainer
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@UninstallModules(AppModule::class, DatabaseModule::class, NetworkModule::class, ExposureNotificationModule::class)
@HiltAndroidTest
class NotificationGuidePageFragmentTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun pageWithoutTitle() {
        launchFragmentInHiltContainer<NotificationGuidePageFragment>(fragmentArgs = bundleOf(
                NotificationGuidePageFragment.ARG_CURRENT_PAGE_ID to 2,
                NotificationGuidePageFragment.ARG_PAGE_COUNT_ID to 5,
                NotificationGuidePageFragment.ARG_BODY_TEXT_ID to R.string.notification_guide_text_1,
                NotificationGuidePageFragment.ARG_IMAGE_ID to R.drawable.notification_guide_1
        ))

        onView(withId(R.id.text_notification_guide_page_body))
                .checkHasText(R.string.notification_guide_text_1)

        onView(withId(R.id.text_notification_guide_page_number))
                .checkHasText("2")

        onView(withId(R.id.text_notification_guide_page_title)).checkIsGone()
    }

    @Test
    fun pageWithTitle() {
        launchFragmentInHiltContainer<NotificationGuidePageFragment>(fragmentArgs = bundleOf(
            NotificationGuidePageFragment.ARG_CURRENT_PAGE_ID to 1,
            NotificationGuidePageFragment.ARG_PAGE_COUNT_ID to 5,
            NotificationGuidePageFragment.ARG_BODY_TEXT_ID to R.string.notification_guide_text_1,
            NotificationGuidePageFragment.ARG_TITLE_TEXT_ID to R.string.notification_guide_title,
            NotificationGuidePageFragment.ARG_IMAGE_ID to R.drawable.notification_guide_1
        ))

        onView(withId(R.id.text_notification_guide_page_body))
            .checkHasText(R.string.notification_guide_text_1)

        onView(withId(R.id.text_notification_guide_page_number))
            .checkHasText("1")

        onView(withId(R.id.text_notification_guide_page_title))
            .checkHasText(R.string.notification_guide_title)
    }

}