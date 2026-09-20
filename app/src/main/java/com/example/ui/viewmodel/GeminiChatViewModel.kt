package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.GeminiRepository
import com.example.data.repository.GeminiRepositoryImpl
import com.example.data.validation.UserRateLimiter
import com.example.domain.model.ComplaintTicket
import com.example.domain.model.Content
import com.example.domain.model.Part
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiChatMessage(
    val id: String = System.currentTimeMillis().toString() + "_" + (0..999).random(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val isError: Boolean = false
)

data class GeminiChatUiState(
    val messages: List<UiChatMessage> = listOf(
        UiChatMessage(
            text = "Hello! I am Velorix AI Assistant. I can help with tournament schedules, bracket generation, rules clarification, and player support. How can I assist you today?",
            isUser = false
        )
    ),
    val isLoading: Boolean = false,
    val selectedModel: String = "gemini-3.1-pro-preview",
    val error: String? = null
)

class GeminiChatViewModel(
    private val geminiRepository: GeminiRepository = GeminiRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeminiChatUiState())
    val uiState: StateFlow<GeminiChatUiState> = _uiState.asStateFlow()

    val cooldownSeconds: StateFlow<Long> = UserRateLimiter.observeCooldownSeconds(UserRateLimiter.ActionType.GEMINI_CHAT_MESSAGE)

    private val conversationHistory = mutableListOf<Content>()

    fun sendMessage(userText: String, logSupportTicket: Boolean = false) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _uiState.value.isLoading) return

        val rateCheck = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.GEMINI_CHAT_MESSAGE)
        if (!rateCheck.isAllowed) {
            val userMsg = UiChatMessage(text = trimmed, isUser = true)
            val rateLimitMsg = UiChatMessage(
                text = "⏳ Rate Limit: ${rateCheck.reasonMessage}",
                isUser = false,
                isError = true
            )
            _uiState.update { current ->
                current.copy(
                    messages = current.messages + userMsg + rateLimitMsg,
                    isLoading = false,
                    error = rateCheck.reasonMessage
                )
            }
            return
        }

        val userMessage = UiChatMessage(text = trimmed, isUser = true)
        val loadingMessage = UiChatMessage(text = "", isUser = false, isLoading = true)

        _uiState.update { current ->
            current.copy(
                messages = current.messages + userMessage + loadingMessage,
                isLoading = true,
                error = null
            )
        }

        conversationHistory.add(Content(parts = listOf(Part(text = trimmed)), role = "user"))

        if (logSupportTicket) {
            logDisputeTicketToFirebase(trimmed)
        }

        viewModelScope.launch {
            val result = geminiRepository.sendChatMessage(
                history = conversationHistory,
                model = _uiState.value.selectedModel,
                enableThinking = true
            )

            result.fold(
                onSuccess = { replyText ->
                    conversationHistory.add(Content(parts = listOf(Part(text = replyText)), role = "model"))
                    _uiState.update { current ->
                        val updatedList = current.messages.dropLast(1) + UiChatMessage(text = replyText, isUser = false)
                        current.copy(messages = updatedList, isLoading = false)
                    }
                },
                onFailure = { err ->
                    val rawMsg = err.localizedMessage ?: "Unknown error"
                    val errorText = if (rawMsg.contains("rate", ignoreCase = true) || rawMsg.contains("quota", ignoreCase = true) || rawMsg.contains("429")) {
                        "Gemini AI rate limit reached. Please wait a moment or try another model."
                    } else {
                        "Unable to connect to Gemini AI: $rawMsg"
                    }
                    _uiState.update { current ->
                        val updatedList = current.messages.dropLast(1) + UiChatMessage(
                            text = errorText,
                            isUser = false,
                            isError = true
                        )
                        current.copy(messages = updatedList, isLoading = false, error = err.message)
                    }
                }
            )
        }
    }

    fun clearChat() {
        conversationHistory.clear()
        _uiState.value = GeminiChatUiState()
    }

    fun setModel(model: String) {
        _uiState.update { it.copy(selectedModel = model) }
    }

    private fun logDisputeTicketToFirebase(query: String) {
        try {
            val currUser = FirebaseAuth.getInstance().currentUser
            val userEmail = currUser?.email?.ifBlank { null } ?: "support_user@velorix.com"
            val rateCheck = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.SUPPORT_TICKET_SUBMISSION, userEmail)
            if (!rateCheck.isAllowed) return

            val ticketId = "TK-AI-${System.currentTimeMillis().toString().takeLast(6)}"
            val username = currUser?.displayName?.ifBlank { null } ?: userEmail.substringBefore("@")
            val ticket = ComplaintTicket(
                id = ticketId,
                userEmail = userEmail,
                username = username,
                gameId = "IGN_${username.take(6).uppercase()}",
                tournamentTitle = "Velorix AI Help Desk",
                issueCategory = "AI Chat Inquiry",
                description = query,
                status = "open",
                updatedAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                isHighPriority = false
            )
            val db = try {
                FirebaseDatabase.getInstance("https://velorix-tournaments-default-rtdb.asia-southeast1.firebasedatabase.app").reference
            } catch (_: Exception) {
                FirebaseDatabase.getInstance().reference
            }
            val fs = FirebaseFirestore.getInstance()
            db.child("complaints").child(ticketId).setValue(ticket)
            db.child("chatbot_complaints").child(ticketId).setValue(ticket)
            fs.collection("complaints").document(ticketId).set(ticket)
        } catch (_: Exception) {}
    }

    companion object {
        fun provideFactory(
            repository: GeminiRepository = GeminiRepositoryImpl()
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GeminiChatViewModel(repository) as T
            }
        }
    }
}
