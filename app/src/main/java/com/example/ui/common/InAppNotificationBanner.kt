package com.example.ui.common

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.domain.model.AppNotification
import com.example.notification.VelorixNotificationManager
import com.example.ui.theme.VelorixAccent
import com.example.ui.theme.VelorixTextPrimary
import com.example.ui.theme.VelorixTextSecondary
import kotlinx.coroutines.delay

@Composable
fun InAppNotificationBannerHost(
    onNotificationClick: (AppNotification) -> Unit = {}
) {
    var activeNotification by remember { mutableStateOf<AppNotification?>(null) }

    LaunchedEffect(Unit) {
        VelorixNotificationManager.inAppNotificationEvents.collect { notif ->
            activeNotification = notif
            // Auto dismiss after 5 seconds
            delay(5000)
            if (activeNotification?.id == notif.id) {
                activeNotification = null
            }
        }
    }

    AnimatedVisibility(
        visible = activeNotification != null,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        ) + fadeOut(),
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(9999f)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val notif = activeNotification
        if (notif != null) {
            InAppNotificationBannerCard(
                notification = notif,
                onClick = {
                    onNotificationClick(notif)
                    activeNotification = null
                },
                onDismiss = { activeNotification = null }
            )
        }
    }
}

@Composable
fun InAppNotificationBannerCard(
    notification: AppNotification,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val isUrgent = notification.priority.equals("URGENT", ignoreCase = true)
    val accentColor = if (isUrgent) Color(0xFFFF3366) else VelorixAccent

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = accentColor)
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(accentColor.copy(alpha = 0.8f), Color(0xFF27272A))
                ),
                RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = Color(0xFF16161A).copy(alpha = 0.96f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (notification.type == "TOURNAMENT") Icons.Default.EmojiEvents else Icons.Default.Campaign,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (notification.type == "TOURNAMENT") "NEW TOURNAMENT CAMPAIGN" else "LIVE BROADCAST",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = accentColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "JUST NOW",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = notification.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = VelorixTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = notification.message,
                    fontSize = 11.sp,
                    color = VelorixTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
