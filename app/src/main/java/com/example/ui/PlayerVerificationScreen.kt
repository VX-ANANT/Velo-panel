package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VelorixBg)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(VelorixAccentLight, CircleShape)
                        .border(2.dp, VelorixAccentBorder, CircleShape)
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = VelorixAccentDark,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Player Verification",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelorixTextPrimary
                    )
                    Text(
                        text = "PENDING QUEUE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelorixTextSecondary,
                        letterSpacing = 1.sp
                    )
                }
            }
        },
        containerColor = VelorixBg
    ) { innerPadding ->
        when (uiState) {
            is VerificationState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VelorixAccent)
                }
            }
            is VerificationState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text(text = "Error: ${(uiState as VerificationState.Error).message}", color = Color.Red)
                }
            }
            is VerificationState.Success -> {
                val registrations = (uiState as VerificationState.Success).registrations
                if (registrations.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        Text(text = "No pending registrations.", color = VelorixTextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(registrations) { registration ->
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
fun RegistrationCard(
    registration: PlayerRegistration,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardVerifyBg, RoundedCornerShape(24.dp))
            .border(1.dp, CardVerifyBorder, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = registration.profile?.username ?: "Unknown User",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = VelorixTextPrimary
                )
                Text(
                    text = "ID: ${registration.userId.take(8)}...",
                    fontSize = 12.sp,
                    color = VelorixTextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .background(VelorixAccentLight, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = registration.tournament?.game ?: "Unknown Game",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelorixAccentDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Region", fontSize = 10.sp, color = VelorixTextSecondary)
                Text(registration.region ?: "N/A", fontSize = 14.sp, color = VelorixTextPrimary)
            }
            Column {
                Text("Age", fontSize = 10.sp, color = VelorixTextSecondary)
                Text(registration.age?.toString() ?: "N/A", fontSize = 14.sp, color = VelorixTextPrimary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Game Account", fontSize = 10.sp, color = VelorixTextSecondary)
                Text(registration.gameAccountId ?: "Not Linked", fontSize = 14.sp, color = VelorixTextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onReject(registration.id) },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E), contentColor = Color(0xFFEF5350)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Reject", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("REJECT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { onApprove(registration.id) },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Approve", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("APPROVE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
