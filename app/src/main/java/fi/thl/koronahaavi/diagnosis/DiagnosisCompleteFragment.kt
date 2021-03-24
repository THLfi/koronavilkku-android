package fi.thl.koronahaavi.diagnosis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import fi.thl.koronahaavi.common.viewScopedProperty
import fi.thl.koronahaavi.databinding.FragmentDiagnosisCompleteBinding

class DiagnosisCompleteFragment : Fragment() {
    private var binding by viewScopedProperty<FragmentDiagnosisCompleteBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDiagnosisCompleteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonDiagnosisCompleteContinue.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}