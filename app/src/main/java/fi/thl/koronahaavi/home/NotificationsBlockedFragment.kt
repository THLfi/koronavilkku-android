package fi.thl.koronahaavi.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.common.openNotificationSettings
import fi.thl.koronahaavi.databinding.FragmentNotificationsBlockedBinding

@AndroidEntryPoint
class NotificationsBlockedFragment : Fragment() {
    private var _binding: FragmentNotificationsBlockedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificationsBlockedViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBlockedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with (binding) {
            layoutToolbar.toolbar.setupWithNavController(findNavController())

            buttonNotificationsBlockedOpenSettings.setOnClickListener {
                openNotificationSettings()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // fragment should be closed automatically when returning to it, if user has unblocked
        // notifications in device settings
        if (viewModel.shouldCloseView()) {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}