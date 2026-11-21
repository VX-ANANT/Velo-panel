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

// Eye-Comfortable Ergonomic Dark Palette (Anti-Halation Midnight Slate & Desaturated Accents)
private val PureBlackBg = Color(0xFF0B0F17)
private val HairlineBorder = Color(0xFF222C3C)
private val TextPrimaryWhite = Color(0xFFE6EDF3)
private val TextSecondaryZinc = Color(0xFF94A3B8)
private val AccentBlue = Color(0xFF38BDF8)
private val AccentGreen = Color(0xFF34D399)
private val AccentAmber = Color(0xFFFBBF24)
private val AccentRed = Color(0xFFF87171)

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
    DEPOSIT_QUEUE("Wallet", Icons.Default.AccountBalanceWallet),
    USERS_MODERATION("Players", Icons.Default.Group),
    DPDP_COMPLIANCE("Privacy", Icons.Default.Security),
    ADMIN_CHANNELS("Channels", Icons.Default.Hub),
    SYSTEM_CONFIG("Config", Icons.Default.Tune),
    TOKENS_MISSIONS("Missions", Icons.Default.Stars),
    LOBBY_CHAT("Chat", Icons.Default.Forum)
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
    var tournamentForChat by remember(initialTournamentIdForChat) { mutableStateOf(initialTournamentIdForChat) }
    var tournamentForStandingsModal by remember { mutableStateOf<Tournament?>(null) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val successState = uiState as? DashboardState.Success

    val selectedChannel by viewModel.selectedAdminChannel.collectAsStateWithLifecycle()
    val channelItems by viewModel.activeChannelItems.collectAsStateWithLifecycle()
    val deletionRequests by viewModel.accountDeletionRequests.collectAsStateWithLifecycle()
    val exportRequests by viewModel.dataExportRequests.collectAsStateWithLifecycle()
    val depositRequests by viewModel.depositRequests.collectAsStateWithLifecycle()
    val tokenConversions by viewModel.tokenConversions.collectAsStateWithLifecycle()
    val missions by viewModel.missions.collectAsStateWithLifecycle()
    val systemConfig by viewModel.systemConfig.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()

    val pendingDeletionsCount = remember(deletionRequests) {
        deletionRequests.count { it.status.equals("PENDING", ignoreCase = true) }
    }
    val pendingDepositsCount = remember(depositRequests, successState?.payoutRequests) {
        val dep = depositRequests.count { it.status.equals("PENDING", ignoreCase = true) }
        val wdr = (successState?.payoutRequests ?: emptyList()).count { it.status.equals("PENDING", ignoreCase = true) }
        dep + wdr
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

        // Scrollable Pill Section Selector Row (Prevents 8 tabs from crowding/overlapping)
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
                    IntegrationHubSection.DPDP_COMPLIANCE -> pendingDeletionsCount
                    IntegrationHubSection.DEPOSIT_QUEUE -> pendingDepositsCount
                    IntegrationHubSection.ADMIN_CHANNELS -> successState?.openSupportComplaintsCount ?: 0
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
                    activeSection = IntegrationHubSection.LOBBY_CHAT
                },
                onCreateTournamentClick = onCreateTournamentClick,
                onTournamentClick = onTournamentClick,
                onSettingsClick = onSettingsClick
            )
            IntegrationHubSection.DEPOSIT_QUEUE -> DepositVerificationQueueSection(
                viewModel = viewModel,
                depositRequests = depositRequests,
                payoutRequests = successState?.payoutRequests ?: emptyList()
            )
            IntegrationHubSection.USERS_MODERATION -> UsersAndModerationModuleSection(
                viewModel = viewModel,
                users = successState?.users ?: emptyList()
            )
            IntegrationHubSection.DPDP_COMPLIANCE -> DpdpAccountDeletionAndExportSection(
                viewModel = viewModel,
                deletionRequests = deletionRequests,
                exportRequests = exportRequests,
                auditLogs = auditLogs,
                users = successState?.users ?: emptyList()
            )
            IntegrationHubSection.ADMIN_CHANNELS -> AdminTenChannelsInboxSection(
                viewModel = viewModel,
                selectedChannel = selectedChannel,
                channelItems = channelItems,
                supportTickets = successState?.supportTickets ?: emptyList(),
                onJumpToSection = { activeSection = it }
            )
            IntegrationHubSection.SYSTEM_CONFIG -> SystemConfigAndBannersSection(
                viewModel = viewModel,
                systemConfig = systemConfig,
                banners = successState?.banners ?: emptyList()
            )
            IntegrationHubSection.TOKENS_MISSIONS -> TokensAndMissionsManagerSection(
                viewModel = viewModel,
                tokenConversions = tokenConversions,
                missions = missions,
                users = successState?.users ?: emptyList()
            )
            IntegrationHubSection.LOBBY_CHAT -> TournamentLobbyChatModeratorSection(
                viewModel = viewModel,
                tournaments = successState?.tournaments ?: emptyList(),
                initialTournamentId = tournamentForChat
            )
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

    val perKillRate = remember(tournament.perKillReward) {
        if (tournament.perKillReward > 0f) tournament.perKillReward.toDouble() else 0.0
    }
    val prizePoolTotal = remember(tournament.prizePool) { tournament.prizePool.toDouble() }

    LaunchedEffect(tournament.id) {
        isLoading = true
        viewModel.loadTournamentStandings(tournament) { loaded ->
            standingsList = loaded
            isLoading = false
        }
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
                .background(Color(0xFF0B0E14), RoundedCornerShape(8.dp))
                .border(0.5.dp, HairlineBorder, RoundedCornerShape(8.dp))
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
                        text = "DECLARE STANDINGS & DISTRIBUTE PRIZES",
                        color = TextPrimaryWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${tournament.title} (${tournament.id}) • Pool: ₹${prizePoolTotal.toInt()} • Per Kill: ₹${perKillRate.toInt()}",
                        color = TextSecondaryZinc,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimaryWhite)
                }
            }

            // Quick Actions Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdminActionChip(
                    label = "Auto-Fill 50/30/20%",
                    color = AccentAmber,
                    onClick = {
                        standingsList = standingsList.map { row ->
                            val rp = when (row.rank) {
                                1 -> kotlin.math.round(prizePoolTotal * 0.50)
                                2 -> kotlin.math.round(prizePoolTotal * 0.30)
                                3 -> kotlin.math.round(prizePoolTotal * 0.20)
                                else -> 0.0
                            }
                            val kp = row.kills * perKillRate
                            val tot = rp + kp
                            row.copy(rankPrize = rp, killPrize = kp, prizeWon = tot, wonAnything = tot > 0.0)
                        }
                    }
                )
                AdminActionChip(
                    label = "+ Add Player Row",
                    color = AccentBlue,
                    onClick = {
                        val nextRank = standingsList.size + 1
                        standingsList = standingsList + TournamentStandingEntry(
                            rank = nextRank,
                            userId = "",
                            username = "Player $nextRank",
                            inGameName = "Player $nextRank",
                            characterId = "",
                            slotNumber = nextRank,
                            kills = 0,
                            rankPrize = 0.0,
                            killPrize = 0.0,
                            prizeWon = 0.0
                        )
                    }
                )
                Text(
                    text = "Players: ${standingsList.size} • Total: ₹${"%.0f".format(totalCalculatedPayout)}",
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
                        text = "Loading joined players from /tournaments/${tournament.id}/participants...",
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
                        text = "No registered participants found. Tap [+ ADD PARTICIPANT ROW] above to add players manually.",
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
                        val totalWon = (entry.rankPrize + entry.killPrize).coerceAtLeast(0.0)
                        val isWinner = totalWon > 0.0
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0A0A0D), RoundedCornerShape(6.dp))
                                .border(0.5.dp, HairlineBorder, RoundedCornerShape(6.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Slot #${entry.slotNumber} • ${entry.inGameName.ifBlank { entry.username.ifBlank { "Player" } }}",
                                    color = TextPrimaryWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isWinner) "WINNER (₹${"%.0f".format(totalWon)})" else "PARTICIPATED (₹0)",
                                    color = if (isWinner) AccentGreen else TextSecondaryZinc,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Editable Identity Row (if manually added or verifying UID/IGN/FF UID)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = entry.inGameName,
                                    onValueChange = { v ->
                                        standingsList = standingsList.toMutableList().also {
                                            it[idx] = entry.copy(inGameName = v, username = v)
                                        }
                                    },
                                    label = { Text("In-Game Name", fontSize = 9.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = entry.characterId,
                                    onValueChange = { v ->
                                        standingsList = standingsList.toMutableList().also {
                                            it[idx] = entry.copy(characterId = v)
                                        }
                                    },
                                    label = { Text("Character ID", fontSize = 9.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.9f)
                                )
                                OutlinedTextField(
                                    value = entry.userId,
                                    onValueChange = { v ->
                                        standingsList = standingsList.toMutableList().also {
                                            it[idx] = entry.copy(userId = v)
                                        }
                                    },
                                    label = { Text("User UID", fontSize = 9.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.1f)
                                )
                            }

                            // Editable Rank, Kills, Rank Prize, Kill Prize Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = entry.rank.toString(),
                                    onValueChange = { v ->
                                        val r = v.toIntOrNull() ?: entry.rank
                                        standingsList = standingsList.toMutableList().also {
                                            it[idx] = entry.copy(rank = r)
                                        }
                                    },
                                    label = { Text("Rank #", fontSize = 9.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(0.7f)
                                )
                                OutlinedTextField(
                                    value = entry.kills.toString(),
                                    onValueChange = { v ->
                                        val k = (v.toIntOrNull() ?: 0).coerceAtLeast(0)
                                        val autoKillPrize = k * perKillRate
                                        val newTotal = entry.rankPrize + autoKillPrize
                                        standingsList = standingsList.toMutableList().also {
                                            it[idx] = entry.copy(
                                                kills = k,
                                                killPrize = autoKillPrize,
                                                prizeWon = newTotal,
                                                wonAnything = newTotal > 0.0
                                            )
                                        }
                                    },
                                    label = { Text("Kills", fontSize = 9.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(0.7f)
                                )
                                OutlinedTextField(
                                    value = entry.rankPrize.toInt().toString(),
                                    onValueChange = { v ->
                                        val rp = (v.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                                        val newTotal = rp + entry.killPrize
                                        standingsList = standingsList.toMutableList().also {
                                            it[idx] = entry.copy(
                                                rankPrize = rp,
                                                prizeWon = newTotal,
                                                wonAnything = newTotal > 0.0
                                            )
                                        }
                                    },
                                    label = { Text("Rank ₹", fontSize = 9.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(0.9f)
                                )
                                OutlinedTextField(
                                    value = entry.killPrize.toInt().toString(),
                                    onValueChange = { v ->
                                        val kp = (v.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                                        val newTotal = entry.rankPrize + kp
                                        standingsList = standingsList.toMutableList().also {
                                            it[idx] = entry.copy(
                                                killPrize = kp,
                                                prizeWon = newTotal,
                                                wonAnything = newTotal > 0.0
                                            )
                                        }
                                    },
                                    label = { Text("Kill ₹", fontSize = 9.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(0.9f)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

            // Publish Action Footer
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
                            viewModel.publishTournamentStandings(tournament, standingsList) { ok ->
                                isPublishing = false
                                if (ok) onDismiss()
                            }
                        }
                    },
                    enabled = !isPublishing && standingsList.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                ) {
                    Text(
                        text = if (isPublishing) "PUBLISHING & CREDITING..." else "PUBLISH FINAL STANDINGS & CREDIT WINNERS",
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
// MODULE 3: USERS, BAN / SUSPEND (/banned_users & /suspended_users) & WALLET
// ============================================================================
@Composable
private fun UsersAndModerationModuleSection(
    viewModel: TournamentDashboardViewModel,
    users: List<UserProfile>
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
                        Text(
                            text = "${u.username.ifBlank { "Player" }} (${u.id.take(12)})",
                            color = TextPrimaryWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (u.isFounder || u.founderTier.isNotBlank()) {
                            Text(
                                text = "👑 ${u.founderTier.ifBlank { "FOUNDER" }}",
                                color = AccentAmber,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Text(
                        text = when {
                            u.isBanned -> "BANNED (PERMANENT)"
                            u.isSuspended -> "SUSPENDED"
                            else -> "ACTIVE"
                        },
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
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

// ============================================================================
// MODULE 5: SUPPORT TICKETS (/reports & /support_tickets), BROADCASTS & 10 CHANNELS
// ============================================================================
@Composable
private fun AdminTenChannelsInboxSection(
    viewModel: TournamentDashboardViewModel,
    selectedChannel: AdminChannelType,
    channelItems: List<AdminChannelItem>,
    supportTickets: List<SupportTicket>,
    onJumpToSection: (IntegrationHubSection) -> Unit
) {
    var statusFilter by remember { mutableStateOf("ALL") }
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMessage by remember { mutableStateOf("") }
    var replyMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val filteredItems = remember(channelItems, statusFilter) {
        if (statusFilter == "ALL") channelItems
        else channelItems.filter { it.status.equals(statusFilter, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 5A. Support Tickets & Problem Reports (/reports & /support_tickets)
        Text(
            text = "5A. SUPPORT TICKETS & PROBLEM REPORTS (/reports & /support_tickets)",
            color = TextPrimaryWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Reply directly to user tickets & mark RESOLVED. Writes to /reports, /support_tickets, and /global_app_state.",
            color = TextSecondaryZinc,
            fontSize = 11.sp
        )

        if (supportTickets.isEmpty()) {
            Text(
                text = "No support tickets or problem reports in /reports or /support_tickets.",
                color = TextSecondaryZinc,
                fontSize = 11.sp
            )
        } else {
            supportTickets.take(25).forEach { ticket ->
                val currentReply = replyMap[ticket.id] ?: ticket.adminReply.ifBlank { ticket.adminNote }
                val isResolved = ticket.status.equals("RESOLVED", true) || ticket.status.equals("CLOSED", true)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${ticket.subject.ifBlank { ticket.tournamentTitle.ifBlank { "Support Ticket" } }} • ${ticket.username.ifBlank { ticket.userEmail }}",
                            color = TextPrimaryWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = ticket.status.uppercase(),
                            color = if (isResolved) AccentGreen else AccentAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = ticket.description,
                        color = TextSecondaryZinc,
                        fontSize = 11.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = currentReply,
                            onValueChange = { replyMap = replyMap + (ticket.id to it) },
                            label = { Text("Admin Reply", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                val updated = ticket.copy().apply {
                                    status = "RESOLVED"
                                    adminNote = currentReply.ifBlank { "Resolved by Admin" }
                                    adminReply = currentReply.ifBlank { "Resolved by Admin" }
                                    updatedAt = System.currentTimeMillis()
                                }
                                viewModel.saveComplaint(updated)
                            }
                        ) {
                            Text("REPLY & RESOLVE", color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)
            }
        }

        // 5B. Global Broadcasts (/notifications/global & /global_announcements)
        Text(
            text = "5B. GLOBAL BROADCASTS (/notifications/global & /global_app_state)",
            color = TextPrimaryWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = broadcastTitle,
                onValueChange = { broadcastTitle = it },
                label = { Text("Broadcast Title", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(0.4f)
            )
            OutlinedTextField(
                value = broadcastMessage,
                onValueChange = { broadcastMessage = it },
                label = { Text("Broadcast Message", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(0.6f)
            )
            TextButton(
                onClick = {
                    if (broadcastTitle.isNotBlank() && broadcastMessage.isNotBlank()) {
                        viewModel.sendGlobalBroadcastNotification(broadcastTitle.trim(), broadcastMessage.trim())
                        broadcastTitle = ""
                        broadcastMessage = ""
                    }
                }
            ) {
                Text("SEND", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "5C. DEDICATED ADMIN CHANNELS (10 INBOX FEEDS)",
                    color = TextPrimaryWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "/admin_channels/{channelType} • /admin_requests/{channelType}",
                    color = TextSecondaryZinc,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            TextButton(
                onClick = {
                    viewModel.selectAdminChannel(AdminChannelType.ADMIN_SITUATION_TESTS)
                    viewModel.pushTestSituationEvent("ADMIN_CHANNEL_DIAGNOSTIC")
                }
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("TEST #SITUATION", color = AccentAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 10 Channel Selector Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminChannelType.entries.forEach { ch ->
                val isSelected = selectedChannel == ch
                Box(
                    modifier = Modifier
                        .border(
                            width = 0.5.dp,
                            color = if (isSelected) TextPrimaryWhite else HairlineBorder,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .background(
                            if (isSelected) TextPrimaryWhite.copy(alpha = 0.1f) else PureBlackBg,
                            RoundedCornerShape(4.dp)
                        )
                        .clickable { viewModel.selectAdminChannel(ch) }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Column {
                        Text(
                            text = ch.hashtag,
                            color = if (isSelected) TextPrimaryWhite else AccentBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = ch.title,
                            color = TextSecondaryZinc,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

        // Active Channel Header + Deep Link Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${selectedChannel.hashtag} — ${selectedChannel.title.uppercase()}",
                    color = TextPrimaryWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${selectedChannel.description} (${filteredItems.size} items)",
                    color = TextSecondaryZinc,
                    fontSize = 11.sp
                )
            }
            when (selectedChannel) {
                AdminChannelType.ACCOUNT_DELETION, AdminChannelType.DATA_EXPORT -> {
                    TextButton(onClick = { onJumpToSection(IntegrationHubSection.DPDP_COMPLIANCE) }) {
                        Text("OPEN DPDP CONSOLE →", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                AdminChannelType.DEPOSIT_REQUESTS -> {
                    TextButton(onClick = { onJumpToSection(IntegrationHubSection.DEPOSIT_QUEUE) }) {
                        Text("OPEN UTR QUEUE →", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                AdminChannelType.TOKEN_CONVERSIONS -> {
                    TextButton(onClick = { onJumpToSection(IntegrationHubSection.TOKENS_MISSIONS) }) {
                        Text("OPEN TOKEN LEDGER →", color = AccentAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {}
            }
        }

        // Status Filter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("ALL", "PENDING", "APPROVED", "COMPLETED", "REJECTED").forEach { st ->
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

        if (filteredItems.isEmpty()) {
            Text(
                text = "No requests in ${selectedChannel.hashtag} matching '$statusFilter'. Listening live on RTDB & Firestore...",
                color = TextSecondaryZinc,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            filteredItems.forEach { item ->
                AdminChannelItemRow(
                    item = item,
                    onUpdateStatus = { newStatus, note ->
                        viewModel.updateAdminChannelItemStatus(
                            channelType = selectedChannel.channelKey,
                            itemId = item.id,
                            status = newStatus,
                            note = note
                        )
                    }
                )
                HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)
            }
        }
    }
}

@Composable
private fun AdminChannelItemRow(
    item: AdminChannelItem,
    onUpdateStatus: (String, String) -> Unit
) {
    val statusColor = when (item.status.uppercase()) {
        "APPROVED", "COMPLETED", "RESOLVED", "VERIFIED" -> AccentGreen
        "REJECTED", "RESTORED" -> AccentRed
        else -> AccentAmber
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title.ifBlank { "${item.channelType} #${item.id.take(8)}" },
                color = TextPrimaryWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = item.status.uppercase(),
                color = statusColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        if (item.subtitle.isNotBlank()) {
            Text(
                text = item.subtitle,
                color = TextSecondaryZinc,
                fontSize = 12.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "User: ${item.username.ifBlank { item.userId.ifBlank { "System" } }} • ID: ${item.id.take(12)}",
                color = TextSecondaryZinc,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = formatTimestampShort(item.createdAt),
                color = TextSecondaryZinc,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminActionChip(
                label = "Approve",
                color = AccentGreen,
                onClick = { onUpdateStatus("COMPLETED", "Approved via Admin Channel Inbox") }
            )
            AdminActionChip(
                label = "Resolve",
                color = AccentBlue,
                onClick = { onUpdateStatus("RESOLVED", "Resolved by Admin") }
            )
            AdminActionChip(
                label = "Reject",
                color = AccentRed,
                onClick = { onUpdateStatus("REJECTED", "Rejected by Admin") }
            )
        }
    }
}

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
// MODULE 2: WALLET REQUESTS — DEPOSITS (/deposit_requests) & WITHDRAWALS (/withdraw_requests)
// ============================================================================
@Composable
fun DepositVerificationQueueSection(
    viewModel: TournamentDashboardViewModel,
    depositRequests: List<DepositRequest>,
    payoutRequests: List<PayoutRequest> = emptyList()
) {
    val systemConfig by viewModel.systemConfig.collectAsStateWithLifecycle()
    var dynamicUpiId by remember(systemConfig.upiId) { mutableStateOf(systemConfig.upiId) }
    var dynamicMerchantName by remember(systemConfig.upiMerchantName) { mutableStateOf(systemConfig.upiMerchantName) }
    var filterStatus by remember { mutableStateOf("ALL") }
    val filteredDeposits = remember(depositRequests, filterStatus) {
        if (filterStatus == "ALL") depositRequests
        else depositRequests.filter { it.status.equals(filterStatus, ignoreCase = true) }
    }
    val filteredWithdrawals = remember(payoutRequests, filterStatus) {
        val wList = payoutRequests.filter { !it.isDeposit }
        if (filterStatus == "ALL") wList
        else wList.filter { it.status.equals(filterStatus, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Dynamic Merchant UPI ID Quick-Update Bar
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Dynamic Merchant UPI",
                        color = TextPrimaryWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                AdminActionChip(
                    label = "Sync UPI",
                    color = AccentGreen,
                    icon = Icons.Default.Sync,
                    onClick = {
                        viewModel.saveSystemConfig(
                            systemConfig.copy(
                                upiId = dynamicUpiId.trim(),
                                upiMerchantName = dynamicMerchantName.trim(),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = dynamicUpiId,
                    onValueChange = { dynamicUpiId = it },
                    label = { Text("UPI ID (VPA)", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(0.55f)
                )
                OutlinedTextField(
                    value = dynamicMerchantName,
                    onValueChange = { dynamicMerchantName = it },
                    label = { Text("Payee Name", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(0.45f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            listOf("ALL", "PENDING", "APPROVED", "COMPLETED", "REJECTED").forEach { st ->
                val selected = filterStatus == st
                Text(
                    text = st,
                    color = if (selected) TextPrimaryWhite else TextSecondaryZinc,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clickable { filterStatus = st }
                        .padding(vertical = 4.dp)
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

        // 2A. Deposit Verification Queue
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
            Text(
                text = "Deposits (${filteredDeposits.size})",
                color = TextPrimaryWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (filteredDeposits.isEmpty()) {
            Text(
                text = "No deposit requests matching '$filterStatus'.",
                color = TextSecondaryZinc,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            filteredDeposits.forEach { req ->
                val stColor = when (req.status.uppercase()) {
                    "APPROVED", "COMPLETED" -> AccentGreen
                    "REJECTED" -> AccentRed
                    else -> AccentAmber
                }
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
                        Text(
                            text = "₹${"%.2f".format(req.amount)} VT • ${req.username.ifBlank { req.userId }}",
                            color = TextPrimaryWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
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
                        text = "${req.paymentMethod} • UTR: ${req.utrNumber.ifBlank { req.transactionId.ifBlank { "N/A" } }}",
                        color = TextSecondaryZinc,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (req.status.equals("PENDING", ignoreCase = true)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AdminActionChip(
                                label = "Approve Deposit",
                                color = AccentGreen,
                                onClick = { viewModel.approveDepositRequest(req) },
                                iconOnly = true
                            )
                            AdminActionChip(
                                label = "Reject UTR",
                                color = AccentRed,
                                onClick = { viewModel.rejectDepositRequest(req, "Invalid or unverifiable UTR reference") },
                                iconOnly = true
                            )
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2B. Withdrawal Requests Queue
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Payments, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(18.dp))
            Text(
                text = "Withdrawals (${filteredWithdrawals.size})",
                color = TextPrimaryWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (filteredWithdrawals.isEmpty()) {
            Text(
                text = "No withdrawal requests matching '$filterStatus'.",
                color = TextSecondaryZinc,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            filteredWithdrawals.forEach { wReq ->
                val stColor = when (wReq.status.uppercase()) {
                    "APPROVED", "COMPLETED" -> AccentGreen
                    "REJECTED" -> AccentRed
                    else -> AccentAmber
                }
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
                        Text(
                            text = "₹${"%.2f".format(wReq.vtAmount)} • ${wReq.username.ifBlank { wReq.uid }}",
                            color = TextPrimaryWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = wReq.status.uppercase(),
                            color = stColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "${wReq.paymentMethod} • ${wReq.paymentId}",
                        color = TextSecondaryZinc,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (wReq.status.equals("PENDING", ignoreCase = true)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AdminActionChip(
                                label = "Approve Payout",
                                color = AccentGreen,
                                onClick = { viewModel.approvePayout(wReq) },
                                iconOnly = true
                            )
                            AdminActionChip(
                                label = "Reject & Refund",
                                color = AccentRed,
                                onClick = { viewModel.rejectPayout(wReq, "Invalid UPI details — Amount refunded to wallet") },
                                iconOnly = true
                            )
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)
            }
        }
    }
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
// SECTION 6: SYSTEM CONFIG (/system_config) & PROMOTIONAL BANNERS (/banners)
// ============================================================================
@Composable
private fun SystemConfigAndBannersSection(
    viewModel: TournamentDashboardViewModel,
    systemConfig: SystemConfig,
    banners: List<AppAnnouncementBanner>
) {
    var maintenanceMode by remember(systemConfig) { mutableStateOf(systemConfig.maintenanceMode) }
    var maintenanceTitle by remember(systemConfig) { mutableStateOf(systemConfig.maintenanceTitle) }
    var maintenanceMessage by remember(systemConfig) { mutableStateOf(systemConfig.maintenanceMessage) }
    var forceUpdateRequired by remember(systemConfig) { mutableStateOf(systemConfig.forceUpdateRequired) }
    var showBanners by remember(systemConfig) { mutableStateOf(systemConfig.showBanners) }
    var showDeveloperModal by remember(systemConfig) { mutableStateOf(systemConfig.showDeveloperModal) }
    var latestVersionCode by remember(systemConfig) { mutableStateOf(systemConfig.latestVersionCode.toString()) }
    var latestVersionName by remember(systemConfig) { mutableStateOf(systemConfig.latestVersionName) }
    var updateDownloadUrl by remember(systemConfig) { mutableStateOf(systemConfig.updateDownloadUrl) }
    var updateChangelog by remember(systemConfig) { mutableStateOf(systemConfig.updateChangelog) }
    var upiId by remember(systemConfig) { mutableStateOf(systemConfig.upiId) }
    var upiMerchantName by remember(systemConfig) { mutableStateOf(systemConfig.upiMerchantName) }
    var minDepositVt by remember(systemConfig) { mutableStateOf(systemConfig.minDepositVt.toString()) }
    var minWithdrawalVt by remember(systemConfig) { mutableStateOf(systemConfig.minWithdrawalVt.toString()) }
    var tokenConversionRate by remember(systemConfig) { mutableStateOf(systemConfig.tokenConversionRate.toString()) }
    var whatsappUrl by remember(systemConfig) { mutableStateOf(systemConfig.whatsappSupportUrl) }
    var telegramUrl by remember(systemConfig) { mutableStateOf(systemConfig.telegramChannelUrl) }
    var instagramUrl by remember(systemConfig) { mutableStateOf(systemConfig.instagramUrl) }
    var discordUrl by remember(systemConfig) { mutableStateOf(systemConfig.discordUrl) }
    var vpnBlockEnabled by remember(systemConfig) { mutableStateOf(systemConfig.vpnBlockEnabled) }

    // OTA Live Config & Calendar Festival Override State
    var otaPatchVersion by remember(systemConfig) { mutableStateOf(systemConfig.otaPatchVersion.toString()) }
    var otaReleaseNotes by remember(systemConfig) { mutableStateOf(systemConfig.otaReleaseNotes) }
    var liveLobbyTicker by remember(systemConfig) { mutableStateOf(systemConfig.liveLobbyTicker) }
    var activeFestivalOverride by remember(systemConfig) { mutableStateOf(systemConfig.activeFestivalOverride) }
    var customFestivalTitle by remember(systemConfig) { mutableStateOf(systemConfig.customFestivalTitle) }
    var customFestivalMessage by remember(systemConfig) { mutableStateOf(systemConfig.customFestivalMessage) }
    var customAttractingTitle by remember(systemConfig) { mutableStateOf(systemConfig.customAttractingTitle) }
    var customAttractingMessage by remember(systemConfig) { mutableStateOf(systemConfig.customAttractingMessage) }
    var attractingAlertWeight by remember(systemConfig) { mutableStateOf(systemConfig.attractingAlertWeight.toString()) }
    var timeContextualWeight by remember(systemConfig) { mutableStateOf(systemConfig.timeContextualWeight.toString()) }
    var weeklyCalendarWeight by remember(systemConfig) { mutableStateOf(systemConfig.weeklyCalendarWeight.toString()) }
    var featureFlagsMap by remember(systemConfig) {
        mutableStateOf(
            SystemConfig.defaultFeatureFlags().toMutableMap().apply {
                putAll(systemConfig.featureFlags)
            }.toMap()
        )
    }

    // Promotional Banner Creator State
    var bannerTitle by remember { mutableStateOf("") }
    var bannerSubtitle by remember { mutableStateOf("") }
    var bannerBadgeText by remember { mutableStateOf("OFFICIAL") }
    var bannerCtaText by remember { mutableStateOf("JOIN NOW") }
    var bannerActionType by remember { mutableStateOf("TOURNAMENT") }
    var bannerGradientTheme by remember { mutableStateOf("INDIGO_VIOLET") }
    var bannerImageUrl by remember { mutableStateOf("") }
    var bannerActionUrl by remember { mutableStateOf("") }
    var bannerPriority by remember { mutableStateOf("1") }

    fun publishLiveConfigNow() {
        viewModel.saveSystemConfig(
            systemConfig.copy(
                maintenanceMode = maintenanceMode,
                maintenanceTitle = maintenanceTitle.trim(),
                maintenanceMessage = maintenanceMessage.trim(),
                forceUpdateRequired = forceUpdateRequired,
                showBanners = showBanners,
                showDeveloperModal = showDeveloperModal,
                latestVersionCode = latestVersionCode.toIntOrNull() ?: 2,
                latestVersionName = latestVersionName.trim(),
                updateDownloadUrl = updateDownloadUrl.trim(),
                updateChangelog = updateChangelog.trim(),
                upiId = upiId.trim(),
                upiMerchantName = upiMerchantName.trim(),
                minDepositVt = minDepositVt.toDoubleOrNull() ?: 10.0,
                minWithdrawalVt = minWithdrawalVt.toDoubleOrNull() ?: 50.0,
                tokenConversionRate = tokenConversionRate.toIntOrNull() ?: 10,
                whatsappSupportUrl = whatsappUrl.trim(),
                telegramChannelUrl = telegramUrl.trim(),
                instagramUrl = instagramUrl.trim(),
                discordUrl = discordUrl.trim(),
                vpnBlockEnabled = vpnBlockEnabled,
                otaPatchVersion = (otaPatchVersion.toIntOrNull() ?: 1).coerceAtLeast(1),
                otaReleaseNotes = otaReleaseNotes.trim(),
                liveLobbyTicker = liveLobbyTicker.trim(),
                activeFestivalOverride = activeFestivalOverride,
                customFestivalTitle = customFestivalTitle.trim(),
                customFestivalMessage = customFestivalMessage.trim(),
                customAttractingTitle = customAttractingTitle.trim(),
                customAttractingMessage = customAttractingMessage.trim(),
                attractingAlertWeight = (attractingAlertWeight.toIntOrNull() ?: 65).coerceIn(0, 100),
                timeContextualWeight = (timeContextualWeight.toIntOrNull() ?: 20).coerceIn(0, 100),
                weeklyCalendarWeight = (weeklyCalendarWeight.toIntOrNull() ?: 15).coerceIn(0, 100),
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
                    text = "OTA Live Config & 14 Flags",
                    color = TextPrimaryWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            AdminActionChip(
                label = "Publish OTA v$otaPatchVersion",
                color = AccentGreen,
                icon = Icons.Default.CloudUpload,
                onClick = { publishLiveConfigNow() }
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

        // 1. OTA Patch Version + +1 Bump & Release Notes + Live Lobby Ticker
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = otaPatchVersion,
                onValueChange = { otaPatchVersion = it },
                label = { Text("OTA Patch #", fontSize = 10.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(105.dp)
            )
            AdminActionChip(
                label = "+1 Bump",
                color = AccentAmber,
                icon = Icons.Default.Add,
                onClick = {
                    val nextVer = (otaPatchVersion.toIntOrNull() ?: 1) + 1
                    otaPatchVersion = nextVer.toString()
                }
            )
            OutlinedTextField(
                value = otaReleaseNotes,
                onValueChange = { otaReleaseNotes = it },
                label = { Text("OTA Release Notes", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = liveLobbyTicker,
            onValueChange = { liveLobbyTicker = it },
            label = { Text("Live Lobby Ticker Banner Text (e.g. MEGA BERMUDA GRAND FINALS AT 9 PM!)", fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

        // 2. Calendar Festival Theme & Alert Override (21+ Festivals + AUTO + CUSTOM)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Celebration, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
            Text(
                text = "Calendar Festival Theme Override: $activeFestivalOverride",
                color = TextPrimaryWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SystemConfig.ALL_FESTIVAL_OPTIONS.forEach { (key, label) ->
                val selected = activeFestivalOverride.equals(key, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .background(
                            if (selected) AccentAmber.copy(alpha = 0.16f) else Color(0xFF0A0A0D),
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            0.5.dp,
                            if (selected) AccentAmber else HairlineBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { activeFestivalOverride = key }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (key == "AUTO") "AUTO (Calendar)" else key.replace("_", " "),
                        color = if (selected) AccentAmber else TextSecondaryZinc,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customFestivalTitle,
                onValueChange = { customFestivalTitle = it },
                label = { Text("Custom Festival Title", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(0.45f)
            )
            OutlinedTextField(
                value = customFestivalMessage,
                onValueChange = { customFestivalMessage = it },
                label = { Text("Custom Festival Alert Message", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(0.55f)
            )
        }

        // 3. Attracting Esports Alert Customizer & Probability Weights (65 / 20 / 15)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customAttractingTitle,
                onValueChange = { customAttractingTitle = it },
                label = { Text("Attracting Alert Title", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(0.45f)
            )
            OutlinedTextField(
                value = customAttractingMessage,
                onValueChange = { customAttractingMessage = it },
                label = { Text("Attracting Alert Message", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(0.55f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = attractingAlertWeight,
                onValueChange = { attractingAlertWeight = it },
                label = { Text("Attracting % (65)", fontSize = 10.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = timeContextualWeight,
                onValueChange = { timeContextualWeight = it },
                label = { Text("Time-of-Day % (20)", fontSize = 10.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = weeklyCalendarWeight,
                onValueChange = { weeklyCalendarWeight = it },
                label = { Text("Weekly % (15)", fontSize = 10.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

        // 4. All 14 Dynamic Feature Flag Switches
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
                    text = "14 Dynamic Feature Flags (${featureFlagsMap.count { it.value }}/14 ON)",
                    color = TextPrimaryWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            AdminActionChip(
                label = "Enable All 14",
                color = AccentBlue,
                onClick = {
                    featureFlagsMap = SystemConfig.defaultFeatureFlags()
                }
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
                            .background(Color(0xFF0A0A0D), RoundedCornerShape(6.dp))
                            .border(
                                0.5.dp,
                                if (enabled) AccentGreen.copy(alpha = 0.45f) else HairlineBorder,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                featureFlagsMap = featureFlagsMap.toMutableMap().apply {
                                    put(flagKey, !enabled)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
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
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

        // 5. System Situation Controls: Clean 2-Column Non-Overlapping Toggle Grid
        val systemToggles = listOf(
            Triple("Maintenance Mode", maintenanceMode, AccentAmber) to { v: Boolean -> maintenanceMode = v },
            Triple("Force APK Update", forceUpdateRequired, AccentRed) to { v: Boolean -> forceUpdateRequired = v },
            Triple("Block VPN / Proxy", vpnBlockEnabled, AccentGreen) to { v: Boolean -> vpnBlockEnabled = v },
            Triple("Show Home Banners", showBanners, AccentGreen) to { v: Boolean -> showBanners = v },
            Triple("Developer Modal", showDeveloperModal, AccentBlue) to { v: Boolean -> showDeveloperModal = v }
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
                label = { Text("Tokens per 1 VT", fontSize = 11.sp) },
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

        HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)

        // 6B. Promotional Banners (/banners/{bannerId}) with Badge, CTA, ActionType & GradientTheme
        Text(
            text = "Promotional Home Banners (/banners/{bannerId})",
            color = TextPrimaryWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = bannerTitle,
                onValueChange = { bannerTitle = it },
                label = { Text("Banner Title", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = bannerSubtitle,
                onValueChange = { bannerSubtitle = it },
                label = { Text("Subtitle", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = bannerBadgeText,
                onValueChange = { bannerBadgeText = it },
                label = { Text("Badge (HOT / LIVE)", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = bannerCtaText,
                onValueChange = { bannerCtaText = it },
                label = { Text("CTA Text", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = bannerActionType,
                onValueChange = { bannerActionType = it },
                label = { Text("Action (TOURNAMENT/WALLET)", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("INDIGO_VIOLET", "AMBER_GOLD", "EMERALD_CYBER", "CRIMSON_FIRE").forEach { theme ->
                val sel = bannerGradientTheme == theme
                Box(
                    modifier = Modifier
                        .background(if (sel) AccentBlue.copy(alpha = 0.16f) else Color(0xFF0A0A0D), RoundedCornerShape(6.dp))
                        .border(0.5.dp, if (sel) AccentBlue else HairlineBorder, RoundedCornerShape(6.dp))
                        .clickable { bannerGradientTheme = theme }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = theme,
                        color = if (sel) AccentBlue else TextSecondaryZinc,
                        fontSize = 10.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = bannerImageUrl,
                onValueChange = { bannerImageUrl = it },
                label = { Text("Banner Image URL (https://...)", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = bannerActionUrl,
                onValueChange = { bannerActionUrl = it },
                label = { Text("Target ID / Deep Link", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(0.8f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = bannerPriority,
                onValueChange = { bannerPriority = it },
                label = { Text("Order", fontSize = 10.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(90.dp)
            )
            AdminActionChip(
                label = "Publish Banner",
                color = AccentBlue,
                icon = Icons.Default.Add,
                onClick = {
                    if (bannerTitle.isNotBlank()) {
                        viewModel.savePromotionalBanner(
                            AppAnnouncementBanner(
                                id = "banner_${System.currentTimeMillis()}",
                                title = bannerTitle.trim(),
                                subtitle = bannerSubtitle.trim(),
                                message = bannerSubtitle.trim(),
                                badgeText = bannerBadgeText.trim().ifBlank { "OFFICIAL" },
                                ctaText = bannerCtaText.trim().ifBlank { "JOIN NOW" },
                                actionType = bannerActionType.trim().ifBlank { "TOURNAMENT" },
                                targetId = bannerActionUrl.trim(),
                                gradientTheme = bannerGradientTheme,
                                imageUrl = bannerImageUrl.trim(),
                                actionUrl = bannerActionUrl.trim(),
                                targetUrl = bannerActionUrl.trim(),
                                isActive = true,
                                priority = bannerPriority.toIntOrNull() ?: 1
                            )
                        )
                        bannerTitle = ""
                        bannerSubtitle = ""
                        bannerImageUrl = ""
                        bannerActionUrl = ""
                    }
                }
            )
        }

        banners.forEach { b ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "[${b.badgeText.ifBlank { "OFFICIAL" }}] ${b.title} • ${b.gradientTheme.ifBlank { "INDIGO_VIOLET" }} (#${b.priority})",
                        color = TextPrimaryWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${b.subtitle.ifBlank { b.message }} • CTA: ${b.ctaText.ifBlank { "JOIN NOW" }} (${b.actionType})",
                        color = TextSecondaryZinc,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AdminActionChip(
                    label = "Delete",
                    color = AccentRed,
                    onClick = { viewModel.deletePromotionalBanner(b.id) },
                    iconOnly = true
                )
            }
            HorizontalDivider(thickness = 0.5.dp, color = HairlineBorder)
        }
    }
}

// ============================================================================
// SECTION 4D: TOURNAMENT LOBBY CHAT & ADMIN MODERATION
// ============================================================================
@Composable
fun TournamentLobbyChatModeratorSection(
    viewModel: TournamentDashboardViewModel,
    tournaments: List<Tournament>,
    initialTournamentId: String? = null
) {
    var selectedTournamentId by remember(tournaments, initialTournamentId) {
        mutableStateOf(initialTournamentId ?: tournaments.firstOrNull()?.id ?: "")
    }
    var messageInput by remember { mutableStateOf("") }
    var pinNewMessage by remember { mutableStateOf(false) }

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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "LIVE TOURNAMENT LOBBY CHAT MODERATION",
            color = TextPrimaryWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "/tournament_chats/{tournamentId} & /tournaments/{tournamentId}/chat (RTDB & Firestore)",
            color = TextSecondaryZinc,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )

        if (tournaments.isEmpty()) {
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
                                if (isSelected) TextPrimaryWhite else HairlineBorder,
                                RoundedCornerShape(4.dp)
                            )
                            .background(
                                if (isSelected) TextPrimaryWhite.copy(alpha = 0.12f) else PureBlackBg,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { selectedTournamentId = t.id }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${t.title} (${t.status})",
                            color = if (isSelected) TextPrimaryWhite else TextSecondaryZinc,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
                        text = "No lobby messages yet for this tournament. Send an official admin announcement below.",
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
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (msg.isPinned) {
                                        Text(
                                            text = "📌 PINNED",
                                            color = AccentAmber,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    if (msg.isAdmin) {
                                        Text(
                                            text = "[OFFICIAL ADMIN]",
                                            color = AccentGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        text = msg.senderName,
                                        color = if (msg.isAdmin) AccentGreen else TextPrimaryWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
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
                                text = msg.message,
                                color = TextPrimaryWhite,
                                fontSize = 12.sp
                            )
                            Row(
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
                                    label = "Delete",
                                    color = AccentRed,
                                    onClick = {
                                        viewModel.deleteTournamentChat(selectedTournamentId, msg.id)
                                    }
                                )
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
                    label = { Text("Post Official Admin Lobby Message...", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TextPrimaryWhite,
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

private fun formatTimestampShort(millis: Long): String {
    if (millis <= 0L) return "Just now"
    return try {
        SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(millis))
    } catch (_: Exception) {
        millis.toString()
    }
}
