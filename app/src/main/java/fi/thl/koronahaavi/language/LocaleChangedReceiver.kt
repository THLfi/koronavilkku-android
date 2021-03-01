package fi.thl.koronahaavi.language

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.service.NotificationService
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LocaleChangedReceiver : BroadcastReceiver() {
    @Inject
    lateinit var notificationService: NotificationService

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_LOCALE_CHANGED) {
            // this updates notification channel name to match current locale
            notificationService.initialize()
        }
    }
}