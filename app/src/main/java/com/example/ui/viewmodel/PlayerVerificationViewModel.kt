package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.TournamentRepositoryImpl
import com.example.domain.model.PlayerRegistration
import com.example.ui.common.GlobalErrorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class VerificationState {
    object Loading : VerificationState()
    data class Success(val registrations: List<PlayerRegistration>) : VerificationState()
    data class Error(val message: String) : VerificationState()
}

class PlayerVerificationViewModel(
    private val repository: TournamentRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow<VerificationState>(VerificationState.Loading)
    val uiState: StateFlow<VerificationState> = _uiState.asStateFlow()

    init {
        fetchPendingRegistrations()
    }

    fun fetchPendingRegistrations() {
        viewModelScope.launch {
            _uiState.value = VerificationState.Loading
            try {
                val pending = repository.getPendingRegistrations()
                _uiState.value = VerificationState.Success(pending)
            } catch (e: Exception) {
                GlobalErrorManager.emitFirestoreError("Registrations", e, actionLabel = "Retry") {
                    fetchPendingRegistrations()
                }
                _uiState.value = VerificationState.Error(e.message ?: "Failed to load registrations")
            }
        }
    }

    fun verifyRegistration(registrationId: String, approve: Boolean) {
        viewModelScope.launch {
            try {
                val status = if (approve) "approved" else "rejected"
                repository.updateRegistrationStatus(registrationId, status)
                GlobalErrorManager.emitSuccess("Registration ${if (approve) "approved" else "rejected"} successfully")
                fetchPendingRegistrations()
            } catch (e: Exception) {
                GlobalErrorManager.emitFirestoreError("Registration Verification", e)
                _uiState.value = VerificationState.Error(e.message ?: "Failed to verify registration")
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: TournamentRepositoryImpl
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(PlayerVerificationViewModel::class.java)) {
                    return PlayerVerificationViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
