package com.example.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Xiaomi HyperOS / MIUI Fluid Animation Physics Specs
 */
object HyperPhysics {
    // HyperOS Bouncy Spring for touch micro-interactions & button presses
    val BouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    // Fluid Smooth Spring for tab switching, screen sliding & container expansions
    val FluidPageSpring = spring<Float>(
        dampingRatio = 0.78f,
        stiffness = 320f
    )

    // Snappy Spring for toggles, pills, and bottom sheets
    val SnappySpring = spring<Float>(
        dampingRatio = 0.88f,
        stiffness = 550f
    )
}

/**
 * Custom tactile bounce click modifier modeled after Xiaomi HyperOS system apps.
 * Compresses with spring physics upon touch down and rebounds smoothly upon release.
 */
enum class HyperPressState { Pressed, Idle }

fun Modifier.bounceClick(
    scaleDown: Float = 0.94f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1.0f,
        animationSpec = HyperPhysics.BouncySpring,
        label = "hyperBounceScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else Modifier
        )
}

/**
 * Xiaomi System App Accent Themes that users can dynamically choose from
 */
enum class HyperOSTheme(
    val themeName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val glowColor: Color
) {
    NEBULA_PURPLE(
        themeName = "Hyper Nebula",
        primaryColor = Color(0xFFD0BCFF),
        secondaryColor = Color(0xFF7F56D9),
        glowColor = Color(0xFF9E77ED)
    ),
    GLACIER_CYAN(
        themeName = "Xiaomi Cyan",
        primaryColor = Color(0xFF4DD0E1),
        secondaryColor = Color(0xFF00ACC1),
        glowColor = Color(0xFF26C6DA)
    ),
    TURBO_AMBER(
        themeName = "Game Turbo Amber",
        primaryColor = Color(0xFFFFB300),
        secondaryColor = Color(0xFFFF6D00),
        glowColor = Color(0xFFFF9100)
    ),
    AURORA_EMERALD(
        themeName = "Aurora Emerald",
        primaryColor = Color(0xFF69F0AE),
        secondaryColor = Color(0xFF00BFA5),
        glowColor = Color(0xFF00E676)
    ),
    ELECTRIC_ROSE(
        themeName = "Cyber Neon Pink",
        primaryColor = Color(0xFFFF4081),
        secondaryColor = Color(0xFFC2185B),
        glowColor = Color(0xFFFF80AB)
    )
}

val LocalHyperTheme = compositionLocalOf { mutableStateOf(HyperOSTheme.NEBULA_PURPLE) }

/**
 * Xiaomi HyperOS System Status Bar & Game Turbo Floating Island
 */
@Composable
fun HyperSystemStatusIsland(
    currentTheme: HyperOSTheme,
    onThemeSelect: (HyperOSTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var isTurboBoosted by remember { mutableStateOf(true) }

    // Pulsing radar light
    val infiniteTransition = rememberInfiniteTransition(label = "pulseRadar")
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radarPulse"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = currentTheme.secondaryColor.copy(alpha = 0.25f),
                ambientColor = Color.Black
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.18f),
                        currentTheme.primaryColor.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp)),
        color = Color(0xCC0D0D14)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: System Engine & Turbo Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pulsing Green / Dynamic Indicator
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier
                            .size(10.dp)
                            .graphicsLayer { scaleX = 1f + radarPulse * 0.4f; scaleY = 1f + radarPulse * 0.4f },
                        shape = CircleShape,
                        color = if (isTurboBoosted) currentTheme.primaryColor.copy(alpha = 0.35f * radarPulse) else Color(0xFF66BB6A).copy(alpha = 0.3f)
                    ) {}
                    Surface(
                        modifier = Modifier.size(6.dp),
                        shape = CircleShape,
                        color = if (isTurboBoosted) currentTheme.primaryColor else Color(0xFF4CAF50)
                    ) {}
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "HYPER ENGINE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = currentTheme.primaryColor,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = currentTheme.secondaryColor.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = "120Hz ULTRA",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Low Latency • 18ms Ping • 60+ FPS",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // Right: System Theme Switcher Pill
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .bounceClick { showThemeDialog = true },
                color = currentTheme.secondaryColor.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, currentTheme.primaryColor.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Theme",
                        tint = currentTheme.primaryColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = currentTheme.themeName.split(" ").last(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        HyperThemeSelectionDialog(
            currentTheme = currentTheme,
            onThemeSelect = {
                onThemeSelect(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
}

/**
 * Theme Selection Dialog inspired by Xiaomi HyperOS customization center
 */
@Composable
fun HyperThemeSelectionDialog(
    currentTheme: HyperOSTheme,
    onThemeSelect: (HyperOSTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = currentTheme.primaryColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text("System Accent Themes", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Select your fluid UI accent style (inspired by Xiaomi HyperOS & Game Turbo):",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                HyperOSTheme.values().forEach { theme ->
                    val isSelected = theme == currentTheme
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick { onThemeSelect(theme) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) theme.secondaryColor.copy(alpha = 0.35f) else Color(0xFF1B1826),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) theme.primaryColor else Color.White.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(24.dp),
                                    shape = CircleShape,
                                    color = theme.primaryColor,
                                    shadowElevation = 4.dp
                                ) {}
                                Text(
                                    text = theme.themeName,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = theme.primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = currentTheme.primaryColor, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF14121E)
    )
}

/**
 * Xiaomi HyperOS Style Spring Toggle Switch
 */
@Composable
fun HyperSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color = Color(0xFFD0BCFF),
    modifier: Modifier = Modifier
) {
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 24f else 0f,
        animationSpec = HyperPhysics.SnappySpring,
        label = "hyperSwitchThumb"
    )

    Surface(
        modifier = modifier
            .width(48.dp)
            .height(26.dp)
            .bounceClick { onCheckedChange(!checked) },
        shape = RoundedCornerShape(13.dp),
        color = if (checked) accentColor.copy(alpha = 0.35f) else Color(0xFF2B2838),
        border = BorderStroke(1.dp, if (checked) accentColor else Color.White.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Surface(
                modifier = Modifier
                    .size(20.dp)
                    .offset(x = thumbOffset.dp),
                shape = CircleShape,
                color = if (checked) accentColor else Color.White.copy(alpha = 0.7f),
                shadowElevation = 3.dp
            ) {}
        }
    }
}
