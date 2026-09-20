package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Tournament
import com.example.domain.model.CheckInToken
import com.example.ui.common.GoogleGIcon
import com.example.ui.theme.*
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInAndAuthScreen(
    tournaments: List<Tournament>,
    initialTokens: List<CheckInToken> = emptyList(),
    onSaveToken: ((CheckInToken) -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current

    var tokenList by remember(initialTokens) {
        mutableStateOf(initialTokens)
    }

    var selectedTournamentForToken by remember { mutableStateOf(tournaments.firstOrNull()?.title ?: "BGMI Pro Tournament") }
    var inputTokenVerify by remember { mutableStateOf("") }
    var verificationStatus by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showNewTokenGeneratedDialog by remember { mutableStateOf<CheckInToken?>(null) }

    // Google Auth Settings toggles state
    var googleAuthEnabled by remember { mutableStateOf(true) }
    var enforceTokenCheckIn by remember { mutableStateOf(true) }
    var autoExpireMinutes by remember { mutableStateOf("15 mins") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onNavigateBack != null) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Column {
                    Text("Check-In Tokens & Auth", color = VelorixTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Player verification & Google login controls", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            // Section 1: Quick Generate Check-In Token
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardLiveBg, RoundedCornerShape(16.dp))
                        .border(1.dp, VelorixAccent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Tournament Check-In Token", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Issue a instant 6-digit passcode token for players entering active tournament lobbies.", color = Color.LightGray, fontSize = 12.sp)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val newTokenCode = "VLX-${Random.nextInt(1000, 9999)}"
                            val newToken = CheckInToken(
                                id = "TK-${Random.nextInt(100, 999)}",
                                tokenCode = newTokenCode,
                                tournamentTitle = selectedTournamentForToken,
                                playerUsername = "Registered_Player_${Random.nextInt(10, 99)}",
                                gameAccountId = "IGN_${Random.nextInt(1000, 9999)}",
                                createdAt = "Just now",
                                isUsed = false
                            )
                            tokenList = listOf(newToken) + tokenList
                            onSaveToken?.invoke(newToken)
                            showNewTokenGeneratedDialog = newToken
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate New Check-In Token", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Section 2: Token Verification Tool
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardLiveBg, RoundedCornerShape(16.dp))
                        .border(1.dp, CardVerifyBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text("Live Token Validator", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputTokenVerify,
                            onValueChange = { inputTokenVerify = it.uppercase() },
                            placeholder = { Text("Enter PIN (e.g. VLX-8821)", color = Color.Gray, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = VelorixAccent,
                                unfocusedBorderColor = CardVerifyBorder
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val match = tokenList.find { it.tokenCode.equals(inputTokenVerify.trim(), ignoreCase = true) }
                                if (match == null) {
                                    verificationStatus = "error" to "Invalid Token PIN"
                                } else if (match.isUsed) {
                                    verificationStatus = "warning" to "Token Already Used by ${match.playerUsername}"
                                } else if (match.isExpired) {
                                    verificationStatus = "error" to "Token Expired"
                                } else {
                                    // Mark as used
                                    val updated = match.copy(isUsed = true)
                                    tokenList = tokenList.map { if (it.tokenCode == match.tokenCode) updated else it }
                                    onSaveToken?.invoke(updated)
                                    verificationStatus = "success" to "VERIFIED: Check-in successful for ${match.playerUsername} (${match.gameAccountId})"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6)),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text("Validate", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    verificationStatus?.let { (type, msg) ->
                        val (icon, color) = when (type) {
                            "success" -> Icons.Default.CheckCircle to Color(0xFF10B981)
                            "warning" -> Icons.Default.Warning to Color(0xFFFFB74D)
                            else -> Icons.Default.Cancel to Color(0xFFEF4444)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF161B28),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(msg, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Section 3: Google Auth & Token Controls Config
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardLiveBg, RoundedCornerShape(16.dp))
                        .border(1.dp, CardVerifyBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GoogleGIcon(size = 18.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Google Auth & Token Security Rules", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enforce Google Sign-In", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Gate tournament entry and prize claims behind Google Auth", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = googleAuthEnabled,
                            onCheckedChange = { googleAuthEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = VelorixAccent)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mandatory Pre-Match Token Check-In", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Players must submit 6-digit PIN before match start", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = enforceTokenCheckIn,
                            onCheckedChange = { enforceTokenCheckIn = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = VelorixAccent)
                        )
                    }
                }
            }

            // Section 4: Active Check-In Token Log
            item {
                Text("Active Check-In Tokens Log", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            items(tokenList) { token ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardLiveBg, RoundedCornerShape(12.dp))
                        .border(1.dp, if (token.isUsed) Color.Green.copy(alpha = 0.3f) else CardVerifyBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(token.tokenCode, color = VelorixAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(token.tournamentTitle, color = Color.Gray, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Player: ${token.playerUsername} (${token.gameAccountId})", color = Color.White, fontSize = 12.sp)
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                if (token.isUsed) Color.Green.copy(alpha = 0.2f)
                                else if (token.isExpired) Color.Red.copy(alpha = 0.2f)
                                else Color(0xFFFFB74D).copy(alpha = 0.2f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (token.isUsed) "CHECKED IN" else if (token.isExpired) "EXPIRED" else "ACTIVE",
                            color = if (token.isUsed) Color.Green else if (token.isExpired) Color.Red else Color(0xFFFFB74D),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Generated Token Success Dialog
    showNewTokenGeneratedDialog?.let { token ->
        AlertDialog(
            onDismissRequest = { showNewTokenGeneratedDialog = null },
            title = { Text("Check-In Token Generated!", color = VelorixAccent, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Share this passcode with player:", color = Color.LightGray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .background(VelorixAccent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .border(2.dp, VelorixAccent, RoundedCornerShape(12.dp))
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(token.tokenCode, color = VelorixAccent, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Tournament: ${token.tournamentTitle}", color = Color.White, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "Passcode ${token.tokenCode} copied!", Toast.LENGTH_SHORT).show()
                        showNewTokenGeneratedDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent)
                ) {
                    Text("Copy Code & Close", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CardLiveBg
        )
    }
}
