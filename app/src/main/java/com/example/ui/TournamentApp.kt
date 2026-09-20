package com.example.ui

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.auth.DeviceAccountBindingManager
import com.example.data.repository.TournamentRepositoryImpl
import com.example.notification.VelorixNotificationManager
import com.example.ui.common.GlassBackgroundBox
import com.example.ui.common.GlassCard
import com.example.ui.common.GlassTokens
import com.example.ui.common.GlobalErrorSnackbarHost
import com.example.ui.common.InAppNotificationBannerHost
import com.example.ui.theme.VelorixAccent
import com.example.ui.theme.VelorixTextPrimary
import com.example.ui.theme.VelorixBg
import com.example.ui.viewmodel.TournamentDashboardViewModel
import com.example.ui.viewmodel.DashboardState
import com.example.domain.model.Tournament
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.Flow

@Composable
fun TournamentApp(
    tournamentRepository: TournamentRepositoryImpl,
    incomingEmailLink: String? = null,
    onEmailLinkHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    val viewModel: TournamentDashboardViewModel = viewModel(
        factory = TournamentDashboardViewModel.provideFactory(tournamentRepository)
    )
    
    val uiState by viewModel.uiState.collectAsState()
    val remoteConfigState by com.example.config.VelorixRemoteConfigManager.configState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Notification Permission launcher for Android 13+ (API 33+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        VelorixNotificationManager.initChannels(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Setup SharedPreferences & Device Binding Manager
    val sharedPrefs = remember { context.getSharedPreferences("VelorixPrefs", Context.MODE_PRIVATE) }
    val bindingManager = remember { DeviceAccountBindingManager(context) }

    // If incoming email link is present, ensure we navigate to login screen if not already logged in
    LaunchedEffect(incomingEmailLink) {
        if (incomingEmailLink != null && tournamentRepository.auth.isSignInWithEmailLink(incomingEmailLink)) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    GlassBackgroundBox(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        NavHost(
            navController = navController,
            startDestination = "splash",
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 280, easing = LinearOutSlowInEasing)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                    targetOffset = { fullWidth -> (fullWidth * 0.12f).toInt() }
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                    initialOffset = { fullWidth -> (fullWidth * 0.12f).toInt() }
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 280, easing = LinearOutSlowInEasing)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
                )
            }
        ) {
            composable(
                route = "splash",
                enterTransition = { fadeIn(tween(durationMillis = 300)) },
                exitTransition = { fadeOut(tween(durationMillis = 250)) }
            ) {
                var hasNavigated by remember { mutableStateOf(false) }

                fun proceedToNext() {
                    if (hasNavigated) return
                    hasNavigated = true
                    try {
                        navController.navigate("login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } catch (_: Exception) {
                        try {
                            navController.navigate("login") {
                                popUpTo("splash") { inclusive = true }
                            }
                        } catch (_: Exception) {}
                    }
                }

                LaunchedEffect(Unit) {
                    delay(600L) // Brief smooth splash transition
                    proceedToNext()
                }

                // Hard fallback timer
                LaunchedEffect(Unit) {
                    delay(1200L)
                    proceedToNext()
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0A0A0E))
                        .clickable { proceedToNext() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "VELORIX",
                            color = VelorixAccent,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "TOURNAMENT OVERSIGHT",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            letterSpacing = 2.sp
                        )
                        
                        Spacer(modifier = Modifier.height(36.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .width(140.dp)
                                .height(3.dp),
                            color = VelorixAccent,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }
            composable(
                route = "login",
                enterTransition = { fadeIn(tween(durationMillis = 280)) },
                exitTransition = { fadeOut(tween(durationMillis = 220)) }
            ) {
                LoginScreen(
                    tournamentRepository = tournamentRepository,
                    incomingEmailLink = incomingEmailLink,
                    onEmailLinkHandled = onEmailLinkHandled,
                    onLoginSuccess = { userEmail ->
                        coroutineScope.launch {
                            val finalEmail = userEmail ?: bindingManager.getBoundAccount()?.email ?: "anantisback47@gmail.com"
                            val cleanEmail = finalEmail.trim().lowercase()
                            val username = cleanEmail.substringBefore("@")
                            bindingManager.recordAccount(cleanEmail, username, role = "super_admin", autoLogin = true)
                            tournamentRepository.recordAccountLocally(cleanEmail, username, role = "super_admin")
                            tournamentRepository.ensureAuthenticatedSession(cleanEmail)
                            viewModel.setManualLoginEmail(cleanEmail)
                            sharedPrefs.edit().putString("user_email", cleanEmail).apply()
                            viewModel.fetchData()
                            navController.navigate("dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }
                )
            }
            composable(
                route = "dashboard",
                enterTransition = { fadeIn(tween(durationMillis = 280)) },
                exitTransition = { fadeOut(tween(durationMillis = 220)) }
            ) {
                DashboardScreen(
                    uiState = uiState,
                    onTournamentClick = { tournamentId ->
                        navController.navigate("tournament_details/$tournamentId")
                    },
                    onSettingsClick = { tournamentId ->
                        navController.navigate("settings/$tournamentId")
                    },
                    onCreateTournamentClick = {
                        navController.navigate("create_tournament")
                    },
                    onVerifyClick = {
                        navController.navigate("verification")
                    },
                    onBracketClick = { tournamentId ->
                        navController.navigate("bracket/$tournamentId")
                    },
                    onNavClick = { route ->
                        if (route == "logout") {
                            sharedPrefs.edit().clear().apply()
                            bindingManager.unbindAccount()
                            viewModel.setManualLoginEmail(null)
                            tournamentRepository.auth.signOut()
                            try {
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else if (route != "dashboard") {
                            try {
                                navController.navigate(route)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    onToggleUserBan = { user ->
                        viewModel.toggleUserBan(user)
                    },
                    onAddFunds = { user, amount ->
                        viewModel.addFundsToUser(user, amount)
                    },
                    onDeleteUser = { user ->
                        viewModel.deleteUser(user)
                    },
                    onUpdateUser = { user ->
                        viewModel.updateUserProfile(user)
                    },
                    onRefreshUserData = {
                        viewModel.loadUserData()
                    },
                    onGrantAdmin = { email, name, role ->
                        viewModel.grantAdminAccess(email.hashCode().toString(), email, name, role)
                    },
                    onRevokeAdmin = { adminUid ->
                        viewModel.revokeAdminAccess(adminUid)
                    },
                    onUpdateAdmin = { admin ->
                        viewModel.updateAdminRecord(admin)
                    },
                    onDeleteAdmin = { adminUid ->
                        viewModel.deleteAdminRecord(adminUid)
                    },
                    onSaveComplaint = { ticket ->
                        viewModel.saveComplaint(ticket)
                    },
                    onDeleteComplaint = { ticketId ->
                        viewModel.deleteComplaint(ticketId)
                    },
                    onSaveToken = { token ->
                        viewModel.saveToken(token)
                    },
                    onSaveBanner = { banner ->
                        viewModel.saveBanner(banner)
                    },
                    onPublishAnnouncement = { announcement ->
                        viewModel.publishGlobalAnnouncement(announcement)
                    },
                    onDeleteAnnouncement = { announcementId ->
                        viewModel.deleteGlobalAnnouncement(announcementId)
                    },
                    onUpdateTournament = { updated ->
                        viewModel.updateTournament(updated)
                    },
                    onApprovePayout = { req ->
                        viewModel.approvePayout(req)
                    },
                    onRejectPayout = { req, reason ->
                        viewModel.rejectPayout(req, reason)
                    },
                    onImportTournamentsJson = { jsonStr ->
                        viewModel.importTournamentsFromJson(jsonStr) { (count, msg) ->
                            Toast.makeText(
                                context,
                                if (count > 0) "Successfully imported $count tournament(s) to Firebase!" else "Import failed: $msg",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    onSyncAllTournaments = {
                        viewModel.syncAllTournamentsToCloud { (count, msg) ->
                            Toast.makeText(
                                context,
                                if (count > 0) "Successfully synced $count tournament(s) to Cloud nodes!" else "Cloud sync failed: $msg",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    onRefreshClick = {
                        viewModel.refreshRealtimeData()
                    },
                    onMarkNotificationRead = { notifId ->
                        viewModel.markNotificationAsRead(notifId)
                    },
                    onClearAllNotifications = {
                        viewModel.clearAllNotifications()
                    },
                    onTestPushNotification = {
                        viewModel.testPushNotification(context)
                    },
                    onPublishCampaign = { title, msg, priority, tourneyId ->
                        viewModel.publishCampaign(title, msg, priority, tourneyId)
                    },
                    onPurgeDemoData = {
                        viewModel.purgeAllDemoData { success, _ ->
                            if (success) {
                                viewModel.refreshRealtimeData()
                            }
                        }
                    }
                )
            }
            composable("tournament_details/{tournamentId}") { backStackEntry ->
                val tournamentId = backStackEntry.arguments?.getString("tournamentId") ?: ""
                val initialTournament = (uiState as? DashboardState.Success)?.tournaments?.find { it.id == tournamentId }
                val liveTournamentFlow: Flow<Tournament?> = remember(tournamentId) {
                    viewModel.getTournamentLiveStream(tournamentId)
                }
                val liveTournamentState by liveTournamentFlow.collectAsState(initial = initialTournament)
                
                val tournament = liveTournamentState ?: initialTournament
                
                if (tournament != null) {
                    val successState = uiState as? DashboardState.Success
                    val proofsForTourney = successState?.matchProofs?.filter { it.tournamentId == tournamentId } ?: emptyList()
                    val startsAtLong = tournament.startsAt?.toLongOrNull()

                    TournamentDetailsScreen(
                        tournament = tournament,
                        currentUserProfile = successState?.userProfiles?.firstOrNull(),
                        matchProofs = proofsForTourney,
                        onNavigateBack = { navController.popBackStack() },
                        onEditTournament = {
                            navController.navigate("settings/$tournamentId")
                        },
                        onEditRules = {
                            navController.navigate("tournament_rules/$it")
                        },
                        onCancelTournament = { id, reason ->
                            viewModel.cancelTournament(id, reason)
                        },
                        onDeleteTournament = { id ->
                            viewModel.deleteTournament(id)
                            com.example.ui.common.GlobalErrorManager.emitSuccess("Tournament deleted successfully.")
                            navController.popBackStack()
                        },
                        onViewBracket = { id ->
                            navController.navigate("bracket/$id")
                        },
                        onPublishRoomCredentials = { roomId, roomPass, bypass ->
                            viewModel.publishRoomCredentialsWith5MinCheck(
                                tournamentId = tournament.id,
                                roomId = roomId,
                                roomPass = roomPass,
                                startsAtTimestamp = startsAtLong,
                                bypassCheck = bypass
                            )
                        },
                        onApproveMatchProof = { proofId, prizeAmt, kills ->
                            viewModel.approveMatchProof(proofId, prizeAmt, kills)
                        },
                        onRejectMatchProof = { proofId, reason ->
                            viewModel.rejectMatchProof(proofId, reason)
                        },
                        onAddParticipant = { tId, reg ->
                            viewModel.joinTournament(tId, reg, 0f)
                        }
                    )
                } else {
                    GlassBackgroundBox(accentColor = VelorixAccent) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                borderBrush = GlassTokens.GlassBorderGradient
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape)
                                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteForever,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Text(
                                        text = "Tournament Unavailable or Deleted",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "This tournament was permanently deleted or is no longer present in the database.",
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 12.5.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 17.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = { navController.popBackStack() },
                                        colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Return to Tournaments", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            composable("chatbot") {
                AdminChatbotScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable("verification") {
                val verificationViewModel: com.example.ui.viewmodel.PlayerVerificationViewModel = viewModel(
                    factory = com.example.ui.viewmodel.PlayerVerificationViewModel.provideFactory(tournamentRepository)
                )
                PlayerVerificationScreen(
                    viewModel = verificationViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("bracket/{tournamentId}") { backStackEntry ->
                val tournamentId = backStackEntry.arguments?.getString("tournamentId")
                if (tournamentId != null) {
                    val bracketViewModel: com.example.ui.viewmodel.BracketViewModel = viewModel(
                        factory = com.example.ui.viewmodel.BracketViewModel.provideFactory(tournamentRepository)
                    )
                    BracketManagementScreen(
                        tournamentId = tournamentId,
                        viewModel = bracketViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
            composable("analytics") {
                AnalyticsScreen(
                    uiState = uiState,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("create_tournament") {
                com.example.ui.CreateTournamentScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onCreate = { newTournament ->
                        viewModel.createTournament(newTournament)
                        navController.popBackStack()
                    }
                )
            }
            composable("staff_management") {
                StaffManagementScreen(
                    uiState = uiState,
                    onGrantAdmin = { email, name, role ->
                        viewModel.grantAdminAccess(email.hashCode().toString(), email, name, role)
                    },
                    onRevokeAdmin = { adminUid ->
                        viewModel.revokeAdminAccess(adminUid)
                    },
                    onUpdateAdmin = { admin ->
                        viewModel.updateAdminRecord(admin)
                    },
                    onDeleteAdmin = { adminUid ->
                        viewModel.deleteAdminRecord(adminUid)
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("tournament_rules") {
                val success = uiState as? DashboardState.Success
                TournamentRulesManagerScreen(
                    tournaments = success?.tournaments ?: emptyList(),
                    onNavigateBack = { navController.popBackStack() },
                    onSaveTournament = { updated ->
                        viewModel.updateTournament(updated)
                    },
                    currentUserEmail = success?.currentUserEmail
                )
            }
            composable("tournament_rules/{tournamentId}") { backStackEntry ->
                val tournamentId = backStackEntry.arguments?.getString("tournamentId")
                val success = uiState as? DashboardState.Success
                TournamentRulesManagerScreen(
                    tournaments = success?.tournaments ?: emptyList(),
                    initialTournamentId = tournamentId,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveTournament = { updated ->
                        viewModel.updateTournament(updated)
                    },
                    currentUserEmail = success?.currentUserEmail
                )
            }
            composable("settings/{tournamentId}") { backStackEntry ->
                val tournamentId = backStackEntry.arguments?.getString("tournamentId")
                val tournament = (uiState as? DashboardState.Success)?.tournaments?.find { it.id == tournamentId }
                
                if (tournament != null) {
                    TournamentSettingsScreen(
                        tournament = tournament,
                        onNavigateBack = { navController.popBackStack() },
                        onSave = { updated ->
                            viewModel.updateTournament(updated)
                            navController.popBackStack()
                        },
                        onCancelTournament = { id, reason ->
                            viewModel.cancelTournament(id, reason)
                            navController.popBackStack()
                        },
                        onDeleteTournament = { id ->
                            viewModel.deleteTournament(id)
                            navController.popBackStack()
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(VelorixBg)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = VelorixAccent)
                            Text(
                                text = "Tournament not found or loading...",
                                color = VelorixTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { navController.popBackStack() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = VelorixAccent),
                                border = BorderStroke(1.dp, VelorixAccent.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Return to Dashboard")
                            }
                        }
                    }
                }
            }
        }

        if (remoteConfigState.isMaintenanceMode) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp, start = 12.dp, end = 12.dp),
                color = Color(0xFF450A0A),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = remoteConfigState.maintenanceMessage,
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(
                        onClick = { com.example.config.VelorixRemoteConfigManager.fetchAndActivate() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("Refresh", color = Color(0xFFFCA5A5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        GlobalErrorSnackbarHost(
            modifier = Modifier.align(Alignment.TopCenter)
        )

        InAppNotificationBannerHost(
            onNotificationClick = { notif ->
                viewModel.markNotificationAsRead(notif.id)
                if (notif.targetTournamentId.isNotBlank()) {
                    navController.navigate("tournament_details/${notif.targetTournamentId}")
                }
            }
        )
    }
}
