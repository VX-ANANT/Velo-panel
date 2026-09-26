package com.example.ui

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.platform.LocalContext
import com.example.ui.common.LiquidGlassOpticsManager
import com.example.ui.common.LiquidGlassOpticsStudio
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
    onApprovePayout: ((com.example.domain.model.PayoutRequest) -> Unit)? = null,
    onRejectPayout: ((com.example.domain.model.PayoutRequest, String) -> Unit)? = null,
    onImportTournamentsJson: ((String) -> Unit)? = null,
    onSyncAllTournaments: (() -> Unit)? = null,
    onRefreshClick: (() -> Unit)? = null,
    onMarkNotificationRead: ((String) -> Unit)? = null,
    onClearAllNotifications: (() -> Unit)? = null,
    onTestPushNotification: (() -> Unit)? = null,
    onPublishCampaign: ((String, String, String, String?) -> Unit)? = null,
    onPurgeDemoData: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val opticsManager = remember { LiquidGlassOpticsManager(context) }
    var selectedTab by remember { mutableStateOf("dashboard") }
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
                                    onDeleteAnnouncement = { onDeleteAnnouncement?.invoke(it) },
                                    onRefreshClick = { onRefreshClick?.invoke() }
                                )
                            }
                            "tournaments_list" -> {
                                TournamentsListScreenContent(
                                    uiState = uiState,
                                    onTournamentClick = onTournamentClick,
                                    onSettingsClick = onSettingsClick,
                                    onBracketClick = onBracketClick,
                                    onImportJson = { onImportTournamentsJson?.invoke(it) },
                                    onSyncCloud = { onSyncAllTournaments?.invoke() }
                                )
                            }
                            "operations" -> {
                                OperationsHubScreenContent(
                                    uiState = uiState,
                                    onVerifyClick = onVerifyClick,
                                    onToggleUserBan = onToggleUserBan,
                                    onAddFunds = onAddFunds,
                                    onDeleteUser = onDeleteUser,
                                    onUpdateUser = { onUpdateUser?.invoke(it) },
                                    onRefreshUserData = onRefreshUserData,
                                    onSaveComplaint = { onSaveComplaint?.invoke(it) },
                                    onDeleteComplaint = { onDeleteComplaint?.invoke(it) },
                                    onSendTicketMessage = onSendTicketMessage,
                                    getTicketMessagesStream = getTicketMessagesStream,
                                    onIssueTicketCompensation = onIssueTicketCompensation,
                                    accentColor = currentHyperTheme.primaryColor
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
                                    onNavigateTab = { target ->
                                        if (target == "chatbot") onNavClick("chatbot")
                                        else if (target == "sfx_lab") showAudioLabDialog = true
                                        else selectedTab = target
                                    },
                                    onOpenAudioLab = { showAudioLabDialog = true },
                                    onOpenApiKeyDialog = { showApiKeyDialog = true },
                                    onOpenRateLimiter = { showRateLimiterDialog = true }
                                )
                            }
                            "leaderboard" -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    SubAppNavigationHeader(
                                        title = "Leaderboard & Rankings",
                                        onBack = { selectedTab = "system" },
                                        accentColor = currentHyperTheme.primaryColor
                                    )
                                    LeaderboardScreen(
                                        players = uiState.leaderboardPlayers,
                                        onPlayerClick = { selectedTab = "operations" }
                                    )
                                }
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
                                Column(modifier = Modifier.fillMaxSize()) {
                                    SubAppNavigationHeader(
                                        title = "User Directory",
                                        onBack = { selectedTab = "operations" },
                                        accentColor = currentHyperTheme.primaryColor
                                    )
                                    UsersManagementScreenContent(
                                        uiState = uiState,
                                        onToggleBan = onToggleUserBan,
                                        onAddFunds = onAddFunds,
                                        onDeleteUser = onDeleteUser,
                                        onUpdateUser = { onUpdateUser?.invoke(it) },
                                        onRefreshUserData = onRefreshUserData
                                    )
                                }
                            }
                            "complaints" -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    SubAppNavigationHeader(
                                        title = "Support & Tickets",
                                        onBack = { selectedTab = "operations" },
                                        accentColor = currentHyperTheme.primaryColor
                                    )
                                    ComplaintsSupportScreen(
                                        users = uiState.users,
                                        initialTickets = uiState.supportTickets,
                                        onAddFundsToUser = onAddFunds,
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
                            }
                            "payouts" -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    PayoutsManagementScreenContent(
                                        uiState = uiState,
                                        onApprovePayout = { req ->
                                            onApprovePayout?.invoke(req)
                                        },
                                        onRejectPayout = { req, reason ->
                                            onRejectPayout?.invoke(req, reason)
                                        }
                                    )
                                }
                            }
                            "admins" -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    SubAppNavigationHeader(
                                        title = "Staff & Admin Center",
                                        onBack = { selectedTab = "system" },
                                        accentColor = currentHyperTheme.primaryColor
                                    )
                                    StaffManagementScreenContent(
                                        uiState = uiState,
                                        onGrantAdmin = onGrantAdmin,
                                        onRevokeAdmin = onRevokeAdmin,
                                        onUpdateAdmin = onUpdateAdmin,
                                        onDeleteAdmin = onDeleteAdmin
                                    )
                                }
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
                                Column(modifier = Modifier.fillMaxSize()) {
                                    SubAppNavigationHeader(
                                        title = "Settings",
                                        onBack = { selectedTab = "system" },
                                        accentColor = currentHyperTheme.primaryColor
                                    )
                                    GlobalSettingsScreenContent(
                                        opticsManager = opticsManager,
                                        accentColor = currentHyperTheme.primaryColor,
                                        currentTheme = currentHyperTheme,
                                        onThemeSelect = { currentHyperTheme = it },
                                        onPurgeDemoData = onPurgeDemoData,
                                        currentUserEmail = (uiState as? DashboardState.Success)?.currentUserEmail,
                                        onLogout = { showLogoutConfirmationDialog = true },
                                        onNavigateTab = { target ->
                                            if (target == "chatbot") onNavClick("chatbot")
                                            else if (target == "sfx_lab") showAudioLabDialog = true
                                            else selectedTab = target
                                        },
                                        onOpenAudioLab = { showAudioLabDialog = true },
                                        onOpenApiKeyDialog = { showApiKeyDialog = true },
                                        onOpenRateLimiter = { showRateLimiterDialog = true }
                                    )
                                }
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
        blurProgress = if (selectedTab == "dashboard") blurProgress else 1f,
        modifier = Modifier.align(Alignment.BottomCenter)
    )

    if (showNotificationCenterDialog) {
        val successState = uiState as? DashboardState.Success
        NotificationCenterDialog(
            notifications = successState?.notifications ?: emptyList(),
            announcements = successState?.globalAnnouncements ?: emptyList(),
            unreadCount = successState?.unreadNotificationCount ?: 0,
            onDismiss = { showNotificationCenterDialog = false },
            onMarkRead = { notifId -> onMarkNotificationRead?.invoke(notifId) },
            onClearAll = { onClearAllNotifications?.invoke() },
            onTestPushNotification = { onTestPushNotification?.invoke() },
            onBroadcastCampaign = {
                showNotificationCenterDialog = false
                showBroadcastDialog = true
            },
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
        AdminProfileDialog(
            email = currentEmail,
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                brush = GlassTokens.GlassBorderSubtle,
                shape = RoundedCornerShape(16.dp)
            ),
        color = Color(0xFF09090B)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF18181B), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF27272A), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Admin Shield",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "VELORIX",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = currentUserEmail ?: "admin@velorix.gg",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = VelorixTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Sync / Refresh Button
                val spinAngle by if (isSyncing) {
                    val infiniteTransition = rememberInfiniteTransition(label = "sync_spin")
                    infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "spin_angle"
                    )
                } else {
                    remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
                }

                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .border(
                            1.dp,
                            if (isSyncing) VelorixAccent else Color(0xFF27272A),
                            RoundedCornerShape(10.dp)
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .bounceClick(scaleDown = 0.90f) { onRefreshClick() },
                    color = Color(0xFF141416)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Realtime Data",
                            tint = if (isSyncing) VelorixAccent else Color.White,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(if (isSyncing) spinAngle else 0f)
                        )
                    }
                }

                // Notification Bell with Unread Badge
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .border(
                            1.dp,
                            if (unreadNotificationCount > 0) VelorixAccent.copy(alpha = 0.5f) else Color(0xFF27272A),
                            RoundedCornerShape(10.dp)
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .bounceClick(scaleDown = 0.90f) { onNotificationsClick() },
                    color = Color(0xFF141416)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = if (unreadNotificationCount > 0) VelorixAccent else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        if (unreadNotificationCount > 0) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(7.dp),
                                shape = CircleShape,
                                color = VelorixAccent
                            ) {}
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, Color(0xFF27272A), RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .bounceClick(scaleDown = 0.90f) { onNavClick("chatbot") },
                    color = Color(0xFF141416)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        GeminiLogo(
                            size = 18.dp
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .bounceClick(scaleDown = 0.90f) { onProfileClick() },
                    color = Color.Transparent
                ) {
                    UserAvatarView(
                        avatarUrl = null,
                        name = currentUserEmail ?: "Admin",
                        size = 36.dp,
                        role = "super_admin",
                        isActiveRecently = true,
                        showGlow = false
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardContent(
    uiState: DashboardState.Success,
    scrollState: ScrollState = rememberScrollState(),
    onTournamentClick: (String) -> Unit = {},
    onSettingsClick: (String) -> Unit,
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
    onRefreshClick: (() -> Unit)? = null
) {
    val liveTournament = uiState.tournaments.firstOrNull()
    var showPublishAnnouncementDialog by remember { mutableStateOf(false) }
    var showFinancialAuditDialog by remember { mutableStateOf(false) }
    var showFirebaseOptimizationDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val screenWidth = maxWidth
        val isTablet = screenWidth >= 600.dp
        val isCompact = screenWidth < 360.dp

        val horizontalPadding = if (isCompact) 12.dp else 16.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = horizontalPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Live Global Announcements Banner (if active)
            if (uiState.globalAnnouncements.isNotEmpty()) {
                GlobalAnnouncementsBanner(
                    announcements = uiState.globalAnnouncements,
                    isAdmin = true,
                    onPublishClick = { showPublishAnnouncementDialog = true },
                    onDeleteClick = { id -> onDeleteAnnouncement?.invoke(id) }
                )
            }

            // 2. System Status & Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Text(
                        text = "EXECUTIVE OVERVIEW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelorixTextSecondary,
                        letterSpacing = 1.sp
                    )
                }
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .bounceClick(scaleDown = 0.95f) { onRefreshClick?.invoke() },
                    color = if (uiState.isSyncing) Color(0xFF00E5FF).copy(alpha = 0.12f) else Color(0xFF10B981).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, if (uiState.isSyncing) Color(0xFF00E5FF).copy(alpha = 0.35f) else Color(0xFF10B981).copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isSyncing) Color(0xFF00E5FF) else Color(0xFF10B981))
                        )
                        Text(
                            text = if (uiState.isSyncing) "EXTRACTING..." else "LIVE CLOUD SYNC",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isSyncing) Color(0xFF00E5FF) else Color(0xFF10B981)
                        )
                    }
                }
            }

            // 3. Overview 4-Key Metrics Grid (Crisp & Clean)
            val liveTournamentsCount = remember(uiState.tournaments) {
                uiState.tournaments.count { it.status.equals("active", ignoreCase = true) || it.status.equals("live", ignoreCase = true) }
            }
            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UntitledMetricTile(
                        modifier = Modifier.weight(1f),
                        title = "PLAYERS",
                        value = "${uiState.totalRegisteredUsersCount}",
                        subtitle = "Active Gamers",
                        icon = UntitledIcons.Users,
                        iconTint = Color(0xFF38BDF8),
                        badgeText = "Total",
                        onClick = onProfilesClick
                    )
                    UntitledMetricTile(
                        modifier = Modifier.weight(1f),
                        title = "MATCHES",
                        value = "${uiState.tournaments.size}",
                        subtitle = if (liveTournamentsCount > 0) "$liveTournamentsCount Live Now" else "Scheduled",
                        icon = UntitledIcons.Trophy,
                        iconTint = Color(0xFFFFD700),
                        badgeText = if (liveTournamentsCount > 0) "Live" else "Matches",
                        badgeColor = if (liveTournamentsCount > 0) Color(0xFF10B981) else Color(0xFFFFD700),
                        onClick = { onTournamentClick("") }
                    )
                    UntitledMetricTile(
                        modifier = Modifier.weight(1f),
                        title = "CASHOUTS",
                        value = "₹${uiState.pendingCashoutsSum.toInt()}",
                        subtitle = "${uiState.pendingCashoutsCount} Pending",
                        icon = UntitledIcons.Coins,
                        iconTint = Color(0xFF34D399),
                        badgeText = if (uiState.pendingCashoutsCount > 0) "Pending" else "Clean",
                        onClick = onPayoutsClick
                    )
                    UntitledMetricTile(
                        modifier = Modifier.weight(1f),
                        title = "DISPUTES",
                        value = "${uiState.openSupportComplaintsCount}",
                        subtitle = if (uiState.openSupportComplaintsCount > 0) "Needs Action" else "Zero Pending",
                        icon = Icons.Default.Warning,
                        iconTint = if (uiState.openSupportComplaintsCount > 0) Color(0xFFF87171) else Color(0xFF94A3B8),
                        badgeText = if (uiState.openSupportComplaintsCount > 0) "Urgent" else "Resolved",
                        onClick = onComplaintsClick
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UntitledMetricTile(
                        modifier = Modifier.weight(1f),
                        title = "PLAYERS",
                        value = "${uiState.totalRegisteredUsersCount}",
                        subtitle = "Active Gamers",
                        icon = UntitledIcons.Users,
                        iconTint = Color(0xFF38BDF8),
                        badgeText = "Total",
                        onClick = onProfilesClick
                    )
                    UntitledMetricTile(
                        modifier = Modifier.weight(1f),
                        title = "MATCHES",
                        value = "${uiState.tournaments.size}",
                        subtitle = if (liveTournamentsCount > 0) "$liveTournamentsCount Live" else "Registered",
                        icon = UntitledIcons.Trophy,
                        iconTint = Color(0xFFFFD700),
                        badgeText = if (liveTournamentsCount > 0) "Live" else "Matches",
                        badgeColor = if (liveTournamentsCount > 0) Color(0xFF10B981) else Color(0xFFFFD700),
                        onClick = { onTournamentClick("") }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UntitledMetricTile(
                        modifier = Modifier.weight(1f),
                        title = "CASHOUTS",
                        value = "₹${uiState.pendingCashoutsSum.toInt()}",
                        subtitle = "${uiState.pendingCashoutsCount} Pending",
                        icon = UntitledIcons.Coins,
                        iconTint = Color(0xFF34D399),
                        badgeText = if (uiState.pendingCashoutsCount > 0) "Pending" else "Clean",
                        onClick = onPayoutsClick
                    )
                    UntitledMetricTile(
                        modifier = Modifier.weight(1f),
                        title = "DISPUTES",
                        value = "${uiState.openSupportComplaintsCount}",
                        subtitle = if (uiState.openSupportComplaintsCount > 0) "Needs Action" else "Zero Pending",
                        icon = Icons.Default.Warning,
                        iconTint = if (uiState.openSupportComplaintsCount > 0) Color(0xFFF87171) else Color(0xFF94A3B8),
                        badgeText = if (uiState.openSupportComplaintsCount > 0) "Urgent" else "Resolved",
                        onClick = onComplaintsClick
                    )
                }
            }

            // 4. Featured Match Command Spotlight
            if (liveTournament != null) {
                UntitledCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFF111116),
                    borderColor = Color(0xFF282834),
                    padding = 14.dp,
                    onClick = { onTournamentClick(liveTournament.id) }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                UntitledBadge(
                                    text = liveTournament.status.uppercase(),
                                    color = if (liveTournament.status.equals("active", ignoreCase = true) || liveTournament.status.equals("live", ignoreCase = true)) Color(0xFF10B981) else Color(0xFF60A5FA)
                                )
                                GameLogoBadge(gameName = liveTournament.game, size = 15.dp)
                                Text(
                                    text = "${liveTournament.game} • ${liveTournament.map}",
                                    color = VelorixTextSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "₹${liveTournament.prizePool.toInt()} Pool",
                                color = Color(0xFFFFD700),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = liveTournament.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val fillRatio = (liveTournament.registeredPlayers.toFloat() / liveTournament.maxPlayers.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${liveTournament.registeredPlayers}/${liveTournament.maxPlayers} Slots Filled",
                                fontSize = 11.sp,
                                color = VelorixTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(fillRatio * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF222228))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fillRatio)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF38BDF8), VelorixAccent)
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF3B3B48), RoundedCornerShape(8.dp))
                                    .bounceClick(scaleDown = 0.96f) { onTournamentClick(liveTournament.id) }
                                    .padding(vertical = 8.dp),
                                color = Color(0xFF1E1E26)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(UntitledIcons.Sliders, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Room & Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF3B3B48), RoundedCornerShape(8.dp))
                                    .bounceClick(scaleDown = 0.96f) { onBracketClick(liveTournament.id) }
                                    .padding(vertical = 8.dp),
                                color = Color(0xFF1E1E26)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(UntitledIcons.Bracket, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Bracket View", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            } else {
                UntitledCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFF111116),
                    borderColor = Color(0xFF282834),
                    padding = 16.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "No Live Matches",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Host and broadcast a tournament",
                                color = VelorixTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .bounceClick(scaleDown = 0.96f) { onCreateTournamentClick() },
                            color = VelorixAccent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                Text("Create Match", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Black)
                            }
                        }
                    }
                }
            }

            // 5. Match Operations Grid
            Text(
                text = "MATCH OPERATIONS",
                color = VelorixTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UntitledActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Verify Queue",
                        subtitle = "${uiState.pendingRegistrationsCount} Pending",
                        icon = UntitledIcons.ShieldCheck,
                        iconTint = Color(0xFF60A5FA),
                        counterBadge = if (uiState.pendingRegistrationsCount > 0) "${uiState.pendingRegistrationsCount}" else null,
                        highlight = uiState.pendingRegistrationsCount > 0,
                        onClick = onVerifyClick
                    )
                    UntitledActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Match Brackets",
                        subtitle = "Elimination Tree",
                        icon = UntitledIcons.Bracket,
                        iconTint = Color(0xFFA78BFA),
                        onClick = { liveTournament?.id?.let { onBracketClick(it) } ?: onTournamentClick("") }
                    )
                    UntitledActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Rules Matrix",
                        subtitle = "Points System",
                        icon = UntitledIcons.Sliders,
                        iconTint = Color(0xFF34D399),
                        onClick = { liveTournament?.id?.let { onSettingsClick(it) } ?: onTournamentClick("") }
                    )
                    UntitledActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Check-In & PINs",
                        subtitle = "${uiState.banners.size} Tokens",
                        icon = UntitledIcons.Key,
                        iconTint = Color(0xFFFBBF24),
                        onClick = onCheckInTokensClick
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UntitledActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Verify Queue",
                        subtitle = "${uiState.pendingRegistrationsCount} Pending",
                        icon = UntitledIcons.ShieldCheck,
                        iconTint = Color(0xFF60A5FA),
                        counterBadge = if (uiState.pendingRegistrationsCount > 0) "${uiState.pendingRegistrationsCount}" else null,
                        highlight = uiState.pendingRegistrationsCount > 0,
                        onClick = onVerifyClick
                    )
                    UntitledActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Match Brackets",
                        subtitle = "Elimination Tree",
                        icon = UntitledIcons.Bracket,
                        iconTint = Color(0xFFA78BFA),
                        onClick = { liveTournament?.id?.let { onBracketClick(it) } ?: onTournamentClick("") }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UntitledActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Rules Matrix",
                        subtitle = "Points System",
                        icon = UntitledIcons.Sliders,
                        iconTint = Color(0xFF34D399),
                        onClick = { liveTournament?.id?.let { onSettingsClick(it) } ?: onTournamentClick("") }
                    )
                    UntitledActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Check-In & PINs",
                        subtitle = "${uiState.banners.size} Tokens",
                        icon = UntitledIcons.Key,
                        iconTint = Color(0xFFFBBF24),
                        onClick = onCheckInTokensClick
                    )
                }
            }

            // 6. Platform Tools & Intelligence
            Text(
                text = "PLATFORM TOOLS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = VelorixTextSecondary,
                letterSpacing = 1.sp
            )

            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UntitledActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Revenue Audit",
                        subtitle = "Daily Profit & ROI",
                        icon = UntitledIcons.ChartSquare,
                        iconTint = Color(0xFF10B981),
                        onClick = onFinancialAuditClick
                    )
                    UntitledActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Leaderboard",
                        subtitle = "Top Players",
                        icon = UntitledIcons.Trophy,
                        iconTint = Color(0xFFFFD700),
                        onClick = onLeaderboardClick
                    )
                    UntitledActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Staff Admins",
                        subtitle = "${uiState.totalActiveAdminsCount} Active",
                        icon = UntitledIcons.ShieldCheck,
                        iconTint = Color(0xFFA78BFA),
                        onClick = onAdminsClick
                    )
                    UntitledActionTile(
                        modifier = Modifier.weight(1f),
                        title = "AI Copilot",
                        subtitle = "Gemini Admin Bot",
                        icon = UntitledIcons.Zap,
                        iconTint = Color(0xFF818CF8),
                        onClick = onChatbotClick
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UntitledActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Revenue Audit",
                        subtitle = "Daily Profit & ROI",
                        icon = UntitledIcons.ChartSquare,
                        iconTint = Color(0xFF10B981),
                        onClick = onFinancialAuditClick
                    )
                    UntitledActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Leaderboard",
                        subtitle = "Top Players",
                        icon = UntitledIcons.Trophy,
                        iconTint = Color(0xFFFFD700),
                        onClick = onLeaderboardClick
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UntitledActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Staff Admins",
                        subtitle = "${uiState.totalActiveAdminsCount} Active",
                        icon = UntitledIcons.ShieldCheck,
                        iconTint = Color(0xFFA78BFA),
                        onClick = onAdminsClick
                    )
                    UntitledActionTile(
                        modifier = Modifier.weight(1f),
                        title = "AI Copilot",
                        subtitle = "Gemini Admin Bot",
                        icon = UntitledIcons.Zap,
                        iconTint = Color(0xFF818CF8),
                        onClick = onChatbotClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(90.dp))
        }

        // Modals
        if (showPublishAnnouncementDialog) {
            PublishAnnouncementDialog(
                onDismiss = { showPublishAnnouncementDialog = false },
                onPublish = { announcement ->
                    onPublishAnnouncement?.invoke(announcement)
                }
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
        shape = RoundedCornerShape(14.dp),
        brush = Brush.linearGradient(
            listOf(Color(0xFF0C0C0E), Color(0xFF09090B))
        ),
        borderBrush = Brush.linearGradient(
            listOf(border.copy(alpha = 0.4f), Color(0xFF27272A))
        ),
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
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = iconTint.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, iconTint.copy(alpha = 0.25f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(13.dp))
                    }
                }
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
        Spacer(modifier = Modifier.height(10.dp))
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(
                width = 1.dp,
                brush = GlassTokens.GlassBorderSubtle,
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp)),
        color = Color(0xFF09090B)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .bounceClick(scaleDown = 0.90f) { onBack() },
                    color = Color(0xFF18181B),
                    border = BorderStroke(1.dp, Color(0xFF27272A))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Apps",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "APPS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
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
        }
    }
}

@Composable
fun OperationsHubScreenContent(
    uiState: DashboardState.Success,
    onVerifyClick: () -> Unit,
    onToggleUserBan: (com.example.domain.model.UserProfile) -> Unit,
    onAddFunds: (com.example.domain.model.UserProfile, Double) -> Unit,
    onDeleteUser: (com.example.domain.model.UserProfile) -> Unit,
    onUpdateUser: ((com.example.domain.model.UserProfile) -> Unit)? = null,
    onRefreshUserData: (() -> Unit)? = null,
    onSaveComplaint: ((com.example.domain.model.ComplaintTicket) -> Unit)? = null,
    onDeleteComplaint: ((String) -> Unit)? = null,
    onSendTicketMessage: ((ticketId: String, message: String, onComplete: (Boolean) -> Unit) -> Unit)? = null,
    getTicketMessagesStream: (suspend (String) -> kotlinx.coroutines.flow.Flow<List<com.example.domain.model.TicketMessage>>)? = null,
    onIssueTicketCompensation: ((ticket: com.example.domain.model.ComplaintTicket, amount: Double, reason: String, onComplete: (Boolean) -> Unit) -> Unit)? = null,
    accentColor: Color
) {
    var selectedSection by remember { mutableStateOf(0) } // 0: KYC Queue, 1: User Directory, 2: Support Tickets

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Section Segmented Control
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color(0xFF27272A), RoundedCornerShape(14.dp)),
            color = Color(0xFF09090B)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab 0: KYC Queue
                val isKyc = selectedSection == 0
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .bounceClick(scaleDown = 0.95f) { selectedSection = 0 },
                    color = if (isKyc) Color(0xFF18181B) else Color.Transparent,
                    border = if (isKyc) BorderStroke(1.dp, Color(0xFF3F3F46)) else null
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "KYC Queue",
                            fontSize = 11.5.sp,
                            fontWeight = if (isKyc) FontWeight.Bold else FontWeight.Medium,
                            color = if (isKyc) Color.White else Color(0xFFA1A1AA)
                        )
                        if (uiState.pendingRegistrationsCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFEF4444), CircleShape)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${uiState.pendingRegistrationsCount}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Tab 1: User Directory
                val isUsers = selectedSection == 1
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .bounceClick(scaleDown = 0.95f) { selectedSection = 1 },
                    color = if (isUsers) Color(0xFF18181B) else Color.Transparent,
                    border = if (isUsers) BorderStroke(1.dp, Color(0xFF3F3F46)) else null
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Players",
                            fontSize = 11.5.sp,
                            fontWeight = if (isUsers) FontWeight.Bold else FontWeight.Medium,
                            color = if (isUsers) Color.White else Color(0xFFA1A1AA)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${uiState.totalRegisteredUsersCount})",
                            fontSize = 10.sp,
                            color = Color(0xFF71717A)
                        )
                    }
                }

                // Tab 2: Support Tickets
                val isSupport = selectedSection == 2
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .bounceClick(scaleDown = 0.95f) { selectedSection = 2 },
                    color = if (isSupport) Color(0xFF18181B) else Color.Transparent,
                    border = if (isSupport) BorderStroke(1.dp, Color(0xFF3F3F46)) else null
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Tickets",
                            fontSize = 11.5.sp,
                            fontWeight = if (isSupport) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSupport) Color.White else Color(0xFFA1A1AA)
                        )
                        if (uiState.openSupportComplaintsCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFF59E0B), CircleShape)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${uiState.openSupportComplaintsCount}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content Display
        when (selectedSection) {
            0 -> {
                // KYC Verification Queue
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF27272A), RoundedCornerShape(16.dp)),
                        color = Color(0xFF09090B),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "ID & PAYMENT VERIFICATION",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VelorixTextSecondary,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${uiState.pendingRegistrationsCount} Pending In Queue",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF18181B), CircleShape)
                                        .border(1.dp, Color(0xFF27272A), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Verify payment screenshots, in-game IDs, and age eligibility for tournament registrations.",
                                fontSize = 12.sp,
                                color = VelorixTextSecondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onVerifyClick,
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("OPEN FULL VERIFICATION QUEUE", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            1 -> {
                // User Directory
                UsersManagementScreenContent(
                    uiState = uiState,
                    onToggleBan = onToggleUserBan,
                    onAddFunds = onAddFunds,
                    onDeleteUser = onDeleteUser,
                    onUpdateUser = { onUpdateUser?.invoke(it) },
                    onRefreshUserData = onRefreshUserData
                )
            }
            2 -> {
                // Support & Tickets
                ComplaintsSupportScreen(
                    users = uiState.users,
                    initialTickets = uiState.supportTickets,
                    onAddFundsToUser = onAddFunds,
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
        }
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
            title = "SFX Audio Lab (Beat Tester)",
            description = "Test subtle Michael Jackson acoustic beat textures, funk snaps, stabs & master volume",
            badge = "SFX Tester",
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
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search tools & system configurations...", color = Color(0xFF71717A), fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(18.dp))
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color(0xFF27272A),
                focusedContainerColor = Color(0xFF09090B),
                unfocusedContainerColor = Color(0xFF09090B)
            )
        )

        Text(
            text = "SYSTEM & TOOLS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = VelorixTextSecondary,
            letterSpacing = 1.sp
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            filteredItems.forEach { item ->
                AppHubRowTile(
                    item = item,
                    accentColor = accentColor,
                    onClick = { onNavigate(item.id) }
                )
            }
        }
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFF27272A),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .bounceClick(scaleDown = 0.98f) { onClick() },
        color = Color(0xFF0E0E11)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon Squircle - Sleek minimalist matte black with crisp zinc border
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        Color(0xFF18181B),
                        RoundedCornerShape(12.dp)
                    )
                    .border(1.dp, Color(0xFF27272A), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Titles & Subtitles
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (item.isAlert) Color(0xFFEF4444).copy(alpha = 0.15f)
                                else Color(0xFF27272A),
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                0.5.dp,
                                if (item.isAlert) Color(0xFFEF4444).copy(alpha = 0.5f)
                                else Color(0xFF3F3F46),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (item.isAlert) Color(0xFFFF8A80) else Color(0xFFE4E4E7)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
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
                tint = Color(0xFF71717A),
                modifier = Modifier.size(16.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(115.dp))
}

@Composable
fun AdminProfileDialog(
    email: String,
    onDismiss: () -> Unit,
    onNavigateTab: (String) -> Unit,
    onOpenAttributions: () -> Unit = {},
    onOpenRateLimiter: () -> Unit = {},
    onOpenAudioLab: () -> Unit = {},
    onLogoutClick: () -> Unit,
    accentColor: Color
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141416),
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatarView(
                        avatarUrl = null,
                        name = email,
                        size = 46.dp,
                        role = "super_admin",
                        isActiveRecently = true,
                        showGlow = true
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Admin Session",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF102A1E),
                            border = BorderStroke(1.dp, Color(0xFF166534)),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "SUPER ADMINISTRATOR",
                                color = Color(0xFF86EFAC),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Email & DB Connection Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF09090B),
                    border = BorderStroke(1.dp, Color(0xFF27272A))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Active Account", color = Color(0xFF71717A), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text(email, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(7.dp).background(Color(0xFF4ADE80), CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Connected to Firebase Realtime DB & Firestore", color = Color(0xFF4ADE80), fontSize = 10.sp)
                        }
                    }
                }

                // Quick Navigation items
                Text("QUICK CONTROLS & SECURITY", color = Color(0xFF71717A), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenRateLimiter() },
                    color = Color(0xFF1A1A1E),
                    border = BorderStroke(1.dp, Color(0xFF27272A))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(UntitledIcons.Sliders, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Rate Limiter & Quota Engine", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(15.dp))
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenAudioLab() },
                    color = Color(0xFF1A1A1E),
                    border = BorderStroke(1.dp, Color(0xFF27272A))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Headphones, contentDescription = null, tint = Color(0xFFD49A3D), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("SFX Audio Lab (Beat Tester)", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(15.dp))
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenAttributions() },
                    color = Color(0xFF1A1A1E),
                    border = BorderStroke(1.dp, Color(0xFF27272A))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Attributions & Tech Credits", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(15.dp))
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onNavigateTab("admins") },
                    color = Color(0xFF1A1A1E),
                    border = BorderStroke(1.dp, Color(0xFF27272A))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Staff & RBAC Roles", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(15.dp))
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onNavigateTab("settings") },
                    color = Color(0xFF1A1A1E),
                    border = BorderStroke(1.dp, Color(0xFF27272A))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("System Settings & Themes", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(15.dp))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Logout Action Button
                Button(
                    onClick = onLogoutClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2A1010),
                        contentColor = Color(0xFFEF4444)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Log Out", tint = Color(0xFFEF4444), modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Out Administrator", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {}
    )
}
