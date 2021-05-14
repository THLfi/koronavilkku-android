package fi.thl.koronahaavi.test

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.WorkManager
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.BuildConfig
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.navigateSafe
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.ExposureRepository
import fi.thl.koronahaavi.data.SettingsRepository
import fi.thl.koronahaavi.databinding.FragmentTestBinding
import fi.thl.koronahaavi.exposure.ExposureStateUpdatedReceiver
import fi.thl.koronahaavi.exposure.ExposureUpdateWorker
import fi.thl.koronahaavi.exposure.MunicipalityUpdateWorker
import fi.thl.koronahaavi.service.DiagnosisKeyUpdateWorker
import fi.thl.koronahaavi.service.ExposureNotificationService
import fi.thl.koronahaavi.service.NotificationService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.time.ZonedDateTime
import java.util.*
import javax.inject.Inject

@SuppressLint("SetTextI18n")
@AndroidEntryPoint
class TestFragment : Fragment() {
    @Inject lateinit var appStateRepository: AppStateRepository
    @Inject lateinit var exposureRepository: ExposureRepository
    @Inject lateinit var exposureNotificationService: ExposureNotificationService
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var notificationService: NotificationService

    private lateinit var binding: FragmentTestBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutToolbar.toolbar.setupWithNavController(findNavController())

        binding.buttonTestUpdateMunicipalities.setOnClickListener {
            startMunicipalityUpdate(it)
        }

        binding.buttonTestClearExposures.setOnClickListener {
            requireActivity().lifecycleScope.launch {
                exposureRepository.deleteAllExposures()
                exposureRepository.deleteAllKeyGroupTokens()
            }
        }

        binding.buttonTestSetBatchId.setOnClickListener {
            binding.inputEditTestBatchId.text?.toString()?.let {
                appStateRepository.setDiagnosisKeyBatchId(it)
            }
        }

        binding.buttonTestDownload.setOnClickListener {
            startDownload(it)
        }

        binding.buttonTestReceiver.visibility = if (BuildConfig.FLAVOR.contains("sim")) View.VISIBLE else View.GONE

        binding.buttonTestReceiver.setOnClickListener {
            simulateReceiver(it)
        }

        binding.buttonTestResetOnboarding.setOnClickListener {
            appStateRepository.setOnboardingComplete(false)
            findNavController().navigateSafe(TestFragmentDirections.toOnboarding())
            requireActivity().finish()
        }

        val wm = WorkManager.getInstance(requireContext())
        wm.getWorkInfosForUniqueWorkLiveData(DiagnosisKeyUpdateWorker.KEY_UPDATER_NAME)
            .observe(viewLifecycleOwner, Observer {
                val infos = it.joinToString("\n") { wi ->
                    "${wi.state} run=${wi.runAttemptCount}"
                }
                binding.testWorkerInfo.text = "Key update worker: $infos"
            })

        wm.getWorkInfosByTagLiveData(ExposureUpdateWorker.TAG)
            .observe(viewLifecycleOwner, Observer {
                val stateHistory = it.joinToString { info -> info.state.toString()}
                binding.testExposureWorkerInfo.text = "Exposure worker: $stateHistory"
            })

        binding.testDeviceInfo.text = "${Build.MANUFACTURER} ${Build.MODEL}"

        binding.buttonTestLock.setOnClickListener {
            appStateRepository.setDiagnosisKeysSubmitted(true)
        }

        binding.buttonTestUnlock.setOnClickListener {
            appStateRepository.setDiagnosisKeysSubmitted(false)
        }

        binding.buttonTestSetOldUpdate.setOnClickListener {
            appStateRepository.setLastExposureCheckTime(ZonedDateTime.now().minusDays(2))
        }

        binding.buttonTestCheckPlayServices.setOnClickListener {
            val gaa = GoogleApiAvailability.getInstance()

            if (false) {

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        // https://developers.google.com/android/reference/com/google/android/gms/common/GoogleApiAvailability#makeGooglePlayServicesAvailable(android.app.Activity)
                        Timber.d("begin making GMS available")
                        gaa.makeGooglePlayServicesAvailable(requireActivity()).await()
                        Timber.d("done, play services available!")
                    } catch (e: Exception) {
                        Timber.e(e, "activity completed too soon, or there were problems making play services available")
                    }
                }

            } else {
                val result = gaa.isGooglePlayServicesAvailable(requireContext())
                //result = ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED // If one needs to check what the dialog looks like.

                // Once the user returns from Play store (possibly the user selected to Update
                // Play services), then the used activity's onActivityResult() gets called with
                // this reqCode.
                val reqCode = 999

                val shown = gaa.showErrorDialogFragment(requireActivity(), result, reqCode)
                // Or alternatively call showErrorNotification() - but dialog seems more appropriate when inside the app (e.g. onboarding).
                // The button in the dialog / notification action can take the user to Play store to update
                // Play services, or open device's Play services app settings to enable it.

                if (!shown) {
                    Toast.makeText(activity, "Play services available, no action needed ($result)", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.buttonTestDailySummaries.setOnClickListener {
            findNavController().navigateSafe(TestFragmentDirections.toTestDailySummaries())
        }

        binding.buttonTestCreateNotification.setOnClickListener {
            GlobalScope.launch {
                delay(5000)
                notificationService.notifyExposure()
            }
            Snackbar.make(it, getString(R.string.test_notification_message), Snackbar.LENGTH_SHORT).show()
        }

        binding.testPlayVersion.text = context?.getPlayServicesVersion() ?: "N/A"
    }

    private fun simulateReceiver(button: View) {
        requireActivity().lifecycleScope.launch {
            delay(5000)

            val intent = Intent().apply {
                component = ComponentName(requireContext(), ExposureStateUpdatedReceiver::class.java)
                action = ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED
            }
            requireContext().sendBroadcast(intent)
        }

        Snackbar.make(
            button, getString(R.string.test_update_exposures_message), Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun startDownload(button: View) {
        requireActivity().lifecycleScope.launch {
            delay(5000)
            DiagnosisKeyUpdateWorker.schedule(
                requireContext(),
                settingsRepository.appConfiguration,
                reconfigure = true
            )
        }

        Snackbar.make(
            button, getString(R.string.test_download_message), Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun startMunicipalityUpdate(button: View) {
        requireActivity().lifecycleScope.launch {
            delay(5000)
            MunicipalityUpdateWorker.schedule(
                requireContext(),
                reconfigure = true
            )
        }

        Snackbar.make(
            button, getString(R.string.test_download_message), Snackbar.LENGTH_SHORT
        ).show()
    }
}

private fun Context.getVersionNameForPackage(packageName: String): String? {
    return try {
        packageManager.getPackageInfo(packageName, 0).versionName
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}

private fun Context.getPlayServicesVersion(): String? {
    return getVersionNameForPackage(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE)
}

private fun Context.openExposureNotificationSettings() {
    startActivity(Intent(ExposureNotificationClient.ACTION_EXPOSURE_NOTIFICATION_SETTINGS))
}
