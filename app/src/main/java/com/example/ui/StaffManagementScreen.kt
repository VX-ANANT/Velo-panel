package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.DashboardState

data class AdminMock(
    val name: String,
    val role: String,
    val onlineTime: String,
    val offlineTime: String,
    val status: Boolean, // true if online
    val matchesConducted: Int
)

val mockAdmins = listOf(
    AdminMock("John Doe", "Admin / Host", "09:00 AM", "05:00 PM", false, 45),
    AdminMock("Jane Smith", "Editor", "10:30 AM", "Current", true, 12),
    AdminMock("Mike Johnson", "Visitor", "02:00 PM", "04:00 PM", false, 0),
    AdminMock("Alex Ranger", "Admin / Host", "08:15 AM", "Current", true, 60)
)

@Composable
fun StaffManagementScreen(
    uiState: DashboardState,
    onNavigateBack: () -> Unit
) {
    val currentUserEmail = (uiState as? DashboardState.Success)?.currentUserEmail

    // Only allow owner to see details
    if (currentUserEmail != "anantisback47@gmail.com") {
        ElaborateScreen("Access Denied", onNavigateBack) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Access Denied",
                        tint = Color.Red,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "You do not have permission to view this page.",
                        color = VelorixTextPrimary,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Required role: Owner",
                        color = VelorixTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
        return
    }

    ElaborateScreen("Staff Management (Owner)", onNavigateBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Staff Salary (Unpaid)", color = VelorixTextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        // Calculate total salary: total matches * 2 INR
                        val totalMatches = mockAdmins.sumOf { it.matchesConducted }
                        val totalSalary = totalMatches * 2
                        Text("₹ $totalSalary", color = VelorixAccent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(VelorixAccentLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = "Security", tint = VelorixAccentDark)
                    }
                }
            }

            item {
                Text(
                    text = "Admin Activity & Payroll",
                    color = VelorixTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            items(mockAdmins) { admin ->
                AdminCard(admin)
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun AdminCard(admin: AdminMock) {
    val salary = admin.matchesConducted * 2

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161616), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF2A2A2A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SupervisorAccount, contentDescription = null, tint = VelorixTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(admin.name, color = VelorixTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(admin.role, color = VelorixAccent, fontSize = 12.sp)
                    }
                }
                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = VelorixTextSecondary)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFF2A2A2A))
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Schedule", color = VelorixTextSecondary, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${admin.onlineTime} - ${admin.offlineTime}", color = VelorixTextPrimary, fontSize = 12.sp)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Status", color = VelorixTextSecondary, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Circle,
                            contentDescription = null,
                            tint = if (admin.status) Color.Green else Color.Gray,
                            modifier = Modifier.size(8.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (admin.status) "Online" else "Offline", color = VelorixTextPrimary, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF222222), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Matches Conducted", color = VelorixTextSecondary, fontSize = 10.sp)
                    Text(admin.matchesConducted.toString(), color = VelorixTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Salary (₹2 / Match)", color = VelorixTextSecondary, fontSize = 10.sp)
                    Text("₹ $salary", color = VelorixAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
