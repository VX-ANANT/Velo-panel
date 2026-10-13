package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.PlayerRegistration
import com.example.ui.common.GlassBackgroundBox
import com.example.ui.common.GlassCard
import com.example.ui.common.GlassTokens
import com.example.ui.common.bounceClick
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

    GlassBackgroundBox(accentColor = Color(0xFF10B981)) {
        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF18181B))
                                .border(1.dp, Color(0xFF3F3F46), CircleShape)
                                .bounceClick { onNavigateBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "PLAYER VERIFICATION",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "GATEWAY",
                                        color = Color(0xFF34D399),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Review, authenticate and approve esports player registrations",
                                fontSize = 11.5.sp,
                                color = Color(0xFFA1A1AA)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sleek BitChord-style Search Field
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(Color(0xFF121216), RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFF27272A), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF71717A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Search by player IGN, Game ID, UID or Region...",
                                        color = Color(0xFF71717A),
                                        fontSize = 12.5.sp
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = Color.White,
                                        fontSize = 13.sp
                                    ),
                                    cursorBrush = SolidColor(Color(0xFF10B981)),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color.Gray,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { searchQuery = "" }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // BitChord style filter pill tabs
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            VerificationFilterChip(
                                label = "All Registrations",
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
                                label = "With Game UID",
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
                                label = "Under 18 Check",
                                isSelected = selectedFilter == "UNDER18",
                                count = minorCount,
                                onClick = { selectedFilter = "UNDER18" }
                            )
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            when (val state = uiState) {
                is VerificationState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = Color(0xFF10B981), strokeWidth = 2.5.dp)
                            Text("Loading verification queue...", color = Color(0xFFA1A1AA), fontSize = 12.sp)
                        }
                    }
                }
                is VerificationState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderBrush = Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFF7F1D1D)))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(36.dp))
                                Text("Error Loading Queue", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(state.message, color = Color(0xFFA1A1AA), fontSize = 12.sp)
                            }
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
                                .padding(innerPadding)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(0.9f),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape)
                                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
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
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
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
}

@Composable
fun VerificationFilterChip(
    label: String,
    isSelected: Boolean,
    count: Int,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF18181B)
    val border = if (isSelected) Color(0xFF10B981).copy(alpha = 0.8f) else Color(0xFF27272A)
    val textColor = if (isSelected) Color(0xFF34D399) else Color(0xFFA1A1AA)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
        Box(
            modifier = Modifier
                .background(if (isSelected) Color(0xFF10B981) else Color(0xFF27272A), CircleShape)
                .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
            Text(
                text = count.toString(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.Black else Color(0xFFA1A1AA)
            )
        }
    }
}

@Composable
fun RegistrationCard(
    registration: PlayerRegistration,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        borderBrush = Brush.horizontalGradient(
            listOf(
                Color(0xFF27272A),
                Color(0xFF3F3F46),
                Color(0xFF27272A)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))),
                                CircleShape
                            )
                            .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(22.dp)
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
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val idToShow = registration.userId.ifBlank { registration.playerId.ifBlank { registration.id } }
                        Text(
                            text = "UID: ${idToShow.take(14)}",
                            fontSize = 11.sp,
                            color = Color(0xFFA1A1AA)
                        )
                    }
                }

                // Game Badge with glowing border
                Box(
                    modifier = Modifier
                        .background(Color(0xFF064E3B).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = registration.tournament?.game ?: "Free Fire",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34D399)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Details Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B0B0E), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF1C1C22), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("REGION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF71717A), letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(registration.region ?: "India / Global", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Column {
                    Text("AGE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF71717A), letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    val ageVal = registration.age ?: 18
                    Text(
                        text = if (ageVal < 18) "$ageVal (Minor)" else "$ageVal yrs",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (ageVal < 18) Color(0xFFFBBF24) else Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("GAME ACCOUNT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF71717A), letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    val accountId = registration.gameAccountId?.takeIf { it.isNotBlank() }
                        ?: registration.gameId.takeIf { it.isNotBlank() }
                        ?: "Not Linked"
                    Text(
                        text = accountId,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (accountId != "Not Linked") Color(0xFF60A5FA) else Color(0xFF71717A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons with high-contrast text and crisp tactile colors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Reject Button
                Button(
                    onClick = { onReject(registration.id) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF271313),
                        contentColor = Color(0xFFF87171)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Reject", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("REJECT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Approve Button (Emerald Green with high contrast dark text, fixed white-on-white bug!)
                Button(
                    onClick = { onApprove(registration.id) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color(0xFF022C22)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Approve", modifier = Modifier.size(16.dp), tint = Color(0xFF022C22))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("APPROVE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF022C22))
                }
            }
        }
    }
}
