package fi.thl.koronahaavi.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import fi.thl.koronahaavi.TestModule
import fi.thl.koronahaavi.TestRepositoryModule
import fi.thl.koronahaavi.di.AppModule
import fi.thl.koronahaavi.di.DatabaseModule
import fi.thl.koronahaavi.di.ExposureNotificationModule
import fi.thl.koronahaavi.di.NetworkModule
import kotlinx.coroutines.runBlocking
import net.sqlcipher.database.SQLiteException
import org.junit.*
import javax.inject.Inject

@UninstallModules(AppModule::class, NetworkModule::class, ExposureNotificationModule::class, TestRepositoryModule::class)
@HiltAndroidTest
class AppDatabaseTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var appDatabase: AppDatabase
    @Inject lateinit var sharedPreferences: SharedPreferences
    @Inject lateinit var settingsRepository: SettingsRepository
    lateinit var context: Context

    @Before
    fun init() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(TestModule.databaseName())
        // create manually since need to do this before injection
        TestModule.provideSharedPreferences(context).edit().clear().apply()

        // inject will use actual DatabaseModule to create an encrypted database
        // and SettingsRepository to create database password, but database name
        // will be different from actual app db
        hiltRule.inject()
    }

    @After
    fun cleanup() {
        context.deleteDatabase(TestModule.databaseName())
    }

    @Test
    fun openSuccess() {
        appDatabase.assertOpen()
        appDatabase.close()

        val reopenedDb = DatabaseModule.provideDatabase(context, TestModule.databaseName(), settingsRepository)
        reopenedDb.assertOpen()
    }

    @Test(expected = SQLiteException::class)
    fun openFailureWithWrongPassword() {
        appDatabase.assertOpen()
        appDatabase.close()

        sharedPreferences.edit()
            .putString(SettingsRepository.DB_PASSWORD_KEY, Base64.encodeToString("wrong".toByteArray(), Base64.NO_WRAP))
            .apply()

        val reopenedDb = DatabaseModule.provideDatabase(context, TestModule.databaseName(), settingsRepository)
        runBlocking {
            reopenedDb.exposureDao().getAll().isEmpty()
        }
    }

    private fun AppDatabase.assertOpen() {
        runBlocking {
            Assert.assertTrue(exposureDao().getAll().isEmpty())
            Assert.assertTrue(isOpen)
        }
    }
}