package pdm.application.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import pdm.application.repository.TournamentRepository
import pdm.application.util.NetworkUtil
import pdm.application.util.SessionManager
import java.io.IOException

class TournamentSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting work - checking tournament statuses")

        try {
            val sessionManager = SessionManager(applicationContext)
            val repository = TournamentRepository(applicationContext, sessionManager)

            if (sessionManager.isLoggedIn()) {
                val beforeUpdate = repository.tournaments.first() // Get current state
                Log.d(TAG, "Current tournaments before update: ${beforeUpdate.map { it.status }}")

                repository.checkAndUpdateStatuses()

                val afterUpdate = repository.tournaments.first() // Get state after update
                Log.d(TAG, "Tournaments after update: ${afterUpdate.map { it.status }}")

                repository.refreshTournaments()
                Log.d(TAG, "Work completed successfully")
                return Result.success()
            }

            Log.d(TAG, "User not logged in")
            return Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "Error during work", e)
            return if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "TournamentSyncWorker"
    }
}