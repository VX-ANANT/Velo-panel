package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextOverflow
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

    // Active tab mode for flat navigation instead of stacked boxes
    var selectedSectionMode by remember { mutableIntStateOf(0) }
    val sectionModes = listOf("LIVE BANNERS", "AUTOMATION RULES", "AI ASSISTANT")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
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
                    Text(
                        "NO-CODE CONTROL CENTER",
                        color = VelorixTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        "Live banners, automated rules & AI engine",
                        color = Color.Gray,
                        fontSize = 11.5.sp
                    )
                }
            }
            if (selectedSectionMode == 0) {
                TextButton(onClick = { showNewBannerDialog = true }) {
                    Text("+ PUBLISH", color = VelorixAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

        // Mode Selector Row (Underline style, zero box-in-box)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            sectionModes.forEachIndexed { index, title ->
                val selected = selectedSectionMode == index
                Column(
                    modifier = Modifier
                        .clickable { selectedSectionMode = index }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        color = if (selected) VelorixAccent else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(2.dp)
                            .background(if (selected) VelorixAccent else Color.Transparent)
                    )
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
        ) {
            when (selectedSectionMode) {
                0 -> {
                    if (banners.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No Active Banners", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Tap + PUBLISH to broadcast a banner to the player app.", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    } else {
                        items(banners.size) { idx ->
                            val banner = banners[idx]
                            val typeColor = when (banner.bannerType.uppercase()) {
                                "WARNING" -> Color(0xFFFF8A80)
                                "PROMO" -> VelorixAccent
                                else -> Color(0xFF90CAF9)
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = banner.bannerType.uppercase(),
                                                fontSize = 10.sp,
                                                color = typeColor,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 0.6.sp
                                            )
                                            Text(
                                                text = "  •  ",
                                                fontSize = 10.sp,
                                                color = Color.DarkGray
                                            )
                                            Text(
                                                text = banner.title,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = banner.content,
                                            color = Color.Gray,
                                            fontSize = 12.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Switch(
                                        checked = banner.isActive,
                                        onCheckedChange = { active ->
                                            val updated = banner.copy(isActive = active)
                                            banners = banners.map { if (it.id == banner.id) updated else it }
                                            onSaveBanner?.invoke(updated)
                                            Toast.makeText(context, "Banner status updated", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = VelorixAccent,
                                            uncheckedTrackColor = Color(0xFF1F1F24)
                                        )
                                    )
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                            }
                        }
                    }
                }
                1 -> {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LowCodeToggleItem(
                                title = "Allow Direct Player Registration",
                                subtitle = "Players can self-register without manual admin approval",
                                checked = allowDirectPlayerRegistration,
                                onCheckedChange = { allowDirectPlayerRegistration = it }
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                            LowCodeToggleItem(
                                title = "Auto-Refund on Valid Complaint",
                                subtitle = "Automatically credit ₹ funds when dispute is approved by admin",
                                checked = autoRefundOnDispute,
                                onCheckedChange = { autoRefundOnDispute = it }
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                            LowCodeToggleItem(
                                title = "Auto-Approve Verified Game IDs",
                                subtitle = "Bypass verification queue for players with verified badges",
                                checked = autoApproveVerifiedPlayers,
                                onCheckedChange = { autoApproveVerifiedPlayers = it }
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                            LowCodeToggleItem(
                                title = "App Emergency Maintenance Lock",
                                subtitle = "Temporarily lock user registrations for system maintenance",
                                checked = maintenanceMode,
                                onCheckedChange = { maintenanceMode = it }
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                        }
                    }
                }
                2 -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFBB86FC), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("GEMINI 3.1 PRO AI ASSISTANT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.6.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Auto-generate tournament rules, resolve player match disputes, or draft announcement banners with Gemini AI.",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onOpenGeminiChatbot,
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("OPEN AI SUPPORT CONSOLE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                        }
                    }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("PROMO", "INFO", "WARNING").forEach { type ->
                            val selected = bannerTypeSelected == type
                            Text(
                                text = type,
                                color = if (selected) VelorixAccent else Color.Gray,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clickable { bannerTypeSelected = type }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 6.dp))
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
                                bannerType = bannerTypeSelected,
                                isActive = true
                            )
                            banners = banners + newBanner
                            onSaveBanner?.invoke(newBanner)
                            Toast.makeText(context, "Banner Published to Player App", Toast.LENGTH_LONG).show()
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
            containerColor = Color.Black
        )
    }
}

@Composable
fun LowCodeToggleItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = Color.Gray, fontSize = 11.5.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = VelorixAccent,
                uncheckedTrackColor = Color(0xFF1F1F24)
            )
        )
    }
}
