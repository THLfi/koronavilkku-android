package fi.thl.koronahaavi

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fi.thl.koronahaavi.data.ExposureRepository
import fi.thl.koronahaavi.di.DatabaseName
import fi.thl.koronahaavi.service.*
import fi.thl.koronahaavi.settings.UserPreferences
import javax.inject.Singleton

// redefine dependencies for android test app
@Module
@InstallIn(SingletonComponent::class)
object TestModule {

    @Singleton
    @Provides
    fun provideTestBackendService(): BackendService {
        return FakeBackendService()
    }

    @Singleton
    @Provides
    fun provideTestMunicipalityService(): MunicipalityService {
        return FakeMunicipalityService()
    }

    @Singleton
    @Provides
    fun provideExposureNotificationService(@ApplicationContext context: Context): ExposureNotificationService {
        return FakeExposureNotificationService(context)
    }

    // use a different shared prefs from real app, and skip encryption
    @Singleton
    @Provides
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences {
        return UserPreferences(context, TEST_USER_PREFERENCES_NAME)
    }

    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(TEST_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    @Singleton @Provides @DatabaseName
    fun databaseName() = "koronavilkku-test-db"

    const val TEST_SHARED_PREFERENCES_NAME = "fi.thl.koronahaavi.test.prefs"
    const val TEST_USER_PREFERENCES_NAME = "fi.thl.koronahaavi.test.user_prefs"
}

