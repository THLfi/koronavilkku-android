package fi.thl.koronahaavi.common

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import timber.log.Timber

/**
 * Provides livedata events to communicate EN API permission request results
 * between activity and fragment
 */
class RequestResolutionViewModel : ViewModel() {
    private val enableResolvedMutableEvent = MutableLiveData<Event<Boolean>>()
    private val keyHistoryResolvedMutableEvent = MutableLiveData<Event<Boolean>>()
    private val playServicesResolvedMutableEvent = MutableLiveData<Event<Boolean>>()

    fun enableResolvedEvent(): LiveData<Event<Boolean>> = enableResolvedMutableEvent
    fun keyHistoryResolvedEvent(): LiveData<Event<Boolean>> = keyHistoryResolvedMutableEvent
    fun playServicesResolvedEvent(): LiveData<Event<Boolean>> = playServicesResolvedMutableEvent

    var requestActivityInProgress = false

    private fun setEnableResolutionResult(accepted: Boolean) {
        enableResolvedMutableEvent.postValue(Event(accepted))
    }

    private fun setKeyHistoryResolutionResult(accepted: Boolean) {
        keyHistoryResolvedMutableEvent.postValue(Event(accepted))
    }

    private fun setPlayServicesUpdateResolutionResult(successful: Boolean) {
        playServicesResolvedMutableEvent.postValue(Event(successful))
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int) {
        Timber.v("requestCode=$requestCode, resultCode=$resultCode")
        val ok = resultCode == Activity.RESULT_OK

        // send result to fragment via a shared view model
        when (requestCode) {
            REQUEST_CODE_ENABLE -> {
                Timber.d("Got enable request result ($resultCode): $ok")
                setEnableResolutionResult(resultCode == Activity.RESULT_OK)
            }
            REQUEST_CODE_KEY_HISTORY -> {
                Timber.d("Got key history request result ($resultCode): $ok")
                setKeyHistoryResolutionResult(resultCode == Activity.RESULT_OK)
            }
            REQUEST_CODE_PLAY_SERVICES_ERROR_DIALOG -> {
                Timber.d("Got play services update result ($resultCode): $ok")
                setPlayServicesUpdateResolutionResult(ok)
            }
        }
    }

    companion object {
        const val REQUEST_CODE_ENABLE = 1
        const val REQUEST_CODE_KEY_HISTORY = 2
        const val REQUEST_CODE_PLAY_SERVICES_ERROR_DIALOG = 3
    }
}