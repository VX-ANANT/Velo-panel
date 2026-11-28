package com.example.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.validation.SecuritySanitizer
import com.example.domain.model.FreeFireCategories
import com.example.domain.model.Tournament
import com.example.domain.model.TournamentBannerPresets
import com.example.domain.model.TournamentCategory
import com.example.ui.common.FrostedGlassAlertDialog
import com.example.ui.theme.*
import com.example.util.ImageUploadResult
import com.example.util.TournamentImageUploader
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTournamentScreen(
    categories: List<TournamentCategory> = FreeFireCategories.DEFAULT_CATEGORIES,
    onSaveCategory: ((TournamentCategory) -> Unit)? = null,
    onNavigateBack: () -> Unit,
    onCreate: (Tournament) -> Unit,
    onCreateBatch: ((List<Tournament>) -> Unit)? = null
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    var creationMode by remember { mutableStateOf("single") } // "single" or "batch"
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCatName by remember { mutableStateOf("") }
    var newCatCode by remember { mutableStateOf("") }
    var newCatDescription by remember { mutableStateOf("") }
    var newCatMaxPlayers by remember { mutableStateOf("48") }

    val effectiveCategories = remember(categories) {
        if (categories.isNotEmpty()) categories else FreeFireCategories.DEFAULT_CATEGORIES
    }

    // Batch creation states
    var selectedBatchCategories by remember { mutableStateOf(setOf("BR", "CS")) }
    var batchMatchesPerCategory by remember { mutableIntStateOf(1) }
    var batchIntervalMinutes by remember { mutableIntStateOf(60) }
    var batchEntryFee by remember { mutableStateOf("20") }
    var batchPrizePool by remember { mutableStateOf("1000") }
    var batchPerKillPrize by remember { mutableStateOf("20") }

    // Single creation states
    var selectedTemplate by remember { mutableStateOf<String?>(null) }
    var showUrlField by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var bannerUrl by remember { mutableStateOf("") }
    var game by remember { mutableStateOf("Free Fire") }
    var category by remember { mutableStateOf("BR") }
    var mapName by remember { mutableStateOf("Bermuda") }
    var format by remember { mutableStateOf("SOLO") }
    var status by remember { mutableStateOf("UPCOMING") }
    var prizePool by remember { mutableStateOf("1000") }
    var entryFee by remember { mutableStateOf("20") }
    var firstPlacePrize by remember { mutableStateOf("500") }
    var secondPlacePrize by remember { mutableStateOf("250") }
    var thirdPlacePrize by remember { mutableStateOf("150") }
    var perKillPrize by remember { mutableStateOf("20") }
    var maxPlayers by remember { mutableStateOf("48") }
    var startsAt by remember { mutableStateOf("") }
    var endsAt by remember { mutableStateOf("") }

    // Rules & Loadout
    var allowedGuns by remember { mutableStateOf("All Standard Weapons Allowed") }
    var bannedGuns by remember { mutableStateOf("M79 Grenade Launcher, M82B, Crossbow") }
    var gunAttributesAllowed by remember { mutableStateOf(false) }
    var limitedAmmo by remember { mutableStateOf(true) }
    var characterSkillsAllowed by remember { mutableStateOf(true) }
    var allowedSkills by remember { mutableStateOf("All Active & Passive Skills") }
    var bannedSkills by remember { mutableStateOf("None") }
    var emulatorAllowed by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("Official Velorix Free Fire Competitive Tournament.") }
    var rules by remember { mutableStateOf("1. Custom room ID will be shared before match.\n2. Emulators & hacks are strictly prohibited.\n3. Send screenshot proof of victory to claim reward.") }

    var isSaving by remember { mutableStateOf(false) }
    var localBannerUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingBanner by remember { mutableStateOf(false) }
    var bannerStatusMessage by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            coroutineScope.launch {
                isUploadingBanner = true
                bannerStatusMessage = "Uploading photo..."
                try {
                    when (val result = TournamentImageUploader.uploadTournamentBannerBitmap(context, bitmap) { bannerStatusMessage = it }) {
                        is ImageUploadResult.Success -> {
                            bannerUrl = result.url
                            localBannerUri = null
                            isUploadingBanner = false
                            bannerStatusMessage = "Uploaded"
                            Toast.makeText(context, "Banner uploaded!", Toast.LENGTH_SHORT).show()
                        }
                        is ImageUploadResult.Failure -> {
                            if (result.fallbackBase64 != null) {
                                bannerUrl = result.fallbackBase64
                                localBannerUri = null
                                isUploadingBanner = false
                                bannerStatusMessage = "Attached"
                                Toast.makeText(context, "Photo attached", Toast.LENGTH_SHORT).show()
                            } else {
                                isUploadingBanner = false
                                bannerStatusMessage = null
                                Toast.makeText(context, "Upload failed: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (t: Throwable) {
                    isUploadingBanner = false
                    bannerStatusMessage = null
                    Toast.makeText(context, "Camera error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isUploadingBanner = true
                localBannerUri = uri
                bannerStatusMessage = "Uploading image..."
                try {
                    when (val result = TournamentImageUploader.uploadTournamentBanner(context, uri) { bannerStatusMessage = it }) {
                        is ImageUploadResult.Success -> {
                            bannerUrl = result.url
                            localBannerUri = null
                            isUploadingBanner = false
                            bannerStatusMessage = "Uploaded"
                            Toast.makeText(context, "Banner uploaded!", Toast.LENGTH_SHORT).show()
                        }
                        is ImageUploadResult.Failure -> {
                            if (result.fallbackBase64 != null) {
                                bannerUrl = result.fallbackBase64
                                localBannerUri = null
                                isUploadingBanner = false
                                bannerStatusMessage = "Attached"
                                Toast.makeText(context, "Banner attached", Toast.LENGTH_SHORT).show()
                            } else {
                                isUploadingBanner = false
                                bannerStatusMessage = null
                                Toast.makeText(context, "Upload error: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (t: Throwable) {
                    isUploadingBanner = false
                    bannerStatusMessage = null
                    Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun buildSingleTournament(): Tournament {
        val pPool = prizePool.toFloatOrNull() ?: 1000f
        val cleanTitle = SecuritySanitizer.sanitizeInput(title.ifBlank { "Free Fire Championship" }, maxLength = 100)
        val cleanGame = SecuritySanitizer.sanitizeInput(game.ifBlank { "Free Fire" }, maxLength = 50)
        val cleanMap = SecuritySanitizer.sanitizeInput(mapName.ifBlank { "Bermuda" }, maxLength = 50)
        val cleanFormat = SecuritySanitizer.sanitizeInput(format.ifBlank { "SOLO" }, maxLength = 20)
        val cleanDesc = SecuritySanitizer.sanitizeInput(description, maxLength = 1000)
        val cleanRules = SecuritySanitizer.sanitizeInput(rules, maxLength = 3000)

        val cleanCategory = when {
            category.equals("CS", true) || category.contains("CLASH", true) || cleanFormat.contains("CS", true) -> "CS"
            category.equals("LONE_WOLF", true) || category.contains("LONE", true) || category.contains("1v1", true) || cleanFormat.contains("1v1", true) -> "LONE_WOLF"
            category.equals("SCRIMS", true) || category.contains("SCRIM", true) -> "SCRIMS"
            else -> category.trim().uppercase().ifBlank { "BR" }
        }
        val finalBanner = when {
            bannerUrl.isNotBlank() && (bannerUrl.startsWith("http://") || bannerUrl.startsWith("https://") || bannerUrl.startsWith("data:image/")) -> bannerUrl.trim()
            localBannerUri != null -> TournamentImageUploader.encodeUriToBase64(context, localBannerUri!!) ?: TournamentBannerPresets.PRESETS[0].url
            bannerUrl.isNotBlank() -> bannerUrl.trim()
            else -> TournamentBannerPresets.PRESETS[0].url
        }
        val defaultSchedule = dateFormatter.format(Date(System.currentTimeMillis() + 3600_000L))
        val finalSchedule = startsAt.ifBlank { defaultSchedule }

        var p1 = firstPlacePrize.toFloatOrNull() ?: (pPool * 0.50f)
        var p2 = secondPlacePrize.toFloatOrNull() ?: (pPool * 0.25f)
        var p3 = thirdPlacePrize.toFloatOrNull() ?: (pPool * 0.15f)
        if (pPool <= 0f) {
            p1 = 0f; p2 = 0f; p3 = 0f
        } else if (p1 + p2 + p3 > pPool) {
            val ratio = pPool / (p1 + p2 + p3)
            p1 = (p1 * ratio).toInt().toFloat()
            p2 = (p2 * ratio).toInt().toFloat()
            p3 = (p3 * ratio).toInt().toFloat()
        }

        return Tournament(
            id = SecuritySanitizer.sanitizeDatabaseKey(UUID.randomUUID().toString()),
            title = cleanTitle,
            bannerUrl = finalBanner,
            game = cleanGame,
            category = cleanCategory,
            map = cleanMap,
            format = cleanFormat,
            status = status,
            entryFee = entryFee.toFloatOrNull() ?: 0f,
            prizePool = pPool,
            firstPlacePrize = p1,
            secondPlacePrize = p2,
            thirdPlacePrize = p3,
            perKillPrize = perKillPrize.toFloatOrNull() ?: 0f,
            maxPlayers = maxPlayers.toIntOrNull() ?: (if (cleanCategory == "CS") 8 else if (cleanCategory == "LONE_WOLF") 2 else 48),
            registeredPlayers = 0,
            startsAt = finalSchedule,
            endsAt = endsAt.ifBlank { null },
            allowedGuns = allowedGuns,
            bannedGuns = bannedGuns,
            gunAttributesAllowed = gunAttributesAllowed,
            limitedAmmo = limitedAmmo,
            characterSkillsAllowed = characterSkillsAllowed,
            allowedSkills = allowedSkills,
            bannedSkills = bannedSkills,
            emulatorAllowed = emulatorAllowed,
            description = cleanDesc,
            rules = cleanRules
        )
    }

    fun publishBatchTournaments() {
        isSaving = true
        val generatedList = mutableListOf<Tournament>()
        var matchIndex = 0
        val fee = batchEntryFee.toFloatOrNull() ?: 20f
        val pool = batchPrizePool.toFloatOrNull() ?: 1000f
        val pKill = batchPerKillPrize.toFloatOrNull() ?: 20f

        selectedBatchCategories.forEach { catKey ->
            val catMeta = effectiveCategories.firstOrNull { it.id == catKey }
            val catName = catMeta?.name ?: catKey
            val defMap = catMeta?.defaultMaps?.firstOrNull() ?: "Bermuda"
            val defFormat = when (catKey.uppercase()) {
                "CS" -> "4v4"
                "LONE_WOLF" -> "1v1"
                else -> "SOLO"
            }
            val defMaxPlayers = catMeta?.defaultMaxPlayers ?: (if (catKey == "CS") 8 else if (catKey == "LONE_WOLF") 2 else 48)

            for (m in 1..batchMatchesPerCategory) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MINUTE, 60 + (matchIndex * batchIntervalMinutes))
                val schedTime = dateFormatter.format(cal.time)
                val presetBanner = TournamentBannerPresets.PRESETS.getOrNull(matchIndex % TournamentBannerPresets.PRESETS.size)?.url
                    ?: TournamentBannerPresets.PRESETS[0].url

                generatedList.add(
                    Tournament(
                        id = SecuritySanitizer.sanitizeDatabaseKey(UUID.randomUUID().toString()),
                        title = "Free Fire $catName Match ${if (batchMatchesPerCategory > 1) "#$m" else ""}".trim(),
                        bannerUrl = presetBanner,
                        game = "Free Fire",
                        category = catKey,
                        map = defMap,
                        format = defFormat,
                        status = "UPCOMING",
                        entryFee = fee,
                        prizePool = pool,
                        firstPlacePrize = pool * 0.5f,
                        secondPlacePrize = pool * 0.25f,
                        thirdPlacePrize = pool * 0.15f,
                        perKillPrize = pKill,
                        maxPlayers = defMaxPlayers,
                        registeredPlayers = 0,
                        startsAt = schedTime,
                        allowedGuns = "All Standard Weapons Allowed",
                        bannedGuns = "M79 Grenade Launcher, M82B, Crossbow",
                        rules = "1. Room ID shared 10 mins before match.\n2. Emulators strictly prohibited.\n3. Submit victory screenshot.",
                        description = "Official Velorix Free Fire tournament in category: $catName."
                    )
                )
                matchIndex++
            }
        }

        if (onCreateBatch != null) onCreateBatch(generatedList) else generatedList.forEach { onCreate(it) }
        Toast.makeText(context, "Published ${generatedList.size} matches to Firebase!", Toast.LENGTH_LONG).show()
        onNavigateBack()
    }

    val totalBatchMatches = selectedBatchCategories.size * batchMatchesPerCategory

    Scaffold(
        containerColor = V14Bg,
        contentWindowInsets = WindowInsets.safeDrawing,
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
                        .height(60.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(V14CardSurface)
                                .border(0.75.dp, V14Border, CircleShape)
                                .testTag("create_tournament_back_button")
                                .v14PressScale(onClick = onNavigateBack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = V14TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "New Tournament",
                            color = V14TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            letterSpacing = (-0.4).sp
                        )
                    }

                    // V14 Live Summary Capsule
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(V14CardSurface)
                            .border(0.75.dp, V14Border, RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (creationMode == "batch") "Batch · $totalBatchMatches" else "$format · ₹${prizePool.ifBlank { "0" }}",
                            color = V14AccentBright,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = V14Border)

                // Clean Underline Mode Switcher (AGENTS.md SPEC)
                Row(modifier = Modifier.fillMaxWidth()) {
                    V14ModeUnderlineTab(
                        label = "Single Match",
                        selected = creationMode == "single",
                        modifier = Modifier.weight(1f).testTag("create_mode_single"),
                        onClick = { creationMode = "single" }
                    )
                    V14ModeUnderlineTab(
                        label = "Batch Scheduler",
                        selected = creationMode == "batch",
                        modifier = Modifier.weight(1f).testTag("create_mode_batch"),
                        onClick = { creationMode = "batch" }
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = V14Border)
            }
        },
        bottomBar = {
            // V14 Dual Pill Publish Dock (Matches Home V14DualPillCta)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(V14Bg)
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(thickness = 0.5.dp, color = V14Border)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (creationMode == "batch") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(
                                    if (!isSaving) Brush.horizontalGradient(listOf(V14AccentPrimary, V14AccentBright))
                                    else Brush.horizontalGradient(listOf(V14BarMuted, V14BarMuted))
                                )
                                .testTag("create_batch_publish_button")
                                .v14PressScale { if (!isSaving) publishBatchTournaments() },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Publish All $totalBatchMatches Matches",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        val canSubmit = !isSaving && !isUploadingBanner
                        // Secondary V14 Pill: Save & Next
                        Row(
                            modifier = Modifier
                                .weight(0.95f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(V14CardElevated)
                                .border(0.75.dp, V14Border, RoundedCornerShape(23.dp))
                                .testTag("create_save_next_button")
                                .v14PressScale {
                                    if (!canSubmit) return@v14PressScale
                                    val created = buildSingleTournament()
                                    onCreate(created)
                                    Toast.makeText(context, "'${created.title}' published! Queue next below.", Toast.LENGTH_SHORT).show()
                                    try {
                                        val cal = Calendar.getInstance()
                                        val parsed = if (startsAt.isNotBlank()) dateFormatter.parse(startsAt) else null
                                        if (parsed != null) cal.time = parsed else cal.add(Calendar.HOUR_OF_DAY, 1)
                                        cal.add(Calendar.HOUR_OF_DAY, 1)
                                        startsAt = dateFormatter.format(cal.time)
                                    } catch (_: Exception) {}
                                    title = if (title.contains("Match", ignoreCase = true)) {
                                        val num = Regex("\\d+").find(title)?.value?.toIntOrNull()
                                        if (num != null) title.replace(num.toString(), (num + 1).toString()) else "$title (Next)"
                                    } else if (title.isNotBlank()) {
                                        "$title (Slot 2)"
                                    } else {
                                        ""
                                    }
                                }
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = V14TextPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save & Next", color = V14TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, maxLines = 1)
                        }

                        // Primary V14 Gradient Pill: Publish Match
                        Row(
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(
                                    if (canSubmit) Brush.horizontalGradient(listOf(V14AccentPrimary, V14AccentBright))
                                    else Brush.horizontalGradient(listOf(V14BarMuted, V14BarMuted))
                                )
                                .testTag("create_publish_button")
                                .v14PressScale {
                                    if (!canSubmit) return@v14PressScale
                                    isSaving = true
                                    val created = buildSingleTournament()
                                    onCreate(created)
                                    Toast.makeText(context, "Tournament published to Firebase!", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                }
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isUploadingBanner || isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isUploadingBanner) "Uploading..." else "Publishing...",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Publish Match", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(V14Bg),
            contentPadding = PaddingValues(top = 14.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (creationMode == "batch") {
                // =============================================================
                // BATCH SCHEDULER MODE (V14 BENTO CARDS — ZERO PURPLE)
                // =============================================================
                item {
                    V14FormBentoCard(
                        icon = Icons.Default.Category,
                        iconTint = V14AccentBright,
                        title = "Categories",
                        trailing = "${selectedBatchCategories.size} selected"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            effectiveCategories.forEach { cat ->
                                val isChecked = selectedBatchCategories.contains(cat.id)
                                V14PillChip(
                                    label = cat.shortName.ifBlank { cat.name },
                                    selected = isChecked,
                                    onClick = {
                                        selectedBatchCategories = if (isChecked) {
                                            if (selectedBatchCategories.size > 1) selectedBatchCategories - cat.id else selectedBatchCategories
                                        } else {
                                            selectedBatchCategories + cat.id
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    V14FormBentoCard(
                        icon = Icons.Default.Schedule,
                        iconTint = V14TextPrimary,
                        title = "Cadence & Interval"
                    ) {
                        V14FieldLabel("Matches per category")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 2, 3).forEach { count ->
                                V14PillChip(
                                    label = "$count Match${if (count > 1) "es" else ""}",
                                    selected = batchMatchesPerCategory == count,
                                    modifier = Modifier.weight(1f),
                                    onClick = { batchMatchesPerCategory = count }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        V14FieldLabel("Interval between matches")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(30 to "30m", 45 to "45m", 60 to "1h", 90 to "1.5h").forEach { (mins, label) ->
                                V14PillChip(
                                    label = label,
                                    selected = batchIntervalMinutes == mins,
                                    modifier = Modifier.weight(1f),
                                    onClick = { batchIntervalMinutes = mins }
                                )
                            }
                        }
                    }
                }

                item {
                    V14FormBentoCard(
                        icon = Icons.Default.EmojiEvents,
                        iconTint = VelorixAmber,
                        title = "Payout Defaults"
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            V14TextField(value = batchEntryFee, onValueChange = { batchEntryFee = it }, label = "Entry (₹)", modifier = Modifier.weight(1f))
                            V14TextField(value = batchPrizePool, onValueChange = { batchPrizePool = it }, label = "Pool (₹)", modifier = Modifier.weight(1f))
                            V14TextField(value = batchPerKillPrize, onValueChange = { batchPerKillPrize = it }, label = "Kill (₹)", modifier = Modifier.weight(1f))
                        }
                    }
                }

                item {
                    V14FormBentoCard(
                        icon = Icons.Default.CalendarMonth,
                        iconTint = VelorixEmerald,
                        title = "Schedule Preview",
                        trailing = "$totalBatchMatches total"
                    ) {
                        var previewIdx = 1
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedBatchCategories.forEach { catKey ->
                                val catMeta = effectiveCategories.firstOrNull { it.id == catKey }
                                val catName = catMeta?.shortName ?: catKey
                                for (m in 1..batchMatchesPerCategory) {
                                    val cal = Calendar.getInstance()
                                    cal.add(Calendar.MINUTE, 60 + ((previewIdx - 1) * batchIntervalMinutes))
                                    val timeStr = dateFormatter.format(cal.time)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(V14CardElevated)
                                            .border(0.6.dp, V14Border, RoundedCornerShape(14.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(
                                                text = "Free Fire $catName #$previewIdx",
                                                color = V14TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${if (catKey == "CS") "4v4" else if (catKey == "LONE_WOLF") "1v1" else "SOLO"} · $timeStr",
                                                color = V14TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Text(
                                            text = "₹$batchPrizePool",
                                            color = VelorixEmerald,
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    previewIdx++
                                }
                            }
                        }
                    }
                }
            } else {
                // =============================================================
                // SINGLE MATCH MODE (100% V14 BENTO CARDS — ZERO PURPLE)
                // =============================================================

                // 1. Quick Templates Strip
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "QUICK TEMPLATES",
                            color = V14TextMuted,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                V14TemplateCard(
                                    title = "Solo Scrim",
                                    meta = "₹500 · 48 slots",
                                    selected = selectedTemplate == "solo",
                                    onClick = {
                                        selectedTemplate = "solo"
                                        title = "Velorix Daily Solo Battle"
                                        category = "BR"; format = "SOLO"; mapName = "Bermuda"
                                        prizePool = "500"; entryFee = "20"
                                        firstPlacePrize = "300"; secondPlacePrize = "120"; thirdPlacePrize = "80"; perKillPrize = "10"
                                        maxPlayers = "48"
                                        bannerUrl = TournamentBannerPresets.PRESETS.getOrNull(0)?.url ?: ""
                                    }
                                )
                            }
                            item {
                                V14TemplateCard(
                                    title = "Squad Cup",
                                    meta = "₹2,000 · 48 slots",
                                    selected = selectedTemplate == "squad",
                                    onClick = {
                                        selectedTemplate = "squad"
                                        title = "Velorix Pro Squad Cup"
                                        category = "BR"; format = "SQUAD"; mapName = "Purgatory"
                                        prizePool = "2000"; entryFee = "80"
                                        firstPlacePrize = "1200"; secondPlacePrize = "500"; thirdPlacePrize = "300"; perKillPrize = "25"
                                        maxPlayers = "48"
                                        bannerUrl = TournamentBannerPresets.PRESETS.getOrNull(2)?.url ?: ""
                                    }
                                )
                            }
                            item {
                                V14TemplateCard(
                                    title = "Clash Squad 4v4",
                                    meta = "₹1,000 · 8 slots",
                                    selected = selectedTemplate == "cs",
                                    onClick = {
                                        selectedTemplate = "cs"
                                        title = "CS Hardcore 4v4 Showdown"
                                        category = "CS"; format = "4v4 CS"; mapName = "Kalahari"
                                        prizePool = "1000"; entryFee = "100"
                                        firstPlacePrize = "800"; secondPlacePrize = "200"; thirdPlacePrize = "0"; perKillPrize = "0"
                                        maxPlayers = "8"
                                        bannerUrl = TournamentBannerPresets.PRESETS.getOrNull(1)?.url ?: ""
                                    }
                                )
                            }
                            item {
                                V14TemplateCard(
                                    title = "Lone Wolf 1v1",
                                    meta = "₹200 · 2 slots",
                                    selected = selectedTemplate == "lw",
                                    onClick = {
                                        selectedTemplate = "lw"
                                        title = "Lone Wolf 1v1 Duel"
                                        category = "LONE_WOLF"; format = "1v1 Lone Wolf"; mapName = "Iron Cage"
                                        prizePool = "200"; entryFee = "20"
                                        firstPlacePrize = "180"; secondPlacePrize = "20"; thirdPlacePrize = "0"; perKillPrize = "0"
                                        maxPlayers = "2"
                                        bannerUrl = TournamentBannerPresets.PRESETS.getOrNull(3)?.url ?: ""
                                        localBannerUri = null; bannerStatusMessage = null
                                    }
                                )
                            }
                        }
                    }
                }

                // 2. Banner Studio Bento Card (Zero Purple Upload Button!)
                item {
                    V14FormBentoCard(
                        icon = Icons.Default.Image,
                        iconTint = V14AccentBright,
                        title = "Banner Artwork",
                        trailing = if (bannerUrl.isNotBlank() || localBannerUri != null) "Remove" else null,
                        onTrailingClick = {
                            bannerUrl = ""
                            localBannerUri = null
                            bannerStatusMessage = null
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(152.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(V14CardElevated)
                                .border(
                                    0.75.dp,
                                    if (bannerUrl.isNotBlank() || localBannerUri != null) V14AccentPrimary.copy(alpha = 0.55f) else V14Border,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                        ) {
                            val resolvedModel = TournamentImageUploader.resolveBannerModel(bannerUrl)
                            val displayBanner = resolvedModel ?: localBannerUri
                            if (displayBanner != null) {
                                AsyncImage(
                                    model = displayBanner,
                                    contentDescription = "Banner Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF210A12), V14CardSurface, V14CardElevated)
                                            )
                                        )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0.0f to Color(0x88000000),
                                            0.55f to Color(0x55000000),
                                            1.0f to Color(0xEB0C0C10)
                                        )
                                    )
                            )

                            if (isUploadingBanner) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color(0xB3000000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = V14AccentPrimary, modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(bannerStatusMessage ?: "Uploading...", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(Color(0xCC0C0C10))
                                            .border(0.6.dp, V14AccentBright.copy(alpha = 0.45f), RoundedCornerShape(50))
                                            .padding(horizontal = 9.dp, vertical = 3.dp)
                                    ) {
                                        Text("$format · $mapName", color = Color(0xFFFFB3C1), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(Color(0xCC0C0C10))
                                            .border(0.6.dp, VelorixEmerald.copy(alpha = 0.45f), RoundedCornerShape(50))
                                            .padding(horizontal = 9.dp, vertical = 3.dp)
                                    ) {
                                        Text("PRIZE ₹${prizePool.ifBlank { "0" }}", color = VelorixEmerald, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Column {
                                    Text(
                                        text = title.ifBlank { "Tournament Title Preview" },
                                        color = V14TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = bannerStatusMessage ?: if (displayBanner != null) "Tap preview to replace artwork" else "Tap preview to upload or select a preset below",
                                        color = V14TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            V14SourcePill(
                                icon = Icons.Default.PhotoLibrary,
                                label = "Gallery",
                                active = false,
                                modifier = Modifier.weight(1f),
                                onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                            )
                            V14SourcePill(
                                icon = Icons.Default.PhotoCamera,
                                label = "Camera",
                                active = false,
                                modifier = Modifier.weight(1f),
                                onClick = { cameraLauncher.launch(null) }
                            )
                            V14SourcePill(
                                icon = Icons.Default.Link,
                                label = "URL",
                                active = showUrlField || bannerUrl.startsWith("http"),
                                modifier = Modifier.weight(0.9f),
                                onClick = { showUrlField = !showUrlField }
                            )
                        }

                        AnimatedVisibility(visible = showUrlField) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                V14TextField(
                                    value = if (localBannerUri != null && !bannerUrl.startsWith("http")) "[Local Image Attached]" else bannerUrl,
                                    onValueChange = {
                                        bannerUrl = it
                                        localBannerUri = null
                                        bannerStatusMessage = null
                                    },
                                    label = "Banner Image URL",
                                    placeholder = "https://example.com/banner.jpg",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        V14FieldLabel("Presets")
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(TournamentBannerPresets.PRESETS) { preset ->
                                val isSelected = bannerUrl == preset.url
                                Box(
                                    modifier = Modifier
                                        .width(124.dp)
                                        .height(68.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.75.dp,
                                            color = if (isSelected) V14AccentPrimary else V14Border,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            bannerUrl = preset.url
                                            localBannerUri = null
                                            bannerStatusMessage = "Preset selected"
                                            if (preset.map != "All Maps" && mapName == "Bermuda") {
                                                mapName = preset.map
                                            }
                                        }
                                ) {
                                    AsyncImage(
                                        model = preset.url,
                                        contentDescription = preset.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xDF0C0C10))))
                                    )
                                    Column(
                                        modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                                    ) {
                                        Text(preset.title, color = V14TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(preset.map, color = if (isSelected) V14AccentBright else V14TextSecondary, fontSize = 9.sp)
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = V14AccentPrimary,
                                            modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Category & Match Details Bento Card (ZERO Purple Cards/Buttons!)
                item {
                    V14FormBentoCard(
                        icon = Icons.Default.SportsEsports,
                        iconTint = V14TextPrimary,
                        title = "Category & Match Details",
                        trailing = "+ New Category",
                        onTrailingClick = { showAddCategoryDialog = true }
                    ) {
                        V14FieldLabel("Tournament Category")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            effectiveCategories.forEach { catMeta ->
                                val isSelected = category == catMeta.id
                                V14PillChip(
                                    label = catMeta.shortName.ifBlank { catMeta.name },
                                    selected = isSelected,
                                    onClick = {
                                        category = catMeta.id
                                        if (mapName == "Bermuda" || mapName.isBlank()) {
                                            mapName = catMeta.defaultMaps.firstOrNull() ?: "Bermuda"
                                        }
                                        format = catMeta.defaultFormats.firstOrNull() ?: "SOLO"
                                        maxPlayers = catMeta.defaultMaxPlayers.toString()
                                        if (title.isBlank() || title.contains("Free Fire", ignoreCase = true)) {
                                            title = "Free Fire ${catMeta.name} Tournament"
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        V14TextField(
                            value = title,
                            onValueChange = { title = it },
                            label = "Tournament Title *",
                            placeholder = "e.g. Bermuda Pro Solo Series",
                            modifier = Modifier.fillMaxWidth().testTag("create_title_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        V14FieldLabel("Format")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("SOLO" to "48", "DUO" to "48", "SQUAD" to "48", "4v4 CS" to "8", "1v1 Lone Wolf" to "2").forEach { (fmt, slots) ->
                                V14PillChip(
                                    label = fmt,
                                    selected = format.equals(fmt, ignoreCase = true),
                                    onClick = { format = fmt; maxPlayers = slots }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        V14FieldLabel("Map")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Bermuda", "Purgatory", "Kalahari", "Alpine", "NexTerra", "Iron Cage").forEach { mapOpt ->
                                V14PillChip(
                                    label = mapOpt,
                                    selected = mapName.equals(mapOpt, ignoreCase = true),
                                    onClick = { mapName = mapOpt }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        V14FieldLabel("Max Slots")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("2", "8", "12", "24", "48", "50", "100").forEach { slotOpt ->
                                V14PillChip(
                                    label = "$slotOpt Slots",
                                    selected = maxPlayers == slotOpt,
                                    onClick = { maxPlayers = slotOpt }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            V14TextField(value = mapName, onValueChange = { mapName = it }, label = "Custom Map", modifier = Modifier.weight(1f))
                            V14TextField(value = maxPlayers, onValueChange = { maxPlayers = it }, label = "Custom Slots", modifier = Modifier.weight(1f))
                        }
                    }
                }

                // 4. Prize Pool & Entry Fee Bento Card
                item {
                    V14FormBentoCard(
                        icon = Icons.Default.EmojiEvents,
                        iconTint = VelorixAmber,
                        title = "Prize Pool & Entry Fee"
                    ) {
                        V14FieldLabel("Entry Fee")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("0", "10", "20", "30", "50", "100", "200").forEach { feeOpt ->
                                V14PillChip(
                                    label = if (feeOpt == "0") "Free" else "₹$feeOpt",
                                    selected = entryFee == feeOpt,
                                    onClick = { entryFee = feeOpt }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        V14FieldLabel("Prize Pool")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("100", "250", "500", "1000", "2500", "5000", "10000").forEach { poolOpt ->
                                V14PillChip(
                                    label = "₹$poolOpt",
                                    selected = prizePool == poolOpt,
                                    onClick = {
                                        prizePool = poolOpt
                                        val p = poolOpt.toFloatOrNull() ?: 0f
                                        if (p > 0f) {
                                            firstPlacePrize = (p * 0.50f).toInt().toString()
                                            secondPlacePrize = (p * 0.25f).toInt().toString()
                                            thirdPlacePrize = (p * 0.15f).toInt().toString()
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            V14TextField(
                                value = prizePool,
                                onValueChange = {
                                    prizePool = it
                                    val p = it.toFloatOrNull() ?: 0f
                                    if (p > 0f) {
                                        firstPlacePrize = (p * 0.50f).toInt().toString()
                                        secondPlacePrize = (p * 0.25f).toInt().toString()
                                        thirdPlacePrize = (p * 0.15f).toInt().toString()
                                    }
                                },
                                label = "Prize Pool (₹) *",
                                modifier = Modifier.weight(1f)
                            )
                            V14TextField(value = entryFee, onValueChange = { entryFee = it }, label = "Entry Fee (₹)", modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        V14FieldLabel("Quick Split Ratios")
                        Spacer(modifier = Modifier.height(8.dp))
                        val p = prizePool.toFloatOrNull() ?: 1000f
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            V14PillChip("50/25/15 + Kill", false, Modifier.weight(1.1f)) {
                                firstPlacePrize = (p * 0.50f).toInt().toString()
                                secondPlacePrize = (p * 0.25f).toInt().toString()
                                thirdPlacePrize = (p * 0.15f).toInt().toString()
                                perKillPrize = "10"
                            }
                            V14PillChip("60/25/15", false, Modifier.weight(0.85f)) {
                                firstPlacePrize = (p * 0.60f).toInt().toString()
                                secondPlacePrize = (p * 0.25f).toInt().toString()
                                thirdPlacePrize = (p * 0.15f).toInt().toString()
                                perKillPrize = "0"
                            }
                            V14PillChip("100% #1", false, Modifier.weight(0.8f)) {
                                firstPlacePrize = p.toInt().toString()
                                secondPlacePrize = "0"
                                thirdPlacePrize = "0"
                                perKillPrize = "0"
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            V14TextField(value = firstPlacePrize, onValueChange = { firstPlacePrize = it }, label = "1st Place (₹)", modifier = Modifier.weight(1f))
                            V14TextField(value = secondPlacePrize, onValueChange = { secondPlacePrize = it }, label = "2nd Place (₹)", modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            V14TextField(value = thirdPlacePrize, onValueChange = { thirdPlacePrize = it }, label = "3rd Place (₹)", modifier = Modifier.weight(1f))
                            V14TextField(value = perKillPrize, onValueChange = { perKillPrize = it }, label = "Per Kill (₹)", modifier = Modifier.weight(1f))
                        }
                    }
                }

                // 5. Schedule Bento Card
                item {
                    V14FormBentoCard(
                        icon = Icons.Default.Schedule,
                        iconTint = VelorixEmerald,
                        title = "Schedule"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            V14PillChip("+1 Hour", false) {
                                val cal = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
                                startsAt = dateFormatter.format(cal.time)
                            }
                            V14PillChip("+2 Hours", false) {
                                val cal = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 2) }
                                startsAt = dateFormatter.format(cal.time)
                            }
                            V14PillChip("Tonight 8 PM", false) {
                                val cal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 20); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                                    if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
                                }
                                startsAt = dateFormatter.format(cal.time)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(V14CardElevated)
                                .border(0.75.dp, V14Border, RoundedCornerShape(14.dp))
                                .clickable {
                                    val cal = Calendar.getInstance()
                                    DatePickerDialog(context, { _, y, m, d ->
                                        TimePickerDialog(context, { _, h, min ->
                                            cal.set(y, m, d, h, min, 0)
                                            startsAt = dateFormatter.format(cal.time)
                                        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                                }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = V14AccentBright, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("Start Date & Time", color = V14TextMuted, fontSize = 10.5.sp)
                                    Text(
                                        text = startsAt.ifBlank { "Tap to pick (defaults to +1 hour)" },
                                        color = if (startsAt.isNotBlank()) V14TextPrimary else V14TextSecondary,
                                        fontSize = 13.5.sp,
                                        fontFamily = if (startsAt.isNotBlank()) FontFamily.Monospace else FontFamily.Default,
                                        fontWeight = if (startsAt.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = V14TextMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // 6. Loadout & Rules Bento Card
                item {
                    V14FormBentoCard(
                        icon = Icons.Default.Gavel,
                        iconTint = V14AccentBright,
                        title = "Loadout & Match Rules"
                    ) {
                        V14FieldLabel("Tap to Ban / Allow Weapons")
                        Spacer(modifier = Modifier.height(8.dp))
                        val weaponList = listOf("M79 Launcher", "M82B", "SVD", "Double Vector", "Crossbow", "MAG-7", "Charge Buster")
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            weaponList.forEach { weapon ->
                                val currentBanned = bannedGuns.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                val isBanned = currentBanned.any { it.equals(weapon, ignoreCase = true) }
                                V14PillChip(
                                    label = if (isBanned) "Banned · $weapon" else weapon,
                                    selected = isBanned,
                                    onClick = {
                                        val updated = if (isBanned) currentBanned.filterNot { it.equals(weapon, ignoreCase = true) } else currentBanned + weapon
                                        bannedGuns = updated.joinToString(", ")
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        V14TextField(value = bannedGuns, onValueChange = { bannedGuns = it }, label = "Banned Weapons", modifier = Modifier.fillMaxWidth())

                        Spacer(modifier = Modifier.height(10.dp))
                        V14ToggleRow(
                            title = "Gun Skin Attributes",
                            subtitle = "Allow weapon skin damage & fire-rate buffs",
                            checked = gunAttributesAllowed,
                            onCheckedChange = { gunAttributesAllowed = it }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = V14Border)
                        V14ToggleRow(
                            title = "Limited Ammo",
                            subtitle = "Standard ammo economy (off = unlimited gloo/ammo)",
                            checked = limitedAmmo,
                            onCheckedChange = { limitedAmmo = it }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = V14Border)
                        V14ToggleRow(
                            title = "Character Skills",
                            subtitle = "Allow active and passive character abilities",
                            checked = characterSkillsAllowed,
                            onCheckedChange = { characterSkillsAllowed = it }
                        )
                        AnimatedVisibility(visible = characterSkillsAllowed) {
                            Column {
                                V14TextField(
                                    value = bannedSkills,
                                    onValueChange = { bannedSkills = it },
                                    label = "Banned Skills (e.g. Chrono, Dimitri)",
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                                )
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = V14Border)
                        V14ToggleRow(
                            title = "Allow PC / Emulator",
                            subtitle = "Keep off for mobile-only competitive lobbies",
                            checked = emulatorAllowed,
                            onCheckedChange = { emulatorAllowed = it }
                        )
                    }
                }

                // 7. Overview & Regulations Bento Card
                item {
                    V14FormBentoCard(
                        icon = Icons.Default.MenuBook,
                        iconTint = V14TextSecondary,
                        title = "Overview & Regulations"
                    ) {
                        V14TextField(value = description, onValueChange = { description = it }, label = "Overview Description", modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(10.dp))
                        V14TextField(value = rules, onValueChange = { rules = it }, label = "Official Rules", minLines = 3, singleLine = false, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }

    // Add Custom Category Dialog
    if (showAddCategoryDialog) {
        FrostedGlassAlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("New Tournament Category", fontWeight = FontWeight.Bold, color = V14TextPrimary, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Saved immediately to Firebase RTDB & Firestore for Admin and Player apps.", fontSize = 12.sp, color = V14TextSecondary)
                    V14TextField(
                        value = newCatName,
                        onValueChange = {
                            newCatName = it
                            if (newCatCode.isBlank()) newCatCode = it.trim().uppercase().replace(" ", "_")
                        },
                        label = "Category Name *",
                        placeholder = "e.g. Bermuda Championship",
                        modifier = Modifier.fillMaxWidth()
                    )
                    V14TextField(
                        value = newCatCode,
                        onValueChange = { newCatCode = it.trim().uppercase().replace(" ", "_") },
                        label = "Category Code *",
                        placeholder = "e.g. BERMUDA_CHAMP",
                        modifier = Modifier.fillMaxWidth()
                    )
                    V14TextField(
                        value = newCatDescription,
                        onValueChange = { newCatDescription = it },
                        label = "Description",
                        modifier = Modifier.fillMaxWidth()
                    )
                    V14TextField(
                        value = newCatMaxPlayers,
                        onValueChange = { newCatMaxPlayers = it },
                        label = "Default Max Slots",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank() && newCatCode.isNotBlank()) {
                            val newCategory = TournamentCategory(
                                id = newCatCode.trim().uppercase(),
                                name = newCatName.trim(),
                                label = newCatName.trim(),
                                shortName = newCatName.trim(),
                                description = newCatDescription.trim(),
                                defaultFormats = listOf("SOLO", "DUO", "SQUAD"),
                                defaultMaps = listOf("Bermuda", "Purgatory", "Kalahari"),
                                defaultMaxPlayers = newCatMaxPlayers.toIntOrNull() ?: 48,
                                isCustom = true
                            )
                            onSaveCategory?.invoke(newCategory)
                            category = newCategory.id
                            showAddCategoryDialog = false
                            Toast.makeText(context, "Category '${newCategory.name}' saved!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = V14AccentPrimary, contentColor = Color.White)
                ) {
                    Text("Save Category", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel", color = V14TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun V14ModeUnderlineTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = if (selected) V14TextPrimary else V14TextMuted,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.46f)
                .height(2.dp)
                .background(if (selected) V14AccentPrimary else Color.Transparent, RoundedCornerShape(1.dp))
        )
    }
}

@Composable
private fun V14FormBentoCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(V14CardSurface)
            .border(0.75.dp, V14Border, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(15.dp))
                }
                Text(
                    text = title,
                    color = V14TextPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (trailing != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(V14CardElevated)
                        .border(0.75.dp, V14Border, RoundedCornerShape(50))
                        .then(if (onTrailingClick != null) Modifier.clickable(onClick = onTrailingClick) else Modifier)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = trailing,
                        color = if (onTrailingClick != null) V14AccentBright else V14TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        content()
    }
}

@Composable
private fun V14FieldLabel(text: String) {
    Text(
        text = text,
        color = V14TextSecondary,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun V14TemplateCard(
    title: String,
    meta: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) V14AccentPrimary.copy(alpha = 0.16f) else V14CardSurface)
            .border(
                width = if (selected) 1.dp else 0.75.dp,
                color = if (selected) V14AccentPrimary else V14Border,
                shape = RoundedCornerShape(16.dp)
            )
            .v14PressScale(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Text(title, color = V14TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = meta,
            color = if (selected) V14AccentBright else V14TextSecondary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}

@Composable
private fun V14SourcePill(
    icon: ImageVector,
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) V14AccentPrimary.copy(alpha = 0.16f) else V14CardElevated)
            .border(
                0.75.dp,
                if (active) V14AccentBright.copy(alpha = 0.5f) else V14Border,
                RoundedCornerShape(20.dp)
            )
            .v14PressScale(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) V14AccentBright else V14TextPrimary,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = if (active) V14AccentBright else V14TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun V14PillChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) V14AccentPrimary else V14CardElevated)
            .border(
                width = 0.75.dp,
                color = if (selected) V14AccentBright else V14Border,
                shape = RoundedCornerShape(18.dp)
            )
            .v14PressScale(onClick = onClick)
            .padding(horizontal = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else V14TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun V14TextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        placeholder = placeholder?.let { { Text(it, color = V14TextMuted, fontSize = 12.5.sp) } },
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = V14CardElevated,
            unfocusedContainerColor = V14CardElevated,
            focusedTextColor = V14TextPrimary,
            unfocusedTextColor = V14TextPrimary,
            focusedBorderColor = V14AccentPrimary.copy(alpha = 0.8f),
            unfocusedBorderColor = V14Border,
            focusedLabelColor = V14AccentBright,
            unfocusedLabelColor = V14TextMuted,
            cursorColor = V14AccentPrimary
        )
    )
}

@Composable
private fun V14ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = V14TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = V14TextMuted, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = V14AccentPrimary,
                uncheckedThumbColor = V14TextSecondary,
                uncheckedTrackColor = V14CardElevated,
                uncheckedBorderColor = V14Border
            )
        )
    }
}
