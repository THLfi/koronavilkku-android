package fi.thl.koronahaavi.diagnosis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.ui.setupWithNavController
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.navigateSafe
import fi.thl.koronahaavi.databinding.FragmentTravelDisclosureBinding

@AndroidEntryPoint
class TravelDisclosureFragment : Fragment() {
    private lateinit var binding: FragmentTravelDisclosureBinding

    private val viewModel by viewModels<CodeEntryViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_travel_disclosure, container, false)
        binding = FragmentTravelDisclosureBinding.bind(root).apply {
            this.model = viewModel
        }

        binding.lifecycleOwner = this.viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutToolbar.toolbar.setupWithNavController(findNavController())

        binding.layoutShareContentContinue.buttonContinue.setOnClickListener {
            findNavController().navigateSafe(toNextDestination())
        }
    }

    private fun toNextDestination(): NavDirections =
            if (viewModel.isSummaryReady())
                TravelDisclosureFragmentDirections.toSummaryConsent()
            else
                TravelDisclosureFragmentDirections.toCountryList()

}