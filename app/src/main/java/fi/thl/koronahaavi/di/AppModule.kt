package fi.thl.koronahaavi.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.thl.koronahaavi.BuildConfig
import fi.thl.koronahaavi.data.*
import fi.thl.koronahaavi.service.UserAgentInterceptor
import fi.thl.koronahaavi.settings.UserPreferences
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton


@Qualifier
annotation class DatabaseName

@Module
@InstallIn(ApplicationComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences {
        return UserPreferences(context)
    }

    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return EncryptedSharedPreferencesWrapper(context)
    }

    // this allows database testing with a different name so it does not interfere with actual app db
    @Singleton @Provides @DatabaseName
    fun databaseName() = "koronavilkku-db"

    @Singleton
    @Provides
    fun provideHttpClient(): OkHttpClient {
        val pinnerBuilder = CertificatePinner.Builder().apply {
            if (BuildConfig.PIN_BACKEND_KEY_HASH.isNotEmpty() && BuildConfig.PIN_BACKEND_KEY_BACKUP_HASH.isNotEmpty()) {
                add(
                    BuildConfig.PIN_BACKEND_HOST,
                    BuildConfig.PIN_BACKEND_KEY_HASH, BuildConfig.PIN_BACKEND_KEY_BACKUP_HASH
                )
            }
        }

        return OkHttpClient.Builder().apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply { setLevel(HttpLoggingInterceptor.Level.BODY) })
            }
            addInterceptor(UserAgentInterceptor())
            certificatePinner(pinnerBuilder.build())
        }.build()
    }

    @Singleton
    @Provides
    fun provideRetrofit(httpClient: OkHttpClient) : Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    @Singleton
    @Provides
    fun provideExposureRepository(keyGroupTokenDao: KeyGroupTokenDao,
                                  exposureDao: ExposureDao,
                                  settingsRepository: SettingsRepository
    ) : ExposureRepository {
        return DefaultExposureRepository(keyGroupTokenDao, exposureDao, settingsRepository)
    }
}