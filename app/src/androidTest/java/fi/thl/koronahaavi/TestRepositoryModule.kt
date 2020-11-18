package fi.thl.koronahaavi

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import fi.thl.koronahaavi.data.ExposureRepository
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object TestRepositoryModule {

    @Singleton
    @Provides
    fun provideExposureRepository(): ExposureRepository {
        return FakeExposureRepository()
    }
}

