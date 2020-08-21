package fi.thl.koronahaavi.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.ENEnablerFragment
import fi.thl.koronahaavi.common.fadeGone
import fi.thl.koronahaavi.common.navigateSafe
import fi.thl.koronahaavi.common.openLink
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.databinding.FragmentAcceptTermsBinding
import javax.inject.Inject

@AndroidEntryPoint
class AcceptTermsFragment : ENEnablerFragment(), View.OnScrollChangeListener {

    @Inject
    lateinit var appStateRepository: AppStateRepository

    private lateinit var binding: FragmentAcceptTermsBinding
    private val viewModel by activityViewModels<OnboardingViewModel>()

    private var hasScrolledDown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            // this should not be needed, but reset in-progress state just in case
            // play/en result callbacks did not work correctly
            viewModel.enableInProgress.value = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_accept_terms, container, false)
        binding = FragmentAcceptTermsBinding.bind(root).apply {
            this.model = viewModel
        }

        binding.lifecycleOwner = this.viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonEnableService.setOnClickListener {
            viewModel.enableInProgress.value = true
            startEnablingSystem()
        }

        binding.buttonTosLink.setOnClickListener {
            openLink(getString(R.string.terms_url))
        }

        if (hasScrolledDown) {
            binding.buttonAcceptTermsScroll.visibility = View.GONE
        }
        else {
            binding.scrollAcceptTerms.setOnScrollChangeListener(this)

            binding.buttonAcceptTermsScroll.setOnClickListener { v ->
                binding.scrollAcceptTerms.fullScroll(View.FOCUS_DOWN)
                v.visibility = View.GONE
                hasScrolledDown = true
            }
        }
    }

    override fun onScrollChange(v: View?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
        (v as? ScrollView)?.let { s ->
            if (scrollY > s.maxScrollAmount && !hasScrolledDown) {
                // this is about half way down
                hasScrolledDown = true
                binding.buttonAcceptTermsScroll.fadeGone(800)
            }
        }
    }

    override fun onExposureNotificationsEnabled() {
        viewModel.enableInProgress.postValue(false)
        appStateRepository.setOnboardingComplete(true)
        findNavController().navigateSafe(AcceptTermsFragmentDirections.toDone())
    }

    override fun onUserRejectedEnable() {
        viewModel.enableInProgress.postValue(false)
        findNavController().navigateSafe(AcceptTermsFragmentDirections.toBlocked())
    }

    override fun onEnableCanceled() {
        viewModel.enableInProgress.value = false
    }
}
