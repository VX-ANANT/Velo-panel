package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.TournamentRepositoryImpl
import com.example.domain.model.Tournament
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DashboardState {
    object Loading : DashboardState()
    data class Success(val tournaments: List<Tournament>) : DashboardState()
    data class Error(val message: String) : DashboardState()
}

class TournamentDashboardViewModel(
    private val repository: TournamentRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    init {
        fetchTournaments()
    }

    fun fetchTournaments() {
        viewModelScope.launch {
            _uiState.value = DashboardState.Loading
            try {
                val tournaments = repository.getAvailableTournaments()
                _uiState.value = DashboardState.Success(tournaments)
            } catch (e: Exception) {
                _uiState.value = DashboardState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun updateTournament(tournament: Tournament) {
        viewModelScope.launch {
            try {
                repository.updateTournament(tournament)
                fetchTournaments() // Refresh list
            } catch (e: Exception) {
                _uiState.value = DashboardState.Error(e.message ?: "Failed to update tournament")
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: TournamentRepositoryImpl
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TournamentDashboardViewModel::class.java)) {
                    return TournamentDashboardViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
