package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.Tournament
import com.example.domain.model.TournamentBannerPresets
import com.example.ui.common.GlassBackgroundBox
import com.example.ui.common.GlassCard
import com.example.ui.common.GlassTokens
import com.example.ui.common.GlobalErrorManager
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentRulesManagerScreen(
    tournaments: List<Tournament>,
    initialTournamentId: String? = null,
    onNavigateBack: () -> Unit,
    onSaveTournament: suspend (Tournament) -> Unit,
    currentUserEmail: String? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Selected tournament resolution
    var selectedId by remember {
        mutableStateOf(
            initialTournamentId
                ?: tournaments.firstOrNull()?.id
                ?: ""
        )
    }

    val currentTournament = tournaments.find { it.id == selectedId } ?: tournaments.firstOrNull()

    // Form fields state bound to selected tournament
    var bannerUrl by remember(currentTournament?.id) {
        mutableStateOf(currentTournament?.bannerUrl ?: "")
    }
    var allowedGuns by remember(currentTournament?.id) {
        mutableStateOf(currentTournament?.allowedGuns ?: "All Standard Weapons (AR, SMG, Shotguns, Snipers)")
    }
    var bannedGuns by remember(currentTournament?.id) {
        mutableStateOf(currentTournament?.bannedGuns ?: "M79 Grenade Launcher, M82B, Crossbow, Flash Freeze")
    }
    var gunAttributesAllowed by remember(currentTournament?.id) {
        mutableStateOf(currentTournament?.gunAttributesAllowed ?: false)
    }
    var limitedAmmo by remember(currentTournament?.id) {
        mutableStateOf(currentTournament?.limitedAmmo ?: true)
    }
    var characterSkillsAllowed by remember(currentTournament?.id) {
        mutableStateOf(currentTournament?.characterSkillsAllowed ?: true)
    }
    var allowedSkills by remember(currentTournament?.id) {
        mutableStateOf(currentTournament?.allowedSkills ?: "All Standard Active & Passive Skills")
    }
    var bannedSkills by remember(currentTournament?.id) {
        mutableStateOf(currentTournament?.bannedSkills ?: "Chrono, Dimitri (Or none if allowed)")
    }
    var allowedCharacters by remember(currentTournament?.id) {
        mutableStateOf(currentTournament?.allowedCharacters ?: "All Standard Characters Allowed")
    }
    var bannedCharacters by remember(currentTournament?.id) {
        mutableStateOf(currentTournament?.bannedCharacters ?: "None")
    }
    var emulatorAllowed by remember(currentTournament?.id) {
        mutableStateOf(currentTournament?.emulatorAllowed ?: false)
    }
    var roofCampingAllowed by remember(currentTournament?.id) {
        mutableStateOf(currentTournament?.roofCampingAllowed ?: false)
    }
    var airdropAllowed by remember(currentTournament?.id) {
        mutableStateOf(currentTournament?.airdropAllowed ?: true)
    }
    var vehiclesAllowed by remember(currentTournament?.id) {
        mutableStateOf(currentTournament?.vehiclesAllowed ?: true)
    }
    var loadoutAllowed by remember(currentTournament?.id) {
        mutableStateOf(currentTournament?.loadoutAllowed ?: true)
    }
    var revivalAllowed by remember(currentTournament?.id) {
        mutableStateOf(currentTournament?.revivalAllowed ?: false)
    }
    var fallDamage by remember(currentTournament?.id) {
        mutableStateOf(currentTournament?.fallDamage ?: true)
    }
    var headshotOnly by remember(currentTournament?.id) {
        mutableStateOf(currentTournament?.headshotOnly ?: false)
    }
    var safeZoneShrinkSpeed by remember(currentTournament?.id) {
        mutableStateOf(currentTournament?.safeZoneShrinkSpeed ?: "Normal")
    }
    var customMatchSettings by remember(currentTournament?.id) {
        mutableStateOf(currentTournament?.customMatchSettings ?: "HP: 200 | EP: 200 | Jump: 100% | Movement: 100% | Gloo Wall Limit: 3")
    }
    var rules by remember(currentTournament?.id) {
        mutableStateOf(currentTournament?.rules ?: "1. Players must join custom room within 10 minutes.\n2. Emulators & hacks strictly prohibited.\n3. Screenshot final scoreboard for victory verification.\n4. Dispute window is 15 minutes post-match.")
    }

    var isSaving by remember { mutableStateOf(false) }
    var showTournamentDropdown by remember { mutableStateOf(false) }
    var activePresetName by remember { mutableStateOf<String?>(null) }

    GlassBackgroundBox(accentColor = VelorixAccent) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Tournament Rules & Config",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "MANUAL ENTRY & FIRESTORE LIVE SYNC",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = VelorixAccentLight,
                                letterSpacing = 1.2.sp
                            )
                        }
                    },
                    navigationIcon = {
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(38.dp)
                                .background(Color(0x332A2045), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                .clickable { onNavigateBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (currentTournament != null) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val formattedRules = buildString {
                                        appendLine("${currentTournament.title} - OFFICIAL RULES & MATCH SETTINGS")
                                        appendLine("Game: ${currentTournament.game} | Map: ${currentTournament.map} | Format: ${currentTournament.format}")
                                        appendLine("----------------------------------------")
                                        appendLine("[WEAPON SETTINGS]")
                                        appendLine("• Allowed: $allowedGuns")
                                        appendLine("• Banned: $bannedGuns")
                                        appendLine("• Gun Attributes: ${if (gunAttributesAllowed) "ENABLED" else "DISABLED (Competitive Fair-Play)"}")
                                        appendLine("• Limited Ammo: ${if (limitedAmmo) "YES" else "NO"}")
                                        appendLine("")
                                        appendLine("[CHARACTER & SKILLS]")
                                        appendLine("• Skills Allowed: ${if (characterSkillsAllowed) "YES" else "NO (Pure Gunplay)"}")
                                        if (characterSkillsAllowed) {
                                            appendLine("• Allowed Skills: $allowedSkills")
                                            appendLine("• Banned Skills: $bannedSkills")
                                        }
                                        appendLine("")
                                        appendLine("[MATCH ENVIRONMENT]")
                                        appendLine("• Device: ${if (emulatorAllowed) "PC & Mobile Allowed" else "MOBILE ONLY (Emulators Blocked)"}")
                                        appendLine("• Roof Camping: ${if (roofCampingAllowed) "ALLOWED" else "STRICTLY FORBIDDEN"}")
                                        appendLine("• Airdrops: ${if (airdropAllowed) "ENABLED" else "DISABLED"}")
                                        appendLine("• In-Game Revival: ${if (revivalAllowed) "ENABLED" else "DISABLED"}")
                                        appendLine("• Vehicles: ${if (vehiclesAllowed) "ENABLED" else "DISABLED"}")
                                        appendLine("• Match Tuning: $customMatchSettings")
                                        appendLine("")
                                        appendLine("[OFFICIAL TOURNAMENT GUIDELINES]")
                                        appendLine(rules)
                                    }
                                    val clip = ClipData.newPlainText("Tournament Rules", formattedRules)
                                    clipboard.setPrimaryClip(clip)
                                    GlobalErrorManager.emitSuccess("Tournament rules copied to clipboard!")
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy Rules",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                // Persistent Floating Save Bar
                if (currentTournament != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xDD1B1430),
                        border = BorderStroke(1.5.dp, GlassTokens.GlassBorderActive),
                        shadowElevation = 16.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Target: ${currentTournament.title}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (currentTournament.rulesModifiedAt > 0L) {
                                        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                                        "Last synced: ${sdf.format(Date(currentTournament.rulesModifiedAt))}"
                                    } else "Live Firestore Sync Ready",
                                    fontSize = 10.sp,
                                    color = VelorixTextSecondary
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = {
                                    if (isSaving) return@Button
                                    coroutineScope.launch {
                                        isSaving = true
                                        try {
                                            val updated = currentTournament.copy(
                                                bannerUrl = bannerUrl.trim(),
                                                allowedGuns = allowedGuns.trim(),
                                                bannedGuns = bannedGuns.trim(),
                                                gunAttributesAllowed = gunAttributesAllowed,
                                                limitedAmmo = limitedAmmo,
                                                characterSkillsAllowed = characterSkillsAllowed,
                                                allowedSkills = allowedSkills.trim(),
                                                bannedSkills = bannedSkills.trim(),
                                                allowedCharacters = allowedCharacters.trim(),
                                                bannedCharacters = bannedCharacters.trim(),
                                                emulatorAllowed = emulatorAllowed,
                                                roofCampingAllowed = roofCampingAllowed,
                                                airdropAllowed = airdropAllowed,
                                                vehiclesAllowed = vehiclesAllowed,
                                                loadoutAllowed = loadoutAllowed,
                                                revivalAllowed = revivalAllowed,
                                                fallDamage = fallDamage,
                                                headshotOnly = headshotOnly,
                                                safeZoneShrinkSpeed = safeZoneShrinkSpeed,
                                                customMatchSettings = customMatchSettings.trim(),
                                                rules = rules.trim(),
                                                rulesModifiedAt = System.currentTimeMillis(),
                                                rulesModifiedBy = currentUserEmail ?: "admin@velorix.com"
                                            )
                                            onSaveTournament(updated)
                                            GlobalErrorManager.emitSuccess("Tournament rules & match settings saved to Firestore!")
                                        } catch (e: Exception) {
                                            GlobalErrorManager.emitError("Failed to save rules to Firestore", e)
                                        } finally {
                                            isSaving = false
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("save_tournament_rules_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VelorixAccent
                                ),
                                shape = RoundedCornerShape(14.dp),
                                enabled = !isSaving
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Saving...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Save",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("SAVE TO FIRESTORE", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            if (tournaments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = "No Tournaments",
                                tint = VelorixAccentLight,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Tournaments Available",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Create a tournament first from the dashboard to configure custom weapon, character, and match rules.",
                                fontSize = 12.sp,
                                color = VelorixTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                        .padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Tournament Selector Card
                    TournamentSelectorCard(
                        tournaments = tournaments,
                        currentTournament = currentTournament,
                        showDropdown = showTournamentDropdown,
                        onToggleDropdown = { showTournamentDropdown = !showTournamentDropdown },
                        onSelectTournament = { t ->
                            selectedId = t.id
                            showTournamentDropdown = false
                            activePresetName = null
                        }
                    )

                    // 1.1 Banner Image Manager Card
                    if (currentTournament != null) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderBrush = if (bannerUrl.isNotBlank()) GlassTokens.GlassBorderActive else GlassTokens.GlassBorderSubtle
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Image, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Tournament Card Banner Image", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    if (bannerUrl.isNotBlank()) {
                                        Surface(
                                            modifier = Modifier.clickable { bannerUrl = "" },
                                            color = Color(0x33FF5252),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "Clear Banner",
                                                color = Color(0xFFFF5252),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Live Card Banner Preview
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F12)),
                                    border = if (bannerUrl.isNotBlank()) BorderStroke(1.dp, VelorixAccent) else BorderStroke(1.dp, GlassTokens.GlassBorderSubtle)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (bannerUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = bannerUrl,
                                                contentDescription = "Banner Preview",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color(0x22000000), Color(0x990E0919), Color(0xF00E0919))
                                                    )
                                                )
                                        )
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = VelorixAccent.copy(alpha = 0.25f),
                                                    border = BorderStroke(0.8.dp, VelorixAccent)
                                                ) {
                                                    Text(
                                                        text = "${currentTournament.format} • ${currentTournament.map}",
                                                        color = VelorixAccentLight,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0x3369F0AE),
                                                    border = BorderStroke(0.8.dp, Color(0xFF69F0AE))
                                                ) {
                                                    Text(
                                                        text = "PRIZE: ₹${currentTournament.prizePool.toInt()}",
                                                        color = Color(0xFF69F0AE),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = currentTournament.title,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = bannerUrl,
                                    onValueChange = { bannerUrl = it },
                                    label = { Text("Banner Image Direct URL") },
                                    placeholder = { Text("https://example.com/banner.jpg") },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        if (bannerUrl.isNotBlank()) {
                                            IconButton(onClick = { bannerUrl = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = VelorixTextSecondary)
                                            }
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Preset Esports HD Banners:", fontSize = 11.sp, color = VelorixTextSecondary, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TournamentBannerPresets.PRESETS.forEach { preset ->
                                        val isSelected = bannerUrl == preset.url
                                        Surface(
                                            modifier = Modifier
                                                .width(110.dp)
                                                .height(60.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { bannerUrl = preset.url },
                                            shape = RoundedCornerShape(8.dp),
                                            border = if (isSelected) BorderStroke(2.dp, VelorixAccent) else BorderStroke(1.dp, GlassTokens.GlassBorderSubtle)
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                AsyncImage(
                                                    model = preset.url,
                                                    contentDescription = preset.title,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                                                )
                                                Text(
                                                    text = preset.title,
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .padding(4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Quick Presets Bar
                    PresetTemplatesSection(
                        activePreset = activePresetName,
                        onApplyPreset = { preset ->
                            activePresetName = preset.name
                            allowedGuns = preset.allowedGuns
                            bannedGuns = preset.bannedGuns
                            gunAttributesAllowed = preset.gunAttributesAllowed
                            limitedAmmo = preset.limitedAmmo
                            characterSkillsAllowed = preset.characterSkillsAllowed
                            allowedSkills = preset.allowedSkills
                            bannedSkills = preset.bannedSkills
                            allowedCharacters = preset.allowedCharacters
                            bannedCharacters = preset.bannedCharacters
                            emulatorAllowed = preset.emulatorAllowed
                            roofCampingAllowed = preset.roofCampingAllowed
                            airdropAllowed = preset.airdropAllowed
                            vehiclesAllowed = preset.vehiclesAllowed
                            loadoutAllowed = preset.loadoutAllowed
                            revivalAllowed = preset.revivalAllowed
                            fallDamage = preset.fallDamage
                            headshotOnly = preset.headshotOnly
                            safeZoneShrinkSpeed = preset.safeZoneShrinkSpeed
                            customMatchSettings = preset.customMatchSettings
                            rules = preset.rules
                            GlobalErrorManager.emitSuccess("Applied \"${preset.name}\" preset!")
                        }
                    )

                    // 3. Section: Weapon & Armament Rules
                    RuleSectionCard(
                        title = "Weapon & Armament Rules",
                        subtitle = "RESTRICTIONS, GUN ATTRIBUTES & AMMO",
                        icon = Icons.Default.GpsFixed,
                        accentColor = Color(0xFFEF4444)
                    ) {
                        // Allowed Weapons
                        Text(
                            text = "Allowed Weapons & Categories",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelorixTextSecondary
                        )
                        OutlinedTextField(
                            value = allowedGuns,
                            onValueChange = { allowedGuns = it },
                            placeholder = { Text("e.g. All Standard Weapons (AR, SMG, Shotguns, Snipers)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("allowed_guns_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = customFieldColors(),
                            maxLines = 3
                        )

                        // Quick Chips for Allowed Weapons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val gunPresets = listOf(
                                "+ All Weapons",
                                "+ ARs Only",
                                "+ SMGs & Shotguns",
                                "+ Snipers Only",
                                "+ Desert Eagle / Pistols",
                                "+ Melee & Fists Only"
                            )
                            gunPresets.forEach { preset ->
                                RuleQuickChip(
                                    label = preset,
                                    onClick = {
                                        val cleanText = preset.removePrefix("+ ")
                                        allowedGuns = if (allowedGuns.isBlank() || allowedGuns.contains("All Standard")) cleanText else "$allowedGuns, $cleanText"
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Banned Weapons
                        Text(
                            text = "Banned Weapons & Heavy Artillery",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF8A80)
                        )
                        OutlinedTextField(
                            value = bannedGuns,
                            onValueChange = { bannedGuns = it },
                            placeholder = { Text("e.g. M79 Grenade Launcher, M82B, Crossbow, Flash Freeze") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("banned_guns_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = customFieldColors(),
                            maxLines = 3
                        )

                        // Quick Chips for Banned Weapons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val bannedPresets = listOf(
                                "+ M79 Launcher",
                                "+ M82B Sniper",
                                "+ Crossbow",
                                "+ Flash Freeze",
                                "+ Smoke Grenades",
                                "+ Landmines",
                                "+ None (All Guns Allowed)"
                            )
                            bannedPresets.forEach { preset ->
                                RuleQuickChip(
                                    label = preset,
                                    chipColor = Color(0x33EF4444),
                                    onClick = {
                                        val cleanText = preset.removePrefix("+ ")
                                        if (cleanText.startsWith("None")) {
                                            bannedGuns = "None"
                                        } else {
                                            bannedGuns = if (bannedGuns.isBlank() || bannedGuns == "None") cleanText else "$bannedGuns, $cleanText"
                                        }
                                    }
                                )
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                        // Gun Skin Attributes Switch
                        RuleToggleRow(
                            title = "Gun Skin Attributes",
                            subtitle = if (gunAttributesAllowed) "ENABLED — Weapon skins provide damage/rate-of-fire buffs." else "OFF (Competitive Fair Play) — Weapon skins provide purely cosmetic look.",
                            checked = gunAttributesAllowed,
                            onCheckedChange = { gunAttributesAllowed = it },
                            badgeText = if (gunAttributesAllowed) "ATTRIBUTES ON" else "FAIR PLAY (OFF)",
                            badgeColor = if (gunAttributesAllowed) Color(0xFFF59E0B) else Color(0xFF10B981)
                        )

                        // Limited Ammo Switch
                        RuleToggleRow(
                            title = "Limited Ammo",
                            subtitle = if (limitedAmmo) "YES — Ammunition is finite and must be scavenged on map." else "NO — Unlimited ammunition for high-action custom matches.",
                            checked = limitedAmmo,
                            onCheckedChange = { limitedAmmo = it },
                            badgeText = if (limitedAmmo) "LIMITED AMMO" else "UNLIMITED",
                            badgeColor = if (limitedAmmo) VelorixAccent else Color(0xFF3B82F6)
                        )
                    }

                    // 4. Section: Character & Skill Rules
                    RuleSectionCard(
                        title = "Character & Ability Rules",
                        subtitle = "ACTIVE & PASSIVE SKILL CONTROLS",
                        icon = Icons.Default.Person,
                        accentColor = Color(0xFF8B5CF6)
                    ) {
                        // Master Character Skills Switch
                        RuleToggleRow(
                            title = "Character Skills Allowed",
                            subtitle = if (characterSkillsAllowed) "Active and Passive abilities are enabled." else "DISABLED (Pure Gunplay) — All skill slots deactivated.",
                            checked = characterSkillsAllowed,
                            onCheckedChange = { characterSkillsAllowed = it },
                            badgeText = if (characterSkillsAllowed) "SKILLS ON" else "NO SKILLS",
                            badgeColor = if (characterSkillsAllowed) Color(0xFF8B5CF6) else Color(0xFF6B7280)
                        )

                        AnimatedVisibility(
                            visible = characterSkillsAllowed,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Divider(color = Color.White.copy(alpha = 0.1f))

                                // Allowed Skills
                                Text(
                                    text = "Allowed Character Skills",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VelorixTextSecondary
                                )
                                OutlinedTextField(
                                    value = allowedSkills,
                                    onValueChange = { allowedSkills = it },
                                    placeholder = { Text("e.g. Alok, Tatsuya, Kelly, Hayato, Moco, Homer") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = customFieldColors(),
                                    maxLines = 2
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val skills = listOf("+ Alok", "+ Tatsuya", "+ Kelly", "+ Hayato", "+ Moco", "+ Homer", "+ Orion")
                                    skills.forEach { skill ->
                                        RuleQuickChip(
                                            label = skill,
                                            onClick = {
                                                val clean = skill.removePrefix("+ ")
                                                allowedSkills = if (allowedSkills.isBlank() || allowedSkills.contains("All Standard")) clean else "$allowedSkills, $clean"
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Banned Skills
                                Text(
                                    text = "Banned Character Skills",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF8A80)
                                )
                                OutlinedTextField(
                                    value = bannedSkills,
                                    onValueChange = { bannedSkills = it },
                                    placeholder = { Text("e.g. Chrono, Dimitri, K, Skyler (or None)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = customFieldColors(),
                                    maxLines = 2
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val banned = listOf("+ Chrono", "+ Dimitri", "+ K", "+ Skyler", "+ Steffie", "+ None (All Allowed)")
                                    banned.forEach { item ->
                                        RuleQuickChip(
                                            label = item,
                                            chipColor = Color(0x33EF4444),
                                            onClick = {
                                                val clean = item.removePrefix("+ ")
                                                if (clean.startsWith("None")) {
                                                    bannedSkills = "None"
                                                } else {
                                                    bannedSkills = if (bannedSkills.isBlank() || bannedSkills == "None") clean else "$bannedSkills, $clean"
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Section: Match & Tactical In-Game Settings
                    RuleSectionCard(
                        title = "Match & Tactical Settings",
                        subtitle = "DEVICES, MAP MECHANICS & REVIVAL",
                        icon = Icons.Default.SettingsSuggest,
                        accentColor = Color(0xFF3B82F6)
                    ) {
                        // Device / Emulator Policy
                        RuleToggleRow(
                            title = "PC / Emulator Allowed",
                            subtitle = if (emulatorAllowed) "ALLOWED — Both mobile and PC/Bluestacks players can enter." else "MOBILE ONLY — Emulators strictly prohibited and kicked.",
                            checked = emulatorAllowed,
                            onCheckedChange = { emulatorAllowed = it },
                            badgeText = if (emulatorAllowed) "PC ALLOWED" else "MOBILE ONLY",
                            badgeColor = if (emulatorAllowed) Color(0xFFF59E0B) else Color(0xFF10B981)
                        )

                        // Roof Camping
                        RuleToggleRow(
                            title = "Roof Camping Policy",
                            subtitle = if (roofCampingAllowed) "ALLOWED — Climbing high-rise building roofs permitted." else "FORBIDDEN — Roof camping is illegal and leads to disqualification.",
                            checked = roofCampingAllowed,
                            onCheckedChange = { roofCampingAllowed = it },
                            badgeText = if (roofCampingAllowed) "ROOF ON" else "NO ROOF CAMPING",
                            badgeColor = if (roofCampingAllowed) Color(0xFFF59E0B) else Color(0xFFEF4444)
                        )

                        // Airdrops
                        RuleToggleRow(
                            title = "Air Drops & Crates",
                            subtitle = if (airdropAllowed) "ENABLED — Standard and high-tier airdrops will spawn." else "DISABLED — No airdrops during the match.",
                            checked = airdropAllowed,
                            onCheckedChange = { airdropAllowed = it },
                            badgeText = if (airdropAllowed) "AIRDROP ON" else "AIRDROP OFF",
                            badgeColor = if (airdropAllowed) Color(0xFF3B82F6) else Color(0xFF6B7280)
                        )

                        // In-Game Revival
                        RuleToggleRow(
                            title = "In-Game Revival (Points & Cards)",
                            subtitle = if (revivalAllowed) "ENABLED — Fallen teammates can be revived via heart points." else "DISABLED (Competitive Esports) — Elimination is final.",
                            checked = revivalAllowed,
                            onCheckedChange = { revivalAllowed = it },
                            badgeText = if (revivalAllowed) "REVIVAL ON" else "NO REVIVAL",
                            badgeColor = if (revivalAllowed) Color(0xFF10B981) else Color(0xFF6366F1)
                        )

                        // Vehicles
                        RuleToggleRow(
                            title = "Vehicles & Cars",
                            subtitle = if (vehiclesAllowed) "ENABLED — Monster trucks, jeeps, and bikes usable." else "DISABLED — No driving vehicles allowed in match.",
                            checked = vehiclesAllowed,
                            onCheckedChange = { vehiclesAllowed = it },
                            badgeText = if (vehiclesAllowed) "VEHICLES ON" else "NO VEHICLES",
                            badgeColor = if (vehiclesAllowed) Color(0xFF10B981) else Color(0xFF6B7280)
                        )

                        // Loadout Items
                        RuleToggleRow(
                            title = "Loadouts (Bonfire / Bounty)",
                            subtitle = if (loadoutAllowed) "ENABLED — Players can equip pre-match loadout items." else "DISABLED — Pure bare spawn without loadout boosters.",
                            checked = loadoutAllowed,
                            onCheckedChange = { loadoutAllowed = it },
                            badgeText = if (loadoutAllowed) "LOADOUTS ON" else "NO LOADOUTS",
                            badgeColor = if (loadoutAllowed) Color(0xFF10B981) else Color(0xFF6B7280)
                        )

                        // Fall Damage
                        RuleToggleRow(
                            title = "Fall Damage",
                            subtitle = if (fallDamage) "YES — Falling from heights depletes health." else "NO — Fall damage is turned off.",
                            checked = fallDamage,
                            onCheckedChange = { fallDamage = it },
                            badgeText = if (fallDamage) "FALL DAMAGE ON" else "NO FALL DMG",
                            badgeColor = if (fallDamage) VelorixAccent else Color(0xFF3B82F6)
                        )

                        // Headshot Only Mode
                        RuleToggleRow(
                            title = "Headshot Only Mode",
                            subtitle = if (headshotOnly) "ENABLED — Only headshots inflict damage to opponents." else "STANDARD — Body shots and headshots both register damage.",
                            checked = headshotOnly,
                            onCheckedChange = { headshotOnly = it },
                            badgeText = if (headshotOnly) "HEADSHOT ONLY" else "STANDARD DMG",
                            badgeColor = if (headshotOnly) Color(0xFFEF4444) else Color(0xFF10B981)
                        )

                        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                        // Safe Zone Speed
                        Text(
                            text = "Safe Zone Shrink Speed",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelorixTextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val speeds = listOf("Normal", "Fast", "Hardcore Blitz")
                            speeds.forEach { speed ->
                                val isSelected = safeZoneShrinkSpeed.equals(speed, ignoreCase = true)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { safeZoneShrinkSpeed = speed },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) VelorixAccent else Color(0xFF18181B),
                                    border = BorderStroke(1.dp, if (isSelected) VelorixAccentLight else Color.White.copy(alpha = 0.15f))
                                ) {
                                    Text(
                                        text = speed,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = Color.White,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Custom Match Values
                        Text(
                            text = "Custom Room Match Parameters",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelorixTextSecondary
                        )
                        OutlinedTextField(
                            value = customMatchSettings,
                            onValueChange = { customMatchSettings = it },
                            placeholder = { Text("HP: 200 | EP: 200 | Jump: 100% | Movement: 100% | Gloo Wall Limit: 3") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = customFieldColors(),
                            singleLine = true
                        )
                    }

                    // 6. Section: Official Rulebook & Guidelines
                    RuleSectionCard(
                        title = "Official Rulebook & Guidelines",
                        subtitle = "FULL TEXT MARKDOWN & DISPUTE POLICIES",
                        icon = Icons.Default.Gavel,
                        accentColor = Color(0xFF10B981)
                    ) {
                        Text(
                            text = "Tournament Rules & Player Conduct",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelorixTextSecondary
                        )
                        OutlinedTextField(
                            value = rules,
                            onValueChange = { rules = it },
                            placeholder = { Text("Enter detailed rules, room entry deadlines, and screenshot requirements...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 140.dp)
                                .testTag("official_rules_text_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = customFieldColors()
                        )

                        Text(
                            text = "Quick Rule Snippets:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelorixTextSecondary
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val snippets = listOf(
                                "+ 5-Min Room Join" to "\n• Room ID/Pass released 5 mins before start. Players must join within 10 mins.",
                                "+ Screenshot Victory Proof" to "\n• Winners must take a full-screen scoreboard screenshot showing kills and placement.",
                                "+ Zero-Tolerance Teaming Ban" to "\n• Any teaming or cross-team cooperation results in immediate permanent ban and zero payout.",
                                "+ 15-Min Dispute Window" to "\n• Scoreboard protests must be submitted to Support Tickets within 15 minutes of match ending."
                            )
                            snippets.forEach { (label, textToAppend) ->
                                RuleQuickChip(
                                    label = label,
                                    onClick = {
                                        rules = rules.trimEnd() + textToAppend
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------
// Helper Subcomponents
// ---------------------------------------------------------

@Composable
fun TournamentSelectorCard(
    tournaments: List<Tournament>,
    currentTournament: Tournament?,
    showDropdown: Boolean,
    onToggleDropdown: () -> Unit,
    onSelectTournament: (Tournament) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        borderBrush = GlassTokens.GlassBorderGradient
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TARGET TOURNAMENT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = VelorixAccentLight,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentTournament?.title ?: "Select Tournament",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Color(0x337F56D9), RoundedCornerShape(10.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .clickable { onToggleDropdown() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "CHANGE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelorixAccentLight
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (showDropdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dropdown",
                            tint = VelorixAccentLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (currentTournament != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TournamentMiniBadge(label = currentTournament.game, icon = Icons.Default.SportsEsports)
                    TournamentMiniBadge(label = currentTournament.map, icon = Icons.Default.Map)
                    TournamentMiniBadge(label = currentTournament.format, icon = Icons.Default.People)
                    TournamentMiniBadge(
                        label = currentTournament.status,
                        icon = Icons.Default.Circle,
                        color = when (currentTournament.status.uppercase()) {
                            "LIVE" -> Color(0xFFFF5252)
                            "COMPLETED" -> Color(0xFF81C784)
                            else -> VelorixAccent
                        }
                    )
                }
            }

            // Dropdown List of all tournaments
            AnimatedVisibility(
                visible = showDropdown,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(Color(0xFF141418), RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFF27272A), RoundedCornerShape(14.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tournaments.forEach { t ->
                        val isSelected = t.id == currentTournament?.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectTournament(t) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0x447F56D9) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = t.title,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${t.game} • ${t.map} • ${t.format}",
                                        fontSize = 10.sp,
                                        color = VelorixTextSecondary
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = VelorixAccent,
                                        modifier = Modifier.size(16.dp)
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

@Composable
fun TournamentMiniBadge(label: String, icon: ImageVector, color: Color = Color.White.copy(alpha = 0.85f)) {
    Box(
        modifier = Modifier
            .background(Color(0x22FFFFFF), RoundedCornerShape(6.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

// ---------------------------------------------------------
// Rule Presets Data & Section
// ---------------------------------------------------------

data class RulePreset(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val allowedGuns: String,
    val bannedGuns: String,
    val gunAttributesAllowed: Boolean,
    val limitedAmmo: Boolean,
    val characterSkillsAllowed: Boolean,
    val allowedSkills: String,
    val bannedSkills: String,
    val allowedCharacters: String,
    val bannedCharacters: String,
    val emulatorAllowed: Boolean,
    val roofCampingAllowed: Boolean,
    val airdropAllowed: Boolean,
    val vehiclesAllowed: Boolean,
    val loadoutAllowed: Boolean,
    val revivalAllowed: Boolean,
    val fallDamage: Boolean,
    val headshotOnly: Boolean,
    val safeZoneShrinkSpeed: String,
    val customMatchSettings: String,
    val rules: String
)

val PRESET_TEMPLATES = listOf(
    RulePreset(
        name = "ESports Standard",
        description = "Competitive Fair-Play with Gun Attributes OFF & Limited Ammo",
        icon = Icons.Default.WorkspacePremium,
        color = Color(0xFF10B981),
        allowedGuns = "All Standard Weapons (AR, SMG, Shotguns, Snipers)",
        bannedGuns = "M79 Grenade Launcher, M82B, Crossbow, Flash Freeze, Smoke",
        gunAttributesAllowed = false,
        limitedAmmo = true,
        characterSkillsAllowed = true,
        allowedSkills = "Alok, Tatsuya, Kelly, Hayato, Moco, Homer",
        bannedSkills = "Chrono, Dimitri",
        allowedCharacters = "All Standard Characters",
        bannedCharacters = "None",
        emulatorAllowed = false,
        roofCampingAllowed = false,
        airdropAllowed = true,
        vehiclesAllowed = true,
        loadoutAllowed = true,
        revivalAllowed = false,
        fallDamage = true,
        headshotOnly = false,
        safeZoneShrinkSpeed = "Normal",
        customMatchSettings = "HP: 200 | EP: 200 | Jump: 100% | Movement: 100% | Gloo Wall Limit: 3",
        rules = "1. Mobile devices only. Emulators/PC are strictly prohibited.\n2. Gun skin attributes are turned OFF for competitive fairness.\n3. Take full scoreboard screenshot upon victory for score verification.\n4. Teaming or hacking will result in permanent account ban."
    ),
    RulePreset(
        name = "Clash Squad 4v4",
        description = "Fast-Paced 4v4 CS with High Movement & Restricted Heavy Guns",
        icon = Icons.Default.FlashOn,
        color = Color(0xFFF59E0B),
        allowedGuns = "Desert Eagle, MP40, UMP, M1887, M1014, SCAR, AK47",
        bannedGuns = "Grenades, Landmines, Flash Freeze, Launch Pad",
        gunAttributesAllowed = false,
        limitedAmmo = true,
        characterSkillsAllowed = true,
        allowedSkills = "Tatsuya, Alok, Kelly, Hayato",
        bannedSkills = "Chrono, Skyler, Dimitri",
        allowedCharacters = "All Standard Characters",
        bannedCharacters = "None",
        emulatorAllowed = false,
        roofCampingAllowed = false,
        airdropAllowed = false,
        vehiclesAllowed = false,
        loadoutAllowed = false,
        revivalAllowed = false,
        fallDamage = true,
        headshotOnly = false,
        safeZoneShrinkSpeed = "Fast",
        customMatchSettings = "HP: 200 | EP: 200 | Jump: 100% | Movement: 110% | Gloo Wall: Unlimited",
        rules = "1. 4v4 Clash Squad custom room mode.\n2. No grenades or tactical roof climbing.\n3. Room ID shared 5 mins before match.\n4. 15-minute dispute window after final round."
    ),
    RulePreset(
        name = "Pure Gunplay",
        description = "No Character Skills, Pure Weapon Aim & Recoil Mastery",
        icon = Icons.Default.Shield,
        color = Color(0xFF8B5CF6),
        allowedGuns = "All Standard Weapons (AR, SMG, Shotgun, Sniper)",
        bannedGuns = "M79 Grenade Launcher, Crossbow",
        gunAttributesAllowed = false,
        limitedAmmo = true,
        characterSkillsAllowed = false,
        allowedSkills = "None (All Skills Disabled)",
        bannedSkills = "All Active & Passive Skills",
        allowedCharacters = "Standard Characters (Skill Slots Empty)",
        bannedCharacters = "None",
        emulatorAllowed = false,
        roofCampingAllowed = false,
        airdropAllowed = true,
        vehiclesAllowed = true,
        loadoutAllowed = false,
        revivalAllowed = false,
        fallDamage = true,
        headshotOnly = false,
        safeZoneShrinkSpeed = "Normal",
        customMatchSettings = "HP: 200 | EP: 0 | Jump: 100% | Movement: 100%",
        rules = "1. Pure Gunplay tournament. All character abilities deactivated.\n2. Pure weapon recoil and aim decide victories.\n3. Screenshot leaderboard upon match conclusion."
    ),
    RulePreset(
        name = "Headshot Hardcore",
        description = "Only Headshots Deal Damage with Fast Blue Zone",
        icon = Icons.Default.CenterFocusStrong,
        color = Color(0xFFEF4444),
        allowedGuns = "Desert Eagle, M1887, Woodpecker, SVD, AWM, KAR98K",
        bannedGuns = "M79, Launchpad, Vehicles, Landmines",
        gunAttributesAllowed = false,
        limitedAmmo = true,
        characterSkillsAllowed = true,
        allowedSkills = "Kelly, Hayato, D-Bee, Laura",
        bannedSkills = "Chrono, Dimitri",
        allowedCharacters = "All Characters",
        bannedCharacters = "None",
        emulatorAllowed = false,
        roofCampingAllowed = false,
        airdropAllowed = false,
        vehiclesAllowed = false,
        loadoutAllowed = false,
        revivalAllowed = false,
        fallDamage = false,
        headshotOnly = true,
        safeZoneShrinkSpeed = "Hardcore Blitz",
        customMatchSettings = "HP: 500 | EP: 200 | Headshot: 100% Only",
        rules = "1. Headshots ONLY. Body shots deal 0 damage.\n2. Test your sniper and one-tap precision.\n3. Any cheat scripts will be detected and banned."
    )
)

@Composable
fun PresetTemplatesSection(
    activePreset: String?,
    onApplyPreset: (RulePreset) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "1-CLICK RULE PRESETS",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = VelorixAccentLight,
                letterSpacing = 1.sp
            )
            Text(
                text = "Auto-fills all categories",
                fontSize = 10.sp,
                color = VelorixTextSecondary
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PRESET_TEMPLATES.forEach { preset ->
                val isActive = activePreset == preset.name
                Surface(
                    modifier = Modifier
                        .width(180.dp)
                        .clickable { onApplyPreset(preset) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isActive) Color(0x447F56D9) else Color(0x221E1533),
                    border = BorderStroke(1.2.dp, if (isActive) preset.color else Color.White.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(preset.color.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = preset.icon,
                                    contentDescription = null,
                                    tint = preset.color,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = preset.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = preset.description,
                            fontSize = 9.5.sp,
                            color = VelorixTextSecondary,
                            lineHeight = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isActive) preset.color else Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isActive) "ACTIVE PRESET" else "APPLY PRESET",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isActive) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// Section & Input Widgets
// ---------------------------------------------------------

@Composable
fun RuleSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        borderBrush = GlassTokens.GlassBorderGradient
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelorixTextSecondary,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            content()
        }
    }
}

@Composable
fun RuleToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    badgeText: String,
    badgeColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .border(0.8.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.5.sp,
                color = VelorixTextSecondary,
                lineHeight = 13.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = VelorixAccent,
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = Color(0x331E1533)
            )
        )
    }
}

@Composable
fun RuleQuickChip(
    label: String,
    chipColor: Color = Color(0x337F56D9),
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(chipColor, RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
fun customFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color.White,
    unfocusedBorderColor = Color(0xFF27272A),
    focusedContainerColor = Color(0xFF121215),
    unfocusedContainerColor = Color(0xFF0F0F12)
)
