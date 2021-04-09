package fi.thl.koronahaavi.diagnosis

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import fi.thl.koronahaavi.common.ChoiceData
import fi.thl.koronahaavi.common.combineWith
import fi.thl.koronahaavi.data.SettingsRepository

/**
 * View model data elements for diagnosis data share to EU and travel
 * information selections
 */
class ShareTravelChoiceData(settingsRepository: SettingsRepository) {

    // radio button choices
    val consentChoice = ChoiceData(positiveChoice = ChoiceData.Choice.FIRST)
    val travelInfoChoice = ChoiceData(positiveChoice = ChoiceData.Choice.SECOND)
    val otherCountrySelected = MutableLiveData<Boolean>()

    fun shareToEU() = consentChoice.selectedPositive
    fun hasTraveled() = travelInfoChoice.selectedPositive

    // summary accept checkboxes
    val dataUseAccepted = MutableLiveData<Boolean>()
    val dataShareAccepted = MutableLiveData<Boolean>()

    val summaryShowCountries = shareToEU().combineWith(hasTraveled()) { share , travel ->
        share == true && travel == true
    }

    // travel choice section hidden if user was not able to select it
    val summaryShowTravelChoice = shareToEU().combineWith(hasTraveled()) { share , travel ->
        share == true && travel != null
    }

    val summaryContinueAllowed = dataUseAccepted.combineWith(dataShareAccepted, shareToEU()) { use, share, shareSelected ->
        use == true && (share == true || shareSelected == false)
    }

    private val allCountries = settingsRepository.getExposureConfiguration()?.availableCountries

    private val selectedCountries = MutableLiveData<Set<String>>(setOf())

    fun traveledToCountries(): Set<String> =
        (if (travelInfoChoice.isPositive() == true) selectedCountries.value else null)
            ?: setOf()

    fun setCountrySelection(code: String, isSelected: Boolean) {
        selectedCountries.postValue(
                if (isSelected)
                    setOf(code).union(selectedCountries.value ?: setOf())
                else
                    selectedCountries.value?.minus(code)
        )
    }

    val countries: LiveData<List<CountryData>> = selectedCountries.map { selected ->
        allCountries?.map { code ->
            CountryData(
                code = code,
                isSelected = selected.contains(code)
            )
        } ?: listOf()
    }

    // do not show travel choice if countries not available
    fun isTravelSelectionAvailable() = allCountries?.isNotEmpty() == true
}

data class CountryData(
    val code: String,
    val isSelected: Boolean
)