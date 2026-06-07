package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.domain.model.Tournament
import com.example.ui.viewmodel.DashboardState
import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton

@Composable
fun DashboardScreen(
    uiState: DashboardState,
    onSettingsClick: (String) -> Unit,
    onCreateTournamentClick: () -> Unit,
    onVerifyClick: () -> Unit,
    onBracketClick: (String) -> Unit,
    onNavClick: (String) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateTournamentClick,
                containerColor = VelorixAccent,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Tournament")
            }
        },
        bottomBar = { DashboardBottomNavBar(onNavClick = onNavClick) },
        containerColor = VelorixBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(VelorixBg)
        ) {
            DashboardHeader(onProfileClick = { onNavClick("staff_management") })
            
            when (uiState) {
                is DashboardState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = VelorixAccent)
                    }
                }
                is DashboardState.Success -> {
                    DashboardContent(
                        uiState = uiState,
                        onSettingsClick = onSettingsClick,
                        onVerifyClick = onVerifyClick,
                        onBracketClick = onBracketClick,
                        onProfilesClick = { onNavClick("player_profiles") },
                        onAnalyticsClick = { onNavClick("analytics") }
                    )
                }
                is DashboardState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${uiState.message}", color = Color.Red)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardHeader(onProfileClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(VelorixAccent, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Admin Shield",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = VelorixTextPrimary)) {
                            append("Velorix ")
                        }
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = VelorixAccent)) {
                            append("Admin")
                        }
                    },
                    fontSize = 18.sp,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "TOURNAMENT OVERSIGHT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = VelorixTextSecondary,
                    letterSpacing = 1.sp
                )
            }
        }
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(VelorixAccentLight, CircleShape)
                .border(2.dp, VelorixAccentBorder, CircleShape)
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile",
                tint = VelorixAccentDark,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun DashboardContent(
    uiState: DashboardState.Success,
    onSettingsClick: (String) -> Unit,
    onVerifyClick: () -> Unit,
    onBracketClick: (String) -> Unit,
    onProfilesClick: () -> Unit,
    onAnalyticsClick: () -> Unit
) {
    val liveTournament = uiState.tournaments.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Live Bracket Card
        if (liveTournament != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardLiveBg, RoundedCornerShape(24.dp))
                    .border(1.dp, VelorixAccentDark.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                    .clickable { onBracketClick(liveTournament.id) }
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .background(VelorixAccentDark, CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LIVE NOW",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = liveTournament.title,
                                color = VelorixAccentDark,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.4f), CircleShape)
                                .clickable { onSettingsClick(liveTournament.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Tournament Settings",
                                tint = VelorixAccentDark
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Status: ${liveTournament.status}",
                                color = VelorixAccentDark.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row {
                                // Mock avatars
                                Box(modifier = Modifier.size(24.dp).background(Color(0xFFEF5350), CircleShape).border(1.dp, Color.White, CircleShape))
                                Box(modifier = Modifier.size(24.dp).background(Color(0xFF42A5F5), CircleShape).border(1.dp, Color.White, CircleShape))
                                Box(modifier = Modifier.size(24.dp).background(Color(0xFFE0E0E0), CircleShape).border(1.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                                    Text("+4", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Black, color = VelorixAccentDark)) {
                                    append(liveTournament.registeredPlayers.toString())
                                }
                                withStyle(style = SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = VelorixAccentDark)) {
                                    append("/${liveTournament.maxPlayers}")
                                }
                            }
                        )
                    }
                }
            }
        } else {
            Text("No tournaments available", color = VelorixTextSecondary)
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Verify Queue
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(CardVerifyBg, RoundedCornerShape(24.dp))
                    .border(1.dp, CardVerifyBorder, RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(CardVerifyIconBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Verify",
                            tint = CardVerifyIcon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "VERIFY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VelorixTextSecondary, letterSpacing = (-0.5).sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = uiState.pendingRegistrationsCount.toString(), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary)
                Text(text = "Players pending manual review", fontSize = 10.sp, color = VelorixTextSecondary, lineHeight = 12.sp)
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = { onVerifyClick() },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "REVIEW ALL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            // Analytics
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onAnalyticsClick() }
                    .background(CardAnalyticsBg, RoundedCornerShape(24.dp))
                    .border(1.dp, CardAnalyticsBorder, RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = "Trending Up",
                        tint = VelorixAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(text = "+24%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VelorixAccent)
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Row(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(modifier = Modifier.weight(1f).height(16.dp).background(VelorixAccent.copy(alpha = 0.2f), RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)))
                    Box(modifier = Modifier.weight(1f).height(32.dp).background(VelorixAccent.copy(alpha = 0.4f), RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)))
                    Box(modifier = Modifier.weight(1f).height(24.dp).background(VelorixAccent.copy(alpha = 0.6f), RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)))
                    Box(modifier = Modifier.weight(1f).height(40.dp).background(VelorixAccent.copy(alpha = 1f), RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)))
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = VelorixTextPrimary)) {
                            append("${uiState.totalRegistrationsCount} ")
                        }
                        withStyle(style = SpanStyle(fontSize = 10.sp, fontWeight = FontWeight.Normal, color = VelorixTextPrimary)) {
                            append("Regs")
                        }
                    },
                    letterSpacing = (-0.5).sp
                )
            }
        }
        
        // Payout Pool Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardPayoutBg, RoundedCornerShape(24.dp))
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PAYOUT POOL",
                    color = Color(0xFFE6E1E5).copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "$${uiState.payoutPool}",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(96.dp)
                            .height(6.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(6.dp)
                                .background(CardLiveBg, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "75% Distributed",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(CardLiveBg, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = "Payments",
                    tint = VelorixAccentDark,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun DashboardBottomNavBar(onNavClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(NavBarBg)
            .border(1.dp, NavBarBorder)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavBarItem(
            icon = Icons.Default.GridView,
            label = "Overview",
            isSelected = true,
            onClick = { onNavClick("dashboard") }
        )
        NavBarItem(
            icon = Icons.Default.EmojiEvents,
            label = "Tourneys",
            isSelected = false,
            onClick = { onNavClick("tournaments_list") }
        )
        NavBarItem(
            icon = Icons.Default.Group,
            label = "Users",
            isSelected = false,
            onClick = { onNavClick("player_profiles") }
        )
        NavBarItem(
            icon = Icons.Default.Settings,
            label = "Config",
            isSelected = false,
            onClick = { onNavClick("global_settings") }
        )
    }
}

@Composable
fun NavBarItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .background(NavBarSelectedBg, CircleShape)
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color(0xFF1D192B)
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = VelorixTextSecondary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF1D192B) else VelorixTextSecondary
        )
    }
}
