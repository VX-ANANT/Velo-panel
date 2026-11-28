package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.PlayerRegistration
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlayerVerificationViewModel
import com.example.ui.viewmodel.VerificationState

@Composable
fun PlayerVerificationScreen(
    viewModel: PlayerVerificationViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "PRIORITY", "UNDER18"

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(V14Bg)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(V14CardSurface)
                            .border(1.dp, V14Border, CircleShape)
                            .clickable(onClick = onNavigateBack),
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

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Player Verification",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = V14TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GATEWAY",
                                color = V14AccentBright,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "Review, authenticate and approve esports player registrations",
                            fontSize = 11.5.sp,
                            color = V14TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Recessed V14 Search Field
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF08080B))
                        .border(1.dp, V14Border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 11.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = V14TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Search by player IGN, Game ID, UID or Region...",
                                color = V14TextMuted,
                                fontSize = 12.5.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = V14TextPrimary,
                                fontSize = 13.sp
                            ),
                            cursorBrush = SolidColor(V14AccentBright),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = V14TextSecondary,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Flat underline mode tabs
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        VerificationFilterChip(
                            label = "ALL REGISTRATIONS",
                            isSelected = selectedFilter == "ALL",
                            count = (uiState as? VerificationState.Success)?.registrations?.size ?: 0,
                            onClick = { selectedFilter = "ALL" }
                        )
                    }
                    item {
                        val priorityCount = (uiState as? VerificationState.Success)?.registrations?.count {
                            it.gameAccountId?.isNotBlank() == true || it.gameId.isNotBlank()
                        } ?: 0
                        VerificationFilterChip(
                            label = "WITH GAME UID",
                            isSelected = selectedFilter == "PRIORITY",
                            count = priorityCount,
                            onClick = { selectedFilter = "PRIORITY" }
                        )
                    }
                    item {
                        val minorCount = (uiState as? VerificationState.Success)?.registrations?.count {
                            (it.age ?: 18) < 18
                        } ?: 0
                        VerificationFilterChip(
                            label = "UNDER 18 CHECK",
                            isSelected = selectedFilter == "UNDER18",
                            count = minorCount,
                            onClick = { selectedFilter = "UNDER18" }
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
            }
        },
        containerColor = Color.Black
    ) { innerPadding ->
        when (val state = uiState) {
            is VerificationState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = V14AccentBright, strokeWidth = 2.5.dp)
                        Text("Loading verification queue...", color = V14TextSecondary, fontSize = 12.sp)
                    }
                }
            }
            is VerificationState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(36.dp))
                        Text("Error Loading Queue", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(state.message, color = Color(0xFFA1A1AA), fontSize = 12.sp)
                    }
                }
            }
            is VerificationState.Success -> {
                val rawList = state.registrations
                val filtered = remember(rawList, searchQuery, selectedFilter) {
                    rawList.filter { reg ->
                        val matchesSearch = if (searchQuery.isBlank()) true else {
                            val q = searchQuery.trim().lowercase()
                            reg.playerName.lowercase().contains(q) ||
                            reg.gameUsername.lowercase().contains(q) ||
                            reg.userId.lowercase().contains(q) ||
                            reg.gameAccountId?.lowercase()?.contains(q) == true ||
                            reg.gameId.lowercase().contains(q) ||
                            (reg.region?.lowercase()?.contains(q) == true)
                        }
                        val matchesFilter = when (selectedFilter) {
                            "PRIORITY" -> reg.gameAccountId?.isNotBlank() == true || reg.gameId.isNotBlank()
                            "UNDER18" -> (reg.age ?: 18) < 18
                            else -> true
                        }
                        matchesSearch && matchesFilter
                    }
                }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .padding(innerPadding)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) "No Matching Registrations" else "Verification Queue Clean",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) "Try refining your search keyword" else "All esports player registrations have been reviewed and processed.",
                                color = Color(0xFFA1A1AA),
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 48.dp)
                    ) {
                        items(filtered, key = { it.id }) { registration ->
                            RegistrationCard(
                                registration = registration,
                                onApprove = { viewModel.verifyRegistration(it, true) },
                                onReject = { viewModel.verifyRegistration(it, false) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VerificationFilterChip(
    label: String,
    isSelected: Boolean,
    count: Int,
    onClick: () -> Unit
) {
    val textColor = if (isSelected) V14TextPrimary else V14TextSecondary

    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$label ($count)",
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(2.dp)
                .background(if (isSelected) V14AccentPrimary else Color.Transparent)
        )
    }
}

@Composable
fun RegistrationCard(
    registration: PlayerRegistration,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(V14Bg)
            .padding(vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(V14CardSurface)
                        .border(1.dp, V14Border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = V14AccentBright,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    val displayName = registration.playerName.ifBlank {
                        registration.gameUsername.ifBlank {
                            registration.profile?.username ?: "Registered Player"
                        }
                    }
                    Text(
                        text = displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = V14TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val idToShow = registration.userId.ifBlank { registration.playerId.ifBlank { registration.id } }
                    Text(
                        text = "UID: ${idToShow.take(14)}",
                        fontSize = 11.sp,
                        color = V14TextSecondary
                    )
                }
            }

            Text(
                text = (registration.tournament?.game ?: "Free Fire").uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = V14AccentBright,
                letterSpacing = 0.6.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Flat inline details row without nested box
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("REGION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = V14TextMuted, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(registration.region ?: "India / Global", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = V14TextPrimary)
            }
            Column {
                Text("AGE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = V14TextMuted, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(2.dp))
                val ageVal = registration.age ?: 18
                Text(
                    text = if (ageVal < 18) "$ageVal (Minor)" else "$ageVal yrs",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (ageVal < 18) VelorixAmber else V14TextPrimary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("GAME ACCOUNT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = V14TextMuted, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(2.dp))
                val accountId = registration.gameAccountId?.takeIf { it.isNotBlank() }
                    ?: registration.gameId.takeIf { it.isNotBlank() }
                    ?: "Not Linked"
                Text(
                    text = accountId,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (accountId != "Not Linked") V14AccentBright else V14TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Clean text action row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { onReject(registration.id) }) {
                Icon(Icons.Default.Close, contentDescription = "Reject", tint = V14AccentBright, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("REJECT", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = V14AccentBright)
            }
            Spacer(modifier = Modifier.width(12.dp))
            TextButton(onClick = { onApprove(registration.id) }) {
                Icon(Icons.Default.Check, contentDescription = "Approve", tint = VelorixEmerald, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("APPROVE", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = VelorixEmerald)
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = V14Border)
    }
}
