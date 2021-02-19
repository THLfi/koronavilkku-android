package fi.thl.koronahaavi.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fi.thl.koronahaavi.service.ExposureNotificationService
import fi.thl.koronahaavi.service.GoogleExposureNotificationService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ExposureNotificationModule {

    @Singleton
    @Provides
    fun provideExposureNotificationService(@ApplicationContext context: Context): ExposureNotificationService {
        return GoogleExposureNotificationService(context)
    }
}
