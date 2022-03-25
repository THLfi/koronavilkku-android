package fi.thl.koronahaavi

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.common.RequestResolutionViewModel
import fi.thl.koronahaavi.common.withSavedLanguage
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.databinding.ActivityMainBinding
import fi.thl.koronahaavi.device.DeviceStateRepository
import fi.thl.koronahaavi.service.*
import fi.thl.koronahaavi.settings.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var appStateRepository: AppStateRepository
    @Inject lateinit var exposureNotificationService: ExposureNotificationService
    @Inject lateinit var workDispatcher: WorkDispatcher
    @Inject lateinit var deviceStateRepository: DeviceStateRepository
    @Inject lateinit var userPreferences: UserPreferences
    @Inject lateinit var notificationService: NotificationService
    @Inject lateinit var diagnosisKeyService: DiagnosisKeyService
    @Inject lateinit var externalScope: CoroutineScope

    private val resolutionViewModel by viewModels<RequestResolutionViewModel>()

    // keep prompt visibility state here since if its cleared on activity recreate
    // we would just show the dialog again
    private var powerOptimizationDialog: AlertDialog? = null

    private val navController by lazy {
        (supportFragmentManager.findFragmentById(R.id.fragment_main_nav_host) as NavHostFragment)
            .navController
    }

    private val topLevelDestinations = setOf(R.id.home, R.id.diagnosis, R.id.settings)

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.withSavedLanguage())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Replace launcher theme with the actual one.
        setTheme(R.style.Theme_Vilkku_NoActionBar)

        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!appStateRepository.isOnboardingComplete()) {
            navController.navigate(MainNavigationDirections.toOnboarding())
            finish()
        }
        else {
            setupShutdownObserver()
            setupServices()

            if (savedInstanceState == null &&
                !appStateRepository.appShutdown().value) {
                // run a single shutdown check to make sure it is detected even if background workers
                // blocked for some reason
                startShutdownCheck()
            }

            // configure navigation to work with bottom nav bar
            binding.navView.setupWithNavController(navController)

            // this needs to be defined to prevent fragment recreate on reselect
            binding.navView.setOnNavigationItemReselectedListener {
                Timber.d("Current tab reselected")
            }

            // only show bottom nav bar for top level tab destinations
            navController.addOnDestinationChangedListener { _, destination, _ ->
                binding.navView.visibility = if (topLevelDestinations.contains(destination.id)) View.VISIBLE else View.GONE
            }

            checkViewIntent(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // avoid WindowLeaked error if dialog was visible during config change
        powerOptimizationDialog?.dismiss()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        checkViewIntent(intent)
        // launchMode=singleTop is used to avoid opening MainActivity if it already open (in which case onNewIntent() gets called).
    }

    private fun checkViewIntent(intent: Intent?) {
        intent?.data?.queryParameterNames?.firstOrNull()?.let { code ->

            // Launched from history flag is set when app task was created from
            // recents screen and it had originally contained a deep link intent.
            // In this case the user probably had backed out from home and closed
            // the app so we should ignore the intent, and start normally
            if (intent.flags.and(FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
                Timber.d("Launched from history, ignoring intent")
            }
            else {
                Timber.d("Handling start intent with code $code")
                if (appStateRepository.lockedAfterDiagnosis().value) {
                    // Diagnosis view will give instructions to the user (already reported).
                    navController.navigate(MainNavigationDirections.toDiagnosis())

                } else {
                    // coroutine needed since isEnabled is async
                    lifecycleScope.launch {
                        if (exposureNotificationService.isEnabled()) {
                            navController.navigate(MainNavigationDirections.toShareConsent(code))
                        } else {
                            // Diagnosis view will give instructions to the user (needs to be enabled first).
                            navController.navigate(MainNavigationDirections.toDiagnosis())
                        }
                    }
                }
            }
        }
    }

    private fun startShutdownCheck() {
        // external application scope, because shutdown will trigger main activity to finish
        // through app state observer, which would cancel lifecycleScope
        externalScope.launch {
            try {
                diagnosisKeyService.reloadExposureConfig()
            }
            catch (t: Throwable) {
                Timber.e(t, "Failed to check for shutdown config")
            }
        }
    }

    private fun setupShutdownObserver() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                appStateRepository.appShutdown().collect { shutdown ->
                    if (shutdown) {
                        Timber.d("App shutdown detected")
                        navController.navigate(MainNavigationDirections.toShutdown())
                        finish()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if (shouldRequestPowerOptimizationDisable()) {
            showPowerOptimizationDisableRationale()
        }
    }

    private fun shouldRequestPowerOptimizationDisable() =
        !deviceStateRepository.isPowerOptimizationsDisabled() &&
        userPreferences.powerOptimizationDisableAllowed != false &&  // do not prompt if user denied before
        !isPowerOptimizationRequestInProgress() &&
        !appStateRepository.appShutdown().value

    /**
     * Request is considered to be in progress if either a rationale or deny confirm dialog is showing
     * or play service permission dialog activity was started
     */
    private fun isPowerOptimizationRequestInProgress() =
        powerOptimizationDialog?.isShowing == true || resolutionViewModel.requestActivityInProgress

    private fun showPowerOptimizationDisableRationale() {
        powerOptimizationDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.power_rationale_title)
            .setMessage(R.string.power_rationale_message)
            .setPositiveButton(R.string.all_allow) { _, _ ->
                showPowerOptimizationDisablePrompt()
            }
            .setNegativeButton(R.string.all_deny) { _, _ ->
                showPowerOptimizationDisableDenyConfirm()
            }
            .setCancelable(false) // dont close on back or when clicking outside dialog
            .show()
    }

    @SuppressLint("BatteryLife")
    private fun showPowerOptimizationDisablePrompt() {
        val intent = Intent(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:$packageName"))

        resolutionViewModel.requestActivityInProgress = true
        startActivityForResult(intent, REQUEST_CODE_POWER_OPTIMIZATION_DISABLE)
    }

    private fun showPowerOptimizationDisableDenyConfirm() {
        powerOptimizationDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.power_deny_confirm_title)
            .setMessage(R.string.power_deny_confirm_message)
            .setPositiveButton(R.string.all_close) { _, _ ->
                userPreferences.powerOptimizationDisableAllowed = false
            }
            .setNegativeButton(R.string.power_allow_retry) { _, _ ->
                showPowerOptimizationDisablePrompt()  // ask again
            }
            .setCancelable(false)
            .show()
    }

    override fun onResume() {
        super.onResume()

        // EN system and notifications enabled status needs to be queried when returning to the app since there
        // is no listener mechanism, and user could have disabled it in device settings
        lifecycleScope.launch {
            exposureNotificationService.refreshEnabledFlow()
            notificationService.refreshIsEnabled()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val cfg = AppBarConfiguration(topLevelDestinations)
        return navController.navigateUp(cfg) || super.onSupportNavigateUp()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_POWER_OPTIMIZATION_DISABLE -> {
                resolutionViewModel.requestActivityInProgress = false

                // pixel android 12 seems to always return RESULT_CANCELED regardless of user choice,
                // so we are also checking the current state through device state repository
                if (resultCode == RESULT_OK || deviceStateRepository.isPowerOptimizationsDisabled()) {
                    userPreferences.powerOptimizationDisableAllowed = true
                }
                else {
                    showPowerOptimizationDisableDenyConfirm()
                }
            }
            else -> {
                resolutionViewModel.handleActivityResult(requestCode, resultCode)
            }
        }
    }

    private fun setupServices() {
        if (appStateRepository.appShutdown().value) {
            workDispatcher.cancelAllWorkers()
        }
        else {
            if (appStateRepository.lockedAfterDiagnosis().value) {
                // make sure download is not running when app locked.. this should not be needed since work is canceled
                // after submitting diagnosis, but just making sure
                workDispatcher.cancelWorkersAfterLock()

                // cover traffic sender should be on, and was turned off in previous versions
                DiagnosisKeySendTrafficCoverWorker.schedule(this.applicationContext)
            }
            else {
                workDispatcher.scheduleWorkers()
            }
        }
    }

    companion object {
        const val REQUEST_CODE_POWER_OPTIMIZATION_DISABLE = 42
    }
}