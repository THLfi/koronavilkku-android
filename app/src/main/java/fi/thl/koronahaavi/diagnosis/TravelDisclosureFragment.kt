package fi.thl.koronahaavi.diagnosis

import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.navGraphViewModels
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.ChoiceFragment
import fi.thl.koronahaavi.common.ChoiceData
import fi.thl.koronahaavi.common.ChoiceData.Choice

@AndroidEntryPoint
class TravelDisclosureFragment : ChoiceFragment() {
    private val viewModel: CodeEntryViewModel by hiltNavGraphViewModels(R.id.diagnosis_share_navigation)

    override fun getChoiceViewModel() = viewModel.shareData.travelInfoChoice

    override val headerTextId = R.string.travel_disclosure_header
    override val bodyTextId: Int? = null
    override val firstChoiceTextId = R.string.travel_disclosure_no
    override val secondChoiceTextId = R.string.travel_disclosure_yes
    override val footerTextId: Int? = null

    override fun getNextDirections(choice: Choice) = when (choice) {
        Choice.FIRST -> TravelDisclosureFragmentDirections.toSummaryConsent()
        Choice.SECOND -> TravelDisclosureFragmentDirections.toCountryList()
    }
}