package fi.thl.koronahaavi.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.thl.koronahaavi.service.FakeExposureNotificationService
import fi.thl.koronahaavi.service.ExposureNotificationService
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object ExposureNotificationModule {

    @Singleton
    @Provides
    fun provideExposureNotificationService(@ApplicationContext context: Context): ExposureNotificationService {
        return FakeExposureNotificationService(context)
    }
}
