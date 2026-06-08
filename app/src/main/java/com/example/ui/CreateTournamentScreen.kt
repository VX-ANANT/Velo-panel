package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.domain.model.Tournament
import com.example.ui.theme.*
import java.util.UUID
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTournamentScreen(
    onNavigateBack: () -> Unit,
    onCreate: (Tournament) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var game by remember { mutableStateOf("Free Fire") }
    var status by remember { mutableStateOf("upcoming") }
    var prizePool by remember { mutableStateOf("1000") }
    var maxPlayers by remember { mutableStateOf("100") }
    var startsAt by remember { mutableStateOf("") }
    var endsAt by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Tournament", color = VelorixTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(40.dp)
                            .background(VelorixAccentLight, CircleShape)
                            .border(2.dp, VelorixAccentBorder, CircleShape)
                            .clickable { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = VelorixAccentDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VelorixBg)
            )
        },
        containerColor = VelorixBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0x1AFFB74D), RoundedCornerShape(8.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New tournaments are instantly broadcasted via Realtime events across the Velorix platform.",
                        color = Color(0xFFFFB74D),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            item {
                SettingsCard {
                    Text("Basic Information", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary, modifier = Modifier.padding(bottom = 12.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Tournament Title") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = game,
                        onValueChange = { game = it },
                        label = { Text("Game Title") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                    )
                }
            }

            item {
                SettingsCard {
                    Text("Prize & Logistics", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary, modifier = Modifier.padding(bottom = 12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = prizePool,
                            onValueChange = { prizePool = it },
                            label = { Text("Prize Pool ($)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                        )
                        OutlinedTextField(
                            value = maxPlayers,
                            onValueChange = { maxPlayers = it },
                            label = { Text("Max Slots") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    var expanded by remember { mutableStateOf(false) }
                    val statusOptions = listOf("upcoming", "live", "ended")
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = status.uppercase(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Match Status") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color(0xFF2A2A2A))
                        ) {
                            statusOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.uppercase(), color = Color.White) },
                                    onClick = {
                                        status = option
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                val context = LocalContext.current
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())

                SettingsCard {
                    Text("Schedule & Phases", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary, modifier = Modifier.padding(bottom = 12.dp))
                    Box {
                        OutlinedTextField(
                            value = startsAt,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Start Date/Time") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Pick Date and Time") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                        )
                        Box(modifier = Modifier.matchParentSize().clickable {
                            val calendar = Calendar.getInstance()
                            DatePickerDialog(context, { _, year, month, day ->
                                TimePickerDialog(context, { _, hour, minute ->
                                    calendar.set(year, month, day, hour, minute, 0)
                                    startsAt = dateFormatter.format(calendar.time)
                                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                        })
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box {
                        OutlinedTextField(
                            value = endsAt,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("End Date/Time") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Pick Date and Time") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                        )
                        Box(modifier = Modifier.matchParentSize().clickable {
                            val calendar = Calendar.getInstance()
                            DatePickerDialog(context, { _, year, month, day ->
                                TimePickerDialog(context, { _, hour, minute ->
                                    calendar.set(year, month, day, hour, minute, 0)
                                    endsAt = dateFormatter.format(calendar.time)
                                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                        })
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        isSaving = true
                        val newTournament = Tournament(
                            id = UUID.randomUUID().toString(),
                            title = title.ifEmpty { "New Tournament" },
                            game = game,
                            entryFee = 0f,
                            prizePool = prizePool.toFloatOrNull() ?: 0f,
                            maxPlayers = maxPlayers.toIntOrNull() ?: 100,
                            registeredPlayers = 0,
                            status = status,
                            startsAt = startsAt.ifEmpty { null },
                            endsAt = endsAt.ifEmpty { null }
                        )
                        onCreate(newTournament)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("CREATE TOURNAMENT", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
