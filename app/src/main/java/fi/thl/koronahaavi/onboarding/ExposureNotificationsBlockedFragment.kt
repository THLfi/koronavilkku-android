package fi.thl.koronahaavi.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.ENEnablerFragment
import fi.thl.koronahaavi.common.navigateSafe
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.databinding.FragmentEnBlockedBinding
import javax.inject.Inject

@AndroidEntryPoint
class ExposureNotificationsBlockedFragment: ENEnablerFragment() {

    @Inject
    lateinit var appStateRepository: AppStateRepository

    private val viewModel by activityViewModels<OnboardingViewModel>()

    private lateinit var binding: FragmentEnBlockedBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_en_blocked, container, false)
        binding = FragmentEnBlockedBinding.bind(root).apply {
            this.model = viewModel
        }

        binding.lifecycleOwner = this.viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonEnBlockedEnable.setOnClickListener {
            startEnablingSystem()
        }
    }

    override fun onExposureNotificationsEnabled() {
        appStateRepository.setOnboardingComplete(true)
        findNavController().navigateSafe(ExposureNotificationsBlockedFragmentDirections.toDone())
    }
}
