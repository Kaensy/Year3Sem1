package pdm.application.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import pdm.application.api.RetrofitClient
import pdm.application.data.local.AppDatabase
import pdm.application.model.Tournament
import pdm.application.model.TournamentStatus
import pdm.application.model.toDomain
import pdm.application.model.toEntity
import pdm.application.util.NotificationHelper
import pdm.application.util.SessionManager
import java.util.Date

class TournamentRepository(
    private val context: Context,
    private val sessionManager: SessionManager
) {
    private val tournamentDao = AppDatabase.getDatabase(context).tournamentDao()
    private val _isRefreshing = MutableStateFlow(false)
    // Get tournaments from local database as Flow
    val tournaments: Flow<List<Tournament>> = tournamentDao.getAllTournaments()
        .map { entities -> entities.map { it.toDomain() } }

    suspend fun clearAllData() {
        tournamentDao.deleteAllTournaments()
    }

    suspend fun refreshTournaments(page: Int = 1, limit: Int = 10) {
        try {
            syncPendingChanges()

            val token = sessionManager.getAuthToken() ?: throw Exception("Not authenticated")
            val response = RetrofitClient.apiService.getTournaments("Bearer $token", page, limit)

            if (response.isSuccessful && response.body() != null) {
                val tournaments = response.body()!!.tournaments
                // Get existing tournaments from local DB
                val existingTournaments = tournamentDao.getAllTournaments().first()

                // Create a map of existing tournaments by ID
                val existingMap = existingTournaments.associateBy { it.id }

                // Merge remote data with local data
                val mergedEntities = tournaments.map { remoteTournament ->
                    val existingTournament = existingMap[remoteTournament.id]
                    remoteTournament.toEntity().copy(
                        latitude = existingTournament?.latitude ?: remoteTournament.latitude,
                        longitude = existingTournament?.longitude ?: remoteTournament.longitude
                    )
                }

                tournamentDao.insertTournaments(mergedEntities)
            } else {
                throw Exception("Failed to fetch tournaments: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }




    suspend fun getTournamentById(id: String): Result<Tournament> {
        return try {
            // First try to get from local database
            val localTournament = tournamentDao.getTournamentById(id)
            if (localTournament != null) {
                Result.success(localTournament.toDomain())
            } else {
                // If not in database, try to fetch from network
                val token = sessionManager.getAuthToken() ?: throw Exception("Not authenticated")
                val response = RetrofitClient.apiService.getTournamentById("Bearer $token", id)

                if (response.isSuccessful && response.body() != null) {
                    val tournament = response.body()!!
                    // Save to local database
                    tournamentDao.insertTournament(tournament.toEntity())
                    Result.success(tournament)
                } else {
                    Result.failure(Exception(response.errorBody()?.string() ?: "Unknown error"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTournament(tournament: Tournament): Result<Tournament> {
        return try {
            if (RetrofitClient.isNetworkAvailable(context)) {
                // Try remote first if network available
                val token = sessionManager.getAuthToken() ?: throw Exception("Not authenticated")
                val response = RetrofitClient.apiService.createTournament("Bearer $token", tournament)

                if (response.isSuccessful && response.body() != null) {
                    val newTournament = response.body()!!
                    tournamentDao.insertTournament(newTournament.toEntity())
                    Result.success(newTournament)
                } else {
                    // If remote fails, save locally
                    tournamentDao.insertTournament(tournament.toEntity())
                    Result.success(tournament)
                }
            } else {
                // If no network, save locally
                tournamentDao.insertTournament(tournament.toEntity())
                Result.success(tournament)
            }
        } catch (e: Exception) {
            // If any error occurs, save locally
            tournamentDao.insertTournament(tournament.toEntity())
            Result.success(tournament)
        }
    }

    // In TournamentRepository.kt
    suspend fun updateTournament(id: String, tournament: Tournament): Result<Tournament> {
        return try {
            // Get the old tournament to check if winner changed
            val oldTournament = tournamentDao.getTournamentById(id)

            // First update local database with pending changes flag
            tournamentDao.updateTournament(tournament.toEntity().copy(hasPendingChanges = true))

            // Check for status changes and send notifications
            if (oldTournament?.status != tournament.status) {
                when (tournament.status) {
                    TournamentStatus.IN_PROGRESS -> {
                        NotificationHelper(context).showTournamentStartingReminder(
                            tournamentName = tournament.name,
                            startTime = tournament.startDate.time,
                            tournamentId = tournament.id,
                            message = "${tournament.name} is starting now!"
                        )
                    }
                    TournamentStatus.COMPLETED -> {
                        if (tournament.winner != null) {
                            NotificationHelper(context).showTournamentResultNotification(
                                tournamentName = tournament.name,
                                winner = tournament.winner,
                                tournamentId = tournament.id,
                                message = "Congratulations to ${tournament.winner} for winning ${tournament.name}!"
                            )
                        }
                    }
                    else -> {} // No notification for UPCOMING status
                }
            }

            if (RetrofitClient.isNetworkAvailable(context)) {
                syncTournament(id, tournament)
            } else {
                Result.success(tournament)
            }
        } catch (e: Exception) {
            Result.success(tournament)
        }
    }

    // In TournamentRepository.kt
    suspend fun checkAndUpdateStatuses() {
        val currentDate = Date()
        val tournaments = tournamentDao.getAllTournaments().first()

        var hasChanges = false
        tournaments.forEach { tournament ->
            val newStatus = when {
                currentDate.before(tournament.startDate) -> TournamentStatus.UPCOMING
                currentDate.after(tournament.endDate) -> TournamentStatus.COMPLETED
                else -> TournamentStatus.IN_PROGRESS
            }

            if (tournament.status != newStatus) {
                hasChanges = true
                val updatedTournament = tournament.toDomain().copy(status = newStatus)
                updateTournament(tournament.id, updatedTournament)
            }
        }

        if (hasChanges) {
            refreshTournaments() // Force immediate refresh if there were changes
        }
    }

    private suspend fun syncTournament(id: String, tournament: Tournament): Result<Tournament> {
        return try {
            val token = sessionManager.getAuthToken() ?: throw Exception("Not authenticated")
            // Make sure there's no trailing slash and the id is included
            val response = RetrofitClient.apiService.updateTournament("Bearer $token", id.trim(), tournament)

            if (response.isSuccessful && response.body() != null) {
                val updatedTournament = response.body()!!
                tournamentDao.updateTournament(updatedTournament.toEntity().copy(hasPendingChanges = false))
                Result.success(updatedTournament)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncPendingChanges() {
        if (!RetrofitClient.isNetworkAvailable(context)) return

        val pendingTournaments = tournamentDao.getTournamentsWithPendingChanges()
        pendingTournaments.forEach { entity ->
            val tournament = entity.toDomain()
            syncTournament(entity.id, tournament)
        }
    }

    suspend fun deleteTournament(tournament: Tournament) {
        try {
            val token = sessionManager.getAuthToken() ?: throw Exception("Not authenticated")
            val response = RetrofitClient.apiService.deleteTournament("Bearer $token", tournament.id)

            if (response.isSuccessful) {
                // Delete from local database
                tournamentDao.deleteTournament(tournament.toEntity())
            }
        } catch (e: Exception) {
            // Handle error
            e.printStackTrace()
        }
    }
}