package fi.thl.koronahaavi.diagnosis

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.jraska.livedata.test
import fi.thl.koronahaavi.data.SettingsRepository
import fi.thl.koronahaavi.utils.TestData
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ShareTravelChoiceDataTest {
    @get:Rule
    val testRule = InstantTaskExecutorRule()

    private lateinit var data: ShareTravelChoiceData
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun init() {
        settingsRepository = mockk(relaxed = true)

        every { settingsRepository.getExposureConfiguration() } returns TestData.exposureConfiguration().copy(
                participatingCountries = listOf("de", "ie", "it")
        )

        data = ShareTravelChoiceData(settingsRepository)
    }

    @Test
    fun noCountriesSelected() {
        data.countries.test().assertValue {
            it.size == 3 && it.none(CountryData::isSelected)
        }
    }

    @Test
    fun countrySelected() {
        data.setCountrySelection("it", true)

        data.countries.test().assertValue {
            it.size == 3 && it.find(CountryData::isSelected)?.code == "it"
        }
    }

    @Test
    fun countryUnselected() {
        data.setCountrySelection("it", true)
        data.setCountrySelection("de", true)
        data.setCountrySelection("it", false)

        data.countries.test().assertValue {
            it.first { c -> c.code == "it" }.isSelected.not() &&
            it.first { c -> c.code == "de" }.isSelected
        }
    }
}