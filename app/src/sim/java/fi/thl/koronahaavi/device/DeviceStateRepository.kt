package fi.thl.koronahaavi.device

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceStateRepository @Inject constructor (
    @ApplicationContext private val context: Context
) {
    fun bluetoothOn(): LiveData<Boolean> = MutableLiveData(true) // fake always on

    fun locationOn(): LiveData<Boolean> = LocationOnLiveData(context)
}
