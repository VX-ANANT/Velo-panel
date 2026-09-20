package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.TournamentRepositoryImpl
import com.example.domain.model.Tournament
import com.example.domain.model.UserProfile
import com.example.domain.model.SupportTicket
import com.example.domain.model.ComplaintTicket
import com.example.domain.model.PayoutRequest
import com.example.domain.model.AdminRecord
import com.example.domain.model.BannedUserRecord
import com.example.domain.model.CheckInToken
import com.example.domain.model.AppAnnouncementBanner
import com.example.data.validation.UserRateLimiter
import com.example.ui.common.GlobalErrorManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DashboardState {
    object Loading : DashboardState()
    data class Success(
        val tournaments: List<Tournament> = emptyList(),
        val users: List<UserProfile> = emptyList(),
        val supportTickets: List<SupportTicket> = emptyList(),
        val payoutRequests: List<PayoutRequest> = emptyList(),
        val admins: List<AdminRecord> = emptyList(),
        val bannedUsers: Map<String, BannedUserRecord> = emptyMap(),
        val checkInTokens: List<CheckInToken> = emptyList(),
        val banners: List<AppAnnouncementBanner> = emptyList(),
        val globalAnnouncements: List<com.example.domain.model.GlobalAnnouncement> = emptyList(),
        val matchProofs: List<com.example.domain.model.MatchProofSubmission> = emptyList(),
        val notifications: List<com.example.domain.model.AppNotification> = emptyList(),
        val pendingRegistrationsCount: Int = 0,
        val totalRegistrationsCount: Int = 0,
        val payoutPool: Float = 0f,
        val currentUserEmail: String? = null
    ) : DashboardState() {
        val complaints: List<ComplaintTicket> get() = supportTickets
        val cashouts: List<PayoutRequest> get() = payoutRequests
        val userProfiles: List<UserProfile> get() = users
        val unreadNotificationCount: Int get() = notifications.count { !it.isRead }

        // Module 1 Metrics
        val totalRegisteredUsersCount: Int get() = users.size
        val totalActiveAdminsCount: Int get() = admins.count { it.active }
        val openSupportComplaintsCount: Int get() = supportTickets.count { it.status.equals("open", ignoreCase = true) || it.status.equals("PENDING", ignoreCase = true) }
        val pendingCashoutsCount: Int get() = payoutRequests.count { it.status.equals("pending", ignoreCase = true) }
        val pendingCashoutsSum: Double get() = payoutRequests.filter { it.status.equals("pending", ignoreCase = true) }.sumOf { it.vtAmount }
        val pendingMatchProofsCount: Int get() = matchProofs.count { it.status.equals("pending", ignoreCase = true) }

        // Top Leaderboard Players (ranked by total earnings, wins, kills)
        val leaderboardPlayers: List<UserProfile> get() = users.sortedWith(
            compareByDescending<UserProfile> { it.totalEarnings }
                .thenByDescending { it.wins }
                .thenByDescending { it.kills }
        )

        val currentUserAdminRecord: AdminRecord? get() {
            val email = currentUserEmail ?: return null
            return admins.firstOrNull { it.email.equals(email, ignoreCase = true) }
        }
    }
    data class Error(val message: String) : DashboardState()
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()

class TournamentDashboardViewModel(
    val repository: TournamentRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    private var overrideUserEmail: String? = null
    private val activeStreamJobs = mutableListOf<kotlinx.coroutines.Job>()

    fun setManualLoginEmail(email: String?) {
        overrideUserEmail = email
        if (email != null && email.isNotBlank()) {
            val cleanEmail = email.trim().lowercase()
            repository.recordAccountLocally(cleanEmail, cleanEmail.substringBefore("@"), "super_admin")
        }
        val curr = _uiState.value
        if (curr is DashboardState.Success) {
            _uiState.value = curr.copy(currentUserEmail = email)
        } else if (email != null) {
            _uiState.value = DashboardState.Success(currentUserEmail = email)
        }
        if (email != null) {
            startStreams(forceRestart = true)
        }
    }

    init {
        startStreams(forceRestart = false)
    }

    fun fetchData() {
        startStreams(forceRestart = true)
    }

    fun refreshRealtimeData() {
        startStreams(forceRestart = true)
        GlobalErrorManager.emitSuccess("Live Data Synced: Tournaments, Users & Finance Active!")
    }

    private fun startStreams(forceRestart: Boolean) {
        if (!forceRestart && activeStreamJobs.isNotEmpty()) return

        viewModelScope.launch {
            // Cancel any stale collectors
            activeStreamJobs.forEach { it.cancel() }
            activeStreamJobs.clear()

            val targetEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            repository.ensureAuthenticatedSession(targetEmail)

            val initialEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            val currState = _uiState.value
            if (currState !is DashboardState.Success) {
                _uiState.value = DashboardState.Success(currentUserEmail = initialEmail)
            } else {
                _uiState.value = currState.copy(currentUserEmail = initialEmail)
            }

            // Stream users
            activeStreamJobs.add(viewModelScope.launch {
                try {
                    repository.getLiveUsersStream().collect { liveUsers ->
                        val curr = _uiState.value as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                        _uiState.value = curr.copy(users = liveUsers)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })

            // Stream support tickets
            activeStreamJobs.add(viewModelScope.launch {
                try {
                    repository.getLiveSupportTicketsStream().collect { tickets ->
                        val curr = _uiState.value as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                        _uiState.value = curr.copy(supportTickets = tickets)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })

            // Stream payout requests
            activeStreamJobs.add(viewModelScope.launch {
                try {
                    repository.getLivePayoutRequestsStream().collect { payouts ->
                        val curr = _uiState.value as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                        _uiState.value = curr.copy(payoutRequests = payouts)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })

            // Stream admins
            activeStreamJobs.add(viewModelScope.launch {
                try {
                    repository.getLiveAdminsStream().collect { adminList ->
                        val curr = _uiState.value as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                        _uiState.value = curr.copy(admins = adminList)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })

            // Stream banned users
            activeStreamJobs.add(viewModelScope.launch {
                try {
                    repository.getLiveBannedUsersStream().collect { bannedMap ->
                        val curr = _uiState.value as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                        _uiState.value = curr.copy(bannedUsers = bannedMap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })

            // Stream tournaments
            activeStreamJobs.add(viewModelScope.launch {
                try {
                    repository.getLiveTournamentsStream().collect { realtimeTournaments ->
                        val curr = _uiState.value as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                        val payout = realtimeTournaments.sumOf { it.prizePool.toDouble() }.toFloat()
                        _uiState.value = curr.copy(
                            tournaments = realtimeTournaments,
                            totalRegistrationsCount = realtimeTournaments.sumOf { it.registeredPlayers },
                            payoutPool = payout
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })

            // Stream tokens & banners
            activeStreamJobs.add(viewModelScope.launch {
                try {
                    repository.getLiveTokensStream().collect { tokens ->
                        val curr = _uiState.value as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                        _uiState.value = curr.copy(checkInTokens = tokens)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })

            activeStreamJobs.add(viewModelScope.launch {
                try {
                    repository.getLiveBannersStream().collect { banners ->
                        val curr = _uiState.value as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                        _uiState.value = curr.copy(banners = banners)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })

            // Stream Global Announcements
            activeStreamJobs.add(viewModelScope.launch {
                try {
                    repository.getGlobalAnnouncementsStream().collect { annList ->
                        val curr = _uiState.value as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                        _uiState.value = curr.copy(globalAnnouncements = annList)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })

            // Stream Match Proof Submissions
            activeStreamJobs.add(viewModelScope.launch {
                try {
                    repository.getMatchProofsStream().collect { proofsList ->
                        val curr = _uiState.value as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                        _uiState.value = curr.copy(matchProofs = proofsList)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })

            // Stream Live Notifications & Campaigns
            activeStreamJobs.add(viewModelScope.launch {
                try {
                    repository.getNotificationsStream().collect { notifs ->
                        val curr = _uiState.value as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                        _uiState.value = curr.copy(notifications = notifs)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
        }
    }

    // Actions
    fun updateTicketStatus(ticket: SupportTicket, newStatus: String, adminNote: String = "", assignedTo: String = "") {
        viewModelScope.launch {
            val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            val updated = ticket.copy(
                status = newStatus,
                adminNote = if (adminNote.isNotBlank()) adminNote else ticket.adminNote,
                assignedTo = if (assignedTo.isNotBlank()) assignedTo else currAdminEmail,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateSupportTicket(updated)
        }
    }

    fun banPlayer(uid: String, email: String, reason: String) {
        viewModelScope.launch {
            val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            repository.banPlayer(uid, email, reason, currAdminEmail)
        }
    }

    fun unbanPlayer(uid: String) {
        viewModelScope.launch {
            repository.unbanPlayer(uid)
        }
    }

    fun adjustUserBalance(uid: String, newBalance: Double, reason: String) {
        viewModelScope.launch {
            val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            repository.adjustUserBalance(uid, newBalance, reason, currAdminEmail)
        }
    }

    fun approvePayout(request: PayoutRequest) {
        val rateLimitCheck = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.WALLET_TRANSACTION, request.id)
        if (!rateLimitCheck.isAllowed) {
            GlobalErrorManager.emitError(rateLimitCheck.reasonMessage)
            return
        }
        val curr = _uiState.value as? DashboardState.Success
        if (curr != null) {
            _uiState.value = curr.copy(
                payoutRequests = curr.payoutRequests.map {
                    if (it.id == request.id) it.copy(status = "approved") else it
                }
            )
        }
        viewModelScope.launch {
            try {
                val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
                repository.approvePayout(request, currAdminEmail)
                GlobalErrorManager.emitSuccess("Payout approved for ${request.username}")
            } catch (e: Exception) {
                e.printStackTrace()
                GlobalErrorManager.emitError("Failed to approve payout: ${e.message}")
            }
        }
    }

    fun rejectPayout(request: PayoutRequest, reason: String) {
        val rateLimitCheck = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.WALLET_TRANSACTION, request.id)
        if (!rateLimitCheck.isAllowed) {
            GlobalErrorManager.emitError(rateLimitCheck.reasonMessage)
            return
        }
        val curr = _uiState.value as? DashboardState.Success
        if (curr != null) {
            _uiState.value = curr.copy(
                payoutRequests = curr.payoutRequests.map {
                    if (it.id == request.id) it.copy(status = "rejected", rejectionReason = reason) else it
                }
            )
        }
        viewModelScope.launch {
            try {
                val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
                repository.rejectPayout(request, reason, currAdminEmail)
                GlobalErrorManager.emitSuccess("Payout rejected for ${request.username}")
            } catch (e: Exception) {
                e.printStackTrace()
                GlobalErrorManager.emitError("Failed to reject payout: ${e.message}")
            }
        }
    }

    fun createTournament(tournament: Tournament) {
        val curr = _uiState.value as? DashboardState.Success
        if (curr != null) {
            val updatedList = listOf(tournament) + curr.tournaments.filter { it.id != tournament.id }
            _uiState.value = curr.copy(
                tournaments = updatedList,
                payoutPool = updatedList.sumOf { it.prizePool.toDouble() }.toFloat()
            )
        }
        viewModelScope.launch {
            try {
                val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
                val currAdminUid = repository.auth.currentUser?.uid
                repository.createTournament(tournament, currAdminEmail, currAdminUid)
            } catch (e: Exception) {
                GlobalErrorManager.emitFirestoreError("Create Tournament", e)
            }
        }
    }

    fun updateTournament(tournament: Tournament) {
        val curr = _uiState.value as? DashboardState.Success
        if (curr != null) {
            val updatedList = curr.tournaments.map { if (it.id == tournament.id) tournament else it }
            _uiState.value = curr.copy(
                tournaments = updatedList,
                payoutPool = updatedList.sumOf { it.prizePool.toDouble() }.toFloat()
            )
        }
        viewModelScope.launch {
            try {
                val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
                val currAdminUid = repository.auth.currentUser?.uid
                repository.updateTournament(tournament, currAdminEmail, currAdminUid)
            } catch (e: Exception) {
                GlobalErrorManager.emitFirestoreError("Update Tournament", e)
            }
        }
    }

    fun cancelTournament(tournamentId: String, reason: String) {
        val curr = _uiState.value as? DashboardState.Success
        if (curr != null) {
            val updatedList = curr.tournaments.map {
                if (it.id == tournamentId) it.copy(status = "CANCELLED", cancellationReason = reason, cancelledAt = System.currentTimeMillis()) else it
            }
            _uiState.value = curr.copy(tournaments = updatedList)
        }
        viewModelScope.launch {
            try {
                val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
                val currAdminUid = repository.auth.currentUser?.uid
                repository.cancelTournament(tournamentId, reason, currAdminEmail, currAdminUid)
            } catch (e: Exception) {
                GlobalErrorManager.emitFirestoreError("Cancel Tournament", e)
            }
        }
    }

    fun deleteTournament(tournamentId: String) {
        val curr = _uiState.value as? DashboardState.Success
        if (curr != null) {
            val updatedList = curr.tournaments.filter { it.id != tournamentId }
            _uiState.value = curr.copy(
                tournaments = updatedList,
                payoutPool = updatedList.sumOf { it.prizePool.toDouble() }.toFloat()
            )
        }
        viewModelScope.launch {
            try {
                val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
                val currAdminUid = repository.auth.currentUser?.uid
                repository.deleteTournament(tournamentId, currAdminEmail, currAdminUid)
            } catch (e: Exception) {
                GlobalErrorManager.emitFirestoreError("Delete Tournament", e)
            }
        }
    }

    fun getTournamentLiveStream(tournamentId: String): Flow<Tournament?> {
        return repository.getTournamentLiveStream(tournamentId)
    }

    fun updateRoomCredentials(tournamentId: String, roomId: String, roomPassword: String) {
        viewModelScope.launch {
            repository.updateRoomCredentials(tournamentId, roomId, roomPassword)
        }
    }

    fun grantAdminAccess(uid: String, email: String, name: String, role: String) {
        val curr = _uiState.value as? DashboardState.Success
        if (curr != null) {
            val newRecord = AdminRecord(uid = uid, email = email, name = name, role = role, active = true)
            _uiState.value = curr.copy(admins = listOf(newRecord) + curr.admins.filter { it.uid != uid && it.email != email })
        }
        viewModelScope.launch {
            try {
                val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
                repository.grantAdminAccess(uid, email, name, role, currAdminEmail)
                GlobalErrorManager.emitSuccess("Admin access granted to $name")
            } catch (e: Exception) {
                e.printStackTrace()
                GlobalErrorManager.emitError("Failed to grant admin: ${e.message}")
            }
        }
    }

    fun revokeAdminAccess(adminUid: String) {
        val curr = _uiState.value as? DashboardState.Success
        if (curr != null) {
            _uiState.value = curr.copy(admins = curr.admins.map { if (it.uid == adminUid) it.copy(active = false) else it })
        }
        viewModelScope.launch {
            try {
                repository.revokeAdminAccess(adminUid)
                GlobalErrorManager.emitSuccess("Admin access revoked")
            } catch (e: Exception) {
                e.printStackTrace()
                GlobalErrorManager.emitError("Failed to revoke admin: ${e.message}")
            }
        }
    }

    fun updateAdminRecord(admin: AdminRecord) {
        val curr = _uiState.value as? DashboardState.Success
        if (curr != null) {
            _uiState.value = curr.copy(admins = curr.admins.map { if (it.uid == admin.uid) admin else it })
        }
        viewModelScope.launch {
            try {
                repository.updateAdminRecord(admin)
                GlobalErrorManager.emitSuccess("Admin record updated")
            } catch (e: Exception) {
                e.printStackTrace()
                GlobalErrorManager.emitError("Failed to update admin: ${e.message}")
            }
        }
    }

    fun deleteAdminRecord(adminUid: String) {
        val curr = _uiState.value as? DashboardState.Success
        if (curr != null) {
            _uiState.value = curr.copy(admins = curr.admins.filter { it.uid != adminUid })
        }
        viewModelScope.launch {
            try {
                repository.deleteAdminRecord(adminUid)
                GlobalErrorManager.emitSuccess("Admin removed")
            } catch (e: Exception) {
                e.printStackTrace()
                GlobalErrorManager.emitError("Failed to delete admin: ${e.message}")
            }
        }
    }

    fun toggleUserBan(user: UserProfile, explicitBanned: Boolean? = null) {
        val newBanned = explicitBanned ?: !user.isBanned
        val curr = _uiState.value as? DashboardState.Success
        if (curr != null) {
            _uiState.value = curr.copy(
                users = curr.users.map { if (it.id == user.id) it.copy(isBanned = newBanned) else it }
            )
        }
        viewModelScope.launch {
            try {
                val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
                if (newBanned) {
                    repository.banPlayer(
                        uid = user.id,
                        email = user.email,
                        reason = "Banned by Admin Panel",
                        bannedBy = currAdminEmail,
                        gameId = user.gameId
                    )
                    GlobalErrorManager.emitSuccess("Player ${user.username} (${user.id}) banned successfully")
                } else {
                    repository.unbanPlayer(
                        uid = user.id,
                        email = user.email,
                        gameId = user.gameId
                    )
                    GlobalErrorManager.emitSuccess("Player ${user.username} (${user.id}) unbanned successfully")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                GlobalErrorManager.emitError("Failed to toggle ban: ${e.message}")
            }
        }
    }

    fun updateUserProfile(user: UserProfile) {
        if (user.email.isNotBlank()) {
            repository.recordAccountLocally(user.email, user.username, user.role, user.id)
        }
        val curr = _uiState.value as? DashboardState.Success
        if (curr != null) {
            val existingIndex = curr.users.indexOfFirst { it.id == user.id || (user.email.isNotBlank() && it.email.equals(user.email, ignoreCase = true)) }
            val updatedList = if (existingIndex >= 0) {
                curr.users.toMutableList().apply { set(existingIndex, user) }
            } else {
                curr.users + user
            }
            _uiState.value = curr.copy(users = updatedList)
        }
        viewModelScope.launch {
            try {
                repository.updateUserProfile(user)
                GlobalErrorManager.emitSuccess("User profile updated for ${user.username}")
            } catch (e: Exception) {
                e.printStackTrace()
                GlobalErrorManager.emitError("Failed to update user profile: ${e.message}")
            }
        }
    }

    fun loadUserData(onFinished: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                repository.loadUserData { liveUsers ->
                    val curr = _uiState.value as? DashboardState.Success
                    if (curr != null && liveUsers.isNotEmpty()) {
                        val merged = (curr.users + liveUsers).distinctBy { it.id }
                        _uiState.value = curr.copy(users = merged)
                    }
                    onFinished?.invoke()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onFinished?.invoke()
            }
        }
    }

    fun addFundsToUser(user: UserProfile, amount: Double) {
        val rateLimitCheck = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.WALLET_TRANSACTION, user.id)
        if (!rateLimitCheck.isAllowed) {
            GlobalErrorManager.emitError(rateLimitCheck.reasonMessage)
            return
        }
        val newBalance = user.funds + amount
        val curr = _uiState.value as? DashboardState.Success
        if (curr != null) {
            _uiState.value = curr.copy(
                users = curr.users.map { if (it.id == user.id) it.copy(funds = newBalance) else it }
            )
        }
        viewModelScope.launch {
            try {
                val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
                repository.adjustUserBalance(user.id, newBalance, "Fund adjustment by admin", currAdminEmail)
                GlobalErrorManager.emitSuccess("Wallet updated: ₹$amount added to ${user.username}")
            } catch (e: Exception) {
                e.printStackTrace()
                GlobalErrorManager.emitError("Failed to adjust wallet: ${e.message}")
            }
        }
    }

    fun deleteUser(user: UserProfile) {
        val curr = _uiState.value as? DashboardState.Success
        if (curr != null) {
            _uiState.value = curr.copy(users = curr.users.filter { it.id != user.id })
        }
        viewModelScope.launch {
            try {
                repository.deleteUserProfile(user.id)
                GlobalErrorManager.emitSuccess("User ${user.username} deleted permanently")
            } catch (e: Exception) {
                e.printStackTrace()
                GlobalErrorManager.emitError("Failed to delete user: ${e.message}")
            }
        }
    }

    fun saveComplaint(ticket: SupportTicket) {
        val curr = _uiState.value as? DashboardState.Success
        if (curr != null) {
            _uiState.value = curr.copy(
                supportTickets = curr.supportTickets.map { if (it.id == ticket.id) ticket else it }
            )
        }
        viewModelScope.launch {
            try {
                repository.updateSupportTicket(ticket)
                GlobalErrorManager.emitSuccess("Ticket #${ticket.id} updated to ${ticket.status}")
            } catch (e: Exception) {
                GlobalErrorManager.emitError("Failed to update ticket: ${e.message}")
            }
        }
    }

    fun deleteComplaint(ticketId: String) {
        val curr = _uiState.value as? DashboardState.Success
        if (curr != null) {
            _uiState.value = curr.copy(
                supportTickets = curr.supportTickets.filter { it.id != ticketId }
            )
        }
        viewModelScope.launch {
            try {
                repository.deleteSupportTicket(ticketId)
                GlobalErrorManager.emitSuccess("Ticket #$ticketId dissolved/deleted successfully.")
            } catch (e: Exception) {
                GlobalErrorManager.emitError("Failed to delete ticket: ${e.message}")
            }
        }
    }

    fun saveToken(token: CheckInToken) {
        viewModelScope.launch {
            repository.saveToken(token)
        }
    }

    fun saveBanner(banner: AppAnnouncementBanner) {
        val rateLimitCheck = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.BROADCAST_ANNOUNCEMENT, banner.id)
        if (!rateLimitCheck.isAllowed) {
            GlobalErrorManager.emitError(rateLimitCheck.reasonMessage)
            return
        }
        viewModelScope.launch {
            repository.saveBanner(banner)
        }
    }

    fun publishRoomCredentialsWith5MinCheck(
        tournamentId: String,
        roomId: String,
        roomPass: String,
        startsAtTimestamp: Long? = null,
        bypassCheck: Boolean = false,
        onResult: (Boolean) -> Unit = {}
    ) {
        val rateLimitCheck = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.ROOM_CREDENTIALS_PUBLISH, tournamentId)
        if (!rateLimitCheck.isAllowed) {
            GlobalErrorManager.emitError(rateLimitCheck.reasonMessage)
            onResult(false)
            return
        }
        viewModelScope.launch {
            val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            val currAdminUid = repository.auth.currentUser?.uid
            val success = repository.publishRoomCredentialsWithTimeCheck(
                tournamentId = tournamentId,
                roomId = roomId,
                roomPass = roomPass,
                startsAtTimestamp = startsAtTimestamp,
                bypassCheck = bypassCheck,
                adminEmail = currAdminEmail,
                adminUid = currAdminUid
            )
            onResult(success)
        }
    }

    fun submitMatchProof(proof: com.example.domain.model.MatchProofSubmission) {
        viewModelScope.launch {
            repository.submitMatchProof(proof)
        }
    }

    fun approveMatchProof(proofId: String, prizeAmount: Double, killsCount: Int) {
        viewModelScope.launch {
            val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            repository.approveMatchProof(proofId, currAdminEmail, prizeAmount, killsCount)
        }
    }

    fun rejectMatchProof(proofId: String, reason: String) {
        viewModelScope.launch {
            val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            repository.rejectMatchProof(proofId, reason, currAdminEmail)
        }
    }

    fun publishGlobalAnnouncement(announcement: com.example.domain.model.GlobalAnnouncement) {
        viewModelScope.launch {
            repository.publishGlobalAnnouncement(announcement)
        }
    }

    fun deleteGlobalAnnouncement(id: String) {
        viewModelScope.launch {
            repository.deleteGlobalAnnouncement(id)
        }
    }

    fun publishAnnouncement(announcement: com.example.domain.model.GlobalAnnouncement) {
        publishGlobalAnnouncement(announcement)
    }

    fun deleteAnnouncement(id: String) {
        deleteGlobalAnnouncement(id)
    }

    fun exportAllTournamentsJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportAllTournamentsJson()
            onResult(json)
        }
    }

    fun importTournamentsFromJson(jsonStr: String, onResult: (Pair<Int, String>) -> Unit) {
        viewModelScope.launch {
            val result = repository.importTournamentsFromJson(jsonStr)
            onResult(result)
        }
    }

    fun syncAllTournamentsToCloud(onResult: (Pair<Int, String>) -> Unit) {
        viewModelScope.launch {
            val result = repository.syncAllTournamentsToCloud()
            onResult(result)
        }
    }

    fun joinTournament(
        tournamentId: String,
        player: com.example.domain.model.PlayerRegistration,
        entryFee: Float,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val userIdentifier = player.userId.ifBlank { player.playerId.ifBlank { tournamentId } }
        val rateLimitCheck = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.TOURNAMENT_JOIN, userIdentifier)
        if (!rateLimitCheck.isAllowed) {
            GlobalErrorManager.emitError(rateLimitCheck.reasonMessage)
            onComplete(false, rateLimitCheck.reasonMessage)
            return
        }
        viewModelScope.launch {
            try {
                val success = repository.joinTournament(tournamentId, player, entryFee)
                if (success) {
                    onComplete(true, "Successfully registered and slot locked!")
                } else {
                    onComplete(false, "Registration failed: Insufficient 2D wallet balance or slots full.")
                }
            } catch (e: Exception) {
                GlobalErrorManager.emitFirestoreError("Join Tournament", e)
                onComplete(false, e.localizedMessage ?: "Failed to join tournament")
            }
        }
    }

    fun markNotificationAsRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }

    fun publishCampaign(
        title: String,
        message: String,
        priority: String = "HIGH",
        tournamentId: String? = null
    ) {
        viewModelScope.launch {
            repository.publishCampaignNotification(title, message, priority, tournamentId)
        }
    }

    fun testPushNotification(context: android.content.Context) {
        viewModelScope.launch {
            val liveTourney = (_uiState.value as? DashboardState.Success)?.tournaments?.firstOrNull()
            if (liveTourney != null) {
                repository.dispatchAutoTournamentCampaign(liveTourney)
            } else {
                repository.publishCampaignNotification(
                    title = "Velorix Push & Campaign Test",
                    message = "Notification Engine is active! Tournament campaigns & system notifications are operating in real-time.",
                    priority = "URGENT"
                )
            }
        }
    }

    fun purgeAllDemoData(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            val result = repository.purgeAllDemoAndMockData(adminEmail)
            if (result.first) {
                GlobalErrorManager.emitSuccess("Mock & Demo data purged successfully.")
                refreshRealtimeData()
            } else {
                GlobalErrorManager.emitError("Purge failed: ${result.second}")
            }
            onResult(result.first, result.second)
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

