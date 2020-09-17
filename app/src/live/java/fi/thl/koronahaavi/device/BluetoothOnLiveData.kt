package fi.thl.koronahaavi.device

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.LiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

class BluetoothOnLiveData @Inject constructor (
    @ApplicationContext private val context: Context
) : LiveData<Boolean>() {

    override fun onActive() {
        super.onActive()

        // set initial value since receiver will only get changes
        value = try {
            BluetoothAdapter.getDefaultAdapter()?.isEnabled ?: false
        } catch (e: Exception) {
            Timber.e(e, "Error checking adapter enabled")
            false
        }

        Timber.d("Register bluetooth receiver")
        context.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    override fun onInactive() {
        Timber.d("Unregister bluetooth receiver")
        context.unregisterReceiver(receiver)
        super.onInactive()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {

                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_ON -> value = true
                    BluetoothAdapter.STATE_OFF -> value = false
                    // ignore turning on/off transition states
                }
            }
        }
    }
}