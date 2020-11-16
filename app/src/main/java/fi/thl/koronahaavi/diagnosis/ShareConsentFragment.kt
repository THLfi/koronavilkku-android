package fi.thl.koronahaavi.diagnosis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.ui.setupWithNavController
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.navigateSafe
import fi.thl.koronahaavi.databinding.FragmentShareConsentBinding

@AndroidEntryPoint
class ShareConsentFragment : Fragment() {
    private lateinit var binding: FragmentShareConsentBinding

    private val viewModel by viewModels<CodeEntryViewModel>()
    private val args by navArgs<ShareConsentFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_share_consent, container, false)
        binding = FragmentShareConsentBinding.bind(root).apply {
            this.model = viewModel
        }

        binding.lifecycleOwner = this.viewLifecycleOwner

        if (savedInstanceState == null) {
            viewModel.code.value = args.code
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutToolbar.toolbar.setupWithNavController(findNavController())

        binding.buttonShareConsentContinue.setOnClickListener {
            findNavController().navigateSafe(ShareConsentFragmentDirections.toCodeEntry())
        }
    }

}