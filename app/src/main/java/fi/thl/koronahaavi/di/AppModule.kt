package fi.thl.koronahaavi.di

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fi.thl.koronahaavi.BuildConfig
import fi.thl.koronahaavi.data.*
import fi.thl.koronahaavi.service.NumericBooleanAdapter
import fi.thl.koronahaavi.service.UserAgentInterceptor
import fi.thl.koronahaavi.settings.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton


@Qualifier
annotation class DatabaseName

@Qualifier
annotation class BaseUrl

@Qualifier
annotation class AppStatePreferencesName

@Module
@InstallIn(SingletonComponent::class)
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

    @Singleton @Provides @AppStatePreferencesName
    fun appStatePreferencesName() = "fi.thl.koronavilkku.app_state_preferences"

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
                addNetworkInterceptor(HttpLoggingInterceptor().apply { setLevel(HttpLoggingInterceptor.Level.BODY) })
            }
            addInterceptor(UserAgentInterceptor())
            certificatePinner(pinnerBuilder.build())
            connectTimeout(20, TimeUnit.SECONDS)
            readTimeout(60, TimeUnit.SECONDS)
        }.build()
    }

    // this allows testing with a different base url
    @Singleton @Provides @BaseUrl
    fun baseUrl() = BuildConfig.BASE_URL

    @Singleton
    @Provides
    fun provideRetrofit(
        httpClient: OkHttpClient,
        @BaseUrl baseUrl: String
    ) : Retrofit {
        val moshi = Moshi.Builder()
            .add(NumericBooleanAdapter())
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    // coroutine scope for operations that should be tied to app process
    @Provides @Singleton
    fun providesApplicationScope() = CoroutineScope(SupervisorJob())
}