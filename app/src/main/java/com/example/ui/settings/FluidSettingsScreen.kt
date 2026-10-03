package com.example.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.common.HyperOSTheme
import com.example.ui.common.HyperPhysics
import com.example.ui.common.bounceClick
import com.example.ui.theme.ThemeManager

data class FluidSettingsState(
    val isDark: Boolean = true,
    val themeColorName: String = "Violet",
    val themeColorHex: Long = 0xFF8B5CF6,
    val textSize: String = "Default", // Small, Default, Large, Extra large
    val uiStyle: String = "Rounded",  // Rounded, Soft, Crisp
    val language: String = "English",
    val pushNotifications: Boolean = true,
    val notifyMessages: Boolean = true,
    val notifyPromotions: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val profileVisibility: String = "Everyone", // Everyone, Contacts, Only me
    val onlineStatus: Boolean = true,
    val activityStatus: Boolean = true,
    val locationPermission: String = "While using the app" // Always, While using the app, Never
)

data class ThemeColorOption(
    val name: String,
    val color: Color,
    val hyperTheme: HyperOSTheme
)

val THEME_COLOR_OPTIONS = listOf(
    ThemeColorOption("Violet", Color(0xFF8B5CF6), HyperOSTheme.NEBULA_PURPLE),
    ThemeColorOption("Rose", Color(0xFFEC4899), HyperOSTheme.ELECTRIC_ROSE),
    ThemeColorOption("Emerald", Color(0xFF10B981), HyperOSTheme.AURORA_EMERALD),
    ThemeColorOption("Ocean", Color(0xFF06B6D4), HyperOSTheme.GLACIER_CYAN),
    ThemeColorOption("Amber", Color(0xFFF59E0B), HyperOSTheme.TURBO_AMBER),
    ThemeColorOption("Blue", Color(0xFF3B82F6), HyperOSTheme.GLACIER_CYAN)
)

val LANGUAGE_OPTIONS = listOf(
    "English" to "English",
    "简体中文" to "Chinese, Simplified",
    "Bahasa Melayu" to "Malay",
    "日本語" to "Japanese",
    "한국어" to "Korean",
    "Hindi" to "हिन्दी",
    "Español" to "Spanish",
    "Français" to "French",
    "Deutsch" to "German",
    "Português" to "Portuguese"
)

@Composable
fun FluidSettingsScreen(
    currentTheme: HyperOSTheme,
    onThemeSelect: (HyperOSTheme) -> Unit,
    currentUserEmail: String? = null,
    onLogout: () -> Unit = {},
    onPurgeDemoData: (() -> Unit)? = null,
    onBackToDashboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Committed settings (baseline)
    var committedState by remember {
        mutableStateOf(
            FluidSettingsState(
                isDark = true,
                themeColorName = when (currentTheme) {
                    HyperOSTheme.ELECTRIC_ROSE -> "Rose"
                    HyperOSTheme.AURORA_EMERALD -> "Emerald"
                    HyperOSTheme.GLACIER_CYAN -> "Ocean"
                    HyperOSTheme.TURBO_AMBER -> "Amber"
                    else -> "Violet"
                },
                themeColorHex = currentTheme.primaryColor.value.toLong()
            )
        )
    }

    // Live staged state (current working copy)
    var stagedState by remember { mutableStateOf(committedState) }

    // Active sub-screen: null = Main Settings Hub, "appearance", "language", "notifications", "privacy"
    var currentSubScreen by remember { mutableStateOf<String?>(null) }

    // Calculate number of unsaved changes
    val changesCount = remember(committedState, stagedState) {
        var count = 0
        if (committedState.isDark != stagedState.isDark) count++
        if (committedState.themeColorName != stagedState.themeColorName) count++
        if (committedState.textSize != stagedState.textSize) count++
        if (committedState.uiStyle != stagedState.uiStyle) count++
        if (committedState.language != stagedState.language) count++
        if (committedState.pushNotifications != stagedState.pushNotifications) count++
        if (committedState.notifyMessages != stagedState.notifyMessages) count++
        if (committedState.notifyPromotions != stagedState.notifyPromotions) count++
        if (committedState.soundEnabled != stagedState.soundEnabled) count++
        if (committedState.vibrationEnabled != stagedState.vibrationEnabled) count++
        if (committedState.profileVisibility != stagedState.profileVisibility) count++
        if (committedState.onlineStatus != stagedState.onlineStatus) count++
        if (committedState.activityStatus != stagedState.activityStatus) count++
        if (committedState.locationPermission != stagedState.locationPermission) count++
        count
    }

    // Dynamic accent color driven by staged selection
    val activeColorOption = THEME_COLOR_OPTIONS.firstOrNull { it.name == stagedState.themeColorName } ?: THEME_COLOR_OPTIONS[0]
    val dynamicAccentColor by animateColorAsState(
        targetValue = activeColorOption.color,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "dynamicAccent"
    )

    var showReviewSummaryDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF09090D))
    ) {
        // Sub-screen navigation transition
        AnimatedContent(
            targetState = currentSubScreen,
            transitionSpec = {
                if (targetState != null) {
                    // Forward slide in
                    (slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f)
                    ) + fadeIn(tween(220)))
                        .togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { -it / 3 },
                                animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f)
                            ) + fadeOut(tween(180))
                        )
                } else {
                    // Back slide out
                    (slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f)
                    ) + fadeIn(tween(220)))
                        .togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f)
                            ) + fadeOut(tween(180))
                        )
                }
            },
            label = "settingsSubScreenTransition"
        ) { subScreen ->
            when (subScreen) {
                null -> {
                    // Main Settings Hub
                    SettingsHubContent(
                        stagedState = stagedState,
                        committedState = committedState,
                        accentColor = dynamicAccentColor,
                        currentUserEmail = currentUserEmail,
                        onNavigateSubScreen = { currentSubScreen = it },
                        onToggleDark = {
                            stagedState = stagedState.copy(isDark = it)
                            ThemeManager.setDarkTheme(it, context)
                        },
                        onBack = onBackToDashboard,
                        onLogout = onLogout,
                        onPurgeDemoData = onPurgeDemoData
                    )
                }
                "appearance" -> {
                    AppearanceSubScreen(
                        stagedState = stagedState,
                        accentColor = dynamicAccentColor,
                        onBack = { currentSubScreen = null },
                        onUpdateState = { stagedState = it },
                        onApplyTheme = { opt ->
                            stagedState = stagedState.copy(themeColorName = opt.name, themeColorHex = opt.color.value.toLong())
                            onThemeSelect(opt.hyperTheme)
                        }
                    )
                }
                "language" -> {
                    LanguageSubScreen(
                        stagedState = stagedState,
                        accentColor = dynamicAccentColor,
                        onBack = { currentSubScreen = null },
                        onSelectLanguage = { stagedState = stagedState.copy(language = it) }
                    )
                }
                "notifications" -> {
                    NotificationsSubScreen(
                        stagedState = stagedState,
                        accentColor = dynamicAccentColor,
                        onBack = { currentSubScreen = null },
                        onUpdateState = { stagedState = it }
                    )
                }
                "privacy" -> {
                    PrivacySubScreen(
                        stagedState = stagedState,
                        accentColor = dynamicAccentColor,
                        onBack = { currentSubScreen = null },
                        onUpdateState = { stagedState = it }
                    )
                }
            }
        }

        // Floating Review/Discard dock (matching the video's bottom floating pill)
        FluidFloatingReviewBar(
            changesCount = changesCount,
            accentColor = dynamicAccentColor,
            onDiscard = {
                stagedState = committedState
                ThemeManager.setDarkTheme(committedState.isDark, context)
                val originalTheme = THEME_COLOR_OPTIONS.firstOrNull { it.name == committedState.themeColorName }?.hyperTheme ?: HyperOSTheme.NEBULA_PURPLE
                onThemeSelect(originalTheme)
                Toast.makeText(context, "Changes discarded", Toast.LENGTH_SHORT).show()
            },
            onReview = {
                showReviewSummaryDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 85.dp) // Sits comfortably above navigation bar
        )

        // Review & Apply Dialog
        if (showReviewSummaryDialog) {
            AlertDialog(
                onDismissRequest = { showReviewSummaryDialog = false },
                containerColor = Color(0xFF15151F),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = dynamicAccentColor.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, dynamicAccentColor)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = dynamicAccentColor, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Apply $changesCount Customization${if (changesCount > 1) "s" else ""}?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("The following fluid visual changes will take effect immediately:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0C0C12),
                            border = BorderStroke(1.dp, Color(0xFF222230)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("• Theme Accent: ${stagedState.themeColorName}", color = dynamicAccentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("• Typography Scale: ${stagedState.textSize}", color = Color.White, fontSize = 12.sp)
                                Text("• UI Shape Style: ${stagedState.uiStyle}", color = Color.White, fontSize = 12.sp)
                                Text("• System Language: ${stagedState.language}", color = Color.White, fontSize = 12.sp)
                                Text("• Dark Mode: ${if (stagedState.isDark) "Enabled" else "Disabled"}", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            committedState = stagedState
                            showReviewSummaryDialog = false
                            Toast.makeText(context, "Preferences applied with 120Hz fluidity!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = dynamicAccentColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply Changes", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReviewSummaryDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }
    }
}

/**
 * Main Settings Hub Screen Content
 */
@Composable
private fun SettingsHubContent(
    stagedState: FluidSettingsState,
    committedState: FluidSettingsState,
    accentColor: Color,
    currentUserEmail: String?,
    onNavigateSubScreen: (String) -> Unit,
    onToggleDark: (Boolean) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onPurgeDemoData: (() -> Unit)?
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(14.dp))
            // Top Bar with back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    modifier = Modifier
                        .size(38.dp)
                        .bounceClick(scaleDown = 0.90f) { onBack() },
                    shape = CircleShape,
                    color = Color(0xFF161622),
                    border = BorderStroke(1.dp, Color(0xFF262638))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "120Hz ULTRA FLUID",
                        color = accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Heading
        item {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "Settings",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Make the app feel like yours.",
                    color = Color(0xFF8E8EA0),
                    fontSize = 13.sp
                )
            }
        }

        // Profile Overview Card (matching video 0:01)
        item {
            FluidSettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar circle with glow
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(accentColor, Color(0xFFEC4899))
                                ),
                                CircleShape
                            )
                            .border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (currentUserEmail?.firstOrNull() ?: 'V').uppercase(),
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentUserEmail?.substringBefore("@")?.capitalize() ?: "Super Admin",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF102A1E),
                                border = BorderStroke(0.5.dp, Color(0xFF166534))
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    color = Color(0xFF86EFAC),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentUserEmail ?: "anantisback47@gmail.com",
                            color = Color(0xFF8E8EA0),
                            fontSize = 11.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats Pill Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileMetricPill(modifier = Modifier.weight(1f), count = "3", label = "Modes", accentColor = accentColor)
                    ProfileMetricPill(modifier = Modifier.weight(1f), count = "120Hz", label = "Fluidity", accentColor = accentColor)
                    ProfileMetricPill(modifier = Modifier.weight(1f), count = "RTDB", label = "Live Sync", accentColor = accentColor)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tag Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val tags = listOf("Free Fire", "Tournament", "Dual Database", "Spring Physics", "Glass UI")
                    items(tags) { tag ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1A1A24),
                            border = BorderStroke(0.8.dp, Color(0xFF2C2C3D))
                        ) {
                            Text(
                                text = tag,
                                color = Color(0xFFB0B0C0),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Dark Mode Quick Toggle Card (matching video 0:03)
        item {
            FluidSettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Dark mode",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (stagedState.isDark) "Easy on the eyes at night" else "Top for daylight",
                            color = Color(0xFF8E8EA0),
                            fontSize = 11.5.sp
                        )
                    }

                    FluidSwitch(
                        checked = stagedState.isDark,
                        onCheckedChange = onToggleDark,
                        accentColor = accentColor,
                        checkedThumbIcon = Icons.Default.NightlightRound,
                        uncheckedThumbIcon = Icons.Default.WbSunny
                    )
                }
            }
        }

        // Section: PERSONALIZATION (Appearance, Language)
        item {
            Text(
                text = "PERSONALIZATION",
                color = Color(0xFF6E6E82),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            FluidSettingsCard {
                FluidSettingsRow(
                    title = "Appearance",
                    subtitle = "${stagedState.themeColorName} • ${stagedState.textSize} text • ${stagedState.uiStyle}",
                    icon = Icons.Default.Palette,
                    iconTint = accentColor,
                    onClick = { onNavigateSubScreen("appearance") }
                )
                HorizontalDivider(color = Color(0xFF1F1F2C), thickness = 0.8.dp, modifier = Modifier.padding(vertical = 4.dp))
                FluidSettingsRow(
                    title = "Language",
                    subtitle = stagedState.language,
                    icon = Icons.Default.Translate,
                    iconTint = Color(0xFF38BDF8),
                    onClick = { onNavigateSubScreen("language") }
                )
            }
        }

        // Section: ALERTS (Notifications)
        item {
            Text(
                text = "ALERTS",
                color = Color(0xFF6E6E82),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            FluidSettingsCard {
                FluidSettingsRow(
                    title = "Notifications",
                    subtitle = if (stagedState.pushNotifications) "Messages & sound active" else "Paused",
                    icon = Icons.Default.Notifications,
                    iconTint = Color(0xFFFFB74D),
                    onClick = { onNavigateSubScreen("notifications") }
                )
            }
        }

        // Section: PRIVACY & SECURITY
        item {
            Text(
                text = "PRIVACY & SECURITY",
                color = Color(0xFF6E6E82),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            FluidSettingsCard {
                FluidSettingsRow(
                    title = "Privacy",
                    subtitle = "${stagedState.profileVisibility} • ${if (stagedState.onlineStatus) "Visible" else "Hidden"}",
                    icon = Icons.Default.Security,
                    iconTint = Color(0xFF4ADE80),
                    onClick = { onNavigateSubScreen("privacy") }
                )
            }
        }

        // Section: SYSTEM ACTIONS
        item {
            Text(
                text = "ADMINISTRATIVE ENGINE",
                color = Color(0xFF6E6E82),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            FluidSettingsCard {
                if (onPurgeDemoData != null) {
                    FluidSettingsRow(
                        title = "Clean Mock Database Data",
                        subtitle = "Purge temporary test records while keeping production safe",
                        icon = Icons.Default.DeleteSweep,
                        iconTint = Color(0xFFEF4444),
                        onClick = onPurgeDemoData
                    )
                    HorizontalDivider(color = Color(0xFF1F1F2C), thickness = 0.8.dp, modifier = Modifier.padding(vertical = 4.dp))
                }
                FluidSettingsRow(
                    title = "Sign Out Administrator",
                    subtitle = "End your administrative session and return to login",
                    icon = Icons.Default.ExitToApp,
                    iconTint = Color(0xFFF87171),
                    onClick = onLogout
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

/**
 * Profile Metric Pill (e.g. "3 Modes", "120Hz Fluidity")
 */
@Composable
private fun ProfileMetricPill(
    count: String,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF171722),
        border = BorderStroke(0.8.dp, Color(0xFF28283A))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = label, color = Color(0xFF8E8EA0), fontSize = 10.sp)
        }
    }
}

/**
 * Appearance Sub-Screen (Matching video 0:04 - 0:10)
 */
@Composable
private fun AppearanceSubScreen(
    stagedState: FluidSettingsState,
    accentColor: Color,
    onBack: () -> Unit,
    onUpdateState: (FluidSettingsState) -> Unit,
    onApplyTheme: (ThemeColorOption) -> Unit
) {
    var searchPrompt by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(14.dp))
            SubScreenHeader(title = "Appearance", onBack = onBack, accentColor = accentColor)
        }

        // Search Prompt Box (matching video 0:04 "Ask anything...")
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF151520),
                border = BorderStroke(1.dp, Color(0xFF282838))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (searchPrompt.isBlank()) "Ask anything..." else searchPrompt,
                        color = if (searchPrompt.isBlank()) Color(0xFF71718A) else Color.White,
                        fontSize = 13.sp
                    )
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.2f),
                        border = BorderStroke(0.8.dp, accentColor)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Dark Mode Card
        item {
            FluidSettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dark mode", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Easy on the eyes at night", color = Color(0xFF8E8EA0), fontSize = 11.5.sp)
                    }

                    FluidSwitch(
                        checked = stagedState.isDark,
                        onCheckedChange = { onUpdateState(stagedState.copy(isDark = it)) },
                        accentColor = accentColor,
                        checkedThumbIcon = Icons.Default.NightlightRound,
                        uncheckedThumbIcon = Icons.Default.WbSunny
                    )
                }
            }
        }

        // THEME COLOR Swatches Row
        item {
            FluidSettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("THEME COLOR", color = Color(0xFF8E8EA0), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    Text(stagedState.themeColorName, color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Swatches row with spring check rings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    THEME_COLOR_OPTIONS.forEach { opt ->
                        val isSelected = opt.name == stagedState.themeColorName
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.15f else 1.0f,
                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
                            label = "swatchScale"
                        )

                        Surface(
                            modifier = Modifier
                                .size(42.dp)
                                .scale(scale)
                                .bounceClick(scaleDown = 0.90f) {
                                    onApplyTheme(opt)
                                },
                            shape = CircleShape,
                            color = opt.color,
                            border = if (isSelected) BorderStroke(2.5.dp, Color.White) else BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            shadowElevation = if (isSelected) 8.dp else 2.dp
                        ) {
                            if (isSelected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // TEXT SIZE Segmented Control (Small, Default, Large, Extra large)
        item {
            FluidSettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TEXT SIZE", color = Color(0xFF8E8EA0), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    Text(stagedState.textSize, color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                val sizes = listOf("Small", "Default", "Large", "Extra large")
                val selectedIdx = sizes.indexOf(stagedState.textSize).coerceAtLeast(0)

                FluidSegmentedControl(
                    items = sizes,
                    selectedIndex = selectedIdx,
                    onItemSelected = { idx -> onUpdateState(stagedState.copy(textSize = sizes[idx])) },
                    accentColor = accentColor
                ) { sizeLabel, isSelected ->
                    val fontSp = when (sizeLabel) {
                        "Small" -> 11.sp
                        "Default" -> 13.sp
                        "Large" -> 15.sp
                        else -> 17.sp
                    }
                    Text(
                        text = "Aa",
                        color = if (isSelected) Color.White else Color(0xFF8E8EA0),
                        fontSize = fontSp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                    )
                }
            }
        }

        // UI STYLE Segmented Control (Rounded, Soft, Crisp)
        item {
            FluidSettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("UI STYLE", color = Color(0xFF8E8EA0), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    Text(stagedState.uiStyle, color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                val styles = listOf("Rounded", "Soft", "Crisp")
                val selectedIdx = styles.indexOf(stagedState.uiStyle).coerceAtLeast(0)

                FluidSegmentedControl(
                    items = styles,
                    selectedIndex = selectedIdx,
                    onItemSelected = { idx -> onUpdateState(stagedState.copy(uiStyle = styles[idx])) },
                    accentColor = accentColor
                ) { styleLabel, isSelected ->
                    Text(
                        text = styleLabel,
                        color = if (isSelected) Color.White else Color(0xFF8E8EA0),
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

/**
 * Language Sub-Screen (Matching video 0:10 - 0:12)
 */
@Composable
private fun LanguageSubScreen(
    stagedState: FluidSettingsState,
    accentColor: Color,
    onBack: () -> Unit,
    onSelectLanguage: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(14.dp))
            SubScreenHeader(title = "Language", onBack = onBack, accentColor = accentColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Pick the language the app speaks.", color = Color(0xFF8E8EA0), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Active Language Banner
        item {
            FluidSettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = stagedState.language, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Menus and replies in ${stagedState.language}", color = accentColor, fontSize = 11.5.sp)
                    }
                    Icon(Icons.Default.Language, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                }
            }
        }

        item {
            Text("ALL LANGUAGES", color = Color(0xFF6E6E82), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        item {
            FluidSettingsCard {
                LANGUAGE_OPTIONS.forEachIndexed { index, (langName, nativeLabel) ->
                    val isSelected = stagedState.language == langName
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .bounceClick(scaleDown = 0.98f) { onSelectLanguage(langName) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = langName, color = if (isSelected) Color.White else Color(0xFFD4D4E0), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = nativeLabel, color = Color(0xFF71718A), fontSize = 11.sp)
                        }

                        // Circular radio indicator
                        Surface(
                            modifier = Modifier.size(22.dp),
                            shape = CircleShape,
                            color = if (isSelected) accentColor else Color.Transparent,
                            border = BorderStroke(1.5.dp, if (isSelected) accentColor else Color(0xFF444458))
                        ) {
                            if (isSelected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    if (index < LANGUAGE_OPTIONS.size - 1) {
                        HorizontalDivider(color = Color(0xFF1E1E2C), thickness = 0.8.dp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

/**
 * Notifications Sub-Screen (Matching video 0:13 - 0:16)
 */
@Composable
private fun NotificationsSubScreen(
    stagedState: FluidSettingsState,
    accentColor: Color,
    onBack: () -> Unit,
    onUpdateState: (FluidSettingsState) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(14.dp))
            SubScreenHeader(title = "Notifications", onBack = onBack, accentColor = accentColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Choose what's worth a buzz.", color = Color(0xFF8E8EA0), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Live Mock Notification Card (matching video 0:14)
        item {
            FluidSettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, accentColor)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Velorix Esports", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("now", color = Color(0xFF71718A), fontSize = 10.sp)
                        }
                        Text("Custom Room ID #88472 is published for CS 4v4!", color = Color(0xFFB0B0C4), fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        // Master Switch: Push notifications
        item {
            FluidSettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Push notifications", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = if (stagedState.pushNotifications) "Active on this device" else "Paused. You won't get alerts",
                            color = Color(0xFF8E8EA0),
                            fontSize = 11.5.sp
                        )
                    }

                    FluidSwitch(
                        checked = stagedState.pushNotifications,
                        onCheckedChange = { onUpdateState(stagedState.copy(pushNotifications = it)) },
                        accentColor = accentColor
                    )
                }
            }
        }

        // NOTIFY ME ABOUT
        item {
            Text("NOTIFY ME ABOUT", color = Color(0xFF6E6E82), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            FluidSettingsCard {
                NotificationToggleRow(
                    title = "Messages",
                    subtitle = "Replies and new player ticket messages",
                    checked = stagedState.notifyMessages,
                    accentColor = accentColor,
                    onCheckedChange = { onUpdateState(stagedState.copy(notifyMessages = it)) }
                )
                HorizontalDivider(color = Color(0xFF1E1E2C), thickness = 0.8.dp, modifier = Modifier.padding(vertical = 4.dp))
                NotificationToggleRow(
                    title = "Promotions",
                    subtitle = "Tournament announcements & sponsor bonuses",
                    checked = stagedState.notifyPromotions,
                    accentColor = accentColor,
                    onCheckedChange = { onUpdateState(stagedState.copy(notifyPromotions = it)) }
                )
            }
        }

        // SOUND & VIBRATION
        item {
            Text("SOUND & VIBRATION", color = Color(0xFF6E6E82), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            FluidSettingsCard {
                NotificationToggleRow(
                    title = "Sound",
                    subtitle = "A soft chime for new room alerts",
                    checked = stagedState.soundEnabled,
                    accentColor = accentColor,
                    onCheckedChange = { onUpdateState(stagedState.copy(soundEnabled = it)) }
                )
                HorizontalDivider(color = Color(0xFF1E1E2C), thickness = 0.8.dp, modifier = Modifier.padding(vertical = 4.dp))
                NotificationToggleRow(
                    title = "Vibration",
                    subtitle = "A gentle haptic buzz for match start",
                    checked = stagedState.vibrationEnabled,
                    accentColor = accentColor,
                    onCheckedChange = { onUpdateState(stagedState.copy(vibrationEnabled = it)) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

/**
 * Privacy Sub-Screen (Matching video 0:17 - 0:19)
 */
@Composable
private fun PrivacySubScreen(
    stagedState: FluidSettingsState,
    accentColor: Color,
    onBack: () -> Unit,
    onUpdateState: (FluidSettingsState) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(14.dp))
            SubScreenHeader(title = "Privacy", onBack = onBack, accentColor = accentColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Control who can see your admin activity.", color = Color(0xFF8E8EA0), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // PROFILE VISIBILITY
        item {
            FluidSettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("PROFILE VISIBILITY", color = Color(0xFF8E8EA0), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    Text(stagedState.profileVisibility, color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                val visibilities = listOf("Everyone", "Contacts", "Only me")
                val selectedIdx = visibilities.indexOf(stagedState.profileVisibility).coerceAtLeast(0)

                FluidSegmentedControl(
                    items = visibilities,
                    selectedIndex = selectedIdx,
                    onItemSelected = { idx -> onUpdateState(stagedState.copy(profileVisibility = visibilities[idx])) },
                    accentColor = accentColor
                ) { label, isSelected ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val icon = when (label) {
                            "Everyone" -> Icons.Default.Public
                            "Contacts" -> Icons.Default.Group
                            else -> Icons.Default.Lock
                        }
                        Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else Color(0xFF71718A), modifier = Modifier.size(13.dp))
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color(0xFF8E8EA0),
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // STATUS TOGGLES
        item {
            Text("STATUS", color = Color(0xFF6E6E82), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            FluidSettingsCard {
                NotificationToggleRow(
                    title = "Online status",
                    subtitle = "Others see when you're live in tournament rooms",
                    checked = stagedState.onlineStatus,
                    accentColor = accentColor,
                    onCheckedChange = { onUpdateState(stagedState.copy(onlineStatus = it)) }
                )
                HorizontalDivider(color = Color(0xFF1E1E2C), thickness = 0.8.dp, modifier = Modifier.padding(vertical = 4.dp))
                NotificationToggleRow(
                    title = "Activity status",
                    subtitle = "Shows when you verified a victory proof",
                    checked = stagedState.activityStatus,
                    accentColor = accentColor,
                    onCheckedChange = { onUpdateState(stagedState.copy(activityStatus = it)) }
                )
            }
        }

        // LOCATION PERMISSION (Always, While using the app, Never)
        item {
            Text("LOCATION PERMISSION", color = Color(0xFF6E6E82), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            FluidSettingsCard {
                val locOptions = listOf(
                    "Always" to "Local server routing, even in the background",
                    "While using the app" to "Only while tournament console is open",
                    "Never" to "Answers won't use your precise location"
                )

                locOptions.forEachIndexed { index, (optTitle, optSub) ->
                    val isSelected = stagedState.locationPermission == optTitle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .bounceClick(scaleDown = 0.98f) { onUpdateState(stagedState.copy(locationPermission = optTitle)) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = optTitle, color = if (isSelected) Color.White else Color(0xFFD4D4E0), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = optSub, color = Color(0xFF71718A), fontSize = 11.sp)
                        }

                        Surface(
                            modifier = Modifier.size(20.dp),
                            shape = CircleShape,
                            color = if (isSelected) accentColor else Color.Transparent,
                            border = BorderStroke(1.5.dp, if (isSelected) accentColor else Color(0xFF444458))
                        ) {
                            if (isSelected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }

                    if (index < locOptions.size - 1) {
                        HorizontalDivider(color = Color(0xFF1E1E2C), thickness = 0.8.dp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(1.dp))
            Text(subtitle, color = Color(0xFF8E8EA0), fontSize = 11.5.sp)
        }

        FluidSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            accentColor = accentColor
        )
    }
}

@Composable
private fun SubScreenHeader(
    title: String,
    onBack: () -> Unit,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(38.dp)
                .bounceClick(scaleDown = 0.90f) { onBack() },
            shape = CircleShape,
            color = Color(0xFF161622),
            border = BorderStroke(1.dp, Color(0xFF262638))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
