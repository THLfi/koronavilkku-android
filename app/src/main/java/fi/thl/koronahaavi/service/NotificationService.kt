package fi.thl.koronahaavi.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_NONE
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.thl.koronahaavi.MainActivity
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.withSavedLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor (
    @ApplicationContext private val context: Context
) {
    private val notificationManager by lazy { NotificationManagerCompat.from(context) }

    private val isEnabledFlow = MutableStateFlow<Boolean?>(null)
    fun isEnabled(): StateFlow<Boolean?> = isEnabledFlow

    fun refreshIsEnabled() {
        isEnabledFlow.value = notificationManager.areNotificationsEnabled() && isNotificationChannelEnabled()
    }

    // channels only available in sdk 26
    private fun isNotificationChannelEnabled() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        notificationManager.getNotificationChannel(CHANNEL_ID)?.importance != IMPORTANCE_NONE
    } else {
        true
    }

    fun notifyExposure() {
        // make sure channel has been created
        initialize()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, FLAG_IMMUTABLE)

        // Localize notification text with app language override, if selected
        val languageContext = context.withSavedLanguage()
        val title = languageContext.getString(R.string.notification_exposure_title)
        val message = languageContext.getString(R.string.notification_exposure_message)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.drawable.ic_exposure_notification)
            .setColor(context.getColor(R.color.mainRed))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // remove when clicked
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)  // Do not reveal on secure lockscreen

        notificationManager.notify(0, builder.build())
    }

    fun notifyShutdown() {
        initialize()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.withSavedLanguage().getString(R.string.notification_shutdown_title))
            .setSmallIcon(R.drawable.ic_exposure_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // remove when clicked

        notificationManager.notify(SHUTDOWN_NOTIFICATION_ID, builder.build())
    }

    fun initialize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // we should not apply app language override for the channel name since it appears in
            // device settings, so we are using application context provided through Hilt annotation
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "fi.thl.koronahaavi.exposure_notification"
        private const val SHUTDOWN_NOTIFICATION_ID = 1
    }
}