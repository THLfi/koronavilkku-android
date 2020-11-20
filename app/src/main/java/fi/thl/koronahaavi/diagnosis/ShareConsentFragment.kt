package fi.thl.koronahaavi.diagnosis

import android.os.Bundle
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.ChoiceFragment
import fi.thl.koronahaavi.common.ChoiceData.Choice

@AndroidEntryPoint
class ShareConsentFragment : ChoiceFragment() {
    private val viewModel by navGraphViewModels<CodeEntryViewModel>(R.id.diagnosis_share_navigation) {
        defaultViewModelProviderFactory
    }

    private val args by navArgs<ShareConsentFragmentArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            viewModel.code.value = args.code
        }
    }

    override fun getChoiceViewModel() = viewModel.shareConsentModel

    override val headerTextId = R.string.share_consent_header
    override val bodyTextId = R.string.share_consent_info
    override val firstChoiceTextId = R.string.share_consent_eu
    override val secondChoiceTextId = R.string.share_consent_finland

    override fun getNextDirections(choice: Choice) = when (choice) {
        Choice.FIRST -> ShareConsentFragmentDirections.toTravelDisclosure()
        Choice.SECOND -> ShareConsentFragmentDirections.toSummaryConsent()
    }
}