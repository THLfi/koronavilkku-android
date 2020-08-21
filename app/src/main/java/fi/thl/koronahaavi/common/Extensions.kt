package fi.thl.koronahaavi.common

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import fi.thl.koronahaavi.settings.GuideFragment
import timber.log.Timber

fun Fragment.hideKeyboard() = view?.let { activity?.hideKeyboard(it) }

fun Fragment.getDrawable(@DrawableRes id: Int) = ContextCompat.getDrawable(requireContext(), id)

fun Context.hideKeyboard(view: View) = getInputMethodManager().hideSoftInputFromWindow(view.windowToken, 0)

fun Context.getInputMethodManager() = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager

fun Fragment.openLink(url: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(url)
    }
    startActivity(intent)
}

fun FragmentActivity.openGuide() {
    GuideFragment().show(supportFragmentManager, "guide")
}

@ColorInt
fun Context.themeColor(@AttrRes themeAttrId: Int): Int {
    return obtainStyledAttributes(
        intArrayOf(themeAttrId)
    ).use {
        it.getColor(0, Color.GREEN)
    }
}

fun NavController.navigateSafe(directions: NavDirections) {
    // Prevents "IllegalArgumentException: navigation destination is unknown" error when navigate
    // is called during navigation, by for example clicking button multiple times. This also ignores the
    // call when using wrong directions class, but at least logs a warning
    currentDestination?.getAction(directions.actionId)?.apply {
        navigate(directions)
    } ?: Timber.w("Ignoring an invalid navigation action")
}

fun View.fadeGone(duration: Long = 1000) {
    animate()
        .alpha(0f)
        .setDuration(duration)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                visibility = View.GONE
            }
        })
}