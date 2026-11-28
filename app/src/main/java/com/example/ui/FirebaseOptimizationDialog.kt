package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.analytics.VelorixAnalytics
import com.example.config.VelorixRemoteConfigManager
import com.example.domain.model.Tournament
import com.example.firebase.VelorixFirebaseSuite
import com.example.notification.VelorixFcmManager
import com.example.ui.theme.VelorixTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FirebaseOptimizationDialog(
    onDismiss: () -> Unit,
    tournaments: List<Tournament> = emptyList()
) {
    val context = LocalContext.current
    val suiteState by VelorixFirebaseSuite.suiteState.collectAsState()
    val remoteConfigState by VelorixRemoteConfigManager.configState.collectAsState()
    val fcmToken by VelorixFcmManager.fcmToken.collectAsState()

    LaunchedEffect(Unit) {
        VelorixFirebaseSuite.refreshDiagnosticsState()
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Crashlytics", "FCM Push", "Remote Config", "Analytics")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp, bottom = 16.dp, start = 12.dp, end = 12.dp),
            color = Color.Black,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.14f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Firebase & Crashlytics",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Project: ${suiteState.projectId} • 9 Services Active",
                            color = Color(0xFF71717A),
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFA1A1AA),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                // Flat Underline Mode Tab Row (No Box-in-Box)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = index }
                                .padding(top = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.White else Color(0xFF71717A),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isSelected) 1.5.dp else 0.5.dp)
                                    .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.08f))
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // ==========================================
                            // TAB 0: CRASHLYTICS, PERFORMANCE & SUITE
                            // ==========================================
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "TELEMETRY & COLLECTION CONTROLS",
                                        color = Color(0xFF71717A),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )

                                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                                    FirebaseToggleRow(
                                        title = "Firebase Crashlytics",
                                        subtitle = "Capture fatal crashes, non-fatal exceptions, and custom keys",
                                        checked = suiteState.crashlyticsEnabled,
                                        onCheckedChange = { VelorixFirebaseSuite.setCrashlyticsEnabled(it) }
                                    )

                                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                                    FirebaseToggleRow(
                                        title = "Firebase Performance Monitoring",
                                        subtitle = "Measure cold start, screen rendering, and database latency",
                                        checked = suiteState.performanceEnabled,
                                        onCheckedChange = { VelorixFirebaseSuite.setPerformanceEnabled(it) }
                                    )

                                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                                    FirebaseToggleRow(
                                        title = "Firebase Google Analytics",
                                        subtitle = "Stream tournament conversion and user property events",
                                        checked = suiteState.analyticsEnabled,
                                        onCheckedChange = { VelorixFirebaseSuite.setAnalyticsEnabled(context, it) }
                                    )
                                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                                }
                            }

                            item {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                                    Text(
                                        text = "CRASHLYTICS DIAGNOSTIC DISPATCH",
                                        color = Color(0xFF71717A),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Admin UID: ${suiteState.currentAdminUid} • App Check: ${suiteState.appCheckProvider}",
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                VelorixFirebaseSuite.triggerTestNonFatalReport("Manual diagnostic check from Admin Console")
                                                Toast.makeText(
                                                    context,
                                                    "Non-fatal diagnostic report recorded & queued for Crashlytics",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.16f)),
                                            contentPadding = PaddingValues(vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = "Send Test Non-Fatal",
                                                color = Color.White,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                VelorixFirebaseSuite.flushUnsentCrashReports()
                                                Toast.makeText(
                                                    context,
                                                    "Flushed unsent Crashlytics reports to Firebase",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.16f)),
                                            contentPadding = PaddingValues(vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = "Flush Unsent Reports",
                                                color = Color.White,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                            }

                            // Provisioned Firebase Services List
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "CONFIGURED FIREBASE SERVICES (${suiteState.services.size})",
                                        color = Color(0xFF71717A),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )

                                    suiteState.services.forEach { service ->
                                        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = service.name,
                                                    color = Color.White,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = service.statusText,
                                                    color = Color(0xFF71717A),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Text(
                                                text = if (service.isEnabled) "Active" else "Paused",
                                                color = if (service.isEnabled) Color(0xFF10B981) else Color(0xFFF59E0B),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                                }
                            }

                            // Recorded Non-Fatal Reports & Breadcrumbs
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "RECENT CRASHLYTICS EVENTS & BREADCRUMBS",
                                        color = Color(0xFF71717A),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )

                                    if (suiteState.recordedNonFatals.isEmpty() && suiteState.recentBreadcrumbs.isEmpty()) {
                                        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                                        Text(
                                            text = "No exceptions recorded in this session.",
                                            color = Color(0xFF71717A),
                                            fontSize = 11.5.sp,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    } else {
                                        suiteState.recordedNonFatals.take(5).forEach { err ->
                                            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                                            val timeStr = remember(err.timestamp) {
                                                SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(err.timestamp))
                                            }
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "[${err.tag}] Non-Fatal Exception",
                                                        color = Color(0xFFF59E0B),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Text(
                                                        text = timeStr,
                                                        color = Color(0xFF71717A),
                                                        fontSize = 10.sp
                                                    )
                                                }
                                                Text(
                                                    text = err.message,
                                                    color = Color(0xFFD4D4D8),
                                                    fontSize = 11.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        suiteState.recentBreadcrumbs.take(8).forEach { crumb ->
                                            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                                            Text(
                                                text = crumb,
                                                color = Color(0xFFA1A1AA),
                                                fontSize = 10.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                                            )
                                        }
                                    }
                                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                                }
                            }
                        }

                        1 -> {
                            // ==========================================
                            // TAB 1: FIREBASE CLOUD MESSAGING (FCM)
                            // ==========================================
                            item {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "FCM DEVICE REGISTRATION",
                                            color = Color(0xFF71717A),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp
                                        )
                                        Text(
                                            "Connected",
                                            color = Color(0xFF10B981),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    val tokenStr = fcmToken ?: "Retrieving device token from Firebase..."
                                    Text(
                                        text = tokenStr.take(52) + if (tokenStr.length > 52) "..." else "",
                                        color = VelorixTextSecondary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedButton(
                                        onClick = {
                                            fcmToken?.let { token ->
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("FCM Token", token))
                                                Toast.makeText(context, "FCM Token copied to clipboard", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.16f)),
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Text("Copy Registration Token", color = Color.White, fontSize = 11.5.sp)
                                    }
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                            }

                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "USER PANEL PUSH DISPATCH",
                                            color = Color(0xFF71717A),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp
                                        )
                                        Text(
                                            text = "TARGET: com.tournaments.velorix",
                                            color = Color(0xFF0A84FF),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    val activeTourney = tournaments.firstOrNull()

                                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                                    PushActionRow(
                                        title = "Test Push to User Panel",
                                        subtitle = "Send live test notification to com.tournaments.velorix players",
                                        actionLabel = "Send Test Push",
                                        onAction = {
                                            VelorixFcmManager.dispatchTestPushToUserPanel(activeTourney)
                                            Toast.makeText(
                                                context,
                                                "Test Push sent to User Panel (com.tournaments.velorix)",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )

                                    if (activeTourney != null) {
                                        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                                        PushActionRow(
                                            title = "Room Credentials Push",
                                            subtitle = "Send Room ID and Password to User Panel for ${activeTourney.title}",
                                            actionLabel = "Send Room Push",
                                            onAction = {
                                                val roomId = activeTourney.roomDetails?.roomId?.ifBlank { "882104" } ?: "882104"
                                                val roomPass = activeTourney.roomDetails?.roomPassword?.ifBlank { "vx77" } ?: "vx77"
                                                VelorixFcmManager.dispatchRoomDetailsPush(
                                                    context = context,
                                                    tournament = activeTourney,
                                                    roomId = roomId,
                                                    roomPassword = roomPass
                                                )
                                                Toast.makeText(
                                                    context,
                                                    "Room Credentials sent to User Panel (com.tournaments.velorix)",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )

                                        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                                        PushActionRow(
                                            title = "15-Minute Match Reminder",
                                            subtitle = "Broadcast 15-minute lobby alert to User Panel for ${activeTourney.title}",
                                            actionLabel = "Send Reminder",
                                            onAction = {
                                                VelorixFcmManager.dispatchMatchReminderPush(
                                                    context = context,
                                                    tournament = activeTourney,
                                                    minutesLeft = 15
                                                )
                                                Toast.makeText(
                                                    context,
                                                    "15-Min Reminder sent to User Panel (com.tournaments.velorix)",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }

                                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                                    PushActionRow(
                                        title = "Payout Settlement Notification",
                                        subtitle = "Trigger instant withdrawal confirmation on User Panel",
                                        actionLabel = "Send Alert",
                                        onAction = {
                                            VelorixFcmManager.dispatchPayoutSettlementPush(
                                                context = context,
                                                username = "PlayerOne",
                                                amount = 1250.0,
                                                approved = true,
                                                details = "UPI ID (player@okhdfcbank)"
                                            )
                                            Toast.makeText(
                                                context,
                                                "Payout Alert sent to User Panel (com.tournaments.velorix)",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                                }
                            }
                        }

                        2 -> {
                            // ==========================================
                            // TAB 2: FIREBASE REMOTE CONFIG
                            // ==========================================
                            item {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "REMOTE CONFIG ENGINE",
                                            color = Color(0xFF71717A),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp
                                        )
                                        Text(
                                            remoteConfigState.lastFetchStatus,
                                            color = V14AccentBright,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Synchronize live parameters from Firebase Console without republishing the APK.",
                                        color = VelorixTextSecondary,
                                        fontSize = 11.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedButton(
                                        onClick = {
                                            VelorixRemoteConfigManager.fetchAndActivate()
                                            Toast.makeText(context, "Fetching latest Remote Config...", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.16f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Fetch & Activate Config", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp)
                                    }
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                            }

                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        "LIVE PARAMETERS",
                                        color = Color(0xFF71717A),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )

                                    val params = listOf(
                                        Triple("is_maintenance_mode", remoteConfigState.isMaintenanceMode.toString().uppercase(), "Global maintenance gate"),
                                        Triple("maintenance_message", remoteConfigState.maintenanceMessage, "Notice shown during maintenance"),
                                        Triple("cta_button_text", remoteConfigState.ctaButtonText, "Registration CTA variant"),
                                        Triple("match_reminder_lead_minutes", "${remoteConfigState.matchReminderLeadMinutes}m", "Lead time before match start"),
                                        Triple("max_slots_per_tournament", "${remoteConfigState.maxSlotsPerTournament}", "Default custom room capacity"),
                                        Triple("vip_experience_enabled", remoteConfigState.isVipExperienceEnabled.toString().uppercase(), "VIP priority slot access")
                                    )

                                    params.forEach { (key, value, desc) ->
                                        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                                Text(
                                                    text = key,
                                                    color = Color.White,
                                                    fontSize = 11.5.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = desc,
                                                    color = Color(0xFF71717A),
                                                    fontSize = 10.5.sp
                                                )
                                            }
                                            Text(
                                                text = value,
                                                color = Color(0xFFD4D4D8),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                                }
                            }
                        }

                        3 -> {
                            // ==========================================
                            // TAB 3: FIREBASE A/B TESTING & ANALYTICS
                            // ==========================================
                            item {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                                    Text(
                                        "CTA CONVERSION EXPERIMENT",
                                        color = Color(0xFF71717A),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Active Variant: ${remoteConfigState.ctaButtonText}",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Tracks conversion rate via Firebase Analytics event 'cta_button_click'.",
                                        color = VelorixTextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = {
                                            VelorixAnalytics.logCtaClick(
                                                ctaText = remoteConfigState.ctaButtonText,
                                                tournamentId = "vx_exp_test",
                                                tournamentTitle = "A/B Experiment Test"
                                            )
                                            Toast.makeText(
                                                context,
                                                "Logged Analytics Event: cta_button_click",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.16f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Log Test Conversion Event", color = Color.White, fontSize = 11.5.sp)
                                    }
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                            }

                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        "REGISTERED ANALYTICS EVENTS",
                                        color = Color(0xFF71717A),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )

                                    listOf(
                                        "cta_button_click" to "A/B Testing conversion tracker",
                                        "tournament_joined" to "Registration checkout event",
                                        "room_credentials_viewed" to "Room ID/Password unlock tracker",
                                        "notification_interaction" to "Push opened and received metric",
                                        "payout_requested" to "Wallet withdrawal request telemetry"
                                    ).forEach { (event, desc) ->
                                        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(event, color = Color.White, fontSize = 11.5.sp, fontFamily = FontFamily.Monospace)
                                                Text(desc, color = VelorixTextSecondary, fontSize = 10.5.sp)
                                            }
                                            Text("Active", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
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
private fun FirebaseToggleRow(
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
                color = Color.White,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = Color(0xFF71717A),
                fontSize = 11.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color.White,
                uncheckedThumbColor = Color(0xFFA1A1AA),
                uncheckedTrackColor = Color(0xFF18181B)
            )
        )
    }
}

@Composable
private fun PushActionRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color(0xFF71717A), fontSize = 11.sp)
        }
        OutlinedButton(
            onClick = onAction,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.16f)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(actionLabel, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
