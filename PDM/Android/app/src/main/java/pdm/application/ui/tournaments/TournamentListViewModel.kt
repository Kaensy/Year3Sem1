package pdm.application.ui.tournaments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pdm.application.model.Tournament
import pdm.application.repository.TournamentRepository
import pdm.application.util.SessionManager

class TournamentListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TournamentRepository
    private val _tournaments = MutableLiveData<List<Tournament>>()
    val tournaments: LiveData<List<Tournament>> = _tournaments

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        val sessionManager = SessionManager(application)
        repository = TournamentRepository(sessionManager)
        loadTournaments()
    }

    private var currentPage = 1
    private val pageSize = 20  // Increase page size

    fun loadTournaments() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.getTournaments(currentPage, pageSize)
                    .onSuccess { tournaments ->
                        _tournaments.value = tournaments
                        _error.value = null
                    }
                    .onFailure { exception ->
                        _error.value = exception.message ?: "Failed to load tournaments"
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
}