package com.example.ui

import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.AdminRecord
import com.example.ui.theme.*
import com.example.ui.viewmodel.DashboardState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StaffManagementScreen(
    uiState: DashboardState,
    onGrantAdmin: ((email: String, name: String, role: String) -> Unit)? = null,
    onRevokeAdmin: ((adminUid: String) -> Unit)? = null,
    onUpdateAdmin: ((AdminRecord) -> Unit)? = null,
    onDeleteAdmin: ((adminUid: String) -> Unit)? = null,
    onNavigateBack: () -> Unit
) {
    ElaborateScreen("Staff Management & Admin Roles", onNavigateBack) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            StaffManagementScreenContent(
                uiState = uiState,
                onGrantAdmin = onGrantAdmin,
                onRevokeAdmin = onRevokeAdmin,
                onUpdateAdmin = onUpdateAdmin,
                onDeleteAdmin = onDeleteAdmin
            )
        }
    }
}

@Composable
fun StaffManagementScreenContent(
    uiState: DashboardState,
    onGrantAdmin: ((email: String, name: String, role: String) -> Unit)? = null,
    onRevokeAdmin: ((adminUid: String) -> Unit)? = null,
    onUpdateAdmin: ((AdminRecord) -> Unit)? = null,
    onDeleteAdmin: ((adminUid: String) -> Unit)? = null
) {
    val successState = uiState as? DashboardState.Success
    val adminsList = successState?.admins ?: emptyList()
    val activeAdminsCount = adminsList.count { it.active }

    var showGrantDialog by remember { mutableStateOf(false) }
    var newEmail by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("tournament_admin") }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    var inspectedAdmin by remember { mutableStateOf<AdminRecord?>(null) }
    val activeInspectedAdmin = remember(adminsList, inspectedAdmin) {
        inspectedAdmin?.let { current -> adminsList.find { it.uid == current.uid } ?: current }
    }

    // Filtered Admins
    val filteredAdmins = remember(adminsList, searchQuery, selectedFilter) {
        adminsList.filter { admin ->
            val matchesSearch = searchQuery.isBlank() ||
                    admin.name.contains(searchQuery, ignoreCase = true) ||
                    admin.email.contains(searchQuery, ignoreCase = true) ||
                    admin.uid.contains(searchQuery, ignoreCase = true) ||
                    admin.role.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "ACTIVE" -> admin.active
                "REVOKED" -> !admin.active
                "SUPER_ADMIN" -> admin.role.equals("super_admin", ignoreCase = true)
                "TOURNAMENT_ADMIN" -> admin.role.equals("tournament_admin", ignoreCase = true)
                "SUPPORT_ADMIN" -> admin.role.equals("support_admin", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    if (showGrantDialog) {
        AlertDialog(
            onDismissRequest = { showGrantDialog = false },
            title = { Text("Grant Admin Staff Access", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Add staff credentials to RTDB /admins node.", color = Color.LightGray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("Email Address*", color = Color.Gray) },
                        placeholder = { Text("admin@velorix.com") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = VelorixAccent,
                            unfocusedBorderColor = CardVerifyBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Display Name", color = Color.Gray) },
                        placeholder = { Text("e.g. Lead Moderator") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = VelorixAccent,
                            unfocusedBorderColor = CardVerifyBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Select Role Level:", color = Color.Gray, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("super_admin", "tournament_admin", "support_admin").forEach { role ->
                            val isSel = selectedRole == role
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) VelorixAccent else CardLiveBg, RoundedCornerShape(8.dp))
                                    .clickable { selectedRole = role }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    role.replace("_", " ").uppercase(),
                                    color = if (isSel) Color.Black else Color.LightGray,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newEmail.isNotBlank()) {
                            onGrantAdmin?.invoke(newEmail.trim(), newName.trim().ifBlank { newEmail.substringBefore("@") }, selectedRole)
                            showGrantDialog = false
                            newEmail = ""
                            newName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent)
                ) {
                    Text("Grant Access", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGrantDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = CardLiveBg
        )
    }

    // Full Admin Detail / Inspector Dialog (identical parity with Player / User Details dialog)
    activeInspectedAdmin?.let { admin ->
        AdminDetailsInspectorDialog(
            admin = admin,
            onDismiss = { inspectedAdmin = null },
            onToggleStatus = { updatedAdmin ->
                onUpdateAdmin?.invoke(updatedAdmin)
                inspectedAdmin = updatedAdmin
            },
            onUpdateAdmin = { updatedAdmin ->
                onUpdateAdmin?.invoke(updatedAdmin)
                inspectedAdmin = updatedAdmin
            },
            onDeleteAdmin = { uid ->
                onDeleteAdmin?.invoke(uid)
                inspectedAdmin = null
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Responsive Header (Prevents edge crushing on mobile screens)
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
                    text = "Admin Management & Roles",
                    color = VelorixTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showGrantDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Add Admin", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap any admin to inspect details & edit credentials",
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
                        text = "$activeAdminsCount / ${adminsList.size} Active",
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
            placeholder = { Text("Search admin by UID, Name, Email, Role...", color = Color.Gray, fontSize = 13.sp) },
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

        // Filter chips (horizontally scrollable to avoid crushing)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp)
        ) {
            listOf("ALL", "ACTIVE", "REVOKED", "SUPER_ADMIN", "TOURNAMENT_ADMIN").forEach { f ->
                val isSel = selectedFilter == f
                val label = when (f) {
                    "SUPER_ADMIN" -> "SUPER"
                    "TOURNAMENT_ADMIN" -> "TOURNAMENT"
                    else -> f
                }
                Box(
                    modifier = Modifier
                        .background(if (isSel) VelorixAccent else CardLiveBg, RoundedCornerShape(8.dp))
                        .clickable { selectedFilter = f }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(label, color = if (isSel) Color.Black else Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        val distinctFilteredAdmins = remember(filteredAdmins) {
            val seen = mutableSetOf<String>()
            filteredAdmins.filter { admin ->
                val k = if (admin.uid.isNotBlank()) "uid:${admin.uid}" else "email:${admin.email.trim().lowercase()}"
                if (k.isNotBlank() && seen.add(k)) true else if (k.isBlank()) true else false
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 90.dp)) {
            if (distinctFilteredAdmins.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.SupervisorAccount, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No matching admin records found.", color = VelorixTextSecondary, fontSize = 14.sp)
                    }
                }
            }

            items(distinctFilteredAdmins, key = { "${it.uid}_${it.email}_${distinctFilteredAdmins.indexOf(it)}" }) { admin ->
                RealAdminCard(
                    admin = admin,
                    onClick = { inspectedAdmin = admin },
                    onEditClick = { inspectedAdmin = admin },
                    onRevoke = { onRevokeAdmin?.invoke(admin.uid) },
                    onToggleStatus = { updatedAdmin ->
                        onUpdateAdmin?.invoke(updatedAdmin)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun RealAdminCard(
    admin: AdminRecord,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onRevoke: () -> Unit,
    onToggleStatus: (AdminRecord) -> Unit = {}
) {
    val dateStr = remember(admin.assignedAt) {
        if (admin.assignedAt > 0) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(admin.assignedAt))
        } else "N/A"
    }

    val roleColor = when (admin.role.lowercase()) {
        "super_admin" -> Color(0xFFFF9800)
        "tournament_admin" -> VelorixAccent
        "support_admin" -> Color(0xFF03A9F4)
        else -> VelorixAccent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardLiveBg, RoundedCornerShape(16.dp))
            .border(1.dp, if (admin.active) CardVerifyBorder else Color.Red.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(if (admin.active) roleColor.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f), CircleShape)
                        .border(1.5.dp, if (admin.active) roleColor else Color.Red, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (admin.name.firstOrNull() ?: admin.email.firstOrNull() ?: 'A').uppercase(),
                        color = if (admin.active) roleColor else Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(admin.name.ifBlank { "Admin" }, color = VelorixTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(if (admin.active) Color(0xFF2E7D32) else Color.Red, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (admin.active) "ACTIVE" else "REVOKED",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(admin.email, color = VelorixTextSecondary, fontSize = 11.sp)
                }
            }
            Box(
                modifier = Modifier
                    .background(roleColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(admin.role.replace("_", " ").uppercase(), color = roleColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = Color(0xFF2A2A2A))
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Assigned Date", color = Color.Gray, fontSize = 10.sp)
                Text(dateStr, color = VelorixTextPrimary, fontSize = 12.sp)
            }
            Column {
                Text("Granted By", color = Color.Gray, fontSize = 10.sp)
                Text(admin.grantedBy.ifBlank { "System" }, color = Color.White, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("UID", color = Color.Gray, fontSize = 10.sp)
                Text(admin.uid.take(10), color = VelorixAccent, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onEditClick,
                modifier = Modifier.weight(1.2f).height(34.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("EDIT DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }

            if (admin.active) {
                Button(
                    onClick = onRevoke,
                    modifier = Modifier.weight(0.9f).height(34.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.85f)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("REVOKE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else {
                Button(
                    onClick = {
                        onToggleStatus(admin.copy(active = true))
                    },
                    modifier = Modifier.weight(0.9f).height(34.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("ACTIVATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun AdminDetailsInspectorDialog(
    admin: AdminRecord,
    onDismiss: () -> Unit,
    onToggleStatus: (AdminRecord) -> Unit,
    onUpdateAdmin: (AdminRecord) -> Unit,
    onDeleteAdmin: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) }

    // Editable state
    var editName by remember(admin) { mutableStateOf(admin.name) }
    var editEmail by remember(admin) { mutableStateOf(admin.email) }
    var editRole by remember(admin) { mutableStateOf(admin.role) }
    var editGrantedBy by remember(admin) { mutableStateOf(admin.grantedBy) }
    var editActive by remember(admin) { mutableStateOf(admin.active) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val assignedStr = remember(admin.assignedAt) {
        if (admin.assignedAt > 0) dateFormat.format(Date(admin.assignedAt)) else "N/A"
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Admin Permanently?", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to permanently delete admin account ${admin.name} (${admin.email}) from /admins database? This will completely wipe all administrative privileges.",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteAdmin(admin.uid)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete Admin", color = Color.White, fontWeight = FontWeight.Bold)
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
            border = BorderStroke(1.dp, CardVerifyBorder)
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
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(if (admin.active) VelorixAccent.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f), CircleShape)
                                .border(2.dp, if (admin.active) VelorixAccent else Color.Red, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (admin.name.firstOrNull() ?: admin.email.firstOrNull() ?: 'A').uppercase(),
                                color = if (admin.active) VelorixAccent else Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = admin.name.ifBlank { "Admin" },
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(if (admin.active) Color(0xFF2E7D32) else Color.Red, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (admin.active) "ACTIVE" else "REVOKED",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = admin.email,
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
                        Text("ADMIN UID: ", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = admin.uid,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                    TextButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(admin.uid))
                            Toast.makeText(context, "Admin UID copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = VelorixAccent, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy UID", color = VelorixAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Tabs: 0 -> Details & Role, 1 -> Controls & Edit, 2 -> Raw Admin DB Data
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
                        text = { Text("Details & Roles", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Controls & Edit", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Raw DB Data", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                // Tab Contents
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(14.dp)) {
                    when (selectedTab) {
                        0 -> {
                            // Details & Role Metadata
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DetailSectionCard(title = "ADMIN PRIVILEGES & ROLE", icon = Icons.Default.Security) {
                                    DetailItemRow(label = "Assigned Role", value = admin.role.replace("_", " ").uppercase(), isHighlight = true)
                                    DetailItemRow(label = "Account Status", value = if (admin.active) "Active Verified" else "Revoked / Disabled")
                                    DetailItemRow(label = "Granted By", value = admin.grantedBy.ifBlank { "super_admin@velorix.com" })
                                    DetailItemRow(label = "Assignment Date", value = assignedStr)
                                }

                                DetailSectionCard(title = "IDENTITY & COMMUNICATIONS", icon = Icons.Default.Person) {
                                    DetailItemRow(label = "Admin Name / Title", value = admin.name)
                                    DetailItemRow(label = "Email Address", value = admin.email, isHighlight = true)
                                    DetailItemRow(label = "Database Key (UID)", value = admin.uid)
                                }

                                DetailSectionCard(title = "PERMITTED CAPABILITIES", icon = Icons.Default.VerifiedUser) {
                                    when (admin.role.lowercase()) {
                                        "super_admin" -> {
                                            DetailItemRow(label = "Tournament Oversight", value = "Full Publish / Edit / Delete")
                                            DetailItemRow(label = "User Management", value = "Ban / Unban / Adjust Funds")
                                            DetailItemRow(label = "Payouts & Finance", value = "Approve / Reject Cashouts")
                                            DetailItemRow(label = "Staff Delegation", value = "Grant & Revoke Admin Roles")
                                        }
                                        "tournament_admin" -> {
                                            DetailItemRow(label = "Tournament Oversight", value = "Publish / Edit / Room ID Updates")
                                            DetailItemRow(label = "User Management", value = "View & Verify Players")
                                            DetailItemRow(label = "Payouts & Finance", value = "View Payout Requests")
                                            DetailItemRow(label = "Staff Delegation", value = "Restricted")
                                        }
                                        else -> {
                                            DetailItemRow(label = "Support Tickets", value = "Resolve & Reply to Tickets")
                                            DetailItemRow(label = "User Management", value = "Verify Players & Tokens")
                                            DetailItemRow(label = "Tournament Oversight", value = "View Only")
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Controls & Edit
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "ADMIN MODIFICATION CONTROLS",
                                    color = VelorixAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )

                                // 1. Edit Identity
                                DetailSectionCard(title = "CHANGE ADMIN PROFILE INFO", icon = Icons.Default.Edit) {
                                    OutlinedTextField(
                                        value = editName,
                                        onValueChange = { editName = it },
                                        label = { Text("Display Name / Title", color = Color.Gray, fontSize = 11.sp) },
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
                                        label = { Text("Admin Email", color = Color.Gray, fontSize = 11.sp) },
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
                                        value = editGrantedBy,
                                        onValueChange = { editGrantedBy = it },
                                        label = { Text("Granted By / Supervised By", color = Color.Gray, fontSize = 11.sp) },
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
                                            val updated = admin.copy(
                                                name = editName.trim().ifBlank { admin.name },
                                                email = editEmail.trim().ifBlank { admin.email },
                                                grantedBy = editGrantedBy.trim()
                                            )
                                            onUpdateAdmin(updated)
                                            Toast.makeText(context, "Admin info updated successfully", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent),
                                        modifier = Modifier.fillMaxWidth().height(38.dp)
                                    ) {
                                        Text("Save Admin Info", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                // 2. Assign Role
                                DetailSectionCard(title = "ASSIGN ADMIN PRIVILEGE ROLE", icon = Icons.Default.Security) {
                                    Text("Current Role: ${admin.role}", color = Color.LightGray, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("super_admin", "tournament_admin", "support_admin").forEach { roleOption ->
                                            val isSelected = editRole.equals(roleOption, ignoreCase = true)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(if (isSelected) VelorixAccent else Color(0xFF232936), RoundedCornerShape(8.dp))
                                                    .border(1.dp, if (isSelected) VelorixAccent else CardVerifyBorder, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    editRole = roleOption
                                                    val updated = admin.copy(role = roleOption)
                                                    onUpdateAdmin(updated)
                                                    Toast.makeText(context, "Role changed to ${roleOption.replace("_", " ").uppercase()}", Toast.LENGTH_SHORT).show()
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

                                // 3. Active Status / Revoke Toggle
                                DetailSectionCard(title = "ACCESS STATUS CONTROL", icon = Icons.Default.Lock) {
                                    Text(
                                        text = if (admin.active) "Staff member currently has full active panel access." else "Staff member's access is currently REVOKED.",
                                        color = if (admin.active) Color.Green else Color.Red,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            val newActive = !admin.active
                                            editActive = newActive
                                            val updated = admin.copy(active = newActive)
                                            onToggleStatus(updated)
                                            Toast.makeText(
                                                context,
                                                if (newActive) "Admin access restored" else "Admin access revoked",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (admin.active) Color.Red else Color(0xFF2E7D32)
                                        ),
                                        modifier = Modifier.fillMaxWidth().height(38.dp)
                                    ) {
                                        Text(
                                            if (admin.active) "REVOKE ACCESS PRIVILEGES" else "RESTORE ACCESS PRIVILEGES",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                // 4. Danger Zone: Permanent Deletion
                                DetailSectionCard(title = "DANGER ZONE", icon = Icons.Default.Warning) {
                                    Text("Permanently remove this admin record from Firebase Realtime Database.", color = Color.Gray, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { showDeleteConfirmDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                                        border = BorderStroke(1.dp, Color.Red),
                                        modifier = Modifier.fillMaxWidth().height(38.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("PERMANENTLY DELETE ADMIN", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Raw DB Data View
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("FIREBASE /admins/${admin.uid}", color = VelorixAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F1218), RoundedCornerShape(12.dp))
                                        .border(1.dp, CardVerifyBorder, RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("\"uid\": \"${admin.uid}\"", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Text("\"name\": \"${admin.name}\"", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Text("\"email\": \"${admin.email}\"", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Text("\"role\": \"${admin.role}\"", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Text("\"active\": ${admin.active}", color = if (admin.active) Color.Green else Color.Red, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Text("\"assignedAt\": ${admin.assignedAt}", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Text("\"grantedBy\": \"${admin.grantedBy}\"", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
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
