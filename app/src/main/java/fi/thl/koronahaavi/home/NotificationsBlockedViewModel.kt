package fi.thl.koronahaavi.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import fi.thl.koronahaavi.service.NotificationService
import javax.inject.Inject

@HiltViewModel
class NotificationsBlockedViewModel @Inject constructor (
    private val notificationService: NotificationService
) : ViewModel() {

    fun shouldCloseView(): Boolean {
        notificationService.refreshIsEnabled()
        return notificationService.isEnabled().value == true
    }
}