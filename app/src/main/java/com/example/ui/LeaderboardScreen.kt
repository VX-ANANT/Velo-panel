package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.UserProfile
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
            .background(Color.Black)
            .padding(horizontal = 16.dp)
            .padding(bottom = 80.dp)
    ) {
        // Title & Mode Tabs Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text(
                text = "HALL OF FAME",
                fontSize = 10.sp,
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

            Spacer(modifier = Modifier.height(10.dp))

            // Flat underline mode selector instead of boxed FilterChips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                listOf(
                    "earnings" to "EARNINGS",
                    "wins" to "MATCH WINS",
                    "kills" to "TOTAL KILLS"
                ).forEach { (key, label) ->
                    val selected = sortBy == key
                    Column(
                        modifier = Modifier
                            .clickable { sortBy = key }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) VelorixAccent else VelorixTextSecondary,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .height(2.dp)
                                .background(if (selected) VelorixAccent else Color.Transparent)
                        )
                    }
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            // TOP 3 PODIUM SECTION (Flat horizontal columns divided by vertical hairlines, zero box-in-box)
            if (topThree.isNotEmpty()) {
                item {
                    PodiumSection(topThree = topThree, sortBy = sortBy)
                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.1f))
                }
            }

            // LEADERBOARD LIST
            if (remainingPlayers.isNotEmpty()) {
                item {
                    Text(
                        text = "CONTENDERS & CHALLENGERS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelorixTextSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                }

                itemsIndexed(remainingPlayers) { index, player ->
                    val rank = index + 4
                    LeaderboardPlayerRow(
                        rank = rank,
                        player = player,
                        sortBy = sortBy,
                        onClick = { onPlayerClick?.invoke(player) }
                    )
                }
            } else if (topThree.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No Registered Players Yet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Player stats and rankings will appear as matches conclude.",
                            color = VelorixTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PodiumSection(topThree: List<UserProfile>, sortBy: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(vertical = 14.dp)
    ) {
        Text(
            text = "TOP 3 PODIUM",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = VelorixTextSecondary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank 2 (Silver)
            if (topThree.size >= 2) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    PodiumPillar(
                        player = topThree[1],
                        rank = 2,
                        badgeColor = Color(0xFFE0E0E0),
                        glowColor = Color(0xFF9E9E9E),
                        height = 110.dp,
                        sortBy = sortBy
                    )
                }
                VerticalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
            }

            // Rank 1 (Gold)
            if (topThree.isNotEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    PodiumPillar(
                        player = topThree[0],
                        rank = 1,
                        badgeColor = Color(0xFFFFD700),
                        glowColor = Color(0xFFFFA000),
                        height = 130.dp,
                        sortBy = sortBy
                    )
                }
            }

            // Rank 3 (Bronze)
            if (topThree.size >= 3) {
                VerticalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    PodiumPillar(
                        player = topThree[2],
                        rank = 3,
                        badgeColor = Color(0xFFCD7F32),
                        glowColor = Color(0xFF8D6E63),
                        height = 100.dp,
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
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .bounceClick(scaleDown = 0.95f) { onClick?.invoke() }
    ) {
        Text(
            text = "#$rank",
            fontSize = if (rank == 1) 16.sp else 13.sp,
            fontWeight = FontWeight.Black,
            color = badgeColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = player.username.ifBlank { "Player" },
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

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

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = when (rank) {
                1 -> "CHAMPION"
                2 -> "RUNNER UP"
                else -> "3RD PLACE"
            },
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = VelorixTextSecondary,
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
fun LeaderboardPlayerRow(
    rank: Int,
    player: UserProfile,
    sortBy: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
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
                    modifier = Modifier.width(34.dp)
                )

                Column {
                    Text(
                        text = player.username.ifBlank { "Unknown Player" },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "UID: ${player.gameAccountId.ifBlank { player.id.take(8) }}  •  ${player.matchesPlayed} Matches",
                        fontSize = 11.sp,
                        color = VelorixTextSecondary
                    )
                }
            }

            // Stat value
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
        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
    }
}
