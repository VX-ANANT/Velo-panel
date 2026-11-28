package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.*
import com.example.ui.common.GlobalErrorManager
import com.example.ui.viewmodel.DashboardState
import com.example.ui.viewmodel.TournamentDashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Pure AMOLED Black Palette with V14 Neutral Hairlines & YouTube Pinkish-Red Primary Accent
private val PureBlackBg = Color.Black
private val HairlineBorder = Color(0xFF1E1E27)
private val TextPrimaryWhite = Color(0xFFF5F5F7)
private val TextSecondaryZinc = Color(0xFF9A9AA6)
private val AccentBlue = Color(0xFFFF2E5B) // Signature YouTube Pinkish-Red Primary Accent
private val AccentGreen = Color(0xFF34D399)
private val AccentAmber = Color(0xFFFBBF24)
private val AccentRed = Color(0xFFFF4D6D)

@Composable
private fun AdminActionChip(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconOnly: Boolean = false
) {
    val resolvedIcon = icon ?: when {
        label.contains("Winner", true) || label.contains("Standing", true) -> Icons.Default.EmojiEvents
        label.contains("Room", true) || label.contains("Pass", true) -> Icons.Default.VpnKey
        label.contains("Detail", true) || label.contains("View", true) -> Icons.Default.Visibility
        label.contains("Edit", true) -> Icons.Default.Edit
        label.contains("Live", true) -> Icons.Default.PlayArrow
        label.contains("Cancel", true) || label.contains("Reject", true) -> Icons.Default.Close
        label.contains("Chat", true) -> Icons.Default.Forum
        label.contains("Delete", true) -> Icons.Default.DeleteOutline
        label.contains("Approve", true) || label.contains("Resolve", true) -> Icons.Default.Check
        label.contains("Unban", true) || label.contains("Restore", true) -> Icons.Default.CheckCircle
        label.contains("Ban", true) -> Icons.Default.Block
        label.contains("Suspend", true) -> Icons.Default.Timer
        label.contains("Avatar", true) -> Icons.Default.AccountCircle
        label.contains("Pin", true) -> Icons.Default.PushPin
        label.contains("Create", true) || label.contains("Add", true) || label.contains("Grant", true) -> Icons.Default.Add
        label.contains("Deduct", true) -> Icons.Default.Remove
        label.contains("Auto", true) -> Icons.Default.AutoFixHigh
        else -> null
    }

    val compactLabel = when {
        !iconOnly -> label
        label.contains("Declare Winners", true) -> "Winners"
        label.contains("Room ID", true) -> "Room"
        label.contains("Cancel & Refund", true) -> "Cancel"
        label.contains("Lobby Chat", true) -> "Chat"
        label.contains("Set Live", true) -> "Go Live"
        label.contains("Approve Deposit", true) || label.contains("Approve Payout", true) -> "Approve"
        label.contains("Reject UTR", true) || label.contains("Reject & Refund", true) -> "Reject"
        label.contains("Permanent Ban", true) -> "Ban"
        label.contains("Unban User", true) -> "Unban"
        label.startsWith("Suspend", true) -> "Suspend"
        label.contains("Reset Avatar", true) -> "Avatar"
        else -> label
    }

    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
            .border(0.75.dp, color.copy(alpha = 0.48f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (resolvedIcon != null) {
            Icon(
                imageVector = resolvedIcon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = compactLabel,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

enum class IntegrationHubSection(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    TOURNAMENTS_RESULTS("Matches", Icons.Default.EmojiEvents),
    WALLET_PAYOUTS("Wallet", Icons.Default.AccountBalanceWallet),
    REDEEM_VAULT("Redeem", Icons.Default.ConfirmationNumber),
    UNIFIED_REQUESTS("Requests", Icons.Default.SupportAgent),
    USERS_AND_CHAT("Players & Chat", Icons.Default.Group),
    BANNERS_BROADCASTS("Notify & Banners", Icons.Default.Campaign),
    SYSTEM_CONFIG_DPDP("Config & Logs", Icons.Default.Tune)
}

@Composable
fun AdminChannelsAndIntegrationHub(
    viewModel: TournamentDashboardViewModel,
    initialSection: IntegrationHubSection = IntegrationHubSection.TOURNAMENTS_RESULTS,
    initialTournamentIdForChat: String? = null,
    onCreateTournamentClick: (() -> Unit)? = null,
    onTournamentClick: ((String) -> Unit)? = null,
    onSettingsClick: ((String) -> Unit)? = null
) {
    var activeSection by remember(initialSection) { mutableStateOf(initialSection) }
    var usersChatSubTab by remember { mutableStateOf("USERS") } // USERS | CHAT
    var configDpdpSubTab by remember { mutableStateOf("CONFIG") } // CONFIG | LOGS | MISSIONS
    var tournamentForChat by remember(initialTournamentIdForChat) { mutableStateOf(initialTournamentIdForChat) }
    var tournamentForStandingsModal by remember { mutableStateOf<Tournament?>(null) }
    var prefillNotifyUserId by remember { mutableStateOf<String?>(null) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val successState = uiState as? DashboardState.Success

    val depositRequests by viewModel.depositRequests.collectAsStateWithLifecycle()
    val unifiedWithdrawals by viewModel.unifiedWithdrawals.collectAsStateWithLifecycle()
    val verifiedBankDeposits by viewModel.verifiedBankDeposits.collectAsStateWithLifecycle()
    val redeemCodes by viewModel.redeemCodes.collectAsStateWithLifecycle()
    val redeemClaims by viewModel.redeemClaims.collectAsStateWithLifecycle()
    val unifiedRequests by viewModel.unifiedRequests.collectAsStateWithLifecycle()
    val chatReports by viewModel.chatReports.collectAsStateWithLifecycle()
    val tokenConversions by viewModel.tokenConversions.collectAsStateWithLifecycle()
    val missions by viewModel.missions.collectAsStateWithLifecycle()
    val systemConfig by viewModel.systemConfig.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val securityAuditLogs by viewModel.securityAuditLogs.collectAsStateWithLifecycle()
    val manualNotifications by viewModel.manualNotifications.collectAsStateWithLifecycle()
    val reconciliationSummaryJson by viewModel.reconciliationSummaryJson.collectAsStateWithLifecycle()
    val isReconcilingBackend by viewModel.isReconcilingBackend.collectAsStateWithLifecycle()

    val mergedWithdrawals = remember(unifiedWithdrawals, successState?.payoutRequests) {
        val byId = LinkedHashMap<String, PayoutRequest>()
        unifiedWithdrawals.forEach { if (it.id.isNotBlank()) byId[it.id] = it }
        (successState?.payoutRequests ?: emptyList()).forEach {
            if (it.id.isNotBlank() && !byId.containsKey(it.id)) byId[it.id] = it
        }
        byId.values.sortedByDescending { it.requestedAt }
    }

    val pendingWalletCount = remember(depositRequests, mergedWithdrawals) {
        val dep = depositRequests.count { it.status.equals("PENDING", ignoreCase = true) }
        val wdr = mergedWithdrawals.count { !it.isDeposit && it.status.equals("PENDING", ignoreCase = true) }
        dep + wdr
    }
    val openRequestsCount = remember(unifiedRequests) {
        unifiedRequests.count {
            !it.status.equals("RESOLVED", true) && !it.status.equals("REJECTED", true)
        }
    }
    val pendingChatReportsCount = remember(chatReports) {
        chatReports.count { !it.status.equals("RESOLVED", true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlackBg)
    ) {
        // Clean Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Admin Hub",
                color = TextPrimaryWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(AccentGreen, CircleShape)
                )
                Text(
                    text = "Live",
                    color = AccentGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

        // Scrollable Pill Section Selector Row (7 Clean Admin Hub Modules)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IntegrationHubSection.entries.forEach { section ->
                val isSelected = activeSection == section
                val badgeCount = when (section) {
                    IntegrationHubSection.WALLET_PAYOUTS -> pendingWalletCount
                    IntegrationHubSection.UNIFIED_REQUESTS -> openRequestsCount
                    IntegrationHubSection.USERS_AND_CHAT -> pendingChatReportsCount
                    else -> 0
                }
                Row(
                    modifier = Modifier
                        .background(
                            if (isSelected) AccentBlue.copy(alpha = 0.16f) else Color(0xFF0A0A0D),
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            0.75.dp,
                            if (isSelected) AccentBlue else HairlineBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { activeSection = section }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = section.icon,
                        contentDescription = section.label,
                        tint = if (isSelected) AccentBlue else TextSecondaryZinc,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = section.label,
                        color = if (isSelected) TextPrimaryWhite else TextSecondaryZinc,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (badgeCount > 0) {
                        Text(
                            text = "$badgeCount",
                            color = AccentAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

        // Active Section Content
        when (activeSection) {
            IntegrationHubSection.TOURNAMENTS_RESULTS -> TournamentsAndStandingsModuleSection(
                viewModel = viewModel,
                tournaments = successState?.tournaments ?: emptyList(),
                onDeclareStandingsClick = { tournamentForStandingsModal = it },
                onOpenLobbyChat = { tId ->
                    tournamentForChat = tId
                    usersChatSubTab = "CHAT"
                    activeSection = IntegrationHubSection.USERS_AND_CHAT
                },
                onCreateTournamentClick = onCreateTournamentClick,
                onTournamentClick = onTournamentClick,
                onSettingsClick = onSettingsClick
            )
            IntegrationHubSection.WALLET_PAYOUTS -> WalletUpiVoucherModuleSection(
                viewModel = viewModel,
                depositRequests = depositRequests,
                withdrawals = mergedWithdrawals,
                verifiedUtrs = verifiedBankDeposits
            )
            IntegrationHubSection.REDEEM_VAULT -> RedeemCodeVaultModuleSection(
                viewModel = viewModel,
                redeemCodes = redeemCodes,
                redeemClaims = redeemClaims
            )
            IntegrationHubSection.UNIFIED_REQUESTS -> UnifiedRequestHubModuleSection(
                viewModel = viewModel,
                unifiedRequests = unifiedRequests
            )
            IntegrationHubSection.USERS_AND_CHAT -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "USERS" to "Players & Sanctions",
                            "CHAT" to if (pendingChatReportsCount > 0) "Lobby Chat & Reports ($pendingChatReportsCount)" else "Lobby Chat & Reports"
                        ).forEach { (key, title) ->
                            val sel = usersChatSubTab == key
                            Box(
                                modifier = Modifier
                                    .background(if (sel) AccentBlue.copy(alpha = 0.16f) else Color(0xFF0A0A0D), RoundedCornerShape(10.dp))
                                    .border(0.75.dp, if (sel) AccentBlue else HairlineBorder, RoundedCornerShape(10.dp))
                                    .clickable { usersChatSubTab = key }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = title,
                                    color = if (sel) TextPrimaryWhite else TextSecondaryZinc,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)
                    if (usersChatSubTab == "USERS") {
                        UsersAndModerationModuleSection(
                            viewModel = viewModel,
                            users = successState?.users ?: emptyList(),
                            onQuickNotifyUser = { uid ->
                                prefillNotifyUserId = uid
                                activeSection = IntegrationHubSection.BANNERS_BROADCASTS
                            }
                        )
                    } else {
                        TournamentLobbyChatModeratorSection(
                            viewModel = viewModel,
                            tournaments = successState?.tournaments ?: emptyList(),
                            initialTournamentId = tournamentForChat,
                            chatReports = chatReports
                        )
                    }
                }
            }
            IntegrationHubSection.BANNERS_BROADCASTS -> BannersAndBroadcastsModuleSection(
                viewModel = viewModel,
                systemConfig = systemConfig,
                banners = successState?.banners ?: emptyList(),
                users = successState?.users ?: emptyList(),
                tournaments = successState?.tournaments ?: emptyList(),
                manualNotifications = manualNotifications,
                prefillUserId = prefillNotifyUserId,
                onClearPrefillUser = { prefillNotifyUserId = null }
            )
            IntegrationHubSection.SYSTEM_CONFIG_DPDP -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "CONFIG" to "Live Config (5 Flags)",
                            "LOGS" to "Audit & Security Logs (${auditLogs.size + securityAuditLogs.size})",
                            "MISSIONS" to "Tokens & Missions"
                        ).forEach { (key, title) ->
                            val sel = configDpdpSubTab == key
                            Box(
                                modifier = Modifier
                                    .background(if (sel) AccentBlue.copy(alpha = 0.16f) else Color(0xFF0A0A0D), RoundedCornerShape(10.dp))
                                    .border(0.75.dp, if (sel) AccentBlue else HairlineBorder, RoundedCornerShape(10.dp))
                                    .clickable { configDpdpSubTab = key }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = title,
                                    color = if (sel) TextPrimaryWhite else TextSecondaryZinc,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)
                    when (configDpdpSubTab) {
                        "CONFIG" -> SystemConfigCleanSection(
                            viewModel = viewModel,
                            systemConfig = systemConfig,
                            reconciliationSummaryJson = reconciliationSummaryJson,
                            isReconcilingBackend = isReconcilingBackend
                        )
                        "LOGS" -> AuditAndSecurityLogsViewerSection(
                            auditLogs = auditLogs,
                            securityAuditLogs = securityAuditLogs
                        )
                        else -> TokensAndMissionsManagerSection(
                            viewModel = viewModel,
                            tokenConversions = tokenConversions,
                            missions = missions,
                            users = successState?.users ?: emptyList()
                        )
                    }
                }
            }
        }
    }

    tournamentForStandingsModal?.let { selectedTournament ->
        DeclareStandingsAndPrizesDialog(
            tournament = selectedTournament,
            viewModel = viewModel,
            onDismiss = { tournamentForStandingsModal = null }
        )
    }
}

// ============================================================================
// MODULE 1 & 2: MULTI-POSITION TOURNAMENT STANDINGS, ROOM ID & CANCELLATION
// ============================================================================
@Composable
fun TournamentsAndStandingsModuleSection(
    viewModel: TournamentDashboardViewModel,
    tournaments: List<Tournament>,
    onDeclareStandingsClick: (Tournament) -> Unit,
    onOpenLobbyChat: (String) -> Unit,
    onCreateTournamentClick: (() -> Unit)? = null,
    onTournamentClick: ((String) -> Unit)? = null,
    onSettingsClick: ((String) -> Unit)? = null
) {
    var statusFilter by remember { mutableStateOf("ALL") }
    var showQuickCreate by remember { mutableStateOf(false) }
    var quickVlxId by remember(tournaments.size) {
        val nextIdx = (tournaments.size + 1).coerceAtLeast(1)
        mutableStateOf("VLX-%02d".format(nextIdx))
    }
    var quickTitle by remember { mutableStateOf("") }
    var quickMatchMode by remember { mutableStateOf("PER_KILL") }
    var quickRuleBadge by remember { mutableStateOf("STANDARD RULES") }
    var quickPerspective by remember { mutableStateOf("TPP") }
    var quickEntryFee by remember { mutableStateOf("20") }
    var quickPrizePool by remember { mutableStateOf("500") }
    var quickKillBounty by remember { mutableStateOf("10") }

    var roomDialogTournament by remember { mutableStateOf<Tournament?>(null) }
    var cancelDialogTournament by remember { mutableStateOf<Tournament?>(null) }
    var deleteDialogTournament by remember { mutableStateOf<Tournament?>(null) }
    var roomIdInput by remember { mutableStateOf("") }
    var roomPassInput by remember { mutableStateOf("") }
    var cancelReasonInput by remember { mutableStateOf("Match Cancelled by Admin — Full Entry Fee Refunded") }

    val filtered = remember(tournaments, statusFilter) {
        if (statusFilter == "ALL") tournaments
        else tournaments.filter {
            if (statusFilter == "LIVE") it.status.equals("LIVE", true) || it.status.equals("ONGOING", true)
            else it.status.equals(statusFilter, true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(18.dp))
                Text(
                    text = "Matches (${filtered.size})",
                    color = TextPrimaryWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminActionChip(
                    label = if (showQuickCreate) "Close" else "+ #VLX Match",
                    color = AccentAmber,
                    icon = if (showQuickCreate) Icons.Default.Close else Icons.Default.Add,
                    onClick = { showQuickCreate = !showQuickCreate }
                )
                if (onCreateTournamentClick != null) {
                    AdminActionChip(
                        label = "Create Match",
                        color = AccentGreen,
                        onClick = onCreateTournamentClick,
                        iconOnly = true
                    )
                }
            }
        }

        if (showQuickCreate) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0A0D), RoundedCornerShape(8.dp))
                    .border(0.5.dp, HairlineBorder, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quickVlxId,
                        onValueChange = { quickVlxId = it },
                        label = { Text("#VLX ID", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(0.35f)
                    )
                    OutlinedTextField(
                        value = quickTitle,
                        onValueChange = { quickTitle = it },
                        label = { Text("Match Title (e.g. Bermuda Pro Cup)", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(0.65f)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "PER_KILL" to "STANDARD RULES",
                        "SURVIVAL" to "RANK PLACEMENT FOCUS",
                        "HEADSHOT_ONLY" to "HEADSHOT ONLY • 0% BODY DMG",
                        "BODY_DAMAGE_ON" to "FULL BODY DAMAGE ON",
                        "SNIPER_ONLY" to "SNIPER ARENA • AWM / KAR98K",
                        "LIMITED_AMMO" to "LIMITED AMMO • TACTICAL",
                        "UNLIMITED_AMMO" to "UNLIMITED GLOO & AMMO",
                        "PISTOL_ONLY" to "DESERT EAGLE / M500 ONLY"
                    ).forEach { (modeKey, defaultBadge) ->
                        val sel = quickMatchMode == modeKey
                        Box(
                            modifier = Modifier
                                .background(if (sel) AccentAmber.copy(alpha = 0.16f) else PureBlackBg, RoundedCornerShape(6.dp))
                                .border(0.5.dp, if (sel) AccentAmber else HairlineBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    quickMatchMode = modeKey
                                    quickRuleBadge = defaultBadge
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = modeKey.replace("_", " "),
                                color = if (sel) AccentAmber else TextSecondaryZinc,
                                fontSize = 10.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quickRuleBadge,
                        onValueChange = { quickRuleBadge = it },
                        label = { Text("Rule Badge", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    listOf("TPP", "FPP").forEach { persp ->
                        val sel = quickPerspective == persp
                        Box(
                            modifier = Modifier
                                .background(if (sel) AccentBlue.copy(alpha = 0.16f) else PureBlackBg, RoundedCornerShape(6.dp))
                                .border(0.5.dp, if (sel) AccentBlue else HairlineBorder, RoundedCornerShape(6.dp))
                                .clickable { quickPerspective = persp }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = persp,
                                color = if (sel) AccentBlue else TextSecondaryZinc,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quickEntryFee,
                        onValueChange = { quickEntryFee = it },
                        label = { Text("Entry ₹", fontSize = 10.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = quickPrizePool,
                        onValueChange = { quickPrizePool = it },
                        label = { Text("Pool ₹", fontSize = 10.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = quickKillBounty,
                        onValueChange = { quickKillBounty = it },
                        label = { Text("Per Kill ₹", fontSize = 10.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    AdminActionChip(
                        label = "Publish",
                        color = AccentGreen,
                        icon = Icons.Default.Check,
                        onClick = {
                            val cleanId = quickVlxId.trim().ifBlank { "VLX-${tournaments.size + 1}" }
                            val poolVal = quickPrizePool.toFloatOrNull() ?: 500f
                            val newTourney = Tournament(
                                id = cleanId,
                                displayTournamentId = cleanId,
                                title = quickTitle.trim().ifBlank { "Velorix $cleanId ${quickMatchMode.replace("_", " ")}" },
                                game = "Free Fire",
                                category = "BR",
                                matchCategory = "BATTLE_ROYALE",
                                matchMode = quickMatchMode,
                                customRuleBadge = quickRuleBadge.trim().ifBlank { "STANDARD RULES" },
                                perspective = quickPerspective,
                                map = "Bermuda",
                                format = "SOLO",
                                entryFee = quickEntryFee.toFloatOrNull() ?: 20f,
                                prizePool = poolVal,
                                firstPlacePrize = kotlin.math.round(poolVal * 0.5f),
                                secondPlacePrize = kotlin.math.round(poolVal * 0.3f),
                                thirdPlacePrize = kotlin.math.round(poolVal * 0.2f),
                                perKillPrize = quickKillBounty.toFloatOrNull() ?: 10f,
                                maxPlayers = 48,
                                status = "UPCOMING",
                                startsAt = (System.currentTimeMillis() + 3600_000L).toString(),
                                headshotOnly = quickMatchMode == "HEADSHOT_ONLY",
                                limitedAmmo = quickMatchMode != "UNLIMITED_AMMO"
                            )
                            viewModel.createTournament(newTourney)
                            showQuickCreate = false
                            quickTitle = ""
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            listOf("ALL", "UPCOMING", "LIVE", "COMPLETED", "CANCELLED").forEach { st ->
                val active = statusFilter == st
                Text(
                    text = st,
                    color = if (active) TextPrimaryWhite else TextSecondaryZinc,
                    fontSize = 11.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clickable { statusFilter = st }
                        .padding(vertical = 4.dp)
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

        if (filtered.isEmpty()) {
            Text(
                text = "No tournaments in '$statusFilter' state.",
                color = TextSecondaryZinc,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            filtered.forEach { t ->
                val stColor = when (t.status.uppercase()) {
                    "LIVE", "ONGOING" -> AccentGreen
                    "COMPLETED", "ENDED" -> AccentBlue
                    "CANCELLED" -> AccentRed
                    else -> AccentAmber
                }
                val vlxLabel = t.displayTournamentId.ifBlank { t.id }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "[#$vlxLabel] ${t.title}",
                                color = TextPrimaryWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${t.format} • ${t.perspective.ifBlank { "TPP" }} • ${t.matchMode.ifBlank { "PER_KILL" }} • ${t.map.ifBlank { "Bermuda" }}",
                                color = TextSecondaryZinc,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Slots: ${t.registeredPlayers}/${t.maxPlayers}  •  Entry: ₹${t.entryFee.toInt()}  •  Pool: ₹${t.prizePool.toInt()}  •  Kill: ₹${t.perKillReward.toInt()}",
                                color = TextPrimaryWhite.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            if (t.customRuleBadge.isNotBlank()) {
                                Text(
                                    text = "⚡ ${t.customRuleBadge}",
                                    color = AccentAmber,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(stColor.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
                                .border(0.5.dp, stColor.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${t.status.uppercase()} • ${t.payoutStatus.ifBlank { "UNPAID" }}",
                                color = stColor,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    val rId = t.roomDetails?.roomId.orEmpty()
                    val rPass = t.roomDetails?.roomPassword.orEmpty()
                    if (rId.isNotBlank()) {
                        Text(
                            text = "Room ID: $rId • Pass: ${rPass.ifBlank { "None" }}",
                            color = AccentGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (t.winnerName.isNotBlank()) {
                        Text(
                            text = "Winner #1: ${t.winnerName} (₹${"%.0f".format(t.winnerPrize)}) • Disbursed: ₹${"%.0f".format(t.totalPrizeDisbursed)}",
                            color = AccentAmber,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AdminActionChip(
                            label = "Declare Winners",
                            color = AccentAmber,
                            onClick = { onDeclareStandingsClick(t) },
                            iconOnly = true
                        )
                        AdminActionChip(
                            label = "Room ID / Pass",
                            color = AccentGreen,
                            onClick = {
                                roomIdInput = t.roomDetails?.roomId.orEmpty()
                                roomPassInput = t.roomDetails?.roomPassword.orEmpty()
                                roomDialogTournament = t
                            },
                            iconOnly = true
                        )
                        if (onTournamentClick != null) {
                            AdminActionChip(
                                label = "Details",
                                color = AccentBlue,
                                onClick = { onTournamentClick(t.id) },
                                iconOnly = true
                            )
                        }
                        if (onSettingsClick != null) {
                            AdminActionChip(
                                label = "Edit",
                                color = AccentBlue,
                                onClick = { onSettingsClick(t.id) },
                                iconOnly = true
                            )
                        }
                        if (!t.status.equals("LIVE", ignoreCase = true) && !t.status.equals("CANCELLED", ignoreCase = true) && !t.status.equals("COMPLETED", ignoreCase = true)) {
                            AdminActionChip(
                                label = "Set Live",
                                color = AccentGreen,
                                onClick = { viewModel.updateTournament(t.copy(status = "LIVE")) },
                                iconOnly = true
                            )
                        }
                        AdminActionChip(
                            label = "Lobby Chat",
                            color = AccentBlue,
                            onClick = { onOpenLobbyChat(t.id) },
                            iconOnly = true
                        )
                        if (!t.status.equals("CANCELLED", ignoreCase = true) && !t.status.equals("COMPLETED", ignoreCase = true)) {
                            AdminActionChip(
                                label = "Cancel & Refund",
                                color = AccentRed,
                                onClick = { cancelDialogTournament = t },
                                iconOnly = true
                            )
                        }
                        AdminActionChip(
                            label = "Delete",
                            color = AccentRed,
                            onClick = { deleteDialogTournament = t },
                            iconOnly = true
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)
            }
        }
    }

    deleteDialogTournament?.let { tourney ->
        AlertDialog(
            onDismissRequest = { deleteDialogTournament = null },
            containerColor = Color(0xFF0A0A0D),
            title = {
                Text(
                    text = "Delete Tournament Permanently?",
                    color = AccentRed,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete '${tourney.title}' from RTDB & Firestore and update /global_app_state?",
                    color = TextSecondaryZinc,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTournament(tourney.id)
                        deleteDialogTournament = null
                    }
                ) {
                    Text("DELETE PERMANENTLY", color = AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialogTournament = null }) {
                    Text("CANCEL", color = TextSecondaryZinc)
                }
            }
        )
    }

    roomDialogTournament?.let { tourney ->
        AlertDialog(
            onDismissRequest = { roomDialogTournament = null },
            containerColor = Color(0xFF0A0A0D),
            title = {
                Text(
                    text = "Release Room ID & Password — ${tourney.title}",
                    color = TextPrimaryWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Writes to RTDB /tournaments/${tourney.id}, /matches/${tourney.id}, Firestore, and pushes ROOM_CREDENTIALS_RELEASED to /global_app_state.",
                        color = TextSecondaryZinc,
                        fontSize = 11.sp
                    )
                    OutlinedTextField(
                        value = roomIdInput,
                        onValueChange = { roomIdInput = it },
                        label = { Text("Custom Room ID", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = roomPassInput,
                        onValueChange = { roomPassInput = it },
                        label = { Text("Room Password", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (roomIdInput.isNotBlank()) {
                            viewModel.publishRoomCredentialsWith5MinCheck(
                                tournamentId = tourney.id,
                                roomId = roomIdInput.trim(),
                                roomPass = roomPassInput.trim(),
                                bypassCheck = true
                            )
                            roomDialogTournament = null
                        }
                    }
                ) {
                    Text("BROADCAST CREDENTIALS", color = AccentGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { roomDialogTournament = null }) {
                    Text("CANCEL", color = TextSecondaryZinc)
                }
            }
        )
    }

    cancelDialogTournament?.let { tourney ->
        AlertDialog(
            onDismissRequest = { cancelDialogTournament = null },
            containerColor = Color(0xFF0A0A0D),
            title = {
                Text(
                    text = "Cancel Tournament & Auto-Refund — ${tourney.title}",
                    color = AccentRed,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Marks status = CANCELLED, refunds ₹${tourney.entryFee.toInt()} to all registered participants in /tournaments/${tourney.id}/participants, records refund_${tourney.id}_{uid} transactions, and pushes TOURNAMENT_CANCELLED to /global_app_state.",
                        color = TextSecondaryZinc,
                        fontSize = 11.sp
                    )
                    OutlinedTextField(
                        value = cancelReasonInput,
                        onValueChange = { cancelReasonInput = it },
                        label = { Text("Cancellation Reason", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelTournamentWithAutoRefund(tourney, cancelReasonInput)
                        cancelDialogTournament = null
                    }
                ) {
                    Text("CONFIRM CANCEL & REFUND ALL", color = AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelDialogTournament = null }) {
                    Text("KEEP ACTIVE", color = TextSecondaryZinc)
                }
            }
        )
    }
}

@Composable
fun DeclareStandingsAndPrizesDialog(
    tournament: Tournament,
    viewModel: TournamentDashboardViewModel,
    onDismiss: () -> Unit
) {
    var standingsList by remember { mutableStateOf<List<TournamentStandingEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isPublishing by remember { mutableStateOf(false) }
    var tiePrizeMode by remember { mutableStateOf("SPLIT_TIED_POOL") } // SPLIT_TIED_POOL | FULL_PRIZE_EACH
    var customRankEditorIdx by remember { mutableStateOf<Int?>(null) }
    var customRankDraft by remember { mutableStateOf("") }

    val perKillRate = remember(tournament.perKillReward, tournament.perKillPrize) {
        when {
            tournament.perKillReward > 0f -> tournament.perKillReward.toDouble()
            tournament.perKillPrize > 0f -> tournament.perKillPrize.toDouble()
            else -> 0.0
        }
    }
    val prizePoolTotal = remember(tournament.prizePool) { tournament.prizePool.toDouble() }

    fun baseRankPrize(rank: Int): Double = when (rank) {
        1 -> if (tournament.firstPlacePrize > 0f) tournament.firstPlacePrize.toDouble() else kotlin.math.round(prizePoolTotal * 0.50)
        2 -> if (tournament.secondPlacePrize > 0f) tournament.secondPlacePrize.toDouble() else kotlin.math.round(prizePoolTotal * 0.30)
        3 -> if (tournament.thirdPlacePrize > 0f) tournament.thirdPlacePrize.toDouble() else kotlin.math.round(prizePoolTotal * 0.20)
        else -> 0.0
    }

    fun recalculateEstimatedPrizes(list: List<TournamentStandingEntry>, mode: String): List<TournamentStandingEntry> {
        val rankCounts = list.filter { it.rank > 0 }.groupingBy { it.rank }.eachCount()
        return list.map { row ->
            val r = row.rank
            val rp = if (r <= 0) {
                0.0
            } else {
                val tiedCount = (rankCounts[r] ?: 1).coerceAtLeast(1)
                if (tiedCount > 1 && mode == "SPLIT_TIED_POOL") {
                    val summed = (r until (r + tiedCount)).sumOf { baseRankPrize(it) }
                    kotlin.math.round((summed / tiedCount) * 100.0) / 100.0
                } else {
                    baseRankPrize(r)
                }
            }
            val kp = row.kills * perKillRate
            val tot = rp + kp
            row.copy(rankPrize = rp, killPrize = kp, prizeWon = tot, wonAnything = tot > 0.0)
        }
    }

    LaunchedEffect(tournament.id) {
        isLoading = true
        viewModel.loadTournamentStandings(tournament) { loaded ->
            standingsList = recalculateEstimatedPrizes(loaded, tiePrizeMode)
            isLoading = false
        }
    }

    val rankCounts = remember(standingsList) {
        standingsList.filter { it.rank > 0 }.groupingBy { it.rank }.eachCount()
    }
    val totalCalculatedPayout = remember(standingsList) {
        standingsList.sumOf { (it.rankPrize + it.killPrize).coerceAtLeast(0.0) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.97f)
                .fillMaxHeight(0.92f)
                .background(Color(0xFF0B0E14), RoundedCornerShape(14.dp))
                .border(0.75.dp, HairlineBorder, RoundedCornerShape(14.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Modal Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AUTO-SYNCED ROSTER & POSITION SELECTOR",
                        color = TextPrimaryWhite,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${tournament.title} (${tournament.id}) • Pool: ₹${prizePoolTotal.toInt()} • Per Kill: ₹${perKillRate.toInt()}",
                        color = TextSecondaryZinc,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimaryWhite)
                }
            }

            // Tie Prize Mode Toggle + Summary Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "SPLIT_TIED_POOL" to "Split Tied Pool",
                    "FULL_PRIZE_EACH" to "Full Prize Each"
                ).forEach { (mode, label) ->
                    val sel = tiePrizeMode == mode
                    Box(
                        modifier = Modifier
                            .background(if (sel) AccentAmber.copy(alpha = 0.2f) else Color(0xFF0A0A0D), RoundedCornerShape(8.dp))
                            .border(0.7.dp, if (sel) AccentAmber else HairlineBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                tiePrizeMode = mode
                                standingsList = recalculateEstimatedPrizes(standingsList, mode)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (sel) AccentAmber else TextSecondaryZinc,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "Players: ${standingsList.size} • Est. Payout: ₹${"%.0f".format(totalCalculatedPayout)}",
                    color = AccentGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )
            }

            HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Syncing live roster from admin_match_rosters/${tournament.id}/players...",
                        color = TextSecondaryZinc,
                        fontSize = 12.sp
                    )
                }
            } else if (standingsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No joined players found in admin_match_rosters/${tournament.id}/players.",
                        color = TextSecondaryZinc,
                        fontSize = 12.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    standingsList.forEachIndexed { idx, entry ->
                        val isTied = entry.rank > 0 && (rankCounts[entry.rank] ?: 0) >= 2
                        val totalWon = (entry.rankPrize + entry.killPrize).coerceAtLeast(0.0)
                        val isWinner = totalWon > 0.0
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0E1014), RoundedCornerShape(10.dp))
                                .border(
                                    0.7.dp,
                                    when {
                                        isTied -> AccentAmber.copy(alpha = 0.6f)
                                        entry.rank in 1..3 -> AccentGreen.copy(alpha = 0.45f)
                                        else -> HairlineBorder
                                    },
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Top Identity Row: SLOT #N Badge + IGN + @username + FF UID + Ticket + TIED badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(AccentBlue.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
                                            .border(0.6.dp, AccentBlue.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "SLOT #${entry.slotNumber}",
                                            color = AccentBlue,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.inGameName.ifBlank { entry.username.ifBlank { "Player" } },
                                            color = TextPrimaryWhite,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = buildString {
                                                append("@${entry.username.ifBlank { "player" }}")
                                                if (entry.characterId.isNotBlank()) append(" • UID: ${entry.characterId}")
                                                if (entry.ticketCode.isNotBlank()) append(" • ${entry.ticketCode}")
                                            },
                                            color = TextSecondaryZinc,
                                            fontSize = 9.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (isTied) {
                                        Box(
                                            modifier = Modifier
                                                .background(AccentAmber.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                                .border(0.6.dp, AccentAmber, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("TIED #${entry.rank}", color = AccentAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                    Text(
                                        text = if (isWinner) "₹${"%.0f".format(totalWon)}" else "₹0",
                                        color = if (isWinner) AccentGreen else TextSecondaryZinc,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // 1-Tap Position Chips: [1st] [2nd] [3rd] [4th] [5th] [#N] [Part.]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val quickRanks = listOf(1 to "1st", 2 to "2nd", 3 to "3rd", 4 to "4th", 5 to "5th", 0 to "Part.")
                                quickRanks.forEach { (rankVal, label) ->
                                    val selected = entry.rank == rankVal
                                    val chipColor = when (rankVal) {
                                        1 -> AccentAmber
                                        2 -> AccentBlue
                                        3 -> AccentGreen
                                        0 -> TextSecondaryZinc
                                        else -> TextPrimaryWhite
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(if (selected) chipColor.copy(alpha = 0.22f) else Color(0xFF0A0A0D), RoundedCornerShape(7.dp))
                                            .border(0.7.dp, if (selected) chipColor else HairlineBorder, RoundedCornerShape(7.dp))
                                            .clickable {
                                                val updated = standingsList.toMutableList().also {
                                                    it[idx] = entry.copy(rank = rankVal)
                                                }
                                                standingsList = recalculateEstimatedPrizes(updated, tiePrizeMode)
                                                if (customRankEditorIdx == idx) customRankEditorIdx = null
                                            }
                                            .padding(horizontal = 9.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (selected) chipColor else TextSecondaryZinc,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Custom #N chip
                                val isCustomRank = entry.rank > 5
                                Box(
                                    modifier = Modifier
                                        .background(if (isCustomRank || customRankEditorIdx == idx) AccentBlue.copy(alpha = 0.2f) else Color(0xFF0A0A0D), RoundedCornerShape(7.dp))
                                        .border(0.7.dp, if (isCustomRank || customRankEditorIdx == idx) AccentBlue else HairlineBorder, RoundedCornerShape(7.dp))
                                        .clickable {
                                            customRankEditorIdx = if (customRankEditorIdx == idx) null else idx
                                            customRankDraft = if (entry.rank > 5) entry.rank.toString() else "6"
                                        }
                                        .padding(horizontal = 9.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = if (isCustomRank) "#${entry.rank}" else "#N",
                                        color = if (isCustomRank || customRankEditorIdx == idx) AccentBlue else TextSecondaryZinc,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (customRankEditorIdx == idx) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = customRankDraft,
                                        onValueChange = { customRankDraft = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Custom Rank #", fontSize = 9.5.sp) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    AdminActionChip(
                                        label = "Set Rank",
                                        color = AccentBlue,
                                        onClick = {
                                            val r = (customRankDraft.toIntOrNull() ?: 6).coerceAtLeast(1)
                                            val updated = standingsList.toMutableList().also {
                                                it[idx] = entry.copy(rank = r)
                                            }
                                            standingsList = recalculateEstimatedPrizes(updated, tiePrizeMode)
                                            customRankEditorIdx = null
                                        }
                                    )
                                }
                            }

                            // Kills Stepper (- / +) Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Rank ₹${entry.rankPrize.toInt()} + Kill ₹${entry.killPrize.toInt()}",
                                    color = TextSecondaryZinc,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("KILLS", color = TextSecondaryZinc, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color(0xFF141821), RoundedCornerShape(7.dp))
                                            .border(0.6.dp, HairlineBorder, RoundedCornerShape(7.dp))
                                            .clickable {
                                                val nextK = (entry.kills - 1).coerceAtLeast(0)
                                                val updated = standingsList.toMutableList().also { it[idx] = entry.copy(kills = nextK) }
                                                standingsList = recalculateEstimatedPrizes(updated, tiePrizeMode)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("-", color = TextPrimaryWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = entry.kills.toString(),
                                        color = TextPrimaryWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(AccentGreen.copy(alpha = 0.16f), RoundedCornerShape(7.dp))
                                            .border(0.6.dp, AccentGreen.copy(alpha = 0.5f), RoundedCornerShape(7.dp))
                                            .clickable {
                                                val nextK = entry.kills + 1
                                                val updated = standingsList.toMutableList().also { it[idx] = entry.copy(kills = nextK) }
                                                standingsList = recalculateEstimatedPrizes(updated, tiePrizeMode)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+", color = AccentGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

            // Publish Action Footer (calls Cloud Function finalizeTournamentPayouts)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("CANCEL", color = TextSecondaryZinc, fontSize = 12.sp)
                }
                Button(
                    onClick = {
                        if (!isPublishing && standingsList.isNotEmpty()) {
                            isPublishing = true
                            viewModel.publishTournamentStandings(
                                tournament = tournament,
                                standings = standingsList,
                                tiePrizeMode = tiePrizeMode
                            ) { ok ->
                                isPublishing = false
                                if (ok) onDismiss()
                            }
                        }
                    },
                    enabled = !isPublishing && standingsList.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    modifier = Modifier.testTag("finalize_tournament_payouts_btn")
                ) {
                    Text(
                        text = if (isPublishing) "SETTLING ON SERVER..." else "PUBLISH & DISTRIBUTE WINNINGS",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ============================================================================
// MODULE 5: USERS, SANCTIONS (users/{uid}) & QUICK NOTIFY
// ============================================================================
@Composable
private fun UsersAndModerationModuleSection(
    viewModel: TournamentDashboardViewModel,
    users: List<UserProfile>,
    onQuickNotifyUser: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var moderationReason by remember { mutableStateOf("Violation of Velorix Fair Play Policy") }
    var suspendHoursInput by remember { mutableStateOf("24") }

    val filteredUsers = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users
        else users.filter {
            it.username.contains(searchQuery, true) ||
                it.email.contains(searchQuery, true) ||
                it.id.contains(searchQuery, true) ||
                it.gameId.contains(searchQuery, true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Group, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
            Text(
                text = "Players (${filteredUsers.size})",
                color = TextPrimaryWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by UID, Username, Email, or Free Fire UID", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = moderationReason,
                onValueChange = { moderationReason = it },
                label = { Text("Ban / Suspend Reason", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(0.7f)
            )
            OutlinedTextField(
                value = suspendHoursInput,
                onValueChange = { suspendHoursInput = it },
                label = { Text("Suspend Hrs", fontSize = 11.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(0.3f)
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

        filteredUsers.take(80).forEach { u ->
            val statusColor = when {
                u.isBanned -> AccentRed
                u.isSuspended -> AccentAmber
                else -> AccentGreen
            }
            val pushReady = u.fcmToken.isNotBlank()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (u.isOnline) {
                            Box(modifier = Modifier.size(7.dp).background(AccentGreen, CircleShape))
                        }
                        Text(
                            text = "${u.username.ifBlank { "Player" }} (${u.id.take(12)})",
                            color = TextPrimaryWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(if (pushReady) AccentGreen.copy(alpha = 0.14f) else Color(0xFF141821), RoundedCornerShape(6.dp))
                                .border(0.5.dp, if (pushReady) AccentGreen.copy(alpha = 0.5f) else HairlineBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (pushReady) "PUSH READY" else "NO FCM",
                                color = if (pushReady) AccentGreen else TextSecondaryZinc,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = when {
                                u.isBanned -> "BANNED"
                                u.isSuspended -> "SUSPENDED"
                                else -> "ACTIVE"
                            },
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Text(
                    text = "Email: ${u.email.ifBlank { "N/A" }} • FF UID: ${u.gameId.ifBlank { "N/A" }}",
                    color = TextSecondaryZinc,
                    fontSize = 11.sp
                )
                Text(
                    text = "Wallet: ₹${"%.2f".format(u.funds)} (Dep ₹${"%.0f".format(u.depositFunds)} / Win ₹${"%.0f".format(u.winningFunds)} / Bonus ₹${"%.0f".format(u.bonusFunds)}) • Tokens: ${u.tokens} • ${u.totalMatchesJoined}M / ${u.wins}W / ${u.kills}K",
                    color = TextPrimaryWhite.copy(alpha = 0.85f),
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AdminActionChip(
                        label = "Notify User",
                        color = AccentBlue,
                        icon = Icons.Default.NotificationsActive,
                        onClick = { onQuickNotifyUser(u.id) }
                    )
                    AdminActionChip(
                        label = if (u.isBanned) "Unban User" else "Permanent Ban",
                        color = if (u.isBanned) AccentGreen else AccentRed,
                        onClick = { viewModel.setUserBanWithReason(u, !u.isBanned, moderationReason) },
                        iconOnly = true
                    )
                    AdminActionChip(
                        label = if (u.isSuspended) "Unsuspend" else "Suspend (${suspendHoursInput}h)",
                        color = AccentAmber,
                        onClick = {
                            val hrs = suspendHoursInput.toIntOrNull() ?: 24
                            viewModel.setUserSuspendWithDuration(u, !u.isSuspended, moderationReason, hrs)
                        },
                        iconOnly = true
                    )
                    AdminActionChip(
                        label = "Reset Avatar",
                        color = AccentBlue,
                        onClick = { viewModel.resetUserAvatarToDefault(u) },
                        iconOnly = true
                    )
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)
        }
    }
}

// Legacy 10-Channel Support Inbox removed — replaced by UnifiedRequestHubModuleSection (12-Channel Hub & My Tickets Desk) in AdminHubNewModules.kt


// ============================================================================
// MODULE 4: ACCOUNT DELETION, DATA EXPORT ARCHIVE & AUDIT LOGS (DPDP ACT 2023)
// ============================================================================
@Composable
private fun DpdpAccountDeletionAndExportSection(
    viewModel: TournamentDashboardViewModel,
    deletionRequests: List<AccountDeletionRequest>,
    exportRequests: List<DataExportRequest>,
    auditLogs: List<AdminAuditLogItem>,
    users: List<UserProfile>
) {
    var inspectedExport by remember { mutableStateOf<DataExportRequest?>(null) }
    var selectedUserIdForExport by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 2A. Account Deletion Requests (#account-deletions)
        Text(
            text = "2A. ACCOUNT DELETION REQUESTS (#account-deletions)",
            color = TextPrimaryWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "DPDP Act 2023 • Approve permanently wipes /users/{uid} in RTDB & Firestore. Reject restores accountStatus = ACTIVE.",
            color = TextSecondaryZinc,
            fontSize = 11.sp
        )

        HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

        if (deletionRequests.isEmpty()) {
            Text(
                text = "No account deletion requests in /account_deletion_requests.",
                color = TextSecondaryZinc,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            deletionRequests.forEach { req ->
                val stColor = when (req.status.uppercase()) {
                    "COMPLETED" -> AccentRed
                    "RESTORED" -> AccentGreen
                    else -> AccentAmber
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${req.username.ifBlank { "Player" }} (${req.userId})",
                            color = TextPrimaryWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = req.status.uppercase(),
                            color = stColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "Email: ${req.email.ifBlank { "N/A" }} • Wallet Balance: ₹${"%.2f".format(req.balance)} • Data Exported: ${if (req.dataExported) "YES" else "NO"}",
                        color = TextSecondaryZinc,
                        fontSize = 11.sp
                    )
                    if (req.reason.isNotBlank()) {
                        Text(
                            text = "Reason: ${req.reason}",
                            color = TextSecondaryZinc,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "Requested: ${formatTimestampShort(req.requestedAt)}",
                        color = TextSecondaryZinc,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdminActionChip(
                            label = "Approve & Delete",
                            color = AccentRed,
                            onClick = { viewModel.approveAccountDeletion(req) }
                        )
                        AdminActionChip(
                            label = "Restore Account",
                            color = AccentGreen,
                            onClick = { viewModel.restoreAccountDeletion(req) }
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2B. Account Data Export Archive (#data-exports)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "2B. DATA EXPORT AUDIT LOG (#data-exports)",
                    color = TextPrimaryWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Snapshots stored in /data_export_requests/{exportId} & /admin_channels/data_export_requests",
                    color = TextSecondaryZinc,
                    fontSize = 11.sp
                )
            }
        }

        // Quick Generator for Any Player DPDP JSON Export
        if (users.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = selectedUserIdForExport,
                    onValueChange = { selectedUserIdForExport = it },
                    label = { Text("Enter Player UID / Username to Archive DPDP JSON", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TextPrimaryWhite,
                        unfocusedBorderColor = HairlineBorder,
                        focusedTextColor = TextPrimaryWhite,
                        unfocusedTextColor = TextPrimaryWhite
                    )
                )
                TextButton(
                    onClick = {
                        val query = selectedUserIdForExport.trim()
                        val target = users.firstOrNull {
                            it.id.equals(query, ignoreCase = true) ||
                                it.username.equals(query, ignoreCase = true) ||
                                it.email.equals(query, ignoreCase = true)
                        } ?: users.firstOrNull()
                        if (target != null) {
                            viewModel.generateUserDpdpExport(target) { created ->
                                if (created != null) inspectedExport = created
                            }
                        }
                    }
                ) {
                    Text("ARCHIVE JSON", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

        if (exportRequests.isEmpty()) {
            Text(
                text = "No data export records yet. Use 'ARCHIVE JSON' above or wait for user exports from the VeloRix app.",
                color = TextSecondaryZinc,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            exportRequests.forEach { exp ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${exp.username.ifBlank { "Player" }} • ${exp.exportId.take(14)}",
                            color = TextPrimaryWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        AdminActionChip(
                            label = "View JSON",
                            color = AccentBlue,
                            onClick = { inspectedExport = exp }
                        )
                    }
                    Text(
                        text = "UID: ${exp.userId} • Balance: ₹${"%.2f".format(exp.balance)} • Txns: ${exp.transactionCount} • Joined Matches: ${exp.joinedTournamentCount}",
                        color = TextSecondaryZinc,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Exported At: ${formatTimestampShort(exp.requestedAt)} • Status: ${exp.status}",
                        color = TextSecondaryZinc,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4C. Administrative Audit Logs (/audit_logs)
        Text(
            text = "4C. ADMINISTRATIVE AUDIT LOGS (/audit_logs in RTDB & Firestore)",
            color = TextPrimaryWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        if (auditLogs.isEmpty()) {
            Text(
                text = "No audit logs recorded yet in /audit_logs.",
                color = TextSecondaryZinc,
                fontSize = 11.sp
            )
        } else {
            auditLogs.take(40).forEach { log ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = log.action,
                            color = AccentAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = formatTimestampShort(log.timestamp),
                            color = TextSecondaryZinc,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = log.detail,
                        color = TextPrimaryWhite,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Admin: ${log.adminEmail} • Target: ${log.targetId.ifBlank { "Global" }}",
                        color = TextSecondaryZinc,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)
            }
        }
    }

    inspectedExport?.let { exp ->
        DataExportJsonArchiveModal(
            exportRecord = exp,
            onDismiss = { inspectedExport = null }
        )
    }
}

@Composable
private fun DataExportJsonArchiveModal(
    exportRecord: DataExportRequest,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val formattedJson = remember(exportRecord) {
        exportRecord.exportedJson.ifBlank {
            """
            {
              "exportId": "${exportRecord.exportId}",
              "userId": "${exportRecord.userId}",
              "username": "${exportRecord.username}",
              "email": "${exportRecord.email}",
              "balance": ${exportRecord.balance},
              "transactionCount": ${exportRecord.transactionCount},
              "joinedTournamentCount": ${exportRecord.joinedTournamentCount},
              "requestedAt": ${exportRecord.requestedAt}
            }
            """.trimIndent()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .background(PureBlackBg, RoundedCornerShape(6.dp))
                .border(0.5.dp, HairlineBorder, RoundedCornerShape(6.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DPDP DATA EXPORT JSON ARCHIVE",
                        color = TextPrimaryWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${exportRecord.exportId} • ${exportRecord.username} (${exportRecord.userId})",
                        color = TextSecondaryZinc,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimaryWhite)
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(0.5.dp, HairlineBorder, RoundedCornerShape(4.dp))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = formattedJson,
                    color = AccentGreen,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("DPDP_Export_JSON", formattedJson))
                        GlobalErrorManager.emitSuccess("DPDP JSON snapshot copied to clipboard!")
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextPrimaryWhite, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("COPY JSON", color = TextPrimaryWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_SUBJECT, "${exportRecord.exportId}.json")
                            putExtra(Intent.EXTRA_TEXT, formattedJson)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Export DPDP JSON Archive"))
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DOWNLOAD / SHARE .JSON", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================================================
// MODULE 2: WALLET REQUESTS — DELEGATES TO UPGRADED WALLET, PRE-VERIFY UTR & DUAL WITHDRAWAL DESK
// ============================================================================
@Composable
fun DepositVerificationQueueSection(
    viewModel: TournamentDashboardViewModel,
    depositRequests: List<DepositRequest>,
    payoutRequests: List<PayoutRequest> = emptyList()
) {
    val unifiedWithdrawals by viewModel.unifiedWithdrawals.collectAsStateWithLifecycle()
    val verifiedBankDeposits by viewModel.verifiedBankDeposits.collectAsStateWithLifecycle()
    val mergedWithdrawals = remember(unifiedWithdrawals, payoutRequests) {
        val byId = LinkedHashMap<String, PayoutRequest>()
        unifiedWithdrawals.forEach { if (it.id.isNotBlank()) byId[it.id] = it }
        payoutRequests.forEach { if (it.id.isNotBlank() && !byId.containsKey(it.id)) byId[it.id] = it }
        byId.values.sortedByDescending { it.requestedAt }
    }
    WalletUpiVoucherModuleSection(
        viewModel = viewModel,
        depositRequests = depositRequests,
        withdrawals = mergedWithdrawals,
        verifiedUtrs = verifiedBankDeposits
    )
}

// ============================================================================
// SECTION 5C: TOKEN CONVERSION LEDGER (10 TOKENS = 1 VT) & MISSIONS MANAGER
// ============================================================================
@Composable
private fun TokensAndMissionsManagerSection(
    viewModel: TournamentDashboardViewModel,
    tokenConversions: List<TokenConversionRecord>,
    missions: List<MissionItem>,
    users: List<UserProfile>
) {
    var targetUserQuery by remember { mutableStateOf("") }
    var tokenDeltaInput by remember { mutableStateOf("50") }
    var tokenReasonInput by remember { mutableStateOf("Admin Mission / Event Reward") }

    var newMissionTitle by remember { mutableStateOf("") }
    var newMissionDesc by remember { mutableStateOf("") }
    var newMissionRewardTokens by remember { mutableStateOf("20") }
    var newMissionTargetCount by remember { mutableStateOf("1") }
    var newMissionType by remember { mutableStateOf("DAILY") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Direct Player Token Grant / Deduct
        Text(
            text = "PLAYER TOKEN ADJUSTER (/users/{uid}/tokens)",
            color = TextPrimaryWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "10 Tokens = 1 VT (Bonus Balance). Increments monotonic user version + 1.",
            color = TextSecondaryZinc,
            fontSize = 11.sp
        )

        OutlinedTextField(
            value = targetUserQuery,
            onValueChange = { targetUserQuery = it },
            label = { Text("Player UID, Username, or Email", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TextPrimaryWhite,
                unfocusedBorderColor = HairlineBorder,
                focusedTextColor = TextPrimaryWhite,
                unfocusedTextColor = TextPrimaryWhite
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = tokenDeltaInput,
                onValueChange = { tokenDeltaInput = it },
                label = { Text("Tokens (+/-)", fontSize = 11.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(0.35f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TextPrimaryWhite,
                    unfocusedBorderColor = HairlineBorder,
                    focusedTextColor = TextPrimaryWhite,
                    unfocusedTextColor = TextPrimaryWhite
                )
            )
            OutlinedTextField(
                value = tokenReasonInput,
                onValueChange = { tokenReasonInput = it },
                label = { Text("Audit Reason", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(0.65f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TextPrimaryWhite,
                    unfocusedBorderColor = HairlineBorder,
                    focusedTextColor = TextPrimaryWhite,
                    unfocusedTextColor = TextPrimaryWhite
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminActionChip(
                label = "+ Grant Tokens",
                color = AccentGreen,
                onClick = {
                    val delta = kotlin.math.abs(tokenDeltaInput.toIntOrNull() ?: 0)
                    val u = users.firstOrNull {
                        it.id.equals(targetUserQuery.trim(), ignoreCase = true) ||
                            it.username.equals(targetUserQuery.trim(), ignoreCase = true) ||
                            it.email.equals(targetUserQuery.trim(), ignoreCase = true)
                    }
                    if (u != null && delta > 0) {
                        viewModel.adjustUserTokens(u, delta, tokenReasonInput)
                    } else {
                        GlobalErrorManager.emitError("Match a valid player UID/Username and enter positive token count.")
                    }
                }
            )
            AdminActionChip(
                label = "- Deduct Tokens",
                color = AccentRed,
                onClick = {
                    val delta = -kotlin.math.abs(tokenDeltaInput.toIntOrNull() ?: 0)
                    val u = users.firstOrNull {
                        it.id.equals(targetUserQuery.trim(), ignoreCase = true) ||
                            it.username.equals(targetUserQuery.trim(), ignoreCase = true) ||
                            it.email.equals(targetUserQuery.trim(), ignoreCase = true)
                    }
                    if (u != null && delta < 0) {
                        viewModel.adjustUserTokens(u, delta, tokenReasonInput)
                    } else {
                        GlobalErrorManager.emitError("Match a valid player UID/Username and enter token count.")
                    }
                }
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

        // 2. Missions Manager (/missions)
        Text(
            text = "MISSIONS MANAGER (/missions in RTDB & Firestore)",
            color = TextPrimaryWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newMissionTitle,
                onValueChange = { newMissionTitle = it },
                label = { Text("Mission Title", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TextPrimaryWhite,
                    unfocusedBorderColor = HairlineBorder,
                    focusedTextColor = TextPrimaryWhite,
                    unfocusedTextColor = TextPrimaryWhite
                )
            )
            OutlinedTextField(
                value = newMissionRewardTokens,
                onValueChange = { newMissionRewardTokens = it },
                label = { Text("Reward Tokens", fontSize = 11.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(120.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TextPrimaryWhite,
                    unfocusedBorderColor = HairlineBorder,
                    focusedTextColor = TextPrimaryWhite,
                    unfocusedTextColor = TextPrimaryWhite
                )
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newMissionDesc,
                onValueChange = { newMissionDesc = it },
                label = { Text("Description (e.g. Play 1 BR Match)", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TextPrimaryWhite,
                    unfocusedBorderColor = HairlineBorder,
                    focusedTextColor = TextPrimaryWhite,
                    unfocusedTextColor = TextPrimaryWhite
                )
            )
            TextButton(
                onClick = {
                    if (newMissionTitle.isNotBlank()) {
                        viewModel.saveMission(
                            MissionItem(
                                id = "mission_${System.currentTimeMillis()}",
                                title = newMissionTitle.trim(),
                                description = newMissionDesc.trim(),
                                rewardTokens = newMissionRewardTokens.toIntOrNull() ?: 20,
                                targetCount = newMissionTargetCount.toIntOrNull() ?: 1,
                                actionType = newMissionType,
                                isActive = true
                            )
                        )
                        newMissionTitle = ""
                        newMissionDesc = ""
                    }
                }
            ) {
                Text("+ ADD MISSION", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        missions.forEach { m ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${m.title} (+${m.rewardTokens} Tokens)",
                        color = TextPrimaryWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${m.description} • Type: ${m.actionType} • Target: ${m.targetCount}",
                        color = TextSecondaryZinc,
                        fontSize = 11.sp
                    )
                }
                AdminActionChip(
                    label = "Delete",
                    color = AccentRed,
                    onClick = { viewModel.deleteMission(m.id) }
                )
            }
            HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)
        }

        // 3. Token Conversion Ledger (/token_conversions)
        Text(
            text = "TOKEN-TO-VT CONVERSION LEDGER (#token-ledger)",
            color = TextPrimaryWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        if (tokenConversions.isEmpty()) {
            Text(
                text = "No token conversions recorded yet in /token_conversions.",
                color = TextSecondaryZinc,
                fontSize = 11.sp
            )
        } else {
            tokenConversions.forEach { conv ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${conv.username.ifBlank { conv.userId }} • ${conv.tokensSpent} Tokens → ₹${"%.2f".format(conv.vtCredited)} VT",
                            color = TextPrimaryWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "UID: ${conv.userId} • ${formatTimestampShort(conv.createdAt)}",
                            color = TextSecondaryZinc,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = conv.status,
                        color = AccentGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)
            }
        }
    }
}

// ============================================================================
// MODULE 7A: LIVE SYSTEM CONFIG (5 ACTIVE FLAGS) + 1-CLICK RECONCILIATION DESK
// ============================================================================
@Composable
private fun SystemConfigCleanSection(
    viewModel: TournamentDashboardViewModel,
    systemConfig: SystemConfig,
    reconciliationSummaryJson: String? = null,
    isReconcilingBackend: Boolean = false
) {
    var maintenanceMode by remember(systemConfig) { mutableStateOf(systemConfig.maintenanceMode) }
    var maintenanceTitle by remember(systemConfig) { mutableStateOf(systemConfig.maintenanceTitle) }
    var maintenanceMessage by remember(systemConfig) { mutableStateOf(systemConfig.maintenanceMessage) }
    var forceUpdateRequired by remember(systemConfig) { mutableStateOf(systemConfig.forceUpdateRequired) }
    var showBanners by remember(systemConfig) { mutableStateOf(systemConfig.showBanners) }
    var latestVersionCode by remember(systemConfig) { mutableStateOf(systemConfig.latestVersionCode.toString()) }
    var latestVersionName by remember(systemConfig) { mutableStateOf(systemConfig.latestVersionName) }
    var updateDownloadUrl by remember(systemConfig) { mutableStateOf(systemConfig.updateDownloadUrl) }
    var updateChangelog by remember(systemConfig) { mutableStateOf(systemConfig.updateChangelog) }
    var upiId by remember(systemConfig) { mutableStateOf(systemConfig.upiId.ifBlank { "veloxyra.anant@fam" }) }
    var upiMerchantName by remember(systemConfig) { mutableStateOf(systemConfig.upiMerchantName.ifBlank { "VeloRix Esports" }) }
    var minDepositVt by remember(systemConfig) { mutableStateOf(systemConfig.minDepositVt.toString()) }
    var minWithdrawalVt by remember(systemConfig) { mutableStateOf(systemConfig.minWithdrawalVt.toString()) }
    var tokenConversionRate by remember(systemConfig) { mutableStateOf(systemConfig.tokenConversionRate.toString()) }
    var whatsappUrl by remember(systemConfig) { mutableStateOf(systemConfig.whatsappSupportUrl) }
    var telegramUrl by remember(systemConfig) { mutableStateOf(systemConfig.telegramChannelUrl) }
    var instagramUrl by remember(systemConfig) { mutableStateOf(systemConfig.instagramUrl) }
    var discordUrl by remember(systemConfig) { mutableStateOf(systemConfig.discordUrl) }
    var vpnBlockEnabled by remember(systemConfig) { mutableStateOf(systemConfig.vpnBlockEnabled) }

    // Strictly the 5 Active Core Feature Flags
    var featureFlagsMap by remember(systemConfig) {
        val activeKeys = SystemConfig.ALL_FEATURE_FLAG_METADATA.map { it.first }.toSet()
        val defaults = SystemConfig.defaultFeatureFlags().toMutableMap()
        systemConfig.featureFlags.forEach { (k, v) ->
            if (k in activeKeys) defaults[k] = v
        }
        mutableStateOf(defaults.toMap())
    }

    fun publishLiveConfigNow() {
        viewModel.saveSystemConfig(
            systemConfig.copy(
                maintenanceMode = maintenanceMode,
                maintenanceTitle = maintenanceTitle.trim(),
                maintenanceMessage = maintenanceMessage.trim(),
                forceUpdateRequired = forceUpdateRequired,
                showBanners = showBanners,
                latestVersionCode = latestVersionCode.toIntOrNull() ?: 2,
                latestVersionName = latestVersionName.trim(),
                updateDownloadUrl = updateDownloadUrl.trim(),
                updateChangelog = updateChangelog.trim(),
                upiId = upiId.trim().ifBlank { "veloxyra.anant@fam" },
                upiMerchantName = upiMerchantName.trim().ifBlank { "VeloRix Esports" },
                minDepositVt = minDepositVt.toDoubleOrNull() ?: 10.0,
                minWithdrawalVt = minWithdrawalVt.toDoubleOrNull() ?: 50.0,
                tokenConversionRate = tokenConversionRate.toIntOrNull() ?: 10,
                whatsappSupportUrl = whatsappUrl.trim(),
                telegramChannelUrl = telegramUrl.trim(),
                instagramUrl = instagramUrl.trim(),
                discordUrl = discordUrl.trim(),
                vpnBlockEnabled = vpnBlockEnabled,
                featureFlags = featureFlagsMap,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                Text(
                    text = "Live System Config",
                    color = TextPrimaryWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            AdminActionChip(
                label = "Publish Config",
                color = AccentGreen,
                icon = Icons.Default.CloudUpload,
                onClick = { publishLiveConfigNow() }
            )
        }

        // 1-Click Backend Reconciliation Desk
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0E1014), RoundedCornerShape(10.dp))
                .border(0.6.dp, AccentBlue.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "1-Click Backend Reconciliation Desk",
                        color = TextPrimaryWhite,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Reconciles deposits, withdrawals, tournament slots & Firestore ↔ RTDB sync health.",
                        color = TextSecondaryZinc,
                        fontSize = 10.sp
                    )
                }
            }
            Button(
                onClick = { viewModel.triggerBackendReconciliation() },
                enabled = !isReconcilingBackend,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                modifier = Modifier.fillMaxWidth().testTag("trigger_backend_reconciliation_btn")
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isReconcilingBackend) "RUNNING RECONCILIATION..." else "RUN BACKEND RECONCILIATION NOW",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (!reconciliationSummaryJson.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF05070A), RoundedCornerShape(8.dp))
                        .border(0.5.dp, HairlineBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = reconciliationSummaryJson,
                        color = AccentGreen,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

        // 2. Strictly 5 Active Core Feature Flags
        val activeFlagsCount = SystemConfig.ALL_FEATURE_FLAG_METADATA.size
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.ToggleOn, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                Text(
                    text = "Core Feature Flags (${featureFlagsMap.count { it.value }}/$activeFlagsCount ON)",
                    color = TextPrimaryWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            AdminActionChip(
                label = "Enable All $activeFlagsCount",
                color = AccentBlue,
                onClick = { featureFlagsMap = SystemConfig.defaultFeatureFlags() }
            )
        }

        SystemConfig.ALL_FEATURE_FLAG_METADATA.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { (flagKey, flagTitle, flagDesc) ->
                    val enabled = featureFlagsMap[flagKey] != false
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF0A0A0D), RoundedCornerShape(8.dp))
                            .border(
                                0.5.dp,
                                if (enabled) AccentGreen.copy(alpha = 0.45f) else HairlineBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                featureFlagsMap = featureFlagsMap.toMutableMap().apply {
                                    put(flagKey, !enabled)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = flagTitle,
                                color = if (enabled) TextPrimaryWhite else TextSecondaryZinc,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = flagDesc,
                                color = TextSecondaryZinc,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = if (enabled) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = flagTitle,
                            tint = if (enabled) AccentGreen else TextSecondaryZinc,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

        // 3. System Controls: Clean 2-Column Non-Overlapping Toggle Grid
        val systemToggles = listOf(
            Triple("Maintenance Mode", maintenanceMode, AccentAmber) to { v: Boolean -> maintenanceMode = v },
            Triple("Force APK Update", forceUpdateRequired, AccentRed) to { v: Boolean -> forceUpdateRequired = v },
            Triple("Block VPN / Proxy", vpnBlockEnabled, AccentGreen) to { v: Boolean -> vpnBlockEnabled = v },
            Triple("Show Home Banners", showBanners, AccentGreen) to { v: Boolean -> showBanners = v }
        )
        systemToggles.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { (meta, onToggle) ->
                    val (title, isOn, activeColor) = meta
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF0A0A0D), RoundedCornerShape(8.dp))
                            .border(
                                0.5.dp,
                                if (isOn) activeColor.copy(alpha = 0.5f) else HairlineBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onToggle(!isOn) }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            color = if (isOn) TextPrimaryWhite else TextSecondaryZinc,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        )
                        Icon(
                            imageVector = if (isOn) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                            contentDescription = title,
                            tint = if (isOn) activeColor else TextSecondaryZinc,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        OutlinedTextField(
            value = maintenanceTitle,
            onValueChange = { maintenanceTitle = it },
            label = { Text("Maintenance Title", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = maintenanceMessage,
            onValueChange = { maintenanceMessage = it },
            label = { Text("Maintenance Banner Message", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = latestVersionCode,
                onValueChange = { latestVersionCode = it },
                label = { Text("Version Code", fontSize = 11.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(0.35f)
            )
            OutlinedTextField(
                value = latestVersionName,
                onValueChange = { latestVersionName = it },
                label = { Text("Version Name", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(0.65f)
            )
        }

        OutlinedTextField(
            value = updateDownloadUrl,
            onValueChange = { updateDownloadUrl = it },
            label = { Text("APK Update Download URL", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = updateChangelog,
            onValueChange = { updateChangelog = it },
            label = { Text("Update Changelog", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // UPI & Financial Limits
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = upiId,
                onValueChange = { upiId = it },
                label = { Text("Official UPI ID", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(0.55f)
            )
            OutlinedTextField(
                value = upiMerchantName,
                onValueChange = { upiMerchantName = it },
                label = { Text("UPI Merchant Name", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(0.45f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = minDepositVt,
                onValueChange = { minDepositVt = it },
                label = { Text("Min Deposit (VT)", fontSize = 11.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = minWithdrawalVt,
                onValueChange = { minWithdrawalVt = it },
                label = { Text("Min Withdraw (VT)", fontSize = 11.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = tokenConversionRate,
                onValueChange = { tokenConversionRate = it },
                label = { Text("Tokens / 1 VT", fontSize = 11.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        // Social Links
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = whatsappUrl,
                onValueChange = { whatsappUrl = it },
                label = { Text("WhatsApp Support URL", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = telegramUrl,
                onValueChange = { telegramUrl = it },
                label = { Text("Telegram Channel URL", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = instagramUrl,
                onValueChange = { instagramUrl = it },
                label = { Text("Instagram URL", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = discordUrl,
                onValueChange = { discordUrl = it },
                label = { Text("Discord Invite URL", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ============================================================================
// MODULE 5B: TOURNAMENT LOBBY CHAT MODERATION & CHAT REPORTS QUEUE
// ============================================================================
@Composable
fun TournamentLobbyChatModeratorSection(
    viewModel: TournamentDashboardViewModel,
    tournaments: List<Tournament>,
    initialTournamentId: String? = null,
    chatReports: List<ChatReportItem>? = null
) {
    var selectedTournamentId by remember(tournaments, initialTournamentId) {
        mutableStateOf(initialTournamentId ?: tournaments.firstOrNull()?.id ?: "")
    }
    var messageInput by remember { mutableStateOf("") }
    var pinNewMessage by remember { mutableStateOf(false) }
    var chatViewMode by remember { mutableStateOf("FEED") } // FEED | REPORTS

    val liveReports by viewModel.chatReports.collectAsStateWithLifecycle()
    val effectiveReports = chatReports ?: liveReports
    val pendingReportsCount = remember(effectiveReports) {
        effectiveReports.count { !it.status.equals("RESOLVED", true) }
    }

    val chatFlow = remember(selectedTournamentId) {
        if (selectedTournamentId.isNotBlank()) {
            viewModel.getTournamentChatStream(selectedTournamentId)
        } else null
    }
    val messages by (chatFlow?.collectAsStateWithLifecycle(initialValue = emptyList())
        ?: remember { mutableStateOf(emptyList()) })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlackBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lobby Chat & Reports Moderation",
                    color = TextPrimaryWhite,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "/tournaments/{id}/chat_messages • /chat_reports",
                    color = TextSecondaryZinc,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Toggle between Live Feed and Reported Messages Queue
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "FEED" to "Live Lobby Chat (${messages.size})",
                "REPORTS" to if (pendingReportsCount > 0) "Chat Reports ($pendingReportsCount Pending)" else "Chat Reports (${effectiveReports.size})"
            ).forEach { (key, label) ->
                val sel = chatViewMode == key
                val tint = if (key == "REPORTS" && pendingReportsCount > 0) AccentRed else AccentBlue
                Box(
                    modifier = Modifier
                        .background(if (sel) tint.copy(alpha = 0.16f) else Color(0xFF0A0A0D), RoundedCornerShape(10.dp))
                        .border(0.75.dp, if (sel) tint else HairlineBorder, RoundedCornerShape(10.dp))
                        .clickable { chatViewMode = key }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = label,
                        color = if (sel) TextPrimaryWhite else TextSecondaryZinc,
                        fontSize = 11.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

        if (chatViewMode == "REPORTS") {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                ChatReportsQueueBlock(
                    viewModel = viewModel,
                    chatReports = effectiveReports
                )
            }
        } else if (tournaments.isEmpty()) {
            Text(
                text = "No tournaments available. Create a tournament first to moderate its lobby chat.",
                color = TextSecondaryZinc,
                fontSize = 12.sp
            )
        } else {
            // Tournament Selector Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tournaments.forEach { t ->
                    val isSelected = t.id == selectedTournamentId
                    Box(
                        modifier = Modifier
                            .border(
                                0.5.dp,
                                if (isSelected) AccentBlue else HairlineBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .background(
                                if (isSelected) AccentBlue.copy(alpha = 0.14f) else Color(0xFF0A0A0D),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedTournamentId = t.id }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${t.title} (${t.status})",
                            color = if (isSelected) TextPrimaryWhite else TextSecondaryZinc,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

            // Messages Feed
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    Text(
                        text = "No lobby messages yet for this tournament. Send an official VeloRix Admin announcement below.",
                        color = TextSecondaryZinc,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    messages.forEach { msg ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (msg.isPinned) {
                                        Text(
                                            text = "PINNED",
                                            color = AccentAmber,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    if (msg.isAdmin || msg.isSystemMessage) {
                                        Text(
                                            text = "[ADMIN]",
                                            color = AccentGreen,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    if (msg.isDeleted) {
                                        Text(
                                            text = "[REMOVED]",
                                            color = AccentRed,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        text = msg.senderName,
                                        color = if (msg.isAdmin || msg.isSystemMessage) AccentGreen else TextPrimaryWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = formatTimestampShort(msg.timestamp),
                                    color = TextSecondaryZinc,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = msg.displayText,
                                color = if (msg.isDeleted) TextSecondaryZinc else TextPrimaryWhite,
                                fontSize = 12.sp
                            )
                            if (!msg.isDeleted) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AdminActionChip(
                                        label = if (msg.isPinned) "Unpin" else "Pin",
                                        color = AccentAmber,
                                        onClick = {
                                            viewModel.togglePinTournamentChat(
                                                selectedTournamentId,
                                                msg.id,
                                                !msg.isPinned
                                            )
                                        }
                                    )
                                    AdminActionChip(
                                        label = "Soft Delete",
                                        color = AccentRed,
                                        onClick = {
                                            viewModel.deleteTournamentChat(selectedTournamentId, msg.id)
                                        }
                                    )
                                }
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)
                    }
                }
            }

            // Admin Message Composer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = pinNewMessage,
                    onCheckedChange = { pinNewMessage = it }
                )
                Text("Pin", color = AccentAmber, fontSize = 11.sp)
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    label = { Text("Post Official VeloRix Admin Announcement...", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = HairlineBorder,
                        focusedTextColor = TextPrimaryWhite,
                        unfocusedTextColor = TextPrimaryWhite
                    )
                )
                IconButton(
                    onClick = {
                        if (selectedTournamentId.isNotBlank() && messageInput.isNotBlank()) {
                            viewModel.sendAdminTournamentChat(
                                tournamentId = selectedTournamentId,
                                message = messageInput.trim(),
                                isPinned = pinNewMessage
                            )
                            messageInput = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send Admin Message", tint = AccentGreen)
                }
            }
        }
    }
}

// ============================================================================
// MODULE 7B: READ-ONLY AUDIT LOGS (audit_logs) & SECURITY LOGS (security_audit_logs)
// ============================================================================
@Composable
private fun AuditAndSecurityLogsViewerSection(
    auditLogs: List<AdminAuditLogItem>,
    securityAuditLogs: List<AdminAuditLogItem>
) {
    var activeLogTab by remember { mutableStateOf("AUDIT") } // AUDIT | SECURITY
    var searchQuery by remember { mutableStateOf("") }

    val activeList = remember(activeLogTab, auditLogs, securityAuditLogs, searchQuery) {
        val base = if (activeLogTab == "AUDIT") auditLogs else securityAuditLogs
        if (searchQuery.isBlank()) base
        else base.filter {
            it.action.contains(searchQuery, ignoreCase = true) ||
                it.detail.contains(searchQuery, ignoreCase = true) ||
                it.adminEmail.contains(searchQuery, ignoreCase = true) ||
                it.targetId.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                "AUDIT" to "Admin Audit (${auditLogs.size})",
                "SECURITY" to "Security Logs (${securityAuditLogs.size})"
            ).forEach { (key, label) ->
                val selected = activeLogTab == key
                val tint = if (key == "SECURITY") AccentRed else AccentBlue
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (selected) tint.copy(alpha = 0.16f) else Color(0xFF0A0A0D),
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            0.75.dp,
                            if (selected) tint else HairlineBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { activeLogTab = key }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (key == "SECURITY") Icons.Default.Security else Icons.Default.History,
                        contentDescription = label,
                        tint = if (selected) tint else TextSecondaryZinc,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        color = if (selected) TextPrimaryWhite else TextSecondaryZinc,
                        fontSize = 11.5.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Text(
            text = if (activeLogTab == "AUDIT") {
                "Read-only live feed from Firestore audit_logs & RTDB /audit_logs."
            } else {
                "Read-only live feed from Firestore security_audit_logs (auth, wallet & payout security events)."
            },
            color = TextSecondaryZinc,
            fontSize = 11.sp
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Filter by action, detail, admin email, or target ID", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("audit_logs_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = HairlineBorder,
                focusedTextColor = TextPrimaryWhite,
                unfocusedTextColor = TextPrimaryWhite
            )
        )

        HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

        if (activeList.isEmpty()) {
            Text(
                text = if (activeLogTab == "AUDIT") {
                    "No admin audit logs matched."
                } else {
                    "No security audit events recorded yet."
                },
                color = TextSecondaryZinc,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            activeList.take(100).forEach { log ->
                val accent = when {
                    activeLogTab == "SECURITY" || log.action.contains("BAN", true) || log.action.contains("REJECT", true) || log.action.contains("DELETE", true) -> AccentRed
                    log.action.contains("APPROVE", true) || log.action.contains("PAYOUT", true) || log.action.contains("RECONCIL", true) -> AccentGreen
                    else -> AccentAmber
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = log.action.ifBlank { "EVENT" },
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatTimestampShort(log.timestamp),
                            color = TextSecondaryZinc,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (log.detail.isNotBlank()) {
                        Text(
                            text = log.detail,
                            color = TextPrimaryWhite,
                            fontSize = 11.5.sp
                        )
                    }
                    Text(
                        text = "Actor: ${log.adminEmail.ifBlank { "system" }} • Target: ${log.targetId.ifBlank { "global" }}",
                        color = TextSecondaryZinc,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)
            }
        }
    }
}

private fun formatTimestampShort(millis: Long): String {
    if (millis <= 0L) return "Just now"
    return try {
        SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(millis))
    } catch (_: Exception) {
        millis.toString()
    }
}
