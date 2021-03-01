package fi.thl.koronahaavi

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fi.thl.koronahaavi.data.ExposureRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TestRepositoryModule {

    @Singleton
    @Provides
    fun provideExposureRepository(): ExposureRepository {
        return FakeExposureRepository()
    }
}

