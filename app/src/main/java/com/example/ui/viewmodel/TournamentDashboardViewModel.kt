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
import com.example.domain.model.AdminChannelType
import com.example.domain.model.AdminChannelItem
import com.example.domain.model.AccountDeletionRequest
import com.example.domain.model.DataExportRequest
import com.example.domain.model.DepositRequest
import com.example.domain.model.TokenConversionRecord
import com.example.domain.model.MissionItem
import com.example.domain.model.TournamentChatMessage
import com.example.domain.model.SystemConfig
import com.example.domain.model.TournamentStandingEntry
import com.example.domain.model.AdminAuditLogItem
import com.example.domain.model.RedeemCodeItem
import com.example.domain.model.RedeemClaimRecord
import com.example.domain.model.UnifiedAdminRequest
import com.example.domain.model.ChatReportItem
import com.example.domain.model.VerifiedBankDeposit
import com.example.domain.model.ManualNotificationRecord
import com.example.domain.model.ManualNotificationResult
import com.example.data.repository.AdminIntegrationSyncManager
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
        val categories: List<com.example.domain.model.TournamentCategory> = com.example.domain.model.FreeFireCategories.DEFAULT_CATEGORIES,
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
    newGlobalAnnouncements: List<com.example.domain.model.GlobalAnnouncement>? = null,
    newCategories: List<com.example.domain.model.TournamentCategory>? = null,
    categories: List<com.example.domain.model.TournamentCategory>? = null
): DashboardState.Success {
    val effectiveUsers = newUsers ?: users ?: this.users
    val effectiveAdmins = newAdmins ?: admins ?: this.admins
    val effectiveTickets = newSupportTickets ?: supportTickets ?: this.supportTickets
    val effectivePayouts = newPayoutRequests ?: payoutRequests ?: this.payoutRequests
    val effectiveProofs = newMatchProofs ?: this.matchProofs
    val effectiveNotifs = newNotifications ?: this.notifications
    val effectiveTournaments = newTournaments ?: tournaments ?: this.tournaments

    val cleanUsers = effectiveUsers.filter { !it.isGuestSession }.map { u ->
        val matchedRecords = effectiveTournaments.mapNotNull { t ->
            val participant = t.participants.find { p ->
                (p.userId.isNotBlank() && p.userId == u.id) ||
                (p.playerId.isNotBlank() && p.playerId == u.id) ||
                (u.gameId.isNotBlank() && p.freeFireUid.equals(u.gameId, ignoreCase = true)) ||
                (u.ign.isNotBlank() && p.inGameName.equals(u.ign, ignoreCase = true)) ||
                (u.username.isNotBlank() && p.playerName.equals(u.username, ignoreCase = true))
            }
            if (participant != null) {
                val entryPaid = if (participant.entryFeePaid > 0.0) participant.entryFeePaid else t.entryFee.toDouble()
                com.example.domain.model.PlayerMatchRecord(
                    tournamentId = t.id,
                    tournamentTitle = t.title,
                    game = t.game,
                    category = t.category,
                    format = t.format,
                    map = t.map,
                    joinedAt = if (participant.joinedAt > 0L) participant.joinedAt else (t.parsedStartsAtMillis ?: 0L),
                    entryFeePaid = entryPaid,
                    paidFromDeposit = if (participant.paidFromDeposit > 0.0) participant.paidFromDeposit else entryPaid,
                    paidFromWinning = participant.paidFromWinning,
                    paidFromBonus = participant.paidFromBonus,
                    kills = participant.kills,
                    rank = participant.rank,
                    winningAmount = participant.winningAmount,
                    status = t.status
                )
            } else null
        }
        val mergedHistory = (u.matchHistory + matchedRecords)
            .distinctBy { it.tournamentId }
            .sortedByDescending { it.joinedAt }
        val joinedIds = (u.joinedMatchIds + mergedHistory.map { it.tournamentId }).filter { it.isNotBlank() }.distinct()
        val earliestMatchJoin = mergedHistory.map { it.joinedAt }.filter { it > 0L }.minOrNull() ?: u.firstMatchJoinedAt
        val latestMatchJoin = mergedHistory.map { it.joinedAt }.filter { it > 0L }.maxOrNull() ?: u.lastMatchJoinedAt
        val supportCases = effectiveTickets.count {
            (it.userId.isNotBlank() && it.userId == u.id) ||
            (u.email.isNotBlank() && it.userEmail.equals(u.email, ignoreCase = true))
        }.coerceAtLeast(u.supportCasesCount)
        val derivedTotalMatches = maxOf(u.totalMatchesJoined, joinedIds.size, mergedHistory.size)
        val derivedEntryFees = maxOf(u.totalEntryFeesPaid, mergedHistory.sumOf { it.entryFeePaid })
        val derivedEarnings = maxOf(u.totalEarnings, u.winningFunds, mergedHistory.sumOf { it.winningAmount })
        val derivedKills = maxOf(u.kills, mergedHistory.sumOf { it.kills })
        u.copy(
            firstAppJoinAt = if (u.firstAppJoinAt > 0L) u.firstAppJoinAt else u.createdAt,
            firstMatchJoinedAt = earliestMatchJoin,
            lastMatchJoinedAt = latestMatchJoin,
            totalMatchesJoined = derivedTotalMatches,
            totalEntryFeesPaid = derivedEntryFees,
            totalEarnings = derivedEarnings,
            kills = derivedKills,
            supportCasesCount = supportCases,
            joinedMatchIds = joinedIds,
            matchHistory = mergedHistory
        )
    }
    val sortedLeaderboard = if (newUsers != null || leaderboardPlayers.isEmpty()) {
        cleanUsers.sortedWith(
            compareByDescending<UserProfile> { it.totalEarnings }
                .thenByDescending { it.wins }
                .thenByDescending { it.kills }
        )
    } else {
        leaderboardPlayers.filter { !it.isGuestSession }
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
        users = cleanUsers,
        admins = effectiveAdmins,
        supportTickets = effectiveTickets,
        payoutRequests = effectivePayouts,
        matchProofs = effectiveProofs,
        notifications = effectiveNotifs,
        bannedUsers = newBannedUsers ?: this.bannedUsers,
        checkInTokens = newTokens ?: this.checkInTokens,
        banners = newBanners ?: this.banners,
        globalAnnouncements = newGlobalAnnouncements ?: this.globalAnnouncements,
        categories = newCategories ?: categories ?: this.categories,
        isSyncing = newSyncing ?: this.isSyncing,
        leaderboardPlayers = sortedLeaderboard,
        totalRegisteredUsersCount = cleanUsers.size,
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
    val integrationSyncManager: AdminIntegrationSyncManager = AdminIntegrationSyncManager(repository.database, repository.firestore)

    private val _selectedAdminChannel = MutableStateFlow(AdminChannelType.ACCOUNT_DELETION)
    val selectedAdminChannel: StateFlow<AdminChannelType> = _selectedAdminChannel.asStateFlow()

    private val _activeChannelItems = MutableStateFlow<List<AdminChannelItem>>(emptyList())
    val activeChannelItems: StateFlow<List<AdminChannelItem>> = _activeChannelItems.asStateFlow()

    private val _accountDeletionRequests = MutableStateFlow<List<AccountDeletionRequest>>(emptyList())
    val accountDeletionRequests: StateFlow<List<AccountDeletionRequest>> = _accountDeletionRequests.asStateFlow()

    private val _dataExportRequests = MutableStateFlow<List<DataExportRequest>>(emptyList())
    val dataExportRequests: StateFlow<List<DataExportRequest>> = _dataExportRequests.asStateFlow()

    private val _depositRequests = MutableStateFlow<List<DepositRequest>>(emptyList())
    val depositRequests: StateFlow<List<DepositRequest>> = _depositRequests.asStateFlow()

    private val _tokenConversions = MutableStateFlow<List<TokenConversionRecord>>(emptyList())
    val tokenConversions: StateFlow<List<TokenConversionRecord>> = _tokenConversions.asStateFlow()

    private val _missions = MutableStateFlow<List<MissionItem>>(emptyList())
    val missions: StateFlow<List<MissionItem>> = _missions.asStateFlow()

    private val _systemConfig = MutableStateFlow(SystemConfig())
    val systemConfig: StateFlow<SystemConfig> = _systemConfig.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AdminAuditLogItem>>(emptyList())
    val auditLogs: StateFlow<List<AdminAuditLogItem>> = _auditLogs.asStateFlow()

    private val _redeemCodes = MutableStateFlow<List<RedeemCodeItem>>(emptyList())
    val redeemCodes: StateFlow<List<RedeemCodeItem>> = _redeemCodes.asStateFlow()

    private val _redeemClaims = MutableStateFlow<List<RedeemClaimRecord>>(emptyList())
    val redeemClaims: StateFlow<List<RedeemClaimRecord>> = _redeemClaims.asStateFlow()

    private val _unifiedRequests = MutableStateFlow<List<UnifiedAdminRequest>>(emptyList())
    val unifiedRequests: StateFlow<List<UnifiedAdminRequest>> = _unifiedRequests.asStateFlow()

    private val _chatReports = MutableStateFlow<List<ChatReportItem>>(emptyList())
    val chatReports: StateFlow<List<ChatReportItem>> = _chatReports.asStateFlow()

    private val _unifiedWithdrawals = MutableStateFlow<List<PayoutRequest>>(emptyList())
    val unifiedWithdrawals: StateFlow<List<PayoutRequest>> = _unifiedWithdrawals.asStateFlow()

    private val _verifiedBankDeposits = MutableStateFlow<List<VerifiedBankDeposit>>(emptyList())
    val verifiedBankDeposits: StateFlow<List<VerifiedBankDeposit>> = _verifiedBankDeposits.asStateFlow()

    private val _manualNotifications = MutableStateFlow<List<ManualNotificationRecord>>(emptyList())
    val manualNotifications: StateFlow<List<ManualNotificationRecord>> = _manualNotifications.asStateFlow()

    private val _securityAuditLogs = MutableStateFlow<List<AdminAuditLogItem>>(emptyList())
    val securityAuditLogs: StateFlow<List<AdminAuditLogItem>> = _securityAuditLogs.asStateFlow()

    private val _reconciliationSummaryJson = MutableStateFlow<String?>(null)
    val reconciliationSummaryJson: StateFlow<String?> = _reconciliationSummaryJson.asStateFlow()

    private val _isReconcilingBackend = MutableStateFlow(false)
    val isReconcilingBackend: StateFlow<Boolean> = _isReconcilingBackend.asStateFlow()

    private var activeChannelStreamJob: Job? = null

    // In-memory locks to prevent real-time stream snapshots from causing 'revert-and-reactivate' UI glitches
    private val lockedAdminStates = java.util.concurrent.ConcurrentHashMap<String, Pair<AdminRecord, Long>>()
    private val lockedDeletedAdminUids = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val lockedUserStates = java.util.concurrent.ConcurrentHashMap<String, Pair<UserProfile, Long>>()
    private val lockedDeletedUserIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun setManualLoginEmail(email: String?) {
        val cleanEmail = email?.trim()?.lowercase()?.takeIf {
            com.example.data.validation.TournamentBackendValidator.isAllowedAdminEmail(it)
        }
        overrideUserEmail = cleanEmail
        if (cleanEmail == null) {
            synchronized(activeStreamJobs) {
                activeStreamJobs.forEach { it.cancel() }
                activeStreamJobs.clear()
            }
            _uiState.value = DashboardState.Loading
            return
        }
        val curr = _uiState.value
        if (curr is DashboardState.Success) {
            _uiState.value = curr.copy(currentUserEmail = cleanEmail)
        } else {
            _uiState.value = DashboardState.Success(currentUserEmail = cleanEmail)
        }
        val firebaseEmail = repository.auth.currentUser?.email?.trim()?.lowercase()
        if (com.example.data.validation.TournamentBackendValidator.isAllowedAdminEmail(firebaseEmail)) {
            startStreams(forceRestart = true)
            extractAndSyncRealDataFromBackend()
        }
    }

    init {
        val currentFirebaseEmail = repository.auth.currentUser?.email?.trim()?.lowercase()
        if (com.example.data.validation.TournamentBackendValidator.isAllowedAdminEmail(currentFirebaseEmail)) {
            overrideUserEmail = currentFirebaseEmail
            startStreams(forceRestart = false)
            extractAndSyncRealDataFromBackend()
        }
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
        val currentTourneysCount = (_uiState.value as? DashboardState.Success)?.tournaments?.size ?: 0
        if (currentTourneysCount == 0 && repository.locallyDeletedTournamentIds.isNotEmpty()) {
            repository.clearDeletedTournamentCache()
        }

        // 2. Set syncing state in UI atomically
        _uiState.update { curr ->
            (curr as? DashboardState.Success)?.copy(isSyncing = true) ?: curr
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authEmail = repository.auth.currentUser?.email?.trim()?.lowercase()
                if (!com.example.data.validation.TournamentBackendValidator.isAllowedAdminEmail(authEmail)) {
                    _uiState.update { curr ->
                        (curr as? DashboardState.Success)?.copy(isSyncing = false) ?: curr
                    }
                    return@launch
                }
                val targetEmail = authEmail!!
                repository.ensureAuthenticatedSession(targetEmail)
                GlobalErrorManager.emitSuccess("Extracting real data from Firebase RTDB & Cloud Firestore...")
                var result = repository.extractRealDataFromBackend(forceServer = true)
                if (result.tournaments.isEmpty() && repository.locallyDeletedTournamentIds.isNotEmpty()) {
                    repository.clearDeletedTournamentCache()
                    result = repository.extractRealDataFromBackend(forceServer = true)
                }
                val cleanTournaments = result.tournaments.filter { t ->
                    t.id !in repository.locallyDeletedTournamentIds &&
                        !repository.isPhantomOrDeletedTournament(
                            tId = t.id,
                            rawTitle = t.title,
                            entryFee = t.entryFee,
                            prizePool = t.prizePool,
                            roomId = t.roomDetails?.roomId,
                            roomPass = t.roomDetails?.roomPassword
                        )
                }

                val initialEmail = targetEmail

                withContext(Dispatchers.Default) {
                    _uiState.update { curr ->
                        val base = curr as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                        base.withDerivedMetrics(
                            newTournaments = cleanTournaments,
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

        val authEmail = repository.auth.currentUser?.email?.trim()?.lowercase()
        if (!com.example.data.validation.TournamentBackendValidator.isAllowedAdminEmail(authEmail)) {
            return
        }
        val initialEmail = authEmail!!
        _uiState.update { curr ->
            if (curr !is DashboardState.Success) {
                DashboardState.Success(currentUserEmail = initialEmail)
            } else {
                curr.copy(currentUserEmail = initialEmail)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureAuthenticatedSession(initialEmail)
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
                                if (now - lockTime < 25_000L) {
                                    if (u.role == lockedUser.role &&
                                        u.isBanned == lockedUser.isBanned &&
                                        u.funds == lockedUser.funds &&
                                        u.depositFunds == lockedUser.depositFunds &&
                                        u.winningFunds == lockedUser.winningFunds &&
                                        u.bonusFunds == lockedUser.bonusFunds
                                    ) {
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
                            val finalUsers = if (resolvedUsers.size <= 1 && s.users.size > 1) {
                                (s.users + resolvedUsers).distinctBy { it.id }.filter { !lockedDeletedUserIds.contains(it.id) }
                            } else {
                                resolvedUsers
                            }
                            s.withDerivedMetrics(newUsers = finalUsers)
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
                        val filteredTournaments = realtimeTournaments.filter { t ->
                            t.id !in repository.locallyDeletedTournamentIds &&
                                !repository.isPhantomOrDeletedTournament(
                                    tId = t.id,
                                    rawTitle = t.title,
                                    entryFee = t.entryFee,
                                    prizePool = t.prizePool,
                                    roomId = t.roomDetails?.roomId,
                                    roomPass = t.roomDetails?.roomPassword
                                )
                        }
                        _uiState.update { curr ->
                            val s = curr as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                            val finalTournaments = if (filteredTournaments.isEmpty() && s.tournaments.isNotEmpty()) {
                                s.tournaments.filter { it.id !in repository.locallyDeletedTournamentIds }
                            } else {
                                filteredTournaments
                            }
                            s.withDerivedMetrics(newTournaments = finalTournaments)
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

        // 12. Stream Dynamic Categories extracted from Firebase RTDB & Firestore
        val categoriesJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                repository.getLiveCategoriesStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { catList ->
                        _uiState.update { curr ->
                            val s = curr as? DashboardState.Success ?: DashboardState.Success(currentUserEmail = initialEmail)
                            s.withDerivedMetrics(newCategories = catList)
                        }
                    }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 13. Stream Account Deletion Requests (#account-deletions)
        val deletionRequestsJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                integrationSyncManager.getAccountDeletionRequestsStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { list ->
                        _accountDeletionRequests.value = list
                    }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 14. Stream Data Export Archive (#data-exports)
        val dataExportsJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                integrationSyncManager.getDataExportRequestsStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { list ->
                        _dataExportRequests.value = list
                    }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 15. Stream Manual / Gateway Deposit Verification Queue (#deposits)
        val depositsJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                integrationSyncManager.getDepositRequestsStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { list ->
                        _depositRequests.value = list
                    }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 16. Stream Token Conversion Ledger (#token-ledger)
        val tokenConvJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                integrationSyncManager.getTokenConversionsStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { list ->
                        _tokenConversions.value = list
                    }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 17. Stream Missions (/missions)
        val missionsJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                integrationSyncManager.getMissionsStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { list ->
                        _missions.value = list
                    }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 18. Stream Global System Config (/system_config & /app_config)
        val sysConfigJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                integrationSyncManager.getSystemConfigStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { cfg ->
                        _systemConfig.value = cfg
                    }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 19. Stream Audit Logs (/audit_logs)
        val auditLogsJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                integrationSyncManager.getAuditLogsStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { logs ->
                        _auditLogs.value = logs
                    }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 20. Stream Redeem Code Vault (/redeem_codes & /redeem_claims)
        val redeemCodesJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                integrationSyncManager.getRedeemCodesStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { list -> _redeemCodes.value = list }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        val redeemClaimsJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                integrationSyncManager.getRedeemClaimsStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { list -> _redeemClaims.value = list }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 21. Stream Unified 12-Channel Request Hub (/admin_requests/all, /ban_appeals, /appeals, /support_tickets, /reports)
        val unifiedRequestsJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                integrationSyncManager.getUnifiedAdminRequestsStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { list ->
                        _unifiedRequests.value = list
                        // Automatically enrich or synthesize banned accounts from incoming Ban Appeals into Players repository
                        val appeals = list.filter { it.isBanAppeal }
                        if (appeals.isNotEmpty()) {
                            _uiState.update { curr ->
                                val s = curr as? DashboardState.Success ?: return@update curr
                                val mutableUsers = s.users.toMutableList()
                                var changed = false
                                appeals.forEach { req ->
                                    val reqUid = req.userId.trim()
                                    val reqEmail = req.userEmail.trim().lowercase()
                                    val reqIgn = req.inGameName.trim().lowercase()
                                    val reqName = req.username.trim().lowercase()
                                    val reqCase = req.relatedId.trim().uppercase()
                                    val isStillOpen = !req.status.equals("RESOLVED", true) && !req.status.equals("APPROVED", true)
                                    val idx = mutableUsers.indexOfFirst { u ->
                                        (reqUid.isNotBlank() && u.id.equals(reqUid, ignoreCase = true)) ||
                                            (reqEmail.contains("@") && u.email.trim().lowercase() == reqEmail) ||
                                            (reqCase.isNotBlank() && u.banCaseId.trim().uppercase() == reqCase) ||
                                            (reqIgn.isNotBlank() && reqIgn != "player" && (u.ign.trim().lowercase() == reqIgn || u.username.trim().lowercase() == reqIgn)) ||
                                            (reqName.isNotBlank() && reqName != "player" && (u.username.trim().lowercase() == reqName || u.ign.trim().lowercase() == reqName))
                                    }
                                    if (idx >= 0) {
                                        val existing = mutableUsers[idx]
                                        val locked = lockedUserStates[existing.id]?.takeIf { System.currentTimeMillis() - it.second < 25_000L }?.first
                                        val effectiveBanned = locked?.isBanned ?: (existing.isBanned || isStillOpen)
                                        val enriched = existing.copy(
                                            isBanned = effectiveBanned,
                                            banCaseId = existing.banCaseId.ifBlank { req.relatedId },
                                            banReason = existing.banReason.ifBlank { req.description.ifBlank { req.subject } },
                                            activeAppealTicketId = req.id.ifBlank { existing.activeAppealTicketId },
                                            activeAppealStatus = req.status.ifBlank { existing.activeAppealStatus }
                                        )
                                        if (enriched != existing) {
                                            mutableUsers[idx] = enriched
                                            changed = true
                                        }
                                    } else {
                                        val synthId = reqUid.ifBlank {
                                            if (reqCase.isNotBlank()) "appeal_${reqCase.lowercase()}"
                                            else "appeal_${req.id.lowercase()}"
                                        }
                                        val locked = lockedUserStates[synthId]?.takeIf { System.currentTimeMillis() - it.second < 25_000L }?.first
                                        if (!lockedDeletedUserIds.contains(synthId)) {
                                            mutableUsers.add(
                                                0,
                                                locked ?: UserProfile(
                                                    id = synthId,
                                                    username = req.username.ifBlank { req.inGameName.ifBlank { "Banned Player" } },
                                                    ign = req.inGameName.ifBlank { req.username },
                                                    email = if (req.userEmail.contains("@")) req.userEmail else "",
                                                    phone = if (!req.userEmail.contains("@") && req.userEmail.any { it.isDigit() }) req.userEmail else "",
                                                    gameId = req.freeFireId,
                                                    isBanned = isStillOpen,
                                                    status = if (isStillOpen) "BANNED" else "ACTIVE",
                                                    accountStatus = if (isStillOpen) "BANNED" else "ACTIVE",
                                                    banReason = req.description.ifBlank { req.subject.ifBlank { "Violation of Fair Play Terms" } },
                                                    banCaseId = req.relatedId,
                                                    activeAppealTicketId = req.id,
                                                    activeAppealStatus = req.status,
                                                    bannedAt = req.createdAt,
                                                    createdAt = req.createdAt,
                                                    lastActive = req.updatedAt
                                                )
                                            )
                                            changed = true
                                        }
                                    }
                                }
                                if (changed) s.withDerivedMetrics(newUsers = mutableUsers) else s
                            }
                        }
                    }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 22. Stream Chat Reports Queue (/chat_reports)
        val chatReportsJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                integrationSyncManager.getChatReportsStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { list -> _chatReports.value = list }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 23. Stream Unified Dual Withdrawals (/withdraw_requests, /wallet_requests, /payout_requests)
        val unifiedWithdrawalsJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                integrationSyncManager.getUnifiedWithdrawalsStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { list -> _unifiedWithdrawals.value = list }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 24. Stream Pre-Verified Bank Deposits (/verified_bank_deposits)
        val verifiedBankDepositsJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                integrationSyncManager.getVerifiedBankDepositsStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { list -> _verifiedBankDeposits.value = list }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 25. Stream Manual Notification History (Firestore manual_notifications)
        val manualNotificationsJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                integrationSyncManager.getManualNotificationsStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { list -> _manualNotifications.value = list }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        // 26. Stream Security Audit Logs (Firestore security_audit_logs)
        val securityAuditLogsJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                integrationSyncManager.getSecurityAuditLogsStream()
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { list -> _securityAuditLogs.value = list }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }

        startSelectedChannelStream(_selectedAdminChannel.value)

        synchronized(activeStreamJobs) {
            activeStreamJobs.addAll(
                listOf(
                    userJob, ticketsJob, payoutsJob, adminsJob, bannedJob, tournamentsJob,
                    tokensJob, bannersJob, annJob, proofsJob, notifsJob, categoriesJob,
                    deletionRequestsJob, dataExportsJob, depositsJob, tokenConvJob, missionsJob, sysConfigJob, auditLogsJob,
                    redeemCodesJob, redeemClaimsJob, unifiedRequestsJob, chatReportsJob, unifiedWithdrawalsJob, verifiedBankDepositsJob,
                    manualNotificationsJob, securityAuditLogsJob
                )
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

    fun createTournamentsBatch(tournaments: List<Tournament>) {
        if (tournaments.isEmpty()) return
        _uiState.update { curr ->
            val s = curr as? DashboardState.Success ?: return@update curr
            val newIds = tournaments.map { it.id }.toSet()
            val updatedList = tournaments + s.tournaments.filter { it.id !in newIds }
            s.withDerivedMetrics(newTournaments = updatedList)
        }
        viewModelScope.launch(Dispatchers.IO) {
            val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            val currAdminUid = repository.auth.currentUser?.uid
            var saved = 0
            for (t in tournaments) {
                try {
                    repository.createTournament(t, currAdminEmail, currAdminUid)
                    saved++
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            GlobalErrorManager.emitSuccess("Published $saved tournaments across categories to Firebase!")
        }
    }

    fun duplicateTournament(tournament: Tournament) {
        val newId = com.example.data.validation.SecuritySanitizer.sanitizeDatabaseKey(java.util.UUID.randomUUID().toString())
        val duplicated = tournament.copy(
            id = newId,
            title = "${tournament.title} (Clone)",
            registeredPlayers = 0,
            status = "UPCOMING"
        )
        createTournament(duplicated)
        GlobalErrorManager.emitSuccess("Duplicated '${tournament.title}' successfully!")
    }

    fun saveCategory(category: com.example.domain.model.TournamentCategory) {
        _uiState.update { curr ->
            val s = curr as? DashboardState.Success ?: return@update curr
            val updatedCategories = listOf(category) + s.categories.filter { it.id != category.id }
            s.withDerivedMetrics(newCategories = updatedCategories)
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.saveCategory(category)
            } catch (e: Exception) {
                GlobalErrorManager.emitError("Failed to save category: ${e.message}")
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        _uiState.update { curr ->
            val s = curr as? DashboardState.Success ?: return@update curr
            val updatedCategories = s.categories.filter { it.id != categoryId }
            s.withDerivedMetrics(newCategories = updatedCategories)
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteCategory(categoryId)
            } catch (e: Exception) {
                GlobalErrorManager.emitError("Failed to delete category: ${e.message}")
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
        val currentState = _uiState.value as? DashboardState.Success
        val targetTournament = currentState?.tournaments?.find { it.id == tournamentId }
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
                if (targetTournament != null) {
                    val allUsers = (_uiState.value as? DashboardState.Success)?.users ?: emptyList()
                    integrationSyncManager.cancelTournamentAndAutoRefund(
                        tournament = targetTournament,
                        reason = reason,
                        allUsers = allUsers,
                        adminEmail = currAdminEmail
                    )
                }
                repository.cancelTournament(tournamentId, reason, currAdminEmail, currAdminUid)
            } catch (e: Exception) {
                GlobalErrorManager.emitFirestoreError("Cancel Tournament", e)
            }
        }
    }

    fun deleteTournament(tournamentId: String) {
        repository.markTournamentDeleted(tournamentId)
        _uiState.update { curr ->
            val s = curr as? DashboardState.Success ?: return@update curr
            val updatedList = s.tournaments.filter {
                it.id != tournamentId &&
                    it.id !in repository.locallyDeletedTournamentIds &&
                    !repository.isPhantomOrDeletedTournament(
                        tId = it.id,
                        rawTitle = it.title,
                        entryFee = it.entryFee,
                        prizePool = it.prizePool,
                        roomId = it.roomDetails?.roomId,
                        roomPass = it.roomDetails?.roomPassword
                    )
            }
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
        if (newBanned && user.isGuestSession) {
            GlobalErrorManager.emitError("Guest/anonymous sessions cannot be banned globally.")
            return
        }
        val reason = if (newBanned) user.banReason.ifBlank { "Violation of Fair Play Terms / Anti-Cheat & Third-Party Tool Detection" } else ""
        val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
        val now = System.currentTimeMillis()
        val optimisticUpdated = user.copy(
            isBanned = newBanned,
            status = if (newBanned) "BANNED" else "ACTIVE",
            accountStatus = if (newBanned) "BANNED" else "ACTIVE",
            banReason = reason,
            banType = if (newBanned) "PERMANENT" else "",
            banCaseId = if (newBanned) user.banCaseId.ifBlank { "BAN-${user.id.take(6).uppercase()}" } else "",
            bannedAt = if (newBanned) now else 0L,
            isSuspended = if (!newBanned) false else user.isSuspended,
            suspendedUntil = if (!newBanned) 0L else user.suspendedUntil,
            activeAppealStatus = if (!newBanned) "RESOLVED" else user.activeAppealStatus,
            version = (user.version + 1).coerceAtLeast(1),
            updatedAt = now
        )

        // Apply optimistic lock + immediate UI update so tapping Unban/Ban responds instantly without reverting
        lockedUserStates[user.id] = optimisticUpdated to now
        repository.lastExtractedUsers[user.id] = optimisticUpdated
        _uiState.update { curr ->
            val s = curr as? DashboardState.Success ?: return@update curr
            s.withDerivedMetrics(users = s.users.map { if (it.id == user.id) optimisticUpdated else it })
        }
        if (!newBanned) {
            _unifiedRequests.update { list ->
                list.map { req ->
                    val matchesUser = req.isBanAppeal && (
                        req.userId.equals(user.id, true) ||
                        (user.activeAppealTicketId.isNotBlank() && req.id.equals(user.activeAppealTicketId, true)) ||
                        (user.banCaseId.isNotBlank() && req.relatedId.equals(user.banCaseId, true)) ||
                        (user.email.isNotBlank() && req.userEmail.equals(user.email, true)) ||
                        (user.ign.isNotBlank() && req.inGameName.equals(user.ign, true))
                    )
                    if (matchesUser) req.copy(status = "RESOLVED", updatedAt = now) else req
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val syncOk = try {
                integrationSyncManager.setUserBanStatus(user, newBanned, reason, currAdminEmail)
            } catch (_: Exception) { false }

            try {
                if (newBanned) {
                    repository.banPlayer(user.id, user.email, reason, currAdminEmail, user.gameId)
                } else {
                    repository.unbanPlayer(user.id, user.email, user.gameId)
                }
            } catch (_: Exception) {}

            val result = userRoleManager.toggleUserBan(user, newBanned, reason, currAdminEmail)
            val confirmed = result.getOrNull() ?: if (syncOk) optimisticUpdated else null
            if (confirmed != null) {
                lockedUserStates[user.id] = confirmed to System.currentTimeMillis()
                repository.lastExtractedUsers[user.id] = confirmed
                _uiState.update { curr ->
                    val s = curr as? DashboardState.Success ?: return@update curr
                    s.withDerivedMetrics(users = s.users.map { if (it.id == user.id) confirmed else it })
                }
                if (newBanned) {
                    GlobalErrorManager.emitSuccess("Player ${user.username} (${user.id}) banned across RTDB & Firestore!")
                } else {
                    GlobalErrorManager.emitSuccess("Player ${user.username} (${user.id}) unbanned & synced across all databases!")
                }
            } else {
                val err = result.exceptionOrNull()
                err?.printStackTrace()
                lockedUserStates.remove(user.id)
                GlobalErrorManager.emitError("Failed to toggle ban: ${err?.message ?: "Unknown error"}")
            }
        }
    }

    fun approveBanAppealAndUnban(req: UnifiedAdminRequest, adminNote: String = "") {
        val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
        val now = System.currentTimeMillis()
        val matchedUser = (_uiState.value as? DashboardState.Success)?.users?.firstOrNull { u ->
            (req.userId.isNotBlank() && u.id.equals(req.userId, true)) ||
                (req.relatedId.isNotBlank() && u.banCaseId.equals(req.relatedId, true)) ||
                (req.userEmail.contains("@") && u.email.equals(req.userEmail, true)) ||
                (req.inGameName.isNotBlank() && (u.ign.equals(req.inGameName, true) || u.username.equals(req.inGameName, true))) ||
                (req.username.isNotBlank() && req.username != "Player" && (u.username.equals(req.username, true) || u.ign.equals(req.username, true)))
        }

        // Optimistic UI update for both Players list and Ban Appeals queue
        if (matchedUser != null) {
            val unbanned = matchedUser.copy(
                isBanned = false,
                status = "ACTIVE",
                accountStatus = "ACTIVE",
                banReason = "",
                banType = "",
                banCaseId = "",
                bannedAt = 0L,
                isSuspended = false,
                suspendedUntil = 0L,
                activeAppealStatus = "RESOLVED",
                updatedAt = now
            )
            lockedUserStates[matchedUser.id] = unbanned to now
            repository.lastExtractedUsers[matchedUser.id] = unbanned
            _uiState.update { curr ->
                val s = curr as? DashboardState.Success ?: return@update curr
                s.withDerivedMetrics(users = s.users.map { if (it.id == matchedUser.id) unbanned else it })
            }
        }
        _unifiedRequests.update { list ->
            list.map { item ->
                if (item.id == req.id) item.copy(
                    status = "RESOLVED",
                    adminReply = adminNote.ifBlank { "Ban appeal APPROVED & account UNBANNED by Admin." },
                    updatedAt = now
                ) else item
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            integrationSyncManager.approveBanAppealAndUnban(
                req = req,
                matchedUser = matchedUser,
                adminNote = adminNote,
                adminEmail = currAdminEmail
            )
            val uidToUnban = matchedUser?.id ?: req.userId
            if (uidToUnban.isNotBlank()) {
                try {
                    repository.unbanPlayer(
                        uid = uidToUnban,
                        email = matchedUser?.email ?: req.userEmail,
                        gameId = matchedUser?.gameId ?: req.freeFireId
                    )
                } catch (_: Exception) {}
            }
        }
    }

    fun rejectBanAppeal(req: UnifiedAdminRequest, adminNote: String = "") {
        val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
        val now = System.currentTimeMillis()
        _unifiedRequests.update { list ->
            list.map { item ->
                if (item.id == req.id) item.copy(
                    status = "REJECTED",
                    adminReply = adminNote.ifBlank { "Ban appeal REJECTED due to confirmed Fair Play violation." },
                    updatedAt = now
                ) else item
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            integrationSyncManager.rejectBanAppeal(
                req = req,
                adminNote = adminNote,
                adminEmail = currAdminEmail
            )
        }
    }

    fun unbanAllGuestSessions(onFinished: ((Int) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = repository.unbanAllGuestAndNonLoggedInSessions()
            fetchData()
            withContext(Dispatchers.Main) {
                onFinished?.invoke(count)
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

    fun addFundsToUser(
        user: UserProfile,
        amount: Double,
        bucket: com.example.domain.model.WalletBucket = com.example.domain.model.WalletBucket.DEPOSIT,
        reason: String = "Fund adjustment by admin"
    ) {
        val currentStateUsers = (_uiState.value as? DashboardState.Success)?.users ?: emptyList()
        val latestUser = lockedUserStates[user.id]?.first
            ?: currentStateUsers.find { it.id == user.id }
            ?: user

        val newDeposit = when (bucket) {
            com.example.domain.model.WalletBucket.DEPOSIT -> (latestUser.depositFunds + amount).coerceAtLeast(0.0)
            else -> latestUser.depositFunds.coerceAtLeast(0.0)
        }
        val newWinning = when (bucket) {
            com.example.domain.model.WalletBucket.WINNING -> (latestUser.winningFunds + amount).coerceAtLeast(0.0)
            else -> latestUser.winningFunds.coerceAtLeast(0.0)
        }
        val newBonus = when (bucket) {
            com.example.domain.model.WalletBucket.BONUS -> (latestUser.bonusFunds + amount).coerceAtLeast(0.0)
            else -> latestUser.bonusFunds.coerceAtLeast(0.0)
        }
        val newReferralBonus = when (bucket) {
            com.example.domain.model.WalletBucket.BONUS -> if (amount > 0) maxOf(latestUser.referralBonusEarned, latestUser.bonusFunds) + amount else newBonus
            else -> maxOf(latestUser.referralBonusEarned, newBonus)
        }
        val newTotal = newDeposit + newWinning + newBonus
        val now = System.currentTimeMillis()
        val updatedUser = latestUser.copy(
            funds = newTotal,
            balance = newTotal,
            depositFunds = newDeposit,
            winningFunds = newWinning,
            bonusFunds = newBonus,
            referralBonusEarned = newReferralBonus,
            totalDeposited = if (bucket == com.example.domain.model.WalletBucket.DEPOSIT && amount > 0) latestUser.totalDeposited + amount else latestUser.totalDeposited,
            totalEarnings = if (bucket == com.example.domain.model.WalletBucket.WINNING && amount > 0) latestUser.totalEarnings + amount else latestUser.totalEarnings,
            lastActive = now
        )
        lockedUserStates[user.id] = updatedUser to now
        repository.recentAdminWalletOverrides[user.id] = updatedUser to now
        _uiState.update { curr ->
            val s = curr as? DashboardState.Success ?: return@update curr
            s.withDerivedMetrics(users = s.users.map { if (it.id == user.id) updatedUser else it })
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
                val syncedUser = repository.adjustUserWalletByBucket(
                    user = latestUser,
                    deltaAmount = amount,
                    bucket = bucket,
                    reason = reason,
                    adminEmail = currAdminEmail
                )
                if (syncedUser != null) {
                    val syncTime = System.currentTimeMillis()
                    lockedUserStates[user.id] = syncedUser to syncTime
                    _uiState.update { curr ->
                        val s = curr as? DashboardState.Success ?: return@update curr
                        s.withDerivedMetrics(users = s.users.map { if (it.id == user.id) syncedUser else it })
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                GlobalErrorManager.emitError("Failed to adjust wallet: ${e.message}")
            }
        }
    }

    fun recordPlayerMatchWinning(
        user: UserProfile,
        tournamentId: String,
        tournamentTitle: String,
        kills: Int,
        rank: Int,
        winningAmount: Double,
        entryFeePaid: Double
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currAdminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
                val updatedUser = repository.recordPlayerMatchStatsAndWinning(
                    user = user,
                    tournamentId = tournamentId,
                    tournamentTitle = tournamentTitle,
                    kills = kills,
                    rank = rank,
                    winningAmount = winningAmount,
                    entryFeePaid = entryFeePaid,
                    adminEmail = currAdminEmail
                )
                if (updatedUser != null) {
                    lockedUserStates[user.id] = updatedUser to System.currentTimeMillis()
                    _uiState.update { curr ->
                        val s = curr as? DashboardState.Success ?: return@update curr
                        s.withDerivedMetrics(users = s.users.map { if (it.id == user.id) updatedUser else it })
                    }
                }
                GlobalErrorManager.emitSuccess("Match stats & ₹${"%.0f".format(winningAmount)} Winning Balance updated for ${user.username}")
            } catch (e: Exception) {
                e.printStackTrace()
                GlobalErrorManager.emitError("Failed to record match stats: ${e.message}")
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
        bucket: com.example.domain.model.WalletBucket = com.example.domain.model.WalletBucket.DEPOSIT,
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
                    val updatedUser = repository.adjustUserWalletByBucket(
                        user = targetUser,
                        deltaAmount = amount,
                        bucket = bucket,
                        reason = reason,
                        adminEmail = adminEmail
                    )
                    if (updatedUser != null) {
                        lockedUserStates[targetUser.id] = updatedUser to System.currentTimeMillis()
                        _uiState.update { current ->
                            val s = current as? DashboardState.Success ?: return@update current
                            s.withDerivedMetrics(users = s.users.map { if (it.id == targetUser.id) updatedUser else it })
                        }
                    }
                } else if (ticket.userEmail.isNotBlank()) {
                    val fallbackId = "USR_${ticket.userEmail.hashCode()}"
                    val fallbackUser = UserProfile(id = fallbackId, email = ticket.userEmail, username = ticket.userEmail.substringBefore("@"))
                    repository.adjustUserWalletByBucket(fallbackUser, amount, bucket, reason, adminEmail)
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
            com.example.notification.VelorixFcmManager.dispatchTestPushToUserPanel(liveTourney)
            GlobalErrorManager.emitSuccess("Test push dispatched to User Panel (com.tournaments.velorix)!")
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

    // =========================================================================
    // ADMIN INTEGRATION MASTER PROMPT ACTIONS (10 CHANNELS, DPDP, DEPOSITS, TOKENS, SYSTEM CONFIG, CHAT)
    // =========================================================================

    fun selectAdminChannel(channel: AdminChannelType) {
        _selectedAdminChannel.value = channel
        startSelectedChannelStream(channel)
    }

    private fun startSelectedChannelStream(channel: AdminChannelType) {
        activeChannelStreamJob?.cancel()
        activeChannelStreamJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                integrationSyncManager.getAdminChannelStream(channel.channelKey)
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Default)
                    .collect { items ->
                        _activeChannelItems.value = items
                    }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace()
            }
        }
    }

    fun updateAdminChannelItemStatus(channelType: String, itemId: String, status: String, note: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            integrationSyncManager.updateAdminChannelItemStatus(channelType, itemId, status, note, adminEmail)
        }
    }

    fun pushTestSituationEvent(situationLabel: String = "ADMIN_SITUATION_VERIFICATION") {
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            integrationSyncManager.pushTestSituationEventToChannels(adminEmail)
        }
    }

    fun approveAccountDeletion(request: AccountDeletionRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            lockedDeletedUserIds.add(request.userId)
            integrationSyncManager.approveAndPermanentlyDeleteUser(request, adminEmail)
            _uiState.update { curr ->
                val s = curr as? DashboardState.Success ?: return@update curr
                s.withDerivedMetrics(newUsers = s.users.filter { it.id != request.userId })
            }
        }
    }

    fun restoreAccountDeletion(request: AccountDeletionRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            integrationSyncManager.rejectAndRestoreUserAccount(request, adminEmail)
        }
    }

    fun generateUserDpdpExport(user: UserProfile, onComplete: (DataExportRequest?) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val record = integrationSyncManager.generateAndArchiveUserDataExport(user)
            withContext(Dispatchers.Main) {
                onComplete(record)
            }
        }
    }

    fun resetUserAvatarToDefault(user: UserProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            integrationSyncManager.resetUserAvatarToDefault(user, adminEmail)
            val updated = user.copy(
                avatarUrl = "avatar_1",
                version = user.version + 1,
                updatedAt = System.currentTimeMillis()
            )
            lockedUserStates[user.id] = Pair(updated, System.currentTimeMillis())
            _uiState.update { curr ->
                val s = curr as? DashboardState.Success ?: return@update curr
                s.withDerivedMetrics(newUsers = s.users.map { if (it.id == user.id) updated else it })
            }
        }
    }

    fun adjustUserTokens(user: UserProfile, tokenDelta: Int, reason: String) {
        if (tokenDelta == 0) return
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            integrationSyncManager.adjustUserTokens(user, tokenDelta, reason, adminEmail)
            val newTokens = (user.tokens + tokenDelta).coerceAtLeast(0)
            val updated = user.copy(
                tokens = newTokens,
                version = user.version + 1,
                updatedAt = System.currentTimeMillis()
            )
            lockedUserStates[user.id] = Pair(updated, System.currentTimeMillis())
            _uiState.update { curr ->
                val s = curr as? DashboardState.Success ?: return@update curr
                s.withDerivedMetrics(newUsers = s.users.map { if (it.id == user.id) updated else it })
            }
        }
    }

    fun approveDepositRequest(request: DepositRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            integrationSyncManager.approveDepositRequestFull(request, adminEmail)
        }
    }

    fun rejectDepositRequest(request: DepositRequest, reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            integrationSyncManager.rejectDepositRequestFull(request, reason, adminEmail)
        }
    }

    fun preVerifyBankUtr(utr: String, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            integrationSyncManager.preVerifyBankUtr(utr, amount, adminEmail)
        }
    }

    fun approveUpiWithdrawal(request: PayoutRequest, payoutUtrOrNote: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            integrationSyncManager.approveUpiWithdrawalWithTicketSync(request, payoutUtrOrNote, adminEmail)
        }
    }

    fun approveRedeemVoucherWithdrawal(request: PayoutRequest, redeemCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            integrationSyncManager.approveRedeemVoucherWithdrawal(request, redeemCode, adminEmail)
        }
    }

    fun rejectWithdrawalAndRefund(request: PayoutRequest, reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            integrationSyncManager.rejectWithdrawalAndRefundWithTicketSync(request, reason, adminEmail)
        }
    }

    fun saveRedeemCode(item: RedeemCodeItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            integrationSyncManager.saveRedeemCode(item, adminEmail)
        }
    }

    fun toggleRedeemCodeActive(code: String, active: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            integrationSyncManager.toggleRedeemCodeActive(code, active, adminEmail)
        }
    }

    fun deleteRedeemCode(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            integrationSyncManager.deleteRedeemCode(code, adminEmail)
        }
    }

    fun replyAndUpdateUnifiedTicket(
        req: UnifiedAdminRequest,
        newStatus: String,
        adminReply: String,
        deliveredRedeemCode: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            integrationSyncManager.replyAndUpdateUnifiedTicket(req, newStatus, adminReply, deliveredRedeemCode, adminEmail)
        }
    }

    fun resolveChatReportAndDeleteMessage(report: ChatReportItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            if (report.tournamentId.isNotBlank() && report.messageId.isNotBlank()) {
                integrationSyncManager.deleteTournamentChatMessage(report.tournamentId, report.messageId)
            }
            integrationSyncManager.resolveChatReport(report, "MESSAGE_SOFT_DELETED", adminEmail)
        }
    }

    fun resolveChatReportAndBanSender(report: ChatReportItem, reason: String = "Abusive / Toxic Lobby Chat") {
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            if (report.tournamentId.isNotBlank() && report.messageId.isNotBlank()) {
                integrationSyncManager.deleteTournamentChatMessage(report.tournamentId, report.messageId)
            }
            if (report.reportedUserId.isNotBlank()) {
                val matchedUser = (_uiState.value as? DashboardState.Success)?.users?.find { it.id == report.reportedUserId }
                    ?: UserProfile(id = report.reportedUserId, username = report.reportedUserName.ifBlank { "Player" })
                integrationSyncManager.setUserBanStatus(matchedUser, true, reason, adminEmail)
            }
            integrationSyncManager.resolveChatReport(report, "SENDER_BANNED_AND_DELETED", adminEmail)
        }
    }

    fun toggleMasterShowBanners(enabled: Boolean) {
        val current = _systemConfig.value
        saveSystemConfig(current.copy(showBanners = enabled, updatedAt = System.currentTimeMillis()))
    }

    fun saveMission(mission: MissionItem) {
        viewModelScope.launch(Dispatchers.IO) {
            integrationSyncManager.saveMission(mission)
        }
    }

    fun deleteMission(missionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            integrationSyncManager.deleteMission(missionId)
        }
    }

    fun saveSystemConfig(config: SystemConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            integrationSyncManager.saveSystemConfig(config, adminEmail)
            _systemConfig.value = config.copy(updatedAt = System.currentTimeMillis())
        }
    }

    fun savePromotionalBanner(banner: AppAnnouncementBanner) {
        viewModelScope.launch(Dispatchers.IO) {
            integrationSyncManager.savePromotionalBanner(banner)
        }
    }

    fun deletePromotionalBanner(bannerId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            integrationSyncManager.deletePromotionalBanner(bannerId)
        }
    }

    fun sendTargetedNotification(userId: String, title: String, message: String, type: String = "SYSTEM") {
        viewModelScope.launch(Dispatchers.IO) {
            integrationSyncManager.sendTargetedUserNotification(userId, title, message, type)
            GlobalErrorManager.emitSuccess("Notification [$type] delivered to /notifications/$userId in RTDB & Firestore.")
        }
    }

    fun sendGlobalBroadcastNotification(title: String, message: String, type: String = "ANNOUNCEMENT") {
        sendManualNotification(
            title = title,
            message = message,
            type = type,
            targetType = "ALL"
        )
    }

    fun sendManualNotification(
        title: String,
        message: String,
        type: String = "MANUAL",
        targetType: String = "ALL",
        targetUserId: String = "",
        targetEmail: String = "",
        targetUsername: String = "",
        targetUserIds: List<String> = emptyList(),
        tournamentId: String = "",
        topic: String = "",
        imageUrl: String = "",
        roomId: String = "",
        roomPassword: String = "",
        onComplete: (ManualNotificationResult) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            val result = integrationSyncManager.sendManualNotification(
                title = title,
                message = message,
                type = type,
                targetType = targetType,
                targetUserId = targetUserId,
                targetEmail = targetEmail,
                targetUsername = targetUsername,
                targetUserIds = targetUserIds,
                tournamentId = tournamentId,
                topic = topic,
                imageUrl = imageUrl,
                roomId = roomId,
                roomPassword = roomPassword,
                adminEmail = adminEmail
            )
            withContext(Dispatchers.Main) {
                onComplete(result)
            }
        }
    }

    fun triggerBackendReconciliation() {
        if (_isReconcilingBackend.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isReconcilingBackend.value = true
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            val res = integrationSyncManager.triggerBackendReconciliation(adminEmail)
            res.onSuccess { json ->
                _reconciliationSummaryJson.value = json
            }.onFailure { err ->
                _reconciliationSummaryJson.value = "{\n  \"error\": \"${err.message?.replace("\"", "'") ?: "Unknown error"}\"\n}"
            }
            _isReconcilingBackend.value = false
        }
    }

    fun approveAccountDeletionFromUnifiedTicket(req: UnifiedAdminRequest, adminNote: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            val ok = integrationSyncManager.approveAccountDeletionFromUnifiedTicket(req, adminNote, adminEmail)
            if (ok && req.userId.isNotBlank()) {
                lockedDeletedUserIds.add(req.userId)
                _uiState.update { curr ->
                    val s = curr as? DashboardState.Success ?: return@update curr
                    s.withDerivedMetrics(newUsers = s.users.filter { it.id != req.userId })
                }
            }
        }
    }

    fun getTournamentChatStream(tournamentId: String): Flow<List<TournamentChatMessage>> {
        return integrationSyncManager.getTournamentChatStream(tournamentId)
    }

    fun sendAdminTournamentChat(tournamentId: String, message: String, isPinned: Boolean = false) {
        if (message.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            integrationSyncManager.sendAdminTournamentChatMessage(
                tournamentId = tournamentId,
                message = message,
                isPinned = isPinned,
                adminName = "VeloRix Admin"
            )
        }
    }

    fun togglePinTournamentChat(tournamentId: String, messageId: String, isPinned: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            integrationSyncManager.togglePinTournamentChatMessage(tournamentId, messageId, isPinned)
        }
    }

    fun deleteTournamentChat(tournamentId: String, messageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            integrationSyncManager.deleteTournamentChatMessage(tournamentId, messageId)
        }
    }

    // =========================================================================
    // MODULE 1-4: MULTI-POSITION STANDINGS, CANCELLATION AUTO-REFUND & MODERATION
    // =========================================================================

    fun loadTournamentStandings(
        tournament: Tournament,
        onLoaded: (List<TournamentStandingEntry>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val allUsers = (_uiState.value as? DashboardState.Success)?.users ?: emptyList()
            val list = integrationSyncManager.loadTournamentParticipantsForStandings(tournament, allUsers)
            withContext(Dispatchers.Main) {
                onLoaded(list)
            }
        }
    }

    fun publishTournamentStandings(
        tournament: Tournament,
        standings: List<TournamentStandingEntry>,
        tiePrizeMode: String = "SPLIT_TIED_POOL",
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            val ok = integrationSyncManager.publishTournamentStandingsAndPrizes(
                tournament = tournament,
                rawStandings = standings,
                adminEmail = adminEmail,
                tiePrizeMode = tiePrizeMode
            )
            if (ok) {
                val sorted = standings.sortedBy { it.rank.coerceAtLeast(1) }
                val totalDisbursed = sorted.sumOf { (it.rankPrize + it.killPrize).coerceAtLeast(0.0) }
                val top1 = sorted.firstOrNull { it.rank == 1 } ?: sorted.firstOrNull()
                _uiState.update { curr ->
                    val s = curr as? DashboardState.Success ?: return@update curr
                    val updatedTourneys = s.tournaments.map { t ->
                        if (t.id == tournament.id) {
                            t.copy(
                                status = "COMPLETED",
                                isCompleted = true,
                                payoutStatus = "PAID",
                                completedAt = System.currentTimeMillis(),
                                totalPrizeDisbursed = totalDisbursed,
                                winnerUid = top1?.userId ?: t.winnerUid,
                                winnerName = top1?.inGameName ?: t.winnerName,
                                winnerPrize = top1?.let { it.rankPrize + it.killPrize } ?: t.winnerPrize,
                                finalLeaderboard = sorted,
                                standings = sorted
                            )
                        } else t
                    }
                    s.withDerivedMetrics(newTournaments = updatedTourneys)
                }
            }
            withContext(Dispatchers.Main) {
                onComplete(ok)
            }
        }
    }

    fun cancelTournamentWithAutoRefund(
        tournament: Tournament,
        reason: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            val allUsers = (_uiState.value as? DashboardState.Success)?.users ?: emptyList()
            val ok = integrationSyncManager.cancelTournamentAndAutoRefund(
                tournament = tournament,
                reason = reason.ifBlank { "Cancelled by Admin" },
                allUsers = allUsers,
                adminEmail = adminEmail
            )
            if (ok) {
                _uiState.update { curr ->
                    val s = curr as? DashboardState.Success ?: return@update curr
                    val updatedTourneys = s.tournaments.map { t ->
                        if (t.id == tournament.id) t.copy(status = "CANCELLED") else t
                    }
                    s.withDerivedMetrics(newTournaments = updatedTourneys)
                }
            }
            withContext(Dispatchers.Main) {
                onComplete(ok)
            }
        }
    }

    fun setUserBanWithReason(user: UserProfile, isBanned: Boolean, reason: String) {
        val now = System.currentTimeMillis()
        val updated = user.copy(
            isBanned = isBanned,
            status = if (isBanned) "BANNED" else "ACTIVE",
            accountStatus = if (isBanned) "BANNED" else "ACTIVE",
            banReason = if (isBanned) reason.ifBlank { "Violation of Fair Play Policy" } else "",
            banType = if (isBanned) "PERMANENT" else "",
            banCaseId = if (isBanned) user.banCaseId.ifBlank { "BAN-${user.id.take(6).uppercase()}" } else "",
            bannedAt = if (isBanned) now else 0L,
            activeAppealStatus = if (!isBanned) "RESOLVED" else user.activeAppealStatus,
            version = user.version + 1,
            updatedAt = now
        )
        lockedUserStates[user.id] = Pair(updated, now)
        repository.lastExtractedUsers[user.id] = updated
        _uiState.update { curr ->
            val s = curr as? DashboardState.Success ?: return@update curr
            s.withDerivedMetrics(newUsers = s.users.map { if (it.id == user.id) updated else it })
        }
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            integrationSyncManager.setUserBanStatus(user, isBanned, reason, adminEmail)
            try {
                if (isBanned) {
                    repository.banPlayer(user.id, user.email, reason, adminEmail, user.gameId)
                } else {
                    repository.unbanPlayer(user.id, user.email, user.gameId)
                }
            } catch (_: Exception) {}
        }
    }

    fun setUserSuspendWithDuration(user: UserProfile, isSuspended: Boolean, reason: String, durationHours: Int = 24) {
        viewModelScope.launch(Dispatchers.IO) {
            val adminEmail = overrideUserEmail ?: repository.auth.currentUser?.email ?: "anantisback47@gmail.com"
            val ok = integrationSyncManager.setUserSuspendStatus(user, isSuspended, reason, durationHours, adminEmail)
            if (ok) {
                val now = System.currentTimeMillis()
                val until = if (isSuspended) now + (durationHours.coerceAtLeast(1) * 3600_000L) else 0L
                val updated = user.copy(
                    isSuspended = isSuspended,
                    status = if (isSuspended) "suspended" else "active",
                    suspendReason = if (isSuspended) reason.ifBlank { "Temporary Suspension" } else "",
                    suspendedUntil = until,
                    version = user.version + 1,
                    updatedAt = now
                )
                lockedUserStates[user.id] = Pair(updated, now)
                _uiState.update { curr ->
                    val s = curr as? DashboardState.Success ?: return@update curr
                    s.withDerivedMetrics(newUsers = s.users.map { if (it.id == user.id) updated else it })
                }
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

