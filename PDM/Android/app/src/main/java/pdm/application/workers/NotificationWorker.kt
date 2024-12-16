package pdm.application.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.firstOrNull
import pdm.application.model.Tournament
import pdm.application.model.TournamentStatus
import pdm.application.repository.TournamentRepository
import pdm.application.util.NotificationHelper
import pdm.application.util.SessionManager
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val notificationHelper = NotificationHelper(appContext)
    private val repository = TournamentRepository(appContext, SessionManager(appContext))
    private val dateTimeFormatter = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())

    override suspend fun doWork(): Result {
        Log.d("NotificationWorker", "Starting notification check")
        try {
            val tournaments = repository.tournaments.firstOrNull() ?: emptyList()
            Log.d("NotificationWorker", "Checking ${tournaments.size} tournaments")
            checkAndNotifyTournaments(tournaments)
            return Result.success()
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Error checking tournaments", e)
            return if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun checkAndNotifyTournaments(tournaments: List<Tournament>) {
        val now = Date()

        tournaments.forEach { tournament ->
            // Check registration deadlines
            checkRegistrationDeadline(tournament, now)

            // Check tournament start times
            checkTournamentStart(tournament, now)

            // Check tournament completion
            checkTournamentCompletion(tournament)
        }
    }

    private fun checkRegistrationDeadline(tournament: Tournament, now: Date) {
        if (!tournament.isRegistrationOpen) return

        val timeToStart = tournament.startDate.time - now.time
        val hoursToStart = TimeUnit.MILLISECONDS.toHours(timeToStart)

        when {
            // 7 days before
            hoursToStart in 167..168 -> {
                val message = "Registration for ${tournament.name} closes in 7 days! Tournament starts on ${dateTimeFormatter.format(tournament.startDate)}"
                notificationHelper.showRegistrationReminder(tournament.name, hoursToStart.toInt(), tournament.id, message)
            }
            // 3 days before
            hoursToStart in 71..72 -> {
                val message = "Only 3 days left to register for ${tournament.name}! Tournament starts on ${dateTimeFormatter.format(tournament.startDate)}"
                notificationHelper.showRegistrationReminder(tournament.name, hoursToStart.toInt(), tournament.id, message)
            }
            // 1 day before
            hoursToStart in 23..24 -> {
                val message = "Last day to register for ${tournament.name}! Tournament starts tomorrow at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(tournament.startDate)}"
                notificationHelper.showRegistrationReminder(tournament.name, hoursToStart.toInt(), tournament.id, message)
            }
            // 12 hours before
            hoursToStart in 11..12 -> {
                val message = "Only 12 hours left to register for ${tournament.name}!"
                notificationHelper.showRegistrationReminder(tournament.name, hoursToStart.toInt(), tournament.id, message)
            }
            // Last hour
            hoursToStart in 0..1 -> {
                val message = "Final call! Registration for ${tournament.name} closes in less than an hour!"
                notificationHelper.showRegistrationReminder(tournament.name, hoursToStart.toInt(), tournament.id, message)
            }
        }
    }

    private fun checkTournamentStart(tournament: Tournament, now: Date) {
        if (tournament.status != TournamentStatus.UPCOMING) return

        val timeToStart = tournament.startDate.time - now.time
        val hoursToStart = TimeUnit.MILLISECONDS.toHours(timeToStart)

        when {
            // 24 hours notice
            hoursToStart in 23..24 -> {
                val message = "Your tournament ${tournament.name} starts tomorrow at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(tournament.startDate)}"
                notificationHelper.showTournamentStartingReminder(tournament.name, tournament.startDate.time, tournament.id, message)
            }
            // 2 hours notice
            hoursToStart in 1..2 -> {
                val message = "Get ready! ${tournament.name} starts in 2 hours"
                notificationHelper.showTournamentStartingReminder(tournament.name, tournament.startDate.time, tournament.id, message)
            }
            // 30 minutes notice
            timeToStart in 29 * 60 * 1000..31 * 60 * 1000 -> {
                val message = "Almost time! ${tournament.name} starts in 30 minutes"
                notificationHelper.showTournamentStartingReminder(tournament.name, tournament.startDate.time, tournament.id, message)
            }
            // Starting now
            timeToStart in -5 * 60 * 1000..5 * 60 * 1000 -> {
                val message = "${tournament.name} is starting now!"
                notificationHelper.showTournamentStartingReminder(tournament.name, tournament.startDate.time, tournament.id, message)
            }
        }
    }

    private fun checkTournamentCompletion(tournament: Tournament) {
        if (tournament.status == TournamentStatus.COMPLETED && tournament.winner != null) {
            val message = "Congratulations to ${tournament.winner} for winning ${tournament.name}!"
            notificationHelper.showTournamentResultNotification(tournament.name, tournament.winner, tournament.id, message)
        }
    }

    companion object {
        const val WORK_NAME = "TOURNAMENT_NOTIFICATIONS"
    }
}