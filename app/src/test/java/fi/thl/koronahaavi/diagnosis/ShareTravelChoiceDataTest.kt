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
                availableCountries = listOf("DE", "IE", "IT")
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
        // first select countries
        data.setCountrySelection("it", true)
        data.setCountrySelection("de", true)

        // then go back and change travel selection
        data.travelInfoChoice.setNegative()

        val countries = data.traveledToCountries()
        assertEquals(emptySet<String>(), countries)
    }

    @Test
    fun summaryShowCountries() {
        val observer = data.summaryShowCountries.test()
        observer.assertNoValue()

        data.consentChoice.setPositive()
        observer.assertValue(false)

        data.travelInfoChoice.setPositive()
        observer.assertValue(true)

        data.consentChoice.setNegative()
        observer.assertValue(false)
    }

    @Test
    fun summaryContinueAllowed() {
        val observer = data.summaryContinueAllowed.test()
        observer.assertNoValue()

        data.dataUseAccepted.postValue(true)
        observer.assertValue(false)

        data.dataShareAccepted.postValue(true)
        observer.assertValue(true)

        data.dataShareAccepted.postValue(false)
        observer.assertValue(false)

        data.consentChoice.setNegative() // data share not required when this selected
        observer.assertValue(true)
    }
}