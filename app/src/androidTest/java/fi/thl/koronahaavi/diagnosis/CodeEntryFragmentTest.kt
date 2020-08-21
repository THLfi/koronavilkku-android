package fi.thl.koronahaavi.diagnosis

import android.content.Context
import androidx.navigation.NavController
import androidx.test.espresso.Espresso.onView
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.withId
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.checkIsDisabled
import fi.thl.koronahaavi.checkIsEnabled
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
class CodeEntryFragmentTest {
    private lateinit var context: Context
    private lateinit var navController: NavController

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        navController = TestNavHostController(context).apply {
            setGraph(R.navigation.main_navigation)
        }
    }

    @Test
    fun codeRequiredToSend() {
        launchFragmentInHiltContainer<CodeEntryFragment>(
            fragmentArgs = CodeEntryFragmentArgs().toBundle(),
            themeResId = R.style.Theme_Vilkku_NoActionBar,
            navController = navController)

        onView(withId(R.id.button_code_entry_submit)).checkIsDisabled()

        onView(withId(R.id.text_input_edit_code_entry)).perform(typeText("1234"))

        onView(withId(R.id.button_code_entry_submit)).checkIsEnabled()
    }
}