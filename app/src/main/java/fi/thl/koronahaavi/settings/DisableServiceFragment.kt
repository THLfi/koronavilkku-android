package fi.thl.koronahaavi.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.databinding.FragmentDisableServiceBinding
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class DisableServiceFragment : Fragment() {

    private lateinit var binding: FragmentDisableServiceBinding
    private val viewModel by viewModels<EnableSystemViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDisableServiceBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this.viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.layoutToolbar.toolbar.setupWithNavController(findNavController())

        binding.disableButtonTurnOff.setOnClickListener {
            confirmAndDisable()
        }
    }

    private fun confirmAndDisable() {
        MaterialAlertDialogBuilder(this.requireContext())
            .setTitle(R.string.disable_confirm_title)
            .setMessage(R.string.disable_confirm_message)
            .setPositiveButton(R.string.disable_button_turn_off) { _, _ ->
                launchDisable()
            }
            .setNegativeButton(R.string.all_cancel, null)
            .show()
    }

    private fun launchDisable() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                viewModel.disableSystem()
                findNavController().popBackStack()
            } catch (e: Exception) {
                Timber.e(e, "Failed to disable EN")
            }
        }
    }
}
