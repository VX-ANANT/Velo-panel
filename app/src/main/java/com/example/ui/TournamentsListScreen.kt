package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.FreeFireCategories
import com.example.domain.model.Tournament
import com.example.ui.theme.*
import com.example.ui.viewmodel.DashboardState
import com.example.ui.viewmodel.TournamentDashboardViewModel
import com.example.util.TournamentImageUploader
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

@Composable
fun TournamentsListScreenContent(
    uiState: DashboardState,
    onTournamentClick: ((String) -> Unit)? = null,
    onSettingsClick: ((String) -> Unit)? = null,
    onBracketClick: ((String) -> Unit)? = null,
    onDuplicateTournament: ((Tournament) -> Unit)? = null,
    onExportJson: (() -> Unit)? = null,
    onImportJson: ((String) -> Unit)? = null,
    onSyncCloud: (() -> Unit)? = null,
    onCreateTournament: (() -> Unit)? = null,
    viewModel: TournamentDashboardViewModel? = null
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var tournamentForStandingsModal by remember { mutableStateOf<Tournament?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var extractedJsonText by remember { mutableStateOf("") }
    var syncStatusMessage by remember { mutableStateOf<String?>(null) }

    val successState = uiState as? DashboardState.Success
    val tournamentsList = successState?.tournaments ?: emptyList()
    val dynamicCategories = successState?.categories?.ifEmpty { FreeFireCategories.DEFAULT_CATEGORIES }
        ?: FreeFireCategories.DEFAULT_CATEGORIES
    val isCloudSyncing = successState?.isSyncing ?: false

    val liveCount = remember(tournamentsList) {
        tournamentsList.count { it.status.equals("live", ignoreCase = true) || it.status.equals("ongoing", ignoreCase = true) }
    }
    val upcomingCount = remember(tournamentsList) {
        tournamentsList.count { it.status.equals("upcoming", ignoreCase = true) }
    }
    val totalPrizePool = remember(tournamentsList) {
        tournamentsList.sumOf { it.prizePool.toDouble() }.toInt()
    }
    val filledSlots = remember(tournamentsList) {
        tournamentsList.sumOf { it.registeredPlayers }
    }
    val totalSlots = remember(tournamentsList) {
        tournamentsList.sumOf { it.maxPlayers }
    }

    val statusTabs = remember(tournamentsList) {
        listOf(
            "All" to tournamentsList.size,
            "Upcoming" to tournamentsList.count { it.status.equals("upcoming", ignoreCase = true) },
            "Live" to tournamentsList.count { it.status.equals("live", ignoreCase = true) || it.status.equals("ongoing", ignoreCase = true) },
            "Completed" to tournamentsList.count {
                it.status.equals("ended", ignoreCase = true) || it.status.equals("completed", ignoreCase = true)
            },
            "Cancelled" to tournamentsList.count { it.status.equals("cancelled", ignoreCase = true) }
        )
    }

    val filteredList = remember(tournamentsList, selectedTab, selectedCategory, searchQuery) {
        tournamentsList.filter { t ->
            val matchesTab = when (selectedTab) {
                1 -> t.status.equals("upcoming", ignoreCase = true)
                2 -> t.status.equals("live", ignoreCase = true) || t.status.equals("ongoing", ignoreCase = true)
                3 -> t.status.equals("ended", ignoreCase = true) || t.status.equals("completed", ignoreCase = true)
                4 -> t.status.equals("cancelled", ignoreCase = true)
                else -> true
            }
            val matchesCategory = selectedCategory == "ALL" ||
                t.canonicalCategory.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                t.title.contains(searchQuery, ignoreCase = true) ||
                t.game.contains(searchQuery, ignoreCase = true) ||
                t.map.contains(searchQuery, ignoreCase = true) ||
                t.format.contains(searchQuery, ignoreCase = true) ||
                t.canonicalCategory.contains(searchQuery, ignoreCase = true)
            matchesTab && matchesCategory && matchesQuery
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "tournamentsSyncSpin")
    val syncRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tournamentsSyncAngle"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(V14Bg)
    ) {
        // =====================================================================
        // 1. V14 EDITORIAL HEADER BAR (MATCHES HOME V14TopBarHeader)
        // =====================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text(
                    text = "Tournaments",
                    color = V14TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 23.sp,
                    letterSpacing = (-0.5).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append("${filteredList.size} ${if (filteredList.size == 1) "match" else "matches"}")
                        if (liveCount > 0) append(" · $liveCount live")
                        if (upcomingCount > 0) append(" · $upcomingCount upcoming")
                    },
                    color = V14TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onCreateTournament != null) {
                    Row(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(Brush.horizontalGradient(listOf(V14AccentPrimary, V14AccentBright)))
                            .testTag("tournaments_create_button")
                            .v14PressScale(onClick = onCreateTournament)
                            .padding(horizontal = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Tournament",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "New",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // V14 Circular Sync Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(V14CardSurface)
                        .border(0.75.dp, V14Border, CircleShape)
                        .testTag("tournaments_sync_button")
                        .v14PressScale {
                            syncStatusMessage = "Syncing live matches from Firebase..."
                            onSyncCloud?.invoke()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync Backend",
                        tint = if (isCloudSyncing) V14AccentBright else V14TextSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(if (isCloudSyncing) syncRotation else 0f)
                    )
                }

                // V14 Circular Overflow Menu Button
                Box {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(V14CardSurface)
                            .border(0.75.dp, V14Border, CircleShape)
                            .testTag("tournaments_overflow_button")
                            .v14PressScale { showOverflowMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Actions",
                            tint = V14TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                        containerColor = V14CardElevated,
                        border = BorderStroke(0.75.dp, V14Border)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export Tournaments JSON", color = V14TextPrimary, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = V14TextSecondary, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                showOverflowMenu = false
                                val json = JSONArray().apply {
                                    tournamentsList.forEach { t ->
                                        put(JSONObject().apply {
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
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("TournamentsJSON", json))
                                onExportJson?.invoke()
                                showExportDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Import / Push JSON", color = V14TextPrimary, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.FileUpload, contentDescription = null, tint = V14AccentBright, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                showOverflowMenu = false
                                showImportDialog = true
                            }
                        )
                    }
                }
            }
        }

        if (syncStatusMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(VelorixEmerald.copy(alpha = 0.08f))
                    .border(0.75.dp, VelorixEmerald.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(VelorixEmerald, CircleShape))
                    Text(
                        text = syncStatusMessage ?: "",
                        color = VelorixEmerald,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "Dismiss",
                    color = V14TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { syncStatusMessage = null }
                )
            }
        }

        // =====================================================================
        // 2. V14 BENTO TELEMETRY SUMMARY STRIP (3 COMPACT TILES)
        // =====================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            V14CompactKpiChip(
                label = "LIVE NOW",
                value = "$liveCount",
                accent = if (liveCount > 0) VelorixEmerald else V14TextSecondary,
                modifier = Modifier.weight(0.85f)
            )
            V14CompactKpiChip(
                label = "TOTAL POOL",
                value = "₹${String.format(Locale.US, "%,d", totalPrizePool)}",
                accent = V14TextPrimary,
                modifier = Modifier.weight(1.25f)
            )
            V14CompactKpiChip(
                label = "SLOTS",
                value = "$filledSlots/$totalSlots",
                accent = V14TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // =====================================================================
        // 3. V14 SEARCH INPUT
        // =====================================================================
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "Search match, map, or mode...",
                    color = V14TextMuted,
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = V14TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = V14TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("tournaments_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = V14CardSurface,
                unfocusedContainerColor = V14CardSurface,
                focusedTextColor = V14TextPrimary,
                unfocusedTextColor = V14TextPrimary,
                focusedBorderColor = V14AccentPrimary.copy(alpha = 0.7f),
                unfocusedBorderColor = V14Border,
                cursorColor = V14AccentPrimary
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // =====================================================================
        // 4. V14 STATUS PILL STRIP (MATCHES HOME V14QuickHubPillStrip)
        // =====================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statusTabs.forEachIndexed { index, (label, count) ->
                val isSelected = selectedTab == index
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isSelected) V14AccentPrimary else V14CardSurface)
                        .border(
                            width = 0.75.dp,
                            color = if (isSelected) V14AccentBright else V14Border,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .v14PressScale { selectedTab = index }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else V14TextSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isSelected) Color.White.copy(alpha = 0.22f)
                                    else V14CardElevated
                                )
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "$count",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) Color.White else V14TextMuted
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // =====================================================================
        // 5. UNDERLINE CATEGORY MODE SELECTOR (AGENTS.md SPEC)
        // =====================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val allCategories = listOf("ALL" to "All Modes") + dynamicCategories.map {
                it.id to (it.shortName.ifBlank { it.name.ifBlank { it.label } })
            }
            allCategories.forEach { (catKey, catLabel) ->
                val isSelected = selectedCategory == catKey
                Column(
                    modifier = Modifier
                        .clickable { selectedCategory = catKey }
                        .padding(top = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = catLabel,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) V14TextPrimary else V14TextMuted,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(2.dp)
                            .background(
                                if (isSelected) V14AccentPrimary else Color.Transparent,
                                RoundedCornerShape(1.dp)
                            )
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = V14Border)

        // =====================================================================
        // 6. V14 BENTO TOURNAMENTS FEED
        // =====================================================================
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 124.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (filteredList.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(V14CardSurface)
                            .border(0.75.dp, V14Border, RoundedCornerShape(22.dp))
                            .padding(vertical = 40.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(V14CardElevated)
                                .border(0.75.dp, V14Border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = V14TextMuted,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching tournaments" else "No tournaments here yet",
                            color = V14TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                "Try clearing your search or switching filters."
                            } else {
                                "Create a Free Fire tournament or sync from Firebase."
                            },
                            color = V14TextSecondary,
                            fontSize = 12.sp
                        )
                        if (onCreateTournament != null && searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(18.dp))
                            Row(
                                modifier = Modifier
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Brush.horizontalGradient(listOf(V14AccentPrimary, V14AccentBright)))
                                    .v14PressScale(onClick = onCreateTournament)
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Create Match", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            itemsIndexed(filteredList, key = { _, item -> item.id }) { index, tournament ->
                V14StaggeredEntrance(delayMillis = (index.coerceAtMost(5)) * 35) {
                    V14TournamentFeedCard(
                        tournament = tournament,
                        onOpenDetails = {
                            if (onTournamentClick != null) onTournamentClick(tournament.id)
                            else onSettingsClick?.invoke(tournament.id)
                        },
                        onDeclareWinners = {
                            if (viewModel != null) tournamentForStandingsModal = tournament
                            else onSettingsClick?.invoke(tournament.id)
                        },
                        onBracketClick = {
                            if (onBracketClick != null) onBracketClick(tournament.id)
                            else onTournamentClick?.invoke(tournament.id)
                        },
                        onEdit = { onSettingsClick?.invoke(tournament.id) },
                        onClone = {
                            onDuplicateTournament?.invoke(tournament)
                            Toast.makeText(context, "Cloning '${tournament.title}'...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    if (tournamentForStandingsModal != null && viewModel != null) {
        DeclareStandingsAndPrizesDialog(
            tournament = tournamentForStandingsModal!!,
            viewModel = viewModel,
            onDismiss = { tournamentForStandingsModal = null }
        )
    }

    // Export JSON Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Exported Tournaments JSON", color = V14TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    Text(
                        "All ${tournamentsList.size} tournaments exported and copied to clipboard.",
                        fontSize = 12.sp,
                        color = V14TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = extractedJsonText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = V14CardElevated,
                            unfocusedContainerColor = V14CardElevated,
                            focusedTextColor = V14TextPrimary,
                            unfocusedTextColor = V14TextPrimary,
                            focusedBorderColor = V14Border,
                            unfocusedBorderColor = V14Border
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showExportDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = V14TextPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = V14CardSurface
        )
    }

    // Import / Push JSON Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Push Tournaments JSON", color = V14TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    Text(
                        "Paste a single tournament or JSON array to push directly to RTDB & Firestore:",
                        fontSize = 12.sp,
                        color = V14TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("Paste JSON here...", color = V14TextMuted, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = V14CardElevated,
                            unfocusedContainerColor = V14CardElevated,
                            focusedTextColor = V14TextPrimary,
                            unfocusedTextColor = V14TextPrimary,
                            focusedBorderColor = V14AccentPrimary,
                            unfocusedBorderColor = V14Border
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonText.isNotBlank()) {
                            onImportJson?.invoke(importJsonText)
                            syncStatusMessage = "JSON pushed to Firebase!"
                            showImportDialog = false
                            importJsonText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = V14AccentPrimary, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Push To Cloud", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = V14TextSecondary)
                }
            },
            containerColor = V14CardSurface
        )
    }
}

@Composable
private fun V14CompactKpiChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(V14CardSurface)
            .border(0.75.dp, V14Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = V14TextMuted,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            color = accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun V14TournamentFeedCard(
    tournament: Tournament,
    onOpenDetails: () -> Unit,
    onDeclareWinners: () -> Unit,
    onBracketClick: () -> Unit,
    onEdit: () -> Unit,
    onClone: () -> Unit
) {
    val statusUpper = tournament.status.trim().uppercase().ifBlank { "UPCOMING" }
    val statusColor = when (statusUpper) {
        "LIVE", "ONGOING", "ACTIVE" -> VelorixEmerald
        "COMPLETED", "ENDED" -> V14TextSecondary
        "CANCELLED" -> Color(0xFFF87171)
        else -> V14AccentBright
    }
    val isMoneyTournament = (tournament.entryFee > 0f) || (tournament.prizePool > 0.0)
    val fillFraction = if (tournament.maxPlayers > 0) {
        (tournament.registeredPlayers.toFloat() / tournament.maxPlayers.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val animFill by animateFloatAsState(
        targetValue = fillFraction,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow),
        label = "feedCardSlotFill"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(V14CardSurface)
            .border(0.75.dp, V14Border, RoundedCornerShape(22.dp))
            .testTag("tournament_card_${tournament.id}")
            .v14PressScale(onClick = onOpenDetails)
    ) {
        // Dark-scrimmed Hero Strip (absorbs even high-key/white uploaded logos seamlessly)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp)
                .background(V14CardElevated)
        ) {
            if (tournament.bannerUrl.isNotBlank()) {
                AsyncImage(
                    model = TournamentImageUploader.resolveBannerModel(tournament.bannerUrl),
                    contentDescription = tournament.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1D0A11), V14CardSurface, V14CardElevated)
                            )
                        )
                )
            }

            // Deep cinema scrim ensures zero blinding white cutouts and crisp text contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color(0xB3000000),
                            0.42f to Color(0x7A000000),
                            0.78f to Color(0xE60C0C10),
                            1.0f to V14CardSurface
                        )
                    )
            )

            // Top Capsule Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xCC0C0C10))
                        .border(0.6.dp, statusColor.copy(alpha = 0.42f), RoundedCornerShape(50))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(modifier = Modifier.size(5.dp).background(statusColor, CircleShape))
                    Text(
                        text = statusUpper,
                        color = statusColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xCC0C0C10))
                        .border(
                            0.6.dp,
                            if (isMoneyTournament) VelorixAmber.copy(alpha = 0.4f) else VelorixEmerald.copy(alpha = 0.4f),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isMoneyTournament) "18+ CASH" else "TRAINING",
                        color = if (isMoneyTournament) VelorixAmber else VelorixEmerald,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Bottom Title + Format Metadata
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = tournament.title.ifBlank { "Untitled Tournament" },
                    color = V14TextPrimary,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 21.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${tournament.format.ifBlank { "SOLO" }.uppercase()}  •  ${tournament.map.ifBlank { "Bermuda" }}  •  ${tournament.canonicalCategory}",
                    color = V14TextSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Body Telemetry & Animated V14 Progress Bar
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (statusUpper == "CANCELLED" && tournament.cancellationReason.isNotBlank()) {
                Text(
                    text = "Cancelled: ${tournament.cancellationReason}",
                    color = Color(0xFFFCA5A5),
                    fontSize = 11.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                V14MetricColumn(
                    label = "PRIZE POOL",
                    value = "₹${String.format(Locale.US, "%,d", tournament.prizePool.toInt())}",
                    valueColor = VelorixEmerald,
                    alignment = Alignment.Start
                )
                V14MetricColumn(
                    label = "ENTRY FEE",
                    value = if (tournament.entryFee > 0f) "₹${tournament.entryFee.toInt()}" else "FREE",
                    valueColor = V14TextPrimary,
                    alignment = Alignment.CenterHorizontally
                )
                V14MetricColumn(
                    label = "SCHEDULE",
                    value = tournament.startsAt?.take(10) ?: "TBA",
                    valueColor = V14TextSecondary,
                    alignment = Alignment.End
                )
            }

            // V14 Slot Occupancy Bar (Identical to Home V14MatchActivityRow)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(V14BarMuted)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animFill)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(V14AccentDeep, V14AccentPrimary, V14AccentBright)
                                )
                            )
                    )
                }
                Text(
                    text = "${tournament.registeredPlayers}/${tournament.maxPlayers} slots",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = V14TextSecondary,
                    maxLines = 1,
                    softWrap = false
                )
            }

            val roomIdVal = tournament.roomDetails?.roomId.orEmpty()
            val roomPassVal = tournament.roomDetails?.roomPassword.orEmpty()
            if (roomIdVal.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(VelorixEmerald.copy(alpha = 0.08f))
                        .border(0.5.dp, VelorixEmerald.copy(alpha = 0.24f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = VelorixEmerald,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Room $roomIdVal  •  Pass ${roomPassVal.ifBlank { "None" }}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = VelorixEmerald,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(V14Border)
            )

            // Sleek V14 Pill Action Row (Matches Home's V14MatchActivityRow!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                V14CardActionPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.EmojiEvents,
                    label = "Winners",
                    tint = VelorixAmber,
                    background = VelorixAmber.copy(alpha = 0.10f),
                    borderColor = VelorixAmber.copy(alpha = 0.28f),
                    testTag = "feed_winners_${tournament.id}",
                    onClick = onDeclareWinners
                )
                V14CardActionPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AccountTree,
                    label = "Bracket",
                    tint = V14TextPrimary,
                    background = V14CardElevated,
                    borderColor = V14Border,
                    testTag = "feed_bracket_${tournament.id}",
                    onClick = onBracketClick
                )
                V14CardActionPill(
                    modifier = Modifier.weight(0.9f),
                    icon = Icons.Default.Edit,
                    label = "Edit",
                    tint = V14AccentBright,
                    background = V14AccentPrimary.copy(alpha = 0.14f),
                    borderColor = V14AccentBright.copy(alpha = 0.34f),
                    testTag = "feed_edit_${tournament.id}",
                    onClick = onEdit
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(V14CardElevated)
                        .border(0.6.dp, V14Border, RoundedCornerShape(10.dp))
                        .testTag("feed_clone_${tournament.id}")
                        .v14PressScale(onClick = onClone),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Clone Tournament",
                        tint = V14TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun V14MetricColumn(
    label: String,
    value: String,
    valueColor: Color,
    alignment: Alignment.Horizontal
) {
    Column(horizontalAlignment = alignment) {
        Text(
            text = label,
            color = V14TextMuted,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun V14CardActionPill(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    tint: Color,
    background: Color,
    borderColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .border(0.6.dp, borderColor, RoundedCornerShape(10.dp))
            .testTag(testTag)
            .v14PressScale(onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
