package fi.thl.koronahaavi.exposure

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import fi.thl.koronahaavi.data.MunicipalityRepository
import fi.thl.koronahaavi.utils.MainCoroutineScopeRule
import fi.thl.koronahaavi.utils.TestData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MunicipalityListViewModelTest {
    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @get:Rule
    val testRule = InstantTaskExecutorRule()

    private lateinit var viewModel: MunicipalityListViewModel
    private lateinit var repository: MunicipalityRepository

    private val codeShowOmaoloBoth = "both"
    private val codeShowOmaoloSymptomOnly = "symptom"
    private val codeShowOmaoloNone = "none"

    @Before
    fun init() {
        repository = mockk(relaxed = true)

        coEvery { repository.loadAll() } returns listOf(
            TestData.municipality().copy(
                code = codeShowOmaoloBoth
            ),
            TestData.municipality().copy(
                code = codeShowOmaoloSymptomOnly,
                omaoloFeatures = TestData.omaoloFeatures().copy(symptomAssessmentOnly = true)
            ),
            TestData.municipality().copy(
                code = codeShowOmaoloNone,
                omaoloFeatures = TestData.omaoloFeatures().copy(available = false)
            ),
            TestData.municipality()
        )

        viewModel = MunicipalityListViewModel(repository)
    }

    @Test
    fun omaoloDisplayOptions() {
        runBlocking {
            viewModel.reloadMunicipalities()

            val dataBoth = viewModel.getMunicipalityData(codeShowOmaoloBoth)
            assertTrue(dataBoth?.showOmaoloSymptomAssessment == true)
            assertTrue(dataBoth?.showOmaoloContactRequest == true)

            val dataSymptom = viewModel.getMunicipalityData(codeShowOmaoloSymptomOnly)
            assertTrue(dataSymptom?.showOmaoloSymptomAssessment == true)
            assertTrue(dataSymptom?.showOmaoloContactRequest == false)

            val dataNone = viewModel.getMunicipalityData(codeShowOmaoloNone)
            assertTrue(dataNone?.showOmaoloSymptomAssessment == false)
            assertTrue(dataNone?.showOmaoloContactRequest == false)
        }
    }
}