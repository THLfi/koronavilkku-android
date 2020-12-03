package fi.thl.koronahaavi.diagnosis

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.jraska.livedata.test
import fi.thl.koronahaavi.data.SettingsRepository
import fi.thl.koronahaavi.utils.TestData
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
                participatingCountries = listOf("DE", "IE", "IT")
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
        data.setCountrySelection("IT", true)

        data.countries.test().assertValue {
            it.size == 3 && it.find(CountryData::isSelected)?.code == "IT"
        }
    }

    @Test
    fun countryUnselected() {
        data.setCountrySelection("IT", true)
        data.setCountrySelection("DE", true)
        data.setCountrySelection("IT", false)

        data.countries.test().assertValue {
            it.first { c -> c.code == "IT" }.isSelected.not() &&
            it.first { c -> c.code == "DE" }.isSelected
        }
    }

    @Test
    fun traveledToCountries() {
        data.travelInfoChoice.setPositive()
        data.setCountrySelection("IT", true)
        data.setCountrySelection("DE", true)

        val countries = data.traveledToCountries()
        assertEquals(setOf("IT", "DE"), countries)
    }

    @Test
    fun noTravelSelected() {
        data.setCountrySelection("it", true)
        data.setCountrySelection("de", true)

        val countries = data.traveledToCountries()
        assertEquals(emptySet<String>(), countries)
    }

    @Test
    fun participatingCountriesValidated() {
        every { settingsRepository.getExposureConfiguration() } returns TestData.exposureConfiguration().copy(
            participatingCountries = listOf("DE", "IE", "", "X", "test", "FI", " ", "3", "dk", "IT")
        )

        data = ShareTravelChoiceData(settingsRepository)

        data.countries.test().assertValue {
            it.size == 3 &&
            it.any { c -> c.code == "IT" } &&
            it.any { c -> c.code == "DE" } &&
            it.any { c -> c.code == "IE" }
        }
    }
}