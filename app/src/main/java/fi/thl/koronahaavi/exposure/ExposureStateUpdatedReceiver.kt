package fi.thl.koronahaavi.exposure

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient.*
import fi.thl.koronahaavi.shutdown.ExposureServiceDisableWorker
import timber.log.Timber

class ExposureStateUpdatedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        when (intent.action) {
            ACTION_EXPOSURE_STATE_UPDATED -> {
                Timber.d("Exposure state updated")
                ExposureUpdateWorker.start(context)
            }
            ACTION_EXPOSURE_NOT_FOUND -> {
                Timber.i("No exposures found")
                // no need to update state, expiring exposures are removed periodically
                // and also excluded when displaying state
            }
            ACTION_SERVICE_STATE_UPDATED -> {
                Timber.d("Exposure service state updated")
                // user has enabled or disabled EN api in device settings, so we should
                // notify and disable if app is in shutdown state
                ExposureServiceDisableWorker.start(context)
            }
            else -> {
                Timber.e("Got an unexpected action: ${intent.action}")
            }
        }
    }
}