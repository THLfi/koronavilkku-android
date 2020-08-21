package fi.thl.koronahaavi.exposure

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient.*
import timber.log.Timber

@Suppress("DEPRECATION")
class ExposureStateUpdatedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        when (intent.action) {
            ACTION_EXPOSURE_STATE_UPDATED -> {
                intent.getStringExtra(EXTRA_TOKEN)?.let { token ->
                    Timber.i("Received update action, starting worker")
                    ExposureUpdateWorker.start(context, token)
                }
            }
            ACTION_EXPOSURE_NOT_FOUND -> {
                Timber.i("No matches found")
                // no action, token entry in key group token table does not need to be updated
                // since its only used for exposure score testing purposes
            }
            else -> {
                Timber.e("Got an unexpected action: ${intent.action}")
            }
        }
    }
}