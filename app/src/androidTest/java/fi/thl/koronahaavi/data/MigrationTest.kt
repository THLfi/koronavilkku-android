package fi.thl.koronahaavi.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.thl.koronahaavi.di.DatabaseModule
import org.junit.*
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val testDatabase = "migration-test"
    private val allMigrations = arrayOf(DatabaseModule.migrationThreeToFour)

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        // Create earliest version of the database.
        helper.createDatabase(testDatabase, 3).apply {
            close()
        }

        // Open latest version of the database. Room will validate the schema
        // once all migrations execute.
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            testDatabase
        ).addMigrations(*allMigrations).build().apply {
            openHelper.writableDatabase
            close()
        }
    }
}