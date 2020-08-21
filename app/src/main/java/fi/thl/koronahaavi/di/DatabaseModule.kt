package fi.thl.koronahaavi.di

import android.content.Context
import androidx.room.Room
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
            .fallbackToDestructiveMigration()
            .openHelperFactory(SupportFactory(settingsRepository.getOrCreateDatabasePassword()))
            .build()
    }

    @Singleton
    @Provides
    fun provideKeyGroupTokenDao(database: AppDatabase) = database.keyGroupTokenDao()

    @Singleton
    @Provides
    fun provideExposureDao(database: AppDatabase) = database.exposureDao()
}