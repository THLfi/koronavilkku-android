package fi.thl.koronahaavi.common

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.ConnectionResult.SERVICE_INVALID
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.RequestResolutionViewModel.Companion.REQUEST_CODE_PLAY_SERVICES_ERROR_DIALOG
import fi.thl.koronahaavi.settings.ENApiError
import fi.thl.koronahaavi.settings.EnableENError
import fi.thl.koronahaavi.settings.EnableSystemViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
open class ENEnablerFragment : Fragment() {

    private val statusViewModel by viewModels<EnableSystemViewModel>()
    private val requestResolutionViewModel by activityViewModels<RequestResolutionViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statusViewModel.enableResolutionRequired().observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.startResolutionForResult(
                requireActivity(), RequestResolutionViewModel.REQUEST_CODE_ENABLE
            )
        })

        statusViewModel.enableErrorEvent().observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { reason ->
                context?.showEnableFailureReasonDialog(reason)
                onEnableCanceled()
            }
        })

        requestResolutionViewModel.enableResolvedEvent().observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { accepted ->
                if (accepted) {
                    enableSystem()
                } else {
                    onUserRejectedEnable()
                }
            }
        })

        requestResolutionViewModel.playServicesResolvedEvent().observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { successful ->
                if (successful) {
                    enableSystem()
                }
                else {
                    onEnableCanceled()
                }
            }
        })
    }

    protected fun enableSystem() {

        viewLifecycleOwner.lifecycleScope.launch {

            if (statusViewModel.enableSystem()) {
                onExposureNotificationsEnabled()
            }
        }
    }

    protected open fun onUserRejectedEnable() {
    }

    // either play service resolution canceled/failed, or EN enable failed
    protected open fun onEnableCanceled() {
    }

    protected open fun onExposureNotificationsEnabled() {
    }

    /**
     * First checks if Play Services needs updating and once that is completed continues with
     * enabling EN API.
     */
    protected fun startEnablingSystem() {
        val gaa = GoogleApiAvailability.getInstance()
        val result = gaa.isGooglePlayServicesAvailable(requireContext(), MIN_GOOGLE_PLAY_VERSION)

        if (result == ConnectionResult.SUCCESS) {
            Timber.v("Play services up-to-date")
            enableSystem()

        } else {
            Timber.i("Play services needs updating ($result)")
            // The shown dialog displays something informative and the action button can for example
            // take the user to Play store to update Play services, or open device's Play services
            // app settings to enable it.
            val shown = gaa.showErrorDialogFragment(activity, result, REQUEST_CODE_PLAY_SERVICES_ERROR_DIALOG) {
                // callback if user backs out of or cancels the dialog
                onEnableCanceled()
            }

            if (!shown) {
                enableSystem()
            } else {
                Timber.d("Play services error dialog shown, result=$result, resolvable=${gaa.isUserResolvableError(result)}")

                if (result == SERVICE_INVALID || !gaa.isUserResolvableError(result)) {
                    // these unresolvable errors show a message dialog but will not post result
                    // codes to activity, so we need to invoke cancel callback to clear in-progress state
                    onEnableCanceled()
                }

                // -> play dialog result is posted to playServicesResolvedEvent
            }
        }
    }

    companion object {
        const val MIN_GOOGLE_PLAY_VERSION = 201813000   // v20.18.13
    }
}

fun Context.showEnableFailureReasonDialog(error: EnableENError) {
    val builder = MaterialAlertDialogBuilder(this)
        .setTitle(R.string.enable_err_title)
        .setPositiveButton(R.string.enable_err_dismiss, null)

    when (error) {
        is EnableENError.MissingCapability -> {
            builder.setMessage(R.string.enable_err_missing_capability)
        }

        is EnableENError.ApiNotSupported -> {
            val apiError = error.enApiError?.let {
                when (it) {
                    is ENApiError.DeviceNotSupported -> getString(R.string.enable_err_api_connection_device)
                    is ENApiError.AppNotAuthorized -> getString(R.string.enable_err_api_connection_unauthorized)
                    is ENApiError.Failed -> getString(R.string.enable_err_api_connection_generic, it.errorCode)
                }
            }

            builder.setMessage(
                getString(R.string.enable_err_api_not_supported, apiError)
            )
        }

        is EnableENError.Failed -> {
            builder.setMessage(
                error.code?.let {
                    getString(R.string.enable_error_api_code, it, error.connectionErrorCode)
                } ?:
                getString(R.string.enable_err_generic)
            )
        }
    }

    builder.show()
}
