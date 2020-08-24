package fi.thl.koronahaavi

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import dagger.hilt.android.HiltAndroidApp
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.service.WorkDispatcher
import timber.log.Timber
import timber.log.Timber.DebugTree
import javax.inject.Inject

@HiltAndroidApp
class VilkkuApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var workDispatcher: WorkDispatcher
    @Inject lateinit var appStateRepository: AppStateRepository

    // this is required to inject workers
    override fun getWorkManagerConfiguration() =
        Configuration.Builder().apply {
            setWorkerFactory(workerFactory)
            if (BuildConfig.DEBUG) {
                setMinimumLoggingLevel(Log.DEBUG)
            }
        }.build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(DebugTree())

        // This is an additional check to make sure workers are scheduled in case
        // app was force-stopped from background and woken up by EN api service.
        if (appStateRepository.isOnboardingComplete()) {
            workDispatcher.scheduleWorkers()
        }
    }
}
