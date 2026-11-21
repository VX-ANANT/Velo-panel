package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.common.LiquidGlassOpticsManager
import com.example.ui.common.LiquidGlassOpticsStudio
import com.example.ui.common.FrostedGlassDialog
import com.example.ui.components.VelorixAudioLabDialog
import com.example.ui.components.GeminiApiKeyDialog
import com.example.ui.audio.rememberVelorixSoundManager
import com.kashif_e.backdrop.*
import com.kashif_e.backdrop.backdrops.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.material3.HorizontalDivider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import com.example.ui.theme.*
import com.example.ui.common.*
import com.example.domain.model.Tournament
import com.example.ui.viewmodel.DashboardState
import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun DashboardScreen(
    uiState: DashboardState,
    onTournamentClick: (String) -> Unit = {},
    onSettingsClick: (String) -> Unit,
    onCreateTournamentClick: () -> Unit,
    onVerifyClick: () -> Unit,
    onBracketClick: (String) -> Unit,
    onNavClick: (String) -> Unit,
    onToggleUserBan: (com.example.domain.model.UserProfile) -> Unit,
    onAddFunds: (com.example.domain.model.UserProfile, Double) -> Unit,
    onAddFundsBucket: ((com.example.domain.model.UserProfile, Double, com.example.domain.model.WalletBucket, String) -> Unit)? = null,
    onRecordMatchWinning: ((com.example.domain.model.UserProfile, String, String, Int, Int, Double, Double) -> Unit)? = null,
    onDeleteUser: (com.example.domain.model.UserProfile) -> Unit,
    onUpdateUser: ((com.example.domain.model.UserProfile) -> Unit)? = null,
    onRefreshUserData: (() -> Unit)? = null,
    onGrantAdmin: ((email: String, name: String, role: String) -> Unit)? = null,
    onRevokeAdmin: ((adminUid: String) -> Unit)? = null,
    onUpdateAdmin: ((com.example.domain.model.AdminRecord) -> Unit)? = null,
    onDeleteAdmin: ((adminUid: String) -> Unit)? = null,
    onSaveComplaint: ((com.example.domain.model.ComplaintTicket) -> Unit)? = null,
    onDeleteComplaint: ((String) -> Unit)? = null,
    onSendTicketMessage: ((ticketId: String, message: String, onComplete: (Boolean) -> Unit) -> Unit)? = null,
    getTicketMessagesStream: (suspend (String) -> kotlinx.coroutines.flow.Flow<List<com.example.domain.model.TicketMessage>>)? = null,
    onIssueTicketCompensation: ((ticket: com.example.domain.model.ComplaintTicket, amount: Double, reason: String, onComplete: (Boolean) -> Unit) -> Unit)? = null,
    onSaveToken: ((com.example.domain.model.CheckInToken) -> Unit)? = null,
    onSaveBanner: ((com.example.domain.model.AppAnnouncementBanner) -> Unit)? = null,
    onPublishAnnouncement: ((com.example.domain.model.GlobalAnnouncement) -> Unit)? = null,
    onDeleteAnnouncement: ((String) -> Unit)? = null,
    onUpdateTournament: ((com.example.domain.model.Tournament) -> Unit)? = null,
    onDuplicateTournament: ((com.example.domain.model.Tournament) -> Unit)? = null,
    onApprovePayout: ((com.example.domain.model.PayoutRequest) -> Unit)? = null,
    onRejectPayout: ((com.example.domain.model.PayoutRequest, String) -> Unit)? = null,
    onImportTournamentsJson: ((String) -> Unit)? = null,
    onSyncAllTournaments: (() -> Unit)? = null,
    onRefreshClick: (() -> Unit)? = null,
    onMarkNotificationRead: ((String) -> Unit)? = null,
    onClearAllNotifications: (() -> Unit)? = null,
    onTestPushNotification: (() -> Unit)? = null,
    onPublishCampaign: ((String, String, String, String?) -> Unit)? = null,
    onPurgeDemoData: (() -> Unit)? = null,
    onUnbanGuestSessions: (() -> Unit)? = null,
    viewModel: com.example.ui.viewmodel.TournamentDashboardViewModel? = null
) {
    val context = LocalContext.current
    val opticsManager = remember { LiquidGlassOpticsManager(context) }
    var selectedTab by remember { mutableStateOf("dashboard") }

    BackHandler(enabled = selectedTab != "dashboard") {
        if (selectedTab in listOf("users", "complaints", "payouts")) {
            selectedTab = "operations"
        } else if (selectedTab in listOf("leaderboard", "admins", "lowcode")) {
            selectedTab = "system"
        } else {
            selectedTab = "dashboard"
        }
    }

    var currentHyperTheme by remember { mutableStateOf(HyperOSTheme.NEBULA_PURPLE) }
    var showNotificationCenterDialog by remember { mutableStateOf(false) }
    var showBroadcastDialog by remember { mutableStateOf(false) }
    var showAdminProfileDialog by remember { mutableStateOf(false) }
    var showRateLimiterDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmationDialog by remember { mutableStateOf(false) }
    var showAudioLabDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    // Scroll Position Tracking & Blur Progress Calculation (iOS Progressive Blur Navbar)
    val dashboardScrollState = rememberScrollState()
    val blurProgress by remember {
        derivedStateOf {
            val totalScroll = dashboardScrollState.value.toFloat()
            // Progressive blur transitions smoothly over 150px of vertical scrolling
            (totalScroll / 150f).coerceIn(0f, 1f)
        }
    }

    val backdrop = rememberLayerBackdrop()

    GlassBackgroundBox(accentColor = currentHyperTheme.primaryColor) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(backdrop)
                        .padding(innerPadding)
                ) {
                if (selectedTab == "dashboard") {
                    DashboardHeader(
                        onProfileClick = { showAdminProfileDialog = true }, 
                        onNavClick = onNavClick,
                        onNotificationsClick = { showNotificationCenterDialog = true },
                        onRefreshClick = { onRefreshClick?.invoke() },
                        unreadNotificationCount = (uiState as? DashboardState.Success)?.unreadNotificationCount ?: 0,
                        currentUserEmail = (uiState as? DashboardState.Success)?.currentUserEmail,
                        accentColor = currentHyperTheme.primaryColor,
                        isSyncing = (uiState as? DashboardState.Success)?.isSyncing ?: false
                    )
                }
            
            when (uiState) {
                is DashboardState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = currentHyperTheme.primaryColor)
                    }
                }
                is DashboardState.Success -> {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing)) + 
                             slideInHorizontally(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)) { width -> (width * 0.08f).toInt() })
                            .togetherWith(
                                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing)) + 
                                slideOutHorizontally(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) { width -> (-width * 0.08f).toInt() }
                            )
                        },
                        label = "materialTabTransition",
                        modifier = Modifier.fillMaxSize()
                    ) { targetTab ->
                        when (targetTab) {
                            "dashboard" -> {
                                DashboardContent(
                                    uiState = uiState,
                                    scrollState = dashboardScrollState,
                                    onTournamentClick = onTournamentClick,
                                    onSettingsClick = onSettingsClick,
                                    onTournamentsListClick = { selectedTab = "tournaments_list" },
                                    onVerifyClick = { selectedTab = "operations" },
                                    onBracketClick = onBracketClick,
                                    onProfilesClick = { selectedTab = "operations" },
                                    onAdminsClick = { selectedTab = "admins" },
                                    onComplaintsClick = { selectedTab = "operations" },
                                    onPayoutsClick = { selectedTab = "payouts" },
                                    onCheckInTokensClick = { selectedTab = "lowcode" },
                                    onChatbotClick = { onNavClick("chatbot") },
                                    onLeaderboardClick = { selectedTab = "leaderboard" },
                                    onFinancialAuditClick = { onNavClick("daily_revenue") },
                                    onCreateTournamentClick = onCreateTournamentClick,
                                    onPublishAnnouncement = { onPublishAnnouncement?.invoke(it) },
                                    onPublishCampaign = onPublishCampaign,
                                    onDeleteAnnouncement = { onDeleteAnnouncement?.invoke(it) },
                                    onRefreshClick = { onRefreshClick?.invoke() },
                                    onOpenIntegrationHubClick = { selectedTab = "integration_hub" },
                                    viewModel = viewModel
                                )
                            }
                            "integration_hub" -> {
                                if (viewModel != null) {
                                    AdminChannelsAndIntegrationHub(
                                        viewModel = viewModel,
                                        onCreateTournamentClick = onCreateTournamentClick,
                                        onTournamentClick = onTournamentClick,
                                        onSettingsClick = onSettingsClick
                                    )
                                }
                            }
                            "tournaments_list" -> {
                                TournamentsListScreenContent(
                                    uiState = uiState,
                                    onTournamentClick = onTournamentClick,
                                    onSettingsClick = onSettingsClick,
                                    onBracketClick = onBracketClick,
                                    onDuplicateTournament = { onDuplicateTournament?.invoke(it) },
                                    onImportJson = { onImportTournamentsJson?.invoke(it) },
                                    onSyncCloud = { onSyncAllTournaments?.invoke() },
                                    onCreateTournament = onCreateTournamentClick,
                                    viewModel = viewModel
                                )
                            }
                            "operations" -> {
                                OperationsHubScreenContent(
                                    uiState = uiState,
                                    onVerifyClick = onVerifyClick,
                                    onToggleUserBan = onToggleUserBan,
                                    onAddFunds = onAddFunds,
                                    onAddFundsBucket = onAddFundsBucket,
                                    onRecordMatchWinning = onRecordMatchWinning,
                                    onDeleteUser = onDeleteUser,
                                    onUpdateUser = { onUpdateUser?.invoke(it) },
                                    onRefreshUserData = onRefreshUserData,
                                    onUnbanGuestSessions = onUnbanGuestSessions,
                                    onSaveComplaint = { onSaveComplaint?.invoke(it) },
                                    onDeleteComplaint = { onDeleteComplaint?.invoke(it) },
                                    onSendTicketMessage = onSendTicketMessage,
                                    getTicketMessagesStream = getTicketMessagesStream,
                                    onIssueTicketCompensation = onIssueTicketCompensation,
                                    accentColor = currentHyperTheme.primaryColor,
                                    viewModel = viewModel,
                                    onCreateTournamentClick = onCreateTournamentClick,
                                    onTournamentClick = onTournamentClick,
                                    onSettingsClick = onSettingsClick
                                )
                            }
                            "system" -> {
                                GlobalSettingsScreenContent(
                                    opticsManager = opticsManager,
                                    accentColor = currentHyperTheme.primaryColor,
                                    currentTheme = currentHyperTheme,
                                    onThemeSelect = { currentHyperTheme = it },
                                    onPurgeDemoData = onPurgeDemoData,
                                    currentUserEmail = (uiState as? DashboardState.Success)?.currentUserEmail,
                                    onLogout = { showLogoutConfirmationDialog = true },
                                    onNavigateTab = { selectedTab = it },
                                    onOpenAudioLab = { showAudioLabDialog = true },
                                    onOpenApiKeyDialog = { showApiKeyDialog = true },
                                    onOpenRateLimiter = { showRateLimiterDialog = true }
                                )
                            }
                            "leaderboard" -> {
                                LeaderboardScreen(
                                    players = uiState.leaderboardPlayers,
                                    onPlayerClick = { selectedTab = "operations" }
                                )
                            }
                            "apps_hub" -> {
                                SystemHubScreenContent(
                                    uiState = uiState,
                                    onNavigate = { target ->
                                        if (target == "chatbot") onNavClick("chatbot")
                                        else if (target == "sfx_lab") showAudioLabDialog = true
                                        else selectedTab = target
                                    },
                                    onPublishAnnouncement = { onPublishAnnouncement?.invoke(it) },
                                    accentColor = currentHyperTheme.primaryColor
                                )
                            }
                            "users" -> {
                                UsersManagementScreenContent(
                                    uiState = uiState,
                                    onToggleBan = onToggleUserBan,
                                    onAddFunds = onAddFunds,
                                    onAddFundsBucket = onAddFundsBucket,
                                    onRecordMatchWinning = onRecordMatchWinning,
                                    onDeleteUser = onDeleteUser,
                                    onUpdateUser = { onUpdateUser?.invoke(it) },
                                    onRefreshUserData = onRefreshUserData,
                                    onUnbanGuestSessions = onUnbanGuestSessions
                                )
                            }
                            "complaints" -> {
                                ComplaintsSupportScreen(
                                    users = uiState.users,
                                    initialTickets = uiState.supportTickets,
                                    onAddFundsToUser = onAddFunds,
                                    onAddFundsBucket = onAddFundsBucket,
                                    onSaveTicket = { ticket ->
                                        onSaveComplaint?.invoke(ticket)
                                    },
                                    onDeleteTicket = { ticketId ->
                                        onDeleteComplaint?.invoke(ticketId)
                                    },
                                    onSendTicketMessage = onSendTicketMessage,
                                    getTicketMessagesStream = getTicketMessagesStream,
                                    onIssueTicketCompensation = onIssueTicketCompensation
                                )
                            }
                            "payouts" -> {
                                var financeSubTab by remember { mutableStateOf(0) }
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val pendingUtrDeposits = viewModel?.depositRequests?.collectAsState()?.value?.count {
                                            it.status.equals("PENDING", ignoreCase = true)
                                        } ?: 0
                                        data class FinanceTabItem(val idx: Int, val icon: ImageVector, val badge: Int, val desc: String)
                                        val financeTabs = listOf(
                                            FinanceTabItem(0, Icons.Default.AccountBalanceWallet, 0, "Send Funds"),
                                            FinanceTabItem(1, Icons.Default.Payments, uiState.pendingCashoutsCount, "Payouts"),
                                            FinanceTabItem(2, Icons.Default.QrCodeScanner, pendingUtrDeposits, "UTR Queue"),
                                            FinanceTabItem(3, Icons.AutoMirrored.Filled.TrendingUp, 0, "Revenue")
                                        )
                                        financeTabs.forEach { tab ->
                                            val selected = financeSubTab == tab.idx
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { financeSubTab = tab.idx }
                                                    .padding(top = 10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = tab.icon,
                                                        contentDescription = tab.desc,
                                                        tint = if (selected) Color.White else Color(0xFF71717A),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    if (tab.badge > 0) {
                                                        Text(
                                                            text = "${tab.badge}",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFFBBF24)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(7.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .height(2.dp)
                                                        .width(36.dp)
                                                        .background(if (selected) Color.White else Color.Transparent)
                                                )
                                            }
                                        }
                                    }
                                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        when (financeSubTab) {
                                            0 -> {
                                                PlayerFundsTransferHubContent(
                                                    users = uiState.users,
                                                    onAddFundsBucket = { u, delta, bucket, reason ->
                                                        if (onAddFundsBucket != null) {
                                                            onAddFundsBucket(u, delta, bucket, reason)
                                                        } else {
                                                            onAddFunds(u, delta)
                                                        }
                                                    }
                                                )
                                            }
                                            1 -> {
                                                PayoutsManagementScreenContent(
                                                    uiState = uiState,
                                                    onApprovePayout = { req -> onApprovePayout?.invoke(req) },
                                                    onRejectPayout = { req, reason -> onRejectPayout?.invoke(req, reason) }
                                                )
                                            }
                                            2 -> {
                                                if (viewModel != null) {
                                                    val liveDeposits by viewModel.depositRequests.collectAsState()
                                                    DepositVerificationQueueSection(
                                                        viewModel = viewModel,
                                                        depositRequests = liveDeposits
                                                    )
                                                }
                                            }
                                            else -> {
                                                DailyRevenueAnalyticsScreen(
                                                    tournaments = uiState.tournaments,
                                                    users = uiState.users,
                                                    payouts = uiState.payoutRequests,
                                                    onBack = { financeSubTab = 0 },
                                                    onTournamentClick = onTournamentClick,
                                                    onBroadcastRoomCredentials = { tourneyId, rId, rPass ->
                                                        val target = uiState.tournaments.find { it.id == tourneyId }
                                                        if (target != null) {
                                                            onUpdateTournament?.invoke(
                                                                target.copy(
                                                                    roomDetails = com.example.domain.model.RoomDetails(
                                                                        roomId = rId,
                                                                        roomPassword = rPass,
                                                                        updatedAt = System.currentTimeMillis()
                                                                    )
                                                                )
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            "admins" -> {
                                StaffManagementScreenContent(
                                    uiState = uiState,
                                    onGrantAdmin = onGrantAdmin,
                                    onRevokeAdmin = onRevokeAdmin,
                                    onUpdateAdmin = onUpdateAdmin,
                                    onDeleteAdmin = onDeleteAdmin
                                )
                            }
                            "tournament_rules" -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    SubAppNavigationHeader(
                                        title = "Tournament Rules & Settings",
                                        onBack = { selectedTab = "system" },
                                        accentColor = currentHyperTheme.primaryColor
                                    )
                                    TournamentRulesManagerScreen(
                                        tournaments = uiState.tournaments,
                                        onNavigateBack = { selectedTab = "system" },
                                        onSaveTournament = { updated ->
                                            onUpdateTournament?.invoke(updated)
                                        }
                                    )
                                }
                            }
                            "lowcode" -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    SubAppNavigationHeader(
                                        title = "App Studio & Banners",
                                        onBack = { selectedTab = "system" },
                                        accentColor = currentHyperTheme.primaryColor
                                    )
                                    LowCodeAppBuilderScreen(
                                        onOpenGeminiChatbot = { onNavClick("chatbot") },
                                        initialBanners = uiState.banners,
                                        onSaveBanner = onSaveBanner
                                    )
                                }
                            }
                            "glass_optics" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    SubAppNavigationHeader(
                                        title = "Liquid Glass Optics Studio",
                                        onBack = { selectedTab = "system" },
                                        accentColor = currentHyperTheme.primaryColor
                                    )
                                    LiquidGlassOpticsStudio(
                                        opticsManager = opticsManager,
                                        accentColor = currentHyperTheme.primaryColor
                                    )
                                    Spacer(modifier = Modifier.height(100.dp))
                                }
                            }
                            "settings" -> {
                                GlobalSettingsScreenContent(
                                    opticsManager = opticsManager,
                                    accentColor = currentHyperTheme.primaryColor,
                                    currentTheme = currentHyperTheme,
                                    onThemeSelect = { currentHyperTheme = it },
                                    onPurgeDemoData = onPurgeDemoData,
                                    currentUserEmail = (uiState as? DashboardState.Success)?.currentUserEmail,
                                    onLogout = { showLogoutConfirmationDialog = true },
                                    onNavigateTab = { selectedTab = it },
                                    onOpenAudioLab = { showAudioLabDialog = true },
                                    onOpenApiKeyDialog = { showApiKeyDialog = true },
                                    onOpenRateLimiter = { showRateLimiterDialog = true }
                                )
                            }
                        }
                    }
                }
                is DashboardState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${uiState.message}", color = Color.Red)
                    }
                }
            }
        }
    }

    // Floating Liquid Glass Navigation Bar directly hovering above edge-to-edge content with Progressive Blur
    FloatingGlassNavBar(
        selectedTab = selectedTab,
        onNavClick = { tab ->
            if (tab == "logout") showLogoutConfirmationDialog = true
            else selectedTab = tab
        },
        onQuickSearchClick = {
            onNavClick("chatbot")
        },
        accentColor = currentHyperTheme.primaryColor,
        optics = opticsManager.state,
        backdrop = backdrop,
        blurProgress = 1f,
        modifier = Modifier.align(Alignment.BottomCenter)
    )

    if (showNotificationCenterDialog) {
        val successState = uiState as? DashboardState.Success
        NotificationCenterDialog(
            notifications = successState?.notifications ?: emptyList(),
            announcements = successState?.globalAnnouncements ?: emptyList(),
            tournaments = successState?.tournaments ?: emptyList(),
            users = successState?.users ?: emptyList(),
            unreadCount = successState?.unreadNotificationCount ?: 0,
            onDismiss = { showNotificationCenterDialog = false },
            onMarkRead = { notifId -> onMarkNotificationRead?.invoke(notifId) },
            onClearAll = { onClearAllNotifications?.invoke() },
            onTestPushNotification = { onTestPushNotification?.invoke() },
            onBroadcastCampaign = {
                showNotificationCenterDialog = false
                showBroadcastDialog = true
            },
            onPublishAnnouncement = { ann -> onPublishAnnouncement?.invoke(ann) },
            onPublishCampaign = onPublishCampaign,
            onTournamentClick = { tourneyId ->
                onTournamentClick(tourneyId)
            }
        )
    }

    if (showBroadcastDialog) {
        PublishAnnouncementDialog(
            onDismiss = { showBroadcastDialog = false },
            onPublish = { ann ->
                onPublishAnnouncement?.invoke(ann)
                showBroadcastDialog = false
            }
        )
    }

    if (showAdminProfileDialog) {
        val currentEmail = (uiState as? DashboardState.Success)?.currentUserEmail ?: "anantisback47@gmail.com"
        val successState = uiState as? DashboardState.Success
        AdminProfileDialog(
            email = currentEmail,
            tournaments = successState?.tournaments ?: emptyList(),
            users = successState?.users ?: emptyList(),
            admins = successState?.admins ?: emptyList(),
            onDismiss = { showAdminProfileDialog = false },
            onNavigateTab = { tab ->
                showAdminProfileDialog = false
                selectedTab = tab
            },
            onOpenAttributions = {
                showAdminProfileDialog = false
                onNavClick("attributions")
            },
            onOpenRateLimiter = {
                showAdminProfileDialog = false
                showRateLimiterDialog = true
            },
            onOpenAudioLab = {
                showAdminProfileDialog = false
                showAudioLabDialog = true
            },
            onLogoutClick = {
                showAdminProfileDialog = false
                showLogoutConfirmationDialog = true
            },
            accentColor = currentHyperTheme.primaryColor
        )
    }

    if (showRateLimiterDialog) {
        val currentEmail = (uiState as? DashboardState.Success)?.currentUserEmail ?: "system"
        RateLimiterManagementDialog(
            userIdentifier = currentEmail,
            onDismissRequest = { showRateLimiterDialog = false }
        )
    }

    if (showLogoutConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmationDialog = false },
            containerColor = Color(0xFF18181B),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out Administrator", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "Are you sure you want to log out of Velorix Super Admin? Your session will be closed and you will be returned to the sign in portal.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmationDialog = false
                        onNavClick("logout")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Yes, Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmationDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    if (showAudioLabDialog) {
        val soundManager = rememberVelorixSoundManager()
        VelorixAudioLabDialog(
            soundManager = soundManager,
            onDismissRequest = { showAudioLabDialog = false }
        )
    }

    if (showApiKeyDialog) {
        GeminiApiKeyDialog(
            onDismissRequest = { showApiKeyDialog = false },
            onKeySaved = { showApiKeyDialog = false }
        )
    }
}
}
}

@Composable
fun DashboardHeader(
    onProfileClick: () -> Unit = {},
    onNavClick: (String) -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
    unreadNotificationCount: Int = 0,
    currentUserEmail: String? = null,
    accentColor: Color = VelorixAccent,
    isSyncing: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.velorix_logo_transparent),
                    contentDescription = "Velorix Logo",
                    modifier = Modifier.size(26.dp),
                    contentScale = ContentScale.Fit
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "VELORIX",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (isSyncing) Color(0xFF38BDF8) else Color(0xFF34D399),
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (isSyncing) "Syncing" else "Live",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSyncing) Color(0xFF38BDF8) else Color(0xFF34D399)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onRefreshClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color.White,
                            strokeWidth = 1.6.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color(0xFFA1A1AA),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = if (unreadNotificationCount > 0) Color.White else Color(0xFFA1A1AA),
                            modifier = Modifier.size(18.dp)
                        )
                        if (unreadNotificationCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(6.dp)
                                    .background(Color(0xFF38BDF8), CircleShape)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { onNavClick("chatbot") },
                    modifier = Modifier.size(34.dp)
                ) {
                    GeminiLogo(size = 17.dp)
                }

                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Admin Profile",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
    }
}

@Composable
fun DashboardContent(
    uiState: DashboardState.Success,
    scrollState: ScrollState = rememberScrollState(),
    onTournamentClick: (String) -> Unit = {},
    onSettingsClick: (String) -> Unit,
    onTournamentsListClick: () -> Unit = {},
    onVerifyClick: () -> Unit,
    onBracketClick: (String) -> Unit,
    onProfilesClick: () -> Unit,
    onAdminsClick: () -> Unit,
    onComplaintsClick: () -> Unit = {},
    onPayoutsClick: () -> Unit = {},
    onCheckInTokensClick: () -> Unit = {},
    onChatbotClick: () -> Unit = {},
    onLeaderboardClick: () -> Unit = {},
    onFinancialAuditClick: () -> Unit = {},
    onCreateTournamentClick: () -> Unit = {},
    onPublishAnnouncement: ((com.example.domain.model.GlobalAnnouncement) -> Unit)? = null,
    onDeleteAnnouncement: ((String) -> Unit)? = null,
    onPublishCampaign: ((title: String, message: String, targetTopic: String, deepLink: String?) -> Unit)? = null,
    onRefreshClick: (() -> Unit)? = null,
    onOpenIntegrationHubClick: (() -> Unit)? = null,
    viewModel: com.example.ui.viewmodel.TournamentDashboardViewModel? = null
) {
    var selectedTournamentId by remember(uiState.tournaments) {
        mutableStateOf(uiState.tournaments.firstOrNull()?.id ?: "")
    }
    var showPublishAnnouncementDialog by remember { mutableStateOf(false) }
    var showFinancialAuditDialog by remember { mutableStateOf(false) }
    var showFirebaseOptimizationDialog by remember { mutableStateOf(false) }
    var tournamentForStandingsModal by remember { mutableStateOf<com.example.domain.model.Tournament?>(null) }

    var homeTab by remember { mutableStateOf(0) } // 0 = Matches, 1 = Hub, 2 = Controls
    var statusFilter by remember { mutableStateOf("ALL") }
    var showManualNotificationSection by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val adminPrefs = remember { context.getSharedPreferences("VelorixAdminSettings", android.content.Context.MODE_PRIVATE) }

    var autoApproveKyc by remember { mutableStateOf(adminPrefs.getBoolean("auto_approve_kyc", true)) }
    var instantRoomDispatch by remember { mutableStateOf(adminPrefs.getBoolean("instant_room_dispatch", true)) }
    var allowLateCheckIn by remember { mutableStateOf(adminPrefs.getBoolean("allow_late_checkin", false)) }
    var antiCheatStrictMode by remember { mutableStateOf(adminPrefs.getBoolean("anti_cheat_strict_mode", true)) }

    fun syncPlatformControl(key: String, value: Boolean) {
        adminPrefs.edit().putBoolean(key, value).apply()
        try {
            com.google.firebase.database.FirebaseDatabase.getInstance()
                .reference.child("admin_settings").child("platform_controls").child(key).setValue(value)
        } catch (_: Exception) {}
    }

    val totalPlatformDeposits = uiState.users.sumOf { maxOf(it.depositFunds, it.funds) }
    val pendingQueueTotal = uiState.pendingCashoutsCount + uiState.openSupportComplaintsCount

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Active Global Announcements Banner (if any)
        if (uiState.globalAnnouncements.isNotEmpty()) {
            GlobalAnnouncementsBanner(
                announcements = uiState.globalAnnouncements,
                isAdmin = true,
                onPublishClick = { showPublishAnnouncementDialog = true },
                onDeleteClick = { id -> onDeleteAnnouncement?.invoke(id) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 1. CLEAN 4-METRIC STRIP (Icon + Big Value + 1-Word Label)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DashboardCompactStatCell(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Group,
                iconTint = Color(0xFF38BDF8),
                value = "${uiState.totalRegisteredUsersCount}",
                label = "Players",
                onClick = onProfilesClick
            )
            Box(modifier = Modifier.width(0.5.dp).height(40.dp).background(Color.White.copy(alpha = 0.08f)))
            DashboardCompactStatCell(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.EmojiEvents,
                iconTint = Color(0xFF34D399),
                value = "${uiState.tournaments.size}",
                label = "Matches",
                onClick = onTournamentsListClick
            )
            Box(modifier = Modifier.width(0.5.dp).height(40.dp).background(Color.White.copy(alpha = 0.08f)))
            DashboardCompactStatCell(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                iconTint = Color(0xFFA855F7),
                value = "₹${String.format(java.util.Locale.US, "%,.0f", totalPlatformDeposits)}",
                label = "Deposits",
                onClick = onFinancialAuditClick
            )
            Box(modifier = Modifier.width(0.5.dp).height(40.dp).background(Color.White.copy(alpha = 0.08f)))
            DashboardCompactStatCell(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Payments,
                iconTint = if (pendingQueueTotal > 0) Color(0xFFFF9F0A) else Color(0xFFA1A1AA),
                value = "$pendingQueueTotal",
                label = "Pending",
                onClick = onPayoutsClick
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

        // 2. 4 PRIMARY QUICK ACTIONS ROW (1-Tap Icon + Short Label)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DashboardQuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Add,
                iconTint = Color(0xFF38BDF8),
                label = "New Match",
                onClick = onCreateTournamentClick
            )
            Box(modifier = Modifier.width(0.5.dp).height(34.dp).background(Color.White.copy(alpha = 0.08f)))
            DashboardQuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Payments,
                iconTint = Color(0xFF34D399),
                label = "Send Funds",
                onClick = onPayoutsClick
            )
            Box(modifier = Modifier.width(0.5.dp).height(34.dp).background(Color.White.copy(alpha = 0.08f)))
            DashboardQuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.GridView,
                iconTint = Color(0xFFFBBF24),
                label = "10 Channels",
                onClick = { onOpenIntegrationHubClick?.invoke() }
            )
            Box(modifier = Modifier.width(0.5.dp).height(34.dp).background(Color.White.copy(alpha = 0.08f)))
            DashboardQuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.Send,
                iconTint = Color(0xFFA855F7),
                label = "Broadcast",
                onClick = { showPublishAnnouncementDialog = true }
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

        // 3. CLEAN 3-SECTION MODE SELECTOR (Matches | All Tools | Controls)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val homeTabs = listOf(
                Triple(0, Icons.Default.EmojiEvents, "Matches (${uiState.tournaments.size})"),
                Triple(1, Icons.Default.Apps, "Tools"),
                Triple(2, Icons.Default.Tune, "Controls")
            )
            homeTabs.forEach { (idx, iconVec, labelText) ->
                val selected = homeTab == idx
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { homeTab = idx }
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = iconVec,
                            contentDescription = labelText,
                            tint = if (selected) Color(0xFF38BDF8) else Color(0xFF71717A),
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = labelText,
                            fontSize = 11.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else Color(0xFF71717A),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .width(48.dp)
                            .background(if (selected) Color(0xFF38BDF8) else Color.Transparent)
                    )
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

        // 4. SELECTED TAB CONTENT
        when (homeTab) {
            0 -> {
                // MATCHES VIEW
                val statuses = listOf("ALL", "UPCOMING", "LIVE", "COMPLETED")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    statuses.forEach { status ->
                        val selected = statusFilter == status
                        val count = if (status == "ALL") uiState.tournaments.size else uiState.tournaments.count {
                            if (status == "LIVE") it.status.equals("LIVE", true) || it.status.equals("ONGOING", true) || it.status.equals("ACTIVE", true)
                            else it.status.equals(status, true)
                        }
                        Text(
                            text = "${status.lowercase().replaceFirstChar { it.uppercase() }} ($count)",
                            fontSize = 11.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color(0xFF38BDF8) else Color(0xFF71717A),
                            modifier = Modifier
                                .clickable { statusFilter = status }
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                val filteredTournaments = remember(uiState.tournaments, statusFilter) {
                    if (statusFilter == "ALL") uiState.tournaments
                    else uiState.tournaments.filter {
                        if (statusFilter == "LIVE") it.status.equals("LIVE", true) || it.status.equals("ONGOING", true) || it.status.equals("ACTIVE", true)
                        else it.status.equals(statusFilter, true)
                    }
                }

                if (filteredTournaments.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFF52525B),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No matches found",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFA1A1AA)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "+ Create New Match",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.clickable { onCreateTournamentClick() }
                        )
                    }
                } else {
                    filteredTournaments.take(8).forEach { tournament ->
                        val statusDotColor = when (tournament.status.uppercase()) {
                            "LIVE", "ONGOING", "ACTIVE" -> Color(0xFF34D399)
                            "COMPLETED" -> Color(0xFFA1A1AA)
                            "CANCELLED" -> Color(0xFFF87171)
                            else -> Color(0xFF38BDF8)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTournamentId = tournament.id
                                    onTournamentClick(tournament.id)
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(statusDotColor, CircleShape)
                                    )
                                    Column {
                                        Text(
                                            text = tournament.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${tournament.format} • ${tournament.map.ifBlank { "Bermuda" }} • ${tournament.registeredPlayers}/${tournament.maxPlayers} slots • ₹${tournament.prizePool.toInt()}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF8E8E93),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (viewModel != null) {
                                                tournamentForStandingsModal = tournament
                                            } else {
                                                onSettingsClick(tournament.id)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color(0xFFFBBF24).copy(alpha = 0.12f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = "Declare Winners",
                                            tint = Color(0xFFFBBF24),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onBracketClick(tournament.id) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color.White.copy(alpha = 0.06f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountTree,
                                            contentDescription = "Bracket",
                                            tint = Color(0xFFA1A1AA),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onSettingsClick(tournament.id) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color(0xFF38BDF8).copy(alpha = 0.12f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Match",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }

                            val roomIdVal = tournament.roomDetails?.roomId.orEmpty()
                            val roomPassVal = tournament.roomDetails?.roomPassword.orEmpty()
                            if (roomIdVal.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Room: $roomIdVal  •  Pass: ${roomPassVal.ifBlank { "None" }}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF34D399),
                                    modifier = Modifier.padding(start = 15.dp)
                                )
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTournamentsListClick() }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "View All Matches (${uiState.tournaments.size})",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF38BDF8)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                }
            }

            1 -> {
                // ALL TOOLS VIEW (Clean 2-Column Visual Grid with Short Labels)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    DashboardVisualHubCell(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Group,
                        iconTint = Color(0xFF38BDF8),
                        title = "Players",
                        badge = "${uiState.totalRegisteredUsersCount}",
                        onClick = onProfilesClick
                    )
                    Box(modifier = Modifier.width(0.5.dp).height(64.dp).background(Color.White.copy(alpha = 0.08f)))
                    DashboardVisualHubCell(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Payments,
                        iconTint = Color(0xFFFF9F0A),
                        title = "Payouts",
                        badge = "${uiState.pendingCashoutsCount} pending",
                        onClick = onPayoutsClick
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    DashboardVisualHubCell(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.GridView,
                        iconTint = Color(0xFF34D399),
                        title = "10-Channel Hub",
                        badge = "Live Sync",
                        onClick = { onOpenIntegrationHubClick?.invoke() }
                    )
                    Box(modifier = Modifier.width(0.5.dp).height(64.dp).background(Color.White.copy(alpha = 0.08f)))
                    DashboardVisualHubCell(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.SupportAgent,
                        iconTint = Color(0xFFF87171),
                        title = "Support Desk",
                        badge = "${uiState.openSupportComplaintsCount} open",
                        onClick = onComplaintsClick
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    DashboardVisualHubCell(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.EmojiEvents,
                        iconTint = Color(0xFFFBBF24),
                        title = "Leaderboard",
                        badge = "Standings",
                        onClick = onLeaderboardClick
                    )
                    Box(modifier = Modifier.width(0.5.dp).height(64.dp).background(Color.White.copy(alpha = 0.08f)))
                    DashboardVisualHubCell(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        iconTint = Color(0xFFA855F7),
                        title = "Revenue",
                        badge = "₹${String.format(java.util.Locale.US, "%,.0f", totalPlatformDeposits)}",
                        onClick = onFinancialAuditClick
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    DashboardVisualHubCell(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.DashboardCustomize,
                        iconTint = Color(0xFFEC4899),
                        title = "Banners & PINs",
                        badge = "${uiState.banners.size} active",
                        onClick = onCheckInTokensClick
                    )
                    Box(modifier = Modifier.width(0.5.dp).height(64.dp).background(Color.White.copy(alpha = 0.08f)))
                    DashboardVisualHubCell(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Shield,
                        iconTint = Color(0xFF38BDF8),
                        title = "Admin Team",
                        badge = "${uiState.totalActiveAdminsCount} staff",
                        onClick = onAdminsClick
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    DashboardVisualHubCell(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.VerifiedUser,
                        iconTint = Color(0xFF34D399),
                        title = "Audit Ledger",
                        badge = "Inspect",
                        onClick = { showFinancialAuditDialog = true }
                    )
                    Box(modifier = Modifier.width(0.5.dp).height(64.dp).background(Color.White.copy(alpha = 0.08f)))
                    DashboardVisualHubCell(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Settings,
                        iconTint = Color(0xFFA1A1AA),
                        title = "Cloud Status",
                        badge = "Online",
                        onClick = { showFirebaseOptimizationDialog = true }
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
            }

            2 -> {
                // CONTROLS & PUSH NOTIFICATION VIEW
                val firebaseSuiteState by com.example.firebase.VelorixFirebaseSuite.suiteState.collectAsState()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showManualNotificationSection = !showManualNotificationSection }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Send Push Alert",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    Icon(
                        imageVector = if (showManualNotificationSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF71717A),
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(visible = showManualNotificationSection) {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        ManualNotificationComposerSection(
                            tournaments = uiState.tournaments,
                            users = uiState.users,
                            onPublishAnnouncement = onPublishAnnouncement,
                            onPublishCampaign = onPublishCampaign
                        )
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                DashboardToggleRow(
                    title = "Auto-Verify Players",
                    subtitle = "Approve Free Fire UIDs automatically",
                    checked = autoApproveKyc,
                    onCheckedChange = {
                        autoApproveKyc = it
                        syncPlatformControl("auto_approve_kyc", it)
                    }
                )
                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                DashboardToggleRow(
                    title = "Auto-Send Room ID",
                    subtitle = "Send room credentials 15m before start",
                    checked = instantRoomDispatch,
                    onCheckedChange = {
                        instantRoomDispatch = it
                        syncPlatformControl("instant_room_dispatch", it)
                    }
                )
                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                DashboardToggleRow(
                    title = "Allow Late Join",
                    subtitle = "Open unclaimed slots 5m before start",
                    checked = allowLateCheckIn,
                    onCheckedChange = {
                        allowLateCheckIn = it
                        syncPlatformControl("allow_late_checkin", it)
                    }
                )
                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                DashboardToggleRow(
                    title = "Block Emulators",
                    subtitle = "Mobile-only tournament lobbies",
                    checked = antiCheatStrictMode,
                    onCheckedChange = {
                        antiCheatStrictMode = it
                        syncPlatformControl("anti_cheat_strict_mode", it)
                    }
                )
                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                DashboardToggleRow(
                    title = "Crash Reporting",
                    subtitle = "Real-time app diagnostics",
                    checked = firebaseSuiteState.crashlyticsEnabled,
                    onCheckedChange = { com.example.firebase.VelorixFirebaseSuite.setCrashlyticsEnabled(it) }
                )
                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
            }
        }

        Spacer(modifier = Modifier.height(110.dp))

        if (showPublishAnnouncementDialog) {
            PublishAnnouncementDialog(
                tournaments = uiState.tournaments,
                users = uiState.users,
                onDismiss = { showPublishAnnouncementDialog = false },
                onPublish = { announcement ->
                    onPublishAnnouncement?.invoke(announcement)
                    showPublishAnnouncementDialog = false
                },
                onPublishCampaign = onPublishCampaign
            )
        }

        if (showFinancialAuditDialog) {
            FinancialAuditDialog(
                tournaments = uiState.tournaments,
                users = uiState.users,
                cashouts = uiState.cashouts,
                onDismiss = { showFinancialAuditDialog = false }
            )
        }

        if (showFirebaseOptimizationDialog) {
            FirebaseOptimizationDialog(
                tournaments = uiState.tournaments,
                onDismiss = { showFirebaseOptimizationDialog = false }
            )
        }

        if (tournamentForStandingsModal != null && viewModel != null) {
            DeclareStandingsAndPrizesDialog(
                tournament = tournamentForStandingsModal!!,
                viewModel = viewModel,
                onDismiss = { tournamentForStandingsModal = null }
            )
        }
    }
}

@Composable
private fun DashboardCompactStatCell(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF8E8E93),
            maxLines = 1
        )
    }
}

@Composable
private fun DashboardQuickActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFE4E4E7),
            maxLines = 1
        )
    }
}

@Composable
private fun DashboardVisualHubCell(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    badge: String,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = badge,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF8E8E93),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DashboardListActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailingText: String,
    trailingColor: Color = Color(0xFFA1A1AA),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFA1A1AA),
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF71717A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = trailingText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = trailingColor
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF52525B),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun DashboardToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF71717A)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = VelorixAccent,
                uncheckedThumbColor = Color(0xFF71717A),
                uncheckedTrackColor = Color(0xFF0A0A0D)
            )
        )
    }
}

@Composable
fun DashboardMetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    subtitle: String,
    bg: Color,
    border: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    padding: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = VelorixTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            fontSize = 10.5.sp,
            color = VelorixTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SubAppNavigationHeader(
    title: String,
    onBack: () -> Unit,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Apps",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "SYSTEM MODULE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = VelorixTextSecondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
    }
}

@Composable
fun OperationsHubScreenContent(
    uiState: DashboardState.Success,
    onVerifyClick: () -> Unit,
    onToggleUserBan: (com.example.domain.model.UserProfile) -> Unit,
    onAddFunds: (com.example.domain.model.UserProfile, Double) -> Unit,
    onAddFundsBucket: ((com.example.domain.model.UserProfile, Double, com.example.domain.model.WalletBucket, String) -> Unit)? = null,
    onRecordMatchWinning: ((com.example.domain.model.UserProfile, String, String, Int, Int, Double, Double) -> Unit)? = null,
    onDeleteUser: (com.example.domain.model.UserProfile) -> Unit,
    onUpdateUser: ((com.example.domain.model.UserProfile) -> Unit)? = null,
    onRefreshUserData: (() -> Unit)? = null,
    onUnbanGuestSessions: (() -> Unit)? = null,
    onSaveComplaint: ((com.example.domain.model.ComplaintTicket) -> Unit)? = null,
    onDeleteComplaint: ((String) -> Unit)? = null,
    onSendTicketMessage: ((ticketId: String, message: String, onComplete: (Boolean) -> Unit) -> Unit)? = null,
    getTicketMessagesStream: (suspend (String) -> kotlinx.coroutines.flow.Flow<List<com.example.domain.model.TicketMessage>>)? = null,
    onIssueTicketCompensation: ((ticket: com.example.domain.model.ComplaintTicket, amount: Double, reason: String, onComplete: (Boolean) -> Unit) -> Unit)? = null,
    accentColor: Color,
    viewModel: com.example.ui.viewmodel.TournamentDashboardViewModel? = null,
    onCreateTournamentClick: (() -> Unit)? = null,
    onTournamentClick: ((String) -> Unit)? = null,
    onSettingsClick: ((String) -> Unit)? = null
) {
    var selectedSection by remember { mutableStateOf(0) }
    val totalReferrersCount = uiState.users.count { it.referralCode.isNotBlank() || it.referralCount > 0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Scrollable Pill Sub-Module Selector (Prevents 5 sub-tabs from crowding)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            data class OpsTab(val idx: Int, val icon: ImageVector, val countText: String, val desc: String)
            val modes = listOf(
                OpsTab(0, Icons.Default.Group, "${uiState.totalRegisteredUsersCount}", "Players"),
                OpsTab(1, Icons.Default.Share, "$totalReferrersCount", "Referrals"),
                OpsTab(2, Icons.Default.SupportAgent, "${uiState.openSupportComplaintsCount}", "Support"),
                OpsTab(3, Icons.Default.EmojiEvents, "${uiState.leaderboardPlayers.size}", "Standings"),
                OpsTab(4, Icons.Default.Hub, "10", "Admin Hub")
            )
            modes.forEach { tab ->
                val selected = selectedSection == tab.idx
                Row(
                    modifier = Modifier
                        .background(
                            if (selected) Color(0xFF38BDF8).copy(alpha = 0.16f) else Color(0xFF0A0A0D),
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            0.75.dp,
                            if (selected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.12f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedSection = tab.idx }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.desc,
                        tint = if (selected) Color(0xFF38BDF8) else Color(0xFF8E8E93),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = tab.desc,
                        fontSize = 11.5.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Color.White else Color(0xFF8E8E93),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = tab.countText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) Color(0xFF38BDF8) else Color(0xFF71717A),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            when (selectedSection) {
                0 -> {
                    UsersManagementScreenContent(
                        uiState = uiState,
                        onToggleBan = onToggleUserBan,
                        onAddFunds = onAddFunds,
                        onAddFundsBucket = onAddFundsBucket,
                        onRecordMatchWinning = onRecordMatchWinning,
                        onDeleteUser = onDeleteUser,
                        onUpdateUser = { onUpdateUser?.invoke(it) },
                        onRefreshUserData = onRefreshUserData,
                        onUnbanGuestSessions = onUnbanGuestSessions
                    )
                }
                1 -> {
                    ReferralNetworkHubContent(
                        users = uiState.users,
                        tournaments = uiState.tournaments,
                        onToggleBan = onToggleUserBan,
                        onAddFunds = onAddFunds,
                        onAddFundsBucket = onAddFundsBucket,
                        onRecordMatchWinning = onRecordMatchWinning,
                        onDeleteUser = onDeleteUser,
                        onUpdateUser = { onUpdateUser?.invoke(it) }
                    )
                }
                2 -> {
                    ComplaintsSupportScreen(
                        users = uiState.users,
                        initialTickets = uiState.supportTickets,
                        onAddFundsToUser = onAddFunds,
                        onAddFundsBucket = onAddFundsBucket,
                        onSaveTicket = { ticket ->
                            onSaveComplaint?.invoke(ticket)
                        },
                        onDeleteTicket = { ticketId ->
                            onDeleteComplaint?.invoke(ticketId)
                        },
                        onSendTicketMessage = onSendTicketMessage,
                        getTicketMessagesStream = getTicketMessagesStream,
                        onIssueTicketCompensation = onIssueTicketCompensation
                    )
                }
                3 -> {
                    LeaderboardScreen(
                        players = uiState.leaderboardPlayers,
                        onPlayerClick = { selectedSection = 0 }
                    )
                }
                4 -> {
                    if (viewModel != null) {
                        AdminChannelsAndIntegrationHub(
                            viewModel = viewModel,
                            onCreateTournamentClick = onCreateTournamentClick,
                            onTournamentClick = onTournamentClick,
                            onSettingsClick = onSettingsClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReferralNetworkHubContent(
    users: List<com.example.domain.model.UserProfile>,
    tournaments: List<com.example.domain.model.Tournament> = emptyList(),
    onToggleBan: (com.example.domain.model.UserProfile) -> Unit,
    onAddFunds: (com.example.domain.model.UserProfile, Double) -> Unit,
    onAddFundsBucket: ((com.example.domain.model.UserProfile, Double, com.example.domain.model.WalletBucket, String) -> Unit)? = null,
    onRecordMatchWinning: ((com.example.domain.model.UserProfile, String, String, Int, Int, Double, Double) -> Unit)? = null,
    onDeleteUser: (com.example.domain.model.UserProfile) -> Unit,
    onUpdateUser: ((com.example.domain.model.UserProfile) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var inspectedUser by remember { mutableStateOf<com.example.domain.model.UserProfile?>(null) }
    var selectedUserForFunds by remember { mutableStateOf<com.example.domain.model.UserProfile?>(null) }
    var expandedUserId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current

    val validUsers = remember(users) {
        users.filter { !it.isGuestSession && !it.email.contains("mock.dummy") }
    }

    val activeInspectedUser = remember(validUsers, inspectedUser) {
        if (inspectedUser == null) null else validUsers.find { it.id == inspectedUser?.id } ?: inspectedUser
    }
    val activeSelectedUserForFunds = remember(validUsers, selectedUserForFunds) {
        if (selectedUserForFunds == null) null else validUsers.find { it.id == selectedUserForFunds?.id } ?: selectedUserForFunds
    }

    val activeReferrers = remember(validUsers, searchQuery) {
        validUsers.filter { u ->
            val hasCode = u.referralCode.isNotBlank()
            val hasReferredCount = u.referralCount > 0
            val hasReferredOthers = validUsers.any { other ->
                other.id != u.id && (
                    (u.referralCode.isNotBlank() && other.referredBy.equals(u.referralCode, ignoreCase = true)) ||
                    (other.referredBy.equals(u.id, ignoreCase = true)) ||
                    (other.referredBy.equals(u.username, ignoreCase = true))
                )
            }
            val matchesSearch = searchQuery.isBlank() ||
                u.username.contains(searchQuery, ignoreCase = true) ||
                u.referralCode.contains(searchQuery, ignoreCase = true) ||
                u.gameId.contains(searchQuery, ignoreCase = true) ||
                u.id.contains(searchQuery, ignoreCase = true)

            (hasCode || hasReferredCount || hasReferredOthers) && matchesSearch
        }.sortedByDescending { u ->
            val actualCount = validUsers.count { other ->
                other.id != u.id && (
                    (u.referralCode.isNotBlank() && other.referredBy.equals(u.referralCode, ignoreCase = true)) ||
                    (other.referredBy.equals(u.id, ignoreCase = true)) ||
                    (other.referredBy.equals(u.username, ignoreCase = true))
                )
            }
            maxOf(actualCount, u.referralCount)
        }
    }

    val totalCodesGenerated = remember(validUsers) { validUsers.count { it.referralCode.isNotBlank() } }
    val totalReferredUsersCount = remember(validUsers) {
        validUsers.count { it.referredBy.isNotBlank() && !it.referredBy.equals("Direct Organic Signup", ignoreCase = true) }
    }
    val totalBonusPaid = remember(validUsers) {
        validUsers.sumOf { maxOf(it.referralBonusEarned, it.bonusFunds) }
    }

    activeSelectedUserForFunds?.let { targetUser ->
        ThreeSectionPlayerFundsDialog(
            user = targetUser,
            onDismiss = { selectedUserForFunds = null },
            onSendBucketFunds = { u, delta, bucket, reason ->
                if (onAddFundsBucket != null) {
                    onAddFundsBucket(u, delta, bucket, reason)
                } else {
                    onAddFunds(u, delta)
                }
            }
        )
    }

    activeInspectedUser?.let { userToInspect ->
        UserDetailsInspectorDialog(
            user = userToInspect,
            allUsers = validUsers,
            tournaments = tournaments,
            onDismiss = { inspectedUser = null },
            onToggleBan = onToggleBan,
            onAddFunds = onAddFunds,
            onAddFundsBucket = onAddFundsBucket,
            onRecordMatchWinning = onRecordMatchWinning,
            onUpdateUser = { updated ->
                onUpdateUser?.invoke(updated)
                inspectedUser = updated
            },
            onDeleteUser = {
                onDeleteUser(it)
                inspectedUser = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
    ) {
        // Flat Hairline KPI Strip (Zero Boxes)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("CODES ACTIVE", fontSize = 9.sp, color = VelorixTextSecondary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text("$totalCodesGenerated", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Box(modifier = Modifier.width(0.5.dp).height(32.dp).background(Color.White.copy(alpha = 0.08f)))
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text("REFERRED", fontSize = 9.sp, color = VelorixTextSecondary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text("$totalReferredUsersCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
            }
            Box(modifier = Modifier.width(0.5.dp).height(32.dp).background(Color.White.copy(alpha = 0.08f)))
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text("BONUS PAID", fontSize = 9.sp, color = VelorixTextSecondary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text("₹${totalBonusPaid.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by player IGN, referral code, or Free Fire UID...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White.copy(alpha = 0.28f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
                focusedContainerColor = Color.Black,
                unfocusedContainerColor = Color.Black,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

        if (activeReferrers.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.AccountTree, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No Matching Referral Records", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "When players generate their referral codes and invite others, their network tree will appear here.",
                    color = VelorixTextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            activeReferrers.forEach { referrer ->
                val resolvedReferred = remember(validUsers, referrer) {
                    validUsers.filter { other ->
                        other.id != referrer.id && (
                            (referrer.referralCode.isNotBlank() && other.referredBy.equals(referrer.referralCode, ignoreCase = true)) ||
                            (other.referredBy.equals(referrer.id, ignoreCase = true)) ||
                            (other.referredBy.equals(referrer.username, ignoreCase = true)) ||
                            (other.rawAttributes["referredByCode"]?.equals(referrer.referralCode, ignoreCase = true) == true) ||
                            (other.rawAttributes["referrerId"]?.equals(referrer.id, ignoreCase = true) == true)
                        )
                    }
                }
                val isExpanded = expandedUserId == referrer.id

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = referrer.bestDisplayName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                            Text(
                                text = "IGN: ${referrer.effectiveIgn} • UID: ${referrer.id.take(8)}...",
                                color = VelorixTextSecondary,
                                fontSize = 10.5.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = referrer.referralCode.ifBlank { "NO CODE" },
                                color = Color(0xFF38BDF8),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    if (referrer.referralCode.isNotBlank()) {
                                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(referrer.referralCode))
                                        android.widget.Toast.makeText(context, "Code copied: ${referrer.referralCode}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            Text(
                                text = "${maxOf(resolvedReferred.size, referrer.referralCount)} Referred • ₹${referrer.referralBonusEarned.toInt()} Bonus",
                                color = Color(0xFF34D399),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .border(0.75.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                                .clickable { inspectedUser = referrer }
                                .padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Profile",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Row(
                            modifier = Modifier
                                .background(Color(0xFFFBBF24).copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                                .border(0.75.dp, Color(0xFFFBBF24).copy(alpha = 0.48f), RoundedCornerShape(10.dp))
                                .clickable { selectedUserForFunds = referrer }
                                .padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                            Text(
                                text = "Send Funds",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Row(
                            modifier = Modifier
                                .background(VelorixAccent.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                                .border(0.75.dp, VelorixAccent.copy(alpha = 0.48f), RoundedCornerShape(10.dp))
                                .clickable {
                                    expandedUserId = if (isExpanded) null else referrer.id
                                }
                                .padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(14.dp))
                            Text(
                                text = if (isExpanded) "Hide (${resolvedReferred.size})" else "Referred (${resolvedReferred.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = VelorixAccent,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.06f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "ONBOARDED BY ${referrer.bestDisplayName.uppercase()} (${resolvedReferred.size})",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (resolvedReferred.isEmpty()) {
                            Text(
                                "No registered player accounts linked under code '${referrer.referralCode}' yet.",
                                fontSize = 11.sp,
                                color = VelorixTextSecondary
                            )
                        } else {
                            resolvedReferred.forEachIndexed { rIdx, rUser ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { inspectedUser = rUser }
                                        .padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Text("${rIdx + 1}.", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(rUser.bestDisplayName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            Text("IGN: ${rUser.effectiveIgn} • FF UID: ${rUser.gameId.ifBlank { "Unlinked" }}", color = VelorixTextSecondary, fontSize = 10.sp)
                                        }
                                    }
                                    Text(
                                        text = "VERIFIED",
                                        color = Color(0xFF34D399),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (rIdx < resolvedReferred.lastIndex) {
                                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.05f))
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
            }
        }
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun SystemHubScreenContent(
    uiState: DashboardState.Success,
    onNavigate: (String) -> Unit,
    onPublishAnnouncement: ((com.example.domain.model.GlobalAnnouncement) -> Unit)? = null,
    accentColor: Color
) {
    var searchQuery by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val systemItems = listOf(
        AppHubItem(
            id = "integration_hub",
            title = "10 Channels, DPDP & System Config",
            description = "Manage #account-deletions, #data-exports, UTR deposits, tokens, missions, remote config & lobby chat",
            badge = "10 Channels",
            icon = Icons.Default.GridView,
            iconGradient = listOf(Color(0xFF34D399), Color(0xFF38BDF8))
        ),
        AppHubItem(
            id = "admins",
            title = "Staff & RBAC Roles",
            description = "Manage administrative permissions, invite admins & view access logs",
            badge = "${uiState.totalActiveAdminsCount} Admins",
            icon = Icons.Default.Shield,
            iconGradient = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
        ),
        AppHubItem(
            id = "tournament_rules",
            title = "Rules & Match Specs",
            description = "Gun restrictions, custom character limits & device policies",
            badge = "${uiState.tournaments.size} Rulesets",
            icon = Icons.Default.MilitaryTech,
            iconGradient = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))
        ),
        AppHubItem(
            id = "lowcode",
            title = "Check-In PINs & Banners",
            description = "Generate 6-digit match passcodes & homepage announcement slides",
            badge = "${uiState.banners.size} Slides",
            icon = Icons.Default.DashboardCustomize,
            iconGradient = listOf(Color(0xFFEC4899), Color(0xFFD946EF))
        ),
        AppHubItem(
            id = "leaderboard",
            title = "Player Leaderboards",
            description = "Real-time rank standings, points calculation & Hall of Fame",
            badge = "Live Ranks",
            icon = Icons.Default.EmojiEvents,
            iconGradient = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
        ),
        AppHubItem(
            id = "chatbot",
            title = "Gemini AI Assistant",
            description = "AI mediator for disputes, rule explanations & automated responses",
            badge = "Gemini Pro",
            icon = Icons.Default.AutoAwesome,
            iconGradient = listOf(Color(0xFFA855F7), Color(0xFF6366F1))
        ),
        AppHubItem(
            id = "sfx_lab",
            title = "SFX Audio Lab",
            description = "Test acoustic UI feedback textures and master volume",
            badge = "Audio",
            icon = Icons.Default.Headphones,
            iconGradient = listOf(Color(0xFFD49A3D), Color(0xFFB45309))
        ),
        AppHubItem(
            id = "settings",
            title = "Theme & Preferences",
            description = "System toggles, auto-registration policies & dark/light palettes",
            badge = "Settings",
            icon = Icons.Default.Settings,
            iconGradient = listOf(Color(0xFF71717A), Color(0xFF52525B))
        )
    )

    val filterQuery = searchQuery.trim().lowercase()
    val filteredItems = systemItems.filter {
        filterQuery.isEmpty() || it.title.lowercase().contains(filterQuery) || it.description.lowercase().contains(filterQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search tools & system configurations...", color = Color(0xFF71717A), fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFFA1A1AA), modifier = Modifier.size(18.dp))
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.25f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
                focusedContainerColor = Color.Black,
                unfocusedContainerColor = Color.Black
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "SYSTEM & TOOLS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = VelorixTextSecondary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

        filteredItems.forEach { item ->
            AppHubRowTile(
                item = item,
                accentColor = accentColor,
                onClick = { onNavigate(item.id) }
            )
            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun AppsHubScreenContent(
    uiState: DashboardState.Success,
    onAppClick: (String) -> Unit,
    accentColor: Color
) {
    SystemHubScreenContent(
        uiState = uiState,
        onNavigate = onAppClick,
        accentColor = accentColor
    )
}

data class AppHubItem(
    val id: String,
    val title: String,
    val description: String,
    val badge: String,
    val icon: ImageVector,
    val iconGradient: List<Color>,
    val isAlert: Boolean = false
)

@Composable
fun AppHubRowTile(
    item: AppHubItem,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 13.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = Color(0xFFD4D4D8),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.badge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isAlert) Color(0xFFEF4444) else Color(0xFF71717A)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.description,
                fontSize = 11.sp,
                color = VelorixTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Open",
            tint = Color(0xFF52525B),
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
fun AdminProfileDialog(
    email: String,
    tournaments: List<com.example.domain.model.Tournament> = emptyList(),
    users: List<com.example.domain.model.UserProfile> = emptyList(),
    admins: List<com.example.domain.model.AdminRecord> = emptyList(),
    onDismiss: () -> Unit,
    onNavigateTab: (String) -> Unit,
    onOpenAttributions: () -> Unit = {},
    onOpenRateLimiter: () -> Unit = {},
    onOpenAudioLab: () -> Unit = {},
    onLogoutClick: () -> Unit,
    accentColor: Color
) {
    val totalMatchesHosted = tournaments.size
    val completedMatches = tournaments.count { it.status.equals("completed", ignoreCase = true) || it.status.equals("ended", ignoreCase = true) }
    val liveMatches = tournaments.count { it.status.equals("live", ignoreCase = true) }
    val upcomingMatches = tournaments.count { it.status.equals("upcoming", ignoreCase = true) }
    val totalPrizeDistributed = tournaments.filter { it.status.equals("completed", ignoreCase = true) || it.status.equals("ended", ignoreCase = true) }.sumOf { it.prizePool.toDouble() }
    val totalPrizePool = tournaments.sumOf { it.prizePool.toDouble() }
    val totalPlayersServed = tournaments.sumOf { it.registeredPlayers }
    val completionRate = if (totalMatchesHosted > 0) ((completedMatches.toFloat() / totalMatchesHosted) * 100).toInt() else 100
    val adminRecord = admins.find { it.email.trim().equals(email.trim(), ignoreCase = true) }
    val adminRole = adminRecord?.role?.uppercase() ?: "SUPER ADMINISTRATOR"

    FrostedGlassDialog(
        onDismissRequest = onDismiss,
        maxWidth = 500.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatarView(
                        avatarUrl = null,
                        name = email,
                        size = 42.dp,
                        role = "super_admin",
                        isActiveRecently = true,
                        showGlow = false
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Admin Profile",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = adminRole,
                            color = Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Authenticated Admin Identity (Flat Row)
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text("AUTHENTICATED ADMIN ID", color = Color(0xFF71717A), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(email, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Full Realtime Access • Root RBAC Permissions", color = Color(0xFF10B981), fontSize = 10.sp)
                }

                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                // Hairline-Divided 2x2 Match Hosting Metrics Grid (Zero Nested Boxes)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f).padding(vertical = 10.dp)) {
                        Text("HOSTED", color = Color(0xFFA78BFA), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        Text("$totalMatchesHosted", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("Total Matches", color = Color(0xFF71717A), fontSize = 9.5.sp)
                    }
                    Box(modifier = Modifier.width(0.5.dp).height(44.dp).background(Color.White.copy(alpha = 0.08f)))
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp, top = 10.dp, bottom = 10.dp)) {
                        Text("COMPLETED", color = Color(0xFF34D399), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        Text("$completedMatches", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("Success: $completionRate%", color = Color(0xFF71717A), fontSize = 9.5.sp)
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f).padding(vertical = 10.dp)) {
                        Text("PRIZE POOL", color = Color(0xFFFBBF24), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        Text("₹${totalPrizePool.toInt()}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("₹${totalPrizeDistributed.toInt()} paid", color = Color(0xFF71717A), fontSize = 9.5.sp)
                    }
                    Box(modifier = Modifier.width(0.5.dp).height(44.dp).background(Color.White.copy(alpha = 0.08f)))
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp, top = 10.dp, bottom = 10.dp)) {
                        Text("PLAYERS", color = Color(0xFF38BDF8), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        Text("$totalPlayersServed", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("Live: $liveMatches • Up: $upcomingMatches", color = Color(0xFF71717A), fontSize = 9.5.sp)
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                // Quick Navigation Links (Flat Hairline Rows)
                val navLinks = listOf(
                    Triple("Tournaments & Matches Center", Icons.Default.EmojiEvents) { onNavigateTab("tournaments_list") },
                    Triple("Staff & RBAC Roles", Icons.Default.Shield) { onNavigateTab("admins") },
                    Triple("Rate Limiter & Quota Engine", UntitledIcons.Sliders) { onOpenRateLimiter() },
                    Triple("SFX Audio Lab", Icons.Default.Headphones) { onOpenAudioLab() },
                    Triple("Attributions & Tech Credits", Icons.Default.Info) { onOpenAttributions() }
                )

                navLinks.forEach { (title, icon, action) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { action() }
                            .padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, contentDescription = null, tint = Color(0xFFA1A1AA), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(title, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF52525B), modifier = Modifier.size(14.dp))
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                }

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(
                    onClick = onLogoutClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Log Out", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Out Administrator", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
