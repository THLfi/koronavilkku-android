package fi.thl.koronahaavi.service

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.thl.koronahaavi.data.SettingsRepository
import fi.thl.koronahaavi.exposure.ClearExpiredExposuresWorker
import fi.thl.koronahaavi.exposure.MunicipalityUpdateWorker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
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

    fun scheduleWorkers() {
        DiagnosisKeyUpdateWorker.schedule(context, settingsRepository.appConfiguration)
        MunicipalityUpdateWorker.schedule(context)
        DiagnosisKeySendTrafficCoverWorker.schedule(context)
        ClearExpiredExposuresWorker.schedule(context)
    }

    fun runUpdateWorker(): Operation = DiagnosisKeyUpdateWorker.runOnce(context)
}