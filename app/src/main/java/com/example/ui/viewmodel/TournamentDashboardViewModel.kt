package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.TournamentRepositoryImpl
import com.example.domain.model.Tournament
import com.example.domain.model.UserProfile
import com.example.domain.model.SupportTicket
import com.example.domain.model.TicketMessage
import com.example.domain.model.ComplaintTicket
import com.example.domain.model.PayoutRequest
import com.example.domain.model.AdminRecord
import com.example.domain.model.BannedUserRecord
import com.example.domain.model.CheckInToken
import com.example.domain.model.AppAnnouncementBanner
import com.example.data.validation.UserRateLimiter
import com.example.ui.common.GlobalErrorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        val currentUserEmail: String? = null,
        val isSyncing: Boolean = false,
        val lastSyncTimestamp: Long = System.currentTimeMillis(),
        // Precalculated, cached metrics & leaderboard to eliminate Main thread sorting and frame drops
        val leaderboardPlayers: List<UserProfile> = emptyList(),
        val totalRegisteredUsersCount: Int = 0,
        val totalActiveAdminsCount: Int = 0,
        val openSupportComplaintsCount: Int = 0,
        val pendingCashoutsCount: Int = 0,
        val pendingCashoutsSum: Double = 0.0,
        val pendingMatchProofsCount: Int = 0,
        val unreadNotificationCount: Int = 0
    ) : DashboardState() {
        val complaints: List<ComplaintTicket> get() = supportTickets
        val cashouts: List<PayoutRequest> get() = payoutRequests
        val userProfiles: List<UserProfile> get() = users

        val currentUserAdminRecord: AdminRecord? get() {
            val email = currentUserEmail ?: return null
            return admins.firstOrNull { it.email.equals(email, ignoreCase = true) }
        }
    }
    data class Error(val message: String) : DashboardState()
}

/**
 * Optimizes state transitions by computing CPU-heavy derived metrics on Dispatchers.Default
 * and reusing existing calculations when unaffected data has not changed.
 */
fun DashboardState.Success.withDerivedMetrics(
    newUsers: List<UserProfile>? = null,
    users: List<UserProfile>? = null,
    newAdmins: List<AdminRecord>? = null,
    admins: List<AdminRecord>? = null,
    newSupportTickets: List<SupportTicket>? = null,
    supportTickets: List<SupportTicket>? = null,
    newPayoutRequests: List<PayoutRequest>? = null,
    payoutRequests: List<PayoutRequest>? = null,
    newMatchProofs: List<com.example.domain.model.MatchProofSubmission>? = null,
    newNotifications: List<com.example.domain.model.AppNotification>? = null,
    newTournaments: List<Tournament>? = null,
    tournaments: List<Tournament>? = null,
    newSyncing: Boolean? = null,
    newBannedUsers: Map<String, BannedUserRecord>? = null,
    newTokens: List<CheckInToken>? = null,
    newBanners: List<AppAnnouncementBanner>? = null,
    newGlobalAnnouncements: List<com.example.domain.model.GlobalAnnouncement>? = null
): DashboardState.Success {
    val effectiveUsers = newUsers ?: users ?: this.users
    val effectiveAdmins = newAdmins ?: admins ?: this.admins
    val effectiveTickets = newSupportTickets ?: supportTickets ?: this.supportTickets
    val effectivePayouts = newPayoutRequests ?: payoutRequests ?: this.payoutRequests
    val effectiveProofs = newMatchProofs ?: this.matchProofs
    val effectiveNotifs = newNotifications ?: this.notifications
    val effectiveTournaments = newTournaments ?: tournaments ?: this.tournaments

    val sortedLeaderboard = if (newUsers != null || leaderboardPlayers.isEmpty()) {
        effectiveUsers.sortedWith(
            compareByDescending<UserProfile> { it.totalEarnings }
                .thenByDescending { it.wins }
                .thenByDescending { it.kills }
        )
    } else {
        leaderboardPlayers
    }

    val activeAdmins = if (newAdmins != null) effectiveAdmins.count { it.active } else totalActiveAdminsCount
    val openComplaints = if (newSupportTickets != null) {
        effectiveTickets.count { it.status.equals("open", ignoreCase = true) || it.status.equals("PENDING", ignoreCase = true) }
    } else {
        openSupportComplaintsCount
    }
    val pCashoutsCount = if (newPayoutRequests != null) {
        effectivePayouts.count { it.status.equals("pending", ignoreCase = true) }
    } else {
        pendingCashoutsCount
    }
    val pCashoutsSum = if (newPayoutRequests != null) {
        effectivePayouts.filter { it.status.equals("pending", ignoreCase = true) }.sumOf { it.vtAmount }
    } else {
        pendingCashoutsSum
    }
    val pProofs = if (newMatchProofs != null) {
        effectiveProofs.count { it.status.equals("pending", ignoreCase = true) }
    } else {
        pendingMatchProofsCount
    }
    val unreadCount = if (newNotifications != null) {
        effectiveNotifs.count { !it.isRead }
    } else {
        unreadNotificationCount
    }
    val totalReg = if (newTournaments != null) {
        effectiveTournaments.sumOf { it.registeredPlayers }
    } else {
        totalRegistrationsCount
    }
    val payout = if (newTournaments != null) {
        effectiveTournaments.sumOf { it.prizePool.toDouble() }.toFloat()
    } else {
        payoutPool
    }

    return this.copy(
        tournaments = effectiveTournaments,
        users = effectiveUsers,
        admins = effectiveAdmins,
        supportTickets = effectiveTickets,
        payoutRequests = effectivePayouts,
        matchProofs = effectiveProofs,
        notifications = effectiveNotifs,
        bannedUsers = newBannedUsers ?: this.bannedUsers,
        checkInTokens = newTokens ?: this.checkInTokens,
        banners = newBanners ?: this.banners,
        globalAnnouncements = newGlobalAnnouncements ?: this.globalAnnouncements,
        isSyncing = newSyncing ?: this.isSyncing,
        leaderboardPlayers = sortedLeaderboard,
        totalRegisteredUsersCount = effectiveUsers.size,
        totalActiveAdminsCount = activeAdmins,
        openSupportComplaintsCount = openComplaints,
        pendingCashoutsCount = pCashoutsCount,
        pendingCashoutsSum = pCashoutsSum,
        pendingMatchProofsCount = pProofs,
        unreadNotificationCount = unreadCount,
        totalRegistrationsCount = totalReg,
        payoutPool = payout
    )
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()

class TournamentDashboardViewModel(
    val repository: TournamentRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    private var overrideUserEmail: String? = null
    private val activeStreamJobs = mutableListOf<kotlinx.coroutines.Job>()

    val userRoleManager: UserRoleManager = UserRoleManager(repository)

    // In-memory locks to prevent real-time stream snapshots from causing 'revert-and-reactivate' UI glitches
    private val lockedAdminStates = java.util.concurrent.ConcurrentHashMap<String, Pair<AdminRecord, Long>>()
    private val lockedDeletedAdminUids = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val lockedUserStates = java.util.concurrent.ConcurrentHashMap<String, Pair<UserProfile, Long>>()
    private val lockedDeletedUserIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

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
        extractAndSyncRealDataFromBackend()
    }

    fun refreshRealtimeData() {
        extractAndSyncRealDataFromBackend()
    }

    fun extractAndSyncRealDataFromBackend(onComplete: ((com.example.domain.model.BackendExtractionResult) -> Unit)? = null) {
        // 1. Clear all in-memory lock states and stale caches
        lockedUserStates.clear()
        lockedAdminStates.clear()
        lockedDeletedUserIds.clear()
        lockedDeletedAdminUids.clear()
        repository.locallyDeletedUserIds.clear()
        repository.locallyDeletedTournamentIds.clear()

        // 2. Set syncing state in UI atomically
        _uiState.update { curr ->
            (curr as? DashboardState.Success)?.copy(isSyncing = true) ?: curr
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                GlobalErrorManager.emitSuccess("Extracting real data from Firebase RTDB & Cloud Firestore...")
                val result = repository.extractRealDataFromBackend(forceServer = true)

                val initialEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"

                withContext(Dispatchers.Default) {
                    _uiState.update { curr ->
                        val base = curr as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                        base.withDerivedMetrics(
                            newTournaments = result.tournaments,
                            newUsers = result.users,
                            newSyncing = false
                        ).copy(lastSyncTimestamp = System.currentTimeMillis())
                    }
                }

                // Restart live stream listeners with debouncing and distinct filtering
                startStreams(forceRestart = true)

                val msg = if (result.success) {
                    "Live extraction complete: ${result.tournamentsCount} Tournaments & ${result.usersCount} Players synced from Firebase!"
                } else {
                    result.message
                }
                GlobalErrorManager.emitSuccess(msg)
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(result)
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    e.printStackTrace()
                    _uiState.update { curr ->
                        (curr as? DashboardState.Success)?.copy(isSyncing = false) ?: curr
                    }
                    GlobalErrorManager.emitError("Backend sync error: ${e.message}")
                    startStreams(forceRestart = true)
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke(com.example.domain.model.BackendExtractionResult(success = false, message = e.message ?: "Sync error"))
                    }
                }
            }
        }
    }

    private fun startStreams(forceRestart: Boolean) {
        if (!forceRestart && activeStreamJobs.isNotEmpty()) return

        synchronized(activeStreamJobs) {
            activeStreamJobs.forEach { it.cancel() }
            activeStreamJobs.clear()
        }

        val initialEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
        _uiState.update { curr ->
            if (curr !is DashboardState.Success) {
                DashboardState.Success(currentUserEmail = initialEmail)
            } else {
                curr.copy(currentUserEmail = initialEmail)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val targetEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            repository.ensureAuthenticatedSession(targetEmail)
        }

        // 1. Stream users (CPU heavy reconciliation on Dispatchers.Default with debouncing & distinct check)
        val userJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                repository.getLiveUsersStream()
                    .distinctUntilChanged()
                    .debounce(100L)
                    .flowOn(Dispatchers.Default)
                    .collect { liveUsers ->
                        val now = System.currentTimeMillis()
                        val filteredIncoming = liveUsers.filter { !lockedDeletedUserIds.contains(it.id) }
                        val resolvedUsers = filteredIncoming.map { u ->
                            val lockedEntry = lockedUserStates[u.id]
                            if (lockedEntry != null) {
                                val (lockedUser, lockTime) = lockedEntry
                                if (now - lockTime < 20_000L) {
                                    if (u.role == lockedUser.role && u.isBanned == lockedUser.isBanned && u.funds == lockedUser.funds) {
                                        lockedUserStates.remove(u.id)
                                        u
                                    } else {
                                        lockedUser
                                    }
                                } else {
                                    lockedUserStates.remove(u.id)
                                    u
                                }
                            } else {
                                u
                            }
                        }.toMutableList()

                        lockedUserStates.forEach { (uid, pair) ->
                            val (lockedUser, lockTime) = pair
                            if (now - lockTime < 20_000L && resolvedUsers.none { it.id == uid } && !lockedDeletedUserIds.contains(uid)) {
                                resolvedUsers.add(0, lockedUser)
                            }
                        }

                        _uiState.update { curr ->
                            val s = curr as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                            s.withDerivedMetrics(newUsers = resolvedUsers)
                        }
                    }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 2. Stream support tickets
        val ticketsJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                repository.getLiveSupportTicketsStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { tickets ->
                        _uiState.update { curr ->
                            val s = curr as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                            s.withDerivedMetrics(newSupportTickets = tickets)
                        }
                    }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 3. Stream payout requests
        val payoutsJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                repository.getLivePayoutRequestsStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { payouts ->
                        _uiState.update { curr ->
                            val s = curr as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                            s.withDerivedMetrics(newPayoutRequests = payouts)
                        }
                    }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 4. Stream admins
        val adminsJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                repository.getLiveAdminsStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { adminList ->
                        val now = System.currentTimeMillis()
                        val filteredIncoming = adminList.filter { !lockedDeletedAdminUids.contains(it.uid) }
                        val resolvedAdmins = filteredIncoming.map { admin ->
                            val lockedEntry = lockedAdminStates[admin.uid]
                            if (lockedEntry != null) {
                                val (lockedAdmin, lockTime) = lockedEntry
                                if (now - lockTime < 20_000L) {
                                    if (admin.role.equals(lockedAdmin.role, ignoreCase = true) && admin.active == lockedAdmin.active && admin.name == lockedAdmin.name) {
                                        lockedAdminStates.remove(admin.uid)
                                        admin
                                    } else {
                                        lockedAdmin
                                    }
                                } else {
                                    lockedAdminStates.remove(admin.uid)
                                    admin
                                }
                            } else {
                                admin
                            }
                        }.toMutableList()

                        lockedAdminStates.forEach { (uid, pair) ->
                            val (lockedAdmin, lockTime) = pair
                            if (now - lockTime < 20_000L && resolvedAdmins.none { it.uid == uid } && !lockedDeletedAdminUids.contains(uid)) {
                                resolvedAdmins.add(0, lockedAdmin)
                            }
                        }

                        _uiState.update { curr ->
                            val s = curr as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                            s.withDerivedMetrics(newAdmins = resolvedAdmins)
                        }
                    }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 5. Stream banned users
        val bannedJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                repository.getLiveBannedUsersStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { bannedMap ->
                        _uiState.update { curr ->
                            val s = curr as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                            s.withDerivedMetrics(newBannedUsers = bannedMap)
                        }
                    }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 6. Stream tournaments
        val tournamentsJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                repository.getLiveTournamentsStream()
                    .distinctUntilChanged()
                    .debounce(100L)
                    .flowOn(Dispatchers.Default)
                    .collect { realtimeTournaments ->
                        _uiState.update { curr ->
                            val s = curr as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                            s.withDerivedMetrics(newTournaments = realtimeTournaments)
                        }
                    }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 7. Stream tokens
        val tokensJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                repository.getLiveTokensStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { tokens ->
                        _uiState.update { curr ->
                            val s = curr as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                            s.withDerivedMetrics(newTokens = tokens)
                        }
                    }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 8. Stream banners
        val bannersJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                repository.getLiveBannersStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { banners ->
                        _uiState.update { curr ->
                            val s = curr as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                            s.withDerivedMetrics(newBanners = banners)
                        }
                    }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 9. Stream Global Announcements
        val annJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                repository.getGlobalAnnouncementsStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { annList ->
                        _uiState.update { curr ->
                            val s = curr as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                            s.withDerivedMetrics(newGlobalAnnouncements = annList)
                        }
                    }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 10. Stream Match Proof Submissions
        val proofsJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                repository.getMatchProofsStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { proofsList ->
                        _uiState.update { curr ->
                            val s = curr as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                            s.withDerivedMetrics(newMatchProofs = proofsList)
                        }
                    }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 11. Stream Live Notifications & Campaigns
        val notifsJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                repository.getNotificationsStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { notifs ->
                        _uiState.update { curr ->
                            val s = curr as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                            s.withDerivedMetrics(newNotifications = notifs)
                        }
                    }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        synchronized(activeStreamJobs) {
            activeStreamJobs.addAll(
                listOf(userJob, ticketsJob, payoutsJob, adminsJob, bannedJob, tournamentsJob, tokensJob, bannersJob, annJob, proofsJob, notifsJob)
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        synchronized(activeStreamJobs) {
            activeStreamJobs.forEach { it.cancel() }
            activeStreamJobs.clear()
        }
        lockedAdminStates.clear()
        lockedDeletedAdminUids.clear()
        lockedUserStates.clear()
        lockedDeletedUserIds.clear()
    }

    // Actions
    fun updateTicketStatus(ticket: SupportTicket, newStatus: String, adminNote: String = "", assignedTo: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
            val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            repository.banPlayer(uid, email, reason, currAdminEmail)
        }
    }

    fun unbanPlayer(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.unbanPlayer(uid)
        }
    }

    fun adjustUserBalance(uid: String, newBalance: Double, reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
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
        _uiState.update { curr ->
            val s = curr as? DashboardState.Success ?: return@update curr
            val updatedPayouts = s.payoutRequests.map {
                if (it.id == request.id) it.copy(status = "approved") else it
            }
            s.withDerivedMetrics(newPayoutRequests = updatedPayouts)
        }
        viewModelScope.launch(Dispatchers.IO) {
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
        _uiState.update { curr ->
            val s = curr as? DashboardState.Success ?: return@update curr
            val updatedPayouts = s.payoutRequests.map {
                if (it.id == request.id) it.copy(status = "rejected", rejectionReason = reason) else it
            }
            s.withDerivedMetrics(newPayoutRequests = updatedPayouts)
        }
        viewModelScope.launch(Dispatchers.IO) {
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
        _uiState.update { curr ->
            val s = curr as? DashboardState.Success ?: return@update curr
            val updatedList = listOf(tournament) + s.tournaments.filter { it.id != tournament.id }
            s.withDerivedMetrics(newTournaments = updatedList)
        }
        viewModelScope.launch(Dispatchers.IO) {
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
        _uiState.update { curr ->
            val s = curr as? DashboardState.Success ?: return@update curr
            val updatedList = s.tournaments.map { if (it.id == tournament.id) tournament else it }
            s.withDerivedMetrics(newTournaments = updatedList)
        }
        viewModelScope.launch(Dispatchers.IO) {
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
        _uiState.update { curr ->
            val s = curr as? DashboardState.Success ?: return@update curr
            val updatedList = s.tournaments.map {
                if (it.id == tournamentId) it.copy(status = "CANCELLED", cancellationReason = reason, cancelledAt = System.currentTimeMillis()) else it
            }
            s.withDerivedMetrics(newTournaments = updatedList)
        }
        viewModelScope.launch(Dispatchers.IO) {
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
        _uiState.update { curr ->
            val s = curr as? DashboardState.Success ?: return@update curr
            val updatedList = s.tournaments.filter { it.id != tournamentId }
            s.withDerivedMetrics(newTournaments = updatedList)
        }
        viewModelScope.launch(Dispatchers.IO) {
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
        return repository.getTournamentLiveStream(tournamentId).flowOn(Dispatchers.Default)
    }

    fun updateRoomCredentials(tournamentId: String, roomId: String, roomPassword: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateRoomCredentials(tournamentId, roomId, roomPassword)
        }
    }

    fun grantAdminAccess(uid: String, email: String, name: String, role: String) {
        val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
        viewModelScope.launch(Dispatchers.IO) {
            val result = userRoleManager.grantAdminRole(
                uid = uid,
                email = email,
                name = name,
                role = role,
                grantedBy = currAdminEmail
            )
            result.onSuccess { confirmedRecord ->
                lockedDeletedAdminUids.remove(confirmedRecord.uid)
                lockedAdminStates[confirmedRecord.uid] = confirmedRecord to System.currentTimeMillis()
                
                // Server-side confirmation completed: safely update local UI state
                _uiState.update { curr ->
                    val s = curr as? DashboardState.Success ?: return@update curr
                    val updatedAdmins = listOf(confirmedRecord) + s.admins.filter { 
                        it.uid != confirmedRecord.uid && !it.email.equals(confirmedRecord.email, ignoreCase = true) 
                    }
                    s.withDerivedMetrics(newAdmins = updatedAdmins)
                }
                GlobalErrorManager.emitSuccess("Admin access granted to ${confirmedRecord.name}")
            }.onFailure { e ->
                e.printStackTrace()
                GlobalErrorManager.emitError("Failed to grant admin: ${e.message}")
            }
        }
    }

    fun revokeAdminAccess(adminUid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = userRoleManager.revokeAdminRole(adminUid)
            result.onSuccess { revokedRecord ->
                lockedAdminStates[adminUid] = revokedRecord to System.currentTimeMillis()
                
                // Server-side confirmation completed: safely update local UI state
                _uiState.update { curr ->
                    val s = curr as? DashboardState.Success ?: return@update curr
                    s.withDerivedMetrics(newAdmins = s.admins.map { if (it.uid == adminUid) revokedRecord else it })
                }
                GlobalErrorManager.emitSuccess("Admin access revoked")
            }.onFailure { e ->
                e.printStackTrace()
                GlobalErrorManager.emitError("Failed to revoke admin: ${e.message}")
            }
        }
    }

    fun updateAdminRecord(admin: AdminRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = userRoleManager.updateAdminRecord(admin)
            result.onSuccess { updatedAdmin ->
                lockedDeletedAdminUids.remove(updatedAdmin.uid)
                lockedAdminStates[updatedAdmin.uid] = updatedAdmin to System.currentTimeMillis()
                
                // Server-side confirmation completed: safely update local UI state
                _uiState.update { curr ->
                    val s = curr as? DashboardState.Success ?: return@update curr
                    s.withDerivedMetrics(newAdmins = s.admins.map { if (it.uid == updatedAdmin.uid) updatedAdmin else it })
                }
                GlobalErrorManager.emitSuccess("Admin record updated")
            }.onFailure { e ->
                e.printStackTrace()
                GlobalErrorManager.emitError("Failed to update admin: ${e.message}")
            }
        }
    }

    fun deleteAdminRecord(adminUid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = userRoleManager.deleteAdminRecord(adminUid)
            result.onSuccess {
                lockedDeletedAdminUids.add(adminUid)
                lockedAdminStates.remove(adminUid)
                
                // Server-side confirmation completed: safely update local UI state
                _uiState.update { curr ->
                    val s = curr as? DashboardState.Success ?: return@update curr
                    s.withDerivedMetrics(newAdmins = s.admins.filter { it.uid != adminUid })
                }
                GlobalErrorManager.emitSuccess("Admin removed")
            }.onFailure { e ->
                e.printStackTrace()
                GlobalErrorManager.emitError("Failed to delete admin: ${e.message}")
            }
        }
    }

    fun toggleUserBan(user: UserProfile, explicitBanned: Boolean? = null) {
        val newBanned = explicitBanned ?: !user.isBanned
        val reason = if (newBanned) user.banReason.ifBlank { "Banned by Admin Panel" } else ""
        val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
        
        viewModelScope.launch(Dispatchers.IO) {
            val result = userRoleManager.toggleUserBan(user, newBanned, reason, currAdminEmail)
            result.onSuccess { updatedUser ->
                lockedUserStates[user.id] = updatedUser to System.currentTimeMillis()
                
                // Server-side confirmation completed: safely update local UI state
                _uiState.update { curr ->
                    val s = curr as? DashboardState.Success ?: return@update curr
                    s.withDerivedMetrics(users = s.users.map { if (it.id == user.id) updatedUser else it })
                }
                if (newBanned) {
                    GlobalErrorManager.emitSuccess("Player ${user.username} (${user.id}) banned successfully")
                } else {
                    GlobalErrorManager.emitSuccess("Player ${user.username} (${user.id}) unbanned successfully")
                }
            }.onFailure { e ->
                e.printStackTrace()
                GlobalErrorManager.emitError("Failed to toggle ban: ${e.message}")
            }
        }
    }

    fun updateUserProfile(user: UserProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = userRoleManager.updateUserProfile(user)
            result.onSuccess { confirmedUser ->
                lockedUserStates[confirmedUser.id] = confirmedUser to System.currentTimeMillis()
                if (confirmedUser.email.isNotBlank()) {
                    repository.recordAccountLocally(confirmedUser.email, confirmedUser.username, confirmedUser.role, confirmedUser.id)
                }
                
                // Server-side confirmation completed: safely update local UI state
                _uiState.update { curr ->
                    val s = curr as? DashboardState.Success ?: return@update curr
                    val existingIndex = s.users.indexOfFirst { 
                        it.id == confirmedUser.id || (confirmedUser.email.isNotBlank() && it.email.equals(confirmedUser.email, ignoreCase = true)) 
                    }
                    val updatedList = if (existingIndex >= 0) {
                        s.users.toMutableList().apply { set(existingIndex, confirmedUser) }
                    } else {
                        s.users + confirmedUser
                    }
                    s.withDerivedMetrics(users = updatedList)
                }
                GlobalErrorManager.emitSuccess("User profile updated for ${confirmedUser.username}")
            }.onFailure { e ->
                e.printStackTrace()
                GlobalErrorManager.emitError("Failed to update user profile: ${e.message}")
            }
        }
    }

    fun updateUserRole(userId: String, newRole: String, isAdmin: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = userRoleManager.updateUserRole(userId, newRole, isAdmin)
            result.onSuccess {
                _uiState.update { curr ->
                    val s = curr as? DashboardState.Success ?: return@update curr
                    val updatedUsers = s.users.map { if (it.id == userId) it.copy(role = newRole) else it }
                    s.withDerivedMetrics(users = updatedUsers)
                }
                GlobalErrorManager.emitSuccess("Role updated to $newRole")
            }.onFailure { e ->
                e.printStackTrace()
                GlobalErrorManager.emitError("Failed to update user role: ${e.message}")
            }
        }
    }

    fun loadUserData(onFinished: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                refreshRealtimeData()
                withContext(Dispatchers.Main) {
                    onFinished?.invoke()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onFinished?.invoke()
                }
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
        _uiState.update { curr ->
            val s = curr as? DashboardState.Success ?: return@update curr
            s.withDerivedMetrics(users = s.users.map { if (it.id == user.id) it.copy(funds = newBalance) else it })
        }
        viewModelScope.launch(Dispatchers.IO) {
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
        _uiState.update { curr ->
            val s = curr as? DashboardState.Success ?: return@update curr
            s.withDerivedMetrics(users = s.users.filter { it.id != user.id })
        }
        viewModelScope.launch(Dispatchers.IO) {
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
        _uiState.update { curr ->
            val s = curr as? DashboardState.Success ?: return@update curr
            val updated = s.supportTickets.map { if (it.id == ticket.id) ticket else it }
            s.withDerivedMetrics(newSupportTickets = updated)
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateSupportTicket(ticket)
                GlobalErrorManager.emitSuccess("Ticket #${ticket.id} updated to ${ticket.status}")
            } catch (e: Exception) {
                GlobalErrorManager.emitError("Failed to update ticket: ${e.message}")
            }
        }
    }

    suspend fun getTicketMessagesStream(ticketId: String): Flow<List<TicketMessage>> {
        return repository.getLiveTicketMessagesStream(ticketId).flowOn(Dispatchers.Default)
    }

    fun sendTicketMessage(
        ticketId: String,
        message: String,
        senderName: String = "Velorix Support Admin",
        senderRole: String = "ADMIN",
        onComplete: (Boolean) -> Unit = {}
    ) {
        val cleanMsg = message.trim()
        if (cleanMsg.isBlank() || ticketId.isBlank()) {
            onComplete(false)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentAdminUid = repository.auth.currentUser?.uid ?: "ADMIN_MASTER"
                val success = repository.sendTicketMessage(
                    ticketId = ticketId,
                    senderId = currentAdminUid,
                    senderName = senderName,
                    senderRole = senderRole,
                    messageText = cleanMsg
                )
                if (success) {
                    GlobalErrorManager.emitSuccess("Support response sent")
                }
                withContext(Dispatchers.Main) {
                    onComplete(success)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                GlobalErrorManager.emitError("Failed to send message: ${e.message}")
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    fun issueTicketCompensation(
        ticket: SupportTicket,
        amount: Double,
        reason: String = "Support Ticket Resolution Compensation",
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (amount <= 0) {
            GlobalErrorManager.emitError("Compensation amount must be greater than ₹0")
            onComplete(false)
            return
        }
        val curr = _uiState.value as? DashboardState.Success
        val targetUser = curr?.users?.find { it.email.equals(ticket.userEmail, ignoreCase = true) || it.id == ticket.userId }
        val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (targetUser != null) {
                    val newBalance = targetUser.funds + amount
                    repository.adjustUserBalance(targetUser.id, newBalance, reason, adminEmail)
                    _uiState.update { current ->
                        val s = current as? DashboardState.Success ?: return@update current
                        s.withDerivedMetrics(users = s.users.map { if (it.id == targetUser.id) it.copy(funds = newBalance) else it })
                    }
                } else if (ticket.userEmail.isNotBlank()) {
                    val fallbackId = "USR_${ticket.userEmail.hashCode()}"
                    repository.adjustUserBalance(fallbackId, amount, reason, adminEmail)
                }
                // Send automated system confirmation message in ticket chat
                repository.sendTicketMessage(
                    ticketId = ticket.id,
                    senderId = repository.auth.currentUser?.uid ?: "ADMIN_PAYOUTS",
                    senderName = "Velorix Financial Support",
                    senderRole = "SYSTEM",
                    messageText = "₹${"%.2f".format(amount)} has been credited to your Velorix wallet for ticket #${ticket.id}. Reason: $reason"
                )
                // Update ticket status to RESOLVED
                val updatedTicket = ticket.copy(
                    status = "RESOLVED",
                    adminNote = "Compensation ₹$amount credited. $reason"
                )
                repository.updateSupportTicket(updatedTicket)
                saveComplaint(updatedTicket)
                GlobalErrorManager.emitSuccess("₹$amount credited to user and ticket #${ticket.id} marked as RESOLVED!")
                withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                GlobalErrorManager.emitError("Failed to process compensation: ${e.message}")
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    fun deleteComplaint(ticketId: String) {
        _uiState.update { curr ->
            val s = curr as? DashboardState.Success ?: return@update curr
            s.withDerivedMetrics(newSupportTickets = s.supportTickets.filter { it.id != ticketId })
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteSupportTicket(ticketId)
                GlobalErrorManager.emitSuccess("Ticket #$ticketId dissolved/deleted successfully.")
            } catch (e: Exception) {
                GlobalErrorManager.emitError("Failed to delete ticket: ${e.message}")
            }
        }
    }

    fun saveToken(token: CheckInToken) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveToken(token)
        }
    }

    fun saveBanner(banner: AppAnnouncementBanner) {
        val rateLimitCheck = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.BROADCAST_ANNOUNCEMENT, banner.id)
        if (!rateLimitCheck.isAllowed) {
            GlobalErrorManager.emitError(rateLimitCheck.reasonMessage)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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
            withContext(Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    fun submitMatchProof(proof: com.example.domain.model.MatchProofSubmission) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.submitMatchProof(proof)
        }
    }

    fun approveMatchProof(proofId: String, prizeAmount: Double, killsCount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            repository.approveMatchProof(proofId, currAdminEmail, prizeAmount, killsCount)
        }
    }

    fun rejectMatchProof(proofId: String, reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            repository.rejectMatchProof(proofId, reason, currAdminEmail)
        }
    }

    fun publishGlobalAnnouncement(announcement: com.example.domain.model.GlobalAnnouncement) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.publishGlobalAnnouncement(announcement)
        }
    }

    fun deleteGlobalAnnouncement(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
            val json = repository.exportAllTournamentsJson()
            withContext(Dispatchers.Main) {
                onResult(json)
            }
        }
    }

    fun importTournamentsFromJson(jsonStr: String, onResult: (Pair<Int, String>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.importTournamentsFromJson(jsonStr)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    fun syncAllTournamentsToCloud(onResult: (Pair<Int, String>) -> Unit) {
        extractAndSyncRealDataFromBackend { result ->
            onResult(Pair(result.tournamentsCount, "Extracted ${result.tournamentsCount} tournaments & ${result.usersCount} players directly from Firebase backend!"))
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = repository.joinTournament(tournamentId, player, entryFee)
                withContext(Dispatchers.Main) {
                    if (success) {
                        onComplete(true, "Successfully registered and slot locked!")
                    } else {
                        onComplete(false, "Registration failed: Insufficient 2D wallet balance or slots full.")
                    }
                }
            } catch (e: Exception) {
                GlobalErrorManager.emitFirestoreError("Join Tournament", e)
                withContext(Dispatchers.Main) {
                    onComplete(false, e.localizedMessage ?: "Failed to join tournament")
                }
            }
        }
    }

    fun markNotificationAsRead(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markNotificationAsRead(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllNotifications()
        }
    }

    fun publishCampaign(
        title: String,
        message: String,
        priority: String = "HIGH",
        tournamentId: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.publishCampaignNotification(title, message, priority, tournamentId)
        }
    }

    fun testPushNotification(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            val result = repository.purgeAllDemoAndMockData(adminEmail)
            withContext(Dispatchers.Main) {
                if (result.first) {
                    GlobalErrorManager.emitSuccess("Mock & Demo data purged successfully.")
                    refreshRealtimeData()
                } else {
                    GlobalErrorManager.emitError("Purge failed: ${result.second}")
                }
                onResult(result.first, result.second)
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

