package pdm.application.ui.tournaments

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import pdm.application.api.RetrofitClient
import pdm.application.model.Tournament
import pdm.application.model.TournamentStatus
import pdm.application.repository.TournamentRepository
import pdm.application.util.GyroscopeSensorManager
import pdm.application.util.NetworkUtil
import pdm.application.util.SessionManager
import java.util.Date

class TournamentListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TournamentRepository
    val tournaments: LiveData<List<Tournament>>

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    private val _networkAvailable = MutableStateFlow(false)
    val networkAvailable: StateFlow<Boolean> = _networkAvailable.asStateFlow()

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _currentSortingCriteria = MutableStateFlow<GyroscopeSensorManager.SortingCriteria>(
        GyroscopeSensorManager.SortingCriteria.DATE
    )
    private val _isSortAscending = MutableStateFlow(true)

    init {
        val sessionManager = SessionManager(application)
        repository = TournamentRepository(application, sessionManager)
        tournaments = repository.tournaments.asLiveData()

        // Set up network callback
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isConnected.postValue(true)
            }

            override fun onLost(network: Network) {
                _isConnected.postValue(false)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        }

        // Set initial state
        _isConnected.value = isNetworkAvailable()
    }

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.let {
            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } ?: false
    }

    fun clearLocalData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    fun loadTournaments() {
        viewModelScope.launch {
            try {
                if (isNetworkAvailable()) {
                    repository.syncPendingChanges()
                }
                repository.refreshTournaments()
            } catch (e: Exception) {
                e.printStackTrace()
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
    val sortedTournaments = combine(
        tournaments.asFlow(),
        _currentSortingCriteria,
        _isSortAscending
    ) { tournaments, criteria, isAscending ->
        tournaments.sortedWith(getTournamentComparator(criteria, isAscending))
    }.asLiveData()

    fun updateSorting(criteria: GyroscopeSensorManager.SortingCriteria, isAscending: Boolean) {
        _currentSortingCriteria.value = criteria
        _isSortAscending.value = isAscending
    }

    private fun getTournamentComparator(
        criteria: GyroscopeSensorManager.SortingCriteria,
        isAscending: Boolean
    ): Comparator<Tournament> {
        val comparator = when (criteria) {
            GyroscopeSensorManager.SortingCriteria.DATE ->
                compareBy<Tournament> { it.startDate }
            GyroscopeSensorManager.SortingCriteria.PRIZE_POOL ->
                compareBy<Tournament> { it.prizePool }
            GyroscopeSensorManager.SortingCriteria.PARTICIPANTS ->
                compareBy<Tournament> { it.participantsCount }
            GyroscopeSensorManager.SortingCriteria.REGISTRATION_STATUS ->
                compareBy<Tournament> { it.isRegistrationOpen }
        }

        return if (isAscending) comparator else comparator.reversed()
    }
}