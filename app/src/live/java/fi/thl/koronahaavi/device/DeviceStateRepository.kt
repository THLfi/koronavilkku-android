package fi.thl.koronahaavi.device

import android.content.Context
import androidx.lifecycle.LiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceStateRepository @Inject constructor (
    @ApplicationContext private val context: Context
) {
    fun bluetoothOn(): LiveData<Boolean> = BluetoothOnLiveData(context)

    fun locationOn(): LiveData<Boolean> = LocationOnLiveData(context)
}
