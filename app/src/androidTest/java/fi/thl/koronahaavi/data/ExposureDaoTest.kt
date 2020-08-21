package fi.thl.koronahaavi.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime


@RunWith(AndroidJUnit4::class)
class ExposureDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var exposureDao: ExposureDao

    @Before
    fun init() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        exposureDao = db.exposureDao()
    }

    @After
    fun cleanup() {
        db.close()
    }

    @Test
    fun writeExposuresAndReadInList() {
        val now = ZonedDateTime.now()
        val e = Exposure(1, now.minusDays(1), now, 200)

        runBlocking {
            exposureDao.insert(e)
            val exposures = exposureDao.getAll()
            Assert.assertEquals(e.detectedDate.toEpochSecond(), exposures[0].detectedDate.toEpochSecond())
        }
    }
}