package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Tournament
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentSettingsScreen(
    tournament: Tournament,
    onNavigateBack: () -> Unit,
    onSave: suspend (Tournament) -> Unit
) {
    var title by remember { mutableStateOf(tournament.title) }
    var game by remember { mutableStateOf(tournament.game) }
    var prizePool by remember { mutableStateOf(tournament.prizePool.toString()) }
    var maxPlayers by remember { mutableStateOf(tournament.maxPlayers.toString()) }
    var startsAt by remember { mutableStateOf(tournament.startsAt ?: "") }
    var rules by remember { mutableStateOf(tournament.rules ?: "") }
    var isSaving by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

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
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = VelorixAccentDark,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Real-time Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelorixTextPrimary
                    )
                    Text(
                        text = "EDIT TOURNAMENT",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Tournament details form
            SettingsCard {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tournament Title") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VelorixAccent,
                        unfocusedBorderColor = CardVerifyBorder
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = game,
                    onValueChange = { game = it },
                    label = { Text("Game") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VelorixAccent,
                        unfocusedBorderColor = CardVerifyBorder
                    )
                )
            }
            
            SettingsCard {
                OutlinedTextField(
                    value = prizePool,
                    onValueChange = { prizePool = it },
                    label = { Text("Prize Pool ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VelorixAccent,
                        unfocusedBorderColor = CardVerifyBorder
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = maxPlayers,
                    onValueChange = { maxPlayers = it },
                    label = { Text("Max Players") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VelorixAccent,
                        unfocusedBorderColor = CardVerifyBorder
                    )
                )
            }
            
            SettingsCard {
                OutlinedTextField(
                    value = startsAt,
                    onValueChange = { startsAt = it },
                    label = { Text("Schedule (Date/Time)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. 2023-12-01T18:00:00Z") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VelorixAccent,
                        unfocusedBorderColor = CardVerifyBorder
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = rules,
                    onValueChange = { rules = it },
                    label = { Text("Participant Rules") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5,
                    placeholder = { Text("Custom rules and requirements...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VelorixAccent,
                        unfocusedBorderColor = CardVerifyBorder
                    )
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { 
                    coroutineScope.launch {
                        isSaving = true
                        val updated = tournament.copy(
                            title = title,
                            game = game,
                            prizePool = prizePool.toFloatOrNull() ?: tournament.prizePool,
                            maxPlayers = maxPlayers.toIntOrNull() ?: tournament.maxPlayers,
                            startsAt = startsAt,
                            rules = rules
                        )
                        onSave(updated)
                        isSaving = false
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent),
                shape = RoundedCornerShape(16.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SAVE CHANGES", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardVerifyBg, RoundedCornerShape(24.dp))
            .border(1.dp, CardVerifyBorder, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        content()
    }
}
