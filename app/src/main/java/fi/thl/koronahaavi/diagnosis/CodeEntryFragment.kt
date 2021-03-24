package fi.thl.koronahaavi.diagnosis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.*
import fi.thl.koronahaavi.databinding.FragmentCodeEntryBinding
import timber.log.Timber

@AndroidEntryPoint
class CodeEntryFragment : Fragment() {
    private var binding by viewScopedProperty<FragmentCodeEntryBinding>()

    private val viewModel: CodeEntryViewModel by hiltNavGraphViewModels(R.id.diagnosis_share_navigation)
    private val requestResolutionViewModel by activityViewModels<RequestResolutionViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCodeEntryBinding.inflate(inflater, container, false).apply {
            model = viewModel
        }

        binding.lifecycleOwner = this.viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutToolbar.toolbar.setupWithNavController(findNavController())

        binding.buttonCodeEntrySubmit.setOnClickListener {
            viewModel.submit() // -> keysSubmittedEvent
        }

        viewModel.keyHistoryResolutionEvent().observe(viewLifecycleOwner, {
            // API permission request needs to be handled by activity.. result is observed through requestResolutionViewModel
            Timber.d("Got keyHistoryResolutionEvent event, starting request")
            it.getContentIfNotHandled()?.startResolutionForResult(
                requireActivity(), RequestResolutionViewModel.REQUEST_CODE_KEY_HISTORY
            )
        })

        requestResolutionViewModel.keyHistoryResolvedEvent().observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let { accepted ->
                if (accepted) {
                    Timber.d("Key history request accepted, trying again")
                    viewModel.submit()
                } else {
                    Timber.d("Key history request denied")
                }
            }
        })

        viewModel.keysSubmittedEvent().observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let {
                findNavController().navigateSafe(CodeEntryFragmentDirections.toDiagnosisComplete())
            }
        })

        viewModel.codeEntryError.observe(viewLifecycleOwner, {
            updateErrorText(it)
        })

        binding.textInputEditCodeEntry.setOnEditorActionListener { _, actionId, _ ->
            handleEditorAction(actionId)
        }

        // don't show keyboard automatically if code provided from app link
        if (viewModel.code.value == null) {
            if (binding.textInputEditCodeEntry.requestFocus()) {
                requireActivity().getInputMethodManager().toggleSoftInput(
                    InputMethodManager.SHOW_IMPLICIT,
                    InputMethodManager.HIDE_IMPLICIT_ONLY
                )
            }
        }
    }

    private fun updateErrorText(error: CodeEntryError?) {
        binding.textCodeEntryError.text = getString(when (error) {
            CodeEntryError.Auth -> R.string.code_entry_invalid
            CodeEntryError.LockedAfterDiagnosis -> R.string.diagnosis_locked_title
            CodeEntryError.ExposureNotificationDisabled -> R.string.code_entry_en_disabled
            else -> R.string.code_entry_submit_failed
        })
    }

    private fun handleEditorAction(actionId: Int): Boolean =
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            // handle keyboard enter, but dont do anything if code not entered
            if (viewModel.submitAllowed().value == true) {
                hideKeyboard()
                viewModel.submit()
            }
            true
        } else {
            false
        }

}