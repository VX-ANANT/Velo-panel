package com.example.ui

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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Match
import com.example.ui.theme.*
import com.example.ui.viewmodel.BracketState
import com.example.ui.viewmodel.BracketViewModel

@Composable
fun BracketManagementScreen(
    tournamentId: String,
    viewModel: BracketViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(tournamentId) {
        viewModel.loadMatches(tournamentId)
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VelorixBg)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(VelorixAccentLight, CircleShape)
                        .border(2.dp, VelorixAccentBorder, CircleShape)
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = VelorixAccentDark,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Bracket Manager",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelorixTextPrimary
                    )
                    Text(
                        text = "TOURNAMENT ID: $tournamentId".take(30),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelorixTextSecondary,
                        letterSpacing = 1.sp
                    )
                }
            }
        },
        containerColor = VelorixBg
    ) { innerPadding ->
        when (uiState) {
            is BracketState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VelorixAccent)
                }
            }
            is BracketState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text(text = "Error: ${(uiState as BracketState.Error).message}", color = Color.Red)
                }
            }
            is BracketState.Success -> {
                val matches = (uiState as BracketState.Success).matches
                if (matches.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        Text(text = "No brackets generated for this tournament.", color = VelorixTextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Group by round
                        val grouped = matches.groupBy { it.round }.toSortedMap()
                        grouped.forEach { (round, roundMatches) ->
                            item {
                                Text(
                                    text = "Round $round",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VelorixTextPrimary,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                )
                            }
                            items(roundMatches) { match ->
                                MatchCard(
                                    match = match,
                                    onUpdateClick = { status, winner ->
                                        viewModel.updateMatchStatus(match, status, winner)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchCard(
    match: Match,
    onUpdateClick: (String, String?) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardLiveBg, RoundedCornerShape(16.dp))
            .border(1.dp, VelorixAccentDark.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .clickable { showDialog = true }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Match #${match.matchNumber}", color = VelorixTextSecondary, fontSize = 12.sp)
            Text(match.status.uppercase(), color = if(match.status == "completed") Color.Green else VelorixAccentDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Player 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(match.player1Id ?: "TBD", color = if(match.player1Id == match.winnerId && match.winnerId != null) Color.Green else VelorixTextPrimary, fontWeight = FontWeight.Bold)
            if (match.score1 != null) {
                Text(match.score1.toString(), color = VelorixTextPrimary)
            }
        }
        
        HorizontalDivider(color = VelorixTextSecondary.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))
        
        // Player 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(match.player2Id ?: "TBD", color = if(match.player2Id == match.winnerId && match.winnerId != null) Color.Green else VelorixTextPrimary, fontWeight = FontWeight.Bold)
            if (match.score2 != null) {
                Text(match.score2.toString(), color = VelorixTextPrimary)
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Update Match", color = VelorixTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Select match outcome (Admins). Advance player manually or mark as bye.", color = VelorixTextSecondary) },
            containerColor = CardLiveBg,
            titleContentColor = VelorixTextPrimary,
            textContentColor = VelorixTextSecondary,
            confirmButton = {
                Column {
                    if (match.player1Id != null) {
                        Button(
                            onClick = {
                                onUpdateClick("completed", match.player1Id)
                                showDialog = false
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent)
                        ) {
                            Text("${match.player1Id} Wins (or Bye)", color = Color.White)
                        }
                    }
                    if (match.player2Id != null) {
                        Button(
                            onClick = {
                                onUpdateClick("completed", match.player2Id)
                                showDialog = false
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent)
                        ) {
                            Text("${match.player2Id} Wins (or Bye)", color = Color.White)
                        }
                    }
                    if (match.status == "completed") {
                        Button(
                            onClick = {
                                onUpdateClick("pending", null)
                                showDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f))
                        ) {
                            Text("Reset Match", color = Color.White)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel", color = VelorixTextSecondary)
                }
            }
        )
    }
}
