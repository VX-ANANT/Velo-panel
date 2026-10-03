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
import com.example.domain.model.FreeFireCategories
import com.example.domain.model.Tournament
import com.example.domain.model.TournamentBannerPresets
import com.example.ui.common.GameLogoBadge
import com.example.ui.theme.*
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
                    val localFile = withContext(Dispatchers.IO) {
                        val tempFile = File(context.cacheDir, "camera_banner_${System.currentTimeMillis()}.jpg")
                        FileOutputStream(tempFile).use { out ->
                            val targetH = (800f * bitmap.height / bitmap.width).toInt().coerceAtLeast(1)
                            val scaled = Bitmap.createScaledBitmap(bitmap, 800, targetH, true)
                            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        }
                        tempFile
                    }
                    val fileUri = Uri.fromFile(localFile)
                    localBannerUri = fileUri
                    bannerStatusMessage = "Uploading to Cloud..."

                    withContext(Dispatchers.IO) {
                        try {
                            val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance()
                                .reference.child("tournament_banners/${UUID.randomUUID()}.jpg")
                            storageRef.putFile(fileUri).await()
                            val downloadUrl = storageRef.downloadUrl.await().toString()
                            withContext(Dispatchers.Main) {
                                bannerUrl = downloadUrl
                                isUploadingBanner = false
                                bannerStatusMessage = "Cloud upload complete ✓"
                                Toast.makeText(context, "Photo uploaded as banner!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.w("CreateTournament", "Storage upload fallback: ${e.message}")
                            withContext(Dispatchers.Main) {
                                bannerUrl = fileUri.toString()
                                isUploadingBanner = false
                                bannerStatusMessage = "Photo saved locally"
                                Toast.makeText(context, "Photo attached as banner", Toast.LENGTH_SHORT).show()
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
                bannerStatusMessage = "Loading image from gallery..."
                try {
                    val localFile = withContext(Dispatchers.IO) {
                        val tempFile = File(context.cacheDir, "banner_${System.currentTimeMillis()}.jpg")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            val bytes = input.readBytes()
                            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

                            var sampleSize = 1
                            val maxDim = 1200
                            while ((boundsOptions.outWidth / sampleSize) > maxDim || (boundsOptions.outHeight / sampleSize) > maxDim) {
                                sampleSize *= 2
                            }

                            val decodeOptions = BitmapFactory.Options().apply {
                                inSampleSize = sampleSize
                                inPreferredConfig = Bitmap.Config.ARGB_8888
                            }
                            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                            if (decoded != null) {
                                FileOutputStream(tempFile).use { out ->
                                    decoded.compress(Bitmap.CompressFormat.JPEG, 85, out)
                                }
                                tempFile
                            } else {
                                null
                            }
                        }
                    }

                    if (localFile != null && localFile.exists()) {
                        val fileUri = Uri.fromFile(localFile)
                        localBannerUri = fileUri
                        bannerStatusMessage = "Uploading to Cloud Storage..."

                        withContext(Dispatchers.IO) {
                            try {
                                val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance()
                                    .reference.child("tournament_banners/${UUID.randomUUID()}.jpg")
                                storageRef.putFile(fileUri).await()
                                val downloadUrl = storageRef.downloadUrl.await().toString()
                                withContext(Dispatchers.Main) {
                                    bannerUrl = downloadUrl
                                    isUploadingBanner = false
                                    bannerStatusMessage = "Cloud upload complete ✓"
                                    Toast.makeText(context, "Tournament banner uploaded to Cloud!", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.w("CreateTournament", "Storage upload fallback: ${e.message}")
                                withContext(Dispatchers.Main) {
                                    bannerUrl = fileUri.toString()
                                    isUploadingBanner = false
                                    bannerStatusMessage = "Saved locally"
                                    Toast.makeText(context, "Banner image attached", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        isUploadingBanner = false
                        bannerStatusMessage = null
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                    isUploadingBanner = false
                    bannerStatusMessage = "Error reading image"
                    Toast.makeText(context, "Error reading image: ${t.message}", Toast.LENGTH_SHORT).show()
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
                            val displayBanner = localBannerUri ?: bannerUrl.ifBlank { null }
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

                        val cleanCategory = when {
                            category.equals("CS", true) || category.contains("CLASH", true) || cleanFormat.contains("CS", true) -> "CS"
                            category.equals("LONE_WOLF", true) || category.contains("LONE", true) || category.contains("1v1", true) || cleanFormat.contains("1v1", true) -> "LONE_WOLF"
                            category.equals("SCRIMS", true) || category.contains("SCRIM", true) -> "SCRIMS"
                            else -> "BR"
                        }
                        val finalBanner = if (bannerUrl.isNotBlank() && (bannerUrl.startsWith("http://") || bannerUrl.startsWith("https://"))) {
                            bannerUrl.trim()
                        } else {
                            TournamentBannerPresets.PRESETS[0].url
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

