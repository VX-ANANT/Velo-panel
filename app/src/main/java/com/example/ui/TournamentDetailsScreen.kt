package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.MatchProofSubmission
import com.example.domain.model.PlayerRegistration
import com.example.domain.model.Tournament
import com.example.domain.model.UserProfile
import com.example.ui.common.GlassBackgroundBox
import com.example.ui.common.GlassCard
import com.example.ui.common.GlassTokens
import com.example.ui.common.PrizeDistributionPieChart
import com.example.ui.common.bounceClick
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TournamentDetailsScreen(
    tournament: Tournament,
    currentUserProfile: UserProfile? = null,
    matchProofs: List<MatchProofSubmission> = emptyList(),
    onNavigateBack: () -> Unit,
    onEditTournament: (String) -> Unit = {},
    onEditRules: (String) -> Unit = {},
    onCancelTournament: (String, String) -> Unit = { _, _ -> },
    onDeleteTournament: (String) -> Unit = {},
    onViewBracket: (String) -> Unit = {},
    onPublishRoomCredentials: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onApproveMatchProof: (String, Double, Int) -> Unit = { _, _, _ -> },
    onRejectMatchProof: (String, String) -> Unit = { _, _ -> },
    onAddParticipant: ((String, PlayerRegistration) -> Unit)? = null
) {
    val context = LocalContext.current
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReasonInput by remember { mutableStateOf("Insufficient player registrations - Match cancelled manually.") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRoomBroadcastDialog by remember { mutableStateOf(false) }
    var showInspectProofsDialog by remember { mutableStateOf(false) }
    var showAddParticipantDialog by remember { mutableStateOf(false) }

    // Participant manual add states
    var manualPlayerIgn by remember { mutableStateOf("") }
    var manualPlayerUid by remember { mutableStateOf("") }
    var manualPlayerAge by remember { mutableStateOf("18") }
    var manualPlayerIsMinor by remember { mutableStateOf(false) }
    var isJoiningMatch by remember { mutableStateOf(false) }

    var roomIdInput by remember { mutableStateOf(tournament.roomDetails?.roomId ?: "") }
    var roomPassInput by remember { mutableStateOf(tournament.roomDetails?.roomPassword ?: "") }
    var bypass5MinCheck by remember { mutableStateOf(false) }

    val isCancelled = tournament.status.equals("CANCELLED", ignoreCase = true)
    val isLive = tournament.status.equals("LIVE", ignoreCase = true)
    val isCompleted = tournament.status.equals("COMPLETED", ignoreCase = true)

    // Calculate time diff to startsAt
    val startsAtMillis = remember(tournament.startsAt) {
        tournament.startsAt?.toLongOrNull() ?: 0L
    }
    val minutesUntilStart = remember(startsAtMillis) {
        if (startsAtMillis > 0L) {
            ((startsAtMillis - System.currentTimeMillis()) / 60000L).coerceAtLeast(0L)
        } else {
            0L
        }
    }
    val isWithin5Minutes = minutesUntilStart <= 5L

    GlassBackgroundBox(accentColor = VelorixAccent) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "TOURNAMENT ADMIN OVERSIGHT",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = tournament.title,
                                color = VelorixAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                                .bounceClick(scaleDown = 0.90f) { onNavigateBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { onEditRules(tournament.id) }) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = "Rules Manager",
                                tint = VelorixAccent
                            )
                        }
                        IconButton(onClick = { onEditTournament(tournament.id) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Tournament",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Delete Tournament",
                                tint = Color(0xFFFF5252)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent,
            bottomBar = {
                Surface(
                    color = Color(0xFF0C0C10).copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, GlassTokens.GlassBorderGradient),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCancelled) {
                        Button(
                            onClick = { },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E1D1D)),
                            shape = RoundedCornerShape(12.dp),
                            enabled = false
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFFF5252))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "MATCH CANCELLED (REFUND ISSUED)",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF8A80),
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = { showRoomBroadcastDialog = true },
                            modifier = Modifier.weight(1.3f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (tournament.roomDetails?.roomId?.isNotBlank() == true) Color(0xFF22C55E) else VelorixAccent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (tournament.roomDetails?.roomId?.isNotBlank() == true) "Room Pass" else "Set Room Pass",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }

                        OutlinedButton(
                            onClick = { onViewBracket(tournament.id) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFF27272A)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Brackets", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.weight(0.9f).height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                            border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Cancellation Notice Banner (When match is cancelled)
            if (isCancelled) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        borderBrush = Brush.horizontalGradient(listOf(Color(0xFFFF5252).copy(alpha = 0.8f), Color(0xFFB91C1C).copy(alpha = 0.4f)))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFFFF5252).copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "TOURNAMENT CANCELLED BY ADMIN",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFF8A80),
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Reason: ${tournament.cancellationReason.ifBlank { "Insufficient number of players joined before match schedule. The admin has manually cancelled the match." }}",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Color(0xFF3B1E1E),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = VelorixAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "100% Entry fees refunded to player wallet balances.",
                                        fontSize = 11.sp,
                                        color = VelorixAccentLight,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tournament Hero Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    borderBrush = GlassTokens.GlassBorderGradient
                ) {
                    Column {
                        if (tournament.bannerUrl.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            ) {
                                AsyncImage(
                                    model = tournament.bannerUrl,
                                    contentDescription = tournament.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    Color(0x660E0919),
                                                    CardVerifyBg
                                                )
                                            )
                                        )
                                    )
                            }
                        }

                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Tags: Game & Format & Map
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TagBadge(text = tournament.format, bg = VelorixAccentLight, textTint = VelorixAccentDark)
                                    TagBadge(text = tournament.map, bg = Color(0x2064B5F6), textTint = Color(0xFF64B5F6))
                                }

                                // Status Tag
                                val statusColor = when (tournament.status.uppercase()) {
                                    "LIVE" -> Color(0xFFFF5252)
                                    "COMPLETED" -> Color(0xFF81C784)
                                    "CANCELLED" -> Color(0xFFFF8A80)
                                    else -> VelorixAccent
                                }
                                Surface(
                                    color = statusColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = tournament.status.uppercase(),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        color = statusColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = tournament.title,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                lineHeight = 28.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = tournament.description.ifBlank { "Official Free Fire tournament organized by Velorix Admin Team." },
                                fontSize = 13.sp,
                                color = VelorixTextSecondary,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = CardVerifyBorder)
                            Spacer(modifier = Modifier.height(14.dp))

                            // Stats 4-Column Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatMiniItem(label = "PRIZE POOL", value = "₹${tournament.prizePool.toInt()}", valueTint = VelorixAccent)
                                StatMiniItem(label = "ENTRY FEE", value = if (tournament.entryFee > 0f) "₹${tournament.entryFee.toInt()}" else "FREE", valueTint = Color.White)
                                StatMiniItem(label = "SLOTS", value = "${tournament.registeredPlayers}/${tournament.maxPlayers}", valueTint = Color.White)
                                StatMiniItem(label = "SCHEDULE", value = tournament.startsAt?.take(10) ?: "TBA", valueTint = Color(0xFF64B5F6))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Slots Filled Progress Bar
                            val progress = if (tournament.maxPlayers > 0) (tournament.registeredPlayers.toFloat() / tournament.maxPlayers.toFloat()).coerceIn(0f, 1f) else 0f
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Slot Capacity", fontSize = 11.sp, color = VelorixTextSecondary)
                                    Text("${tournament.registeredPlayers}/${tournament.maxPlayers} (${(progress * 100).toInt()}%)", fontSize = 11.sp, color = VelorixAccent, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = VelorixAccent,
                                    trackColor = CardVerifyBorder
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // AGE COMPLIANCE & ELIGIBILITY GATEWAY
            // ==========================================
            item {
                val isMoneyMatch = (tournament.entryFee > 0f) || (tournament.prizePool > 0.0)
                val userAge = currentUserProfile?.age ?: 0
                val isUserMinor = (currentUserProfile?.isUnder18 == true) || (userAge in 1..17)
                val userAlreadyJoined = tournament.registrations.any { reg ->
                    (currentUserProfile != null && reg.playerId == currentUserProfile.id) ||
                    (currentUserProfile != null && reg.userId == currentUserProfile.id) ||
                    (currentUserProfile?.email?.isNotBlank() == true && reg.profile?.email.equals(currentUserProfile.email, ignoreCase = true))
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    borderBrush = if (isMoneyMatch && isUserMinor) {
                        Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFF59E0B)))
                    } else if (isMoneyMatch) {
                        Brush.linearGradient(listOf(Color(0xFF10B981).copy(alpha = 0.5f), Color(0xFF059669).copy(alpha = 0.3f)))
                    } else {
                        Brush.linearGradient(listOf(Color(0xFF38BDF8).copy(alpha = 0.5f), Color(0xFF0284C7).copy(alpha = 0.3f)))
                    }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isMoneyMatch && isUserMinor) Icons.Default.Lock
                                    else if (isMoneyMatch) Icons.Default.Verified
                                    else Icons.Default.SportsEsports,
                                    contentDescription = null,
                                    tint = if (isMoneyMatch && isUserMinor) Color(0xFFF87171)
                                    else if (isMoneyMatch) Color(0xFF34D399)
                                    else Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isMoneyMatch && isUserMinor) "AGE RESTRICTION NOTICE (18+)"
                                    else if (isMoneyMatch) "AGE VERIFIED (18+ ELIGIBLE)"
                                    else "TRAINING MATCH (OPEN TO ALL AGES)",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMoneyMatch && isUserMinor) Color(0xFFFCA5A5)
                                    else if (isMoneyMatch) Color(0xFF6EE7B7)
                                    else Color(0xFF7DD3FC),
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isMoneyMatch && isUserMinor) Color(0xFF7F1D1D).copy(alpha = 0.4f)
                                else if (isMoneyMatch) Color(0xFF065F46).copy(alpha = 0.4f)
                                else Color(0xFF0C4A6E).copy(alpha = 0.4f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isMoneyMatch && isUserMinor) Color(0xFFDC2626)
                                    else if (isMoneyMatch) Color(0xFF059669)
                                    else Color(0xFF0284C7)
                                )
                            ) {
                                Text(
                                    text = if (isMoneyMatch && isUserMinor) "MINOR DETECTED"
                                    else if (isMoneyMatch) "18+ VERIFIED"
                                    else "ALL AGES",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isMoneyMatch && isUserMinor) Color(0xFFFCA5A5)
                                    else if (isMoneyMatch) Color(0xFF6EE7B7)
                                    else Color(0xFF7DD3FC),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val explanationText = if (isMoneyMatch && isUserMinor) {
                            "This is a real-money tournament with a ₹${tournament.prizePool.toInt()} prize pool and ₹${tournament.entryFee.toInt()} entry fee. Legal regulations prohibit players under 18 from participating in cash contests. You are eligible to compete in all Free Training & Scrim matches."
                        } else if (isMoneyMatch) {
                            "You are verified as 18+ (Age: ${if (userAge > 0) userAge else "18+"}) and eligible to compete for the ₹${tournament.prizePool.toInt()} cash prize pool. Real-money wallet entry of ₹${tournament.entryFee.toInt()} will be processed upon joining."
                        } else {
                            "This is a Free Training & Scrim match without cash entry fees or monetary prizes. Players of all ages (including under 18) are welcome to practice, test team lineups, and compete for leaderboards."
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0B0E14),
                            border = BorderStroke(1.dp, Color(0xFF1E2530)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = explanationText,
                                    color = if (isMoneyMatch && isUserMinor) Color(0xFFFCA5A5) else Color(0xFFCBD5E1),
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF131822),
                                        border = BorderStroke(1.dp, Color(0xFF222B3D)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Match Type", color = VelorixTextSecondary, fontSize = 9.sp)
                                            Text(if (isMoneyMatch) "Cash Tournament" else "Training / Scrim", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF131822),
                                        border = BorderStroke(1.dp, Color(0xFF222B3D)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Age Rule", color = VelorixTextSecondary, fontSize = 9.sp)
                                            Text(if (isMoneyMatch) "18+ Mandatory" else "Minors Permitted", color = if (isMoneyMatch) Color(0xFFFBBF24) else Color(0xFF34D399), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (userAlreadyJoined) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF065F46).copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, Color(0xFF059669)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("YOU ARE REGISTERED IN THIS TOURNAMENT", color = Color(0xFF6EE7B7), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (isMoneyMatch && isUserMinor) {
                            Button(
                                onClick = {
                                    Toast.makeText(context, "Entry Blocked: Real-money tournaments require 18+. You can join Free Training matches.", Toast.LENGTH_LONG).show()
                                },
                                enabled = false,
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    disabledContainerColor = Color(0xFF2A1515),
                                    disabledContentColor = Color(0xFFEF4444)
                                )
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("LOCKED FOR MINORS (18+ CASH ONLY)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (currentUserProfile == null) {
                                        Toast.makeText(context, "Please sign in to register for tournaments.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val newReg = PlayerRegistration(
                                        id = "${tournament.id}_${currentUserProfile.id}",
                                        userId = currentUserProfile.id,
                                        playerId = currentUserProfile.id,
                                        playerName = currentUserProfile.username.ifBlank { currentUserProfile.ign.ifBlank { "Player" } },
                                        gameUsername = currentUserProfile.ign.ifBlank { currentUserProfile.username },
                                        gameId = currentUserProfile.gameId.ifBlank { currentUserProfile.id },
                                        age = userAge,
                                        paymentStatus = if (isMoneyMatch) "PAID" else "FREE_TRAINING",
                                        status = "confirmed",
                                        registeredAt = System.currentTimeMillis()
                                    )
                                    onAddParticipant?.invoke(tournament.id, newReg)
                                    Toast.makeText(context, "Registration submitted successfully!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isMoneyMatch) VelorixAccent else Color(0xFF38BDF8)
                                )
                            ) {
                                Icon(
                                    imageVector = if (isMoneyMatch) Icons.Default.Paid else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isMoneyMatch) "JOIN CASH TOURNAMENT (₹${tournament.entryFee.toInt()})" else "JOIN TRAINING MATCH (FREE)",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Room Credentials Management Card (ADMIN OVERSIGHT)
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    borderBrush = GlassTokens.GlassBorderGradient
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ROOM CREDENTIALS CONTROL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VelorixAccent, letterSpacing = 0.5.sp)
                            }
                            if (tournament.roomDetails != null && tournament.roomDetails?.roomId?.isNotBlank() == true) {
                                Surface(color = VelorixAccent.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                    Text("BROADCASTED TO PLAYERS", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = VelorixAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Surface(color = Color(0xFFFFA000).copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                    Text("5-MIN LOCK ARMED", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color(0xFFFFD54F), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (tournament.roomDetails != null && tournament.roomDetails?.roomId?.isNotBlank() == true) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CredentialBox(
                                    modifier = Modifier.weight(1f),
                                    label = "ROOM ID",
                                    value = tournament.roomDetails?.roomId ?: "-",
                                    onCopy = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Room ID", tournament.roomDetails?.roomId ?: ""))
                                        Toast.makeText(context, "Room ID copied!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                CredentialBox(
                                    modifier = Modifier.weight(1f),
                                    label = "PASSWORD",
                                    value = tournament.roomDetails?.roomPassword ?: "-",
                                    onCopy = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Room Password", tournament.roomDetails?.roomPassword ?: ""))
                                        Toast.makeText(context, "Password copied!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        } else {
                            Surface(
                                color = VelorixBg,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, CardVerifyBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LockClock, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            if (minutesUntilStart > 5L) "Locked: Broadcast window opens 5m before start" else "Broadcast window is OPEN",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            if (minutesUntilStart > 5L)
                                                "Admins can broadcast Room ID & Pass 5 mins prior ($minutesUntilStart min remaining). Bypass available in admin modal."
                                            else
                                                "Ready for immediate Free Fire Custom Room credential broadcast.",
                                            fontSize = 11.sp,
                                            color = VelorixTextSecondary,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showRoomBroadcastDialog = true },
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.BroadcastOnPersonal, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (tournament.roomDetails?.roomId?.isNotBlank() == true) "Update / Re-broadcast Room ID" else "Enter & Broadcast Room ID & Password",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                com.example.notification.VelorixFcmManager.dispatchMatchReminderPush(
                                    context = context,
                                    tournament = tournament,
                                    minutesLeft = 15
                                )
                                Toast.makeText(context, "15-Minute Match Reminder pushed to all registered players!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send 15-Min Match Reminder (FCM Push)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                        }
                    }
                }
            }

            // Participant Roster Management Section (ADMIN)
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    borderBrush = GlassTokens.GlassBorderGradient
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("REGISTERED PARTICIPANTS (${tournament.registeredPlayers}/${tournament.maxPlayers})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.5.sp)
                            }
                            IconButton(
                                onClick = { showAddParticipantDialog = true },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "Add Participant", tint = VelorixAccent, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Manage player registrations, assign room slots, whitelist VIPs, or remove inactive players.",
                            fontSize = 11.sp,
                            color = VelorixTextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (tournament.registrations.isEmpty()) {
                            Surface(
                                color = Color(0xFF141416),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF27272A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "No player records in roster yet.",
                                        fontSize = 12.sp,
                                        color = VelorixTextSecondary
                                    )
                                    Button(
                                        onClick = { showAddParticipantDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Add Player", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val displayRegistrations: List<PlayerRegistration> = tournament.registrations.take(10)
                                for (idx in displayRegistrations.indices) {
                                    val reg: PlayerRegistration = displayRegistrations[idx]
                                    Surface(
                                        color = Color(0xFF141416),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFF27272A)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    color = Color(0xFF27272A),
                                                    shape = CircleShape,
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text("${idx + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(reg.playerName.ifBlank { reg.gameUsername }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    Text("UID: ${reg.gameId}", fontSize = 10.sp, color = VelorixTextSecondary)
                                                }
                                            }
                                            Surface(
                                                color = if (reg.paymentStatus == "PAID") Color(0xFF166534) else Color(0xFF3F3F46),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    reg.paymentStatus,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Match Proofs & Anti-Cheat Verification Center (ADMIN ONLY)
            item {
                val pendingProofsCount = matchProofs.count { it.status.equals("pending", ignoreCase = true) }
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    borderBrush = if (pendingProofsCount > 0) Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))) else GlassTokens.GlassBorderGradient
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = if (pendingProofsCount > 0) Color(0xFFF59E0B) else VelorixAccent, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("MATCH PROOFS & RESULT CLAIMS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.5.sp)
                            }
                            if (pendingProofsCount > 0) {
                                Surface(color = Color(0xFFF59E0B), shape = RoundedCornerShape(6.dp)) {
                                    Text("$pendingProofsCount PENDING", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Inspect player scoreboard screenshots, verify kills/rank claims, and dispatch automated wallet payouts.",
                            fontSize = 11.sp,
                            color = VelorixTextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showInspectProofsDialog = true },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (pendingProofsCount > 0) Color(0xFFF59E0B) else Color(0xFF27272A)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FactCheck, contentDescription = null, tint = if (pendingProofsCount > 0) Color.Black else Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (matchProofs.isEmpty()) "Open Proofs Inspector (0 Submissions)" else "Inspect & Review Proofs (${matchProofs.size})",
                                fontWeight = FontWeight.Bold,
                                color = if (pendingProofsCount > 0) Color.Black else Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Prize Pool Distribution Graph & Visual Pie Chart
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    borderBrush = GlassTokens.GlassBorderGradient
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PieChart, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PRIZE POOL DISTRIBUTION & PIE CHART", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary, letterSpacing = 0.5.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Interactive Pie Chart
                        PrizeDistributionPieChart(
                            prizePool = tournament.prizePool,
                            firstPlacePrize = tournament.firstPlacePrize,
                            secondPlacePrize = tournament.secondPlacePrize,
                            thirdPlacePrize = tournament.thirdPlacePrize,
                            perKillPrize = tournament.perKillPrize,
                            maxPlayers = tournament.maxPlayers,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val firstPrize = if (tournament.firstPlacePrize > 0f) tournament.firstPlacePrize else tournament.prizePool * 0.50f
                        val secondPrize = if (tournament.secondPlacePrize > 0f) tournament.secondPlacePrize else tournament.prizePool * 0.25f
                        val thirdPrize = if (tournament.thirdPlacePrize > 0f) tournament.thirdPlacePrize else tournament.prizePool * 0.15f
                        val perKill = if (tournament.perKillPrize > 0f) tournament.perKillPrize else 20f

                        // Individual Prize Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PrizeRankCard(modifier = Modifier.weight(1f), rank = "1st Place", prize = "₹${firstPrize.toInt()}", color = Color(0xFFFFD700))
                            PrizeRankCard(modifier = Modifier.weight(1f), rank = "2nd Place", prize = "₹${secondPrize.toInt()}", color = Color(0xFFC0C0C0))
                            PrizeRankCard(modifier = Modifier.weight(1f), rank = "3rd Place", prize = "₹${thirdPrize.toInt()}", color = Color(0xFFCD7F32))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            color = VelorixBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CardVerifyBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.GpsFixed, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Per Kill Bounty", fontSize = 12.sp, color = VelorixTextPrimary, fontWeight = FontWeight.SemiBold)
                                }
                                Text("₹${perKill.toInt()} / kill", fontSize = 13.sp, color = VelorixAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Weapon & Gun Rules Section (ADMIN OVERSIGHT)
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    borderBrush = GlassTokens.GlassBorderGradient
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("GUNS & WEAPONS REGULATIONS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary, letterSpacing = 0.5.sp)
                            }
                            IconButton(onClick = { onEditRules(tournament.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Rules", tint = VelorixAccent, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Allowed Guns
                        RuleDetailItem(
                            icon = Icons.Default.CheckCircle,
                            iconTint = Color(0xFF81C784),
                            title = "Allowed Weapons",
                            details = tournament.allowedGuns.ifBlank { "All Standard Weapons (AR, SMG, Shotguns, Snipers)" }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Banned Guns
                        RuleDetailItem(
                            icon = Icons.Default.HighlightOff,
                            iconTint = Color(0xFFFF5252),
                            title = "Banned & Restricted Guns",
                            details = tournament.bannedGuns.ifBlank { "M79 (Launcher), M82B, Crossbow, Flash Freeze" }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Gun Attributes & Ammo Rules
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SettingPill(
                                modifier = Modifier.weight(1f),
                                label = "Gun Skin Attributes",
                                value = if (tournament.gunAttributesAllowed) "ATTRIBUTES ON" else "NO ATTRIBUTES (OFF)",
                                isAllowed = tournament.gunAttributesAllowed
                            )
                            SettingPill(
                                modifier = Modifier.weight(1f),
                                label = "Ammo Mode",
                                value = if (tournament.limitedAmmo) "LIMITED AMMO" else "UNLIMITED AMMO",
                                isAllowed = tournament.limitedAmmo
                            )
                        }
                    }
                }
            }

            // Character Skills & Gameplay Tactical Rules (ADMIN OVERSIGHT)
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    borderBrush = GlassTokens.GlassBorderGradient
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PersonOutline, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CHARACTER SKILLS & GAMEPLAY SETTINGS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary, letterSpacing = 0.5.sp)
                            }
                            IconButton(onClick = { onEditRules(tournament.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Skills", tint = VelorixAccent, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Character Skills Status
                        SettingPill(
                            modifier = Modifier.fillMaxWidth(),
                            label = "Character Skills Mode",
                            value = if (tournament.characterSkillsAllowed) "CHARACTER SKILLS ALLOWED" else "NO CHARACTER SKILLS (DISABLED)",
                            isAllowed = tournament.characterSkillsAllowed
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (tournament.characterSkillsAllowed) {
                            RuleDetailItem(
                                icon = Icons.Default.Check,
                                iconTint = Color(0xFF81C784),
                                title = "Allowed Character Skills",
                                details = tournament.allowedSkills.ifBlank { "All Standard Active & Passive (Alok, Tatsuya, Kelly, Hayato, Moco, Maxim)" }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            RuleDetailItem(
                                icon = Icons.Default.Block,
                                iconTint = Color(0xFFFF5252),
                                title = "Banned Character Skills",
                                details = tournament.bannedSkills.ifBlank { "None (Or e.g. Chrono, Dimitry, Wukong, Orion)" }
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Device & Tactical Restrictions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SettingPill(
                                modifier = Modifier.weight(1f),
                                label = "Emulator / PC Players",
                                value = if (tournament.emulatorAllowed) "EMULATOR ALLOWED" else "MOBILE ONLY (BLOCKED)",
                                isAllowed = tournament.emulatorAllowed
                            )
                            SettingPill(
                                modifier = Modifier.weight(1f),
                                label = "Rooftop Camping",
                                value = if (tournament.roofCampingAllowed) "ROOF CAMPING ON" else "ROOF CAMPING BANNED",
                                isAllowed = tournament.roofCampingAllowed
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Match Modifiers (Headshot, Revival, Fall Damage)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SettingPill(
                                modifier = Modifier.weight(1f),
                                label = "Headshot Only",
                                value = if (tournament.headshotOnly) "HEADSHOT ONLY" else "STANDARD DAMAGE",
                                isAllowed = !tournament.headshotOnly
                            )
                            SettingPill(
                                modifier = Modifier.weight(1f),
                                label = "Revival System",
                                value = if (tournament.revivalAllowed) "REVIVAL ON" else "PERMADEATH",
                                isAllowed = tournament.revivalAllowed
                            )
                        }

                        if (tournament.customMatchSettings.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            RuleDetailItem(
                                icon = Icons.Default.Tune,
                                iconTint = VelorixAccent,
                                title = "Room Modifiers & Safe Zone",
                                details = "${tournament.customMatchSettings} | Safe Zone: ${tournament.safeZoneShrinkSpeed}"
                            )
                        }
                    }
                }
            }

            // Description & Official Rules
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    borderBrush = GlassTokens.GlassBorderGradient
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("OFFICIAL RULES & REGULATIONS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary, letterSpacing = 0.5.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = tournament.rules.ifBlank {
                                "1. All players must enter the custom room with their registered Free Fire UID.\n" +
                                "2. Teaming with opponents or using third-party cheats will result in instant disqualification and permanent platform ban.\n" +
                                "3. Screenshot of the final scoreboard is mandatory for prize claim and dispute resolution.\n" +
                                "4. In case of tie, kill count will be given higher priority.\n" +
                                "5. Room ID and password are confidential. Any player inviting external unapproved players will be disqualified."
                            },
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // ==========================================
    // DIALOG: 5-MIN LOCKED ROOM ID BROADCASTER
    // ==========================================
    if (showRoomBroadcastDialog) {
        AlertDialog(
            onDismissRequest = { showRoomBroadcastDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = VelorixAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Broadcast Room ID & Pass", color = VelorixTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (minutesUntilStart > 5L && !bypass5MinCheck) {
                        Surface(
                            color = Color(0xFF3E2723),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFB74D))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("5-Minute Rule Enforced", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB74D))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Room ID & Password can only be entered 5 minutes before the match start time to prevent unauthorized room filling. ($minutesUntilStart minutes remaining).",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Admin Emergency Bypass", fontSize = 10.sp, color = VelorixTextSecondary)
                                    Switch(
                                        checked = bypass5MinCheck,
                                        onCheckedChange = { bypass5MinCheck = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = VelorixAccent)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = roomIdInput,
                        onValueChange = { roomIdInput = it },
                        label = { Text("Free Fire Custom Room ID") },
                        placeholder = { Text("e.g. 84920194") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VelorixAccent,
                            unfocusedBorderColor = CardVerifyBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = roomPassInput,
                        onValueChange = { roomPassInput = it },
                        label = { Text("Room Password") },
                        placeholder = { Text("e.g. 1234 or velorix") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VelorixAccent,
                            unfocusedBorderColor = CardVerifyBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        "Credentials will be synced in real-time to all verified participants.",
                        fontSize = 11.sp,
                        color = VelorixTextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (roomIdInput.isBlank()) {
                            Toast.makeText(context, "Please enter Room ID", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val cleanedRoomId = roomIdInput.trim()
                        val cleanedRoomPass = roomPassInput.trim()
                        onPublishRoomCredentials(cleanedRoomId, cleanedRoomPass, bypass5MinCheck)
                        com.example.notification.VelorixFcmManager.dispatchRoomDetailsPush(
                            context = context,
                            tournament = tournament,
                            roomId = cleanedRoomId,
                            roomPassword = cleanedRoomPass
                        )
                        com.example.analytics.VelorixAnalytics.logRoomCredentialsViewed(tournament.id, cleanedRoomId)
                        showRoomBroadcastDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent),
                    enabled = isWithin5Minutes || bypass5MinCheck
                ) {
                    Text("Broadcast to Players", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRoomBroadcastDialog = false }) {
                    Text("Cancel", color = VelorixTextSecondary)
                }
            },
            containerColor = CardVerifyBg
        )
    }

    // ==========================================
    // DIALOG: ADD PARTICIPANT MANUALLY (ADMIN)
    // ==========================================
    if (showAddParticipantDialog) {
        AlertDialog(
            onDismissRequest = { showAddParticipantDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = VelorixAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Player to Roster", color = VelorixTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val isMoneyMatch = (tournament.entryFee > 0f) || (tournament.prizePool > 0.0)
                    val parsedAge = manualPlayerAge.toIntOrNull() ?: 18
                    val isPlayerUnder18 = manualPlayerIsMinor || (parsedAge in 1..17)

                    Text("Manually register a VIP, offline, or invited player to this tournament.", fontSize = 12.sp, color = VelorixTextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = manualPlayerIgn,
                        onValueChange = { manualPlayerIgn = it },
                        label = { Text("In-Game Name (IGN)") },
                        placeholder = { Text("e.g. Velorix_Champion") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualPlayerUid,
                        onValueChange = { manualPlayerUid = it },
                        label = { Text("Free Fire UID") },
                        placeholder = { Text("e.g. 589302194") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualPlayerAge,
                            onValueChange = { if (it.length <= 3 && it.all { ch -> ch.isDigit() }) manualPlayerAge = it },
                            label = { Text("Player Age") },
                            placeholder = { Text("18") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                        )
                        Row(
                            modifier = Modifier
                                .weight(1.3f)
                                .clickable { manualPlayerIsMinor = !manualPlayerIsMinor },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isPlayerUnder18,
                                onCheckedChange = { manualPlayerIsMinor = it },
                                colors = CheckboxDefaults.colors(checkedColor = if (isMoneyMatch) Color(0xFFEF4444) else VelorixAccent)
                            )
                            Text("Under 18 (Minor)", fontSize = 11.sp, color = if (isMoneyMatch && isPlayerUnder18) Color(0xFFFCA5A5) else VelorixTextSecondary)
                        }
                    }

                    if (isMoneyMatch && isPlayerUnder18) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2A1515),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Minor Age Restriction: Under-18 players cannot participate in real-money matches (Prize ₹${tournament.prizePool.toInt()}). Enroll them in Training matches only.",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val isMoneyMatch = (tournament.entryFee > 0f) || (tournament.prizePool > 0.0)
                val parsedAge = manualPlayerAge.toIntOrNull() ?: 18
                val isPlayerUnder18 = manualPlayerIsMinor || (parsedAge in 1..17)
                val canSubmit = !(isMoneyMatch && isPlayerUnder18)

                Button(
                    onClick = {
                        if (manualPlayerIgn.isBlank() || manualPlayerUid.isBlank()) {
                            Toast.makeText(context, "Please enter Player IGN & UID", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (isMoneyMatch && isPlayerUnder18) {
                            Toast.makeText(context, "Registration blocked: Minors cannot join cash prize matches.", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        val newReg = PlayerRegistration(
                            playerId = "adm_${System.currentTimeMillis()}",
                            playerName = manualPlayerIgn.trim(),
                            gameUsername = manualPlayerIgn.trim(),
                            gameId = manualPlayerUid.trim(),
                            age = parsedAge,
                            paymentStatus = if (isMoneyMatch) "ADMIN_ADDED" else "FREE_TRAINING",
                            registeredAt = System.currentTimeMillis()
                        )
                        onAddParticipant?.invoke(tournament.id, newReg)
                        manualPlayerIgn = ""
                        manualPlayerUid = ""
                        manualPlayerAge = "18"
                        manualPlayerIsMinor = false
                        showAddParticipantDialog = false
                        Toast.makeText(context, "Player added to tournament roster!", Toast.LENGTH_SHORT).show()
                    },
                    enabled = canSubmit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VelorixAccent,
                        disabledContainerColor = Color(0xFF27272A),
                        disabledContentColor = Color(0xFF71717A)
                    )
                ) {
                    Text(if (isMoneyMatch && isPlayerUnder18) "Age Restricted" else "Add to Match", color = if (canSubmit) Color.Black else Color(0xFF71717A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddParticipantDialog = false }) {
                    Text("Cancel", color = VelorixTextSecondary)
                }
            },
            containerColor = CardVerifyBg
        )
    }

    // ==========================================
    // DIALOG: ADMIN PROOFS & ANTI-CHEAT INSPECTOR
    // ==========================================
    if (showInspectProofsDialog) {
        AlertDialog(
            onDismissRequest = { showInspectProofsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = VelorixAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Match Proofs & Anti-Cheat (${matchProofs.size})", color = VelorixTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                if (matchProofs.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No match screenshot submissions yet.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Players upload victory scoreboard proofs which will appear here for admin review.", color = VelorixTextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(matchProofs) { proof ->
                            Surface(
                                color = VelorixBg,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (proof.status == "approved") Color(0xFF81C784) else CardVerifyBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(proof.username.ifBlank { "Player" }, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                            Text("UID: ${proof.gameAccountId}", fontSize = 10.sp, color = VelorixTextSecondary)
                                        }
                                        Surface(
                                            color = when (proof.status) {
                                                "approved" -> Color(0xFF2E7D32)
                                                "rejected" -> Color(0xFFC62828)
                                                else -> Color(0xFFE65100)
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = proof.status.uppercase(),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(color = CardVerifyBg, shape = RoundedCornerShape(6.dp), modifier = Modifier.weight(1f)) {
                                            Column(modifier = Modifier.padding(6.dp)) {
                                                Text("Claimed Rank", fontSize = 9.sp, color = VelorixTextSecondary)
                                                Text("#${proof.claimedRank}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                                            }
                                        }
                                        Surface(color = CardVerifyBg, shape = RoundedCornerShape(6.dp), modifier = Modifier.weight(1f)) {
                                            Column(modifier = Modifier.padding(6.dp)) {
                                                Text("Claimed Kills", fontSize = 9.sp, color = VelorixTextSecondary)
                                                Text("${proof.claimedKills} Kills", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VelorixAccent)
                                            }
                                        }
                                    }

                                    if (proof.status == "pending") {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    val prizeAmt = when (proof.claimedRank) {
                                                        1 -> if (tournament.firstPlacePrize > 0f) tournament.firstPlacePrize.toDouble() else tournament.prizePool * 0.50
                                                        2 -> if (tournament.secondPlacePrize > 0f) tournament.secondPlacePrize.toDouble() else tournament.prizePool * 0.25
                                                        3 -> if (tournament.thirdPlacePrize > 0f) tournament.thirdPlacePrize.toDouble() else tournament.prizePool * 0.15
                                                        else -> 0.0
                                                    } + (proof.claimedKills * (if (tournament.perKillPrize > 0f) tournament.perKillPrize.toDouble() else 20.0))

                                                    onApproveMatchProof(proof.id, prizeAmt, proof.claimedKills)
                                                },
                                                modifier = Modifier.weight(1f).height(36.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Approve & Pay", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    onRejectMatchProof(proof.id, "Scoreboard mismatch or suspicious gameplay.")
                                                },
                                                modifier = Modifier.weight(1f).height(36.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                                                border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.6f)),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Reject", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInspectProofsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent)
                ) {
                    Text("Close Inspector", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CardVerifyBg
        )
    }

    // Manual Cancellation Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5252))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manual Match Cancellation", color = VelorixTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        "Cancel this tournament manually (e.g. if player registrations are not sufficient). This will set the tournament status to CANCELLED and broadcast the reason to all players.",
                        fontSize = 13.sp,
                        color = VelorixTextSecondary,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cancelReasonInput,
                        onValueChange = { cancelReasonInput = it },
                        label = { Text("Cancellation Reason") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF5252),
                            unfocusedBorderColor = CardVerifyBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelTournament(tournament.id, cancelReasonInput.trim().ifBlank { "Insufficient players joined. Match cancelled manually." })
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Confirm Cancel Match", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Match", color = VelorixTextSecondary)
                }
            },
            containerColor = CardVerifyBg
        )
    }

        // Delete Tournament Dialog
        var isDeleting by remember { mutableStateOf(false) }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Delete Tournament?", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Are you sure you want to permanently delete \"${tournament.title}\"? This action cannot be undone and will purge all match slots, proofs, and registrations from the database.",
                            fontSize = 13.sp,
                            color = Color(0xFFA1A1AA),
                            lineHeight = 17.sp
                        )
                        if (isDeleting) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFFEF4444),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    "Deleting tournament from database...",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFCA5A5)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (!isDeleting) {
                                isDeleting = true
                                Toast.makeText(context, "Deleting tournament...", Toast.LENGTH_SHORT).show()
                                onDeleteTournament(tournament.id)
                                showDeleteDialog = false
                            }
                        },
                        enabled = !isDeleting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Delete Permanently", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false },
                        enabled = !isDeleting
                    ) {
                        Text("Cancel", color = Color(0xFFA1A1AA))
                    }
                },
                containerColor = Color(0xFF141418),
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}

@Composable
private fun TagBadge(text: String, bg: Color, textTint: Color) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, textTint.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = textTint,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatMiniItem(label: String, value: String, valueTint: Color) {
    Column {
        Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = VelorixTextSecondary, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = valueTint)
    }
}

@Composable
private fun CredentialBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onCopy() },
        color = VelorixBg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, VelorixAccent.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = label, fontSize = 9.sp, color = VelorixTextSecondary, fontWeight = FontWeight.Bold)
                Text(text = value, fontSize = 13.sp, color = VelorixAccent, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = VelorixAccent, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun PrizeRankCard(
    modifier: Modifier = Modifier,
    rank: String,
    prize: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        color = VelorixBg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = rank, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = prize, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
    }
}

@Composable
private fun RuleDetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    details: String
) {
    Surface(
        color = VelorixBg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CardVerifyBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp).padding(top = 2.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = iconTint)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = details, fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f), lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun SettingPill(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    isAllowed: Boolean
) {
    val tint = if (isAllowed) Color(0xFF81C784) else Color(0xFFFF8A80)
    Surface(
        modifier = modifier,
        color = VelorixBg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = label, fontSize = 9.sp, color = VelorixTextSecondary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tint)
        }
    }
}
