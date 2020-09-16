package fi.thl.koronahaavi.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.BuildConfig
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.fromDeviceState
import fi.thl.koronahaavi.common.navigateSafe
import fi.thl.koronahaavi.common.openGuide
import fi.thl.koronahaavi.common.openLink
import fi.thl.koronahaavi.databinding.FragmentSettingsBinding
import fi.thl.koronahaavi.device.SystemState
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;


@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private val viewModel by viewModels<SettingsViewModel>()
    private var toggleServiceStateNavDirections: NavDirections? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_settings, container, false)
        binding = FragmentSettingsBinding.bind(root).apply {
            this.model = viewModel
        }

        binding.lifecycleOwner = this.viewLifecycleOwner
        return binding.root
    }

    private fun updateStatusItemText(state: SystemState) {
        // updating text with data binding adapter directly here since layout uses an
        // include tag with a string variable and adapter wont't work there

        val itemCard = binding.settingsStatusItem.linkItemCard.rootView
        itemCard.findViewById<TextView>(R.id.link_item_value)?.fromDeviceState(state)
    }

    private fun updateStatusDirections(state: SystemState) {
        toggleServiceStateNavDirections = when (state) {
            SystemState.On -> SettingsFragmentDirections.toDisableService()
            SystemState.Off -> SettingsFragmentDirections.toEnableService()
            SystemState.Locked -> null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.systemState().observe(viewLifecycleOwner, Observer {
            it?.let { state ->
                updateStatusItemText(state)
                updateStatusDirections(state)
            }
        })

        binding.settingsStatusItem.linkItemCard.setOnClickListener {
            toggleServiceStateNavDirections?.let { directions ->
                findNavController().navigateSafe(directions)
            }
        }

        binding.settingsGuideItem.linkItemContainer.setOnClickListener {
            activity?.openGuide()
        }

        binding.settingsFaqItem.linkItemContainer.setOnClickListener {
            openLink(getString(R.string.settings_faq_link_url))
        }

        binding.settingsPrivacyItem.linkItemContainer.setOnClickListener {
            openLink(getString(R.string.settings_privacy_link_url))
        }

        binding.settingsTosItem.linkItemContainer.setOnClickListener {
            openLink(getString(R.string.terms_url))
        }

        binding.settingsOpenSourceNotices.linkItemContainer.setOnClickListener {
            startActivity(Intent(requireContext(), OssLicensesMenuActivity::class.java))
        }

        binding.settingsAppNameVersion.text = getString(R.string.settings_version,
            getString(R.string.app_name), BuildConfig.VERSION_NAME)
    }
}
