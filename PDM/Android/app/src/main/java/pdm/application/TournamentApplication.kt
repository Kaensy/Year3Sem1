package pdm.application

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.work.*
import pdm.application.data.local.AppDatabase
import pdm.application.workers.CountdownWorker
import pdm.application.workers.NotificationWorker
import pdm.application.workers.TournamentSyncWorker
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import androidx.work.WorkRequest
import pdm.application.util.NetworkUtil
import pdm.application.util.LightSensorManager

class TournamentApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    private lateinit var lightSensorManager: LightSensorManager

    override fun onCreate() {
        super.onCreate()
        NetworkUtil.startNetworkMonitoring(this)
        setupPeriodicWorkManager()

        // Initialize and start light sensor
        lightSensorManager = LightSensorManager(this)
        lightSensorManager.startListening()
    }

    override fun onTerminate() {
        super.onTerminate()
        lightSensorManager.stopListening()
    }

//    private fun setupPeriodicWorkManager() {
//        val constraints = Constraints.Builder()
//            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
//            .build()
//
//        // Initial sync work
//        val syncWork = OneTimeWorkRequestBuilder<TournamentSyncWorker>()
//            .setConstraints(constraints)
//            .build()
//
//        // Notification check work
//        val notificationWork = OneTimeWorkRequestBuilder<NotificationWorker>()
//            .setConstraints(constraints)
//            .build()
//
//        // Chain work requests
//        WorkManager.getInstance(this).let { workManager ->
//            // Initial chain
//            workManager.beginWith(syncWork)
//                .then(notificationWork)
//                .enqueue()
//
//            // Schedule periodic work
//            val periodicSync = PeriodicWorkRequestBuilder<TournamentSyncWorker>(
//                15, TimeUnit.MINUTES,
//                5, TimeUnit.MINUTES
//            ).build()
//
//            val periodicNotification = PeriodicWorkRequestBuilder<NotificationWorker>(
//                15, TimeUnit.MINUTES,
//                5, TimeUnit.MINUTES
//            ).build()
//
//            workManager.enqueueUniquePeriodicWork(
//                "PERIODIC_SYNC",
//                ExistingPeriodicWorkPolicy.REPLACE,
//                periodicSync
//            )
//
//            workManager.enqueueUniquePeriodicWork(
//                "PERIODIC_NOTIFICATIONS",
//                ExistingPeriodicWorkPolicy.REPLACE,
//                periodicNotification
//            )
//        }
//    }

    private fun setupPeriodicWorkManager() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        // Initial sync and countdown work
        val syncRequest = OneTimeWorkRequestBuilder<TournamentSyncWorker>()
            .setConstraints(constraints)
            .build()

        val countdownWork = OneTimeWorkRequestBuilder<CountdownWorker>()
            .build()

        // Chain work requests
        WorkManager.getInstance(this).let { workManager ->
            // Initial chain
            workManager.beginWith(syncRequest)
                .then(countdownWork)
                .enqueue()

            // Schedule periodic checks using Handler for more frequent updates (every 30 seconds)
            Handler(Looper.getMainLooper()).let { handler ->
                val checkRunnable = object : Runnable {
                    override fun run() {
                        workManager.beginWith(
                            OneTimeWorkRequestBuilder<TournamentSyncWorker>().build()
                        ).then(
                            OneTimeWorkRequestBuilder<CountdownWorker>().build()
                        ).enqueue()
                        handler.postDelayed(this, 30_000) // Run every 30 seconds
                    }
                }
                handler.post(checkRunnable)
            }
        }
    }
    companion object {
        fun startOneTimeSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<TournamentSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(syncRequest)
        }

        fun scheduleImmediateNotificationCheck(context: Context) {
            val request = OneTimeWorkRequestBuilder<NotificationWorker>()
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}