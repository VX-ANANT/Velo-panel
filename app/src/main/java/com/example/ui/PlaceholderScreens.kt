package com.example.ui

import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.util.TournamentImageUploader
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.Tournament
import com.example.domain.model.UserProfile
import com.example.data.validation.UserRateLimiter
import com.example.ui.theme.*
import com.example.ui.common.*
import com.example.ui.viewmodel.DashboardState
import com.example.ui.audio.rememberVelorixSoundManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElaborateScreen(title: String, onNavigateBack: () -> Unit, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(V14Bg)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(V14CardSurface)
                            .border(1.dp, V14Border, CircleShape)
                            .clickable(onClick = onNavigateBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        color = V14TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        letterSpacing = (-0.3).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = V14Border)
            }
        },
        containerColor = V14Bg
    ) { padding ->
        content(padding)
    }
}

// TournamentsListScreenContent has been rebuilt in TournamentsListScreen.kt


@Composable
fun AnalyticsScreen(
    uiState: DashboardState = DashboardState.Loading,
    onNavigateBack: () -> Unit
) {
    val successState = uiState as? DashboardState.Success
    val usersCount = successState?.totalRegisteredUsersCount ?: 0
    val tourneys = successState?.tournaments ?: emptyList()
    val totalPrizePool = tourneys.sumOf { it.prizePool.toDouble() }
    val totalRegistrations = tourneys.sumOf { it.registeredPlayers }
    val pendingCashouts = successState?.pendingCashoutsSum ?: 0.0
    val openTickets = successState?.openSupportComplaintsCount ?: 0
    val activeAdmins = successState?.totalActiveAdminsCount ?: 0

    ElaborateScreen("Live Platform Analytics", onNavigateBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(V14Bg)
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 32.dp)
        ) {
            item {
                Text(
                    "REALTIME METRICS SUMMARY",
                    color = V14TextMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AnalyticsCard(modifier = Modifier.weight(1f), title = "Registered Players", value = "$usersCount", accent = V14AccentBright)
                    AnalyticsCard(modifier = Modifier.weight(1f), title = "Active Tournaments", value = "${tourneys.size}", accent = VelorixEmerald)
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AnalyticsCard(modifier = Modifier.weight(1f), title = "Total Prize Pools", value = "₹${totalPrizePool.toInt()}", accent = VelorixAmber)
                    AnalyticsCard(modifier = Modifier.weight(1f), title = "Player Enrollments", value = "$totalRegistrations", accent = V14TextPrimary)
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AnalyticsCard(modifier = Modifier.weight(1f), title = "Pending Cashouts", value = "₹${pendingCashouts.toInt()}", accent = VelorixAmber)
                    AnalyticsCard(modifier = Modifier.weight(1f), title = "Open Support Tickets", value = "$openTickets", accent = V14AccentBright)
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AnalyticsCard(modifier = Modifier.weight(1f), title = "Verified Staff Admins", value = "$activeAdmins", accent = VelorixEmerald)
                    AnalyticsCard(modifier = Modifier.weight(1f), title = "Pending Verification", value = "${successState?.pendingRegistrationsCount ?: 0}", accent = VelorixAmber)
                }
            }
        }
    }
}

@Composable
fun AnalyticsCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    accent: Color = V14AccentBright
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = V14CardSurface,
        border = BorderStroke(1.dp, V14Border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Text(
                    text = title.uppercase(),
                    color = V14TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                color = V14TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun GlobalSettingsScreenContent(
    opticsManager: LiquidGlassOpticsManager? = null,
    accentColor: Color = VelorixAccent,
    currentTheme: com.example.ui.common.HyperOSTheme = com.example.ui.common.HyperOSTheme.NEBULA_PURPLE,
    onThemeSelect: (com.example.ui.common.HyperOSTheme) -> Unit = {},
    onPurgeDemoData: (() -> Unit)? = null,
    currentUserEmail: String? = null,
    onLogout: (() -> Unit)? = null,
    onNavigateTab: ((String) -> Unit)? = null,
    onOpenAudioLab: (() -> Unit)? = null,
    onOpenApiKeyDialog: (() -> Unit)? = null,
    onOpenRateLimiter: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { com.example.data.repository.TournamentRepositoryImpl(context) }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val soundManager = rememberVelorixSoundManager()

    var showPurgeConfirmationDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmationDialog by remember { mutableStateOf(false) }
    var showRtdbRulesDialog by remember { mutableStateOf(false) }
    var showFirestoreRulesDialog by remember { mutableStateOf(false) }
    var isSyncingRls by remember { mutableStateOf(false) }
    var rlsFeedbackMessage by remember { mutableStateOf<String?>(null) }

    if (showRtdbRulesDialog) {
        val rtdbRules = remember { repository.getProductionRtdbRules() }
        AlertDialog(
            onDismissRequest = { showRtdbRulesDialog = false },
            containerColor = V14CardSurface,
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = V14AccentBright)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Realtime Database Security Rules", color = V14TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Paste this JSON into Firebase Console -> Realtime Database -> Rules. This protects slot locks and room details while giving admin full authority.",
                        color = V14TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = V14Border)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = rtdbRules,
                            color = V14TextPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        soundManager.playSnapPop()
                        clipboardManager.setText(AnnotatedString(rtdbRules))
                        Toast.makeText(context, "RTDB Rules copied to clipboard!", Toast.LENGTH_SHORT).show()
                        showRtdbRulesDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = V14AccentPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Rules", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRtdbRulesDialog = false }) {
                    Text("Close", color = V14TextSecondary)
                }
            }
        )
    }

    if (showFirestoreRulesDialog) {
        val firestoreRules = remember { repository.getProductionFirestoreRules() }
        AlertDialog(
            onDismissRequest = { showFirestoreRulesDialog = false },
            containerColor = V14CardSurface,
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalPolice, contentDescription = null, tint = VelorixEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Firestore Security Rules", color = V14TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Paste this into Firebase Console -> Firestore Database -> Rules. Enforces Row-Level Security for wallets and user data.",
                        color = V14TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = V14Border)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = firestoreRules,
                            color = VelorixEmerald,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        soundManager.playSnapPop()
                        clipboardManager.setText(AnnotatedString(firestoreRules))
                        Toast.makeText(context, "Firestore Rules copied to clipboard!", Toast.LENGTH_SHORT).show()
                        showFirestoreRulesDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = V14AccentPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Rules", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFirestoreRulesDialog = false }) {
                    Text("Close", color = V14TextSecondary)
                }
            }
        )
    }

    if (showLogoutConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmationDialog = false },
            containerColor = Color.Black,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out Admin", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "Are you sure you want to end your administrator session? You will be signed out of Firebase and returned to the login screen.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmationDialog = false
                        onLogout?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmationDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    if (showPurgeConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeConfirmationDialog = false },
            containerColor = Color.Black,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clean Mock Data", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "This will clear temporary test mock tournaments and dummy records from Firebase, resetting to clean live production data.\n\nYour admin credentials and real users remain safe.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        soundManager.playOrchestraHit()
                        showPurgeConfirmationDialog = false
                        onPurgeDemoData?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Confirm Wipe", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeConfirmationDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    val v14Crimson = Color(0xFFFF2E5B)
    val resolvedAdminEmail = currentUserEmail?.takeIf { it.isNotBlank() } ?: "Authorized Administrator"

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 144.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp)
    ) {
        // Page Header
        item {
            Column(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                Text(
                    text = "Settings",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Admin identity, match tools, audio & Firebase session",
                    color = Color(0xFF9A9AA6),
                    fontSize = 12.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
            }
        }

        // Section 01: Administrator Identity
        item {
            SystemClassifiedSection(sectionTitle = "01 · Administrator Identity") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(v14Crimson.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                            .border(0.75.dp, v14Crimson.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (resolvedAdminEmail.firstOrNull() ?: 'A').uppercase(),
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Super Administrator", color = Color(0xFFF5F5F7), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF34D399), CircleShape))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                "VERIFIED",
                                color = Color(0xFF34D399),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(resolvedAdminEmail, color = Color(0xFFD4D4DC), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Firebase RTDB & Cloud Firestore Live", color = Color(0xFF71717A), fontSize = 10.5.sp)
                    }
                }
            }
        }

        // Section 02: Competition & Content
        item {
            SystemClassifiedSection(sectionTitle = "02 · Competition & Content") {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                    SettingsModuleRow(
                        title = "Rules & Match Specs",
                        subtitle = "Free Fire room presets, bans & map rules",
                        icon = Icons.Default.MilitaryTech,
                        onClick = {
                            soundManager.playBeatTap()
                            onNavigateTab?.invoke("tournament_rules")
                        }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
                    SettingsModuleRow(
                        title = "Banners & Check-In PINs",
                        subtitle = "Manage home banners & match check-in PINs",
                        icon = Icons.Default.DashboardCustomize,
                        onClick = {
                            soundManager.playBeatTap()
                            onNavigateTab?.invoke("lowcode")
                        }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
                    SettingsModuleRow(
                        title = "Player Leaderboards",
                        subtitle = "Live rankings, kills & Hall of Fame",
                        icon = Icons.Default.EmojiEvents,
                        onClick = {
                            soundManager.playBeatTap()
                            onNavigateTab?.invoke("leaderboard")
                        }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
                    SettingsModuleRow(
                        title = "Authorized Admins",
                        subtitle = "Inspect active admin roster & permissions",
                        icon = Icons.Default.Shield,
                        onClick = {
                            soundManager.playBeatTap()
                            onNavigateTab?.invoke("admins")
                        }
                    )
                }
            }
        }

        // Section 03: Interface Audio
        item {
            var sfxMuted by remember { mutableStateOf(soundManager.isMuted) }
            var sfxVol by remember { mutableFloatStateOf(soundManager.masterVolume) }

            SystemClassifiedSection(sectionTitle = "03 · Interface Audio") {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = v14Crimson, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("UI Sound Effects", color = Color(0xFFF5F5F7), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("Tactile taps & confirmation chimes", color = Color(0xFF71717A), fontSize = 11.5.sp)
                            }
                        }
                        Switch(
                            checked = !sfxMuted,
                            onCheckedChange = { active ->
                                sfxMuted = !active
                                soundManager.isMuted = !active
                                if (active) soundManager.playSnapPop()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = v14Crimson,
                                uncheckedTrackColor = Color(0xFF27272A)
                            )
                        )
                    }

                    if (!sfxMuted) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Text("Volume", color = Color(0xFFA1A1AA), fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Slider(
                                value = sfxVol,
                                onValueChange = { newVol ->
                                    sfxVol = newVol
                                    soundManager.masterVolume = newVol
                                },
                                onValueChangeFinished = { soundManager.playBeatTap() },
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = v14Crimson,
                                    inactiveTrackColor = Color(0xFF27272A)
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("${(sfxVol * 100).toInt()}%", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Section 04: Firebase Sync & Session
        item {
            SystemClassifiedSection(sectionTitle = "04 · Firebase Sync & Session") {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = v14Crimson, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Firebase Security Rules", color = Color(0xFFF5F5F7), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("RTDB & Firestore admin access sync", color = Color(0xFF71717A), fontSize = 11.sp)
                            }
                        }
                        Text(
                            "ALLOWLISTED",
                            color = Color(0xFF34D399),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    val feedback = rlsFeedbackMessage
                    if (feedback != null) {
                        Text(
                            text = feedback,
                            color = Color(0xFF34D399),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    soundManager.playSnapPop()
                                    showRtdbRulesDialog = true
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFE4E4E7), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RTDB Rules", color = Color(0xFFE4E4E7), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }

                        VerticalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    soundManager.playSnapPop()
                                    showFirestoreRulesDialog = true
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocalPolice, contentDescription = null, tint = Color(0xFFE4E4E7), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Firestore", color = Color(0xFFE4E4E7), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }

                        VerticalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = !isSyncingRls) {
                                    coroutineScope.launch {
                                        soundManager.playBrassHit()
                                        isSyncingRls = true
                                        rlsFeedbackMessage = "Syncing Admin permissions across Firebase..."
                                        val ok = repository.syncAdminPermissionsNow(resolvedAdminEmail)
                                        isSyncingRls = false
                                        rlsFeedbackMessage = if (ok) "Admin permissions synced for $resolvedAdminEmail" else "Sign in with an authorized admin account to sync."
                                    }
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSyncingRls) {
                                CircularProgressIndicator(color = v14Crimson, modifier = Modifier.size(13.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = v14Crimson, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Admin", color = v14Crimson, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                soundManager.playSnapPop()
                                showLogoutConfirmationDialog = true
                            }
                            .padding(vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sign Out Session", color = Color(0xFFF5F5F7), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("End administrator session and return to login", color = Color(0xFF71717A), fontSize = 11.5.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFFF4D6D), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Sign Out", color = Color(0xFFFF4D6D), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, isDanger: Boolean = false) {
    Text(
        text = title,
        color = if (isDanger) Color(0xFFEF4444) else Color(0xFF71717A),
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
fun SettingsGroupedToggle(
    label: String,
    subtitle: String? = null,
    initial: Boolean = false,
    onSound: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("VelorixAdminSettings", android.content.Context.MODE_PRIVATE) }
    var checked by remember { mutableStateOf(sharedPrefs.getBoolean(label, initial)) }
    val sanitizedKey = remember(label) { label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_') }

    fun updateToggle(next: Boolean) {
        checked = next
        sharedPrefs.edit().putBoolean(label, next).apply()
        onSound?.invoke()
        try {
            com.google.firebase.database.FirebaseDatabase.getInstance()
                .reference.child("admin_settings").child("policies").child(sanitizedKey).setValue(next)
        } catch (_: Exception) {}
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { updateToggle(!checked) }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = { next -> updateToggle(next) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = VelorixAccent,
                uncheckedTrackColor = Color(0xFF18181F)
            )
        )
    }
}

@Composable
fun SettingsModuleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = VelorixAccentLight, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF71717A), modifier = Modifier.size(15.dp))
    }
}

@Composable
fun SettingsToggle(label: String, initial: Boolean, subtitle: String? = null) {
    SettingsGroupedToggle(label = label, subtitle = subtitle, initial = initial)
}

@Composable
fun UsersManagementScreenContent(
    uiState: DashboardState,
    onToggleBan: (UserProfile) -> Unit,
    onAddFunds: (UserProfile, Double) -> Unit,
    onAddFundsBucket: ((UserProfile, Double, com.example.domain.model.WalletBucket, String) -> Unit)? = null,
    onRecordMatchWinning: ((UserProfile, String, String, Int, Int, Double, Double) -> Unit)? = null,
    onDeleteUser: (UserProfile) -> Unit,
    onUpdateUser: (UserProfile) -> Unit = {},
    onRefreshUserData: (() -> Unit)? = null,
    onUnbanGuestSessions: (() -> Unit)? = null,
    banAppeals: List<com.example.domain.model.UnifiedAdminRequest> = emptyList(),
    onApproveBanAppeal: ((com.example.domain.model.UnifiedAdminRequest, String) -> Unit)? = null,
    onRejectBanAppeal: ((com.example.domain.model.UnifiedAdminRequest, String) -> Unit)? = null
) {
    val successState = uiState as? DashboardState.Success
    val usersList = successState?.users ?: emptyList()
    val tournamentsList = successState?.tournaments ?: emptyList()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedTournamentFilterId by remember { mutableStateOf<String?>(null) }

    var selectedUserForFunds by remember { mutableStateOf<UserProfile?>(null) }

    var inspectedUser by remember { mutableStateOf<UserProfile?>(null) }

    var showAddUserDialog by remember { mutableStateOf(false) }
    var newUserEmail by remember { mutableStateOf("") }
    var newUsername by remember { mutableStateOf("") }
    var newUserGameId by remember { mutableStateOf("") }
    var newUserInitialDeposit by remember { mutableStateOf("0") }
    var newUserInitialWinning by remember { mutableStateOf("0") }
    var newUserInitialReferralBonus by remember { mutableStateOf("0") }

    // Keep inspected user and funds dialog user fresh if data updates in stream
    val activeInspectedUser = remember(usersList, inspectedUser) {
        if (inspectedUser == null) null
        else usersList.find { it.id == inspectedUser?.id } ?: inspectedUser
    }
    val activeSelectedUserForFunds = remember(usersList, selectedUserForFunds) {
        if (selectedUserForFunds == null) null
        else usersList.find { it.id == selectedUserForFunds?.id } ?: selectedUserForFunds
    }

    val pendingBanAppeals = remember(banAppeals) {
        banAppeals.filter {
            !it.status.equals("RESOLVED", true) && !it.status.equals("APPROVED", true) && !it.status.equals("REJECTED", true)
        }
    }

    val filteredBanAppeals = remember(banAppeals, searchQuery) {
        banAppeals.filter { req ->
            searchQuery.isBlank() ||
                req.id.contains(searchQuery, ignoreCase = true) ||
                req.relatedId.contains(searchQuery, ignoreCase = true) ||
                req.username.contains(searchQuery, ignoreCase = true) ||
                req.inGameName.contains(searchQuery, ignoreCase = true) ||
                req.userEmail.contains(searchQuery, ignoreCase = true) ||
                req.userId.contains(searchQuery, ignoreCase = true) ||
                req.description.contains(searchQuery, ignoreCase = true)
        }.sortedWith(
            compareBy<com.example.domain.model.UnifiedAdminRequest> {
                when (it.status.uppercase()) {
                    "PENDING" -> 0
                    "IN_REVIEW", "INVESTIGATING" -> 1
                    "REJECTED" -> 2
                    else -> 3
                }
            }.thenByDescending { it.updatedAt }
        )
    }

    val filteredUsers = remember(usersList, searchQuery, selectedFilter, selectedTournamentFilterId, tournamentsList) {
        usersList.filter { u ->
            val emailLower = u.email.trim().lowercase()
            val isMock = emailLower.contains("@example.com") || emailLower.contains("mock.dummy")
            if (isMock || u.isGuestSession) return@filter false

            val matchesQuery = searchQuery.isBlank() ||
                u.username.contains(searchQuery, ignoreCase = true) ||
                u.ign.contains(searchQuery, ignoreCase = true) ||
                u.email.contains(searchQuery, ignoreCase = true) ||
                u.gameId.contains(searchQuery, ignoreCase = true) ||
                u.id.contains(searchQuery, ignoreCase = true) ||
                u.banCaseId.contains(searchQuery, ignoreCase = true) ||
                u.activeAppealTicketId.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "BANNED" -> u.isBanned
                "ACTIVE" -> !u.isBanned
                "ADMIN" -> u.role.contains("admin", ignoreCase = true)
                "JOINED_MATCH" -> u.totalMatchesJoined > 0 || u.matchHistory.isNotEmpty()
                "NO_MATCH" -> u.totalMatchesJoined == 0 && u.matchHistory.isEmpty()
                "BAN_APPEALS" -> u.isBanned || u.activeAppealTicketId.isNotBlank()
                else -> true
            }

            val matchesTournament = if (selectedTournamentFilterId == null) true else {
                val targetTourney = tournamentsList.find { it.id == selectedTournamentFilterId }
                targetTourney?.registrations?.any { r ->
                    r.userId == u.id || r.playerId == u.id ||
                    (u.gameId.isNotBlank() && (r.gameId == u.gameId || r.gameAccountId == u.gameId)) ||
                    (u.ign.isNotBlank() && (r.playerName.equals(u.ign, ignoreCase = true) || r.gameUsername.equals(u.ign, ignoreCase = true))) ||
                    (u.username.isNotBlank() && (r.playerName.equals(u.username, ignoreCase = true) || r.gameUsername.equals(u.username, ignoreCase = true)))
                } == true || u.joinedMatchIds.contains(selectedTournamentFilterId)
            }

            matchesQuery && matchesFilter && matchesTournament
        }
    }

    if (showAddUserDialog) {
        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            title = { Text("Register / Sync Player Profile", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Directly add or sync a player account with structured database sections.", color = Color.LightGray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = newUserEmail,
                        onValueChange = { newUserEmail = it },
                        label = { Text("Email Address*", color = Color.Gray) },
                        placeholder = { Text("e.g. player@gmail.com") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("Username / IGN", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newUserGameId,
                        onValueChange = { newUserGameId = it },
                        label = { Text("Free Fire / Game ID", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder),
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.1f))
                    Text("Initial 3-Section Wallet Balances:", color = Color(0xFFA1A1AA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = newUserInitialDeposit,
                        onValueChange = { newUserInitialDeposit = it },
                        label = { Text("1. Deposited Money (₹) [Non-Withdrawable]", color = V14AccentBright, fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = V14AccentBright, unfocusedBorderColor = V14Border),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newUserInitialWinning,
                        onValueChange = { newUserInitialWinning = it },
                        label = { Text("2. Winning Money (₹) [Withdrawable]", color = Color(0xFF34D399), fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF34D399), unfocusedBorderColor = V14Border),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newUserInitialReferralBonus,
                        onValueChange = { newUserInitialReferralBonus = it },
                        label = { Text("3. Referral Bonus Money (₹) [Non-Withdrawable]", color = Color(0xFFFBBF24), fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFBBF24), unfocusedBorderColor = V14Border),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUserEmail.isNotBlank()) {
                            val now = System.currentTimeMillis()
                            val uid = try {
                                com.google.firebase.database.FirebaseDatabase.getInstance().reference.child("users").push().key
                            } catch (_: Exception) {
                                null
                            } ?: "usr_$now"
                            val depAmt = (newUserInitialDeposit.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                            val winAmt = (newUserInitialWinning.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                            val bonAmt = (newUserInitialReferralBonus.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                            val totalAmt = depAmt + winAmt + bonAmt
                            val user = UserProfile(
                                id = uid,
                                username = newUsername.ifBlank { newUserEmail.substringBefore("@") },
                                ign = newUsername.ifBlank { newUserEmail.substringBefore("@") },
                                email = newUserEmail.trim(),
                                funds = totalAmt,
                                balance = totalAmt,
                                depositFunds = depAmt,
                                winningFunds = winAmt,
                                bonusFunds = bonAmt,
                                referralBonusEarned = bonAmt,
                                totalDeposited = depAmt,
                                totalEarnings = winAmt,
                                gameId = newUserGameId.trim(),
                                createdAt = now,
                                firstAppJoinAt = now,
                                lastActive = now
                            )
                            onUpdateUser(user)
                            showAddUserDialog = false
                            newUserEmail = ""
                            newUsername = ""
                            newUserGameId = ""
                            newUserInitialDeposit = "0"
                            newUserInitialWinning = "0"
                            newUserInitialReferralBonus = "0"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = V14AccentPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Add Player to DB", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUserDialog = false }) {
                    Text("Cancel", color = V14TextSecondary)
                }
            },
            containerColor = V14CardSurface,
            shape = RoundedCornerShape(18.dp)
        )
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

    // Full User Detail Inspector Dialog
    activeInspectedUser?.let { user ->
        UserDetailsInspectorDialog(
            user = user,
            allUsers = usersList,
            tournaments = tournamentsList,
            onDismiss = { inspectedUser = null },
            onToggleBan = { 
                onToggleBan(it)
                inspectedUser = it.copy(isBanned = !it.isBanned)
            },
            onAddFunds = { u, amt -> onAddFunds(u, amt) },
            onAddFundsBucket = onAddFundsBucket,
            onRecordMatchWinning = onRecordMatchWinning,
            onUpdateUser = { updatedUser ->
                onUpdateUser(updatedUser)
                inspectedUser = updatedUser
            },
            onDeleteUser = {
                onDeleteUser(it)
                inspectedUser = null
            }
        )
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)) {
        // Clean Full-Width Header + Flat Hairline Action Strip (Zero Truncation, Zero Boxed Buttons)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 6.dp)
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
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        tint = VelorixAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Players",
                        color = VelorixTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFF34D399), CircleShape)
                    )
                    Text(
                        text = "${usersList.count { !it.isGuestSession }}",
                        color = Color(0xFFA1A1AA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }

                val context = LocalContext.current
                val isUserSyncing = (uiState as? DashboardState.Success)?.isSyncing ?: false
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF14141B))
                            .border(0.75.dp, Color(0xFF1E1E27), CircleShape)
                            .clickable { showAddUserDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = "Add Player",
                            tint = Color(0xFFE4E4E7),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (onRefreshUserData != null) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(VelorixAccent.copy(alpha = 0.14f))
                                .border(0.75.dp, VelorixAccent.copy(alpha = 0.35f), CircleShape)
                                .clickable {
                                    Toast.makeText(context, "Syncing live players from Firebase...", Toast.LENGTH_SHORT).show()
                                    onRefreshUserData()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUserSyncing) {
                                CircularProgressIndicator(
                                    color = VelorixAccent,
                                    strokeWidth = 1.8.dp,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription = "Sync RTDB",
                                    tint = VelorixAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (onUnbanGuestSessions != null) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF14141B))
                                .border(0.75.dp, Color(0xFF1E1E27), CircleShape)
                                .clickable {
                                    Toast.makeText(context, "Unbanning guest sessions...", Toast.LENGTH_SHORT).show()
                                    onUnbanGuestSessions()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = "Unban Guests",
                                tint = Color(0xFFA1A1AA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
        }

        // Flat AMOLED Search Bar with bottom hairline
        Column(modifier = Modifier.fillMaxWidth().background(Color.Black)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF71717A),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            "Search player by name, email, game ID or UID...",
                            color = Color(0xFF71717A),
                            fontSize = 13.sp
                        )
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 13.5.sp
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(VelorixAccent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { searchQuery = "" }
                    )
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.12f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Flat Underline Mode Switcher (No Box-in-Box)
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            listOf(
                Triple("ALL", "All Players", usersList.count { !it.isGuestSession }),
                Triple("BANNED", "Banned", usersList.count { !it.isGuestSession && it.isBanned }),
                Triple("BAN_APPEALS", "Ban Appeals", pendingBanAppeals.size.takeIf { it > 0 } ?: banAppeals.size),
                Triple("ACTIVE", "Active", usersList.count { !it.isGuestSession && !it.isBanned }),
                Triple("JOINED_MATCH", "Joined Match", usersList.count { !it.isGuestSession && (it.totalMatchesJoined > 0 || it.matchHistory.isNotEmpty()) }),
                Triple("NO_MATCH", "No Match Yet", usersList.count { !it.isGuestSession && it.totalMatchesJoined == 0 && it.matchHistory.isEmpty() }),
                Triple("ADMIN", "Staff", usersList.count { !it.isGuestSession && it.role.contains("admin", ignoreCase = true) })
            ).forEach { (key, label, count) ->
                val isSel = selectedFilter == key
                val isUrgentAppeals = key == "BAN_APPEALS" && pendingBanAppeals.isNotEmpty()
                val textColor = when {
                    isSel -> Color.White
                    isUrgentAppeals -> Color(0xFFFFB300)
                    else -> Color(0xFF71717A)
                }

                Column(
                    modifier = Modifier
                        .clickable { selectedFilter = key }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$label ($count)",
                        fontSize = 12.sp,
                        fontWeight = if (isSel || isUrgentAppeals) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(1.5.dp)
                            .background(if (isSel) VelorixAccent else if (isUrgentAppeals) Color(0xFFFFB300).copy(alpha = 0.6f) else Color.Transparent)
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
        Spacer(modifier = Modifier.height(8.dp))

        // Multi-Tournament Roster Mode Filter (Underline text links, No Box-in-Box)
        if (tournamentsList.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.SportsEsports,
                    contentDescription = null,
                    tint = VelorixAccent,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    "Filter by Tournament Participation:",
                    color = Color(0xFFA1A1AA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                item {
                    val isAll = selectedTournamentFilterId == null
                    Column(
                        modifier = Modifier
                            .clickable { selectedTournamentFilterId = null }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "All Players (${usersList.count { !it.isGuestSession }})",
                            color = if (isAll) VelorixAccent else Color(0xFF71717A),
                            fontSize = 11.5.sp,
                            fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(1.5.dp)
                                .background(if (isAll) VelorixAccent else Color.Transparent)
                        )
                    }
                }
                items(tournamentsList) { tourney ->
                    val isSel = selectedTournamentFilterId == tourney.id
                    val registeredCount = tourney.registrations.size
                    Column(
                        modifier = Modifier
                            .clickable { selectedTournamentFilterId = if (isSel) null else tourney.id }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${tourney.title.take(18)} ($registeredCount)",
                            color = if (isSel) Color(0xFFEAB308) else Color(0xFF94A3B8),
                            fontSize = 11.5.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(1.5.dp)
                                .background(if (isSel) Color(0xFFEAB308) else Color.Transparent)
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(6.dp))
        }
        
        val distinctUsers = remember(filteredUsers) {
            val seen = mutableSetOf<String>()
            filteredUsers.filter { u ->
                val k = if (u.id.isNotBlank()) "id:${u.id}" else "email:${u.email.trim().lowercase()}"
                if (k.isNotBlank() && seen.add(k)) true else if (k.isBlank()) true else false
            }
        }

        val shortDateFmt = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Live Incoming Ban Appeals Banner when on ALL / BANNED tabs
            if (selectedFilter != "BAN_APPEALS" && pendingBanAppeals.isNotEmpty()) {
                item(key = "ban_appeals_alert_banner") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1A1006), RoundedCornerShape(12.dp))
                            .border(0.75.dp, Color(0xFFFFB300).copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .clickable { selectedFilter = "BAN_APPEALS" }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Gavel, contentDescription = "Pending Ban Appeals", tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
                            Column {
                                Text(
                                    text = "${pendingBanAppeals.size} Pending Ban Appeal${if (pendingBanAppeals.size > 1) "s" else ""} Caught Live",
                                    color = Color(0xFFFFB300),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = pendingBanAppeals.take(2).joinToString(" • ") { "${it.username} (${it.id})" },
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Text(
                            text = "Review →",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Dedicated Ban Appeals Catcher Desk inside Players Repository
            if (selectedFilter == "BAN_APPEALS") {
                item(key = "ban_appeals_header") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Live Ban Appeal Catcher (${filteredBanAppeals.size})",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Catches appeals from /ban_appeals, /appeals, /admin_requests, /support_tickets, /reports & /banned_users. 1-tap Approve simultaneously unbans the player & resolves their ticket.",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.5.sp
                        )
                    }
                }
                if (filteredBanAppeals.isEmpty()) {
                    item(key = "no_ban_appeals") {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No ban appeal tickets caught yet.", color = VelorixTextSecondary, fontSize = 13.sp)
                            Text("Any VRX-XXXXXX appeal submitted from Velorix Player app appears here live.", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                } else {
                    items(filteredBanAppeals, key = { "appeal_${it.id}_${it.sourceNode}" }) { req ->
                        val isPendingAppeal = !req.status.equals("RESOLVED", true) && !req.status.equals("APPROVED", true) && !req.status.equals("REJECTED", true)
                        val stColor = when (req.status.uppercase()) {
                            "RESOLVED", "APPROVED" -> Color(0xFF34D399)
                            "REJECTED" -> Color(0xFFF87171)
                            else -> Color(0xFFFFB300)
                        }
                        GlassCard(modifier = Modifier.fillMaxWidth(), onClick = null) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${req.username.ifBlank { req.inGameName.ifBlank { "Player" } }}${if (req.inGameName.isNotBlank() && !req.inGameName.equals(req.username, true)) " • IGN: ${req.inGameName}" else ""}",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Ticket: ${req.id}${if (req.relatedId.isNotBlank()) " • Sanction: ${req.relatedId}" else ""}",
                                            color = Color(0xFFFFB300),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(6.dp).background(stColor, CircleShape))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(req.status.uppercase(), color = stColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (req.userId.isNotBlank() || req.userEmail.isNotBlank()) {
                                    Text(
                                        text = listOfNotNull(
                                            req.userId.takeIf { it.isNotBlank() }?.let { "UID: $it" },
                                            req.userEmail.takeIf { it.isNotBlank() }
                                        ).joinToString(" • "),
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (req.description.isNotBlank() || req.subject.isNotBlank()) {
                                    Text(
                                        text = req.description.ifBlank { req.subject },
                                        color = Color.White.copy(alpha = 0.88f),
                                        fontSize = 11.5.sp
                                    )
                                }
                                Text(
                                    text = "Source: /${req.sourceNode} • ${shortDateFmt.format(Date(req.createdAt))}",
                                    color = Color(0xFF71717A),
                                    fontSize = 9.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(Color(0xFF34D399).copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                                            .border(0.75.dp, Color(0xFF34D399).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                            .clickable {
                                                onApproveBanAppeal?.invoke(req, "Ban appeal approved by Admin. Account unbanned and restored to ACTIVE.")
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Approve & Unban", tint = Color(0xFF34D399), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (isPendingAppeal) "Approve & Unban" else "Re-Approve & Unban",
                                            color = Color(0xFF34D399),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                    if (isPendingAppeal) {
                                        Row(
                                            modifier = Modifier
                                                .weight(0.85f)
                                                .background(Color(0xFFF87171).copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                                                .border(0.75.dp, Color(0xFFF87171).copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                                .clickable {
                                                    onRejectBanAppeal?.invoke(req, "Ban appeal rejected after Fair Play / Anti-Cheat review.")
                                                }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Cancel, contentDescription = "Reject Appeal", tint = Color(0xFFF87171), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("Reject", color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (selectedFilter != "BAN_APPEALS" && distinctUsers.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.PersonSearch, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No matching users found.", color = VelorixTextSecondary, fontSize = 14.sp)
                    }
                }
            }
            if (selectedFilter != "BAN_APPEALS") {
            items(distinctUsers, key = { "${it.id}_${it.email}_${distinctUsers.indexOf(it)}" }) { user ->
                val matchingAppeal = remember(banAppeals, user) {
                    banAppeals.firstOrNull { req ->
                        (req.userId.isNotBlank() && req.userId.equals(user.id, true)) ||
                            (user.activeAppealTicketId.isNotBlank() && req.id.equals(user.activeAppealTicketId, true)) ||
                            (user.banCaseId.isNotBlank() && req.relatedId.equals(user.banCaseId, true)) ||
                            (user.email.contains("@") && req.userEmail.equals(user.email, true)) ||
                            (user.ign.isNotBlank() && req.inGameName.equals(user.ign, true))
                    }
                }
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { inspectedUser = user },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            val isRecentlyActive = (System.currentTimeMillis() - user.lastActive) < 20 * 60 * 1000
                            UserAvatarView(
                                avatarUrl = user.avatarUrl,
                                name = user.bestDisplayName,
                                size = 46.dp,
                                isBanned = user.isBanned,
                                isSuspended = user.isSuspended,
                                role = user.role,
                                isActiveRecently = isRecentlyActive,
                                showGlow = true
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user.bestDisplayName,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (user.role.contains("admin", ignoreCase = true)) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val roleBadgeColor = if (user.role.contains("super", ignoreCase = true)) VelorixAmber else V14AccentBright
                                        Text(
                                            text = "• ${user.role.uppercase()}",
                                            color = roleBadgeColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                                Text(
                                    text = user.bestEmailOrPhone,
                                    color = VelorixTextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "UID: ${user.id}${if (user.ign.isNotBlank()) " • IGN: ${user.ign}" else ""}",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            val statusLabel = when {
                                user.isBanned -> "BANNED"
                                user.isSuspended -> "SUSPENDED"
                                else -> "ACTIVE"
                            }
                            val statusDotColor = when {
                                user.isBanned -> Color(0xFFFF1744)
                                user.isSuspended -> Color(0xFFFF9800)
                                else -> Color(0xFF34D399)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).background(statusDotColor, CircleShape))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(statusLabel, color = statusDotColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("DB Sections", color = Color(0xFFA1A1AA), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFA1A1AA), modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // Inline Restriction & Active Ban Appeal Status Box
                    if (user.isBanned || matchingAppeal != null || user.activeAppealTicketId.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val ticketIdDisplay = matchingAppeal?.id ?: user.activeAppealTicketId
                        val ticketStatusDisplay = (matchingAppeal?.status ?: user.activeAppealStatus).ifBlank { if (ticketIdDisplay.isNotBlank()) "PENDING" else "" }
                        val sanctionIdDisplay = user.banCaseId.ifBlank { matchingAppeal?.relatedId.orEmpty() }
                        val reasonDisplay = user.banReason.ifBlank { matchingAppeal?.description.orEmpty() }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1A090C), RoundedCornerShape(8.dp))
                                .border(0.6.dp, Color(0xFFFF1744).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (sanctionIdDisplay.isNotBlank()) "Sanction: PERMANENT ($sanctionIdDisplay)" else "Sanction: PERMANENT",
                                    color = Color(0xFFFF6B81),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (ticketIdDisplay.isNotBlank()) {
                                    Text(
                                        text = "$ticketIdDisplay ($ticketStatusDisplay)",
                                        color = if (ticketStatusDisplay.equals("RESOLVED", true)) Color(0xFF34D399) else Color(0xFFFFB300),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            if (reasonDisplay.isNotBlank()) {
                                Text(
                                    text = reasonDisplay,
                                    color = Color(0xFFD4D4D8),
                                    fontSize = 10.5.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 1: 3-Bucket Wallet Breakdown (Deposited vs Winning vs Bonus vs Stats)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Deposited", color = V14AccentBright, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            Text("₹${user.depositFunds.toInt()}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, softWrap = false)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Winning", color = Color(0xFF34D399), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            Text("₹${user.winningFunds.toInt()}", color = Color(0xFF34D399), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, softWrap = false)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bonus", color = Color(0xFFFBBF24), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            Text("₹${maxOf(user.bonusFunds, user.referralBonusEarned).toInt()}", color = Color(0xFFFBBF24), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, softWrap = false)
                        }
                        Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.End) {
                            Text("Record", color = VelorixTextSecondary, fontSize = 9.5.sp, maxLines = 1, softWrap = false)
                            Text("${user.totalMatchesJoined}M • ${user.wins}W • ${user.kills}K", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    // Row 2: App Join Time, First Match Join Time & Cases Summary
                    val appJoinStr = if (user.effectiveFirstAppJoinAt > 0L) shortDateFmt.format(Date(user.effectiveFirstAppJoinAt)) else "N/A"
                    val firstMatchStr = if (user.firstMatchJoinedAt > 0L) shortDateFmt.format(Date(user.firstMatchJoinedAt)) else "No Match Joined"
                    Text(
                        text = "App Join: $appJoinStr  •  1st Match: $firstMatchStr  •  Cases: ${user.supportCasesCount}",
                        color = Color(0xFFA1A1AA),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .border(0.75.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                                .clickable { inspectedUser = user }
                                .padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "Inspect Player DB", tint = Color.White, modifier = Modifier.size(14.dp))
                            Text("Inspect", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }

                        Row(
                            modifier = Modifier
                                .background(Color(0xFF34D399).copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                                .border(0.75.dp, Color(0xFF34D399).copy(alpha = 0.48f), RoundedCornerShape(10.dp))
                                .clickable { selectedUserForFunds = user }
                                .padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Send Funds", tint = Color(0xFF34D399), modifier = Modifier.size(14.dp))
                            Text("Wallet", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }

                        val ctx = LocalContext.current
                        val actionColor = if (user.isBanned) Color(0xFF34D399) else Color(0xFFF87171)
                        Row(
                            modifier = Modifier
                                .background(actionColor.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                                .border(0.75.dp, actionColor.copy(alpha = 0.48f), RoundedCornerShape(10.dp))
                                .clickable {
                                    if (!user.isBanned && user.isGuestSession) {
                                        Toast.makeText(ctx, "Guest sessions cannot be banned globally.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onToggleBan(user)
                                    }
                                }
                                .padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                if (user.isBanned) Icons.Default.CheckCircle else Icons.Default.Block,
                                contentDescription = if (user.isBanned) "Unban Player" else "Ban Player",
                                tint = actionColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (user.isBanned) "Unban" else "Ban",
                                color = actionColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        if (matchingAppeal != null && !matchingAppeal.status.equals("RESOLVED", true) && !matchingAppeal.status.equals("APPROVED", true)) {
                            Row(
                                modifier = Modifier
                                    .background(Color(0xFFFFB300).copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                                    .border(0.75.dp, Color(0xFFFFB300).copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        onApproveBanAppeal?.invoke(matchingAppeal, "Ban appeal approved by Admin. Account unbanned.")
                                    }
                                    .padding(horizontal = 11.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Default.Gavel, contentDescription = "Approve Appeal & Unban", tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                                Text("Approve Appeal", color = Color(0xFFFFB300), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
fun UserDetailsInspectorDialog(
    user: UserProfile,
    allUsers: List<UserProfile> = emptyList(),
    tournaments: List<Tournament> = emptyList(),
    onDismiss: () -> Unit,
    onToggleBan: (UserProfile) -> Unit,
    onAddFunds: (UserProfile, Double) -> Unit,
    onAddFundsBucket: ((UserProfile, Double, com.example.domain.model.WalletBucket, String) -> Unit)? = null,
    onRecordMatchWinning: ((UserProfile, String, String, Int, Int, Double, Double) -> Unit)? = null,
    onUpdateUser: (UserProfile) -> Unit,
    onDeleteUser: (UserProfile) -> Unit,
    onNavigateToTournament: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val joinedTournaments = remember(tournaments, user) {
        tournaments.filter { t ->
            user.joinedMatchIds.contains(t.id) ||
            t.registrations.any { r ->
                r.userId == user.id ||
                r.playerId == user.id ||
                (user.gameId.isNotBlank() && (r.gameId == user.gameId || r.gameAccountId == user.gameId)) ||
                (user.ign.isNotBlank() && (r.playerName.equals(user.ign, ignoreCase = true) || r.gameUsername.equals(user.ign, ignoreCase = true))) ||
                (user.username.isNotBlank() && (r.playerName.equals(user.username, ignoreCase = true) || r.gameUsername.equals(user.username, ignoreCase = true)))
            }
        }
    }

    val unifiedMatchRecords = remember(joinedTournaments, user) {
        val fromLiveTournaments = joinedTournaments.map { t ->
            val reg = t.registrations.find { r ->
                r.userId == user.id ||
                r.playerId == user.id ||
                (user.gameId.isNotBlank() && (r.gameId == user.gameId || r.gameAccountId == user.gameId)) ||
                (user.ign.isNotBlank() && (r.playerName.equals(user.ign, ignoreCase = true) || r.gameUsername.equals(user.ign, ignoreCase = true))) ||
                (user.username.isNotBlank() && (r.playerName.equals(user.username, ignoreCase = true) || r.gameUsername.equals(user.username, ignoreCase = true)))
            }
            val existingHist = user.matchHistory.find { it.tournamentId == t.id }
            val feePaid = existingHist?.entryFeePaid?.takeIf { it > 0.0 }
                ?: reg?.entryFeePaid?.takeIf { it > 0.0 }
                ?: t.entryFee.toDouble()
            com.example.domain.model.PlayerMatchRecord(
                tournamentId = t.id,
                tournamentTitle = t.title,
                game = t.game,
                category = t.category,
                format = t.format,
                map = t.map,
                joinedAt = existingHist?.joinedAt?.takeIf { it > 0L }
                    ?: reg?.joinedAt?.takeIf { it > 0L }
                    ?: (t.parsedStartsAtMillis ?: 0L),
                entryFeePaid = feePaid,
                paidFromDeposit = existingHist?.paidFromDeposit?.takeIf { it > 0.0 }
                    ?: reg?.paidFromDeposit?.takeIf { it > 0.0 }
                    ?: feePaid,
                paidFromWinning = existingHist?.paidFromWinning ?: reg?.paidFromWinning ?: 0.0,
                paidFromBonus = existingHist?.paidFromBonus ?: reg?.paidFromBonus ?: 0.0,
                kills = maxOf(existingHist?.kills ?: 0, reg?.kills ?: 0),
                rank = existingHist?.rank?.takeIf { it > 0 } ?: reg?.rank ?: 0,
                winningAmount = maxOf(existingHist?.winningAmount ?: 0.0, reg?.winningAmount ?: 0.0),
                status = t.status
            )
        }
        (fromLiveTournaments + user.matchHistory)
            .distinctBy { it.tournamentId }
            .sortedByDescending { it.joinedAt }
    }

    val referredPlayers = remember(allUsers, user) {
        allUsers.filter { other ->
            other.id != user.id && (
                (user.referralCode.isNotBlank() && other.referredBy.equals(user.referralCode, ignoreCase = true)) ||
                (user.id.isNotBlank() && other.referredBy.equals(user.id, ignoreCase = true)) ||
                (user.username.isNotBlank() && other.referredBy.equals(user.username, ignoreCase = true)) ||
                (other.rawAttributes["referredByCode"]?.equals(user.referralCode, ignoreCase = true) == true) ||
                (other.rawAttributes["referrerId"]?.equals(user.id, ignoreCase = true) == true)
            )
        }
    }

    // Editable state
    var editUsername by remember(user) { mutableStateOf(user.username) }
    var editEmail by remember(user) { mutableStateOf(user.email) }
    var editGameId by remember(user) { mutableStateOf(user.gameId) }
    var editPhone by remember(user) { mutableStateOf(user.phone) }
    var editRole by remember(user) { mutableStateOf(user.role) }
    var editWins by remember(user) { mutableStateOf(user.wins.toString()) }
    var editKills by remember(user) { mutableStateOf(user.kills.toString()) }
    var editActivityPoints by remember(user) { mutableStateOf(user.activityPoints.toString()) }
    var editEarnings by remember(user) { mutableStateOf(user.totalEarnings.toString()) }

    // 3-Bucket Wallet Control state in Tab 2
    var selectedControlBucket by remember { mutableStateOf(com.example.domain.model.WalletBucket.WINNING) }
    var bucketAmountInput by remember { mutableStateOf("100") }
    var bucketReasonInput by remember { mutableStateOf("Admin wallet bucket adjustment") }

    // Per-Match Winning & Kills Recording Dialog State
    var showRecordMatchDialog by remember { mutableStateOf(false) }
    var recordTourneyId by remember { mutableStateOf(tournaments.firstOrNull()?.id ?: "") }
    var recordTourneyTitle by remember { mutableStateOf(tournaments.firstOrNull()?.title ?: "Custom Match") }
    var recordEntryFeeInput by remember { mutableStateOf("50") }
    var recordKillsInput by remember { mutableStateOf("0") }
    var recordRankInput by remember { mutableStateOf("1") }
    var recordWinningInput by remember { mutableStateOf("100") }

    // Individual Toggles for each ID separately
    var isBannedState by remember(user.id, user.isBanned) { mutableStateOf(user.isBanned) }
    var banReasonInput by remember(user.id, user.banReason) { mutableStateOf(user.banReason.ifBlank { "Violation of tournament guidelines / Cheating" }) }
    var banCaseIdInput by remember(user.id, user.banCaseId) { mutableStateOf(user.banCaseId) }

    var isSuspendedState by remember(user.id, user.isSuspended) { mutableStateOf(user.isSuspended) }
    var suspensionReasonInput by remember(user.id, user.suspensionReason) { mutableStateOf(user.suspensionReason.ifBlank { "Temporary match suspension" }) }
    var selectedSuspensionHours by remember { mutableIntStateOf(24) }

    var isVpnBlockedState by remember(user.id, user.isVpnBlocked) { mutableStateOf(user.isVpnBlocked) }
    var isForceUpdateState by remember(user.id, user.isForceUpdateRequired) { mutableStateOf(user.isForceUpdateRequired) }
    var isMaintenanceBypassState by remember(user.id, user.isMaintenanceBypass) { mutableStateOf(user.isMaintenanceBypass) }
    var minVersionInput by remember(user.id, user.minVersionRequired) { mutableStateOf(user.minVersionRequired) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    fun triggerBucketAdjustment(delta: Double, bucket: com.example.domain.model.WalletBucket, reason: String) {
        if (onAddFundsBucket != null) {
            onAddFundsBucket(user, delta, bucket, reason)
        } else {
            onAddFunds(user, delta)
        }
    }

    fun buildUpdatedUser(): UserProfile {
        val suspendUntilTime = if (isSuspendedState) {
            if (user.suspendedUntil > System.currentTimeMillis()) user.suspendedUntil
            else System.currentTimeMillis() + (selectedSuspensionHours.toLong() * 3600 * 1000)
        } else 0L

        return user.copy(
            username = editUsername.trim().ifBlank { user.username },
            email = editEmail.trim().ifBlank { user.email },
            gameId = editGameId.trim(),
            ign = editUsername.trim().ifBlank { user.ign },
            phone = editPhone.trim(),
            role = editRole,
            wins = editWins.toIntOrNull() ?: user.wins,
            kills = editKills.toIntOrNull() ?: user.kills,
            activityPoints = editActivityPoints.toIntOrNull() ?: user.activityPoints,
            totalEarnings = editEarnings.toDoubleOrNull() ?: user.totalEarnings,
            isBanned = isBannedState,
            banReason = if (isBannedState) banReasonInput.trim() else "",
            banCaseId = if (isBannedState) banCaseIdInput.trim() else "",
            isSuspended = isSuspendedState,
            suspensionReason = if (isSuspendedState) suspensionReasonInput.trim() else "",
            suspendedUntil = suspendUntilTime,
            isVpnBlocked = isVpnBlockedState,
            isForceUpdateRequired = isForceUpdateState,
            minVersionRequired = minVersionInput.trim(),
            isMaintenanceBypass = isMaintenanceBypassState
        )
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()) }
    val firstAppJoinStr = remember(user.effectiveFirstAppJoinAt) {
        if (user.effectiveFirstAppJoinAt > 0L) dateFormat.format(Date(user.effectiveFirstAppJoinAt)) else "N/A (Early Account)"
    }
    val firstMatchJoinStr = remember(user.firstMatchJoinedAt, unifiedMatchRecords) {
        val earliest = unifiedMatchRecords.map { it.joinedAt }.filter { it > 0L }.minOrNull() ?: user.firstMatchJoinedAt
        if (earliest > 0L) dateFormat.format(Date(earliest)) else "Never Joined Any Match"
    }
    val lastMatchJoinStr = remember(user.lastMatchJoinedAt, unifiedMatchRecords) {
        val latest = unifiedMatchRecords.map { it.joinedAt }.filter { it > 0L }.maxOrNull() ?: user.lastMatchJoinedAt
        if (latest > 0L) dateFormat.format(Date(latest)) else "Never Joined Any Match"
    }
    val lastActiveStr = remember(user.lastActive) {
        if (user.lastActive > 0) {
            val formatted = dateFormat.format(Date(user.lastActive))
            val diffMs = System.currentTimeMillis() - user.lastActive
            val diffHours = diffMs / (1000 * 60 * 60)
            val diffDays = diffHours / 24
            val relative = when {
                diffHours < 1 -> "Just now"
                diffHours < 24 -> "${diffHours}h ago"
                else -> "${diffDays}d ago"
            }
            "$formatted ($relative)"
        } else "N/A"
    }

    if (showRecordMatchDialog) {
        AlertDialog(
            onDismissRequest = { showRecordMatchDialog = false },
            title = { Text("Record Match Participation & Winning", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Record match entry fee, kills & winning money for ${user.bestDisplayName}. Winning money is credited directly to their WITHDRAWABLE Winning Wallet.",
                        color = Color.LightGray,
                        fontSize = 11.5.sp
                    )
                    if (tournaments.isNotEmpty()) {
                        Text("Quick Select Tournament:", color = Color(0xFFA1A1AA), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(tournaments) { t ->
                                val isSel = recordTourneyId == t.id
                                TextButton(
                                    onClick = {
                                        recordTourneyId = t.id
                                        recordTourneyTitle = t.title
                                        recordEntryFeeInput = t.entryFee.toInt().toString()
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = t.title.take(18),
                                        color = if (isSel) VelorixAccent else Color.Gray,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = recordTourneyTitle,
                        onValueChange = { recordTourneyTitle = it },
                        label = { Text("Match / Tournament Title", color = Color.Gray, fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = recordEntryFeeInput,
                            onValueChange = { recordEntryFeeInput = it },
                            label = { Text("Entry Fee Paid (₹)", color = Color.Gray, fontSize = 10.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = recordKillsInput,
                            onValueChange = { recordKillsInput = it },
                            label = { Text("Match Kills", color = Color.Gray, fontSize = 10.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = recordRankInput,
                            onValueChange = { recordRankInput = it },
                            label = { Text("Match Rank (#)", color = Color.Gray, fontSize = 10.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = recordWinningInput,
                            onValueChange = { recordWinningInput = it },
                            label = { Text("Winning Money (₹)", color = Color(0xFF34D399), fontSize = 10.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF34D399), unfocusedBorderColor = CardVerifyBorder),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tId = recordTourneyId.ifBlank { "match_${System.currentTimeMillis()}" }
                        val tTitle = recordTourneyTitle.ifBlank { "Tournament Match" }
                        val fee = (recordEntryFeeInput.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                        val kills = (recordKillsInput.toIntOrNull() ?: 0).coerceAtLeast(0)
                        val rank = (recordRankInput.toIntOrNull() ?: 0).coerceAtLeast(0)
                        val winAmt = (recordWinningInput.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                        if (onRecordMatchWinning != null) {
                            onRecordMatchWinning(user, tId, tTitle, kills, rank, winAmt, fee)
                        } else {
                            triggerBucketAdjustment(winAmt, com.example.domain.model.WalletBucket.WINNING, "Match winning: $tTitle")
                        }
                        showRecordMatchDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399))
                ) {
                    Text("Save Match & Credit Winning", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordMatchDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = CardLiveBg
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete User Permanently?", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to permanently delete account ${user.username} (${user.id}) from all database nodes? This action cannot be undone.",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteUser(user)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete Account", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color.Black
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.Black,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.14f))
        ) {
            Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        val isRecentlyActive = (System.currentTimeMillis() - user.lastActive) < 20 * 60 * 1000
                        UserAvatarView(
                            avatarUrl = user.avatarUrl,
                            name = user.bestDisplayName,
                            size = 50.dp,
                            isBanned = isBannedState,
                            isSuspended = isSuspendedState,
                            role = editRole,
                            isActiveRecently = isRecentlyActive,
                            showGlow = true
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.bestDisplayName,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val statusDot = when {
                                    isBannedState -> Color(0xFFFF1744)
                                    isSuspendedState -> Color(0xFFFF9800)
                                    else -> Color(0xFF34D399)
                                }
                                Box(modifier = Modifier.size(6.dp).background(statusDot, CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when {
                                        isBannedState -> "BANNED"
                                        isSuspendedState -> "SUSPENDED"
                                        else -> "ACTIVE"
                                    },
                                    color = statusDot,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = user.bestEmailOrPhone,
                                color = VelorixTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                // Copy UID Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("UID: ", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = user.id,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                    TextButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(user.id))
                            Toast.makeText(context, "User ID copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = VelorixAccent, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy ID", color = VelorixAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                // Quick Admin Actions Strip: Distinct Bordered Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFF87171).copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                            .border(0.75.dp, Color(0xFFF87171).copy(alpha = 0.48f), RoundedCornerShape(10.dp))
                            .clickable {
                                val resetUser = user.copy(
                                    avatarUrl = "avatar_1",
                                    version = user.version + 1,
                                    updatedAt = System.currentTimeMillis()
                                )
                                onUpdateUser(resetUser)
                                Toast.makeText(context, "Avatar reset to avatar_1 (v${resetUser.version})!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Reset Avatar",
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Reset Avatar",
                            color = Color(0xFFF87171),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFFBBF24).copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                            .border(0.75.dp, Color(0xFFFBBF24).copy(alpha = 0.48f), RoundedCornerShape(10.dp))
                            .clickable {
                                val tokenUser = user.copy(
                                    tokens = user.tokens + 50,
                                    version = user.version + 1,
                                    updatedAt = System.currentTimeMillis()
                                )
                                onUpdateUser(tokenUser)
                                Toast.makeText(context, "+50 Tokens credited -> ${tokenUser.tokens} Tokens", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Default.Stars,
                            contentDescription = "Grant +50 Tokens",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "+50 (${user.tokens})",
                            color = Color(0xFFFBBF24),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Row(
                        modifier = Modifier
                            .background(V14CardSurface, RoundedCornerShape(10.dp))
                            .border(1.dp, V14Border, RoundedCornerShape(10.dp))
                            .clickable {
                                val dpdpJson = """{"userId":"${user.id}","username":"${user.username}","email":"${user.email}","inGameName":"${user.effectiveIgn}","freeFireId":"${user.gameId}","depositBalance":${user.depositFunds},"winningsBalance":${user.winningFunds},"bonusBalance":${user.bonusFunds},"balance":${user.totalWalletBalance},"tokens":${user.tokens},"joinedMatchesCount":${unifiedMatchRecords.size},"version":${user.version},"exportedAt":${System.currentTimeMillis()}}"""
                                clipboard.setText(AnnotatedString(dpdpJson))
                                Toast.makeText(context, "DPDP JSON snapshot copied!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = "Copy DPDP JSON",
                            tint = V14TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Export JSON",
                            color = V14TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                // Icon Tabs: 0 -> Overview, 1 -> Matches, 2 -> Wallet, 3 -> Raw Data
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Black,
                    contentColor = VelorixAccent,
                    divider = { HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f)) },
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = VelorixAccent
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Overview", modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = "Matches", modifier = Modifier.size(17.dp))
                                Text("${unifiedMatchRecords.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet & Access", modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.DataObject, contentDescription = "Raw Data", modifier = Modifier.size(18.dp)) }
                    )
                }

                // Tab Contents
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black).padding(14.dp)) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { width -> width / 3 } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> -width / 3 } + fadeOut()
                                )
                            } else {
                                (slideInHorizontally { width -> -width / 3 } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> width / 3 } + fadeOut()
                                )
                            }
                        },
                        label = "dialog_tab_content"
                    ) { currentTab ->
                        when (currentTab) {
                        0 -> {
                            // Structured Database Sections & Account Metadata
                            val totalMatchesCount = maxOf(user.totalMatchesJoined, unifiedMatchRecords.size, user.joinedMatchIds.size)
                            val hasJoinedMatches = totalMatchesCount > 0
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Section 01: Account Lifecycle & Match Join Timestamps (DB: accountLifecycle)
                                DetailSectionCard(title = "01 · DB SECTION: ACCOUNT LIFECYCLE & JOIN TIMESTAMPS", icon = Icons.Default.Schedule) {
                                    DetailItemRow(label = "First Joined App At", value = firstAppJoinStr, isHighlight = true)
                                    DetailItemRow(label = "First Match Joined At", value = firstMatchJoinStr, isHighlight = hasJoinedMatches)
                                    DetailItemRow(label = "Latest Match Joined At", value = lastMatchJoinStr, isHighlight = hasJoinedMatches)
                                    DetailItemRow(
                                        label = "Has Joined Any Match?",
                                        value = if (hasJoinedMatches) "YES ($totalMatchesCount Matches Joined)" else "NO (Has Not Joined Any Match)",
                                        isHighlight = hasJoinedMatches
                                    )
                                    DetailItemRow(label = "Last Active / Session", value = lastActiveStr)
                                    DetailItemRow(label = "Support Cases / Tickets Filed", value = "${user.supportCasesCount} Cases", isHighlight = user.supportCasesCount > 0)
                                }

                                // Section 02: 3-Bucket Wallet & Withdrawal Rules (DB: walletAndFinanceSummary)
                                DetailSectionCard(title = "02 · DB SECTION: 3-BUCKET WALLET & WITHDRAWAL RULES", icon = Icons.Default.AccountBalanceWallet) {
                                    DetailItemRow(
                                        label = "Winning Money (Withdrawable)",
                                        value = "₹${"%.2f".format(user.winningFunds)} [WITHDRAWABLE]",
                                        isHighlight = user.winningFunds > 0
                                    )
                                    DetailItemRow(
                                        label = "Deposited Money (Entry Only)",
                                        value = "₹${"%.2f".format(user.depositFunds)} [NON-WITHDRAWABLE]",
                                        isHighlight = false
                                    )
                                    DetailItemRow(
                                        label = "Bonus Money (Promotional)",
                                        value = "₹${"%.2f".format(user.bonusFunds)} [NON-WITHDRAWABLE]",
                                        isHighlight = false
                                    )
                                    DetailItemRow(
                                        label = "Total Combined Wallet",
                                        value = "₹${"%.2f".format(user.totalWalletBalance)}",
                                        isHighlight = true
                                    )
                                    DetailItemRow(
                                        label = "Withdrawal Request Status",
                                        value = if (user.canRequestWithdrawal) "ALLOWED (Up to ₹${"%.0f".format(user.winningFunds)} Winning)" else "LOCKED (₹0 Winning • Deposit Cannot Be Withdrawn)",
                                        isHighlight = user.canRequestWithdrawal
                                    )
                                    DetailItemRow(
                                        label = "Total Lifetime Deposited",
                                        value = "₹${"%.2f".format(maxOf(user.totalDeposited, user.depositFunds))}"
                                    )
                                    DetailItemRow(
                                        label = "Total Entry Fees Paid",
                                        value = "₹${"%.2f".format(maxOf(user.totalEntryFeesPaid, unifiedMatchRecords.sumOf { it.entryFeePaid }))}"
                                    )
                                    DetailItemRow(
                                        label = "Total Lifetime Winning Earned",
                                        value = "₹${"%.2f".format(maxOf(user.totalEarnings, user.winningFunds, unifiedMatchRecords.sumOf { it.winningAmount }))}",
                                        isHighlight = user.totalEarnings > 0 || user.winningFunds > 0
                                    )
                                    DetailItemRow(
                                        label = "Total Lifetime Withdrawn",
                                        value = "₹${"%.2f".format(user.totalWithdrawn)}"
                                    )
                                    DetailItemRow(label = "VT Loyalty Tokens", value = "${user.tokens} Tokens", isHighlight = user.tokens > 0)
                                }

                                // Section 03: Combat, Cases & Match Participation Summary (DB: combatAndMatchSummary)
                                DetailSectionCard(title = "03 · DB SECTION: COMBAT, CASES & MATCH SUMMARY", icon = Icons.Default.SportsEsports) {
                                    DetailItemRow(label = "Free Fire IGN", value = user.effectiveIgn, isHighlight = user.effectiveIgn != "Player")
                                    DetailItemRow(label = "Free Fire Game UID", value = user.gameId.ifBlank { "Not Linked" }, isHighlight = user.gameId.isNotBlank())
                                    DetailItemRow(label = "Total Matches Joined", value = "$totalMatchesCount Matches", isHighlight = totalMatchesCount > 0)
                                    DetailItemRow(label = "Matches Won", value = "${user.wins} Wins", isHighlight = user.wins > 0)
                                    DetailItemRow(label = "Total Kills Recorded", value = "${user.kills} Kills", isHighlight = user.kills > 0)
                                    DetailItemRow(label = "Support Cases / Disputes", value = "${user.supportCasesCount} Cases", isHighlight = user.supportCasesCount > 0)
                                    DetailItemRow(label = "Activity Reward Points", value = "${user.activityPoints} pts")
                                    val joinedNames = if (unifiedMatchRecords.isNotEmpty()) {
                                        unifiedMatchRecords.joinToString(", ") { it.tournamentTitle.ifBlank { it.tournamentId } }
                                    } else if (user.joinedMatchIds.isNotEmpty()) {
                                        user.joinedMatchIds.joinToString(", ")
                                    } else "None"
                                    DetailItemRow(label = "Joined Matches List", value = joinedNames, isHighlight = hasJoinedMatches)
                                }

                                // Daily Streak, Missions & Referrals
                                DetailSectionCard(title = "04 · DAILY STREAK, MISSIONS & REFERRALS", icon = Icons.Default.MilitaryTech) {
                                    DetailItemRow(
                                        label = "Daily Login Streak",
                                        value = if (user.loginStreak > 0) "${user.loginStreak} Days Active Streak" else "No active streak",
                                        isHighlight = user.loginStreak > 0
                                    )
                                    DetailItemRow(
                                        label = "Daily Missions Claimed",
                                        value = "${user.dailyMissionsTokensClaimed} Tokens Claimed"
                                    )
                                    DetailItemRow(
                                        label = "Last Mission Claim Date",
                                        value = user.lastMissionClaimDate.ifBlank { "Never" }
                                    )
                                    DetailItemRow(
                                        label = "Referral Code",
                                        value = user.referralCode.ifBlank { "Not generated" },
                                        isHighlight = user.referralCode.isNotBlank()
                                    )
                                    DetailItemRow(
                                        label = "Referred By (Inviter)",
                                        value = user.referredBy.ifBlank { "Direct Organic Signup" }
                                    )
                                    DetailItemRow(
                                        label = "Total Players Referred",
                                        value = "${if (referredPlayers.isNotEmpty()) referredPlayers.size else user.referralCount} Users",
                                        isHighlight = user.referralCount > 0 || referredPlayers.isNotEmpty()
                                    )
                                    DetailItemRow(
                                        label = "Referral Bonus Earned",
                                        value = "₹${user.referralBonusEarned}",
                                        isHighlight = user.referralBonusEarned > 0
                                    )
                                }

                                // Itemized List of Referred Players (Flat Hairline Rows, No Nested Boxes)
                                DetailSectionCard(
                                    title = "REFERRED PLAYERS NETWORK (${referredPlayers.size} VERIFIED)",
                                    icon = Icons.Default.Group
                                ) {
                                    if (referredPlayers.isEmpty()) {
                                        Text(
                                            "No players have registered via referral code ${user.referralCode.ifBlank { "N/A" }} yet.",
                                            fontSize = 11.5.sp,
                                            color = VelorixTextSecondary,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                    } else {
                                        Column {
                                            referredPlayers.forEachIndexed { idx, refUser ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "#${idx + 1}",
                                                            color = VelorixAccent,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column {
                                                            Text(
                                                                text = refUser.bestDisplayName,
                                                                color = Color.White,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Text(
                                                                text = "IGN: ${refUser.effectiveIgn} • FF UID: ${refUser.gameId.ifBlank { "Unlinked" }}",
                                                                color = VelorixTextSecondary,
                                                                fontSize = 10.sp
                                                            )
                                                            if (refUser.createdAt > 0) {
                                                                val refDateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(refUser.createdAt))
                                                                Text(
                                                                    text = "Registered: $refDateStr",
                                                                    color = Color(0xFF6B7280),
                                                                    fontSize = 9.sp
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            text = "ACTIVE",
                                                            color = Color(0xFF34D399),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "${refUser.wins}W • ${refUser.kills}K",
                                                            color = Color.LightGray,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.06f))
                                            }
                                        }
                                    }
                                }

                                // Age, KYC & Cash Tournament Compliance
                                DetailSectionCard(title = "05 · AGE, KYC & CASH MATCH ELIGIBILITY", icon = Icons.Default.VerifiedUser) {
                                    DetailItemRow(
                                        label = "Date of Birth",
                                        value = user.dateOfBirth.ifBlank { "Not submitted" }
                                    )
                                    DetailItemRow(
                                        label = "Calculated Age",
                                        value = if (user.age > 0) "${user.age} Years Old" else "Unverified Age"
                                    )
                                    DetailItemRow(
                                        label = "Under-18 / Minor Status",
                                        value = if (user.isMinor) "Under-18 (Minor Protected)" else "18+ Adult",
                                        isHighlight = user.isMinor
                                    )
                                    DetailItemRow(
                                        label = "Cash Tournaments Eligibility",
                                        value = if (user.canJoinCashTournaments) "ELIGIBLE FOR CASH MATCHES" else "RESTRICTED (Practice / Free Only)",
                                        isHighlight = user.canJoinCashTournaments
                                    )
                                    DetailItemRow(
                                        label = "Age Self-Confirmed",
                                        value = if (user.ageConfirmed) "Yes (Confirmed by Player)" else "Pending Confirmation"
                                    )
                                }

                                // Identity & Contact Details
                                DetailSectionCard(title = "06 · IDENTITY & DEVICE METADATA", icon = Icons.Default.Person) {
                                    DetailItemRow(label = "Display Name", value = user.bestDisplayName, isHighlight = true)
                                    DetailItemRow(label = "Free Fire IGN", value = user.effectiveIgn, isHighlight = user.effectiveIgn != "Player")
                                    DetailItemRow(label = "Email Address", value = user.email.ifBlank { if (user.bestEmailOrPhone.contains("@")) user.bestEmailOrPhone else "Not linked" })
                                    DetailItemRow(label = "Phone Number", value = user.phone.ifBlank { if (!user.phoneOrEmail.contains("@") && user.phoneOrEmail.isNotBlank()) user.phoneOrEmail else "Not linked" }, isHighlight = user.phone.isNotBlank() || user.phoneOrEmail.isNotBlank())
                                    DetailItemRow(label = "Free Fire / Game UID", value = user.gameId.ifBlank { "Not Linked" }, isHighlight = user.gameId.isNotBlank())
                                    DetailItemRow(label = "User Identifier (UID)", value = user.id)
                                    val photoStatus = if (!user.avatarUrl.isNullOrBlank()) "Linked Profile Picture" else "Generated Dynamic Avatar"
                                    DetailItemRow(label = "Profile Picture", value = photoStatus, isHighlight = !user.avatarUrl.isNullOrBlank())
                                    val device = user.deviceModel.ifBlank { user.rawAttributes["deviceModel"] ?: user.rawAttributes["model"] ?: "Android Client" }
                                    DetailItemRow(label = "Last Known Device", value = device)
                                    val ip = user.ipAddress.ifBlank { user.rawAttributes["ipAddress"] ?: user.rawAttributes["ip"] ?: "Dynamic Cellular" }
                                    DetailItemRow(label = "Last Known IP", value = ip)
                                }

                                // Active Security & Restriction Badges Card
                                DetailSectionCard(title = "07 · SECURITY & RESTRICTION STATUS", icon = Icons.Default.Shield) {
                                    DetailItemRow(
                                        label = "Account Ban Status",
                                        value = if (isBannedState) "BANNED: ${banReasonInput}" else "CLEAN (Active)",
                                        isHighlight = isBannedState
                                    )
                                    DetailItemRow(
                                        label = "Temporary Suspension",
                                        value = if (isSuspendedState) "SUSPENDED (${suspensionReasonInput})" else "Not Suspended",
                                        isHighlight = isSuspendedState
                                    )
                                    DetailItemRow(
                                        label = "VPN & Proxy Shield",
                                        value = if (isVpnBlockedState) "BLOCKED (VPN Forbidden)" else "Standard Allowed",
                                        isHighlight = isVpnBlockedState
                                    )
                                    DetailItemRow(
                                        label = "Mandatory App Update",
                                        value = if (isForceUpdateState) "ENFORCED" else "Standard",
                                        isHighlight = isForceUpdateState
                                    )
                                    DetailItemRow(
                                        label = "Maintenance Mode Bypass",
                                        value = if (isMaintenanceBypassState) "VIP BYPASS ENABLED" else "Standard (No Bypass)",
                                        isHighlight = isMaintenanceBypassState
                                    )
                                }
                            }
                        }
                        1 -> {
                            // Joined Tournaments, Timestamps, Entry Fees Paid & Per-Match Winnings (DB: joinedMatches)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "PER-MATCH PARTICIPATION & WINNING LEDGER",
                                            color = VelorixAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp
                                        )
                                        Text(
                                            text = "${unifiedMatchRecords.size} Matches Joined • First App Join: $firstAppJoinStr",
                                            color = Color.LightGray,
                                            fontSize = 11.sp
                                        )
                                    }
                                    TextButton(
                                        onClick = { showRecordMatchDialog = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Record Match / Win", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                                // Summary KPI Strip for Matches
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("1ST MATCH JOIN", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = if (user.firstMatchJoinedAt > 0L) SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(user.firstMatchJoinedAt)) else "Never",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column {
                                        Text("FEES PAID", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "₹${"%.0f".format(maxOf(user.totalEntryFeesPaid, unifiedMatchRecords.sumOf { it.entryFeePaid }))}",
                                            color = V14AccentBright,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Column {
                                        Text("MATCH KILLS", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${maxOf(user.kills, unifiedMatchRecords.sumOf { it.kills })} Kills",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("TOTAL WON", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "₹${"%.0f".format(maxOf(user.totalEarnings, unifiedMatchRecords.sumOf { it.winningAmount }))}",
                                            color = Color(0xFF34D399),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                                if (unifiedMatchRecords.isEmpty()) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.SportsEsports,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            "No Matches Joined Yet (hasJoinedAnyMatch = false)",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        )
                                        Text(
                                            "This player has not joined any match yet. Tap '+ Record Match / Win' above to log match participation or award winning balance.",
                                            color = Color.Gray,
                                            fontSize = 11.5.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                } else {
                                    unifiedMatchRecords.forEach { record ->
                                        val tourney = tournaments.find { it.id == record.tournamentId }
                                        val isLive = record.status.equals("live", ignoreCase = true)
                                        val isUpcoming = record.status.equals("upcoming", ignoreCase = true)
                                        val joinTimeFormatted = if (record.joinedAt > 0L) dateFormat.format(Date(record.joinedAt)) else "Recorded in DB"

                                        Column(
                                            modifier = Modifier.fillMaxWidth().background(V14Bg).padding(vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(
                                                                if (isLive) Color(0xFF34D399) else if (isUpcoming) VelorixAccent else Color.Gray,
                                                                CircleShape
                                                            )
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = record.tournamentTitle.ifBlank { "Match #${record.tournamentId}" },
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(
                                                    text = record.status.uppercase(),
                                                    color = if (isLive) Color(0xFF34D399) else Color.LightGray,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Text(
                                                text = "Match ID: ${record.tournamentId}  •  ${record.format} / ${record.map}",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 10.5.sp,
                                                fontFamily = FontFamily.Monospace
                                            )

                                            Column(modifier = Modifier.padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Exact Join Timestamp:", color = Color.Gray, fontSize = 10.5.sp)
                                                    Text(joinTimeFormatted, color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Entry Fee / Deposit Used:", color = Color.Gray, fontSize = 10.5.sp)
                                                    Text(
                                                        "₹${"%.0f".format(record.entryFeePaid)} (Dep: ₹${"%.0f".format(record.paidFromDeposit)} • Win: ₹${"%.0f".format(record.paidFromWinning)} • Bon: ₹${"%.0f".format(record.paidFromBonus)})",
                                                        color = V14AccentBright,
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Kills & Rank in Match:", color = Color.Gray, fontSize = 10.5.sp)
                                                    Text(
                                                        "${record.kills} Kills  •  Rank: ${if (record.rank > 0) "#${record.rank}" else "Unranked"}",
                                                        color = Color.White,
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Winning Money in Match:", color = Color.Gray, fontSize = 10.5.sp)
                                                    Text(
                                                        "₹${"%.2f".format(record.winningAmount)} (Withdrawable)",
                                                        color = if (record.winningAmount > 0) Color(0xFF34D399) else Color.LightGray,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                val room = tourney?.roomDetails
                                                if (room != null && room.roomId.isNotBlank()) {
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Text("Room ID & Pass:", color = Color.Gray, fontSize = 10.5.sp)
                                                        Text(
                                                            "${room.roomId} / ${room.roomPassword.ifBlank { "N/A" }}",
                                                            color = Color(0xFF34D399),
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(
                                                    onClick = {
                                                        recordTourneyId = record.tournamentId
                                                        recordTourneyTitle = record.tournamentTitle
                                                        recordEntryFeeInput = record.entryFeePaid.toInt().toString()
                                                        recordKillsInput = record.kills.toString()
                                                        recordRankInput = (if (record.rank > 0) record.rank else 1).toString()
                                                        recordWinningInput = record.winningAmount.toInt().toString()
                                                        showRecordMatchDialog = true
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(13.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Update Winning / Kills", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                                }

                                                if (onNavigateToTournament != null && tourney != null) {
                                                    TextButton(
                                                        onClick = {
                                                            onDismiss()
                                                            onNavigateToTournament(tourney.id)
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(Icons.Default.AccountTree, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(13.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Inspect Bracket", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VelorixAccent)
                                                    }
                                                }
                                            }
                                            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Admin Controls, 3-Bucket Wallet Addition & Individual Toggles
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // 1. 3-Section Wallet Transfer Control (1. Deposited Money, 2. Winning Money, 3. Referral Bonus)
                                DetailSectionCard(title = "SEND MONEY — 3 WALLET SECTIONS (DEPOSITED / WINNING / REFERRAL BONUS)", icon = Icons.Default.Payments) {
                                    ThreeSectionWalletControlPanel(
                                        user = user,
                                        onSendBucketFunds = { delta, bucket, reason ->
                                            triggerBucketAdjustment(
                                                delta = delta,
                                                bucket = bucket,
                                                reason = reason
                                            )
                                        }
                                    )
                                }

                                // Section: Granular Restriction Toggles
                                DetailSectionCard(title = "ACCESS & COMPLIANCE TOGGLES", icon = Icons.Default.Tune) {
                                    // 1. Account Ban Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Account Ban (Freeze & Lock)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Blocks login, match registrations, and freezes wallet balance", color = Color.Gray, fontSize = 11.sp)
                                        }
                                        Switch(
                                            checked = isBannedState,
                                            onCheckedChange = {
                                                if (it && user.isGuestSession) {
                                                    Toast.makeText(context, "Guest sessions cannot be banned globally.", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    isBannedState = it
                                                }
                                            },
                                            enabled = isBannedState || !user.isGuestSession,
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Red, checkedTrackColor = Color.Red.copy(alpha = 0.5f))
                                        )
                                    }

                                    if (isBannedState) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = banReasonInput,
                                            onValueChange = { banReasonInput = it },
                                            label = { Text("Ban Reason", color = Color.Gray, fontSize = 10.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.Red, unfocusedBorderColor = CardVerifyBorder)
                                        )
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                                    // 2. Temporary Suspension Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Match Suspension", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Temporarily suspends user from entering custom rooms", color = Color.Gray, fontSize = 11.sp)
                                        }
                                        Switch(
                                            checked = isSuspendedState,
                                            onCheckedChange = { isSuspendedState = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF9800), checkedTrackColor = Color(0xFFFF9800).copy(alpha = 0.5f))
                                        )
                                    }

                                    if (isSuspendedState) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = suspensionReasonInput,
                                            onValueChange = { suspensionReasonInput = it },
                                            label = { Text("Suspension Reason", color = Color.Gray, fontSize = 10.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFF9800), unfocusedBorderColor = CardVerifyBorder)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Duration:", color = Color.LightGray, fontSize = 11.sp)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(24 to "24h", 72 to "3d", 168 to "7d", 720 to "30d").forEach { (hours, label) ->
                                                val isSelected = selectedSuspensionHours == hours
                                                TextButton(
                                                    onClick = { selectedSuspensionHours = hours },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        label,
                                                        color = if (isSelected) Color(0xFFFF9800) else Color.Gray,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                                    // 3. VPN / Proxy Block Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("VPN & Proxy Shield", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Enforce direct ISP connection, disallowing VPNs/proxies", color = Color.Gray, fontSize = 11.sp)
                                        }
                                        Switch(
                                            checked = isVpnBlockedState,
                                            onCheckedChange = { isVpnBlockedState = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = VelorixAccent)
                                        )
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                                    // 4. Force Update App Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Mandatory Client App Update", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Forces player to update app before joining match lobbies", color = Color.Gray, fontSize = 11.sp)
                                        }
                                        Switch(
                                            checked = isForceUpdateState,
                                            onCheckedChange = { isForceUpdateState = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = VelorixAccent)
                                        )
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                                    // 5. Maintenance Bypass Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Maintenance Mode Bypass (VIP)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Allows this user into app during scheduled maintenance", color = Color.Gray, fontSize = 11.sp)
                                        }
                                        Switch(
                                            checked = isMaintenanceBypassState,
                                            onCheckedChange = { isMaintenanceBypassState = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = VelorixAccent)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = {
                                            val updated = buildUpdatedUser()
                                            onUpdateUser(updated)
                                            Toast.makeText(context, "Access toggles successfully applied to Firebase!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = V14AccentPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().height(42.dp)
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("APPLY ALL TOGGLES TO FIREBASE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                // 2. Edit Contact & Profile Info
                                DetailSectionCard(title = "CONTACT & PROFILE INFO", icon = Icons.Default.Edit) {
                                    OutlinedTextField(
                                        value = editUsername,
                                        onValueChange = { editUsername = it },
                                        label = { Text("Display Name / IGN", color = Color.Gray, fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = VelorixAccent,
                                            unfocusedBorderColor = CardVerifyBorder
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = editEmail,
                                        onValueChange = { editEmail = it },
                                        label = { Text("Email Address", color = Color.Gray, fontSize = 11.sp) },
                                        placeholder = { Text("e.g. player@gmail.com", color = Color.DarkGray, fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = VelorixAccent,
                                            unfocusedBorderColor = CardVerifyBorder
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = editGameId,
                                        onValueChange = { editGameId = it },
                                        label = { Text("Free Fire / Game UID", color = Color.Gray, fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = VelorixAccent,
                                            unfocusedBorderColor = CardVerifyBorder
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = editPhone,
                                        onValueChange = { editPhone = it },
                                        label = { Text("Mobile / Phone Number", color = Color.Gray, fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = VelorixAccent,
                                            unfocusedBorderColor = CardVerifyBorder
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            val updated = buildUpdatedUser()
                                            onUpdateUser(updated)
                                            Toast.makeText(context, "Profile info updated successfully", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = V14AccentPrimary),
                                        modifier = Modifier.fillMaxWidth().height(42.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save Contact & Profile Info", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                // 4. Role & Permissions (Flat Underline Mode Selector)
                                DetailSectionCard(title = "ASSIGN USER ROLE", icon = Icons.Default.Security) {
                                    Text("Current Role: ${user.role}", color = Color.LightGray, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        listOf("player", "tournament_admin", "support_admin", "super_admin").forEach { roleOption ->
                                            val isSelected = editRole.equals(roleOption, ignoreCase = true)
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        editRole = roleOption
                                                        val updated = buildUpdatedUser()
                                                        onUpdateUser(updated)
                                                        Toast.makeText(context, "Role updated to $roleOption", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .padding(vertical = 8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = roleOption.replace("_", " ").take(10).uppercase(),
                                                    color = if (isSelected) VelorixAccent else Color.LightGray,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.7f)
                                                        .height(1.5.dp)
                                                        .background(if (isSelected) VelorixAccent else Color.Transparent)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Rate Limiting & Cooldowns Control
                                DetailSectionCard(title = "RATE LIMITING & SECURITY LOCKOUTS", icon = Icons.Default.LockClock) {
                                    val isLockedOut = remember(user.id) {
                                        UserRateLimiter.isUserLockedOut(user.id) ||
                                        (user.phone.isNotBlank() && UserRateLimiter.isUserLockedOut(user.phone)) ||
                                        (user.email.isNotBlank() && UserRateLimiter.isUserLockedOut(user.email))
                                    }
                                    val lockoutSummary = remember(user.id) {
                                        UserRateLimiter.getActiveLockoutSummary(user.id)
                                            ?: (if (user.phone.isNotBlank()) UserRateLimiter.getActiveLockoutSummary(user.phone) else null)
                                            ?: (if (user.email.isNotBlank()) UserRateLimiter.getActiveLockoutSummary(user.email) else null)
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (isLockedOut) Icons.Default.Warning else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (isLockedOut) Color(0xFFF44336) else Color(0xFF4CAF50),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (isLockedOut) "SECURITY RATE LOCK ACTIVE" else "NO ACTIVE RATE LIMITS",
                                                color = if (isLockedOut) Color(0xFFF44336) else Color(0xFF4CAF50),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            if (lockoutSummary != null) {
                                                Text(text = lockoutSummary, color = Color.LightGray, fontSize = 10.sp)
                                            } else {
                                                Text(text = "User can perform auth, wallet, and tournament actions normally", color = Color.Gray, fontSize = 10.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            var totalReset = UserRateLimiter.resetUserLimits(user.id)
                                            if (user.phone.isNotBlank()) totalReset += UserRateLimiter.resetUserLimits(user.phone)
                                            if (user.email.isNotBlank()) totalReset += UserRateLimiter.resetUserLimits(user.email)
                                            Toast.makeText(context, "Rate limits & cooldowns reset for ${user.username} ($totalReset cleared)", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = V14CardElevated),
                                        border = BorderStroke(1.dp, V14Border),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().height(40.dp)
                                    ) {
                                        Icon(Icons.Default.RestartAlt, contentDescription = null, tint = V14AccentBright, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("RESET ALL RATE LIMIT COOLDOWNS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }

                                // 5. Modify Stats
                                DetailSectionCard(title = "EDIT COMPETITIVE STATS", icon = Icons.Default.EmojiEvents) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = editWins,
                                            onValueChange = { editWins = it },
                                            label = { Text("Wins", color = Color.Gray, fontSize = 10.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                                        )
                                        OutlinedTextField(
                                            value = editKills,
                                            onValueChange = { editKills = it },
                                            label = { Text("Kills", color = Color.Gray, fontSize = 10.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                                        )
                                        OutlinedTextField(
                                            value = editActivityPoints,
                                            onValueChange = { editActivityPoints = it },
                                            label = { Text("Points", color = Color.Gray, fontSize = 10.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = editEarnings,
                                        onValueChange = { editEarnings = it },
                                        label = { Text("Total Earnings Won (₹)", color = Color.Gray, fontSize = 10.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            val updated = buildUpdatedUser()
                                            onUpdateUser(updated)
                                            Toast.makeText(context, "Player stats updated", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = V14AccentPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().height(40.dp)
                                    ) {
                                        Text("Save Stats", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                // 6. Delete Account (Danger Zone — Flat Hairline Action, No OutlinedButton Box)
                                DetailSectionCard(title = "DANGER ZONE", icon = Icons.Default.Warning) {
                                    Text("Permanently erase this account and all associated profile nodes from Firebase database.", color = Color.LightGray, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(thickness = 0.5.dp, color = Color.Red.copy(alpha = 0.25f))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showDeleteConfirmDialog = true }
                                            .padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Permanently Delete User", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    HorizontalDivider(thickness = 0.5.dp, color = Color.Red.copy(alpha = 0.25f))
                                }
                            }
                        }
                        3 -> {
                            // Structured Database Tree & Raw Attributes
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "STRUCTURED DATABASE RECORD & RAW ATTRIBUTES",
                                    color = VelorixAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Live Firebase structure at /users/${user.id}, /player_database_records/${user.id} & /wallet_accounts/${user.id}:",
                                    color = Color.LightGray,
                                    fontSize = 11.5.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                                val combinedRaw = remember(user, unifiedMatchRecords) {
                                    val map = linkedMapOf<String, String>()
                                    map["accountLifecycle.firstAppJoinAt"] = "${user.effectiveFirstAppJoinAt} ($firstAppJoinStr)"
                                    map["accountLifecycle.firstMatchJoinedAt"] = "${user.firstMatchJoinedAt} ($firstMatchJoinStr)"
                                    map["accountLifecycle.lastMatchJoinedAt"] = "${user.lastMatchJoinedAt} ($lastMatchJoinStr)"
                                    map["accountLifecycle.hasJoinedAnyMatch"] = (unifiedMatchRecords.isNotEmpty() || user.totalMatchesJoined > 0).toString()
                                    map["combatAndMatchSummary.totalMatchesJoined"] = maxOf(user.totalMatchesJoined, unifiedMatchRecords.size).toString()
                                    map["combatAndMatchSummary.joinedMatchIds"] = unifiedMatchRecords.map { it.tournamentId }.ifEmpty { user.joinedMatchIds }.joinToString(", ").ifBlank { "[]" }
                                    map["combatAndMatchSummary.totalKills"] = user.kills.toString()
                                    map["combatAndMatchSummary.totalWins"] = user.wins.toString()
                                    map["combatAndMatchSummary.supportCasesCount"] = user.supportCasesCount.toString()
                                    map["walletAndFinanceSummary.winningMoney.balance"] = "₹${user.winningFunds} (withdrawable=true)"
                                    map["walletAndFinanceSummary.depositedMoney.balance"] = "₹${user.depositFunds} (withdrawable=false)"
                                    map["walletAndFinanceSummary.bonusMoney.balance"] = "₹${user.bonusFunds} (withdrawable=false)"
                                    map["walletAndFinanceSummary.withdrawableBalance"] = "₹${user.withdrawableBalance}"
                                    map["walletAndFinanceSummary.nonWithdrawableBalance"] = "₹${user.nonWithdrawableBalance}"
                                    map["walletAndFinanceSummary.canSendWithdrawalRequest"] = user.canRequestWithdrawal.toString()
                                    map["walletAndFinanceSummary.totalLifetimeDeposited"] = "₹${user.totalDeposited}"
                                    map["walletAndFinanceSummary.totalEntryFeesPaid"] = "₹${user.totalEntryFeesPaid}"
                                    map["walletAndFinanceSummary.totalLifetimeWinningEarned"] = "₹${user.totalEarnings}"
                                    map["walletAndFinanceSummary.totalLifetimeWithdrawn"] = "₹${user.totalWithdrawn}"
                                    unifiedMatchRecords.forEach { m ->
                                        map["joinedMatches.${m.tournamentId}.title"] = m.tournamentTitle
                                        map["joinedMatches.${m.tournamentId}.joinedAt"] = "${m.joinedAt}"
                                        map["joinedMatches.${m.tournamentId}.entryFeePaid"] = "₹${m.entryFeePaid} (Dep: ₹${m.paidFromDeposit}, Win: ₹${m.paidFromWinning})"
                                        map["joinedMatches.${m.tournamentId}.killsAndWinning"] = "${m.kills} Kills • Rank #${m.rank} • Won ₹${m.winningAmount}"
                                    }
                                    user.rawAttributes.forEach { (k, v) ->
                                        map[k] = v
                                    }
                                    map
                                }

                                combinedRaw.forEach { (k, v) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(V14Bg)
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = k,
                                            color = VelorixAccent,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(0.46f)
                                        )
                                        Text(
                                            text = v,
                                            color = Color.White,
                                            fontSize = 10.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(0.54f)
                                        )
                                    }
                                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.06f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun DetailSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    val sectionAccentColor = when {
        title.contains("ESPORTS", ignoreCase = true) || title.contains("COMBAT", ignoreCase = true) -> V14AccentBright
        title.contains("WALLET", ignoreCase = true) || title.contains("FINANCIAL", ignoreCase = true) -> VelorixEmerald
        title.contains("STREAK", ignoreCase = true) || title.contains("FIRE", ignoreCase = true) -> VelorixAmber
        title.contains("KYC", ignoreCase = true) || title.contains("ELIGIBILITY", ignoreCase = true) -> V14TextPrimary
        title.contains("SECURITY", ignoreCase = true) || title.contains("RESTRICTION", ignoreCase = true) -> V14AccentBright
        title.contains("LIFECYCLE", ignoreCase = true) || title.contains("TIMESTAMPS", ignoreCase = true) -> VelorixAmber
        else -> V14AccentBright
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(V14Bg)
            .padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = sectionAccentColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.12f))
        Spacer(modifier = Modifier.height(6.dp))
        content()
    }
}

@Composable
fun DetailItemRow(label: String, value: String, isHighlight: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = Color(0xFFA1A1AA), fontSize = 12.sp, modifier = Modifier.weight(0.45f))
            Text(
                text = value,
                color = if (isHighlight) VelorixAccent else Color(0xFFEEEEEE),
                fontSize = 12.sp,
                fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier.weight(0.55f)
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.05f))
    }
}

@Composable
fun PayoutsManagementScreenContent(
    uiState: DashboardState,
    onApprovePayout: (com.example.domain.model.PayoutRequest) -> Unit = {},
    onRejectPayout: (com.example.domain.model.PayoutRequest, String) -> Unit = { _, _ -> }
) {
    val successState = uiState as? DashboardState.Success
    val payouts = successState?.payoutRequests ?: emptyList()
    val usersList = successState?.users ?: emptyList()
    var selectedFilter by remember { mutableStateOf("pending") }
    var showRejectDialog by remember { mutableStateOf<com.example.domain.model.PayoutRequest?>(null) }
    var rejectionReasonInput by remember { mutableStateOf("Invalid UPI Details / Account Holder Name Mismatch") }

    val filteredPayouts = remember(payouts, selectedFilter) {
        when (selectedFilter) {
            "all" -> payouts
            else -> payouts.filter { it.status.equals(selectedFilter, ignoreCase = true) }
        }
    }

    val currentRejectReq = showRejectDialog
    if (currentRejectReq != null) {
        val req = currentRejectReq
        AlertDialog(
            onDismissRequest = { showRejectDialog = null },
            title = { Text("Reject Cashout Request", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Reject ₹${req.vtAmount} cashout for ${req.username} (${req.email})?", color = Color.LightGray, fontSize = 12.sp)
                    Text("The ₹${req.vtAmount} amount will be refunded directly back to their Withdrawable Winning Balance.", color = VelorixAccent, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rejectionReasonInput,
                        onValueChange = { rejectionReasonInput = it },
                        label = { Text("Rejection Reason", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Red,
                            unfocusedBorderColor = CardVerifyBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRejectPayout(req, rejectionReasonInput.trim())
                        showRejectDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Confirm Reject & Refund Winning", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color.Black
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(V14Bg).padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Winning Payout Approvals",
                    color = VelorixTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Only Winning Money is withdrawable • Deposited Money is locked",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            val pendingSum = payouts.filter { it.status.equals("pending", ignoreCase = true) }.sumOf { it.vtAmount }
            Text(
                text = "₹${pendingSum.toInt()} Pending",
                color = VelorixAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            listOf("pending", "approved", "rejected", "all").forEach { f ->
                val isSel = selectedFilter == f
                Column(
                    modifier = Modifier
                        .clickable { selectedFilter = f }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        f.uppercase(),
                        color = if (isSel) VelorixAccent else Color(0xFF71717A),
                        fontSize = 11.5.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(1.5.dp)
                            .background(if (isSel) VelorixAccent else Color.Transparent)
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 90.dp)) {
            if (filteredPayouts.isEmpty()) {
                item {
                    Text("No cashout requests found.", color = VelorixTextSecondary, modifier = Modifier.padding(16.dp))
                }
            }
            items(filteredPayouts.size) { i ->
                val req = filteredPayouts[i]
                val playerProfile = usersList.find {
                    (req.userId.isNotBlank() && it.id == req.userId) ||
                    (req.email.isNotBlank() && it.email.equals(req.email, ignoreCase = true))
                }
                val winningBal = playerProfile?.winningFunds ?: req.vtAmount
                val depositBal = playerProfile?.depositFunds ?: 0.0
                val isWinningEligible = winningBal >= req.vtAmount || !req.status.equals("pending", ignoreCase = true)
                val statusColor = when (req.status.lowercase()) {
                    "approved" -> Color(0xFF34D399)
                    "rejected" -> Color(0xFFEF5350)
                    else -> VelorixAccentLight
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(req.username, fontWeight = FontWeight.Bold, color = VelorixTextPrimary, fontSize = 16.sp)
                            Text(req.email, color = VelorixTextSecondary, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(req.status.uppercase(), color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Payment Method", color = Color.Gray, fontSize = 10.sp)
                            Text("${req.paymentMethod}: ${req.paymentId}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Cashout Request", color = Color.Gray, fontSize = 10.sp)
                            Text("₹${req.vtAmount}", color = VelorixAccentLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Winning Money (Withdrawable)", color = Color(0xFF34D399), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                            Text("₹${"%.2f".format(winningBal)}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Deposited Money (Non-Withdrawable)", color = V14AccentBright, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                            Text("₹${"%.2f".format(depositBal)} (Locked)", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isWinningEligible) {
                            "ELIGIBLE: Covered by player's Winning Balance (₹${"%.0f".format(winningBal)})"
                        } else {
                            "BLOCKED: Exceeds Winning Balance (₹${"%.0f".format(winningBal)}). Deposited Money (₹${"%.0f".format(depositBal)}) cannot be withdrawn."
                        },
                        color = if (isWinningEligible) Color(0xFF34D399) else Color(0xFFEF5350),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (req.status.equals("pending", ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF34D399).copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                                    .border(0.75.dp, Color(0xFF34D399).copy(alpha = 0.48f), RoundedCornerShape(10.dp))
                                    .clickable { onApprovePayout(req) }
                                    .padding(vertical = 8.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Approve Payout", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399), maxLines = 1, softWrap = false)
                            }
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFFF87171).copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                                    .border(0.75.dp, Color(0xFFF87171).copy(alpha = 0.48f), RoundedCornerShape(10.dp))
                                    .clickable { showRejectDialog = req }
                                    .padding(vertical = 8.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reject & Refund", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF87171), maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

// StaffManagementScreenContent is implemented with full editing & inspection dialog parity in StaffManagementScreen.kt

@Composable
fun ThreeSectionWalletControlPanel(
    user: UserProfile,
    onSendBucketFunds: (Double, com.example.domain.model.WalletBucket, String) -> Unit
) {
    val context = LocalContext.current
    var depositAmountInput by remember { mutableStateOf("") }
    var winningAmountInput by remember { mutableStateOf("") }
    var referralBonusAmountInput by remember { mutableStateOf("") }
    var auditReasonInput by remember { mutableStateOf("Admin wallet credit") }

    val effectiveReferralBonus = maxOf(user.bonusFunds, user.referralBonusEarned)
    val totalWallet = user.depositFunds + user.winningFunds + user.bonusFunds

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Live Summary Bar of the 3 Sections
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("1. Deposited", color = Color(0xFFFF2E5B), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                Text(
                    "₹${"%.0f".format(user.depositFunds)}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )
                Text("Entry Only", color = Color(0xFF71717A), fontSize = 9.sp, maxLines = 1, softWrap = false)
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("2. Winning", color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                Text(
                    "₹${"%.0f".format(user.winningFunds)}",
                    color = Color(0xFF34D399),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )
                Text("Withdrawable", color = Color(0xFF34D399).copy(alpha = 0.8f), fontSize = 9.sp, maxLines = 1, softWrap = false)
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("3. Bonus", color = Color(0xFFFBBF24), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                Text(
                    "₹${"%.0f".format(effectiveReferralBonus)}",
                    color = Color(0xFFFBBF24),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )
                Text("Referral", color = Color(0xFF71717A), fontSize = 9.sp, maxLines = 1, softWrap = false)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total Wallet Balance", color = Color(0xFFA1A1AA), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                "₹${"%.0f".format(totalWallet)}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                softWrap = false
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.12f))

        // ==================== SECTION 1: DEPOSITED MONEY ====================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "1. Deposited Money",
                    color = Color(0xFFFF2E5B),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Entry Only",
                    color = Color(0xFF71717A),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(50.0, 100.0, 500.0, -50.0, -100.0).forEach { delta ->
                    val chipColor = if (delta > 0) Color(0xFFFF2E5B) else Color(0xFFFF4D6D)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(chipColor.copy(alpha = 0.12f))
                            .border(0.5.dp, chipColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .clickable {
                                onSendBucketFunds(
                                    delta,
                                    com.example.domain.model.WalletBucket.DEPOSIT,
                                    auditReasonInput.ifBlank { "Deposited Money quick adjustment" }
                                )
                                Toast.makeText(context, "${if (delta > 0) "+" else ""}₹${delta.toInt()} sent to Deposited Money", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (delta > 0) "+₹${delta.toInt()}" else "-₹${(-delta).toInt()}",
                            color = chipColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            softWrap = false
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
                    value = depositAmountInput,
                    onValueChange = { depositAmountInput = it },
                    label = { Text("Deposited ₹ (e.g. 100)", color = Color.Gray, fontSize = 10.5.sp, maxLines = 1) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF2E5B),
                        unfocusedBorderColor = CardVerifyBorder
                    )
                )
                Button(
                    onClick = {
                        val delta = depositAmountInput.toDoubleOrNull()
                        if (delta != null && delta != 0.0) {
                            onSendBucketFunds(
                                delta,
                                com.example.domain.model.WalletBucket.DEPOSIT,
                                auditReasonInput.ifBlank { "Admin Deposited Money transfer" }
                            )
                            Toast.makeText(context, "₹${delta.toInt()} sent to ${user.bestDisplayName}'s Deposited Money!", Toast.LENGTH_SHORT).show()
                            depositAmountInput = ""
                        } else {
                            Toast.makeText(context, "Enter an amount for Deposited Money", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2E5B)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Send", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.12f))

        // ==================== SECTION 2: WINNING MONEY ====================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "2. Winning Money",
                    color = Color(0xFF34D399),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Withdrawable",
                    color = Color(0xFF34D399),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(50.0, 100.0, 500.0, -50.0, -100.0).forEach { delta ->
                    val chipColor = if (delta > 0) Color(0xFF34D399) else Color(0xFFEF5350)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(chipColor.copy(alpha = 0.12f))
                            .border(0.5.dp, chipColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .clickable {
                                onSendBucketFunds(
                                    delta,
                                    com.example.domain.model.WalletBucket.WINNING,
                                    auditReasonInput.ifBlank { "Winning Money quick adjustment" }
                                )
                                Toast.makeText(context, "${if (delta > 0) "+" else ""}₹${delta.toInt()} sent to Winning Money", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (delta > 0) "+₹${delta.toInt()}" else "-₹${(-delta).toInt()}",
                            color = chipColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            softWrap = false
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
                    value = winningAmountInput,
                    onValueChange = { winningAmountInput = it },
                    label = { Text("Winning ₹ (e.g. 200)", color = Color.Gray, fontSize = 10.5.sp, maxLines = 1) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF34D399),
                        unfocusedBorderColor = CardVerifyBorder
                    )
                )
                Button(
                    onClick = {
                        val delta = winningAmountInput.toDoubleOrNull()
                        if (delta != null && delta != 0.0) {
                            onSendBucketFunds(
                                delta,
                                com.example.domain.model.WalletBucket.WINNING,
                                auditReasonInput.ifBlank { "Admin Winning Money transfer" }
                            )
                            Toast.makeText(context, "₹${delta.toInt()} sent to ${user.bestDisplayName}'s Winning Money!", Toast.LENGTH_SHORT).show()
                            winningAmountInput = ""
                        } else {
                            Toast.makeText(context, "Enter an amount for Winning Money", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Send", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.12f))

        // ==================== SECTION 3: REFERRAL BONUS MONEY ====================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "3. Referral Bonus",
                    color = Color(0xFFFBBF24),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bonus Only",
                    color = Color(0xFFFBBF24).copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(25.0, 50.0, 100.0, 250.0, -25.0).forEach { delta ->
                    val chipColor = if (delta > 0) Color(0xFFFBBF24) else Color(0xFFEF5350)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(chipColor.copy(alpha = 0.12f))
                            .border(0.5.dp, chipColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .clickable {
                                onSendBucketFunds(
                                    delta,
                                    com.example.domain.model.WalletBucket.BONUS,
                                    auditReasonInput.ifBlank { "Referral Bonus quick adjustment" }
                                )
                                Toast.makeText(context, "${if (delta > 0) "+" else ""}₹${delta.toInt()} sent to Referral Bonus", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (delta > 0) "+₹${delta.toInt()}" else "-₹${(-delta).toInt()}",
                            color = chipColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            softWrap = false
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
                    value = referralBonusAmountInput,
                    onValueChange = { referralBonusAmountInput = it },
                    label = { Text("Bonus ₹ (e.g. 50)", color = Color.Gray, fontSize = 10.5.sp, maxLines = 1) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFBBF24),
                        unfocusedBorderColor = CardVerifyBorder
                    )
                )
                Button(
                    onClick = {
                        val delta = referralBonusAmountInput.toDoubleOrNull()
                        if (delta != null && delta != 0.0) {
                            onSendBucketFunds(
                                delta,
                                com.example.domain.model.WalletBucket.BONUS,
                                auditReasonInput.ifBlank { "Admin Referral Bonus transfer" }
                            )
                            Toast.makeText(context, "₹${delta.toInt()} sent to ${user.bestDisplayName}'s Referral Bonus!", Toast.LENGTH_SHORT).show()
                            referralBonusAmountInput = ""
                        } else {
                            Toast.makeText(context, "Enter an amount for Referral Bonus", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Send", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.12f))

        // Audit Note & Send All 3 Sections Button
        OutlinedTextField(
            value = auditReasonInput,
            onValueChange = { auditReasonInput = it },
            label = { Text("Note / Reason", color = Color.Gray, fontSize = 10.5.sp, maxLines = 1) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = CardVerifyBorder
            )
        )

        val depVal = depositAmountInput.toDoubleOrNull() ?: 0.0
        val winVal = winningAmountInput.toDoubleOrNull() ?: 0.0
        val bonVal = referralBonusAmountInput.toDoubleOrNull() ?: 0.0
        val hasAnyEntered = depVal != 0.0 || winVal != 0.0 || bonVal != 0.0

        if (hasAnyEntered) {
            Button(
                onClick = {
                    val note = auditReasonInput.ifBlank { "Admin 3-section wallet transfer" }
                    if (depVal != 0.0) {
                        onSendBucketFunds(depVal, com.example.domain.model.WalletBucket.DEPOSIT, note)
                    }
                    if (winVal != 0.0) {
                        onSendBucketFunds(winVal, com.example.domain.model.WalletBucket.WINNING, note)
                    }
                    if (bonVal != 0.0) {
                        onSendBucketFunds(bonVal, com.example.domain.model.WalletBucket.BONUS, note)
                    }
                    Toast.makeText(context, "Sent all entered sections to ${user.bestDisplayName}!", Toast.LENGTH_SHORT).show()
                    depositAmountInput = ""
                    winningAmountInput = ""
                    referralBonusAmountInput = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Text(
                    "SEND ALL ENTERED AMOUNTS",
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun ThreeSectionPlayerFundsDialog(
    user: UserProfile,
    onDismiss: () -> Unit,
    onSendBucketFunds: (UserProfile, Double, com.example.domain.model.WalletBucket, String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(16.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp)),
            color = Color.Black,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                            "Send Player Funds",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${user.bestDisplayName} • ${user.bestEmailOrPhone}",
                            color = Color(0xFFA1A1AA),
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Done", color = VelorixAccent, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.12f))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThreeSectionWalletControlPanel(
                        user = user,
                        onSendBucketFunds = { delta, bucket, reason ->
                            onSendBucketFunds(user, delta, bucket, reason)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerFundsTransferHubContent(
    users: List<UserProfile>,
    onAddFundsBucket: (UserProfile, Double, com.example.domain.model.WalletBucket, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPlayerId by remember { mutableStateOf<String?>(null) }
    var dialogPlayerId by remember { mutableStateOf<String?>(null) }

    val realPlayers = remember(users, searchQuery) {
        users.filter { u ->
            val emailLower = u.email.trim().lowercase()
            val isMock = emailLower.contains("@example.com") || emailLower.contains("mock.dummy")
            if (isMock || u.isGuestSession) return@filter false
            searchQuery.isBlank() ||
                u.username.contains(searchQuery, ignoreCase = true) ||
                u.ign.contains(searchQuery, ignoreCase = true) ||
                u.email.contains(searchQuery, ignoreCase = true) ||
                u.gameId.contains(searchQuery, ignoreCase = true) ||
                u.id.contains(searchQuery, ignoreCase = true)
        }
    }

    val activeSelectedPlayer = remember(realPlayers, selectedPlayerId) {
        realPlayers.find { it.id == selectedPlayerId } ?: realPlayers.firstOrNull()
    }

    val activeDialogPlayer = remember(users, dialogPlayerId) {
        if (dialogPlayerId == null) null else users.find { it.id == dialogPlayerId }
    }

    activeDialogPlayer?.let { target ->
        ThreeSectionPlayerFundsDialog(
            user = target,
            onDismiss = { dialogPlayerId = null },
            onSendBucketFunds = onAddFundsBucket
        )
    }

    val totalDepositedPool = remember(realPlayers) { realPlayers.sumOf { it.depositFunds } }
    val totalWinningPool = remember(realPlayers) { realPlayers.sumOf { it.winningFunds } }
    val totalReferralBonusPool = remember(realPlayers) { realPlayers.sumOf { maxOf(it.bonusFunds, it.referralBonusEarned) } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 148.dp)
    ) {
        item {
            Text(
                text = "3-SECTION PLAYER FUNDS DISPATCHER",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Send money directly to any player's 1. Deposited Money, 2. Winning Money, or 3. Referral Bonus",
                color = Color(0xFFA1A1AA),
                fontSize = 11.sp
            )
        }

        // Global 3-Section Pool Strip
        item {
            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("TOTAL DEPOSITED", color = Color(0xFFFF2E5B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("₹${totalDepositedPool.toInt()}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("Locked / Entry Only", color = Color(0xFF71717A), fontSize = 8.5.sp)
                }
                Box(modifier = Modifier.width(0.5.dp).height(34.dp).background(Color.White.copy(alpha = 0.08f)))
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("TOTAL WINNING", color = Color(0xFF34D399), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("₹${totalWinningPool.toInt()}", color = Color(0xFF34D399), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("Withdrawable", color = Color(0xFF34D399).copy(alpha = 0.8f), fontSize = 8.5.sp)
                }
                Box(modifier = Modifier.width(0.5.dp).height(34.dp).background(Color.White.copy(alpha = 0.08f)))
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("REFERRAL BONUS", color = Color(0xFFFBBF24), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("₹${totalReferralBonusPool.toInt()}", color = Color(0xFFFBBF24), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("Referral Locked", color = Color(0xFF71717A), fontSize = 8.5.sp)
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
        }

        // Search Player Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search player by IGN, username, email, or Free Fire UID...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White.copy(alpha = 0.3f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        // Active Player 3-Section Panel
        if (activeSelectedPlayer != null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SELECTED PLAYER: ${activeSelectedPlayer.bestDisplayName.uppercase()}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "FF UID: ${activeSelectedPlayer.gameId.ifBlank { "Unlinked" }} • ${activeSelectedPlayer.bestEmailOrPhone}",
                                color = Color(0xFFA1A1AA),
                                fontSize = 10.5.sp
                            )
                        }
                        Text(
                            text = "ACTIVE TARGET",
                            color = Color(0xFF34D399),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.12f))
                    ThreeSectionWalletControlPanel(
                        user = activeSelectedPlayer,
                        onSendBucketFunds = { delta, bucket, reason ->
                            onAddFundsBucket(activeSelectedPlayer, delta, bucket, reason)
                        }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.15f))
                }
            }
        }

        item {
            Text(
                text = "ALL PLAYERS — TAP TO SELECT OR OPEN 3-SECTION TRANSFER (${realPlayers.size})",
                color = Color(0xFFA1A1AA),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(realPlayers.size) { idx ->
            val p = realPlayers[idx]
            val isSelected = activeSelectedPlayer?.id == p.id
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedPlayerId = p.id }
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = p.bestDisplayName,
                            color = if (isSelected) Color(0xFF34D399) else Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Dep: ₹${p.depositFunds.toInt()} • Win: ₹${p.winningFunds.toInt()} • RefBonus: ₹${maxOf(p.bonusFunds, p.referralBonusEarned).toInt()}",
                            color = Color(0xFFA1A1AA),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(VelorixAccent.copy(alpha = 0.14f))
                            .border(0.75.dp, VelorixAccent.copy(alpha = 0.35f), CircleShape)
                            .clickable {
                                selectedPlayerId = p.id
                                dialogPlayerId = p.id
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = "Send Funds",
                            tint = VelorixAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
            }
        }
    }
}
