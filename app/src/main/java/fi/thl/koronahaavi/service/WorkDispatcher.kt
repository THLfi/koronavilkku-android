package fi.thl.koronahaavi.service

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.SettingsRepository
import fi.thl.koronahaavi.exposure.ClearExpiredExposuresWorker
import fi.thl.koronahaavi.exposure.MunicipalityUpdateWorker
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val appStateRepository: AppStateRepository
)  {
    fun cancelWorkersAfterLock() {
        with (WorkManager.getInstance(context)) {
            cancelUniqueWork(DiagnosisKeyUpdateWorker.KEY_UPDATER_NAME)
            cancelUniqueWork(MunicipalityUpdateWorker.MUNICIPALITY_UPDATER_NAME)
            cancelUniqueWork(DiagnosisKeySendTrafficCoverWorker.SEND_TRAFFIC_COVER_WORKER_NAME)
            // not canceling clear expired exposures worker because they need to be cleared also
            // after app locking
        }
    }

    fun scheduleWorkers(reconfigureStale: Boolean = false) {
        // Attempting to trigger worker to execute by reconfiguring at app startup
        // This seems to be required on some android models, like Xiaomi, to execute worker
        // when Google services periodically restart the app process using WakeUpService

        val reconfigure = reconfigureStale && isLastExposureCheckOld()
        DiagnosisKeyUpdateWorker.schedule(context, settingsRepository.appConfiguration, reconfigure)
        ClearExpiredExposuresWorker.schedule(context, reconfigure)

        MunicipalityUpdateWorker.schedule(context)
        DiagnosisKeySendTrafficCoverWorker.schedule(context)
    }

    private fun isLastExposureCheckOld(): Boolean {
        // last check is considered old if it's been longer than two update intervals, because
        // work manager execution is not exact, and we do not want to always update on process start
        // since that would give the impression that updates never work on the background
        val intervalMinutes = settingsRepository.appConfiguration.pollingIntervalMinutes
        val limit = ZonedDateTime.now().minusMinutes(2 * intervalMinutes)

        return appStateRepository.getLastExposureCheckTime()?.isBefore(limit) != false
    }
}