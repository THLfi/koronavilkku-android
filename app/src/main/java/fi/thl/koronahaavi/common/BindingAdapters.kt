package fi.thl.koronahaavi.common

import android.widget.TextView
import androidx.databinding.BindingAdapter
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.device.SystemState

@BindingAdapter("android:text")
fun TextView.fromDeviceState(state: SystemState?) {
    state?.let {
        text = resources.getString(when (it) {
            SystemState.On -> R.string.settings_status_on
            SystemState.Off -> R.string.settings_status_off
            SystemState.Locked -> R.string.settings_status_locked
        })
    }
}
