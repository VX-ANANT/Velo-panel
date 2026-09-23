package com.example.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
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
import com.example.domain.model.FreeFireCategories
import com.example.domain.model.Tournament
import com.example.domain.model.TournamentBannerPresets
import com.example.ui.common.GameLogoBadge
import com.example.ui.theme.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTournamentScreen(
    onNavigateBack: () -> Unit,
    onCreate: (Tournament) -> Unit
) {
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
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val outputStream = ByteArrayOutputStream()
                val scaled = Bitmap.createScaledBitmap(bitmap, 800, (800f * bitmap.height / bitmap.width).toInt(), true)
                scaled.compress(Bitmap.CompressFormat.JPEG, 82, outputStream)
                val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                bannerUrl = "data:image/jpeg;base64,$base64"
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                val outputStream = ByteArrayOutputStream()
                val maxDim = 900
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val (w, h) = if (ratio >= 1) {
                    val targetW = minOf(maxDim, bitmap.width)
                    Pair(targetW, (targetW / ratio).toInt())
                } else {
                    val targetH = minOf(maxDim, bitmap.height)
                    Pair((targetH * ratio).toInt(), targetH)
                }
                val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
                scaled.compress(Bitmap.CompressFormat.JPEG, 82, outputStream)
                val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                bannerUrl = "data:image/jpeg;base64,$base64"
            } catch (e: Exception) {
                bannerUrl = uri.toString()
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
                        if (bannerUrl.isNotBlank()) {
                            Surface(
                                modifier = Modifier.clickable { bannerUrl = "" },
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

                    // Live Card Banner Preview
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F12)),
                        border = BorderStroke(1.dp, if (bannerUrl.isNotBlank()) VelorixAccent else CardVerifyBorder)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (bannerUrl.isNotBlank()) {
                                AsyncImage(
                                    model = bannerUrl,
                                    contentDescription = "Tournament Card Banner Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
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
                                        text = if (bannerUrl.isNotBlank()) "Live Banner Image Attached" else "Default theme gradient active",
                                        color = if (bannerUrl.isNotBlank()) VelorixAccentLight else VelorixTextSecondary,
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
                            onClick = { galleryLauncher.launch("image/*") },
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
                        value = bannerUrl,
                        onValueChange = { bannerUrl = it },
                        label = { Text("Or Paste Banner Image URL") },
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

            // Tournament Category Selector
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
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = VelorixAccent.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, VelorixAccent)
                        ) {
                            Text(
                                text = FreeFireCategories.getCategoryMeta(category).shortName,
                                color = VelorixAccentLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        "Synchronizes immediately into player category tabs (BR, Clash Squad, Lone Wolf, Scrims).",
                        fontSize = 11.sp,
                        color = VelorixTextSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    // 4 Category selection cards / chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FreeFireCategories.ALL_CATEGORIES.forEach { catMeta ->
                            val isSelected = category == catMeta.key
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        category = catMeta.key
                                        // Auto-populate sensible defaults
                                        if (mapName == "Bermuda" || mapName.isBlank()) {
                                            mapName = catMeta.defaultMaps.firstOrNull() ?: "Bermuda"
                                        }
                                        format = catMeta.defaultFormats.firstOrNull() ?: "SOLO"
                                        maxPlayers = catMeta.defaultMaxPlayers.toString()
                                        if (title.isBlank() || title.contains("Free Fire", ignoreCase = true)) {
                                            title = "Free Fire ${catMeta.label} Tournament"
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
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
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
                                        text = catMeta.defaultFormats.firstOrNull() ?: "",
                                        color = if (isSelected) Color(0xFF69F0AE) else VelorixTextSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
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

                        val newTournament = Tournament(
                            id = SecuritySanitizer.sanitizeDatabaseKey(UUID.randomUUID().toString()),
                            title = cleanTitle,
                            bannerUrl = bannerUrl.trim(),
                            game = cleanGame,
                            category = category,
                            map = cleanMap,
                            format = cleanFormat,
                            status = status,
                            entryFee = entryFee.toFloatOrNull() ?: 0f,
                            prizePool = pPool,
                            firstPlacePrize = firstPlacePrize.toFloatOrNull() ?: (pPool * 0.50f),
                            secondPlacePrize = secondPlacePrize.toFloatOrNull() ?: (pPool * 0.25f),
                            thirdPlacePrize = thirdPlacePrize.toFloatOrNull() ?: (pPool * 0.15f),
                            perKillPrize = perKillPrize.toFloatOrNull() ?: 0f,
                            maxPlayers = maxPlayers.toIntOrNull() ?: 48,
                            registeredPlayers = 0,
                            startsAt = startsAt.ifBlank { null },
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
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                    } else {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CREATE & PUBLISH TOURNAMENT", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
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
