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

    private var editingTournamentId: String? = null

    private var startDate: Date = Date()
    private var endDate: Date = Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)

    fun setStartDate(date: Date) {
        startDate = date
    }

    fun setEndDate(date: Date) {
        endDate = date
    }

    init {
        val sessionManager = SessionManager(application)
        repository = TournamentRepository(sessionManager)
    }

    fun loadTournament(tournamentId: String) {
        viewModelScope.launch {
            try {
                repository.getTournamentById(tournamentId)
                    .onSuccess { tournament ->
                        editingTournamentId = tournamentId
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

    fun saveTournament(
        name: String,
        description: String,
        participantsCount: Int,
        prizePool: Double,
        isRegistrationOpen: Boolean,
        id: String? = null,  // Optional parameter for editing
        startDate: Date? = null,
        endDate: Date? = null
    ) {
        viewModelScope.launch {
            try {
                val tournament = Tournament(
                    id = id ?: "",
                    name = name,
                    description = description,
                    startDate = startDate ?: Date(),
                    endDate = endDate ?: Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000),
                    participantsCount = participantsCount,
                    prizePool = prizePool,
                    isRegistrationOpen = isRegistrationOpen,
                    winner = null,
                    status = TournamentStatus.UPCOMING,
                    userId = ""
                )

                val result = if (id != null) {
                    repository.updateTournament(id, tournament)
                } else {
                    repository.createTournament(tournament)
                }

                result.onSuccess {
                    _saveResult.value = true
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}