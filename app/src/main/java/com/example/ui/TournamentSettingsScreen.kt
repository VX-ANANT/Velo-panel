package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.RoomDetails
import com.example.domain.model.Tournament
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentSettingsScreen(
    tournament: Tournament,
    onNavigateBack: () -> Unit,
    onSave: suspend (Tournament) -> Unit,
    onCancelTournament: (String, String) -> Unit = { _, _ -> },
    onDeleteTournament: (String) -> Unit = {}
) {
    // Basic Details
    var title by remember { mutableStateOf(tournament.title) }
    var game by remember { mutableStateOf(tournament.game) }
    var mapName by remember { mutableStateOf(tournament.map) }
    var format by remember { mutableStateOf(tournament.format) }
    var status by remember { mutableStateOf(tournament.status) }
    var prizePool by remember { mutableStateOf(tournament.prizePool.toString()) }
    var entryFee by remember { mutableStateOf(tournament.entryFee.toString()) }
    var maxPlayers by remember { mutableStateOf(tournament.maxPlayers.toString()) }
    var startsAt by remember { mutableStateOf(tournament.startsAt ?: "") }
    var endsAt by remember { mutableStateOf(tournament.endsAt ?: "") }

    // Prize Distribution
    var firstPlacePrize by remember { mutableStateOf(if (tournament.firstPlacePrize > 0f) tournament.firstPlacePrize.toString() else (tournament.prizePool * 0.5f).toInt().toString()) }
    var secondPlacePrize by remember { mutableStateOf(if (tournament.secondPlacePrize > 0f) tournament.secondPlacePrize.toString() else (tournament.prizePool * 0.25f).toInt().toString()) }
    var thirdPlacePrize by remember { mutableStateOf(if (tournament.thirdPlacePrize > 0f) tournament.thirdPlacePrize.toString() else (tournament.prizePool * 0.15f).toInt().toString()) }
    var perKillPrize by remember { mutableStateOf(if (tournament.perKillPrize > 0f) tournament.perKillPrize.toString() else "20") }

    // Weapon & Gun Rules
    var allowedGuns by remember { mutableStateOf(tournament.allowedGuns) }
    var bannedGuns by remember { mutableStateOf(tournament.bannedGuns) }
    var gunAttributesAllowed by remember { mutableStateOf(tournament.gunAttributesAllowed) }
    var limitedAmmo by remember { mutableStateOf(tournament.limitedAmmo) }

    // Character Skills
    var characterSkillsAllowed by remember { mutableStateOf(tournament.characterSkillsAllowed) }
    var allowedSkills by remember { mutableStateOf(tournament.allowedSkills) }
    var bannedSkills by remember { mutableStateOf(tournament.bannedSkills) }

    // Device & Tactical
    var emulatorAllowed by remember { mutableStateOf(tournament.emulatorAllowed) }
    var roofCampingAllowed by remember { mutableStateOf(tournament.roofCampingAllowed) }
    var airdropAllowed by remember { mutableStateOf(tournament.airdropAllowed) }

    // Descriptions & Rules
    var description by remember { mutableStateOf(tournament.description) }
    var rules by remember { mutableStateOf(tournament.rules) }

    // Room Credentials
    var roomIdInput by remember { mutableStateOf(tournament.roomDetails?.roomId ?: "") }
    var roomPasswordInput by remember { mutableStateOf(tournament.roomDetails?.roomPassword ?: "") }

    // Cancellation
    var cancellationReason by remember { mutableStateOf(tournament.cancellationReason.ifBlank { "Insufficient number of players joined." }) }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Manage Tournament & Rules",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelorixTextPrimary
                        )
                        Text(
                            text = "LIVE CONFIGURATION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelorixTextSecondary,
                            letterSpacing = 1.sp
                        )
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(38.dp)
                            .background(VelorixAccentLight, CircleShape)
                            .border(1.5.dp, VelorixAccentBorder, CircleShape)
                            .clickable { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = VelorixAccentDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status & Lifecycle Controller
            SettingsCard {
                Text("Match Status Controller", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary, modifier = Modifier.padding(bottom = 10.dp))
                // Status Segmented Control (Vercel Minimalist Aesthetic)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF000000),
                    border = BorderStroke(1.dp, Color(0xFF27272A))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val statusOptions = listOf("UPCOMING", "LIVE", "COMPLETED", "CANCELLED")
                        statusOptions.forEach { opt ->
                            val isSelected = status.equals(opt, ignoreCase = true)
                            val activeBg = when (opt) {
                                "LIVE" -> Color(0xFF3B1219)
                                "COMPLETED" -> Color(0xFF102A1E)
                                "CANCELLED" -> Color(0xFF3B1219)
                                else -> Color(0xFF27272A)
                            }
                            val activeText = when (opt) {
                                "LIVE" -> Color(0xFFFF6B6B)
                                "COMPLETED" -> Color(0xFF4ADE80)
                                "CANCELLED" -> Color(0xFFFF6B6B)
                                else -> Color.White
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) activeBg else Color.Transparent, RoundedCornerShape(8.dp))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color(0xFF3F3F46) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { status = opt }
                                    .padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = opt,
                                    color = if (isSelected) activeText else Color(0xFFA1A1AA),
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                if (status.equals("CANCELLED", ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cancellationReason,
                        onValueChange = { cancellationReason = it },
                        label = { Text("Cancellation Reason (Shown to Players)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF5252),
                            unfocusedBorderColor = CardVerifyBorder
                        )
                    )
                }
            }

            // Tournament Basic details form
            SettingsCard {
                Text("General Match Info", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary, modifier = Modifier.padding(bottom = 12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tournament Title") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = game,
                        onValueChange = { game = it },
                        label = { Text("Game") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                    )
                    OutlinedTextField(
                        value = mapName,
                        onValueChange = { mapName = it },
                        label = { Text("Map (e.g. Bermuda)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = format,
                        onValueChange = { format = it },
                        label = { Text("Format (SOLO/DUO/SQUAD)") },
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
            }

            // Prize Distribution & Financials
            SettingsCard {
                Text("Prize Pool & Distribution", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary, modifier = Modifier.padding(bottom = 12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = prizePool,
                        onValueChange = { prizePool = it },
                        label = { Text("Total Prize Pool (₹)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                    )
                    OutlinedTextField(
                        value = entryFee,
                        onValueChange = { entryFee = it },
                        label = { Text("Entry Fee (₹)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Prize Breakdown by Rank", fontSize = 13.sp, color = VelorixAccent, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = firstPlacePrize,
                        onValueChange = { firstPlacePrize = it },
                        label = { Text("1st (₹)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                    )
                    OutlinedTextField(
                        value = secondPlacePrize,
                        onValueChange = { secondPlacePrize = it },
                        label = { Text("2nd (₹)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                    )
                    OutlinedTextField(
                        value = thirdPlacePrize,
                        onValueChange = { thirdPlacePrize = it },
                        label = { Text("3rd (₹)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = perKillPrize,
                    onValueChange = { perKillPrize = it },
                    label = { Text("Per Kill Bounty (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                )
            }

            // Weapon & Gun Rules (USER REQUIREMENT)
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gun & Weapon Regulations", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary)
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Presets
                Text("Quick Weapon Presets:", fontSize = 11.sp, color = VelorixTextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WeaponPresetChip("All Weapons", "All Standard Weapons Allowed", "M79 Launcher, Crossbow") { a, b -> allowedGuns = a; bannedGuns = b }
                    WeaponPresetChip("AR + SMG", "AR (Scar, AK, M4A1) & SMG (MP40, UMP)", "Shotguns, Snipers, M79") { a, b -> allowedGuns = a; bannedGuns = b }
                    WeaponPresetChip("Shotguns Only", "M1887, M1014, MAG-7 Only", "All ARs, SMGs, Snipers, Launchers") { a, b -> allowedGuns = a; bannedGuns = b }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = allowedGuns,
                    onValueChange = { allowedGuns = it },
                    label = { Text("Allowed Weapons") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = bannedGuns,
                    onValueChange = { bannedGuns = it },
                    label = { Text("Banned / Restricted Weapons") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF5252), unfocusedBorderColor = CardVerifyBorder)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Gun Skin Attributes Allowed", fontSize = 13.sp, color = Color.White)
                    Switch(checked = gunAttributesAllowed, onCheckedChange = { gunAttributesAllowed = it })
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Limited Ammo Enabled", fontSize = 13.sp, color = Color.White)
                    Switch(checked = limitedAmmo, onCheckedChange = { limitedAmmo = it })
                }
            }

            // Character Skills & Tactical Rules (USER REQUIREMENT)
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonOutline, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Character Skills & Devices", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary)
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Character Skills Enabled", fontSize = 13.sp, color = Color.White)
                    Switch(checked = characterSkillsAllowed, onCheckedChange = { characterSkillsAllowed = it })
                }

                if (characterSkillsAllowed) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = allowedSkills,
                        onValueChange = { allowedSkills = it },
                        label = { Text("Allowed Character Skills") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = bannedSkills,
                        onValueChange = { bannedSkills = it },
                        label = { Text("Banned Character Skills (e.g. Chrono, Dimitry)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF5252), unfocusedBorderColor = CardVerifyBorder)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Emulator / PC Players Allowed", fontSize = 13.sp, color = Color.White)
                    Switch(checked = emulatorAllowed, onCheckedChange = { emulatorAllowed = it })
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Rooftop Camping Allowed", fontSize = 13.sp, color = Color.White)
                    Switch(checked = roofCampingAllowed, onCheckedChange = { roofCampingAllowed = it })
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Airdrops & Loadouts Enabled", fontSize = 13.sp, color = Color.White)
                    Switch(checked = airdropAllowed, onCheckedChange = { airdropAllowed = it })
                }
            }

            // Description & Official Rules
            SettingsCard {
                Text("Tournament Description & Rules", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary, modifier = Modifier.padding(bottom = 12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Tournament Overview / Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = rules,
                    onValueChange = { rules = it },
                    label = { Text("Rules & Guidelines") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                )
            }
            
            // Schedule
            SettingsCard {
                Text("Schedule & Timings", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary, modifier = Modifier.padding(bottom = 12.dp))
                OutlinedTextField(
                    value = startsAt,
                    onValueChange = { startsAt = it },
                    label = { Text("Start Date/Time") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. 2026-08-20T18:00:00Z") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = endsAt,
                    onValueChange = { endsAt = it },
                    label = { Text("End Date/Time") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. 2026-08-20T21:00:00Z") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                )
            }
            
            // Room Details
            SettingsCard {
                Text("Custom Room Credentials (Instant Broadcast)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary, modifier = Modifier.padding(bottom = 12.dp))
                OutlinedTextField(
                    value = roomIdInput,
                    onValueChange = { roomIdInput = it },
                    label = { Text("Room ID") },
                    placeholder = { Text("e.g. 8492041") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = roomPasswordInput,
                    onValueChange = { roomPasswordInput = it },
                    label = { Text("Room Password") },
                    placeholder = { Text("e.g. 1234") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            // Save Changes Button
            Button(
                onClick = { 
                    coroutineScope.launch {
                        isSaving = true
                        val updatedRoomDetails = if (roomIdInput.isNotBlank() || roomPasswordInput.isNotBlank()) {
                            RoomDetails(
                                roomId = roomIdInput.trim(),
                                roomPassword = roomPasswordInput.trim(),
                                updatedAt = System.currentTimeMillis()
                            )
                        } else null

                        val updated = tournament.copy(
                            title = title.trim(),
                            game = game.trim(),
                            map = mapName.trim(),
                            format = format.trim(),
                            status = status.trim(),
                            prizePool = prizePool.toFloatOrNull() ?: tournament.prizePool,
                            entryFee = entryFee.toFloatOrNull() ?: tournament.entryFee,
                            maxPlayers = maxPlayers.toIntOrNull() ?: tournament.maxPlayers,
                            startsAt = startsAt,
                            endsAt = endsAt,
                            firstPlacePrize = firstPlacePrize.toFloatOrNull() ?: tournament.firstPlacePrize,
                            secondPlacePrize = secondPlacePrize.toFloatOrNull() ?: tournament.secondPlacePrize,
                            thirdPlacePrize = thirdPlacePrize.toFloatOrNull() ?: tournament.thirdPlacePrize,
                            perKillPrize = perKillPrize.toFloatOrNull() ?: tournament.perKillPrize,
                            allowedGuns = allowedGuns,
                            bannedGuns = bannedGuns,
                            gunAttributesAllowed = gunAttributesAllowed,
                            limitedAmmo = limitedAmmo,
                            characterSkillsAllowed = characterSkillsAllowed,
                            allowedSkills = allowedSkills,
                            bannedSkills = bannedSkills,
                            emulatorAllowed = emulatorAllowed,
                            roofCampingAllowed = roofCampingAllowed,
                            airdropAllowed = airdropAllowed,
                            description = description,
                            rules = rules,
                            cancellationReason = cancellationReason,
                            roomDetails = updatedRoomDetails
                        )
                        onSave(updated)
                        isSaving = false
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SAVE TOURNAMENT CHANGES", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 13.sp, letterSpacing = 0.5.sp)
                }
            }

            // Quick Cancel & Delete Controls in Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showCancelConfirmDialog = true },
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18181B)),
                    border = BorderStroke(1.dp, Color(0xFF3F3F46)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFFFBBF24))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cancel Match", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFFFBBF24))
                }

                Button(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18181B)),
                    border = BorderStroke(1.dp, Color(0xFF7F1D1D)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFFF87171))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Match", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFFF87171))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Cancel Tournament Dialog
    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = { Text("Cancel Tournament?", color = VelorixTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Manually cancel this tournament (e.g. if joined players are not sufficient).", fontSize = 13.sp, color = VelorixTextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = cancellationReason,
                        onValueChange = { cancellationReason = it },
                        label = { Text("Cancellation Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelTournament(tournament.id, cancellationReason)
                        showCancelConfirmDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Confirm Cancel", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = false }) { Text("Dismiss", color = VelorixTextSecondary) }
            },
            containerColor = CardVerifyBg
        )
    }

    // Delete Tournament Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Tournament?", color = VelorixTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Permanently delete \"${tournament.title}\"? This action is irreversible.", fontSize = 13.sp, color = VelorixTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTournament(tournament.id)
                        showDeleteConfirmDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Delete Permanently", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Dismiss", color = VelorixTextSecondary) }
            },
            containerColor = CardVerifyBg
        )
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0D0D0F),
        border = BorderStroke(1.dp, Color(0xFF27272A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun WeaponPresetChip(label: String, allowed: String, banned: String, onApply: (String, String) -> Unit) {
    Surface(
        modifier = Modifier.clickable { onApply(allowed, banned) },
        color = Color(0xFF18181B),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF3F3F46))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 11.sp,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}
