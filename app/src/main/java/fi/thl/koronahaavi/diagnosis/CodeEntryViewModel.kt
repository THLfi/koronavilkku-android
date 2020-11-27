package fi.thl.koronahaavi.diagnosis

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import fi.thl.koronahaavi.common.ChoiceData
import fi.thl.koronahaavi.common.ChoiceData.Choice
import fi.thl.koronahaavi.common.Event
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.SettingsRepository
import fi.thl.koronahaavi.service.*
import fi.thl.koronahaavi.service.ExposureNotificationService.ResolvableResult.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class CodeEntryViewModel @ViewModelInject constructor(
    private val exposureNotificationService: ExposureNotificationService,
    private val diagnosisKeyService: DiagnosisKeyService,
    private val appStateRepository: AppStateRepository,
    private val workDispatcher: WorkDispatcher,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val shareConsentModel = ChoiceData(positiveChoice = Choice.FIRST)
    val travelSelectionModel = ChoiceData(positiveChoice = Choice.SECOND)

    fun shareToEU() = shareConsentModel.selectedPositive
    fun hasTraveled() = travelSelectionModel.selectedPositive

    private val allCountries = settingsRepository.getExposureConfiguration()?.participatingCountries
    private val selectedCountries = MutableLiveData<Set<String>>(setOf())

    val countries: LiveData<List<CountryData>> = selectedCountries.map { selected ->
        allCountries?.map { code ->
            CountryData(
                code = code,
                name = Locale("", code).getDisplayCountry(Locale.getDefault()),
                isSelected = selected.contains(code)
            )
        } ?: listOf()
    }

    fun setCountrySelection(code: String, isSelected: Boolean) {
        selectedCountries.postValue(
            if (isSelected)
                setOf(code).union(selectedCountries.value ?: setOf())
            else
                selectedCountries.value?.minus(code)
        )
    }

    val code = MutableLiveData<String>()

    val maxCodeLength = settingsRepository.appConfiguration.tokenLength

    private val keyHistoryResolutionMutableEvent = MutableLiveData<Event<Status>>()
    fun keyHistoryResolutionEvent(): LiveData<Event<Status>> = keyHistoryResolutionMutableEvent

    private val enEnabled = exposureNotificationService.isEnabledFlow().asLiveData()
    private val lockedAfterDiagnosis = appStateRepository.lockedAfterDiagnosis().asLiveData()

    private val submitError = MutableLiveData<CodeEntryError?>()
    val codeEntryError = MediatorLiveData<CodeEntryError?>().apply {
        addSource(submitError) { updateCodeEntryError() }
        addSource(lockedAfterDiagnosis) { updateCodeEntryError() }
        addSource(enEnabled) { updateCodeEntryError() }
    }

    private fun updateCodeEntryError() {
        codeEntryError.value = when {
            (lockedAfterDiagnosis.value == true) -> CodeEntryError.LockedAfterDiagnosis
            (enEnabled.value == false) -> CodeEntryError.ExposureNotificationDisabled
            else -> submitError.value
        }
    }

    private val keysSubmittedEvent = MutableLiveData<Event<Boolean>>()
    fun keysSubmittedEvent(): LiveData<Event<Boolean>> = keysSubmittedEvent

    private val submitInProgress = MutableLiveData<Boolean>(false)
    fun submitInProgress(): LiveData<Boolean> = submitInProgress

    private val submitAllowed = MediatorLiveData<Boolean>().apply {
        addSource(submitInProgress) { updateSubmitAllowed() }
        addSource(enEnabled) { updateSubmitAllowed() }
        addSource(lockedAfterDiagnosis) { updateSubmitAllowed() }
        addSource(code) { updateSubmitAllowed() }
    }
    fun submitAllowed(): LiveData<Boolean> = submitAllowed

    private fun updateSubmitAllowed() {
        submitAllowed.value = when {
            (lockedAfterDiagnosis.value == true) -> false
            (enEnabled.value == false) -> false
            (submitInProgress.value == true) -> false
            else -> (code.value?.isNotBlank() == true)
        }
    }

    private fun clearError() {
        submitError.value = null
    }

    fun submit() {
        clearError()
        submitInProgress.value = true

        viewModelScope.launch {
            code.value?.let {
                getKeysAndSend(it.trim())
            }
            submitInProgress.value = false
        }
    }

    private suspend fun getKeysAndSend(authCode: String) {
        when (val result = exposureNotificationService.getTemporaryExposureKeys()) {
            is Success -> {
                Timber.d("Sending keys, size ${result.data.size}")
                sendKeys(authCode, result.data)
            }
            is ResolutionRequired -> {
                // fragment needs to trigger the flow to resolve request
                keyHistoryResolutionMutableEvent.postValue(Event(result.status))
            }
            is Failed -> {
                Timber.e("Failed to get exposure keys")
                submitError.postValue(CodeEntryError.Failed)
            }
        }
    }

    private suspend fun sendKeys(authCode: String, keys: List<TemporaryExposureKey>) {
        // todo replace with actual UI selections
        val selectedCountries = settingsRepository.getExposureConfiguration()?.participatingCountries?.take(2)
            ?: listOf()
        val consentToShare = shareConsentModel.isPositive()
            ?: false

        when (diagnosisKeyService.sendExposureKeys(
            authCode = authCode,
            keyHistory = keys,
            visitedCountryCodes = selectedCountries,
            consentToShare = consentToShare
        )) {
            is SendKeysResult.Success -> {
                // stop observing locked state since we are about to lock the app, and error
                // might have time to show in UI before navigating away
                codeEntryError.removeSource(lockedAfterDiagnosis)
                codeEntryError.removeSource(enEnabled)

                appStateRepository.setDiagnosisKeysSubmitted(true)
                workDispatcher.cancelWorkersAfterLock()
                exposureNotificationService.disable()
                keysSubmittedEvent.postValue(Event(true))
            }
            is SendKeysResult.Unauthorized -> {
                submitError.postValue(CodeEntryError.Auth)
            }
            else -> {
                submitError.postValue(CodeEntryError.Failed)
            }
        }
    }
}

data class CountryData(
    val code: String,
    val name: String,
    val isSelected: Boolean
)

sealed class CodeEntryError {
    object Auth : CodeEntryError()
    object ExposureNotificationDisabled: CodeEntryError()
    object LockedAfterDiagnosis: CodeEntryError()
    object Failed : CodeEntryError()
}