package pdm.application.ui.edit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pdm.application.model.Tournament
import pdm.application.model.TournamentStatus
import pdm.application.repository.TournamentRepository
import pdm.application.util.SessionManager
import java.util.Date

class TournamentEditViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TournamentRepository
    private val _tournament = MutableLiveData<Tournament>()
    val tournament: LiveData<Tournament> = _tournament

    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error


    init {
        val sessionManager = SessionManager(application)
        repository = TournamentRepository(application, sessionManager)
    }

    fun loadTournament(tournamentId: String) {
        viewModelScope.launch {
            try {
                repository.getTournamentById(tournamentId)
                    .onSuccess { tournament ->
                        _tournament.value = tournament
                    }
                    .onFailure { exception ->
                        _error.value = exception.message
                    }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun saveTournament(tournament: Tournament) {
        viewModelScope.launch {
            try {
                repository.updateTournament(tournament.id, tournament)
                    .onSuccess {
                        _saveResult.value = true
                    }
                    .onFailure { exception ->
                        _error.value = exception.message
                    }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun createTournament(
        name: String,
        description: String,
        participantsCount: Int,
        prizePool: Double,
        isRegistrationOpen: Boolean,
        startDate: Date,
        endDate: Date,
        latitude: Double?,
        longitude: Double?,
        status: TournamentStatus,
        winner: String?
    ) {
        viewModelScope.launch {
            try {
                val tournament = Tournament(
                    id = "",
                    name = name,
                    description = description,
                    startDate = startDate,
                    endDate = endDate,
                    participantsCount = participantsCount,
                    prizePool = prizePool,
                    isRegistrationOpen = isRegistrationOpen,
                    winner = winner,
                    status = status,
                    userId = "",
                    latitude = latitude,
                    longitude = longitude
                )

                repository.createTournament(tournament)
                    .onSuccess { _saveResult.value = true }
                    .onFailure { exception -> _error.value = exception.message }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}