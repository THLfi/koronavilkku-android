package fi.thl.koronahaavi.common

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.service.WorkState

class ExposureCheckObserver(
    private val context: Context,
    private val inProgressLiveData: MutableLiveData<Boolean>
) : Observer<WorkState> {

    override fun onChanged(t: WorkState?) {
        when (t) {
            WorkState.Failed -> {
                inProgressLiveData.value = false
                showManualExposureCheckError()
            }
            WorkState.Success -> {
                inProgressLiveData.value = false
            }
        }
    }

    private fun showManualExposureCheckError() {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.exposure_check_error_title)
            .setMessage(R.string.exposure_check_error_message)
            .setPositiveButton(R.string.all_ok, null)
            .show()
    }
}