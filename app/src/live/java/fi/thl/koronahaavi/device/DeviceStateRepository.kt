package fi.thl.koronahaavi.device

import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.thl.koronahaavi.service.ExposureNotificationService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceStateRepository @Inject constructor (
    @ApplicationContext private val context: Context,
    private val exposureNotificationService: ExposureNotificationService
) {
    private val powerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    fun bluetoothOn(): LiveData<Boolean> = BluetoothOnLiveData(context)

    fun locationOn(): LiveData<Boolean> =
        if (exposureNotificationService.deviceSupportsLocationlessScanning())
            MutableLiveData(true) // always on since EN does not care
        else
            LocationOnLiveData(context)

    fun isPowerOptimizationsDisabled() = powerManager.isIgnoringBatteryOptimizations(context.packageName)
}
