package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.agent.ActionExecutionStatus
import com.example.data.agent.AgentActionType
import com.example.data.agent.VelorixAgentAction
import com.example.ui.audio.rememberVelorixSoundManager

// Refined Minimalist Palette (subtle, matte, non-oversaturated)
private val SubtleAmber = Color(0xFFD49A3D)
private val SubtleTeal = Color(0xFF438A8A)
private val SubtleEmerald = Color(0xFF3B8A6E)
private val SubtleRose = Color(0xFFB85D6B)
private val SubtlePurple = Color(0xFF7C72A0)
private val SubtleBlue = Color(0xFF4D7298)
private val CardBackground = Color(0xFF141722)
private val CardBorderColor = Color(0xFF262C3D)
private val ParameterBadgeBg = Color(0xFF1A1F2C)
private val SubtleTextColor = Color(0xFF94A3B8)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CyberpunkActionCard(
    action: VelorixAgentAction,
    onExecute: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColor = when (action.actionType) {
        AgentActionType.CREATE_TOURNAMENT -> SubtleAmber
        AgentActionType.UPDATE_ROOM_DETAILS -> SubtleTeal
        AgentActionType.BAN_PLAYER -> SubtleRose
        AgentActionType.UNBAN_PLAYER -> SubtleEmerald
        AgentActionType.DELETE_TOURNAMENT -> SubtleRose
        AgentActionType.UPDATE_TOURNAMENT -> SubtleBlue
        AgentActionType.RESOLVE_TICKET -> SubtlePurple
        AgentActionType.BROADCAST_ANNOUNCEMENT -> SubtleAmber
        else -> SubtleBlue
    }

    val actionIcon = when (action.actionType) {
        AgentActionType.CREATE_TOURNAMENT -> Icons.Default.SportsEsports
        AgentActionType.UPDATE_ROOM_DETAILS -> Icons.Default.VpnKey
        AgentActionType.BAN_PLAYER -> Icons.Default.Gavel
        AgentActionType.UNBAN_PLAYER -> Icons.Default.VerifiedUser
        AgentActionType.DELETE_TOURNAMENT -> Icons.Default.DeleteOutline
        AgentActionType.UPDATE_TOURNAMENT -> Icons.Default.Edit
        AgentActionType.RESOLVE_TICKET -> Icons.Default.ConfirmationNumber
        AgentActionType.BROADCAST_ANNOUNCEMENT -> Icons.Default.Campaign
        else -> Icons.Default.Bolt
    }

    val soundManager = rememberVelorixSoundManager()

    LaunchedEffect(action.status) {
        if (action.status == ActionExecutionStatus.COMPLETED) {
            soundManager.playSubtleChime()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = when (action.status) {
                    ActionExecutionStatus.COMPLETED -> SubtleEmerald.copy(alpha = 0.6f)
                    ActionExecutionStatus.FAILED -> SubtleRose.copy(alpha = 0.6f)
                    else -> CardBorderColor
                },
                shape = RoundedCornerShape(12.dp)
            ),
        color = CardBackground,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Action Type and Security Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(themeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = action.actionType.name.replace("_", " "),
                        color = themeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.3.sp
                    )
                }

                Surface(
                    color = Color(0xFF1A1F2E),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF283144))
                ) {
                    Text(
                        text = "SECURE ACTION",
                        color = Color(0xFF7E8B9F),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Summary Title
            Text(
                text = action.summary,
                color = Color(0xFFE2E8F0),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Parameter Badges (clean, structured, subtle)
            if (action.params.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    action.params.forEach { (key, value) ->
                        if (value.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ParameterBadgeBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "$key:",
                                        color = SubtleTextColor,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = value,
                                        color = Color(0xFFDDE3EA),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Sync Scope description
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF10131A), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = SubtleTextColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Syncs across Admin & User panels (RTDB + Firestore)",
                    color = SubtleTextColor,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action status / confirmation buttons
            when (action.status) {
                ActionExecutionStatus.PENDING_CONFIRMATION -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (action.actionType == AgentActionType.BAN_PLAYER || action.actionType == AgentActionType.DELETE_TOURNAMENT) {
                                    soundManager.playOrchestraHit()
                                } else {
                                    soundManager.playBrassHit()
                                }
                                onExecute()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColor,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CONFIRM & APPLY",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                soundManager.playSnapPop()
                                onDismiss()
                            },
                            modifier = Modifier.height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SubtleTextColor
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Text("DISMISS", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                ActionExecutionStatus.EXECUTING -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = themeColor,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Applying changes to database...",
                            color = SubtleTextColor,
                            fontSize = 11.5.sp
                        )
                    }
                }

                ActionExecutionStatus.COMPLETED -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SubtleEmerald.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SubtleEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = action.resultMessage ?: "Successfully applied & synced across panels.",
                            color = SubtleEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                ActionExecutionStatus.FAILED -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SubtleRose.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = SubtleRose,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = action.resultMessage ?: "Action failed.",
                            color = SubtleRose,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onExecute) {
                            Text("RETRY", color = SubtleAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                ActionExecutionStatus.REJECTED -> {
                    Text(
                        text = "Action dismissed.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}
