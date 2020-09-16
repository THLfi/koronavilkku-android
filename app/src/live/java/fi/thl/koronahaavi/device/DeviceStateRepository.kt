package fi.thl.koronahaavi.device

import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.LiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceStateRepository @Inject constructor (
    @ApplicationContext private val context: Context
) {
    private val powerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    fun bluetoothOn(): LiveData<Boolean> = BluetoothOnLiveData(context)

    fun locationOn(): LiveData<Boolean> = LocationOnLiveData(context)

    fun isPowerOptimizationsDisabled() = powerManager.isIgnoringBatteryOptimizations(context.packageName)
}
