package fi.thl.koronahaavi.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import fi.thl.koronahaavi.common.navigateSafe
import fi.thl.koronahaavi.common.openGuide
import fi.thl.koronahaavi.databinding.FragmentConceptBinding

class ConceptFragment: Fragment() {
    private lateinit var binding: FragmentConceptBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentConceptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonConceptNext.setOnClickListener {
            findNavController().navigateSafe(ConceptFragmentDirections.toTerms())
        }

        binding.buttonConceptHowItWorks.setOnClickListener {
            activity?.openGuide()
        }
    }
}