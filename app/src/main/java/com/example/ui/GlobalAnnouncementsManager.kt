package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.domain.model.GlobalAnnouncement
import com.example.ui.common.GlassCard
import com.example.ui.theme.*

@Composable
fun GlobalAnnouncementsBanner(
    announcements: List<GlobalAnnouncement>,
    isAdmin: Boolean = true,
    onPublishClick: () -> Unit,
    onDeleteClick: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(8.dp),
                    shape = CircleShape,
                    color = Color(0xFFFF5252)
                ) {}
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LIVE ANNOUNCEMENTS (${announcements.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = VelorixTextSecondary,
                    letterSpacing = 1.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isAdmin) {
                    Text(
                        text = "+ Broadcast",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelorixAccent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onPublishClick() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (announcements.isEmpty()) {
            Surface(
                color = CardVerifyBg,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CardVerifyBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "All tournament servers operational. Tap + Broadcast to send alerts to players.",
                        fontSize = 11.sp,
                        color = VelorixTextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(announcements) { announcement ->
                    val priorityColor = when (announcement.priority.uppercase()) {
                        "URGENT" -> Color(0xFFFF5252)
                        "MAINTENANCE" -> Color(0xFFFFB74D)
                        "TOURNAMENT" -> VelorixAccent
                        else -> Color(0xFF64B5F6)
                    }

                    GlassCard(
                        modifier = Modifier.width(280.dp),
                        shape = RoundedCornerShape(16.dp),
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                priorityColor.copy(alpha = 0.15f),
                                Color(0xFF1E1A2A)
                            )
                        ),
                        borderBrush = Brush.horizontalGradient(
                            colors = listOf(
                                priorityColor.copy(alpha = 0.6f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = priorityColor.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = announcement.priority.uppercase(),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = priorityColor
                                    )
                                }

                                if (isAdmin) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete announcement",
                                        tint = VelorixTextSecondary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { onDeleteClick(announcement.id) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = announcement.title,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = announcement.message,
                                fontSize = 11.sp,
                                color = VelorixTextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PublishAnnouncementDialog(
    onDismiss: () -> Unit,
    onPublish: (GlobalAnnouncement) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("URGENT") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Campaign, contentDescription = null, tint = VelorixAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Broadcast Live Announcement", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Announcement Title") },
                    placeholder = { Text("e.g. Free Fire Room ID Released!") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Broadcast Message") },
                    placeholder = { Text("Enter details for all active players...") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                )

                Text("Priority Level", fontSize = 11.sp, color = VelorixTextSecondary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("URGENT", "TOURNAMENT", "MAINTENANCE", "INFO").forEach { p ->
                        val isSelected = priority == p
                        FilterChip(
                            selected = isSelected,
                            onClick = { priority = p },
                            label = { Text(p, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VelorixAccent,
                                selectedLabelColor = Color.Black,
                                containerColor = CardVerifyBg,
                                labelColor = VelorixTextSecondary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank() || message.isBlank()) {
                        Toast.makeText(context, "Please enter title and message", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val announcement = GlobalAnnouncement(
                        id = "ANN-${System.currentTimeMillis()}",
                        title = title.trim(),
                        message = message.trim(),
                        priority = priority
                    )
                    onPublish(announcement)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent)
            ) {
                Text("Broadcast to All", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VelorixTextSecondary)
            }
        },
        containerColor = CardVerifyBg
    )
}
