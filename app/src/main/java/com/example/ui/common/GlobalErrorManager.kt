package com.example.ui.common

import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GlobalErrorUiState(
    val message: String,
    val details: String? = null,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val isError: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

object GlobalErrorManager {
    private val _errorState = MutableStateFlow<GlobalErrorUiState?>(null)
    val errorState: StateFlow<GlobalErrorUiState?> = _errorState.asStateFlow()

    private fun isCancellation(throwable: Throwable?): Boolean {
        if (throwable == null) return false
        if (throwable is CancellationException) return true
        val msg = throwable.message?.lowercase() ?: ""
        val name = throwable.javaClass.simpleName.lowercase()
        return name.contains("cancel") || msg.contains("job was cancelled") || msg.contains("job was canceled") || msg.contains("coroutine was cancelled")
    }

    fun emitError(
        message: String,
        throwable: Throwable? = null,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        if (isCancellation(throwable)) return
        val lower = message.lowercase()
        if (lower.contains("job was cancelled") || lower.contains("job was canceled")) return

        // Gracefully format or bypass unprovisioned Firestore notice since RTDB is live and operational
        val cleanMessage = if (lower.contains("the database (default) does not exist") || lower.contains("datastore/setup")) {
            "Realtime Database is active. (Cloud Firestore default database is not yet created in Firebase Console)."
        } else {
            message
        }

        val rawDetails = throwable?.localizedMessage ?: throwable?.message
        val details = if (!rawDetails.isNullOrBlank() && !cleanMessage.contains(rawDetails) && rawDetails != cleanMessage) {
            if (rawDetails.contains("datastore/setup") || rawDetails.contains("does not exist")) null else rawDetails
        } else null

        _errorState.value = GlobalErrorUiState(
            message = cleanMessage,
            details = details,
            actionLabel = actionLabel,
            onAction = onAction,
            isError = !cleanMessage.contains("Realtime Database is active"),
            timestamp = System.currentTimeMillis()
        )
    }

    fun emitFirestoreError(
        operationName: String,
        throwable: Throwable,
        actionLabel: String? = "Retry",
        onAction: (() -> Unit)? = null
    ) {
        if (isCancellation(throwable)) return

        val userFriendlyMessage = when (throwable) {
            is FirebaseFirestoreException -> when (throwable.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Permission denied: Unable to access $operationName on Firestore."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Network error: Firestore service is currently unavailable while fetching $operationName."
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                    "Request timed out while loading $operationName from Firestore."
                FirebaseFirestoreException.Code.NOT_FOUND ->
                    "Requested $operationName record was not found on Firestore."
                FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    "Authentication required: Please log in to fetch $operationName."
                FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                    "Firestore quota or limit exceeded while fetching $operationName."
                else ->
                    "Firestore sync failed for $operationName (${throwable.code.name})."
            }
            is FirebaseException ->
                "Firebase service issue while fetching $operationName: ${throwable.localizedMessage ?: "Connection error"}"
            else -> {
                val msg = throwable.localizedMessage ?: throwable.message ?: ""
                if (msg.contains("Job was cancelled", ignoreCase = true) || msg.contains("cancelled", ignoreCase = true)) {
                    return
                }
                "Unable to fetch $operationName: $msg"
            }
        }

        emitError(
            message = userFriendlyMessage,
            throwable = throwable,
            actionLabel = actionLabel,
            onAction = onAction
        )
    }

    fun emitSuccess(message: String) {
        _errorState.value = GlobalErrorUiState(
            message = message,
            isError = false,
            timestamp = System.currentTimeMillis()
        )
    }

    fun dismissError() {
        _errorState.value = null
    }
}
