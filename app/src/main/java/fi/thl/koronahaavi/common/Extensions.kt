package fi.thl.koronahaavi.common

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fi.thl.koronahaavi.R
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

    activity?.let {
        if (intent.resolveActivity(it.packageManager) != null) {
            startActivity(intent)
        }
        else {
            // device does not have any apps that can open a web link, maybe
            // browser app disabled, or profile restrictions in place
            MaterialAlertDialogBuilder(it)
                .setTitle(R.string.open_link_failed_title)
                .setMessage(R.string.open_link_failed_message)
                .setPositiveButton(R.string.all_ok, null)
                .show()
        }
    }
}

fun Fragment.openNotificationSettings() {
    val intent = Intent().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            context?.let {
                putExtra(Settings.EXTRA_APP_PACKAGE, it.packageName)
            }
        }
        else {
            action = "android.settings.APP_NOTIFICATION_SETTINGS"
            context?.let {
                putExtra("app_package", it.packageName)
                putExtra("app_uid", it.applicationInfo.uid)
            }
        }
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

/**
 * Creates a MediatorLiveData that is updated with given block
 */
fun <T, S, R> LiveData<T>.combineWith(
        liveData: LiveData<S>,
        block: (T?, S?) -> R
): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) {
        result.value = block(this.value, liveData.value)
    }
    result.addSource(liveData) {
        result.value = block(this.value, liveData.value)
    }
    return result
}

fun <T, S, U, R> LiveData<T>.combineWith(
        liveData1: LiveData<S>,
        liveData2: LiveData<U>,
        block: (T?, S?, U?) -> R
): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) {
        result.value = block(this.value, liveData1.value, liveData2.value)
    }
    result.addSource(liveData1) {
        result.value = block(this.value, liveData1.value, liveData2.value)
    }
    result.addSource(liveData2) {
        result.value = block(this.value, liveData1.value, liveData2.value)
    }
    return result
}