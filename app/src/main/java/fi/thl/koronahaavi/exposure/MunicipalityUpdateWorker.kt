package fi.thl.koronahaavi.exposure

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import fi.thl.koronahaavi.data.MunicipalityRepository
import java.util.concurrent.TimeUnit

/**
 * Downloads municipality list from remote service and updates a local cache file
 */
@HiltWorker
class MunicipalityUpdateWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val municipalityRepository: MunicipalityRepository

) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        municipalityRepository.reloadMunicipalities()
        return Result.success()
    }

    companion object {
        const val MUNICIPALITY_UPDATER_NAME = "MunicipalityUpdateWorker"

        fun schedule(context: Context, reconfigure: Boolean = false) {
            val policy = if (reconfigure) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<MunicipalityUpdateWorker>(48, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                MUNICIPALITY_UPDATER_NAME, policy, request
            )
        }
    }
}