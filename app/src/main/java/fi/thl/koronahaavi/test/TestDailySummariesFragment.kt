package fi.thl.koronahaavi.test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.nearby.exposurenotification.DailySummary
import com.google.android.gms.nearby.exposurenotification.ReportType.CONFIRMED_TEST
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.data.DailyExposure
import fi.thl.koronahaavi.data.SettingsRepository
import fi.thl.koronahaavi.databinding.FragmentTestDailySummariesBinding
import fi.thl.koronahaavi.service.ExposureConfigurationData
import fi.thl.koronahaavi.service.ExposureNotificationService
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToLong

@AndroidEntryPoint
class TestDailySummariesFragment : Fragment() {
    @Inject
    lateinit var exposureNotificationService: ExposureNotificationService
    @Inject
    lateinit var settingsRepository: SettingsRepository

    private lateinit var binding: FragmentTestDailySummariesBinding

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTestDailySummariesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutToolbar.toolbar.setupWithNavController(findNavController())

        binding.buttonDailySummariesUpdate.setOnClickListener {
            updateDailySummaries()
        }

        updateDailySummaries()
    }

    private fun updateDailySummaries() {
        viewLifecycleOwner.lifecycleScope.launch {
            val summaries = exposureNotificationService.getDailyExposures(config())

            binding.textDailySummaries.text = if (summaries.isEmpty()) "No data" else
                    summaries.joinToString(separator = "\n") { summary ->
                        Timber.d(summary.toString())
                        "${summary.day}, ${summary.score}"
                    }
        }
    }

    private fun config() = ExposureConfigurationData(
            version = 1,
            minimumRiskScore = 1,
            attenuationScores = listOf(),
            daysSinceLastExposureScores = listOf(),
            durationScores = listOf(),
            transmissionRiskScoresAndroid = listOf(),
            durationAtAttenuationThresholds = listOf(),
            durationAtAttenuationWeights = listOf(1.0f, 0.5f, 0.0f),
            exposureRiskDuration = 15,
            participatingCountries = listOf("DK", "DE", "IE", "IT", "LV", "ES")
    )
}