package com.example.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.draw.clip
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
    BackHandler { onNavigateBack() }

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

    val v14FieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = V14TextPrimary,
        unfocusedTextColor = V14TextPrimary,
        focusedContainerColor = Color(0xFF08080B),
        unfocusedContainerColor = Color(0xFF08080B),
        focusedBorderColor = V14AccentBright,
        unfocusedBorderColor = V14Border,
        focusedLabelColor = V14AccentBright,
        unfocusedLabelColor = V14TextMuted,
        cursorColor = V14AccentBright
    )
    val v14DangerFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = V14TextPrimary,
        unfocusedTextColor = V14TextPrimary,
        focusedContainerColor = Color(0xFF08080B),
        unfocusedContainerColor = Color(0xFF08080B),
        focusedBorderColor = V14AccentPrimary,
        unfocusedBorderColor = V14Border,
        focusedLabelColor = V14AccentBright,
        unfocusedLabelColor = V14TextMuted,
        cursorColor = V14AccentPrimary
    )
    val v14SwitchColors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = V14AccentPrimary,
        checkedBorderColor = V14AccentPrimary,
        uncheckedThumbColor = V14TextSecondary,
        uncheckedTrackColor = V14CardElevated,
        uncheckedBorderColor = V14Border
    )

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(V14Bg)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(V14CardSurface)
                            .border(1.dp, V14Border, CircleShape)
                            .clickable { onNavigateBack() },
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
                    Column {
                        Text(
                            text = "Manage Tournament & Rules",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = V14TextPrimary
                        )
                        Text(
                            text = "LIVE CONFIGURATION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = V14TextMuted,
                            letterSpacing = 1.sp
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = V14Border)
            }
        },
        containerColor = V14Bg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(V14Bg)
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status & Lifecycle Controller
            SettingsCard {
                Text("Match Status Controller", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = V14TextPrimary, modifier = Modifier.padding(bottom = 10.dp))
                HorizontalDivider(thickness = 0.5.dp, color = V14Border)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusOptions = listOf("UPCOMING", "LIVE", "COMPLETED", "CANCELLED")
                    statusOptions.forEachIndexed { idx, opt ->
                        val isSelected = status.equals(opt, ignoreCase = true)
                        val activeText = when (opt) {
                            "LIVE" -> V14AccentBright
                            "COMPLETED" -> VelorixEmerald
                            "CANCELLED" -> V14AccentPrimary
                            else -> V14AccentBright
                        }
                        if (idx > 0) {
                            Box(
                                modifier = Modifier
                                    .width(0.5.dp)
                                    .height(18.dp)
                                    .background(V14Border)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { status = opt }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = opt,
                                color = if (isSelected) activeText else V14TextSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.65f)
                                    .height(if (isSelected) 2.dp else 0.dp)
                                    .background(if (isSelected) activeText else Color.Transparent)
                            )
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
                        shape = RoundedCornerShape(12.dp),
                        colors = v14DangerFieldColors
                    )
                }
            }

            // Tournament Basic details form
            SettingsCard {
                Text("General Match Info", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = V14TextPrimary, modifier = Modifier.padding(bottom = 12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tournament Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = v14FieldColors
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = game,
                        onValueChange = { game = it },
                        label = { Text("Game") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = v14FieldColors
                    )
                    OutlinedTextField(
                        value = mapName,
                        onValueChange = { mapName = it },
                        label = { Text("Map (e.g. Bermuda)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = v14FieldColors
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = format,
                        onValueChange = { format = it },
                        label = { Text("Format (SOLO/DUO/SQUAD)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = v14FieldColors
                    )
                    OutlinedTextField(
                        value = maxPlayers,
                        onValueChange = { maxPlayers = it },
                        label = { Text("Max Slots") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = v14FieldColors
                    )
                }
            }

            // Prize Distribution & Financials
            SettingsCard {
                Text("Prize Pool & Distribution", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = V14TextPrimary, modifier = Modifier.padding(bottom = 12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = prizePool,
                        onValueChange = { prizePool = it },
                        label = { Text("Total Prize Pool (₹)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = v14FieldColors
                    )
                    OutlinedTextField(
                        value = entryFee,
                        onValueChange = { entryFee = it },
                        label = { Text("Entry Fee (₹)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = v14FieldColors
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Prize Breakdown by Rank", fontSize = 13.sp, color = V14AccentBright, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = firstPlacePrize,
                        onValueChange = { firstPlacePrize = it },
                        label = { Text("1st (₹)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = v14FieldColors
                    )
                    OutlinedTextField(
                        value = secondPlacePrize,
                        onValueChange = { secondPlacePrize = it },
                        label = { Text("2nd (₹)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = v14FieldColors
                    )
                    OutlinedTextField(
                        value = thirdPlacePrize,
                        onValueChange = { thirdPlacePrize = it },
                        label = { Text("3rd (₹)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = v14FieldColors
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = perKillPrize,
                    onValueChange = { perKillPrize = it },
                    label = { Text("Per Kill Bounty (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = v14FieldColors
                )
            }

            // Weapon & Gun Rules
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = V14AccentBright, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gun & Weapon Regulations", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = V14TextPrimary)
                }
                Spacer(modifier = Modifier.height(12.dp))

                Text("Quick Weapon Presets:", fontSize = 11.sp, color = V14TextSecondary)
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
                    shape = RoundedCornerShape(12.dp),
                    colors = v14FieldColors
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = bannedGuns,
                    onValueChange = { bannedGuns = it },
                    label = { Text("Banned / Restricted Weapons") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = v14DangerFieldColors
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Gun Skin Attributes Allowed", fontSize = 13.sp, color = V14TextPrimary)
                    Switch(checked = gunAttributesAllowed, onCheckedChange = { gunAttributesAllowed = it }, colors = v14SwitchColors)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Limited Ammo Enabled", fontSize = 13.sp, color = V14TextPrimary)
                    Switch(checked = limitedAmmo, onCheckedChange = { limitedAmmo = it }, colors = v14SwitchColors)
                }
            }

            // Character Skills & Tactical Rules
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonOutline, contentDescription = null, tint = V14AccentBright, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Character Skills & Devices", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = V14TextPrimary)
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Character Skills Enabled", fontSize = 13.sp, color = V14TextPrimary)
                    Switch(checked = characterSkillsAllowed, onCheckedChange = { characterSkillsAllowed = it }, colors = v14SwitchColors)
                }

                if (characterSkillsAllowed) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = allowedSkills,
                        onValueChange = { allowedSkills = it },
                        label = { Text("Allowed Character Skills") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = v14FieldColors
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = bannedSkills,
                        onValueChange = { bannedSkills = it },
                        label = { Text("Banned Character Skills (e.g. Chrono, Dimitry)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = v14DangerFieldColors
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Emulator / PC Players Allowed", fontSize = 13.sp, color = V14TextPrimary)
                    Switch(checked = emulatorAllowed, onCheckedChange = { emulatorAllowed = it }, colors = v14SwitchColors)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Rooftop Camping Allowed", fontSize = 13.sp, color = V14TextPrimary)
                    Switch(checked = roofCampingAllowed, onCheckedChange = { roofCampingAllowed = it }, colors = v14SwitchColors)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Airdrops & Loadouts Enabled", fontSize = 13.sp, color = V14TextPrimary)
                    Switch(checked = airdropAllowed, onCheckedChange = { airdropAllowed = it }, colors = v14SwitchColors)
                }
            }

            // Description & Official Rules
            SettingsCard {
                Text("Tournament Description & Rules", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = V14TextPrimary, modifier = Modifier.padding(bottom = 12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Tournament Overview / Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = v14FieldColors
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = rules,
                    onValueChange = { rules = it },
                    label = { Text("Rules & Guidelines") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(12.dp),
                    colors = v14FieldColors
                )
            }

            // Schedule
            SettingsCard {
                Text("Schedule & Timings", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = V14TextPrimary, modifier = Modifier.padding(bottom = 12.dp))
                OutlinedTextField(
                    value = startsAt,
                    onValueChange = { startsAt = it },
                    label = { Text("Start Date/Time") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. 2026-08-20T18:00:00Z", color = V14TextMuted) },
                    shape = RoundedCornerShape(12.dp),
                    colors = v14FieldColors
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = endsAt,
                    onValueChange = { endsAt = it },
                    label = { Text("End Date/Time") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. 2026-08-20T21:00:00Z", color = V14TextMuted) },
                    shape = RoundedCornerShape(12.dp),
                    colors = v14FieldColors
                )
            }

            // Room Details
            SettingsCard {
                Text("Custom Room Credentials (Instant Broadcast)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = V14TextPrimary, modifier = Modifier.padding(bottom = 12.dp))
                OutlinedTextField(
                    value = roomIdInput,
                    onValueChange = { roomIdInput = it },
                    label = { Text("Room ID") },
                    placeholder = { Text("e.g. 8492041", color = V14TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = v14FieldColors
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = roomPasswordInput,
                    onValueChange = { roomPasswordInput = it },
                    label = { Text("Room Password") },
                    placeholder = { Text("e.g. 1234", color = V14TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = v14FieldColors
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
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = V14AccentPrimary),
                shape = RoundedCornerShape(14.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SAVE TOURNAMENT CHANGES", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp, letterSpacing = 0.5.sp)
                }
            }

            // Quick Cancel & Delete Controls in a single hairline-divided action strip
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showCancelConfirmDialog = true }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(15.dp), tint = VelorixAmber)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cancel Match", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = VelorixAmber)
                    }

                    Box(
                        modifier = Modifier
                            .width(0.5.dp)
                            .height(20.dp)
                            .background(V14Border)
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDeleteConfirmDialog = true }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp), tint = V14AccentBright)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Match", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = V14AccentBright)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Cancel Tournament Dialog
    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = { Text("Cancel Tournament?", color = V14TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Manually cancel this tournament (e.g. if joined players are not sufficient).", fontSize = 13.sp, color = V14TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = cancellationReason,
                        onValueChange = { cancellationReason = it },
                        label = { Text("Cancellation Reason") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = v14DangerFieldColors
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
                    colors = ButtonDefaults.buttonColors(containerColor = V14AccentPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm Cancel", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = false }) { Text("Dismiss", color = V14TextSecondary) }
            },
            containerColor = V14CardSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete Tournament Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Tournament?", color = V14TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Permanently delete \"${tournament.title}\"? This action is irreversible.", fontSize = 13.sp, color = V14TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTournament(tournament.id)
                        showDeleteConfirmDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = V14AccentPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete Permanently", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Dismiss", color = V14TextSecondary) }
            },
            containerColor = V14CardSurface,
            shape = RoundedCornerShape(20.dp)
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
        shape = RoundedCornerShape(18.dp),
        color = V14CardSurface,
        border = BorderStroke(1.dp, V14Border)
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
    Column(
        modifier = Modifier
            .clickable { onApply(allowed, banned) }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = V14TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(1.5.dp)
                .background(V14AccentBright.copy(alpha = 0.7f))
        )
    }
}
