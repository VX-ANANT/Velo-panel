package com.example.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.validation.SecuritySanitizer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextAlign
import com.example.domain.model.TournamentCategory
import com.example.domain.model.FreeFireCategories
import com.example.domain.model.Tournament
import com.example.domain.model.TournamentBannerPresets
import com.example.ui.common.GameLogoBadge
import com.example.ui.theme.*
import com.example.util.ImageUploadResult
import com.example.util.TournamentImageUploader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

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

    var creationMode by remember { mutableStateOf("single") } // "single" or "batch"
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCatName by remember { mutableStateOf("") }
    var newCatCode by remember { mutableStateOf("") }
    var newCatDescription by remember { mutableStateOf("") }
    var newCatFormat by remember { mutableStateOf("SOLO, DUO, SQUAD") }
    var newCatMaps by remember { mutableStateOf("Bermuda, Purgatory, Kalahari") }
    var newCatMaxPlayers by remember { mutableStateOf("48") }

    val effectiveCategories = remember(categories) {
        if (categories.isNotEmpty()) categories else FreeFireCategories.DEFAULT_CATEGORIES
    }

    // Batch creation states
    var selectedBatchCategories by remember { mutableStateOf(setOf("BR", "CS")) }
    var batchMatchesPerCategory by remember { mutableStateOf(1) }
    var batchIntervalMinutes by remember { mutableStateOf(60) }
    var batchFormat by remember { mutableStateOf("SOLO") }
    var batchEntryFee by remember { mutableStateOf("20") }
    var batchPrizePool by remember { mutableStateOf("1000") }
    var batchPerKillPrize by remember { mutableStateOf("20") }
    var batchStartsAt by remember { mutableStateOf("") }

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

    // Rules & Guns
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

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    var localBannerUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingBanner by remember { mutableStateOf(false) }
    var bannerStatusMessage by remember { mutableStateOf<String?>(null) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            coroutineScope.launch {
                isUploadingBanner = true
                bannerStatusMessage = "Processing camera photo..."
                try {
                    val result = TournamentImageUploader.uploadTournamentBannerBitmap(
                        context = context,
                        bitmap = bitmap,
                        onProgress = { bannerStatusMessage = it }
                    )
                    when (result) {
                        is ImageUploadResult.Success -> {
                            bannerUrl = result.url
                            localBannerUri = null
                            isUploadingBanner = false
                            bannerStatusMessage = "Uploaded via ${result.provider} ✓"
                            Toast.makeText(context, "Photo uploaded successfully!", Toast.LENGTH_SHORT).show()
                        }
                        is ImageUploadResult.Failure -> {
                            if (result.fallbackBase64 != null) {
                                bannerUrl = result.fallbackBase64
                                localBannerUri = null
                                isUploadingBanner = false
                                bannerStatusMessage = "Photo attached ✓"
                                Toast.makeText(context, "Photo attached as banner", Toast.LENGTH_SHORT).show()
                            } else {
                                isUploadingBanner = false
                                bannerStatusMessage = "Upload error: ${result.errorMessage}"
                                Toast.makeText(context, "Upload failed: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                    isUploadingBanner = false
                    bannerStatusMessage = null
                    Toast.makeText(context, "Camera error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Modern Zero-Permission Android Photo Picker (never crashes with OOM or SecurityException)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isUploadingBanner = true
                localBannerUri = uri
                bannerStatusMessage = "Processing gallery image..."
                try {
                    val result = TournamentImageUploader.uploadTournamentBanner(
                        context = context,
                        uri = uri,
                        onProgress = { bannerStatusMessage = it }
                    )
                    when (result) {
                        is ImageUploadResult.Success -> {
                            bannerUrl = result.url
                            localBannerUri = null
                            isUploadingBanner = false
                            bannerStatusMessage = "Uploaded via ${result.provider} ✓"
                            Toast.makeText(context, "Tournament banner uploaded!", Toast.LENGTH_SHORT).show()
                        }
                        is ImageUploadResult.Failure -> {
                            if (result.fallbackBase64 != null) {
                                bannerUrl = result.fallbackBase64
                                localBannerUri = null
                                isUploadingBanner = false
                                bannerStatusMessage = "Image attached ✓"
                                Toast.makeText(context, "Banner image attached", Toast.LENGTH_SHORT).show()
                            } else {
                                isUploadingBanner = false
                                bannerStatusMessage = "Upload error: ${result.errorMessage}"
                                Toast.makeText(context, "Upload error: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                    isUploadingBanner = false
                    bannerStatusMessage = "Error reading image"
                    Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Tournament", color = VelorixTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Mode Selector: Single Tournament vs ⚡ Multi-Category Batch
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF14141A), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { creationMode = "single" },
                        color = if (creationMode == "single") VelorixAccent else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Single Tournament",
                            color = if (creationMode == "single") Color.Black else VelorixTextSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 9.dp)
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { creationMode = "batch" },
                        color = if (creationMode == "batch") VelorixAccent else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 9.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (creationMode == "batch") Color.Black else Color(0xFFFFD54F),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "⚡ Batch Multi-Category",
                                color = if (creationMode == "batch") Color.Black else VelorixTextSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (creationMode == "batch") {
                item {
                    SettingsCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("⚡ Batch Multi-Category Generator", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Schedule multiple matches across multiple categories in 1 click", fontSize = 11.5.sp, color = VelorixTextSecondary)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF6366F1).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFF6366F1))
                            ) {
                                Text(
                                    "${selectedBatchCategories.size} Categories",
                                    color = Color(0xFF818CF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Category Multi-Select
                        Text("1. Select Categories to Schedule Matches For *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            effectiveCategories.forEach { cat ->
                                val isChecked = selectedBatchCategories.contains(cat.id)
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedBatchCategories = if (isChecked) {
                                                if (selectedBatchCategories.size > 1) selectedBatchCategories - cat.id else selectedBatchCategories
                                            } else {
                                                selectedBatchCategories + cat.id
                                            }
                                        },
                                    color = if (isChecked) VelorixAccent.copy(alpha = 0.25f) else Color(0xFF1E1E28),
                                    border = BorderStroke(1.dp, if (isChecked) VelorixAccent else CardVerifyBorder),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            if (isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isChecked) VelorixAccent else Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Column {
                                            Text(cat.shortName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(if (cat.isCustom) "Firebase Custom" else "Preset", color = VelorixTextSecondary, fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Matches per category
                        Text("2. Matches per Category", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 2, 3).forEach { count ->
                                val isSelected = batchMatchesPerCategory == count
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { batchMatchesPerCategory = count },
                                    color = if (isSelected) VelorixAccent.copy(alpha = 0.2f) else Color(0xFF1E1E28),
                                    border = BorderStroke(1.dp, if (isSelected) VelorixAccent else CardVerifyBorder),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "$count Match${if (count > 1) "es" else ""}",
                                        textAlign = TextAlign.Center,
                                        color = if (isSelected) VelorixAccent else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Interval between matches
                        Text("3. Time Interval Between Matches", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(30 to "30 Min", 45 to "45 Min", 60 to "1 Hour", 90 to "1.5 Hrs").forEach { (mins, label) ->
                                val isSelected = batchIntervalMinutes == mins
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { batchIntervalMinutes = mins },
                                    color = if (isSelected) VelorixAccent.copy(alpha = 0.2f) else Color(0xFF1E1E28),
                                    border = BorderStroke(1.dp, if (isSelected) VelorixAccent else CardVerifyBorder),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        label,
                                        textAlign = TextAlign.Center,
                                        color = if (isSelected) VelorixAccent else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Match Fee and Prize Pool
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = batchEntryFee,
                                onValueChange = { batchEntryFee = it },
                                label = { Text("Entry Fee (₹)") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                            )
                            OutlinedTextField(
                                value = batchPrizePool,
                                onValueChange = { batchPrizePool = it },
                                label = { Text("Prize Pool (₹)") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                            )
                            OutlinedTextField(
                                value = batchPerKillPrize,
                                onValueChange = { batchPerKillPrize = it },
                                label = { Text("Per Kill (₹)") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Total Matches calculated
                        val totalMatchesToCreate = selectedBatchCategories.size * batchMatchesPerCategory
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Preview of Scheduled Matches", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 13.sp)
                                    Text("Total: $totalMatchesToCreate Matches", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                var previewIdx = 1
                                selectedBatchCategories.forEach { catKey ->
                                    val catMeta = effectiveCategories.firstOrNull { it.id == catKey }
                                    val catName = catMeta?.shortName ?: catKey
                                    for (m in 1..batchMatchesPerCategory) {
                                        val cal = Calendar.getInstance()
                                        cal.add(Calendar.MINUTE, 60 + ((previewIdx - 1) * batchIntervalMinutes))
                                        val timeStr = dateFormatter.format(cal.time)
                                        Text(
                                            "• Match $previewIdx: Free Fire $catName (${if (catKey == "CS") "4v4" else if (catKey == "LONE_WOLF") "1v1" else "SOLO"}) at $timeStr (₹$batchPrizePool Pool)",
                                            fontSize = 11.sp,
                                            color = Color.LightGray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        previewIdx++
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Big Publish Batch Button
                        Button(
                            onClick = {
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

                                        val newT = Tournament(
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
                                        generatedList.add(newT)
                                        matchIndex++
                                    }
                                }

                                if (onCreateBatch != null) {
                                    onCreateBatch(generatedList)
                                } else {
                                    generatedList.forEach { onCreate(it) }
                                }
                                Toast.makeText(context, "Published ${generatedList.size} tournaments across categories to Firebase!", Toast.LENGTH_LONG).show()
                                onNavigateBack()
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !isSaving
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("⚡ GENERATE & PUBLISH ALL ($totalMatchesToCreate) MATCHES", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        }
                    }
                }
            }

            if (creationMode == "single") {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x1AFFB74D), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Newly created tournaments are broadcast in Realtime to player feeds and admin oversight.",
                            color = Color(0xFFFFB74D),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

            // QUICK AUTOFILL TEMPLATES (Making tournament addition super fast and easy)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("1-Tap Quick Setup Templates", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            QuickTemplateChip(
                                label = "⚡ Solo Scrim (₹500)",
                                onClick = {
                                    title = "Velorix Daily Solo Battle"
                                    category = "BR"
                                    format = "SOLO"
                                    mapName = "Bermuda"
                                    prizePool = "500"
                                    entryFee = "20"
                                    firstPlacePrize = "300"
                                    secondPlacePrize = "120"
                                    thirdPlacePrize = "80"
                                    perKillPrize = "10"
                                    maxPlayers = "48"
                                    bannerUrl = TournamentBannerPresets.PRESETS.getOrNull(0)?.url ?: "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=1200&q=80"
                                }
                            )
                        }
                        item {
                            QuickTemplateChip(
                                label = "⚡ Squad Championship (₹2,000)",
                                onClick = {
                                    title = "Velorix Pro Squad Cup"
                                    category = "BR"
                                    format = "SQUAD"
                                    mapName = "Purgatory"
                                    prizePool = "2000"
                                    entryFee = "80"
                                    firstPlacePrize = "1200"
                                    secondPlacePrize = "500"
                                    thirdPlacePrize = "300"
                                    perKillPrize = "25"
                                    maxPlayers = "48"
                                    bannerUrl = TournamentBannerPresets.PRESETS.getOrNull(2)?.url ?: "https://images.unsplash.com/photo-1511512578047-dfb367046420?auto=format&fit=crop&w=1200&q=80"
                                }
                            )
                        }
                        item {
                            QuickTemplateChip(
                                label = "⚡ Clash Squad 4v4 (₹1,000)",
                                onClick = {
                                    title = "CS Hardcore 4v4 Showdown"
                                    category = "CS"
                                    format = "SQUAD"
                                    mapName = "Kalahari"
                                    prizePool = "1000"
                                    entryFee = "100"
                                    firstPlacePrize = "800"
                                    secondPlacePrize = "200"
                                    thirdPlacePrize = "0"
                                    perKillPrize = "0"
                                    maxPlayers = "8"
                                    bannerUrl = TournamentBannerPresets.PRESETS.getOrNull(1)?.url ?: "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=1200&q=80"
                                }
                            )
                        }
                        item {
                            QuickTemplateChip(
                                label = "⚡ 1v1 Lone Wolf (₹200)",
                                onClick = {
                                    title = "Lone Wolf 1v1 Duel"
                                    category = "LONE_WOLF"
                                    format = "1v1"
                                    mapName = "Iron Cage"
                                    prizePool = "200"
                                    entryFee = "20"
                                    firstPlacePrize = "180"
                                    secondPlacePrize = "20"
                                    thirdPlacePrize = "0"
                                    perKillPrize = "0"
                                    maxPlayers = "2"
                                    bannerUrl = TournamentBannerPresets.PRESETS.getOrNull(3)?.url ?: "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?auto=format&fit=crop&w=1200&q=80"
                                    localBannerUri = null
                                    bannerStatusMessage = null
                                }
                            )
                        }
                    }
                }
            }

            // Banner Image & Live Card Preview Section
            item {
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tournament Card Banner Image", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary)
                        }
                        if (bannerUrl.isNotBlank() || localBannerUri != null) {
                            Surface(
                                modifier = Modifier.clickable { 
                                    bannerUrl = ""
                                    localBannerUri = null
                                    bannerStatusMessage = null
                                },
                                color = Color(0x33FF5252),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Remove Banner",
                                    color = Color(0xFFFF5252),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Add an eye-catching banner image to display on top of the tournament card in player feeds and the dashboard.",
                        color = VelorixTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Prominent Direct Upload Button
                    Button(
                        onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📸 UPLOAD THUMBNAIL / BANNER", fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.5.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Card Banner Preview
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F12)),
                        border = BorderStroke(1.dp, if (bannerUrl.isNotBlank() || localBannerUri != null) VelorixAccent else CardVerifyBorder)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val resolvedModel = TournamentImageUploader.resolveBannerModel(bannerUrl)
                            val displayBanner = resolvedModel ?: localBannerUri
                            if (displayBanner != null) {
                                AsyncImage(
                                    model = displayBanner,
                                    contentDescription = "Tournament Card Banner Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            if (isUploadingBanner) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0x88000000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = VelorixAccent, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(bannerStatusMessage ?: "Uploading...", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            // Dark gradient overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0x33000000),
                                                Color(0x880E0919),
                                                Color(0xF00E0919)
                                            )
                                        )
                                    )
                            )

                            // Preview elements inside the card
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
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
                                            text = if (format.isNotBlank()) "$format • $mapName" else "SOLO • Bermuda",
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
                                            text = "PRIZE: ₹$prizePool",
                                            color = Color(0xFF69F0AE),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = if (title.isNotBlank()) title else "Tournament Title Card Preview",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (bannerUrl.isNotBlank() || localBannerUri != null) "Live Banner Image Attached" else "Default theme gradient active",
                                        color = if (bannerUrl.isNotBlank() || localBannerUri != null) VelorixAccentLight else VelorixTextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Camera & Gallery Upload Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, VelorixAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gallery", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = { cameraLauncher.launch(null) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, VelorixAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Camera", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Custom URL Input
                    OutlinedTextField(
                        value = if (localBannerUri != null && !bannerUrl.startsWith("http")) "[Local Banner Image Attached]" else bannerUrl,
                        onValueChange = { 
                            bannerUrl = it 
                            localBannerUri = null
                            bannerStatusMessage = null
                        },
                        label = { Text("Or Paste Banner Image URL") },
                        placeholder = { Text("https://example.com/banner.jpg") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (bannerUrl.isNotBlank() || localBannerUri != null) {
                                IconButton(onClick = { 
                                    bannerUrl = "" 
                                    localBannerUri = null
                                    bannerStatusMessage = null
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = VelorixTextSecondary)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Or Select High-Def Free Fire Banner Preset:", fontSize = 12.sp, color = VelorixTextSecondary, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Presets Horizontal Slider
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(TournamentBannerPresets.PRESETS) { preset ->
                            val isSelected = bannerUrl == preset.url
                            Surface(
                                modifier = Modifier
                                    .width(130.dp)
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        bannerUrl = preset.url
                                        localBannerUri = null
                                        bannerStatusMessage = null
                                        if (preset.map != "All Maps" && mapName == "Bermuda") {
                                            mapName = preset.map
                                        }
                                    },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) VelorixAccent else CardVerifyBorder
                                )
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
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Transparent, Color(0xCC000000))
                                                )
                                            )
                                    )
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp),
                                        verticalArrangement = Arrangement.Bottom
                                    ) {
                                        Text(
                                            text = preset.title,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = preset.map,
                                            color = if (isSelected) VelorixAccent else Color.LightGray,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tournament Category Selector (Dynamic from Firebase)
            item {
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Free Fire Tournament Category *",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelorixTextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = VelorixAccent.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, VelorixAccent)
                            ) {
                                Text(
                                    text = effectiveCategories.firstOrNull { it.id == category }?.shortName ?: category,
                                    color = VelorixAccentLight,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF6366F1).copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, Color(0xFF6366F1)),
                                modifier = Modifier.clickable { showAddCategoryDialog = true }
                            ) {
                                Text(
                                    text = "+ Add",
                                    color = Color(0xFF818CF8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        "Extracted live from Firebase. Synchronizes immediately into user panel category tabs.",
                        fontSize = 11.sp,
                        color = VelorixTextSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    // Categories horizontally scrollable chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        effectiveCategories.forEach { catMeta ->
                            val isSelected = category == catMeta.id
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        category = catMeta.id
                                        if (mapName == "Bermuda" || mapName.isBlank()) {
                                            mapName = catMeta.defaultMaps.firstOrNull() ?: "Bermuda"
                                        }
                                        format = catMeta.defaultFormats.firstOrNull() ?: "SOLO"
                                        maxPlayers = catMeta.defaultMaxPlayers.toString()
                                        if (title.isBlank() || title.contains("Free Fire", ignoreCase = true)) {
                                            title = "Free Fire ${catMeta.name} Tournament"
                                        }
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) VelorixAccent.copy(alpha = 0.2f) else Color(0xFF1E1630),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) VelorixAccent else CardVerifyBorder
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = catMeta.shortName,
                                        color = if (isSelected) VelorixAccentLight else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (catMeta.isCustom) "Firebase Custom" else (catMeta.defaultFormats.firstOrNull() ?: ""),
                                        color = if (isSelected) Color(0xFF69F0AE) else VelorixTextSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Add category chip
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showAddCategoryDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E1E2C),
                            border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(14.dp))
                                Text("+ Category", color = Color(0xFF818CF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Basic Info
            item {
                SettingsCard {
                    Text("Basic Tournament Information", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary, modifier = Modifier.padding(bottom = 10.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Tournament Title *") },
                        placeholder = { Text("e.g. Free Fire Bermuda Solo Championship") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = game,
                            onValueChange = { game = it },
                            label = { Text("Game") },
                            leadingIcon = { GameLogoBadge(gameName = game, size = 20.dp) },
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
            }

            // Financials & Prize Breakdown
            item {
                SettingsCard {
                    Text("Prize Pool & Prize Distribution", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary, modifier = Modifier.padding(bottom = 10.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = prizePool,
                            onValueChange = { 
                                prizePool = it 
                                val p = it.toFloatOrNull() ?: 0f
                                if (p > 0) {
                                    firstPlacePrize = (p * 0.50f).toInt().toString()
                                    secondPlacePrize = (p * 0.25f).toInt().toString()
                                    thirdPlacePrize = (p * 0.15f).toInt().toString()
                                }
                            },
                            label = { Text("Total Prize Pool (₹) *") },
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

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Prize Pool Presets", fontSize = 12.sp, color = VelorixTextSecondary, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val p = prizePool.toFloatOrNull() ?: 1000f
                        Surface(
                            modifier = Modifier.clickable {
                                firstPlacePrize = (p * 0.50f).toInt().toString()
                                secondPlacePrize = (p * 0.25f).toInt().toString()
                                thirdPlacePrize = (p * 0.15f).toInt().toString()
                                perKillPrize = "20"
                            },
                            color = VelorixBg,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, VelorixAccent.copy(alpha = 0.4f))
                        ) {
                            Text("50/25/15 Standard", modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), fontSize = 10.sp, color = VelorixAccent)
                        }

                        Surface(
                            modifier = Modifier.clickable {
                                firstPlacePrize = (p * 0.60f).toInt().toString()
                                secondPlacePrize = (p * 0.25f).toInt().toString()
                                thirdPlacePrize = (p * 0.15f).toInt().toString()
                                perKillPrize = "0"
                            },
                            color = VelorixBg,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.4f))
                        ) {
                            Text("60/25/15 Pro", modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), fontSize = 10.sp, color = Color(0xFFFFD54F))
                        }

                        Surface(
                            modifier = Modifier.clickable {
                                firstPlacePrize = p.toInt().toString()
                                secondPlacePrize = "0"
                                thirdPlacePrize = "0"
                                perKillPrize = "0"
                            },
                            color = VelorixBg,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.4f))
                        ) {
                            Text("100% Winner", modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), fontSize = 10.sp, color = Color(0xFF81C784))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Prize Distribution Values (Rank 1, 2, 3 & Per Kill)", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = firstPlacePrize,
                            onValueChange = { firstPlacePrize = it },
                            label = { Text("1st Place (₹)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFFD54F), unfocusedBorderColor = CardVerifyBorder)
                        )
                        OutlinedTextField(
                            value = secondPlacePrize,
                            onValueChange = { secondPlacePrize = it },
                            label = { Text("2nd Place (₹)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFB0BEC5), unfocusedBorderColor = CardVerifyBorder)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = thirdPlacePrize,
                            onValueChange = { thirdPlacePrize = it },
                            label = { Text("3rd Place (₹)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFCD7F32), unfocusedBorderColor = CardVerifyBorder)
                        )
                        OutlinedTextField(
                            value = perKillPrize,
                            onValueChange = { perKillPrize = it },
                            label = { Text("Per Kill (₹)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF5252), unfocusedBorderColor = CardVerifyBorder)
                        )
                    }
                }
            }

            // Gun & Weapon Rules
            item {
                SettingsCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guns & Loadout Rules", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = allowedGuns,
                        onValueChange = { allowedGuns = it },
                        label = { Text("Allowed Weapons") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bannedGuns,
                        onValueChange = { bannedGuns = it },
                        label = { Text("Banned Weapons (e.g. M79, M82B)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF5252), unfocusedBorderColor = CardVerifyBorder)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Gun Skin Attributes Allowed", fontSize = 13.sp, color = Color.White)
                        Switch(checked = gunAttributesAllowed, onCheckedChange = { gunAttributesAllowed = it })
                    }
                }
            }

            // Character Skills & Devices
            item {
                SettingsCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PersonOutline, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Character Skills & Device Rules", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Character Skills Enabled", fontSize = 13.sp, color = Color.White)
                        Switch(checked = characterSkillsAllowed, onCheckedChange = { characterSkillsAllowed = it })
                    }
                    if (characterSkillsAllowed) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bannedSkills,
                            onValueChange = { bannedSkills = it },
                            label = { Text("Banned Skills (e.g. Chrono, Dimitry)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF5252), unfocusedBorderColor = CardVerifyBorder)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Emulator / PC Players Allowed", fontSize = 13.sp, color = Color.White)
                        Switch(checked = emulatorAllowed, onCheckedChange = { emulatorAllowed = it })
                    }
                }
            }

            // Schedule
            item {
                SettingsCard {
                    Text("Schedule & Timings", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary, modifier = Modifier.padding(bottom = 10.dp))
                    Box {
                        OutlinedTextField(
                            value = startsAt,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Start Date / Time") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Click to pick date & time") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                        )
                        Box(modifier = Modifier.matchParentSize().clickable {
                            val cal = Calendar.getInstance()
                            DatePickerDialog(context, { _, y, m, d ->
                                TimePickerDialog(context, { _, h, min ->
                                    cal.set(y, m, d, h, min, 0)
                                    startsAt = dateFormatter.format(cal.time)
                                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                        })
                    }
                }
            }

            // Description & Rules
            item {
                SettingsCard {
                    Text("Description & Rules", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary, modifier = Modifier.padding(bottom = 10.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Overview Description") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = rules,
                        onValueChange = { rules = it },
                        label = { Text("Official Rules") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                    )
                }
            }

            item {
                val pPool = prizePool.toFloatOrNull() ?: 1000f
                Button(
                    onClick = {
                        isSaving = true
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
                            else -> "BR"
                        }
                        val finalBanner = when {
                            bannerUrl.isNotBlank() && (bannerUrl.startsWith("http://") || bannerUrl.startsWith("https://") || bannerUrl.startsWith("data:image/")) -> {
                                bannerUrl.trim()
                            }
                            localBannerUri != null -> {
                                TournamentImageUploader.encodeUriToBase64(context, localBannerUri!!)
                                    ?: TournamentBannerPresets.PRESETS[0].url
                            }
                            bannerUrl.isNotBlank() -> bannerUrl.trim()
                            else -> TournamentBannerPresets.PRESETS[0].url
                        }
                        val defaultSchedule = dateFormatter.format(Date(System.currentTimeMillis() + 3600_000L))
                        val finalSchedule = startsAt.ifBlank { defaultSchedule }

                        var p1 = firstPlacePrize.toFloatOrNull() ?: (pPool * 0.50f)
                        var p2 = secondPlacePrize.toFloatOrNull() ?: (pPool * 0.25f)
                        var p3 = thirdPlacePrize.toFloatOrNull() ?: (pPool * 0.15f)
                        if (pPool <= 0f) {
                            p1 = 0f
                            p2 = 0f
                            p3 = 0f
                        } else if (p1 + p2 + p3 > pPool) {
                            val ratio = pPool / (p1 + p2 + p3)
                            p1 = (p1 * ratio).toInt().toFloat()
                            p2 = (p2 * ratio).toInt().toFloat()
                            p3 = (p3 * ratio).toInt().toFloat()
                        }

                        val newTournament = Tournament(
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
                        onCreate(newTournament)
                        Toast.makeText(context, "Tournament published to Firebase!", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isSaving && !isUploadingBanner
                ) {
                    if (isUploadingBanner) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("UPLOADING BANNER...", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    } else if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PUBLISHING TO CLOUD...", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CREATE & FINISH", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // CREATE & ADD ANOTHER (+) BUTTON
                OutlinedButton(
                    onClick = {
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
                            else -> category.trim().uppercase()
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

                        val newTournament = Tournament(
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
                        onCreate(newTournament)
                        Toast.makeText(context, "✓ '${newTournament.title}' created! Schedule next match below.", Toast.LENGTH_SHORT).show()

                        // Advance schedule time by 1 hour for next tournament
                        try {
                            val currentCal = Calendar.getInstance()
                            val parsed = if (startsAt.isNotBlank()) dateFormatter.parse(startsAt) else null
                            if (parsed != null) currentCal.time = parsed
                            else currentCal.add(Calendar.HOUR_OF_DAY, 1)
                            currentCal.add(Calendar.HOUR_OF_DAY, 1)
                            startsAt = dateFormatter.format(currentCal.time)
                        } catch (_: Exception) {}

                        // Update title for next match
                        title = if (title.contains("Match", ignoreCase = true)) {
                            val num = Regex("\\d+").find(title)?.value?.toIntOrNull()
                            if (num != null) title.replace(num.toString(), (num + 1).toString()) else "$title (Next)"
                        } else {
                            "$title (Slot 2)"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, VelorixAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    enabled = !isSaving && !isUploadingBanner
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CREATE & ADD ANOTHER (+)", color = VelorixAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            }
        }
    }

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add Custom Free Fire Category", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("This category will immediately be saved to Firebase RTDB and Cloud Firestore so both Admin and Player panels have it.", fontSize = 12.sp, color = VelorixTextSecondary)
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { 
                            newCatName = it
                            if (newCatCode.isBlank()) newCatCode = it.trim().uppercase().replace(" ", "_")
                        },
                        label = { Text("Category Name *") },
                        placeholder = { Text("e.g. Bermuda Championship") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                    )
                    OutlinedTextField(
                        value = newCatCode,
                        onValueChange = { newCatCode = it.trim().uppercase().replace(" ", "_") },
                        label = { Text("Category ID/Code *") },
                        placeholder = { Text("e.g. BERMUDA_CHAMP") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                    )
                    OutlinedTextField(
                        value = newCatDescription,
                        onValueChange = { newCatDescription = it },
                        label = { Text("Description") },
                        placeholder = { Text("Official tournament category") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                    )
                    OutlinedTextField(
                        value = newCatMaxPlayers,
                        onValueChange = { newCatMaxPlayers = it },
                        label = { Text("Default Max Players") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
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
                            Toast.makeText(context, "Category '${newCategory.name}' saved to Firebase!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent)
                ) {
                    Text("Save to Firebase", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF181820)
        )
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardVerifyBg),
        border = BorderStroke(1.dp, CardVerifyBorder)
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
        color = VelorixBg,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, VelorixAccent.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            fontSize = 10.sp,
            color = VelorixAccent,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun QuickTemplateChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E2235),
        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.45f))
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

