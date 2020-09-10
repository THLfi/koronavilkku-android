package fi.thl.koronahaavi.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.navigateSafe
import fi.thl.koronahaavi.common.openGuide
import fi.thl.koronahaavi.databinding.FragmentIntroBinding

class IntroFragment: Fragment() {
    private lateinit var binding: FragmentIntroBinding

    private val viewModel by activityViewModels<OnboardingViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentIntroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (viewModel.isPrimaryUser) {
            binding.buttonIntroNext.setOnClickListener {
                findNavController().navigateSafe(IntroFragmentDirections.toConcept())
            }

            binding.buttonIntroHowItWorks.setOnClickListener {
                activity?.openGuide()
            }
        }
        else {
            // Exposure notifications module will not work on secondary user profiles
            // due to google play services design and privacy decisions
            MaterialAlertDialogBuilder(this.requireContext())
                .setTitle(R.string.secondary_user_error_title)
                .setMessage(R.string.secondary_user_error_message)
                .setPositiveButton(R.string.secondary_user_error_close) { _, _ ->
                    requireActivity().finish()
                }
                .setCancelable(false)
                .show()
        }
    }
}