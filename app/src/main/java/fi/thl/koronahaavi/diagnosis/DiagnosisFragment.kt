package fi.thl.koronahaavi.diagnosis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.common.navigateSafe
import fi.thl.koronahaavi.common.viewScopedProperty
import fi.thl.koronahaavi.databinding.FragmentDiagnosisBinding

@AndroidEntryPoint
class DiagnosisFragment : Fragment() {
    private var binding by viewScopedProperty<FragmentDiagnosisBinding>()

    private val viewModel by activityViewModels<DiagnosisViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDiagnosisBinding.inflate(inflater, container, false).apply {
            this.model = viewModel
        }

        binding.lifecycleOwner = this.viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonDiagnosisStart.setOnClickListener {
            findNavController().navigateSafe(DiagnosisFragmentDirections.toShareConsent())
        }
    }
}