package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.analytics.VelorixAnalytics
import com.example.config.VelorixRemoteConfigManager
import com.example.domain.model.Tournament
import com.example.notification.VelorixFcmManager
import com.example.ui.theme.VelorixAccent
import com.example.ui.theme.VelorixTextPrimary
import com.example.ui.theme.VelorixTextSecondary
import kotlinx.coroutines.launch

@Composable
fun FirebaseOptimizationDialog(
    onDismiss: () -> Unit,
    tournaments: List<Tournament> = emptyList()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val remoteConfigState by VelorixRemoteConfigManager.configState.collectAsState()
    val fcmToken by VelorixFcmManager.fcmToken.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: FCM, 1: Remote Config, 2: A/B Testing
    val tabs = listOf("FCM Messaging", "Remote Config", "A/B Testing")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 32.dp, bottom = 20.dp, start = 14.dp, end = 14.dp),
            color = Color(0xFF0D0D11),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF27272A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(0xFFFF9100).copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, Color(0xFFFF9100).copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color(0xFFFF9100),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "FIREBASE OPTIMIZATION",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "FCM • Remote Config • A/B Testing",
                                color = Color(0xFFFF9100),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF1F1F24), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF16161B), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFFF9100) else Color.Transparent)
                                .clickable { selectedTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.7f),
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content by Tab
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // ==========================================
                            // TAB 0: FIREBASE CLOUD MESSAGING (FCM)
                            // ==========================================
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFF141418),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0xFF27272A))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "FCM DEVICE REGISTRATION",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Surface(
                                                color = Color(0xFF00E676).copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    "CONNECTED",
                                                    color = Color(0xFF00E676),
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        val tokenStr = fcmToken ?: "Retrieving device token from Firebase..."
                                        Text(
                                            text = tokenStr.take(48) + if (tokenStr.length > 48) "..." else "",
                                            color = VelorixTextSecondary,
                                            fontSize = 11.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
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
                                            border = BorderStroke(1.dp, Color(0xFF3F3F46)),
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Copy Registration Token", color = Color.White, fontSize = 11.5.sp)
                                        }
                                    }
                                }
                            }

                            item {
                                Text(
                                    "SUBSCRIBED FCM TOPICS",
                                    color = VelorixTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("tournaments", "match_reminders", "announcements").forEach { topic ->
                                        Surface(
                                            color = Color(0xFF1E1E24),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Color(0xFF32323A))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(modifier = Modifier.size(6.dp).background(Color(0xFF38BDF8), CircleShape))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(topic, color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Text(
                                    "TEST PUSH WORKFLOWS (REAL PUSH & IN-APP)",
                                    color = VelorixTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val activeTourney = tournaments.firstOrNull()
                                if (activeTourney == null) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color(0xFF1E1420),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, VelorixAccent.copy(alpha = 0.3f))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = "No Live Tournaments in Database",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Create an active tournament first to broadcast real FCM push notifications to registered players.",
                                                color = VelorixTextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                } else {
                                    // Push 1: Room Details
                                    PushActionCard(
                                        icon = Icons.Default.Lock,
                                        title = "Send Room Details Push (FCM)",
                                        subtitle = "Notifies registered players with instant Room ID and Password for ${activeTourney.title}",
                                        buttonText = "Trigger Room Push",
                                        onAction = {
                                            val roomId = activeTourney.roomDetails?.roomId?.ifBlank { "882104" } ?: "882104"
                                            val roomPass = activeTourney.roomDetails?.roomPassword?.ifBlank { "vx77" } ?: "vx77"
                                            VelorixFcmManager.dispatchRoomDetailsPush(
                                                context = context,
                                                tournament = activeTourney,
                                                roomId = roomId,
                                                roomPassword = roomPass
                                            )
                                            Toast.makeText(context, "Room Details Push Dispatched for ${activeTourney.title}!", Toast.LENGTH_SHORT).show()
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Push 2: Match Reminder
                                    PushActionCard(
                                        icon = Icons.Default.Notifications,
                                        title = "Send 15-Minute Match Reminder (FCM)",
                                        subtitle = "Broadcasts urgent 15-minute preparation warning for ${activeTourney.title}",
                                        buttonText = "Trigger 15-Min Reminder",
                                        onAction = {
                                            VelorixFcmManager.dispatchMatchReminderPush(
                                                context = context,
                                                tournament = activeTourney,
                                                minutesLeft = 15
                                            )
                                            Toast.makeText(context, "15-Min Match Reminder Dispatched for ${activeTourney.title}!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Push 3: Transaction Alert
                                PushActionCard(
                                    icon = Icons.Default.ShoppingCart,
                                    title = "Send Payout Alert Push (FCM)",
                                    subtitle = "Alerts user immediately when withdrawal request changes to completed",
                                    buttonText = "Trigger Payout Alert",
                                    onAction = {
                                        com.example.notification.VelorixNotificationManager.dispatchPayoutNotification(
                                            context = context,
                                            username = "PlayerOne",
                                            amount = 1250.0,
                                            approved = true,
                                            details = "UPI ID (player@okhdfcbank)"
                                        )
                                        Toast.makeText(context, "Transaction Alert Dispatched!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }

                        1 -> {
                            // ==========================================
                            // TAB 1: FIREBASE REMOTE CONFIG
                            // ==========================================
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFF141418),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0xFF27272A))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "REMOTE CONFIG ENGINE",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Surface(
                                                color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    remoteConfigState.lastFetchStatus,
                                                    color = Color(0xFF38BDF8),
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            "Dynamic app configuration without Play Store releases. Changes in Firebase Console apply immediately.",
                                            color = VelorixTextSecondary,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Button(
                                            onClick = {
                                                VelorixRemoteConfigManager.fetchAndActivate()
                                                Toast.makeText(context, "Fetching latest Remote Config...", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Fetch & Activate Realtime Config", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            item {
                                Text(
                                    "LIVE CONFIGURATION PARAMETERS",
                                    color = VelorixTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                RemoteConfigParamRow(
                                    paramKey = "is_maintenance_mode",
                                    paramValue = remoteConfigState.isMaintenanceMode.toString().uppercase(),
                                    description = "Toggles global maintenance banner/lock screen",
                                    badgeColor = if (remoteConfigState.isMaintenanceMode) Color(0xFFEF4444) else Color(0xFF10B981)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                RemoteConfigParamRow(
                                    paramKey = "maintenance_message",
                                    paramValue = remoteConfigState.maintenanceMessage,
                                    description = "Custom notice shown to players when maintenance mode is active"
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                RemoteConfigParamRow(
                                    paramKey = "cta_button_text",
                                    paramValue = remoteConfigState.ctaButtonText,
                                    description = "Active A/B testing variant for registration button",
                                    badgeColor = Color(0xFF818CF8)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                RemoteConfigParamRow(
                                    paramKey = "match_reminder_lead_minutes",
                                    paramValue = "${remoteConfigState.matchReminderLeadMinutes} Minutes",
                                    description = "Lead time for FCM push notifications before match starts"
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                RemoteConfigParamRow(
                                    paramKey = "max_slots_per_tournament",
                                    paramValue = "${remoteConfigState.maxSlotsPerTournament} Slots",
                                    description = "Default lobby capacity rule for Free Fire custom rooms"
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                RemoteConfigParamRow(
                                    paramKey = "vip_experience_enabled",
                                    paramValue = remoteConfigState.isVipExperienceEnabled.toString().uppercase(),
                                    description = "Enables VIP badge and priority slot reservation"
                                )
                            }
                        }

                        2 -> {
                            // ==========================================
                            // TAB 2: FIREBASE A/B TESTING & ANALYTICS
                            // ==========================================
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFF141418),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0xFF27272A))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            "EXPERIMENT 1: CALL-TO-ACTION (CTA) REFINEMENT",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "Testing whether button text 'Join Tournament' outperforms 'Register Now' using Google Analytics event 'cta_button_click' to measure conversion success.",
                                            color = VelorixTextSecondary,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                modifier = Modifier.weight(1f),
                                                color = if (remoteConfigState.ctaButtonText == "Join Tournament") Color(0xFF1E293B) else Color(0xFF0F172A),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, if (remoteConfigState.ctaButtonText == "Join Tournament") Color(0xFF38BDF8) else Color(0xFF1E293B))
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text("Variant A (Default)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                                    Text("Join Tournament", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                            }

                                            Surface(
                                                modifier = Modifier.weight(1f),
                                                color = if (remoteConfigState.ctaButtonText != "Join Tournament") Color(0xFF1E293B) else Color(0xFF0F172A),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, if (remoteConfigState.ctaButtonText != "Join Tournament") Color(0xFF38BDF8) else Color(0xFF1E293B))
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text("Variant B (Experimental)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                                    Text("Register Now", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Button(
                                            onClick = {
                                                VelorixAnalytics.logCtaClick(
                                                    ctaText = remoteConfigState.ctaButtonText,
                                                    tournamentId = "vx_exp_test",
                                                    tournamentTitle = "A/B Experiment Test"
                                                )
                                                Toast.makeText(context, "Logged A/B Analytics Event: cta_button_click ('${remoteConfigState.ctaButtonText}')", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Log CTA Click Event (${remoteConfigState.ctaButtonText})", color = Color.White, fontSize = 11.5.sp)
                                        }
                                    }
                                }
                            }

                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFF141418),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0xFF27272A))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            "EXPERIMENT 2: PUSH NOTIFICATION COPY OPTIMIZATION",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "Evaluating push notification open rates between urgency hooks ('Are you ready for battle?') vs informative hooks ('New tournament starting now!').",
                                            color = VelorixTextSecondary,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            color = Color(0xFF0D121E),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Active Variant: \"${remoteConfigState.notificationCopyVariant}\"",
                                                color = Color(0xFF38BDF8),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFF141418),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0xFF27272A))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            "ANALYTICS FUNNEL TELEMETRY",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        listOf(
                                            "cta_button_click" to "A/B Testing conversion tracker",
                                            "tournament_joined" to "Registration checkout event",
                                            "room_credentials_viewed" to "Room ID/Pass access tracker",
                                            "notification_interaction" to "Push opened / received metric"
                                        ).forEach { (event, desc) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(event, color = Color.White, fontSize = 11.5.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                                    Text(desc, color = VelorixTextSecondary, fontSize = 10.sp)
                                                }
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
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
    }
}

@Composable
private fun PushActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    buttonText: String,
    onAction: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF141418),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF27272A))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF22222A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color(0xFFFF9100), modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = VelorixTextSecondary, fontSize = 10.5.sp, lineHeight = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F1F26)),
                border = BorderStroke(1.dp, Color(0xFFFF9100).copy(alpha = 0.6f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color(0xFFFF9100), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(buttonText, color = Color(0xFFFF9100), fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
            }
        }
    }
}

@Composable
private fun RemoteConfigParamRow(
    paramKey: String,
    paramValue: String,
    description: String,
    badgeColor: Color = Color(0xFF64748B)
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF141418),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF22222A))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = paramKey,
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    color = badgeColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = paramValue,
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, color = VelorixTextSecondary, fontSize = 10.sp)
        }
    }
}
