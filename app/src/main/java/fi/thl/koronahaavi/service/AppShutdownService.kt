package fi.thl.koronahaavi.service

import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.ExposureRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppShutdownService @Inject constructor(
    private val appStateRepository: AppStateRepository,
    private val workDispatcher: WorkDispatcher,
    private val exposureNotificationService: ExposureNotificationService,
    private val exposureRepository: ExposureRepository
    ) {

    suspend fun shutdownByConfiguration(configuration: ExposureConfigurationData) {
        if (configuration.endOfLifeReached == true) {
            shutdown()
        }
    }

    suspend fun shutdown() {
        appStateRepository.setAppShutdown(true)
        exposureRepository.deleteAllExposures()
        exposureRepository.deleteAllKeyGroupTokens()
        exposureNotificationService.disable()
        workDispatcher.cancelAllWorkers()
    }
}