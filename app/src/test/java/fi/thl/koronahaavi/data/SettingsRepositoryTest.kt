package fi.thl.koronahaavi.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.thl.koronahaavi.service.AppConfiguration
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This runs as local JVM unit test with Robolectric
 */
/*
@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun appConfigurationFromPreferences() {
        val prefs = context.getSharedPreferences("TEST", Context.MODE_PRIVATE)
        val saveCfg = AppConfiguration(120, 60, 20, 12)
        prefs.saveAppConfiguration(saveCfg)
        val loadCfg = prefs.loadAppConfiguration()
        assertEquals(saveCfg, loadCfg)
    }
}

 */
