package fi.thl.koronahaavi.exposure

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import fi.thl.koronahaavi.common.RemoteResource
import fi.thl.koronahaavi.data.MunicipalityRepository
import kotlinx.coroutines.launch
import timber.log.Timber

class MunicipalityListViewModel @ViewModelInject constructor(
    private val repository: MunicipalityRepository
) : ViewModel() {

    private val allMunicipalities = MutableLiveData<RemoteResource<List<Municipality>>>()
    private val searchTerm = MutableLiveData<String?>()

    private val filteredNameList = MediatorLiveData<List<MunicipalityNameData>>().apply {
        addSource(allMunicipalities) { updateFilteredList() }
        addSource(searchTerm ) { updateFilteredList() }
    }
    fun filteredNameList(): LiveData<List<MunicipalityNameData>> = filteredNameList

    fun reloadMunicipalities() {
        Timber.d("Reloading municipalities")
        viewModelScope.launch {
            allMunicipalities.postValue(RemoteResource.Loading())
            allMunicipalities.postValue(
                repository.loadAll()?.let { RemoteResource.Ready(it) }
                    ?: RemoteResource.Failed()
            )
        }
    }

    val showLoading = allMunicipalities.map {
        it is RemoteResource.Loading
    }

    val showFailed = allMunicipalities.map {
        it is RemoteResource.Failed
    }

    val showNoResults = filteredNameList.map { it.isEmpty() }

    private fun updateFilteredList() {
        if (allMunicipalities.value is RemoteResource.Ready) {
            filteredNameList.value = allMunicipalities.value?.data?.filter {
                searchTerm.value?.let { term ->
                    it.name.startsWith(term, ignoreCase = true)
                } ?: true
            }?.map {
                MunicipalityNameData(code = it.code, name = it.name)
            }
        }
    }

    fun search(term: String?) {
        searchTerm.postValue(term)
    }

    fun getMunicipality(code: String) =
        (allMunicipalities.value as? RemoteResource.Ready)?.let {
            it.data?.find { m ->
                m.code == code
            }
        }

    fun getMunicipalityData(code: String) =
        getMunicipality(code)?.let {
            MunicipalityData.from(it)
        }
}

data class MunicipalityData(
    val showOmaoloSymptomAssessment: Boolean,
    val showOmaoloContactRequest: Boolean,
    val contacts: List<MunicipalityContact>
) {
    companion object {
        fun from(m: Municipality) = MunicipalityData(
            showOmaoloSymptomAssessment = m.omaoloFeatures.available,
            showOmaoloContactRequest = m.omaoloFeatures.available && m.omaoloFeatures.symptomAssessmentOnly != true,
            contacts = m.contacts
        )
    }
}

data class MunicipalityNameData(
    val code: String,
    val name: String
)