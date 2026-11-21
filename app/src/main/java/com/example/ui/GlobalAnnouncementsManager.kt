package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.GlobalAnnouncement
import com.example.ui.common.FrostedGlassDialog
import com.example.ui.theme.*

@Composable
fun GlobalAnnouncementsBanner(
    announcements: List<GlobalAnnouncement>,
    isAdmin: Boolean = true,
    onPublishClick: () -> Unit,
    onDeleteClick: (String) -> Unit
) {
    var dismissedIds by remember { mutableStateOf(setOf<String>()) }
    val visibleAnnouncements = remember(announcements, dismissedIds) {
        announcements.filter { it.id !in dismissedIds }
    }

    if (visibleAnnouncements.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
    ) {
        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color(0xFFEF4444), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ANNOUNCEMENTS (${visibleAnnouncements.size})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.8.sp
                )
            }

            if (isAdmin) {
                Text(
                    text = "+ New",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .clickable { onPublishClick() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.07f))

        visibleAnnouncements.take(3).forEachIndexed { index, announcement ->
            val priorityColor = when (announcement.priority.uppercase()) {
                "URGENT" -> Color(0xFFEF4444)
                "MAINTENANCE" -> Color(0xFFF59E0B)
                "TOURNAMENT" -> VelorixAccent
                else -> Color(0xFF38BDF8)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = announcement.priority.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = priorityColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = announcement.title,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = announcement.message,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isAdmin) {
                    IconButton(
                        onClick = {
                            dismissedIds = dismissedIds + announcement.id
                            onDeleteClick(announcement.id)
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color(0xFF71717A),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = Color.White.copy(alpha = 0.08f)
            )
        }
    }
}

@Composable
fun PublishAnnouncementDialog(
    tournaments: List<com.example.domain.model.Tournament> = emptyList(),
    users: List<com.example.domain.model.UserProfile> = emptyList(),
    onDismiss: () -> Unit,
    onPublish: (GlobalAnnouncement) -> Unit,
    onPublishCampaign: ((title: String, message: String, targetTopic: String, deepLink: String?) -> Unit)? = null
) {
    FrostedGlassDialog(
        onDismissRequest = onDismiss,
        maxWidth = 520.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Send Notification & Broadcast",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            ManualNotificationComposerSection(
                tournaments = tournaments,
                users = users,
                onPublishAnnouncement = onPublish,
                onPublishCampaign = onPublishCampaign,
                onSentSuccess = onDismiss
            )
        }
    }
}
