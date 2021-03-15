package fi.thl.koronahaavi.di

import android.content.Context
import com.huawei.hms.contactshield.ContactShield
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.service.ExposureNotificationService
import fi.thl.koronahaavi.service.HuaweiContactShieldService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ExposureNotificationModule {

    @Singleton
    @Provides
    fun provideExposureNotificationService(@ApplicationContext context: Context,
                                           appStateRepository: AppStateRepository
    ): ExposureNotificationService {
        val engine = ContactShield.getContactShieldEngine(context)
        return HuaweiContactShieldService(context, engine, appStateRepository)
    }
}
