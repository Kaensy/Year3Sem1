package pdm.application.repository

import pdm.application.api.RetrofitClient
import pdm.application.api.TournamentResponse
import pdm.application.model.Tournament
import pdm.application.util.SessionManager

class TournamentRepository(private val sessionManager: SessionManager) {

    suspend fun getTournaments(page: Int = 1, limit: Int = 10): Result<List<Tournament>> {
        return try {
            val token = sessionManager.getAuthToken() ?: return Result.failure(Exception("Not authenticated"))
            val response = RetrofitClient.apiService.getTournaments("Bearer $token", page, limit)

            if (response.isSuccessful && response.body() != null) {
                // Sort tournaments by ID descending (newest first)
                val sortedTournaments = response.body()!!.tournaments.sortedByDescending { it.id.toInt() }
                Result.success(sortedTournaments)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTournamentById(id: String): Result<Tournament> {
        return try {
            val token = sessionManager.getAuthToken() ?: return Result.failure(Exception("Not authenticated"))
            val response = RetrofitClient.apiService.getTournamentById("Bearer $token", id)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTournament(tournament: Tournament): Result<Tournament> {
        return try {
            val token = sessionManager.getAuthToken() ?: return Result.failure(Exception("Not authenticated"))
            val response = RetrofitClient.apiService.createTournament("Bearer $token", tournament)

            if (response.isSuccessful && response.body() != null) {
                // Refresh tournament list
                getTournaments()
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun updateTournament(id: String, tournament: Tournament): Result<Tournament> {
        return try {
            val token = sessionManager.getAuthToken() ?: return Result.failure(Exception("Not authenticated"))
            val response = RetrofitClient.apiService.updateTournament("Bearer $token", id, tournament)

            if (response.isSuccessful && response.body() != null) {
                getTournaments() // Refresh list after update
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}