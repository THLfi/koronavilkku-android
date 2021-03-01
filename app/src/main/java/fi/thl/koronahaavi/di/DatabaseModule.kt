package fi.thl.koronahaavi.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fi.thl.koronahaavi.data.*
import net.sqlcipher.database.SupportFactory
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
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

    @Singleton
    @Provides
    fun provideExposureRepository(keyGroupTokenDao: KeyGroupTokenDao,
                                  exposureDao: ExposureDao,
                                  settingsRepository: SettingsRepository
    ) : ExposureRepository {
        return DefaultExposureRepository(keyGroupTokenDao, exposureDao, settingsRepository)
    }

    // first app version 1.0 had schema version 3, so this is the first migration we need to address
    val migrationThreeToFour = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            Timber.d("Migrating from schema 3 to 4")
            database.execSQL("ALTER TABLE key_group_token ADD COLUMN exposure_count INTEGER")
            database.execSQL("ALTER TABLE key_group_token ADD COLUMN latest_exposure_date INTEGER")
        }
    }
}