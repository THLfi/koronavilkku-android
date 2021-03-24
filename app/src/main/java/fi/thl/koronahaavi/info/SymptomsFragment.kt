package fi.thl.koronahaavi.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.openLink
import fi.thl.koronahaavi.common.viewScopedProperty
import fi.thl.koronahaavi.databinding.FragmentSymptomsBinding

@AndroidEntryPoint
class SymptomsFragment : Fragment() {

    private var binding by viewScopedProperty<FragmentSymptomsBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSymptomsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutToolbar.toolbar.setupWithNavController(findNavController())

        binding.symptomsDiagnosisLink.linkItemCard.setOnClickListener {
            openLink(getString(R.string.symptoms_link))
        }
    }
}
