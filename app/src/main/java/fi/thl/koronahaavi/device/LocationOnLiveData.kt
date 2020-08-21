package fi.thl.koronahaavi.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.LiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

class LocationOnLiveData @Inject constructor (
    @ApplicationContext private val context: Context
) : LiveData<Boolean>() {

    override fun onActive() {
        super.onActive()

        // set initial value since receiver will only get changes
        updateValue()

        Timber.d("Register location mode changed receiver")
        context.registerReceiver(receiver, IntentFilter(LocationManager.MODE_CHANGED_ACTION))
    }

    override fun onInactive() {
        Timber.d("Unregister location mode changed receiver")
        context.unregisterReceiver(receiver)
        super.onInactive()
    }

    private fun updateValue() {
        value = locationManager.let {
            LocationManagerCompat.isLocationEnabled(it)
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == LocationManager.MODE_CHANGED_ACTION) {
                // Not all versions include the enabled value in the intent -> to keep things simple always call updateValue().
                updateValue()
            }
        }
    }

    private val locationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
}
