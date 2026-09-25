package com.example.ui

import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.Tournament
import com.example.domain.model.UserProfile
import com.example.data.validation.UserRateLimiter
import com.example.ui.theme.*
import com.example.ui.common.*
import com.example.ui.viewmodel.DashboardState
import com.example.ui.audio.rememberVelorixSoundManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElaborateScreen(title: String, onNavigateBack: () -> Unit, content: @Composable (PaddingValues) -> Unit) {
    GlassBackgroundBox {
        Scaffold(
            topBar = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .border(1.dp, GlassTokens.GlassBorderSubtle, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent
                ) {
                    Box(modifier = Modifier.background(GlassTokens.GlassSurfacePrimary)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable { onNavigateBack() },
                                color = Color(0x332A1F45),
                                border = BorderStroke(1.dp, GlassTokens.GlassBorderSubtle)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = VelorixAccentLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { padding ->
            content(padding)
        }
    }
}

@Composable
fun TournamentsListScreenContent(
    uiState: DashboardState,
    onTournamentClick: ((String) -> Unit)? = null,
    onSettingsClick: ((String) -> Unit)? = null,
    onBracketClick: ((String) -> Unit)? = null,
    onExportJson: (() -> Unit)? = null,
    onImportJson: ((String) -> Unit)? = null,
    onSyncCloud: (() -> Unit)? = null
) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    val tournamentsList = (uiState as? DashboardState.Success)?.tournaments ?: emptyList()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var extractedJsonText by remember { mutableStateOf("") }
    var syncStatusMessage by remember { mutableStateOf<String?>(null) }
    
    val filteredList = remember(tournamentsList, selectedTab, selectedCategory, searchQuery) {
        tournamentsList.filter { t ->
            val matchesTab = when (selectedTab) {
                1 -> t.status.equals("upcoming", ignoreCase = true)
                2 -> t.status.equals("live", ignoreCase = true)
                3 -> t.status.equals("ended", ignoreCase = true) || t.status.equals("completed", ignoreCase = true)
                4 -> t.status.equals("cancelled", ignoreCase = true)
                else -> true
            }
            val matchesCategory = selectedCategory == "ALL" || t.canonicalCategory.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    t.title.contains(searchQuery, ignoreCase = true) ||
                    t.game.contains(searchQuery, ignoreCase = true) ||
                    t.map.contains(searchQuery, ignoreCase = true) ||
                    t.canonicalCategory.contains(searchQuery, ignoreCase = true)
            matchesTab && matchesCategory && matchesQuery
        }
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("All Tournaments", color = VelorixTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            
            // Fast Extraction & Push Action Pill
            Surface(
                color = Color(0xFF18181B),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF27272A))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val json = org.json.JSONArray().apply {
                                tournamentsList.forEach { t ->
                                    put(org.json.JSONObject().apply {
                                        put("id", t.id)
                                        put("tournamentId", t.id)
                                        put("title", t.title)
                                        put("game", t.game)
                                        put("map", t.map)
                                        put("format", t.format)
                                        put("status", t.status)
                                        put("prizePool", t.prizePool)
                                        put("entryFee", t.entryFee)
                                        put("registeredPlayers", t.registeredPlayers)
                                        put("maxPlayers", t.maxPlayers)
                                        put("bannerUrl", t.bannerUrl)
                                    })
                                }
                            }.toString(2)
                            extractedJsonText = json
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("TournamentsJSON", json))
                            showExportDialog = true
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Export JSON", tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                    
                    IconButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Import / Push JSON", tint = Color(0xFF38BDF8), modifier = Modifier.size(15.dp))
                    }

                    val isCloudSyncing = (uiState as? DashboardState.Success)?.isSyncing ?: false
                    IconButton(
                        onClick = {
                            syncStatusMessage = "Extracting live tournaments directly from Firebase backend..."
                            onSyncCloud?.invoke()
                        },
                        modifier = Modifier.size(28.dp).bounceClick(scaleDown = 0.90f)
                    ) {
                        if (isCloudSyncing) {
                            CircularProgressIndicator(
                                color = Color(0xFF4ADE80),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.CloudSync,
                                contentDescription = "Extract & Sync Backend",
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
        
        if (syncStatusMessage != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                color = Color(0xFF102A1E),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF166534))
            ) {
                Text(
                    text = syncStatusMessage ?: "",
                    color = Color(0xFF86EFAC),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search tournament, map, or game...", color = Color.Gray, fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = VelorixAccent,
                unfocusedBorderColor = CardVerifyBorder
            )
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = VelorixAccent,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = VelorixAccent
                    )
                }
            }
        ) {
            listOf("All", "Upcoming", "Live", "Completed", "Cancelled").forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, color = if (selectedTab == index) VelorixAccent else VelorixTextSecondary, fontWeight = FontWeight.Bold) }
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf(
                "ALL" to "All Categories",
                "BR" to "Battle Royale",
                "CS" to "Clash Squad",
                "LONE_WOLF" to "Lone Wolf",
                "SCRIMS" to "Scrims"
            )
            categories.forEach { (catKey, catLabel) ->
                val isSelected = selectedCategory == catKey
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = catKey },
                    label = {
                        Text(
                            catLabel,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VelorixAccent.copy(alpha = 0.25f),
                        selectedLabelColor = VelorixAccentLight,
                        containerColor = Color(0xFF161224),
                        labelColor = VelorixTextSecondary
                    ),
                    border = BorderStroke(1.dp, if (isSelected) VelorixAccent else CardVerifyBorder)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 90.dp)) {
            if (filteredList.isEmpty()) {
                item {
                    Text("No tournaments found matching criteria.", color = VelorixTextSecondary, modifier = Modifier.padding(16.dp))
                }
            }
            items(filteredList.size) { i ->
                val t = filteredList[i]
                val statusColor = when (t.status.uppercase()) {
                    "UPCOMING" -> Color(0xFF64B5F6)
                    "LIVE" -> Color(0xFFFF5252)
                    "COMPLETED", "ENDED" -> Color(0xFF81C784)
                    "CANCELLED" -> Color(0xFFFF8A80)
                    else -> VelorixAccent
                }
                val borderBrush = if (t.status.equals("CANCELLED", ignoreCase = true)) {
                    androidx.compose.ui.graphics.SolidColor(Color(0xFFFF5252).copy(alpha = 0.6f))
                } else {
                    GlassTokens.GlassBorderSubtle
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    borderBrush = borderBrush,
                    onClick = {
                        if (onTournamentClick != null) onTournamentClick(t.id)
                        else onSettingsClick?.invoke(t.id)
                    }
                ) {
                    if (t.bannerUrl.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp, max = 140.dp)
                                .aspectRatio(16f / 9f, matchHeightConstraintsFirst = false)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = t.bannerUrl,
                                contentDescription = t.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color(0x660E0919),
                                                Color(0xEE0E0919)
                                            )
                                        )
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
                            Text(
                                text = t.title,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${t.game} • ${t.canonicalCategory} • ${t.format} • Map: ${t.map}",
                                color = VelorixTextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = VelorixAccent.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, VelorixAccent.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = t.canonicalCategory,
                                    color = VelorixAccentLight,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }

                            val isMoneyTournament = (t.entryFee > 0f) || (t.prizePool > 0.0)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isMoneyTournament) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, if (isMoneyTournament) Color(0xFFF59E0B).copy(alpha = 0.4f) else Color(0xFF10B981).copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isMoneyTournament) Icons.Default.Lock else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isMoneyTournament) Color(0xFFFBBF24) else Color(0xFF34D399),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isMoneyTournament) "18+ CASH" else "TRAINING",
                                        color = if (isMoneyTournament) Color(0xFFFBBF24) else Color(0xFF34D399),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = statusColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = t.status.uppercase(),
                                    color = statusColor,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    if (t.status.equals("CANCELLED", ignoreCase = true) && t.cancellationReason.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0x66331515),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0x66FF5252))
                        ) {
                            Text(
                                text = "Cancelled: ${t.cancellationReason}",
                                color = Color(0xFFFF8A80),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Responsive Data Strip (handles large amounts cleanly with weights)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF09090B),
                        border = BorderStroke(1.dp, Color(0xFF27272A))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Prize Pool", color = VelorixTextSecondary, fontSize = 9.5.sp)
                                Text("₹${t.prizePool.toInt()}", color = VelorixAccentLight, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Entry Fee", color = VelorixTextSecondary, fontSize = 9.5.sp)
                                Text(if (t.entryFee > 0f) "₹${t.entryFee.toInt()}" else "FREE", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("Slots Filled", color = VelorixTextSecondary, fontSize = 9.5.sp)
                                Text("${t.registeredPlayers}/${t.maxPlayers}", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassButton(
                            onClick = { 
                                if (onTournamentClick != null) onTournamentClick(t.id)
                                else onSettingsClick?.invoke(t.id)
                            },
                            modifier = Modifier.weight(1f),
                            text = "DETAILS & RULES"
                        )
                        GlassOutlinedButton(
                            onClick = { onSettingsClick?.invoke(t.id) },
                            modifier = Modifier.weight(1f),
                            text = "ADMIN EDIT"
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(115.dp))
            }
        }
    }

    // Export JSON Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Extracted Tournaments JSON", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    Text("All ${tournamentsList.size} tournaments exported and copied to clipboard.", fontSize = 12.sp, color = VelorixTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = extractedJsonText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF27272A),
                            unfocusedBorderColor = Color(0xFF27272A)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showExportDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF121214)
        )
    }

    // Import / Push JSON Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Push Tournaments JSON", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    Text("Paste a single tournament or array of tournaments JSON to instantly push to Realtime Database & Firestore:", fontSize = 12.sp, color = VelorixTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("Paste JSON here...", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = VelorixAccent,
                            unfocusedBorderColor = Color(0xFF27272A)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonText.isNotBlank()) {
                            onImportJson?.invoke(importJsonText)
                            syncStatusMessage = "JSON pushed to Cloud endpoints!"
                            showImportDialog = false
                            importJsonText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Push To Cloud", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = VelorixTextSecondary)
                }
            },
            containerColor = Color(0xFF121214)
        )
    }
}

@Composable
fun AnalyticsScreen(
    uiState: DashboardState = DashboardState.Loading,
    onNavigateBack: () -> Unit
) {
    val successState = uiState as? DashboardState.Success
    val usersCount = successState?.totalRegisteredUsersCount ?: 0
    val tourneys = successState?.tournaments ?: emptyList()
    val totalPrizePool = tourneys.sumOf { it.prizePool.toDouble() }
    val totalRegistrations = tourneys.sumOf { it.registeredPlayers }
    val pendingCashouts = successState?.pendingCashoutsSum ?: 0.0
    val openTickets = successState?.openSupportComplaintsCount ?: 0
    val activeAdmins = successState?.totalActiveAdminsCount ?: 0

    ElaborateScreen("Live Platform Analytics", onNavigateBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("REALTIME METRICS SUMMARY", color = VelorixTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AnalyticsCard(modifier = Modifier.weight(1f), title = "Total Registered Players", value = "$usersCount")
                    AnalyticsCard(modifier = Modifier.weight(1f), title = "Active Tournaments", value = "${tourneys.size}")
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AnalyticsCard(modifier = Modifier.weight(1f), title = "Total Tournament Prize Pools", value = "₹${totalPrizePool.toInt()}")
                    AnalyticsCard(modifier = Modifier.weight(1f), title = "Total Player Enrollments", value = "$totalRegistrations")
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AnalyticsCard(modifier = Modifier.weight(1f), title = "Pending Cashouts", value = "₹${pendingCashouts.toInt()}")
                    AnalyticsCard(modifier = Modifier.weight(1f), title = "Open Support Complaints", value = "$openTickets")
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AnalyticsCard(modifier = Modifier.weight(1f), title = "Verified Staff Admins", value = "$activeAdmins")
                    AnalyticsCard(modifier = Modifier.weight(1f), title = "Pending Verification", value = "${successState?.pendingRegistrationsCount ?: 0}")
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AnalyticsCard(modifier: Modifier = Modifier, title: String, value: String) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        borderBrush = GlassTokens.GlassBorderSubtle
    ) {
        Text(title, color = VelorixTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun GlobalSettingsScreenContent(
    opticsManager: LiquidGlassOpticsManager? = null,
    accentColor: Color = VelorixAccent,
    currentTheme: com.example.ui.common.HyperOSTheme = com.example.ui.common.HyperOSTheme.NEBULA_PURPLE,
    onThemeSelect: (com.example.ui.common.HyperOSTheme) -> Unit = {},
    onPurgeDemoData: (() -> Unit)? = null,
    currentUserEmail: String? = null,
    onLogout: (() -> Unit)? = null,
    onNavigateTab: ((String) -> Unit)? = null,
    onOpenAudioLab: (() -> Unit)? = null,
    onOpenApiKeyDialog: (() -> Unit)? = null,
    onOpenRateLimiter: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { com.example.data.repository.TournamentRepositoryImpl(context) }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val soundManager = rememberVelorixSoundManager()

    var showPurgeConfirmationDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmationDialog by remember { mutableStateOf(false) }
    var showRtdbRulesDialog by remember { mutableStateOf(false) }
    var showFirestoreRulesDialog by remember { mutableStateOf(false) }
    var isSyncingRls by remember { mutableStateOf(false) }
    var rlsFeedbackMessage by remember { mutableStateOf<String?>(null) }

    if (showRtdbRulesDialog) {
        val rtdbRules = remember { repository.getProductionRtdbRules() }
        AlertDialog(
            onDismissRequest = { showRtdbRulesDialog = false },
            containerColor = Color(0xFF18181B),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = VelorixAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Realtime Database Security Rules", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Paste this JSON into Firebase Console -> Realtime Database -> Rules. This protects slot locks and room details while giving admin full authority.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF09090B),
                        border = BorderStroke(1.dp, Color(0xFF27272A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp)
                        ) {
                            Text(
                                text = rtdbRules,
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        soundManager.playSnapPop()
                        clipboardManager.setText(AnnotatedString(rtdbRules))
                        Toast.makeText(context, "RTDB Rules copied to clipboard!", Toast.LENGTH_SHORT).show()
                        showRtdbRulesDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Rules", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRtdbRulesDialog = false }) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }

    if (showFirestoreRulesDialog) {
        val firestoreRules = remember { repository.getProductionFirestoreRules() }
        AlertDialog(
            onDismissRequest = { showFirestoreRulesDialog = false },
            containerColor = Color(0xFF18181B),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalPolice, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Firestore Security Rules", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Paste this into Firebase Console -> Firestore Database -> Rules. Enforces Row-Level Security for wallets and user data.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF09090B),
                        border = BorderStroke(1.dp, Color(0xFF27272A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp)
                        ) {
                            Text(
                                text = firestoreRules,
                                color = Color(0xFF34D399),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        soundManager.playSnapPop()
                        clipboardManager.setText(AnnotatedString(firestoreRules))
                        Toast.makeText(context, "Firestore Rules copied to clipboard!", Toast.LENGTH_SHORT).show()
                        showFirestoreRulesDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Rules", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFirestoreRulesDialog = false }) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }

    if (showLogoutConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmationDialog = false },
            containerColor = Color(0xFF18181B),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out Admin", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "Are you sure you want to end your administrator session? You will be signed out of Firebase and returned to the login screen.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmationDialog = false
                        onLogout?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmationDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    if (showPurgeConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeConfirmationDialog = false },
            containerColor = Color(0xFF18181B),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clean Mock Data", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "This will clear temporary test mock tournaments and dummy records from Firebase, resetting to clean live production data.\n\nYour admin credentials and real users remain safe.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        soundManager.playOrchestraHit()
                        showPurgeConfirmationDialog = false
                        onPurgeDemoData?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Confirm Wipe", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeConfirmationDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 120.dp)
    ) {
        // Page Header
        item {
            Column(modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)) {
                Text(
                    text = "Settings",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "System preferences, audio feedback & admin control panel",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.5.sp
                )
            }
        }

        // Section 1: Super Admin Profile Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF141416),
                border = BorderStroke(1.dp, Color(0xFF27272A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(accentColor, Color(0xFF6366F1))
                                ),
                                CircleShape
                            )
                            .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (currentUserEmail?.firstOrNull() ?: 'A').uppercase(),
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Super Administrator", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF102A1E),
                                border = BorderStroke(0.5.dp, Color(0xFF166534))
                            ) {
                                Text(
                                    "ONLINE",
                                    color = Color(0xFF86EFAC),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(currentUserEmail ?: "anantisback47@gmail.com", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF22C55E), CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Firebase RTDB & Firestore Connected", color = Color(0xFF86EFAC), fontSize = 10.5.sp)
                        }
                    }
                }
            }
        }

        // Section 2: Audio & Sound Effects
        item {
            SettingsSectionHeader("AUDIO & FEEDBACK")
            var sfxMuted by remember { mutableStateOf(soundManager.isMuted) }
            var sfxVol by remember { mutableFloatStateOf(soundManager.masterVolume) }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF141416),
                border = BorderStroke(1.dp, Color(0xFF27272A))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(0xFF202024), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("In-App Sound Effects", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("Tactile button taps, snaps & alerts", color = Color(0xFF94A3B8), fontSize = 11.5.sp)
                            }
                        }
                        Switch(
                            checked = !sfxMuted,
                            onCheckedChange = { active ->
                                sfxMuted = !active
                                soundManager.isMuted = !active
                                if (active) soundManager.playSnapPop()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF2563EB),
                                uncheckedTrackColor = Color(0xFF27272A)
                            )
                        )
                    }

                    if (!sfxMuted) {
                        HorizontalDivider(color = Color(0xFF27272A), thickness = 0.5.dp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Volume", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Slider(
                                value = sfxVol,
                                onValueChange = { newVol ->
                                    sfxVol = newVol
                                    soundManager.masterVolume = newVol
                                },
                                onValueChangeFinished = { soundManager.playBeatTap() },
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF2563EB),
                                    inactiveTrackColor = Color(0xFF27272A)
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("${(sfxVol * 100).toInt()}%", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Color(0xFF27272A), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                soundManager.playBeatTap()
                                onOpenAudioLab?.invoke()
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Headphones, contentDescription = null, tint = Color(0xFFD49A3D), modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("SFX Audio Lab (Test Sound Effects)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }

        // Section 3: Appearance & Accent Theme
        item {
            SettingsSectionHeader("APPEARANCE & THEME")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF141416),
                border = BorderStroke(1.dp, Color(0xFF27272A))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Theme Palette", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Subtle ambient accents for surfaces and active tabs", color = Color(0xFF94A3B8), fontSize = 11.5.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.ui.common.HyperOSTheme.values().forEach { theme ->
                            val isSelected = currentTheme == theme
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) theme.primaryColor.copy(alpha = 0.25f)
                                        else Color(0xFF1C1C20)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) theme.primaryColor else Color(0xFF2A2A30),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        soundManager.playSnapPop()
                                        onThemeSelect(theme)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(theme.primaryColor)
                                    )
                                    Text(
                                        text = theme.themeName.replace("Hyper ", ""),
                                        color = if (isSelected) Color.White else Color(0xFFD4D4D8),
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Tournament Defaults & Match Rules (Grouped Card)
        item {
            SettingsSectionHeader("TOURNAMENT POLICIES")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF141416),
                border = BorderStroke(1.dp, Color(0xFF27272A))
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsGroupedToggle(
                        label = "Direct Player Registration",
                        subtitle = "Players can join without prior admin check",
                        initial = true,
                        onSound = { soundManager.playSnapPop() }
                    )
                    HorizontalDivider(color = Color(0xFF27272A), thickness = 0.5.dp)
                    SettingsGroupedToggle(
                        label = "Require ID Verification",
                        subtitle = "Mandatory KYC before cash withdrawals",
                        initial = false,
                        onSound = { soundManager.playSnapPop() }
                    )
                    HorizontalDivider(color = Color(0xFF27272A), thickness = 0.5.dp)
                    SettingsGroupedToggle(
                        label = "Auto-Approve Payouts",
                        subtitle = "Instantly process UPI transfers below ₹1,000",
                        initial = false,
                        onSound = { soundManager.playSnapPop() }
                    )
                    HorizontalDivider(color = Color(0xFF27272A), thickness = 0.5.dp)
                    SettingsGroupedToggle(
                        label = "Match Countdown Alerts",
                        subtitle = "Automated push notification 15m before start",
                        initial = true,
                        onSound = { soundManager.playSnapPop() }
                    )
                    HorizontalDivider(color = Color(0xFF27272A), thickness = 0.5.dp)
                    SettingsGroupedToggle(
                        label = "Support Dispute Notifications",
                        subtitle = "Notify admins on incoming player disputes",
                        initial = true,
                        onSound = { soundManager.playSnapPop() }
                    )
                }
            }
        }

        // Section 5: AI & Gemini Engine
        item {
            SettingsSectionHeader("AI MEDIATOR & INTELLIGENCE")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF141416),
                border = BorderStroke(1.dp, Color(0xFF27272A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF2E1065).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF7C3AED).copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(17.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Gemini AI Mediator", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                            Text("Automated disputes & bracket suggestions", color = Color(0xFF94A3B8), fontSize = 11.5.sp)
                        }
                    }
                    Button(
                        onClick = {
                            soundManager.playBeatTap()
                            onOpenApiKeyDialog?.invoke()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
                        border = BorderStroke(1.dp, Color(0xFF3F3F46)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("API Key", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 6: Administrative Modules
        item {
            SettingsSectionHeader("ADMINISTRATIVE TOOLS")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF141416),
                border = BorderStroke(1.dp, Color(0xFF27272A))
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    SettingsModuleRow(
                        title = "Staff & RBAC Permissions",
                        subtitle = "Manage admin team access and roles",
                        icon = Icons.Default.Shield,
                        onClick = {
                            soundManager.playBeatTap()
                            onNavigateTab?.invoke("admins")
                        }
                    )
                    HorizontalDivider(color = Color(0xFF27272A), thickness = 0.5.dp)
                    SettingsModuleRow(
                        title = "Rules & Match Specs",
                        subtitle = "Gun bans, custom character & device limits",
                        icon = Icons.Default.MilitaryTech,
                        onClick = {
                            soundManager.playBeatTap()
                            onNavigateTab?.invoke("tournament_rules")
                        }
                    )
                    HorizontalDivider(color = Color(0xFF27272A), thickness = 0.5.dp)
                    SettingsModuleRow(
                        title = "Check-In PINs & Banners",
                        subtitle = "Generate match PINs & announcement slides",
                        icon = Icons.Default.DashboardCustomize,
                        onClick = {
                            soundManager.playBeatTap()
                            onNavigateTab?.invoke("lowcode")
                        }
                    )
                    HorizontalDivider(color = Color(0xFF27272A), thickness = 0.5.dp)
                    SettingsModuleRow(
                        title = "Player Leaderboards",
                        subtitle = "Rankings, points tally & Hall of Fame",
                        icon = Icons.Default.EmojiEvents,
                        onClick = {
                            soundManager.playBeatTap()
                            onNavigateTab?.invoke("leaderboard")
                        }
                    )
                    HorizontalDivider(color = Color(0xFF27272A), thickness = 0.5.dp)
                    SettingsModuleRow(
                        title = "Rate Limiter & Quotas",
                        subtitle = "Inspect flood prevention & token buckets",
                        icon = Icons.Default.Speed,
                        onClick = {
                            soundManager.playBeatTap()
                            onOpenRateLimiter?.invoke()
                        }
                    )
                }
            }
        }

        // Section 7: Firebase & Database Security
        item {
            SettingsSectionHeader("FIREBASE RLS & SECURITY")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF141416),
                border = BorderStroke(1.dp, Color(0xFF27272A))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(0xFF0284C7).copy(alpha = 0.15f), CircleShape)
                                    .border(1.dp, Color(0xFF0284C7), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(17.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Database RLS Policies", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                Text("Row-Level Security & Super Admin Auth", color = Color(0xFF94A3B8), fontSize = 11.5.sp)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF0E2E3B),
                            border = BorderStroke(0.5.dp, Color(0xFF0284C7))
                        ) {
                            Text(
                                "ACTIVE RLS",
                                color = Color(0xFF7DD3FC),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    val feedback = rlsFeedbackMessage
                    if (feedback != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = feedback,
                            color = Color(0xFF34D399),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                soundManager.playSnapPop()
                                showRtdbRulesDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RTDB Rules", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                soundManager.playSnapPop()
                                showFirestoreRulesDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.LocalPolice, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Firestore Rules", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                soundManager.playBrassHit()
                                isSyncingRls = true
                                rlsFeedbackMessage = "Syncing Admin RLS claims across Firebase..."
                                val target = currentUserEmail ?: "anantisback47@gmail.com"
                                val ok = repository.syncAdminPermissionsNow(target)
                                isSyncingRls = false
                                rlsFeedbackMessage = if (ok) "Super Admin RLS permissions synced for $target" else "Sync completed. Check console if token refresh is needed."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isSyncingRls,
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        if (isSyncingRls) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("1-Tap Sync Admin Permissions", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }

        // Section 8: Session & Maintenance (Danger Zone)
        item {
            SettingsSectionHeader("SESSION & MAINTENANCE", isDanger = true)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF141416),
                border = BorderStroke(1.dp, Color(0xFF27272A))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reset Mock / Demo Data", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Clean dummy test records from Firebase", color = Color(0xFF94A3B8), fontSize = 11.5.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                soundManager.playSnapPop()
                                showPurgeConfirmationDialog = true
                            },
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clean Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Color(0xFF27272A), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sign Out Session", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Close admin session and return to login", color = Color(0xFF94A3B8), fontSize = 11.5.sp)
                        }
                        Button(
                            onClick = {
                                soundManager.playSnapPop()
                                showLogoutConfirmationDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
                            border = BorderStroke(1.dp, Color(0xFF3F3F46)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sign Out", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, isDanger: Boolean = false) {
    Text(
        text = title,
        color = if (isDanger) Color(0xFFEF4444) else Color(0xFF71717A),
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
    )
}

@Composable
fun SettingsGroupedToggle(
    label: String,
    subtitle: String? = null,
    initial: Boolean = false,
    onSound: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("VelorixAdminSettings", android.content.Context.MODE_PRIVATE) }
    var checked by remember { mutableStateOf(sharedPrefs.getBoolean(label, initial)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val next = !checked
                checked = next
                sharedPrefs.edit().putBoolean(label, next).apply()
                onSound?.invoke()
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = { next ->
                checked = next
                sharedPrefs.edit().putBoolean(label, next).apply()
                onSound?.invoke()
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2563EB),
                uncheckedTrackColor = Color(0xFF27272A)
            )
        )
    }
}

@Composable
fun SettingsModuleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color(0xFF202024), RoundedCornerShape(8.dp))
                .border(0.5.dp, Color(0xFF2E2E34), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF71717A), modifier = Modifier.size(15.dp))
    }
}

@Composable
fun SettingsToggle(label: String, initial: Boolean, subtitle: String? = null) {
    SettingsGroupedToggle(label = label, subtitle = subtitle, initial = initial)
}

@Composable
fun UsersManagementScreenContent(
    uiState: DashboardState,
    onToggleBan: (UserProfile) -> Unit,
    onAddFunds: (UserProfile, Double) -> Unit,
    onDeleteUser: (UserProfile) -> Unit,
    onUpdateUser: (UserProfile) -> Unit = {},
    onRefreshUserData: (() -> Unit)? = null
) {
    val successState = uiState as? DashboardState.Success
    val usersList = successState?.users ?: emptyList()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    var selectedUserForFunds by remember { mutableStateOf<UserProfile?>(null) }
    var fundsAmountInput by remember { mutableStateOf("100") }

    var inspectedUser by remember { mutableStateOf<UserProfile?>(null) }

    var showAddUserDialog by remember { mutableStateOf(false) }
    var newUserEmail by remember { mutableStateOf("") }
    var newUsername by remember { mutableStateOf("") }
    var newUserGameId by remember { mutableStateOf("") }
    var newUserInitialBalance by remember { mutableStateOf("0") }

    // Keep inspected user fresh if data updates in stream
    val activeInspectedUser = remember(usersList, inspectedUser) {
        if (inspectedUser == null) null
        else usersList.find { it.id == inspectedUser?.id } ?: inspectedUser
    }

    val filteredUsers = remember(usersList, searchQuery, selectedFilter) {
        usersList.filter { u ->
            val emailLower = u.email.trim().lowercase()
            val isMock = emailLower.contains("@example.com") || emailLower.contains("mock.dummy")
            if (isMock) return@filter false

            val matchesQuery = searchQuery.isBlank() || 
                u.username.contains(searchQuery, ignoreCase = true) ||
                u.email.contains(searchQuery, ignoreCase = true) ||
                u.gameId.contains(searchQuery, ignoreCase = true) ||
                u.id.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "BANNED" -> u.isBanned
                "ACTIVE" -> !u.isBanned
                "ADMIN" -> u.role.contains("admin", ignoreCase = true)
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    if (showAddUserDialog) {
        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            title = { Text("Register / Sync Player Profile", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Directly add or sync a player account into /users database path.", color = Color.LightGray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = newUserEmail,
                        onValueChange = { newUserEmail = it },
                        label = { Text("Email Address*", color = Color.Gray) },
                        placeholder = { Text("e.g. player@gmail.com") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("Username / IGN", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newUserGameId,
                        onValueChange = { newUserGameId = it },
                        label = { Text("Free Fire / Game ID", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newUserInitialBalance,
                        onValueChange = { newUserInitialBalance = it },
                        label = { Text("Initial Balance (₹)", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUserEmail.isNotBlank()) {
                            val uid = try {
                                com.google.firebase.database.FirebaseDatabase.getInstance().reference.child("users").push().key
                            } catch (_: Exception) {
                                null
                            } ?: "usr_${System.currentTimeMillis()}"
                            val user = UserProfile(
                                id = uid,
                                username = newUsername.ifBlank { newUserEmail.substringBefore("@") },
                                email = newUserEmail.trim(),
                                funds = newUserInitialBalance.toDoubleOrNull() ?: 0.0,
                                gameId = newUserGameId.trim(),
                                createdAt = System.currentTimeMillis(),
                                lastActive = System.currentTimeMillis()
                            )
                            onUpdateUser(user)
                            showAddUserDialog = false
                            newUserEmail = ""
                            newUsername = ""
                            newUserGameId = ""
                            newUserInitialBalance = "0"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent)
                ) {
                    Text("Add Player to DB", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUserDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = CardLiveBg
        )
    }

    if (selectedUserForFunds != null) {
        AlertDialog(
            onDismissRequest = { selectedUserForFunds = null },
            title = { Text("Adjust Wallet Balance", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Adjust VT balance for ${selectedUserForFunds?.username} (${selectedUserForFunds?.email})", color = Color.LightGray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = fundsAmountInput,
                        onValueChange = { fundsAmountInput = it },
                        label = { Text("Amount (₹)", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = VelorixAccent,
                            unfocusedBorderColor = CardVerifyBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = fundsAmountInput.toDoubleOrNull() ?: 0.0
                        if (amount != 0.0) {
                            selectedUserForFunds?.let { onAddFunds(it, amount) }
                        }
                        selectedUserForFunds = null
                        fundsAmountInput = "100"
                    }, 
                    colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent)
                ) {
                    Text("Update Funds", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedUserForFunds = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = CardLiveBg
        )
    }

    // Full User Detail Inspector Dialog
    activeInspectedUser?.let { user ->
        UserDetailsInspectorDialog(
            user = user,
            onDismiss = { inspectedUser = null },
            onToggleBan = { 
                onToggleBan(it)
                inspectedUser = it.copy(isBanned = !it.isBanned)
            },
            onAddFunds = { u, amt -> onAddFunds(u, amt) },
            onUpdateUser = { updatedUser ->
                onUpdateUser(updatedUser)
                inspectedUser = updatedUser
            },
            onDeleteUser = {
                onDeleteUser(it)
                inspectedUser = null
            }
        )
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Responsive Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "User Base & Accounts",
                    color = VelorixTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (onRefreshUserData != null) {
                        val context = LocalContext.current
                        val isUserSyncing = (uiState as? DashboardState.Success)?.isSyncing ?: false
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Extracting live players from Firebase...", Toast.LENGTH_SHORT).show()
                                onRefreshUserData()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VelorixAccent),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VelorixAccent),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.bounceClick(scaleDown = 0.95f)
                        ) {
                            if (isUserSyncing) {
                                CircularProgressIndicator(
                                    color = VelorixAccent,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(12.dp)
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync RTDB", tint = VelorixAccent, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isUserSyncing) "Extracting..." else "Sync RTDB", color = VelorixAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = { showAddUserDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add Player", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap any user to view details & controls",
                    color = VelorixAccent,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(VelorixAccent.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                        .border(1.dp, VelorixAccent.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${usersList.size} Total",
                        color = VelorixAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by ID, Username, Email, Game ID...", color = Color.Gray, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = VelorixAccent,
                unfocusedBorderColor = CardVerifyBorder
            )
        )

        // Filter chips (horizontally scrollable)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp)
        ) {
            listOf("ALL", "ACTIVE", "BANNED", "ADMIN").forEach { f ->
                val isSel = selectedFilter == f
                Box(
                    modifier = Modifier
                        .background(if (isSel) VelorixAccent else CardLiveBg, RoundedCornerShape(8.dp))
                        .clickable { selectedFilter = f }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(f, color = if (isSel) Color.Black else Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        val distinctUsers = remember(filteredUsers) {
            val seen = mutableSetOf<String>()
            filteredUsers.filter { u ->
                val k = if (u.id.isNotBlank()) "id:${u.id}" else "email:${u.email.trim().lowercase()}"
                if (k.isNotBlank() && seen.add(k)) true else if (k.isBlank()) true else false
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            if (distinctUsers.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.PersonSearch, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No matching users found.", color = VelorixTextSecondary, fontSize = 14.sp)
                    }
                }
            }
            items(distinctUsers, key = { "${it.id}_${it.email}_${distinctUsers.indexOf(it)}" }) { user ->
                val userBorderBrush = if (user.isBanned) {
                    androidx.compose.ui.graphics.SolidColor(Color.Red.copy(alpha = 0.6f))
                } else {
                    GlassTokens.GlassBorderSubtle
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    borderBrush = userBorderBrush,
                    onClick = null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { inspectedUser = user },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            val isRecentlyActive = (System.currentTimeMillis() - user.lastActive) < 20 * 60 * 1000
                            UserAvatarView(
                                avatarUrl = user.avatarUrl,
                                name = user.bestDisplayName,
                                size = 48.dp,
                                isBanned = user.isBanned,
                                isSuspended = user.isSuspended,
                                role = user.role,
                                isActiveRecently = isRecentlyActive,
                                showGlow = true
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user.bestDisplayName,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (user.role.contains("admin", ignoreCase = true)) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val roleBadgeColor = if (user.role.contains("super", ignoreCase = true)) Color(0xFFFFB300) else Color(0xFF00E5FF)
                                        Box(
                                            modifier = Modifier
                                                .background(roleBadgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .border(0.5.dp, roleBadgeColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                        ) {
                                            Text(user.role.uppercase(), color = roleBadgeColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        }
                                    }
                                }
                                Text(
                                    text = user.bestEmailOrPhone,
                                    color = VelorixTextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "ID: ${user.id}",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            if (user.isBanned) {
                                Box(modifier = Modifier.background(Color(0xFFFF1744), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                    Text("BANNED", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (user.isSuspended) {
                                Box(modifier = Modifier.background(Color(0xFFFF9800), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                    Text("SUSPENDED", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Box(modifier = Modifier.background(Color(0xFF2E7D32), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                    Text("ACTIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Inspect", color = Color(0xFFA1A1AA), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFA1A1AA), modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF121215), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF27272A), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Free Fire IGN", color = VelorixTextSecondary, fontSize = 9.sp)
                            Text(user.effectiveIgn.ifEmpty { "N/A" }, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column {
                            Text("Wallet Balance", color = VelorixTextSecondary, fontSize = 9.sp)
                            Text("₹${user.totalWalletBalance.toInt()}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        Column {
                            Text("Wins / Kills", color = VelorixTextSecondary, fontSize = 9.sp)
                            Text("${user.wins}W / ${user.kills}K", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                        Column {
                            Text("Total Won", color = VelorixTextSecondary, fontSize = 9.sp)
                            Text("₹${user.totalEarnings.toInt()}", color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }

                    // User Panel Extra Highlights: UID, Tokens, Streak
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (user.gameId.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF1E2330), RoundedCornerShape(6.dp))
                                    .border(0.5.dp, Color(0xFF2D3748), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("FF ID: ${user.gameId}", color = Color(0xFF90CAF9), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (user.tokens > 0) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF2E2415), RoundedCornerShape(6.dp))
                                    .border(0.5.dp, Color(0xFFFFB300).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("🪙 ${user.tokens} Tokens", color = Color(0xFFFFD54F), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (user.loginStreak > 0) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF2D1616), RoundedCornerShape(6.dp))
                                    .border(0.5.dp, Color(0xFFFF5722).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("🔥 ${user.loginStreak}d Streak", color = Color(0xFFFF8A65), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { inspectedUser = user },
                            modifier = Modifier.weight(1f).height(42.dp).bounceClick(scaleDown = 0.96f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F1F23)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3F3F46)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Manage", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                        }

                        Button(
                            onClick = { selectedUserForFunds = user },
                            modifier = Modifier.weight(1f).height(42.dp).bounceClick(scaleDown = 0.96f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x2200E676)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.AddCard, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Funds", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676), maxLines = 1)
                        }

                        Button(
                            onClick = { onToggleBan(user) },
                            modifier = Modifier.weight(1f).height(42.dp).bounceClick(scaleDown = 0.96f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (user.isBanned) Color(0xFF1B5E20) else Color(0xFFB71C1C)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (user.isBanned) Color(0xFF4CAF50) else Color(0xFFEF5350)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(if (user.isBanned) Icons.Default.CheckCircle else Icons.Default.Block, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (user.isBanned) "Unban" else "Ban", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserDetailsInspectorDialog(
    user: UserProfile,
    onDismiss: () -> Unit,
    onToggleBan: (UserProfile) -> Unit,
    onAddFunds: (UserProfile, Double) -> Unit,
    onUpdateUser: (UserProfile) -> Unit,
    onDeleteUser: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) }

    // Editable state
    var editUsername by remember(user) { mutableStateOf(user.username) }
    var editEmail by remember(user) { mutableStateOf(user.email) }
    var editGameId by remember(user) { mutableStateOf(user.gameId) }
    var editPhone by remember(user) { mutableStateOf(user.phone) }
    var editRole by remember(user) { mutableStateOf(user.role) }
    var editWins by remember(user) { mutableStateOf(user.wins.toString()) }
    var editKills by remember(user) { mutableStateOf(user.kills.toString()) }
    var editActivityPoints by remember(user) { mutableStateOf(user.activityPoints.toString()) }
    var editEarnings by remember(user) { mutableStateOf(user.totalEarnings.toString()) }
    var editBalanceInput by remember(user) { mutableStateOf(user.funds.toString()) }

    // Individual Toggles for each ID separately
    var isBannedState by remember(user.id, user.isBanned) { mutableStateOf(user.isBanned) }
    var banReasonInput by remember(user.id, user.banReason) { mutableStateOf(user.banReason.ifBlank { "Violation of tournament guidelines / Cheating" }) }
    var banCaseIdInput by remember(user.id, user.banCaseId) { mutableStateOf(user.banCaseId) }

    var isSuspendedState by remember(user.id, user.isSuspended) { mutableStateOf(user.isSuspended) }
    var suspensionReasonInput by remember(user.id, user.suspensionReason) { mutableStateOf(user.suspensionReason.ifBlank { "Temporary match suspension" }) }
    var selectedSuspensionHours by remember { mutableIntStateOf(24) }

    var isVpnBlockedState by remember(user.id, user.isVpnBlocked) { mutableStateOf(user.isVpnBlocked) }
    var isForceUpdateState by remember(user.id, user.isForceUpdateRequired) { mutableStateOf(user.isForceUpdateRequired) }
    var isMaintenanceBypassState by remember(user.id, user.isMaintenanceBypass) { mutableStateOf(user.isMaintenanceBypass) }
    var minVersionInput by remember(user.id, user.minVersionRequired) { mutableStateOf(user.minVersionRequired) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    fun buildUpdatedUser(): UserProfile {
        val suspendUntilTime = if (isSuspendedState) {
            if (user.suspendedUntil > System.currentTimeMillis()) user.suspendedUntil
            else System.currentTimeMillis() + (selectedSuspensionHours.toLong() * 3600 * 1000)
        } else 0L

        return user.copy(
            username = editUsername.trim().ifBlank { user.username },
            email = editEmail.trim().ifBlank { user.email },
            gameId = editGameId.trim(),
            ign = editUsername.trim().ifBlank { user.ign },
            phone = editPhone.trim(),
            role = editRole,
            wins = editWins.toIntOrNull() ?: user.wins,
            kills = editKills.toIntOrNull() ?: user.kills,
            activityPoints = editActivityPoints.toIntOrNull() ?: user.activityPoints,
            totalEarnings = editEarnings.toDoubleOrNull() ?: user.totalEarnings,
            isBanned = isBannedState,
            banReason = if (isBannedState) banReasonInput.trim() else "",
            banCaseId = if (isBannedState) banCaseIdInput.trim() else "",
            isSuspended = isSuspendedState,
            suspensionReason = if (isSuspendedState) suspensionReasonInput.trim() else "",
            suspendedUntil = suspendUntilTime,
            isVpnBlocked = isVpnBlockedState,
            isForceUpdateRequired = isForceUpdateState,
            minVersionRequired = minVersionInput.trim(),
            isMaintenanceBypass = isMaintenanceBypassState
        )
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val createdStr = remember(user.createdAt) {
        if (user.createdAt > 0) dateFormat.format(Date(user.createdAt)) else "N/A (Early Account)"
    }
    val lastActiveStr = remember(user.lastActive) {
        if (user.lastActive > 0) {
            val formatted = dateFormat.format(Date(user.lastActive))
            val diffMs = System.currentTimeMillis() - user.lastActive
            val diffHours = diffMs / (1000 * 60 * 60)
            val diffDays = diffHours / 24
            val relative = when {
                diffHours < 1 -> "Just now"
                diffHours < 24 -> "${diffHours}h ago"
                else -> "${diffDays}d ago"
            }
            "$formatted ($relative)"
        } else "N/A"
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete User Permanently?", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to permanently delete account ${user.username} (${user.id}) from all database nodes? This action cannot be undone.",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteUser(user)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete Account", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = CardLiveBg
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF14171E)),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardVerifyBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1F29))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        val isRecentlyActive = (System.currentTimeMillis() - user.lastActive) < 20 * 60 * 1000
                        UserAvatarView(
                            avatarUrl = user.avatarUrl,
                            name = user.bestDisplayName,
                            size = 54.dp,
                            isBanned = isBannedState,
                            isSuspended = isSuspendedState,
                            role = editRole,
                            isActiveRecently = isRecentlyActive,
                            showGlow = true
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.bestDisplayName,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            when {
                                                isBannedState -> Color(0xFFFF1744)
                                                isSuspendedState -> Color(0xFFFF9800)
                                                else -> Color(0xFF2E7D32)
                                            },
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = when {
                                            isBannedState -> "BANNED"
                                            isSuspendedState -> "SUSPENDED"
                                            else -> "ACTIVE"
                                        },
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = user.bestEmailOrPhone,
                                color = VelorixTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                // Copy UID Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F1218))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("UID: ", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = user.id,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                    TextButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(user.id))
                            Toast.makeText(context, "User ID copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = VelorixAccent, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy ID", color = VelorixAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Tabs: 0 -> Minor Details, 1 -> Admin Controls, 2 -> Raw DB Attributes
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF161B24),
                    contentColor = VelorixAccent,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = VelorixAccent
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Details & Logins", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Toggles & Controls", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Raw DB Data", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                // Tab Contents
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(14.dp)) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { width -> width / 3 } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> -width / 3 } + fadeOut()
                                )
                            } else {
                                (slideInHorizontally { width -> -width / 3 } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> width / 3 } + fadeOut()
                                )
                            }
                        },
                        label = "dialog_tab_content"
                    ) { currentTab ->
                        when (currentTab) {
                        0 -> {
                            // Minor Details & Account Metadata
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Free Fire Esports & Gaming Profile
                                DetailSectionCard(title = "FREE FIRE ESPORTS & GAMING PROFILE", icon = Icons.Default.SportsEsports) {
                                    DetailItemRow(label = "Free Fire IGN", value = user.effectiveIgn, isHighlight = user.effectiveIgn != "Player")
                                    DetailItemRow(label = "Free Fire Game UID", value = user.gameId.ifBlank { "Not Linked" }, isHighlight = user.gameId.isNotBlank())
                                    DetailItemRow(label = "Matches Won", value = "${user.wins} Wins", isHighlight = user.wins > 0)
                                    DetailItemRow(label = "Total Kills Recorded", value = "${user.kills} Kills", isHighlight = user.kills > 0)
                                    DetailItemRow(label = "Activity Reward Points", value = "${user.activityPoints} pts")
                                    val winRatio = if (user.wins + user.kills > 0) "${((user.wins.toDouble() / (user.wins + 5).coerceAtLeast(1)) * 100).toInt()}% est." else "No matches yet"
                                    DetailItemRow(label = "Estimated Win Ratio", value = winRatio)
                                    DetailItemRow(label = "Player Status", value = if (user.wins + user.kills > 0) "Active Competitor" else "Casual / New Player")
                                }

                                // Wallet, Balances & VT Tokens
                                DetailSectionCard(title = "WALLET, BALANCES & VT TOKENS", icon = Icons.Default.AccountBalanceWallet) {
                                    DetailItemRow(label = "Total Wallet Balance", value = "₹${user.totalWalletBalance}", isHighlight = true)
                                    DetailItemRow(label = "Deposit Balance", value = "₹${user.depositFunds}")
                                    DetailItemRow(label = "Winning Balance", value = "₹${user.winningFunds}", isHighlight = user.winningFunds > 0)
                                    DetailItemRow(label = "Bonus Balance", value = "₹${user.bonusFunds}")
                                    DetailItemRow(label = "VT Loyalty Tokens", value = "${user.tokens} Tokens", isHighlight = user.tokens > 0)
                                    DetailItemRow(label = "Total Lifetime Prize Won", value = "₹${user.totalEarnings}", isHighlight = user.totalEarnings > 0)
                                }

                                // Daily Streak, Missions & Referrals (User Panel Feature)
                                DetailSectionCard(title = "DAILY STREAK, MISSIONS & REFERRALS", icon = Icons.Default.LocalFireDepartment) {
                                    DetailItemRow(
                                        label = "Daily Login Streak",
                                        value = if (user.loginStreak > 0) "🔥 ${user.loginStreak} Days Streak" else "No active streak",
                                        isHighlight = user.loginStreak > 0
                                    )
                                    DetailItemRow(
                                        label = "Daily Missions Claimed",
                                        value = "${user.dailyMissionsTokensClaimed} Tokens Claimed"
                                    )
                                    DetailItemRow(
                                        label = "Last Mission Claim Date",
                                        value = user.lastMissionClaimDate.ifBlank { "Never" }
                                    )
                                    DetailItemRow(
                                        label = "Referral Code",
                                        value = user.referralCode.ifBlank { "Not generated" },
                                        isHighlight = user.referralCode.isNotBlank()
                                    )
                                    DetailItemRow(
                                        label = "Referred By (Inviter)",
                                        value = user.referredBy.ifBlank { "Direct Organic Signup" }
                                    )
                                    DetailItemRow(
                                        label = "Total Players Referred",
                                        value = "${user.referralCount} Users",
                                        isHighlight = user.referralCount > 0
                                    )
                                    DetailItemRow(
                                        label = "Referral Bonus Earned",
                                        value = "₹${user.referralBonusEarned}",
                                        isHighlight = user.referralBonusEarned > 0
                                    )
                                }

                                // Age, KYC & Cash Tournament Compliance (User Panel Feature)
                                DetailSectionCard(title = "AGE, KYC & CASH MATCH ELIGIBILITY", icon = Icons.Default.VerifiedUser) {
                                    DetailItemRow(
                                        label = "Date of Birth",
                                        value = user.dateOfBirth.ifBlank { "Not submitted" }
                                    )
                                    DetailItemRow(
                                        label = "Calculated Age",
                                        value = if (user.age > 0) "${user.age} Years Old" else "Unverified Age"
                                    )
                                    DetailItemRow(
                                        label = "Under-18 / Minor Status",
                                        value = if (user.isMinor) "Under-18 (Minor Protected)" else "18+ Adult",
                                        isHighlight = user.isMinor
                                    )
                                    DetailItemRow(
                                        label = "Cash Tournaments Eligibility",
                                        value = if (user.canJoinCashTournaments) "ELIGIBLE FOR CASH MATCHES" else "RESTRICTED (Practice / Free Only)",
                                        isHighlight = user.canJoinCashTournaments
                                    )
                                    DetailItemRow(
                                        label = "Age Self-Confirmed",
                                        value = if (user.ageConfirmed) "Yes (Confirmed by Player)" else "Pending Confirmation"
                                    )
                                }

                                // Identity & Contact Details
                                DetailSectionCard(title = "IDENTITY & DEVICE METADATA", icon = Icons.Default.Person) {
                                    DetailItemRow(label = "Display Name", value = user.bestDisplayName, isHighlight = true)
                                    DetailItemRow(label = "Free Fire IGN", value = user.effectiveIgn, isHighlight = user.effectiveIgn != "Player")
                                    DetailItemRow(label = "Email Address", value = user.email.ifBlank { if (user.bestEmailOrPhone.contains("@")) user.bestEmailOrPhone else "Not linked" })
                                    DetailItemRow(label = "Phone Number", value = user.phone.ifBlank { if (!user.phoneOrEmail.contains("@") && user.phoneOrEmail.isNotBlank()) user.phoneOrEmail else "Not linked" }, isHighlight = user.phone.isNotBlank() || user.phoneOrEmail.isNotBlank())
                                    DetailItemRow(label = "Free Fire / Game UID", value = user.gameId.ifBlank { "Not Linked" }, isHighlight = user.gameId.isNotBlank())
                                    DetailItemRow(label = "User Identifier (UID)", value = user.id)
                                    val photoStatus = if (!user.avatarUrl.isNullOrBlank()) "Linked Profile Picture" else "Generated Dynamic Avatar"
                                    DetailItemRow(label = "Profile Picture", value = photoStatus, isHighlight = !user.avatarUrl.isNullOrBlank())
                                    val device = user.deviceModel.ifBlank { user.rawAttributes["deviceModel"] ?: user.rawAttributes["model"] ?: "Android Client" }
                                    DetailItemRow(label = "Last Known Device", value = device)
                                    val ip = user.ipAddress.ifBlank { user.rawAttributes["ipAddress"] ?: user.rawAttributes["ip"] ?: "Dynamic Cellular" }
                                    DetailItemRow(label = "Last Known IP", value = ip)
                                }

                                // Active Security & Restriction Badges Card
                                DetailSectionCard(title = "SECURITY & RESTRICTION STATUS", icon = Icons.Default.Shield) {
                                    DetailItemRow(
                                        label = "Account Ban Status",
                                        value = if (isBannedState) "BANNED: ${banReasonInput}" else "CLEAN (Active)",
                                        isHighlight = isBannedState
                                    )
                                    DetailItemRow(
                                        label = "Temporary Suspension",
                                        value = if (isSuspendedState) "SUSPENDED (${suspensionReasonInput})" else "Not Suspended",
                                        isHighlight = isSuspendedState
                                    )
                                    DetailItemRow(
                                        label = "VPN & Proxy Shield",
                                        value = if (isVpnBlockedState) "BLOCKED (VPN Forbidden)" else "Standard Allowed",
                                        isHighlight = isVpnBlockedState
                                    )
                                    DetailItemRow(
                                        label = "Mandatory App Update",
                                        value = if (isForceUpdateState) "ENFORCED" else "Standard",
                                        isHighlight = isForceUpdateState
                                    )
                                    DetailItemRow(
                                        label = "Maintenance Mode Bypass",
                                        value = if (isMaintenanceBypassState) "VIP BYPASS ENABLED" else "Standard (No Bypass)",
                                        isHighlight = isMaintenanceBypassState
                                    )
                                }

                                // Account Timestamps Card
                                DetailSectionCard(title = "ACCOUNT TIMESTAMPS & LOGINS", icon = Icons.Default.Schedule) {
                                    DetailItemRow(label = "Account Created", value = createdStr, isHighlight = user.createdAt > 0)
                                    DetailItemRow(label = "Last Active / Logged In", value = lastActiveStr, isHighlight = true)
                                    DetailItemRow(label = "Account Role", value = user.role.uppercase())
                                }
                            }
                        }
                        1 -> {
                            // Admin Controls & Individual Toggles
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "INDIVIDUAL SECURITY & ACCESS TOGGLES",
                                    color = VelorixAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )

                                // Section: Granular Restriction Toggles
                                DetailSectionCard(title = "ACCESS & COMPLIANCE TOGGLES", icon = Icons.Default.Tune) {
                                    // 1. Account Ban Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Account Ban (Freeze & Lock)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Blocks login, match registrations, and freezes wallet balance", color = Color.Gray, fontSize = 11.sp)
                                        }
                                        Switch(
                                            checked = isBannedState,
                                            onCheckedChange = { isBannedState = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Red, checkedTrackColor = Color.Red.copy(alpha = 0.5f))
                                        )
                                    }

                                    if (isBannedState) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = banReasonInput,
                                            onValueChange = { banReasonInput = it },
                                            label = { Text("Ban Reason", color = Color.Gray, fontSize = 10.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.Red, unfocusedBorderColor = CardVerifyBorder)
                                        )
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFF232936))

                                    // 2. Temporary Suspension Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Match Suspension", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Temporarily suspends user from entering custom rooms", color = Color.Gray, fontSize = 11.sp)
                                        }
                                        Switch(
                                            checked = isSuspendedState,
                                            onCheckedChange = { isSuspendedState = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF9800), checkedTrackColor = Color(0xFFFF9800).copy(alpha = 0.5f))
                                        )
                                    }

                                    if (isSuspendedState) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = suspensionReasonInput,
                                            onValueChange = { suspensionReasonInput = it },
                                            label = { Text("Suspension Reason", color = Color.Gray, fontSize = 10.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFF9800), unfocusedBorderColor = CardVerifyBorder)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Duration:", color = Color.LightGray, fontSize = 11.sp)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf(24 to "24h", 72 to "3d", 168 to "7d", 720 to "30d").forEach { (hours, label) ->
                                                val isSelected = selectedSuspensionHours == hours
                                                Button(
                                                    onClick = { selectedSuspensionHours = hours },
                                                    modifier = Modifier.weight(1f).height(30.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) Color(0xFFFF9800) else Color(0xFF232936)),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text(label, color = if (isSelected) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFF232936))

                                    // 3. VPN / Proxy Block Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("VPN & Proxy Shield", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Enforce direct ISP connection, disallowing VPNs/proxies", color = Color.Gray, fontSize = 11.sp)
                                        }
                                        Switch(
                                            checked = isVpnBlockedState,
                                            onCheckedChange = { isVpnBlockedState = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = VelorixAccent)
                                        )
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFF232936))

                                    // 4. Force Update App Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Mandatory Client App Update", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Forces player to update app before joining match lobbies", color = Color.Gray, fontSize = 11.sp)
                                        }
                                        Switch(
                                            checked = isForceUpdateState,
                                            onCheckedChange = { isForceUpdateState = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = VelorixAccent)
                                        )
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFF232936))

                                    // 5. Maintenance Bypass Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Maintenance Mode Bypass (VIP)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Allows this user into app during scheduled maintenance", color = Color.Gray, fontSize = 11.sp)
                                        }
                                        Switch(
                                            checked = isMaintenanceBypassState,
                                            onCheckedChange = { isMaintenanceBypassState = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = VelorixAccent)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = {
                                            val updated = buildUpdatedUser()
                                            onUpdateUser(updated)
                                            Toast.makeText(context, "Access toggles successfully applied to Firebase!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent),
                                        modifier = Modifier.fillMaxWidth().height(40.dp)
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("APPLY ALL TOGGLES TO FIREBASE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                // 2. Edit Contact & Profile Info
                                DetailSectionCard(title = "CONTACT & PROFILE INFO", icon = Icons.Default.Edit) {
                                    OutlinedTextField(
                                        value = editUsername,
                                        onValueChange = { editUsername = it },
                                        label = { Text("Display Name / IGN", color = Color.Gray, fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = VelorixAccent,
                                            unfocusedBorderColor = CardVerifyBorder
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = editEmail,
                                        onValueChange = { editEmail = it },
                                        label = { Text("Email Address", color = Color.Gray, fontSize = 11.sp) },
                                        placeholder = { Text("e.g. player@gmail.com", color = Color.DarkGray, fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = VelorixAccent,
                                            unfocusedBorderColor = CardVerifyBorder
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = editGameId,
                                        onValueChange = { editGameId = it },
                                        label = { Text("Free Fire / Game UID", color = Color.Gray, fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = VelorixAccent,
                                            unfocusedBorderColor = CardVerifyBorder
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = editPhone,
                                        onValueChange = { editPhone = it },
                                        label = { Text("Mobile / Phone Number", color = Color.Gray, fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = VelorixAccent,
                                            unfocusedBorderColor = CardVerifyBorder
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            val updated = buildUpdatedUser()
                                            onUpdateUser(updated)
                                            Toast.makeText(context, "Profile info updated successfully", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent),
                                        modifier = Modifier.fillMaxWidth().height(42.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save Contact & Profile Info", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                // 3. Adjust Wallet Balance
                                DetailSectionCard(title = "WALLET BALANCE CONTROL", icon = Icons.Default.Payments) {
                                    Text("Current Balance: ₹${user.funds}", color = Color.LightGray, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    // Quick +/- buttons
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(50.0, 100.0, 500.0, -100.0).forEach { delta ->
                                            Button(
                                                onClick = {
                                                    onAddFunds(user, delta)
                                                    Toast.makeText(context, "Balance adjusted by ₹$delta", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.weight(1f).height(32.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = if (delta > 0) Color(0xFF1B5E20) else Color(0xFFB71C1C)),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text(if (delta > 0) "+₹${delta.toInt()}" else "-₹${(-delta).toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = editBalanceInput,
                                            onValueChange = { editBalanceInput = it },
                                            label = { Text("Set Exact Balance (₹)", color = Color.Gray, fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = VelorixAccent,
                                                unfocusedBorderColor = CardVerifyBorder
                                            )
                                        )
                                        Button(
                                            onClick = {
                                                val newBal = editBalanceInput.toDoubleOrNull()
                                                if (newBal != null) {
                                                    val diff = newBal - user.funds
                                                    onAddFunds(user, diff)
                                                    Toast.makeText(context, "Balance set to ₹$newBal", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent),
                                            modifier = Modifier.height(50.dp)
                                        ) {
                                            Text("Set", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // 4. Role & Permissions
                                DetailSectionCard(title = "ASSIGN USER ROLE", icon = Icons.Default.Security) {
                                    Text("Current Role: ${user.role}", color = Color.LightGray, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("player", "tournament_admin", "support_admin", "super_admin").forEach { roleOption ->
                                            val isSelected = editRole.equals(roleOption, ignoreCase = true)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(if (isSelected) VelorixAccent else Color(0xFF232936), RoundedCornerShape(8.dp))
                                                    .border(1.dp, if (isSelected) VelorixAccent else CardVerifyBorder, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        editRole = roleOption
                                                        val updated = buildUpdatedUser()
                                                        onUpdateUser(updated)
                                                        Toast.makeText(context, "Role updated to $roleOption", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = roleOption.replace("_", " ").take(10).uppercase(),
                                                    color = if (isSelected) Color.Black else Color.LightGray,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                // Rate Limiting & Cooldowns Control
                                DetailSectionCard(title = "RATE LIMITING & SECURITY LOCKOUTS", icon = Icons.Default.LockClock) {
                                    val isLockedOut = remember(user.id) {
                                        UserRateLimiter.isUserLockedOut(user.id) ||
                                        (user.phone.isNotBlank() && UserRateLimiter.isUserLockedOut(user.phone)) ||
                                        (user.email.isNotBlank() && UserRateLimiter.isUserLockedOut(user.email))
                                    }
                                    val lockoutSummary = remember(user.id) {
                                        UserRateLimiter.getActiveLockoutSummary(user.id)
                                            ?: (if (user.phone.isNotBlank()) UserRateLimiter.getActiveLockoutSummary(user.phone) else null)
                                            ?: (if (user.email.isNotBlank()) UserRateLimiter.getActiveLockoutSummary(user.email) else null)
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isLockedOut) Color(0x33F44336) else Color(0x224CAF50), RoundedCornerShape(8.dp))
                                            .border(1.dp, if (isLockedOut) Color(0xFFF44336) else Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (isLockedOut) Icons.Default.Warning else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (isLockedOut) Color(0xFFF44336) else Color(0xFF4CAF50),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (isLockedOut) "SECURITY RATE LOCK ACTIVE" else "NO ACTIVE RATE LIMITS",
                                                color = if (isLockedOut) Color(0xFFF44336) else Color(0xFF4CAF50),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            if (lockoutSummary != null) {
                                                Text(text = lockoutSummary, color = Color.LightGray, fontSize = 10.sp)
                                            } else {
                                                Text(text = "User can perform auth, wallet, and tournament actions normally", color = Color.Gray, fontSize = 10.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            var totalReset = UserRateLimiter.resetUserLimits(user.id)
                                            if (user.phone.isNotBlank()) totalReset += UserRateLimiter.resetUserLimits(user.phone)
                                            if (user.email.isNotBlank()) totalReset += UserRateLimiter.resetUserLimits(user.email)
                                            Toast.makeText(context, "Rate limits & cooldowns reset for ${user.username} ($totalReset cleared)", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                        modifier = Modifier.fillMaxWidth().height(36.dp)
                                    ) {
                                        Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("RESET ALL RATE LIMIT COOLDOWNS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }

                                // 5. Modify Stats
                                DetailSectionCard(title = "EDIT COMPETITIVE STATS", icon = Icons.Default.EmojiEvents) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = editWins,
                                            onValueChange = { editWins = it },
                                            label = { Text("Wins", color = Color.Gray, fontSize = 10.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                                        )
                                        OutlinedTextField(
                                            value = editKills,
                                            onValueChange = { editKills = it },
                                            label = { Text("Kills", color = Color.Gray, fontSize = 10.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                                        )
                                        OutlinedTextField(
                                            value = editActivityPoints,
                                            onValueChange = { editActivityPoints = it },
                                            label = { Text("Points", color = Color.Gray, fontSize = 10.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = editEarnings,
                                        onValueChange = { editEarnings = it },
                                        label = { Text("Total Earnings Won (₹)", color = Color.Gray, fontSize = 10.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = VelorixAccent, unfocusedBorderColor = CardVerifyBorder)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            val updated = buildUpdatedUser()
                                            onUpdateUser(updated)
                                            Toast.makeText(context, "Player stats updated", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent),
                                        modifier = Modifier.fillMaxWidth().height(38.dp)
                                    ) {
                                        Text("Save Stats", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                // 6. Delete Account (Danger Zone)
                                DetailSectionCard(title = "DANGER ZONE", icon = Icons.Default.Warning) {
                                    Text("Permanently erase this account and all associated profile nodes from Firebase database.", color = Color.LightGray, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { showDeleteConfirmDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.25f)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                                        modifier = Modifier.fillMaxWidth().height(38.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Permanently Delete User", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Raw Node Data (Every Minor Detail)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "RAW DATABASE ATTRIBUTES",
                                    color = VelorixAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Every minor attribute found in the database for this record:",
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )

                                val combinedRaw = remember(user) {
                                    val map = mutableMapOf<String, String>()
                                    map["id"] = user.id
                                    map["username"] = user.username
                                    map["email"] = user.email
                                    map["gameId"] = user.gameId
                                    map["role"] = user.role
                                    map["funds"] = user.funds.toString()
                                    map["wins"] = user.wins.toString()
                                    map["kills"] = user.kills.toString()
                                    map["activityPoints"] = user.activityPoints.toString()
                                    map["totalEarnings"] = user.totalEarnings.toString()
                                    map["isBanned"] = user.isBanned.toString()
                                    map["createdAt"] = user.createdAt.toString()
                                    map["lastActive"] = user.lastActive.toString()
                                    user.rawAttributes.forEach { (k, v) ->
                                        map[k] = v
                                    }
                                    map
                                }

                                combinedRaw.forEach { (k, v) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF1E2430), RoundedCornerShape(8.dp))
                                            .border(0.5.dp, CardVerifyBorder, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = k,
                                            color = VelorixAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(0.4f)
                                        )
                                        Text(
                                            text = v,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(0.6f)
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
}
}

@Composable
fun DetailSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    val sectionAccentColor = when {
        title.contains("ESPORTS", ignoreCase = true) || title.contains("GAMING", ignoreCase = true) -> Color(0xFF00E5FF)
        title.contains("WALLET", ignoreCase = true) || title.contains("FINANCIAL", ignoreCase = true) -> Color(0xFF00E676)
        title.contains("STREAK", ignoreCase = true) || title.contains("FIRE", ignoreCase = true) -> Color(0xFFFF6D00)
        title.contains("KYC", ignoreCase = true) || title.contains("ELIGIBILITY", ignoreCase = true) -> Color(0xFFB388FF)
        title.contains("SECURITY", ignoreCase = true) || title.contains("RESTRICTION", ignoreCase = true) -> Color(0xFFFF1744)
        title.contains("TIMESTAMPS", ignoreCase = true) -> Color(0xFFFFD54F)
        else -> VelorixAccent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161B24), RoundedCornerShape(14.dp))
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        sectionAccentColor.copy(alpha = 0.45f),
                        Color(0xFF272F3D).copy(alpha = 0.2f)
                    )
                ),
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(sectionAccentColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .border(0.8.dp, sectionAccentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = sectionAccentColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
fun DetailItemRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFFA1A1AA), fontSize = 12.sp)
        if (isHighlight) {
            Box(
                modifier = Modifier
                    .background(VelorixAccent.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .border(0.5.dp, VelorixAccent.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = value,
                    color = VelorixAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text(
                text = value,
                color = Color(0xFFEEEEEE),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PayoutsManagementScreenContent(
    uiState: DashboardState,
    onApprovePayout: (com.example.domain.model.PayoutRequest) -> Unit = {},
    onRejectPayout: (com.example.domain.model.PayoutRequest, String) -> Unit = { _, _ -> }
) {
    val payouts = (uiState as? DashboardState.Success)?.payoutRequests ?: emptyList()
    var selectedFilter by remember { mutableStateOf("pending") }
    var showRejectDialog by remember { mutableStateOf<com.example.domain.model.PayoutRequest?>(null) }
    var rejectionReasonInput by remember { mutableStateOf("Invalid UPI Details / Account Holder Name Mismatch") }

    val filteredPayouts = remember(payouts, selectedFilter) {
        when (selectedFilter) {
            "all" -> payouts
            else -> payouts.filter { it.status.equals(selectedFilter, ignoreCase = true) }
        }
    }

    val currentRejectReq = showRejectDialog
    if (currentRejectReq != null) {
        val req = currentRejectReq
        AlertDialog(
            onDismissRequest = { showRejectDialog = null },
            title = { Text("Reject Cashout Request", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Reject ₹${req.vtAmount} cashout for ${req.username} (${req.email})?", color = Color.LightGray, fontSize = 12.sp)
                    Text("The ₹${req.vtAmount} VT balance will be refunded automatically to their wallet.", color = VelorixAccent, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rejectionReasonInput,
                        onValueChange = { rejectionReasonInput = it },
                        label = { Text("Rejection Reason", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Red,
                            unfocusedBorderColor = CardVerifyBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRejectPayout(req, rejectionReasonInput.trim())
                        showRejectDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Confirm Reject & Refund", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = CardLiveBg
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Payout Approvals",
                    color = VelorixTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Manage cashouts at /payout_requests",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(VelorixAccent.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                    .border(1.dp, VelorixAccent.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                val pendingSum = payouts.filter { it.status.equals("pending", ignoreCase = true) }.sumOf { it.vtAmount }
                Text(
                    text = "₹${pendingSum.toInt()} Pending",
                    color = VelorixAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp)
        ) {
            listOf("pending", "approved", "rejected", "all").forEach { f ->
                val isSel = selectedFilter == f
                Box(
                    modifier = Modifier
                        .background(if (isSel) VelorixAccent else CardLiveBg, RoundedCornerShape(8.dp))
                        .clickable { selectedFilter = f }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(f.uppercase(), color = if (isSel) Color.Black else Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 90.dp)) {
            if (filteredPayouts.isEmpty()) {
                item {
                    Text("No cashout requests found.", color = VelorixTextSecondary, modifier = Modifier.padding(16.dp))
                }
            }
            items(filteredPayouts.size) { i ->
                val req = filteredPayouts[i]
                val statusColor = when (req.status.lowercase()) {
                    "approved" -> Color(0xFF2E7D32)
                    "rejected" -> Color.Red
                    else -> VelorixAccent
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardLiveBg, RoundedCornerShape(16.dp))
                        .border(1.dp, CardVerifyBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(req.username, fontWeight = FontWeight.Bold, color = VelorixTextPrimary, fontSize = 16.sp)
                            Text(req.email, color = VelorixTextSecondary, fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(req.status.uppercase(), color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Payment Method", color = Color.Gray, fontSize = 10.sp)
                            Text("${req.paymentMethod}: ${req.paymentId}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Column {
                            Text("Cashout Amount", color = Color.Gray, fontSize = 10.sp)
                            Text("₹${req.vtAmount}", color = VelorixAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (req.status.equals("pending", ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onApprovePayout(req)
                                },
                                modifier = Modifier.weight(1f).height(34.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent)
                            ) {
                                Text("APPROVE PAYOUT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                            Button(
                                onClick = { showRejectDialog = req },
                                modifier = Modifier.weight(1f).height(34.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("REJECT & REFUND", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// StaffManagementScreenContent is implemented with full editing & inspection dialog parity in StaffManagementScreen.kt


