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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.RequestResolutionViewModel.Companion.REQUEST_CODE_PLAY_SERVICES_ERROR_DIALOG
import fi.thl.koronahaavi.settings.ENApiError
import fi.thl.koronahaavi.settings.EnableENError
import fi.thl.koronahaavi.settings.EnableSystemViewModel
import fi.thl.koronahaavi.settings.UserEnableStep
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
            it.getContentIfNotHandled()?.let { error ->
                onErrorEvent(error)
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

    private fun onErrorEvent(error: EnableENError) {
        if (error is EnableENError.UserCanceled && error.step == UserEnableStep.UserConsent) {
            // huawei reports cancel here instead of activity result through request resolution
            onUserRejectedEnable()
        }
        else if (error is EnableENError.UserCanceled && error.step is UserEnableStep.UpdateRequired) {
            // huawei requires specific handling of HMS Core update
            onHmsUpdateRequired(error.step)
        }
        else {
            context?.showEnableFailureReasonDialog(error)
            onEnableCanceled()
        }
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

    private fun onHmsUpdateRequired(step: UserEnableStep.UpdateRequired) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (step.retry(this@ENEnablerFragment.requireActivity())) {
                Timber.d("Retry successful")
                onExposureNotificationsEnabled()
            }
        }
    }

    /**
     * First checks if Play Services needs updating and once that is completed continues with
     * enabling EN API.
     */
    protected fun startEnablingSystem() {
        val resolver = statusViewModel.getSystemAvailability()
        val result = resolver.isSystemAvailable(requireContext())

        if (result == ConnectionResult.SUCCESS) {
            Timber.v("Exposure notification system services available")
            enableSystem()
        }
        else {
            Timber.i("Exposure notification system needs updating ($result)")
            // The shown dialog displays something informative and the action button can for example
            // take the user to Play store to update Play services, or open device's Play services
            // app settings to enable it.
            val shown = resolver.showErrorDialogFragment(requireActivity(), result, REQUEST_CODE_PLAY_SERVICES_ERROR_DIALOG) {
                // callback if user backs out of or cancels the dialog
                onEnableCanceled()
            }

            if (!shown) {
                enableSystem()
            } else {
                Timber.d("Play services error dialog shown, result=$result, resolvable=${resolver.isUserResolvableError(result)}")

                if (result == SERVICE_INVALID || !resolver.isUserResolvableError(result)) {
                    // these unresolvable errors show a message dialog but will not post result
                    // codes to activity, so we need to invoke cancel callback to clear in-progress state
                    onEnableCanceled()
                }

                // -> play dialog result is posted to playServicesResolvedEvent
            }
        }
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
                    is ENApiError.UserIsNotOwner -> getString(R.string.enable_err_not_owner)
                    is ENApiError.AppNotAuthorized -> getString(R.string.enable_err_api_connection_unauthorized)
                    is ENApiError.Failed -> getString(R.string.enable_err_api_connection_generic, it.errorCode)
                }
            }

            builder.setMessage(
                if (error.enApiError == ENApiError.UserIsNotOwner)
                    apiError
                else
                    getString(R.string.enable_err_api_not_supported, apiError)
            )
        }

        is EnableENError.UserCanceled -> {
            // Huawei only
            when (error.step) {
                is UserEnableStep.LocationPermission -> {
                    builder.setMessage(getString(R.string.enable_err_hms_location_permission))
                }
                else -> {
                    return // should not get here, but handle anyway by skipping dialog
                }
            }
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
