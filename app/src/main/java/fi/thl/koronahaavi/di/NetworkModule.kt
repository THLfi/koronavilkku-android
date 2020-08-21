package fi.thl.koronahaavi.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import fi.thl.koronahaavi.service.BackendService
import fi.thl.koronahaavi.service.MunicipalityService
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun provideBackendService(retrofit: Retrofit): BackendService {
        return retrofit.create(BackendService::class.java)
    }

    @Singleton
    @Provides
    fun provideMunicipalityService(retrofit: Retrofit): MunicipalityService {
        return retrofit.create(MunicipalityService::class.java)
    }

}