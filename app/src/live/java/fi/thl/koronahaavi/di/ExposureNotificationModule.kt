package fi.thl.koronahaavi.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.thl.koronahaavi.data.SettingsRepository
import fi.thl.koronahaavi.service.ExposureNotificationService
import fi.thl.koronahaavi.service.GoogleExposureNotificationService
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object ExposureNotificationModule {

    @Singleton
    @Provides
    fun provideExposureNotificationService(@ApplicationContext context: Context): ExposureNotificationService {
        return GoogleExposureNotificationService(context)
    }
}
