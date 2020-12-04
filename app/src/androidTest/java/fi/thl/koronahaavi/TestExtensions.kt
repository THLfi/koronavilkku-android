package fi.thl.koronahaavi

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ListView
import android.widget.ScrollView
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.core.util.Preconditions
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario.EmptyFragmentActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher
import org.hamcrest.Matchers

/**
 * -- copied from google architecture-samples project hilt branch
 *
 * launchFragmentInContainer from the androidx.fragment:fragment-testing library
 * is NOT possible to use right now as it uses a hardcoded Activity under the hood
 * (i.e. [EmptyFragmentActivity]) which is not annotated with @AndroidEntryPoint.
 *
 * As a workaround, use this function that is equivalent. It requires you to add
 * [HiltTestActivity] in the debug folder and include it in the debug AndroidManifest.xml file
 * as can be found in this project.
 */
inline fun <reified T : Fragment> launchFragmentInHiltContainer(
    fragmentArgs: Bundle? = null,
    @StyleRes themeResId: Int = R.style.FragmentScenarioEmptyFragmentActivityTheme,
    navController: NavController? = null,
    crossinline action: Fragment.() -> Unit = {}
) {
    val startActivityIntent = Intent.makeMainActivity(
        ComponentName(
            ApplicationProvider.getApplicationContext(),
            HiltTestActivity::class.java
        )
    ).putExtra(EmptyFragmentActivity.THEME_EXTRAS_BUNDLE_KEY, themeResId)

    ActivityScenario.launch<HiltTestActivity>(startActivityIntent).onActivity { activity ->
        val fragment: Fragment = activity.supportFragmentManager.fragmentFactory.instantiate(
            Preconditions.checkNotNull(T::class.java.classLoader),
            T::class.java.name
        )
        fragment.arguments = fragmentArgs

        navController?.let {
            fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                if (viewLifecycleOwner != null) {
                    // The fragment’s view has just been created
                    Navigation.setViewNavController(fragment.requireView(), it)
                }
            }
        }

        activity.supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, fragment, "")
            .commitNow()

        fragment.action()
    }
}

fun ViewInteraction.checkIsVisible() {
    check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
}

fun ViewInteraction.checkIsInvisible() {
    check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE)))
}

fun ViewInteraction.checkIsGone() {
    check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
}

fun ViewInteraction.checkIsDisplayed() {
    check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
}

fun ViewInteraction.checkIsNotDisplayed() {
    check(ViewAssertions.matches(Matchers.not(ViewMatchers.isDisplayed())))
}

fun ViewInteraction.checkIsEnabled() {
    check(ViewAssertions.matches(ViewMatchers.isEnabled()))
}

fun ViewInteraction.checkIsDisabled() {
    check(ViewAssertions.matches(Matchers.not(ViewMatchers.isEnabled())))
}

fun ViewInteraction.checkHasEmptyText() {
    check(ViewAssertions.matches(ViewMatchers.withText("")))
}

fun ViewInteraction.checkHasText(text: String) {
    check(ViewAssertions.matches(ViewMatchers.withText(text)))
}

fun ViewInteraction.checkHasText(@StringRes resId: Int) {
    check(ViewAssertions.matches(ViewMatchers.withText(resId)))
}

// this adds scrollTo support for NestedScrollView
class NestedScrollViewAction(scrollToAction: ViewAction) : ViewAction by scrollToAction {
    override fun getConstraints(): Matcher<View> {
        return Matchers.allOf(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
            ViewMatchers.isDescendantOfA(Matchers.anyOf(
                ViewMatchers.isAssignableFrom(NestedScrollView::class.java),
                ViewMatchers.isAssignableFrom(ScrollView::class.java),
                ViewMatchers.isAssignableFrom(HorizontalScrollView::class.java),
                ViewMatchers.isAssignableFrom(ListView::class.java))))
    }
}