package com.example.ui

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.domain.model.AppNotification
import com.example.domain.model.GlobalAnnouncement
import com.example.ui.common.GlassCard
import com.example.ui.theme.*

@Composable
fun NotificationCenterDialog(
    notifications: List<AppNotification>,
    announcements: List<GlobalAnnouncement> = emptyList(),
    unreadCount: Int,
    onDismiss: () -> Unit,
    onMarkRead: (String) -> Unit,
    onClearAll: () -> Unit,
    onTestPushNotification: () -> Unit,
    onBroadcastCampaign: () -> Unit,
    onTournamentClick: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredNotifications = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            "TOURNAMENT" -> notifications.filter { it.type == "TOURNAMENT" }
            "CAMPAIGN" -> notifications.filter { it.type == "CAMPAIGN" }
            "UNREAD" -> notifications.filter { !it.isRead }
            else -> notifications
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp, bottom = 16.dp, start = 12.dp, end = 12.dp),
            color = Color(0xFF0F0F12),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF27272A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
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
                                .size(40.dp)
                                .background(VelorixAccent.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, VelorixAccent.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = VelorixAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "NOTIFICATIONS & CAMPAIGNS",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(6.dp),
                                    shape = CircleShape,
                                    color = Color(0xFF00E676)
                                ) {}
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (unreadCount > 0) "$unreadCount Unread • Push Engine Active" else "Push & In-App Engine Active",
                                    fontSize = 11.sp,
                                    color = VelorixTextSecondary
                                )
                            }
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

                Spacer(modifier = Modifier.height(14.dp))

                // Action Bar (Test Push & Broadcast)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTestPushNotification,
                        colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Push Alert", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }

                    OutlinedButton(
                        onClick = onBroadcastCampaign,
                        border = BorderStroke(1.dp, Color(0xFF3F3F46)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Broadcast", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Chips & Clear Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        item {
                            FilterChipItem(
                                label = "All (${notifications.size})",
                                selected = selectedFilter == "ALL",
                                onClick = { selectedFilter = "ALL" }
                            )
                        }
                        item {
                            FilterChipItem(
                                label = "Tournaments (${notifications.count { it.type == "TOURNAMENT" }})",
                                selected = selectedFilter == "TOURNAMENT",
                                onClick = { selectedFilter = "TOURNAMENT" }
                            )
                        }
                        item {
                            FilterChipItem(
                                label = "Campaigns (${notifications.count { it.type == "CAMPAIGN" }})",
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

                    if (notifications.isNotEmpty()) {
                        Text(
                            text = "Clear All",
                            fontSize = 11.sp,
                            color = Color(0xFFFF5252),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onClearAll() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notification List
                if (filteredNotifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color(0xFF3F3F46),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No notifications found",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = VelorixTextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Every tournament created or campaign sent will appear here and fire real push notifications.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    val distinctNotifications = remember(filteredNotifications) {
                        val seen = mutableSetOf<String>()
                        filteredNotifications.filter { n ->
                            val k = if (n.id.isNotBlank()) n.id else "${n.title}_${n.timestamp}"
                            if (k.isNotBlank() && seen.add(k)) true else if (k.isBlank()) true else false
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(distinctNotifications, key = { "${it.id}_${distinctNotifications.indexOf(it)}" }) { notif ->
                            NotificationListItemCard(
                                notification = notif,
                                onMarkRead = { onMarkRead(notif.id) },
                                onClick = {
                                    onMarkRead(notif.id)
                                    if (notif.targetTournamentId.isNotBlank()) {
                                        onTournamentClick(notif.targetTournamentId)
                                        onDismiss()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) VelorixAccent.copy(alpha = 0.2f) else Color(0xFF1F1F24),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (selected) VelorixAccent else Color(0xFF27272A)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) VelorixAccent else VelorixTextSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun NotificationListItemCard(
    notification: AppNotification,
    onMarkRead: () -> Unit,
    onClick: () -> Unit
) {
    val isUrgent = notification.priority.equals("URGENT", ignoreCase = true)
    val accentColor = if (isUrgent) Color(0xFFFF3366) else VelorixAccent
    val isUnread = !notification.isRead

    Surface(
        color = if (isUnread) Color(0xFF17171C) else Color(0xFF121215),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isUnread) accentColor.copy(alpha = 0.4f) else Color(0xFF27272A)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, accentColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (notification.type == "TOURNAMENT") Icons.Default.EmojiEvents else Icons.Default.Campaign,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isUnread) {
                            Surface(
                                modifier = Modifier.size(6.dp),
                                shape = CircleShape,
                                color = accentColor
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (notification.type == "TOURNAMENT") "TOURNAMENT CAMPAIGN" else "BROADCAST",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor,
                            letterSpacing = 0.5.sp
                        )
                    }

                    val timeAgo = try {
                        DateUtils.getRelativeTimeSpanString(
                            notification.timestamp,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE
                        ).toString()
                    } catch (e: Exception) {
                        "Just now"
                    }

                    Text(
                        text = timeAgo,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = notification.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnread) Color.White else VelorixTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = notification.message,
                    fontSize = 11.sp,
                    color = VelorixTextSecondary,
                    lineHeight = 15.sp
                )

                if (notification.targetTournamentId.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = VelorixAccent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, VelorixAccent.copy(alpha = 0.3f)),
                        modifier = Modifier.clickable { onClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "View Tournament Details",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = VelorixAccent
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = VelorixAccent,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
