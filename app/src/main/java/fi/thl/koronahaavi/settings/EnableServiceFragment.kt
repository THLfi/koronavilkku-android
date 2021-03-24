package fi.thl.koronahaavi.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.common.ENEnablerFragment
import fi.thl.koronahaavi.common.viewScopedProperty
import fi.thl.koronahaavi.databinding.FragmentEnableServiceBinding

@AndroidEntryPoint
class EnableServiceFragment : ENEnablerFragment() {

    private var binding by viewScopedProperty<FragmentEnableServiceBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEnableServiceBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this.viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.layoutToolbar.toolbar.setupWithNavController(findNavController())

        binding.buttonEnableTurnOn.setOnClickListener {
            enableSystem()
        }
    }

    override fun onExposureNotificationsEnabled() {
        findNavController().popBackStack()
    }
}
