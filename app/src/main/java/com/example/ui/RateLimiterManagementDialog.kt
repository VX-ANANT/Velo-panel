package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.validation.UserRateLimiter
import com.example.ui.common.GlobalErrorManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateLimiterManagementDialog(
    userIdentifier: String = "system",
    onDismissRequest: () -> Unit
) {
    var snapshots by remember { mutableStateOf(UserRateLimiter.getAllSnapshots(userIdentifier)) }
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        snapshots = UserRateLimiter.getAllSnapshots(userIdentifier)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Speed, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Backend Rate Limiter", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Sliding Window & Token Bucket Engine", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }

                IconButton(onClick = {
                    UserRateLimiter.resetAllLimits()
                    snapshots = UserRateLimiter.getAllSnapshots(userIdentifier)
                    GlobalErrorManager.emitSuccess("All rate limits & token buckets flushed")
                }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Reset All", tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF090B12),
                    border = BorderStroke(1.dp, Color(0xFF1E2638)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ACTIVE ENGINE", color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("Continuous Token Refill", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("SERVER SYNC", color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("Dual RTDB + Firestore", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(snapshots) { snap ->
                        RateSnapshotRow(
                            snapshot = snap,
                            onReset = {
                                try {
                                    val action = UserRateLimiter.ActionType.values().firstOrNull { it.actionDisplayName == snap.actionName }
                                    if (action != null) {
                                        UserRateLimiter.resetCooldown(action, userIdentifier)
                                        refreshTrigger++
                                    }
                                } catch (_: Exception) {}
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismissRequest,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Close Monitor", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF111420),
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun RateSnapshotRow(
    snapshot: UserRateLimiter.RateLimitSnapshot,
    onReset: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0C0F1A),
        border = BorderStroke(1.dp, if (snapshot.isLocked) Color(0xFFEF4444).copy(alpha = 0.5f) else Color(0xFF1E2638)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = snapshot.actionName,
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Text(
                                text = snapshot.category,
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Quota: ${snapshot.requestsPerMinute.toInt()} req/min • Burst: ${snapshot.burstCapacity}",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (snapshot.isLocked) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF7F1D1D)
                        ) {
                            Text(
                                text = "LOCKED ${snapshot.cooldownSeconds}s",
                                color = Color(0xFFFCA5A5),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF065F46)
                        ) {
                            Text(
                                text = "%.1f / %d Tokens".format(snapshot.availableTokens, snapshot.burstCapacity),
                                color = Color(0xFF6EE7B7),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Micro progress indicator for tokens
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (snapshot.availableTokens / snapshot.burstCapacity).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (snapshot.isLocked) Color(0xFFEF4444) else Color(0xFF10B981),
                trackColor = Color(0xFF1E2638)
            )
        }
    }
}
