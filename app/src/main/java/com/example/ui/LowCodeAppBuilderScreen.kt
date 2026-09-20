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
import androidx.compose.material.icons.automirrored.filled.Chat
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
import com.example.domain.model.AppAnnouncementBanner
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LowCodeAppBuilderScreen(
    onOpenGeminiChatbot: () -> Unit,
    initialBanners: List<AppAnnouncementBanner> = emptyList(),
    onSaveBanner: ((AppAnnouncementBanner) -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current

    var banners by remember(initialBanners) {
        mutableStateOf(initialBanners)
    }

    var showNewBannerDialog by remember { mutableStateOf(false) }
    var bannerTitleInput by remember { mutableStateOf("") }
    var bannerContentInput by remember { mutableStateOf("") }
    var bannerTypeSelected by remember { mutableStateOf("PROMO") }

    // Low-Code Automation Rules State
    var autoRefundOnDispute by remember { mutableStateOf(true) }
    var autoApproveVerifiedPlayers by remember { mutableStateOf(false) }
    var allowDirectPlayerRegistration by remember { mutableStateOf(true) }
    var maintenanceMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header
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
                    Text("No-Code App Control Center", color = VelorixTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Live banners, workflows & Gemini AI engine", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Section 1: Gemini AI Assistant Shortcut Banner
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1F1B2E), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFBB86FC).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFBB86FC), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gemini 3.1 Pro AI Assistant", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Auto-generate tournament rules, solve player match disputes, or draft announcement banners instantly with Gemini AI.", color = Color.LightGray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onOpenGeminiChatbot,
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Gemini AI Support Chatbot", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Section 2: Live App Banners & Broadcasts
            item {
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
                        Text("Live App Banners & Alerts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Button(
                            onClick = { showNewBannerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("+ Publish Banner", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    banners.forEach { banner ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(Color(0xFF181818), RoundedCornerShape(12.dp))
                                .border(1.dp, if (banner.isActive) VelorixAccent.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                when (banner.bannerType.uppercase()) {
                                                    "WARNING" -> Color(0xFFFF5252).copy(alpha = 0.2f)
                                                    "PROMO" -> VelorixAccent.copy(alpha = 0.2f)
                                                    else -> Color(0xFF64B5F6).copy(alpha = 0.2f)
                                                },
                                                RoundedCornerShape(4.dp)
                                            )
                                            .border(
                                                0.5.dp,
                                                when (banner.bannerType.uppercase()) {
                                                    "WARNING" -> Color(0xFFFF5252).copy(alpha = 0.5f)
                                                    "PROMO" -> VelorixAccent.copy(alpha = 0.5f)
                                                    else -> Color(0xFF64B5F6).copy(alpha = 0.5f)
                                                },
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = banner.bannerType.uppercase(),
                                            fontSize = 9.sp,
                                            color = when (banner.bannerType.uppercase()) {
                                                "WARNING" -> Color(0xFFFF8A80)
                                                "PROMO" -> VelorixAccent
                                                else -> Color(0xFF90CAF9)
                                            },
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = banner.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = banner.content,
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            Switch(
                                checked = banner.isActive,
                                onCheckedChange = { active ->
                                    val updated = banner.copy(isActive = active)
                                    banners = banners.map { if (it.id == banner.id) updated else it }
                                    onSaveBanner?.invoke(updated)
                                    Toast.makeText(context, "Banner status updated!", Toast.LENGTH_SHORT).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = VelorixAccent,
                                    uncheckedTrackColor = Color.DarkGray
                                )
                            )
                        }
                    }
                }
            }

            // Section 3: Low-Code Rule Engine Configuration
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardLiveBg, RoundedCornerShape(16.dp))
                        .border(1.dp, CardVerifyBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text("Low-Code Automated Workflows", color = VelorixAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    LowCodeToggleItem(
                        title = "Allow Direct Player Registration",
                        subtitle = "Players can self-register without manual admin approval",
                        checked = allowDirectPlayerRegistration,
                        onCheckedChange = { allowDirectPlayerRegistration = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    LowCodeToggleItem(
                        title = "Auto-Refund on Valid Complaint",
                        subtitle = "Automatically credit ₹ funds when dispute is approved by admin",
                        checked = autoRefundOnDispute,
                        onCheckedChange = { autoRefundOnDispute = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    LowCodeToggleItem(
                        title = "Auto-Approve Verified Game IDs",
                        subtitle = "Bypass verification queue for players with verified badges",
                        checked = autoApproveVerifiedPlayers,
                        onCheckedChange = { autoApproveVerifiedPlayers = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    LowCodeToggleItem(
                        title = "App Emergency Maintenance Lock",
                        subtitle = "Temporarily lock user registrations for system maintenance",
                        checked = maintenanceMode,
                        onCheckedChange = { maintenanceMode = it }
                    )
                }
            }
        }
    }

    // New Banner Dialog
    if (showNewBannerDialog) {
        AlertDialog(
            onDismissRequest = { showNewBannerDialog = false },
            title = { Text("Publish New App Banner", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = bannerTitleInput,
                        onValueChange = { bannerTitleInput = it },
                        label = { Text("Banner Title", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = VelorixAccent),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bannerContentInput,
                        onValueChange = { bannerContentInput = it },
                        label = { Text("Content / Announcement Text", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = VelorixAccent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bannerTitleInput.isNotBlank()) {
                            val newBanner = AppAnnouncementBanner(
                                id = "B${banners.size + 1}",
                                title = bannerTitleInput,
                                content = bannerContentInput,
                                bannerType = "PROMO",
                                isActive = true
                            )
                            banners = banners + newBanner
                            onSaveBanner?.invoke(newBanner)
                            Toast.makeText(context, "New Banner Published to Player App!", Toast.LENGTH_LONG).show()
                        }
                        showNewBannerDialog = false
                        bannerTitleInput = ""
                        bannerContentInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent)
                ) {
                    Text("Publish Banner", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewBannerDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = CardLiveBg
        )
    }
}

@Composable
fun LowCodeToggleItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.Gray, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = VelorixAccent)
        )
    }
}
