package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.UserProfile
import com.example.ui.common.GlassCard
import com.example.ui.common.GlassTokens
import com.example.ui.common.bounceClick
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    players: List<UserProfile>,
    onPlayerClick: ((UserProfile) -> Unit)? = null
) {
    var sortBy by remember { mutableStateOf("earnings") } // "earnings", "wins", "kills"

    val sortedPlayers = remember(players, sortBy) {
        when (sortBy) {
            "wins" -> players.sortedByDescending { it.matchesWon }
            "kills" -> players.sortedByDescending { it.totalKills }
            else -> players.sortedByDescending { it.totalEarnings }
        }
    }

    val topThree = remember(sortedPlayers) { sortedPlayers.take(3) }
    val remainingPlayers = remember(sortedPlayers) { sortedPlayers.drop(3) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 80.dp)
    ) {
        // Title & Filters Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "HALL OF FAME",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = VelorixAccent,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Player Rankings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Filter Chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = sortBy == "earnings",
                    onClick = { sortBy = "earnings" },
                    label = { Text("Earnings", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VelorixAccent,
                        selectedLabelColor = Color.Black,
                        containerColor = CardVerifyBg,
                        labelColor = VelorixTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = sortBy == "earnings",
                        borderColor = if (sortBy == "earnings") VelorixAccent else CardVerifyBorder
                    )
                )
                FilterChip(
                    selected = sortBy == "wins",
                    onClick = { sortBy = "wins" },
                    label = { Text("Wins", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF64B5F6),
                        selectedLabelColor = Color.Black,
                        containerColor = CardVerifyBg,
                        labelColor = VelorixTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = sortBy == "wins",
                        borderColor = if (sortBy == "wins") Color(0xFF64B5F6) else CardVerifyBorder
                    )
                )
                FilterChip(
                    selected = sortBy == "kills",
                    onClick = { sortBy = "kills" },
                    label = { Text("Kills", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF8A65),
                        selectedLabelColor = Color.Black,
                        containerColor = CardVerifyBg,
                        labelColor = VelorixTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = sortBy == "kills",
                        borderColor = if (sortBy == "kills") Color(0xFFFF8A65) else CardVerifyBorder
                    )
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // TOP 3 PODIUM SECTION
            if (topThree.isNotEmpty()) {
                item {
                    PodiumSection(topThree = topThree, sortBy = sortBy)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // LEADERBOARD LIST
            if (remainingPlayers.isNotEmpty()) {
                item {
                    Text(
                        text = "CONTENDERS & CHALLENGERS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelorixTextSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                itemsIndexed(remainingPlayers) { index, player ->
                    val rank = index + 4
                    LeaderboardPlayerRow(rank = rank, player = player, sortBy = sortBy, onClick = { onPlayerClick?.invoke(player) })
                }
            } else if (topThree.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No Registered Players Yet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Player stats and rankings will appear as matches conclude.", color = VelorixTextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PodiumSection(topThree: List<UserProfile>, sortBy: String) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF231F33),
                Color(0xFF14121E)
            )
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // Rank 2 (Silver)
                if (topThree.size >= 2) {
                    PodiumPillar(
                        player = topThree[1],
                        rank = 2,
                        badgeColor = Color(0xFFE0E0E0),
                        glowColor = Color(0xFF9E9E9E),
                        height = 130.dp,
                        sortBy = sortBy
                    )
                }

                // Rank 1 (Gold)
                if (topThree.isNotEmpty()) {
                    PodiumPillar(
                        player = topThree[0],
                        rank = 1,
                        badgeColor = Color(0xFFFFD700),
                        glowColor = Color(0xFFFFA000),
                        height = 160.dp,
                        sortBy = sortBy
                    )
                }

                // Rank 3 (Bronze)
                if (topThree.size >= 3) {
                    PodiumPillar(
                        player = topThree[2],
                        rank = 3,
                        badgeColor = Color(0xFFCD7F32),
                        glowColor = Color(0xFF8D6E63),
                        height = 110.dp,
                        sortBy = sortBy
                    )
                }
            }
        }
    }
}

@Composable
fun PodiumPillar(
    player: UserProfile,
    rank: Int,
    badgeColor: Color,
    glowColor: Color,
    height: androidx.compose.ui.unit.Dp,
    sortBy: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(96.dp)
            .bounceClick(scaleDown = 0.93f) { onClick?.invoke() }
    ) {
        // Avatar with Crown / Rank
        Box(
            modifier = Modifier.size(54.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = badgeColor.copy(alpha = 0.2f),
                border = BorderStroke(2.dp, badgeColor)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = player.username.take(2).uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            // Rank Badge Pill
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 6.dp),
                shape = RoundedCornerShape(10.dp),
                color = badgeColor,
                shadowElevation = 4.dp
            ) {
                Text(
                    text = "#$rank",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = player.username.ifBlank { "Player" },
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val statDisplay = when (sortBy) {
            "wins" -> "${player.matchesWon} Wins"
            "kills" -> "${player.totalKills} Kills"
            else -> "₹${player.totalEarnings.toInt()}"
        }

        Text(
            text = statDisplay,
            fontWeight = FontWeight.ExtraBold,
            color = badgeColor,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Pillar block
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            color = glowColor.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, glowColor.copy(alpha = 0.4f))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (rank == 1) Icons.Default.EmojiEvents else Icons.Default.MilitaryTech,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (rank) {
                            1 -> "CHAMPION"
                            2 -> "RUNNER UP"
                            else -> "3RD PLACE"
                        },
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeColor,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LeaderboardPlayerRow(
    rank: Int,
    player: UserProfile,
    sortBy: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .bounceClick(scaleDown = 0.96f) { onClick() },
        color = CardVerifyBg,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CardVerifyBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Rank number
                Text(
                    text = "#$rank",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = VelorixTextSecondary,
                    modifier = Modifier.width(32.dp)
                )

                // Avatar
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = Color(0x337F56D9),
                    border = BorderStroke(1.dp, Color(0xFF9E77ED).copy(alpha = 0.5f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = player.username.take(2).uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = player.username.ifBlank { "Unknown Player" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "UID: ${player.gameAccountId.ifBlank { player.id.take(8) }}  •  ${player.matchesPlayed} Matches",
                        fontSize = 10.sp,
                        color = VelorixTextSecondary
                    )
                }
            }

            // Stat badge
            Column(horizontalAlignment = Alignment.End) {
                val primaryStat = when (sortBy) {
                    "wins" -> "${player.matchesWon} Wins"
                    "kills" -> "${player.totalKills} Kills"
                    else -> "₹${player.totalEarnings.toInt()}"
                }
                Text(
                    text = primaryStat,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = VelorixAccent
                )
                Text(
                    text = "${player.matchesWon}W / ${player.totalKills}K",
                    fontSize = 10.sp,
                    color = VelorixTextSecondary
                )
            }
        }
    }
}
