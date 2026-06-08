package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Tournament
import com.example.ui.theme.*
import com.example.ui.viewmodel.DashboardState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElaborateScreen(title: String, onNavigateBack: () -> Unit, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = VelorixTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(40.dp)
                            .background(VelorixAccentLight, CircleShape)
                            .border(2.dp, VelorixAccentBorder, CircleShape)
                            .clickable { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VelorixAccentDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VelorixBg)
            )
        },
        containerColor = VelorixBg
    ) { padding ->
        content(padding)
    }
}

@Composable
fun TournamentsListScreen(uiState: DashboardState, onNavigateBack: () -> Unit) {
    ElaborateScreen("All Tournaments", onNavigateBack) { padding ->
        var selectedTab by remember { mutableStateOf(0) }
        val tournamentsList = (uiState as? DashboardState.Success)?.tournaments ?: emptyList()
        val displayList = when (selectedTab) {
            0 -> tournamentsList
            1 -> tournamentsList.filter { it.status.equals("upcoming", ignoreCase = true) }
            2 -> tournamentsList.filter { it.status.equals("live", ignoreCase = true) }
            else -> tournamentsList.filter { it.status.equals("ended", ignoreCase = true) }
        }
        
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
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
                listOf("All", "Upcoming", "Ongoing", "Completed").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, color = if (selectedTab == index) VelorixAccent else VelorixTextSecondary) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (displayList.isEmpty()) {
                    item {
                        Text("No tournaments found.", color = VelorixTextSecondary, modifier = Modifier.padding(16.dp))
                    }
                }
                items(displayList.size) { i ->
                    val t = displayList[i]
                    val statusColor = when (t.status.lowercase()) {
                        "upcoming" -> Color(0xFF64B5F6)
                        "live" -> Color(0xFF81C784)
                        "ended" -> Color(0xFFE0E0E0)
                        else -> Color(0xFFBCAAA4)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardLiveBg, RoundedCornerShape(16.dp))
                            .border(1.dp, VelorixAccentDark.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(t.title, fontWeight = FontWeight.Bold, color = VelorixTextPrimary, fontSize = 18.sp)
                            Box(modifier = Modifier.background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(4.dp)) {
                                Text(t.status.uppercase(), color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Prize Pool: $${t.prizePool}", color = VelorixTextSecondary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Game: ${t.game}", color = VelorixTextPrimary, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsScreen(onNavigateBack: () -> Unit) {
    ElaborateScreen("Analytics & Revenue", onNavigateBack) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(64.dp), tint = VelorixTextSecondary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Syncing live analytics from Supabase...", color = VelorixTextSecondary)
        }
    }
}

@Composable
fun AnalyticsCard(modifier: Modifier = Modifier, title: String, value: String) {
    Column(
        modifier = modifier
            .background(CardAnalyticsBg, RoundedCornerShape(16.dp))
            .border(1.dp, CardAnalyticsBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(title, color = VelorixTextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, color = VelorixTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GlobalSettingsScreen(onNavigateBack: () -> Unit) {
    ElaborateScreen("Platform Settings", onNavigateBack) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("General", color = VelorixAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            item { SettingsToggle("Allow Direct Player Registration", true) }
            item { SettingsToggle("Require ID Verification", false) }
            item { SettingsToggle("Auto-Approve Prize Payouts", false) }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item { Text("Notifications", color = VelorixAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            item { SettingsToggle("Tournament Start Alerts", true) }
            item { SettingsToggle("Admin Dispute Notifications", true) }
        }
    }
}

@Composable
fun SettingsToggle(label: String, initial: Boolean) {
    var checked by remember { mutableStateOf(initial) }
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp)).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = VelorixTextPrimary, fontSize = 16.sp)
        Switch(checked = checked, onCheckedChange = { checked = it }, colors = SwitchDefaults.colors(checkedThumbColor = VelorixAccent))
    }
}

@Composable
fun PlayerProfilesScreen(onNavigateBack: () -> Unit) {
    ElaborateScreen("Global Leaderboard", onNavigateBack) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = VelorixTextSecondary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Real-time leaderboard syncing in progress.", color = VelorixTextSecondary)
        }
    }
}
