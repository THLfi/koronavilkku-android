package fi.thl.koronahaavi.diagnosis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.ui.setupWithNavController
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.FormatExtensions.convertToCountryName
import fi.thl.koronahaavi.common.navigateSafe
import fi.thl.koronahaavi.databinding.FragmentSummaryConsentBinding
import fi.thl.koronahaavi.databinding.ItemCountryBulletBinding

@AndroidEntryPoint
class SummaryConsentFragment : Fragment() {
    private lateinit var binding: FragmentSummaryConsentBinding

    private val viewModel: CodeEntryViewModel by hiltNavGraphViewModels(R.id.diagnosis_share_navigation)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_summary_consent, container, false)
        binding = FragmentSummaryConsentBinding.bind(root).apply {
            this.model = viewModel.shareData
        }

        binding.lifecycleOwner = this.viewLifecycleOwner

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutToolbar.toolbar.setupWithNavController(findNavController())

        binding.buttonSummaryConsentContinue.setOnClickListener {
            findNavController().navigateSafe(SummaryConsentFragmentDirections.toCodeEntry())
        }

        viewModel.shareData.countries.observe(viewLifecycleOwner, Observer { allCountries ->
            updateCountryBullets(allCountries.filter { it.isSelected })
        })
    }

    private fun updateCountryBullets(selectedCountries: List<CountryData>) {
        if (viewModel.shareData.summaryShowCountries.value == false) {
            return // no need to update since parent view is hidden
        }

        binding.layoutSummaryConsentCountries.removeAllViews()

        selectedCountries.map {
            it.code.convertToCountryName()
        }.sorted().forEach {
            addCountryBullet(it)
        }

        // view model should have a valid value for this selection, since it is selected in a previous fragment
        // no country selections also implies this other selection
        if (viewModel.shareData.otherCountrySelected.value == true || selectedCountries.isEmpty()) {
            addCountryBullet(getString(R.string.country_selection_other))
        }
    }

    private fun addCountryBullet(label: String) {
        binding.layoutSummaryConsentCountries.addView(
            ItemCountryBulletBinding.inflate(layoutInflater, binding.layoutSummaryConsentCountries, false)
                    .apply { root.text = label }
                    .root
        )
    }
}