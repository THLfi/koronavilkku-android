package fi.thl.koronahaavi.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import fi.thl.koronahaavi.service.SystemOperations
import fi.thl.koronahaavi.service.MunicipalityService
import fi.thl.koronahaavi.utils.MainCoroutineScopeRule
import io.mockk.*
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.*

class MunicipalityRepositoryTest {
    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()
    @get:Rule
    val testRule = InstantTaskExecutorRule()

    private lateinit var repository: MunicipalityRepository

    private lateinit var municipalityService: MunicipalityService
    private lateinit var systemOperations: SystemOperations

    @get:Rule
    val folder = TemporaryFolder()

    @Before
    fun init() {
        municipalityService = mockk(relaxed = true)
        systemOperations = mockk()

        Locale.setDefault(Locale("fi"))

        every { systemOperations.fileExistsInPersistedStorage(any()) } returns true
        every { systemOperations.createFileInPersistedStorage(any()) } returns
                File(javaClass.getResource("/municipalities.json")!!.file)

        repository = MunicipalityRepository(municipalityService, systemOperations)
    }

    @Test
    fun loadsJsonFromFile() {
        runBlocking {
            val list = repository.loadAll()
            assertEquals(2, list?.size)
            assertTrue(list?.any { it.code == "837" && it.name == "Tampere" } == true)
        }
    }

    @Test
    fun usesSwedish() {
        Locale.setDefault(Locale("sv"))
        runBlocking {
            val list = repository.loadAll()
            assertEquals(2, list?.size)
            assertTrue(list?.any { it.code == "837" && it.name == "Tammerfors" } == true)
        }
    }

    @Test
    fun usesEnglish() {
        Locale.setDefault(Locale("en"))
        runBlocking {
            val list = repository.loadAll()
            assertEquals(2, list?.size)
            assertTrue(list?.any { it.code == "091" && it.contacts[0].title == "Meilahti Hospital" } == true)
        }
    }

    @Test
    fun downloadsJson() {
        every { systemOperations.fileExistsInPersistedStorage(any()) } returns false
        every { systemOperations.createFileInPersistedStorage(any()) } returns folder.newFile()
        coEvery { municipalityService.getMunicipalities(any()) } returns testInput().toResponseBody()

        runBlocking {
            repository.loadAll()
            coVerify { municipalityService.getMunicipalities(any()) }
        }
    }

    @Test
    fun recoversFromInvalidJsonFile() {
        // mock two return values that are returned for first and second call
        // so that it won't overwrite the invalid test file in resources
        every { systemOperations.createFileInPersistedStorage(any()) } returns
                File(javaClass.getResource("/municipalities_invalid.json")!!.file) andThen
                folder.newFile()

        // this should be called after an error occurs when reading invalid file
        coEvery { municipalityService.getMunicipalities(any()) } returns testInput().toResponseBody()

        runBlocking {
            val list = repository.loadAll()

            coVerify { municipalityService.getMunicipalities(any()) }
            assertTrue(list?.any { it.code == "123" } == true)
        }
    }

    @Test
    fun recoversFromEmptyJsonFile() {
        every { systemOperations.createFileInPersistedStorage(any()) } returns folder.newFile()

        coEvery { municipalityService.getMunicipalities(any()) } returns testInput().toResponseBody()

        runBlocking {
            val list = repository.loadAll()

            coVerify { municipalityService.getMunicipalities(any()) }
            assertTrue(list?.any { it.code == "123" } == true)
        }
    }

    private fun testInput() = """
        [
            {
                "code": "123",
                "name": {
                    "fi": "nimi",
                    "sv": "namn",
                    "en": "name"
                },
                "omaolo": {
                    "available": true,
                    "serviceLanguages": {
                        "fi": true,
                        "sv": false,
                        "en": true
                    },
                    "symptomAssessmentOnly": false
                },
                "contact":
                [
                    {
                        "title": {
                            "fi" : "PSHP:n neuvontapuhelin",
                            "sv" : "PSHP samma på svenska",
                            "en" : "PSHP info line"
                        },
                        "phoneNumber": "+358 40 123 4567",
                        "info": {
                            "fi" : "Maanantai-perjantai 8-16",
                            "sv" : "Måndag - fredag 8-16",
                            "en" : "Monday to Friday 8am - 4pm"
                        }
                    },
                    {
                        "title": {
                            "fi" : "Toinen neuvontapuhelin",
                            "sv" : "Den andra samma på svenska",
                            "en" : "The other info line"
                        },
                        "phoneNumber": "0401234567",
                        "info": {
                            "fi" : "Maanantai-perjantai 8-16",
                            "sv" : "Måndag - fredag 8-16",
                            "en" : "Monday to Friday 8am - 4pm"
                        }
                    }
                ]
            },
            {
                "code": "091",
                "name": {
                    "fi": "Helsinki",
                    "sv": "Helsingfors",
                    "en": "Helsinki"
                },
                "omaolo": {
                    "available": false,
                    "symptomAssessmentOnly": false
                },
                "contact":
                [
                    {
                        "title": {
                            "fi" : "Kallion pandemiapäivistys",
                            "sv" : "Kallion pandemiapäivistys",
                            "en" : "Kallion pandemiapäivistys"
                        },
                        "phoneNumber": "+358 40 123 4567",
                        "info": {
                            "fi" : "Maanantai-perjantai 8-16",
                            "sv" : "Måndag - fredag 8-16",
                            "en" : "Monday to Friday 8am - 4pm"
                        }
                    },
                    {
                        "title": {
                            "fi" : "Kallion pandemiapäivistys Kallion pandemiapäivistys Kallion pandemiapäivistys ",
                            "sv" : "Kallion pandemiapäivistys Kallion pandemiapäivistys Kallion pandemiapäivistys ",
                            "en" : "Kallion pandemiapäivistysKallion pandemiapäivistys Kallion pandemiapäivistys "
                        },
                        "phoneNumber": "+358 555 123 4567",
                        "info": {
                            "fi" : "Maanantai-perjantai 8-16",
                            "sv" : "Måndag - fredag 8-16",
                            "en" : "Monday to Friday 8am - 4pm Monday to Friday 8am - 4pm Monday to Friday 8am - 4pm Monday to Friday 8am - 4pm"
                        }
                    },
                    {
                        "title": {
                            "fi" : "Kallion pandemiapäivistys",
                            "sv" : "Kallion pandemiapäivistys",
                            "en" : "Kallion pandemiapäivistys"
                        },
                        "phoneNumber": "+358 556 123 4567",
                        "info": {
                            "fi" : "Maanantai-perjantai 8-16",
                            "sv" : "Måndag - fredag 8-16",
                            "en" : "Monday to Friday 8am - 4pm"
                        }
                    }
                ]
            }
        ]
        """.trimIndent()
}