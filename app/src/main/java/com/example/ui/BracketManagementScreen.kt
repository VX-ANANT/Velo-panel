package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Match
import com.example.ui.bracket.BracketVisualizationComponent
import com.example.ui.common.GlassBackgroundBox
import com.example.ui.common.GlassCard
import com.example.ui.common.GlassTokens
import com.example.ui.common.bounceClick
import com.example.ui.theme.*
import com.example.ui.viewmodel.BracketState
import com.example.ui.viewmodel.BracketViewModel

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SportsEsports
import com.example.domain.model.Tournament
import com.example.ui.common.FrostedGlassDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BracketManagementScreen(
    tournamentId: String,
    viewModel: BracketViewModel,
    tournaments: List<Tournament> = emptyList(),
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    var activeTournamentId by remember(tournamentId, tournaments) {
        mutableStateOf(if (tournamentId.isNotBlank()) tournamentId else tournaments.firstOrNull()?.id ?: "")
    }
    val activeTournament = remember(activeTournamentId, tournaments) {
        tournaments.find { it.id == activeTournamentId }
    }
    var showTournamentSelectModal by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    var viewMode by remember { mutableStateOf("tree") } // "tree" (stage pan) or "list"

    LaunchedEffect(activeTournamentId) {
        if (activeTournamentId.isNotBlank()) {
            viewModel.loadMatches(activeTournamentId)
        }
    }

    // Modal to switch tournament directly
    if (showTournamentSelectModal) {
        FrostedGlassDialog(
            onDismissRequest = { showTournamentSelectModal = false },
            maxWidth = 520.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(VelorixAccent.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, VelorixAccent.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountTree, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Switch Tournament Bracket", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Select an active or upcoming match to inspect its bracket tree", color = Color(0xFFA1A1AA), fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = { showTournamentSelectModal = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (tournaments.isEmpty()) {
                    Text("No other tournaments found in database.", color = Color.Gray, fontSize = 12.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tournaments) { tourney ->
                            val isCurrent = tourney.id == activeTournamentId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .bounceClick(scaleDown = 0.97f) {
                                        activeTournamentId = tourney.id
                                        showTournamentSelectModal = false
                                    },
                                color = if (isCurrent) Color(0xFF1E293B) else Color(0xFF141418),
                                border = BorderStroke(1.dp, if (isCurrent) VelorixAccent else Color(0xFF27272A)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        when (tourney.status.uppercase()) {
                                                            "LIVE", "ACTIVE" -> Color(0xFF10B981)
                                                            "UPCOMING" -> Color(0xFF60A5FA)
                                                            else -> Color(0xFF94A3B8)
                                                        }.copy(alpha = 0.2f),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .border(
                                                        0.5.dp,
                                                        when (tourney.status.uppercase()) {
                                                            "LIVE", "ACTIVE" -> Color(0xFF10B981)
                                                            "UPCOMING" -> Color(0xFF60A5FA)
                                                            else -> Color(0xFF94A3B8)
                                                        },
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = tourney.status.uppercase(),
                                                    color = when (tourney.status.uppercase()) {
                                                        "LIVE", "ACTIVE" -> Color(0xFF34D399)
                                                        "UPCOMING" -> Color(0xFF93C5FD)
                                                        else -> Color.White
                                                    },
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = "${tourney.game} • ${tourney.format}",
                                                color = Color(0xFFA1A1AA),
                                                fontSize = 11.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = tourney.title,
                                            color = Color.White,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Slots: ${tourney.registeredPlayers}/${tourney.maxPlayers} • ₹${tourney.prizePool.toInt()} Pool",
                                            color = VelorixAccent,
                                            fontSize = 10.5.sp
                                        )
                                    }

                                    if (isCurrent) {
                                        Surface(
                                            shape = CircleShape,
                                            color = VelorixAccent,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Check, contentDescription = "Active", tint = Color.Black, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    GlassBackgroundBox(accentColor = VelorixAccent) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // Sleek Glass Header with Tournament Selector
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0E0E12).copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, GlassTokens.GlassBorderGradient)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Circular Glass Back Button
                                Box(
                                    modifier = Modifier
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
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = "BRACKET MANAGEMENT",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = 0.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    // Interactive Tournament Selector Chip
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .bounceClick(scaleDown = 0.94f) { showTournamentSelectModal = true },
                                        color = Color(0xFF1E222E),
                                        border = BorderStroke(0.5.dp, VelorixAccent.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = activeTournament?.title ?: "Tournament: $activeTournamentId",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = VelorixAccent,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 140.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = "Switch",
                                                tint = VelorixAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Glass View Switcher Pill (Tree vs List)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF18181B).copy(alpha = 0.75f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                            ) {
                                Row(modifier = Modifier.padding(3.dp)) {
                                    val treeSelected = viewMode == "tree"
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(
                                                if (treeSelected) Color.White.copy(alpha = 0.18f)
                                                else Color.Transparent
                                            )
                                            .clickable { viewMode = "tree" }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.AccountTree,
                                                contentDescription = "Tree View",
                                                tint = if (treeSelected) VelorixAccent else Color(0xFF94A3B8),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Tree",
                                                fontSize = 11.sp,
                                                fontWeight = if (treeSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (treeSelected) Color.White else Color(0xFF94A3B8)
                                            )
                                        }
                                    }

                                    val listSelected = viewMode == "list"
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(
                                                if (listSelected) Color.White.copy(alpha = 0.18f)
                                                else Color.Transparent
                                            )
                                            .clickable { viewMode = "list" }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.FormatListBulleted,
                                                contentDescription = "List View",
                                                tint = if (listSelected) VelorixAccent else Color(0xFF94A3B8),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "List",
                                                fontSize = 11.sp,
                                                fontWeight = if (listSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (listSelected) Color.White else Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Multi-Tournament Quick Switch Strip
                    if (tournaments.size > 1) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(tournaments) { tourney ->
                                val isSelected = tourney.id == activeTournamentId
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .bounceClick(scaleDown = 0.95f) { activeTournamentId = tourney.id },
                                    color = if (isSelected) Color(0xFF1E293B) else Color(0xFF101014),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) VelorixAccent else Color(0xFF27272A)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(
                                                    when (tourney.status.uppercase()) {
                                                        "LIVE", "ACTIVE" -> Color(0xFF10B981)
                                                        "UPCOMING" -> Color(0xFF60A5FA)
                                                        else -> Color(0xFF94A3B8)
                                                    },
                                                    CircleShape
                                                )
                                        )
                                        Text(
                                            text = tourney.title.take(18),
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFFA1A1AA)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(3.dp),
                                            color = Color.White.copy(alpha = 0.08f)
                                        ) {
                                            Text(
                                                text = "${tourney.registeredPlayers}/${tourney.maxPlayers}",
                                                fontSize = 8.5.sp,
                                                color = if (isSelected) VelorixAccent else Color(0xFF71717A),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val screenWidth = maxWidth
                val isCompact = screenWidth < 360.dp

                when (val state = uiState) {
                    is BracketState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = VelorixAccent,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    "Loading brackets & live match slots...",
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    is BracketState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                borderBrush = GlassTokens.GlassBorderGradient
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        "Bracket Synchronization Failed",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        state.message,
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 12.5.sp,
                                        lineHeight = 17.sp
                                    )
                                    Button(
                                        onClick = { viewModel.loadMatches(activeTournamentId) },
                                        colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    is BracketState.Success -> {
                        val matches = state.matches
                        if (matches.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    borderBrush = GlassTokens.GlassBorderGradient
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .background(VelorixAccent.copy(alpha = 0.12f), CircleShape)
                                                .border(1.dp, VelorixAccent.copy(alpha = 0.35f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.AccountTree,
                                                contentDescription = null,
                                                tint = VelorixAccent,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Text(
                                            "No Brackets Generated",
                                            color = Color.White,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Brackets will appear automatically once participants register and match seeding begins.",
                                            color = Color(0xFFA1A1AA),
                                            fontSize = 12.5.sp,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            val grouped = matches.groupBy { it.round }.toSortedMap()

                            if (viewMode == "tree") {
                                BracketVisualizationComponent(
                                    tournamentId = activeTournamentId,
                                    matches = matches,
                                    onAdvancePlayer = { sourceMatchId, targetMatchId, playerId, targetSlot ->
                                        viewModel.advancePlayer(
                                            tournamentId = activeTournamentId,
                                            sourceMatchId = sourceMatchId,
                                            targetMatchId = targetMatchId,
                                            playerId = playerId,
                                            targetSlot = targetSlot
                                        )
                                    },
                                    onUpdateMatchStatus = { match, newStatus, winnerId ->
                                        viewModel.updateMatchStatus(match, newStatus, winnerId)
                                    },
                                    onRecordScore = { matchId, winnerId, s1, s2 ->
                                        viewModel.recordMatchScore(activeTournamentId, matchId, winnerId, s1, s2)
                                    },
                                    onGenerateBracket = {
                                        viewModel.generateBracket(activeTournamentId)
                                    }
                                )
                            } else {
                                // Standard Grouped List View
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    item { Spacer(modifier = Modifier.height(6.dp)) }
                                    grouped.forEach { (round, roundMatches) ->
                                        item {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color(0xFF121216).copy(alpha = 0.85f),
                                                border = BorderStroke(1.dp, GlassTokens.GlassBorderGradient)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "STAGE ROUND $round",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = VelorixAccent,
                                                        letterSpacing = 0.8.sp
                                                    )
                                                    Text(
                                                        text = "${roundMatches.size} Matches Scheduled",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFFA1A1AA)
                                                    )
                                                }
                                            }
                                        }

                                        items(roundMatches) { match ->
                                            GlassMatchCard(
                                                match = match,
                                                onUpdateClick = { status, winner ->
                                                    viewModel.updateMatchStatus(match, status, winner)
                                                }
                                            )
                                        }
                                    }
                                    item {
                                        Spacer(modifier = Modifier.height(48.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Liquid Glass Match Card with specular edges, winner highlights, and score badges
 */
@Composable
fun GlassMatchCard(
    match: Match,
    onUpdateClick: (String, String?) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        borderBrush = GlassTokens.GlassBorderGradient,
        onClick = { showDialog = true }
    ) {
        // Match Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${match.matchNumber}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MATCH #${match.matchNumber}",
                    color = Color(0xFFA1A1AA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            val isCompleted = match.status == "completed"
            Text(
                text = match.status.uppercase(),
                color = if (isCompleted) Color(0xFF4ADE80) else VelorixAccent,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

        // Player 1 Row (Flat Hairline Row)
        val p1Winner = match.player1Id == match.winnerId && match.winnerId != null
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false).padding(end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (p1Winner) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = "Winner",
                        tint = Color(0xFF4ADE80),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = match.player1Id ?: "TBD (Slot 1)",
                    color = if (p1Winner) Color(0xFF4ADE80) else Color.White,
                    fontWeight = if (p1Winner) FontWeight.Black else FontWeight.Medium,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (match.score1 != null) {
                Text(
                    text = match.score1.toString(),
                    color = if (p1Winner) Color(0xFF4ADE80) else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.06f))

        // Player 2 Row (Flat Hairline Row)
        val p2Winner = match.player2Id == match.winnerId && match.winnerId != null
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false).padding(end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (p2Winner) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = "Winner",
                        tint = Color(0xFF4ADE80),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = match.player2Id ?: "TBD (Slot 2)",
                    color = if (p2Winner) Color(0xFF4ADE80) else Color.White,
                    fontWeight = if (p2Winner) FontWeight.Black else FontWeight.Medium,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (match.score2 != null) {
                Text(
                    text = match.score2.toString(),
                    color = if (p2Winner) Color(0xFF4ADE80) else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }

    // Glass Match Update Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(VelorixAccent.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Match #${match.matchNumber} Result",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Select the winning participant to advance them in the tournament bracket, or reset to pending status:",
                        color = Color(0xFFA1A1AA),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    if (match.player1Id != null) {
                        Button(
                            onClick = {
                                onUpdateClick("completed", match.player1Id)
                                showDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${match.player1Id} Wins (Advance)",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (match.player2Id != null) {
                        Button(
                            onClick = {
                                onUpdateClick("completed", match.player2Id)
                                showDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${match.player2Id} Wins (Advance)",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (match.status == "completed") {
                        Button(
                            onClick = {
                                onUpdateClick("pending", null)
                                showDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Match to Pending", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            },
            containerColor = Color(0xFF141418),
            shape = RoundedCornerShape(18.dp),
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Close", color = Color(0xFFA1A1AA), fontSize = 13.sp)
                }
            }
        )
    }
}
