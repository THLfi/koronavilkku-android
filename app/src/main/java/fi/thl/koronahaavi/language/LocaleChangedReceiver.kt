package fi.thl.koronahaavi.language

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class LocaleChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_LOCALE_CHANGED) {
            Timber.d("Locale changed")
            // todo inject notification service and initialize
        }
    }
}