package fi.thl.koronahaavi.di

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.huawei.hms.api.HuaweiApiAvailability
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.thl.koronahaavi.common.ENEnablerFragment
import fi.thl.koronahaavi.data.SettingsRepository
import fi.thl.koronahaavi.service.ExposureNotificationService
import fi.thl.koronahaavi.service.GoogleExposureNotificationService
import fi.thl.koronahaavi.service.HuaweiContactShieldService
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object ExposureNotificationModule {

    @Singleton
    @Provides
    fun provideExposureNotificationService(@ApplicationContext context: Context): ExposureNotificationService {
        return when (ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) -> {
                Timber.d("create GoogleExposureNotificationService")
                GoogleExposureNotificationService(context)
            }
            HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(context) -> {
                Timber.d("create HuaweiContactShieldService")
                HuaweiContactShieldService(context)
            }
            else -> {
                // default GMS so that system is functional and user is prompted about missing GMS in onboarding
                GoogleExposureNotificationService(context)
            }
        }
    }
}
