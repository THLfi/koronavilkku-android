package fi.thl.koronahaavi.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.thl.koronahaavi.data.AppDatabase
import fi.thl.koronahaavi.data.SettingsRepository
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object DatabaseModule {
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context,
                        @DatabaseName databaseName: String,
                        settingsRepository: SettingsRepository
    ): AppDatabase {

        return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, databaseName)
                .addMigrations(migrationThreeToFour)
                .openHelperFactory(SupportFactory(settingsRepository.getOrCreateDatabasePassword()))
                .build()
    }

    @Singleton
    @Provides
    fun provideKeyGroupTokenDao(database: AppDatabase) = database.keyGroupTokenDao()

    @Singleton
    @Provides
    fun provideExposureDao(database: AppDatabase) = database.exposureDao()

    // first app version 1.0 had schema version 3, so this is the first migration we need to address
    private val migrationThreeToFour = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE key_group_token ADD COLUMN exposure_count INTEGER")
            database.execSQL("ALTER TABLE key_group_token ADD COLUMN latest_exposure_date INTEGER")
        }
    }
}