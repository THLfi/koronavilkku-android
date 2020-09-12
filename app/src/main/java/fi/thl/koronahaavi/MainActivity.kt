package fi.thl.koronahaavi

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.common.RequestResolutionViewModel
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.databinding.ActivityMainBinding
import fi.thl.koronahaavi.service.ExposureNotificationService
import fi.thl.koronahaavi.service.WorkDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var appStateRepository: AppStateRepository
    @Inject lateinit var exposureNotificationService: ExposureNotificationService
    @Inject lateinit var workDispatcher: WorkDispatcher

    private val resolutionViewModel by viewModels<RequestResolutionViewModel>()

    private val navController by lazy {
        (supportFragmentManager.findFragmentById(R.id.fragment_main_nav_host) as NavHostFragment)
            .navController
    }

    private val topLevelDestinations = setOf(R.id.home, R.id.diagnosis, R.id.settings)

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
            //Fix: Issue #31 - automatically whitelist application from Doze
            checkDoze(manualToggle = false)
            setupServices()

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
                            navController.navigate(MainNavigationDirections.toCodeEntry(code))
                        } else {
                            // Diagnosis view will give instructions to the user (needs to be enabled first).
                            navController.navigate(MainNavigationDirections.toDiagnosis())
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // EN enabled status needs to be queried when returning to the app since there
        // is no listener mechanism, and user could have disabled it in device settings

        lifecycleScope.launch { exposureNotificationService.refreshEnabledFlow() }
    }

    override fun onSupportNavigateUp(): Boolean {
        val cfg = AppBarConfiguration(topLevelDestinations)
        return navController.navigateUp(cfg) || super.onSupportNavigateUp()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        resolutionViewModel.handleActivityResult(requestCode, resultCode)
    }

    private fun setupServices() {
        if (appStateRepository.lockedAfterDiagnosis().value) {
            // make sure download is not running when app locked.. this should not be needed since work is canceled
            // after submitting diagnosis, but just making sure
            workDispatcher.cancelWorkersAfterLock()
        }
        else {
            workDispatcher.scheduleWorkers()
        }
    }

    /**
     * Check if Doze is enabled and request user's intervention
     * @param manualToggle: Boolean - default=false, to minimize users' burden and minimize misconfiguration
     */
    private fun checkDoze(manualToggle: Boolean) {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
            if (manualToggle) {
                //TODO: Show instructions GUI on how to add app to the ignore list.
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                //TODO: Come back to the app, checkDoze state
            } else {
                val whitelist = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:$packageName"))
                startActivityForResult(whitelist, RequestResolutionViewModel.REQUEST_CODE_WHITELIST)
            }
        }
    }
}