package com.example.ui

import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppNotification
import com.example.domain.model.GlobalAnnouncement
import com.example.domain.model.Tournament
import com.example.domain.model.UserProfile
import com.example.ui.common.FrostedGlassDialog
import com.example.ui.theme.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun NotificationCenterDialog(
    notifications: List<AppNotification>,
    announcements: List<GlobalAnnouncement> = emptyList(),
    tournaments: List<Tournament> = emptyList(),
    users: List<UserProfile> = emptyList(),
    unreadCount: Int,
    onDismiss: () -> Unit,
    onMarkRead: (String) -> Unit,
    onClearAll: () -> Unit,
    onTestPushNotification: () -> Unit,
    onBroadcastCampaign: () -> Unit,
    onPublishAnnouncement: ((GlobalAnnouncement) -> Unit)? = null,
    onPublishCampaign: ((String, String, String, String?) -> Unit)? = null,
    onTournamentClick: (String) -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Inbox, 1 = Compose Manual
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredNotifications = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            "TOURNAMENT" -> notifications.filter { it.type == "TOURNAMENT" }
            "CAMPAIGN" -> notifications.filter { it.type == "CAMPAIGN" }
            "UNREAD" -> notifications.filter { !it.isRead }
            else -> notifications
        }
    }

    FrostedGlassDialog(
        onDismissRequest = onDismiss,
        maxWidth = 560.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(16.dp)
        ) {
            // Minimal Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Notifications",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (unreadCount > 0) "$unreadCount unread" else "Live sync active",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Flat Underline Mode Selector: Inbox vs Write & Send (No nested box)
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Inbox (${notifications.size})", "Write & Send").forEachIndexed { index, label ->
                    val selected = activeTab == index
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = index }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else Color(0xFF71717A)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (selected) 1.5.dp else 0.5.dp)
                                .background(if (selected) Color.White else Color.White.copy(alpha = 0.08f))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (activeTab == 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    ManualNotificationComposerSection(
                        tournaments = tournaments,
                        users = users,
                        onPublishAnnouncement = onPublishAnnouncement,
                        onPublishCampaign = onPublishCampaign,
                        onSentSuccess = { activeTab = 0 }
                    )
                }
            } else {
                // Filter Mode Selector & Clear Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        item {
                            FilterChipItem(
                                label = "All",
                                selected = selectedFilter == "ALL",
                                onClick = { selectedFilter = "ALL" }
                            )
                        }
                        item {
                            FilterChipItem(
                                label = "Matches",
                                selected = selectedFilter == "TOURNAMENT",
                                onClick = { selectedFilter = "TOURNAMENT" }
                            )
                        }
                        item {
                            FilterChipItem(
                                label = "Broadcasts",
                                selected = selectedFilter == "CAMPAIGN",
                                onClick = { selectedFilter = "CAMPAIGN" }
                            )
                        }
                        if (unreadCount > 0) {
                            item {
                                FilterChipItem(
                                    label = "Unread ($unreadCount)",
                                    selected = selectedFilter == "UNREAD",
                                    onClick = { selectedFilter = "UNREAD" }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        val context = LocalContext.current
                        Text(
                            text = "Test User Push",
                            fontSize = 11.sp,
                            color = VelorixAccent,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable {
                                    onTestPushNotification()
                                    Toast.makeText(
                                        context,
                                        "Test Push dispatched to User Panel (com.tournaments.velorix)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .padding(vertical = 4.dp)
                        )
                        if (notifications.isNotEmpty()) {
                            Text(
                                text = "Clear",
                                fontSize = 11.sp,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable { onClearAll() }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                if (filteredNotifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.NotificationsNone,
                                contentDescription = null,
                                tint = Color(0xFF52525B),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No notifications",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF71717A)
                            )
                        }
                    }
                } else {
                    val distinctNotifications = remember(filteredNotifications) {
                        val seen = mutableSetOf<String>()
                        filteredNotifications.filter { n ->
                            val k = if (n.id.isNotBlank()) n.id else "${n.title}_${n.timestamp}"
                            if (k.isNotBlank() && seen.add(k)) true else k.isBlank()
                        }
                    }

                    // Flat Hairline List (Zero nested Surface box)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 420.dp)
                    ) {
                        itemsIndexed(
                            distinctNotifications,
                            key = { idx, item -> "${item.id}_$idx" }
                        ) { _, notif ->
                            NotificationHairlineRow(
                                notification = notif,
                                onClick = {
                                    onMarkRead(notif.id)
                                    if (notif.targetTournamentId.isNotBlank()) {
                                        onTournamentClick(notif.targetTournamentId)
                                        onDismiss()
                                    }
                                }
                            )
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = Color.White.copy(alpha = 0.08f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable Manual Notification Composer & Dispatcher Section.
 * Flat pure AMOLED black with 0.5.dp hairline dividers and underline mode selectors (no box-in-box).
 */
@Composable
fun ManualNotificationComposerSection(
    tournaments: List<Tournament> = emptyList(),
    users: List<UserProfile> = emptyList(),
    onPublishAnnouncement: ((GlobalAnnouncement) -> Unit)? = null,
    onPublishCampaign: ((String, String, String, String?) -> Unit)? = null,
    onSentSuccess: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Target Audience: ALL, TOURNAMENT, USER
    var audienceType by remember { mutableStateOf("ALL") }
    var selectedTournamentId by remember(tournaments) {
        mutableStateOf(tournaments.firstOrNull()?.id ?: "")
    }
    var selectedUserId by remember { mutableStateOf("") }

    // Notification Category & Priority
    var categoryChip by remember { mutableStateOf("ANNOUNCEMENT") }
    var priorityChip by remember { mutableStateOf("HIGH") }

    // Toggles
    var attachRoomDetails by remember { mutableStateOf(false) }
    var pinAsLiveBanner by remember { mutableStateOf(true) }
    var sendPushAlert by remember { mutableStateOf(true) }

    // Text inputs
    var titleInput by remember { mutableStateOf("") }
    var messageInput by remember { mutableStateOf("") }

    val selectedTournament = remember(tournaments, selectedTournamentId) {
        tournaments.find { it.id == selectedTournamentId } ?: tournaments.firstOrNull()
    }

    val quickTemplates = remember {
        listOf(
            Triple("Room ID Live", "Room ID Released", "Custom Room ID and password are now live. Join your slot within 5 minutes."),
            Triple("Starting 10m", "Match Starts in 10m", "Check in now and verify your Free Fire IGN before the lobby locks."),
            Triple("Prize Sent", "Payout Disbursed", "Match winnings have been credited to your wallet balance."),
            Triple("Results Up", "Standings Updated", "Official match standings and kill points are now updated."),
            Triple("Maintenance", "Server Update", "Scheduled maintenance in progress. Match lobbies will resume shortly.")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
    ) {
        // Section 1: Target Audience Underline Mode Selector
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TARGET",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF71717A),
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = when (audienceType) {
                        "TOURNAMENT" -> selectedTournament?.title?.take(22) ?: "Select Match"
                        "USER" -> if (selectedUserId.isNotBlank()) "UID: ${selectedUserId.take(10)}" else "Select Player"
                        else -> "All Registered Players"
                    },
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf(
                    "ALL" to "All Players",
                    "TOURNAMENT" to "Match Roster",
                    "USER" to "Direct Player"
                ).forEach { (key, label) ->
                    val isSelected = audienceType == key
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { audienceType = key }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF71717A)
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isSelected) 1.5.dp else 0.5.dp)
                                .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.08f))
                        )
                    }
                }
            }

            // Conditional Match Selector
            if (audienceType == "TOURNAMENT" && tournaments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    tournaments.forEach { t ->
                        val isSel = selectedTournament?.id == t.id
                        FilterChipItem(
                            label = "${t.title.take(18)} (${t.registeredPlayers}/${t.maxPlayers})",
                            selected = isSel,
                            onClick = { selectedTournamentId = t.id }
                        )
                    }
                }
            }

            // Conditional Direct Player Selector
            if (audienceType == "USER") {
                Spacer(modifier = Modifier.height(8.dp))
                val validUsers = remember(users) { users.filter { !it.isGuestSession }.take(12) }
                if (validUsers.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        validUsers.forEach { u ->
                            val isSel = selectedUserId == u.id
                            FilterChipItem(
                                label = u.bestDisplayName.take(14),
                                selected = isSel,
                                onClick = { selectedUserId = u.id }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                OutlinedTextField(
                    value = selectedUserId,
                    onValueChange = { selectedUserId = it },
                    placeholder = { Text("Player UID or select above", fontSize = 11.5.sp, color = Color(0xFF71717A)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

        // Section 2: Type, Priority & Quick Presets
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TYPE & PRIORITY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF71717A),
                    letterSpacing = 0.8.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("NORMAL", "HIGH", "URGENT").forEach { p ->
                        val isSel = priorityChip == p
                        val pColor = when (p) {
                            "URGENT" -> Color(0xFFEF4444)
                            "HIGH" -> VelorixAccent
                            else -> Color.White
                        }
                        Text(
                            text = p,
                            fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) pColor else Color(0xFF71717A),
                            modifier = Modifier
                                .clickable { priorityChip = p }
                                .padding(vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category selection items
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                listOf(
                    "ANNOUNCEMENT" to "Announcement",
                    "ROOM_ID" to "Room ID",
                    "MATCH_ALERT" to "Match Alert",
                    "PAYOUT" to "Payout",
                    "SYSTEM" to "System"
                ).forEach { (key, label) ->
                    val isSel = categoryChip == key
                    FilterChipItem(
                        label = label,
                        selected = isSel,
                        onClick = {
                            categoryChip = key
                            if (key == "ROOM_ID") attachRoomDetails = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.06f))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "QUICK TEMPLATES",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF71717A),
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                quickTemplates.forEach { (chipLabel, presetTitle, presetBody) ->
                    val isCurrent = titleInput == presetTitle
                    FilterChipItem(
                        label = chipLabel,
                        selected = isCurrent,
                        onClick = {
                            titleInput = presetTitle
                            messageInput = presetBody
                        }
                    )
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

        // Section 3: Title & Message Inputs
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                placeholder = { Text("Notification title", fontSize = 12.5.sp, color = Color(0xFF71717A)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                placeholder = { Text("Write message...", fontSize = 12.5.sp, color = Color(0xFF71717A)) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

        // Section 4: Hairline-Divided Toggles
        NotificationToggleRow(
            title = "Include Room ID & Password",
            subtitle = if (selectedTournament?.roomDetails?.roomId?.isNotBlank() == true)
                "Room: ${selectedTournament.roomDetails?.roomId}"
            else "Appends active match room credentials",
            checked = attachRoomDetails,
            onCheckedChange = { attachRoomDetails = it }
        )

        HorizontalDivider(
            thickness = 0.5.dp,
            color = Color.White.copy(alpha = 0.08f)
        )

        NotificationToggleRow(
            title = "Pin to Live Banner",
            subtitle = "Shows on player home feed",
            checked = pinAsLiveBanner,
            onCheckedChange = { pinAsLiveBanner = it }
        )

        HorizontalDivider(
            thickness = 0.5.dp,
            color = Color.White.copy(alpha = 0.08f)
        )

        NotificationToggleRow(
            title = "Send Push Notification",
            subtitle = "Triggers real-time device alert",
            checked = sendPushAlert,
            onCheckedChange = { sendPushAlert = it }
        )

        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

        // Section 5: Dispatch Action
        Box(modifier = Modifier.padding(top = 12.dp)) {
            Button(
                onClick = {
                    if (titleInput.isBlank() || messageInput.isBlank()) {
                        Toast.makeText(context, "Enter title and message", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val roomAppendix = if (attachRoomDetails && selectedTournament != null) {
                        val rId = selectedTournament.roomDetails?.roomId?.ifBlank { "TBA" } ?: "TBA"
                        val rPass = selectedTournament.roomDetails?.roomPassword?.ifBlank { "None" } ?: "None"
                        "\nRoom ID: $rId | Pass: $rPass"
                    } else ""

                    val finalTitle = titleInput.trim()
                    val finalBody = (messageInput.trim() + roomAppendix).trim()
                    val targetTourneyId = if (audienceType == "TOURNAMENT") selectedTournament?.id else null
                    val targetUid = if (audienceType == "USER") selectedUserId.trim() else null

                    // 1. Trigger Campaign / Push via ViewModel callback if global/tournament
                    if (sendPushAlert && targetUid.isNullOrBlank()) {
                        onPublishCampaign?.invoke(finalTitle, finalBody, priorityChip, targetTourneyId)
                    }

                    // 2. Pin to Live Banner if toggled
                    if (pinAsLiveBanner) {
                        val ann = GlobalAnnouncement(
                            id = "ANN-${System.currentTimeMillis()}",
                            title = finalTitle,
                            message = finalBody,
                            level = priorityChip,
                            createdAt = System.currentTimeMillis(),
                            author = "Admin",
                            isActive = true
                        )
                        onPublishAnnouncement?.invoke(ann)
                    }

                    // 3. Direct User Panel RTDB & Firestore dispatch via VelorixFcmManager
                    val notifId = "notif_${System.currentTimeMillis()}"
                    val userNotif = AppNotification(
                        id = notifId,
                        title = finalTitle,
                        message = finalBody,
                        type = categoryChip,
                        targetTournamentId = targetTourneyId ?: "",
                        game = selectedTournament?.game ?: "Free Fire",
                        timestamp = System.currentTimeMillis(),
                        priority = priorityChip,
                        isRead = false
                    )
                    com.example.notification.VelorixFcmManager.dispatchToUserPanel(
                        notification = userNotif,
                        audience = audienceType,
                        targetUserId = targetUid,
                        targetTournamentId = targetTourneyId,
                        pinToGlobalAnnouncements = pinAsLiveBanner
                    )

                    Toast.makeText(
                        context,
                        "Notification sent to User Panel (com.tournaments.velorix)",
                        Toast.LENGTH_SHORT
                    ).show()
                    titleInput = ""
                    messageInput = ""
                    onSentSuccess?.invoke()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Send to User Panel",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 10.5.sp,
                color = Color(0xFF71717A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color.White,
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF1F1F24)
            )
        )
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else Color(0xFF71717A)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(1.5.dp)
                .background(if (selected) Color.White else Color.Transparent)
        )
    }
}

@Composable
private fun NotificationHairlineRow(
    notification: AppNotification,
    onClick: () -> Unit
) {
    val isUrgent = notification.priority.equals("URGENT", ignoreCase = true)
    val accentColor = if (isUrgent) Color(0xFFEF4444) else VelorixAccent
    val isUnread = !notification.isRead

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(7.dp)
                .background(
                    if (isUnread) accentColor else Color(0xFF3F3F46),
                    CircleShape
                )
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.title.ifBlank { "Notification" },
                    fontSize = 13.sp,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                val timeAgo = try {
                    DateUtils.getRelativeTimeSpanString(
                        notification.timestamp,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE
                    ).toString()
                } catch (_: Exception) {
                    "Now"
                }

                Text(
                    text = timeAgo,
                    fontSize = 10.sp,
                    color = Color(0xFF71717A)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = notification.message,
                fontSize = 11.5.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (notification.targetTournamentId.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Open Match",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VelorixAccent
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = VelorixAccent,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}
