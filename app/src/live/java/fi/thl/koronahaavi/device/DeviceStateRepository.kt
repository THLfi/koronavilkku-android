package fi.thl.koronahaavi.device

import android.content.Context
import android.os.Process
import android.os.UserManager
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

    fun isPrimaryUser(): Boolean =
        (context.getSystemService(Context.USER_SERVICE) as? UserManager)?.let {
            it.getSerialNumberForUser(Process.myUserHandle()) == 0L
        } ?: false
}
