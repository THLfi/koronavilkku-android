package fi.thl.koronahaavi.test

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.data.SettingsRepository
import fi.thl.koronahaavi.databinding.FragmentTestDailySummariesBinding
import fi.thl.koronahaavi.service.ExposureNotificationService
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.*
import javax.inject.Inject

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

        updateDailySummaries()
    }

    private fun updateDailySummaries() {
        viewLifecycleOwner.lifecycleScope.launch {
            val summaries = settingsRepository.getExposureConfiguration()?.let {
                exposureNotificationService.getDailyExposures(it)
            }

            binding.textDailySummaries.text = if (summaries?.isEmpty() != false) "No data" else
                    summaries.joinToString(separator = "\n") { summary ->
                        Timber.d(summary.toString())
                        "${summary.day}, scoreSum:${summary.score}"
                    }


            exposureNotificationService.getExposureWindows().sortedBy { it.dateMillisSinceEpoch }.forEach {
                binding.layoutDailySummaries.addView(it.dumpWindowToTextView())
                binding.layoutDailySummaries.addView(it.dumpScansToTextView())
            }
        }
    }

    private fun ExposureWindow.dumpWindowToTextView() = TextView(requireContext()).apply {
        val day = Instant.ofEpochMilli(dateMillisSinceEpoch).atZone(ZoneOffset.UTC).toLocalDate()
        text = day.toString()
    }

    private fun ExposureWindow.dumpScansToTextView() = TextView(requireContext()).apply {
        text = scanInstances.joinToString(separator = "\n") { scan ->
            "  typical:${scan.typicalAttenuationDb}, min:${scan.minAttenuationDb}, sec:${scan.secondsSinceLastScan}"
        }
    }
}