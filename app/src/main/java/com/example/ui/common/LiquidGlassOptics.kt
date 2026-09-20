package com.example.ui.common

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kashif_e.backdrop.*
import com.kashif_e.backdrop.backdrops.*
import com.kashif_e.backdrop.effects.*
import com.kashif_e.backdrop.highlight.*
import com.kashif_e.backdrop.shadow.*
import com.example.ui.theme.VelorixAccent
import com.example.ui.theme.VelorixAccentLight
import com.example.ui.theme.VelorixTextPrimary
import com.example.ui.theme.VelorixTextSecondary

/**
 * Liquid Glass Optical Parameters Model.
 * Corresponds to the real-time optical refraction pipeline.
 */
data class LiquidGlassOpticsState(
    val vibrancy: Float = 0.85f,                // 0f..1f (Boost the saturation of the glass backdrop)
    val blurRadius: Float = 0.90f,              // 0f..1f (Amount of blur on the glass surface: 90% Gaussian blur)
    val lensRefractionHeight: Float = 0.75f,    // 0f..1f (Manage Lens Refraction Height settings)
    val lensRefractionAmount: Float = 0.80f,    // 0f..1f (Manage Lens Refraction Amount settings)
    val chromaticAberration: Boolean = true,    // Manage Chromatic Aberration settings
    val depthEffect: Boolean = true,            // Manage Depth Effect settings
    val surfaceTint: Color = Color(0xFF1E1528), // Tint colour applied to the glass surface
    val surfaceOpacity: Float = 0.20f,          // Opacity of the glass tint overlay for readability
    val glassTextColor: Color = Color.White,    // Text colour used on glass surfaces
    val glassPlayer: Boolean = true,            // Manage Glass Player settings
    val glassMiniPlayer: Boolean = true,        // Manage Glass Mini Player settings
    val glassNavigationBar: Boolean = true      // Manage Glass Navigation Bar settings
)

/**
 * Persistent Manager for Liquid Glass Optics.
 */
class LiquidGlassOpticsManager(context: Context) {
    private val prefs = context.getSharedPreferences("LiquidGlassOpticsPrefs", Context.MODE_PRIVATE)

    var state by mutableStateOf(
        LiquidGlassOpticsState(
            vibrancy = prefs.getFloat("vibrancy", 0.85f),
            blurRadius = prefs.getFloat("blurRadius", 0.90f),
            lensRefractionHeight = prefs.getFloat("lensRefractionHeight", 0.75f),
            lensRefractionAmount = prefs.getFloat("lensRefractionAmount", 0.80f),
            chromaticAberration = prefs.getBoolean("chromaticAberration", true),
            depthEffect = prefs.getBoolean("depthEffect", true),
            surfaceOpacity = prefs.getFloat("surfaceOpacity", 0.20f),
            glassPlayer = prefs.getBoolean("glassPlayer", true),
            glassMiniPlayer = prefs.getBoolean("glassMiniPlayer", true),
            glassNavigationBar = prefs.getBoolean("glassNavigationBar", true)
        )
    )
        private set

    fun updateVibrancy(value: Float) {
        state = state.copy(vibrancy = value.coerceIn(0f, 1f))
        prefs.edit().putFloat("vibrancy", state.vibrancy).apply()
    }

    fun updateBlurRadius(value: Float) {
        state = state.copy(blurRadius = value.coerceIn(0f, 1f))
        prefs.edit().putFloat("blurRadius", state.blurRadius).apply()
    }

    fun updateLensRefractionHeight(value: Float) {
        state = state.copy(lensRefractionHeight = value.coerceIn(0f, 1f))
        prefs.edit().putFloat("lensRefractionHeight", state.lensRefractionHeight).apply()
    }

    fun updateLensRefractionAmount(value: Float) {
        state = state.copy(lensRefractionAmount = value.coerceIn(0f, 1f))
        prefs.edit().putFloat("lensRefractionAmount", state.lensRefractionAmount).apply()
    }

    fun setChromaticAberration(enabled: Boolean) {
        state = state.copy(chromaticAberration = enabled)
        prefs.edit().putBoolean("chromaticAberration", enabled).apply()
    }

    fun setDepthEffect(enabled: Boolean) {
        state = state.copy(depthEffect = enabled)
        prefs.edit().putBoolean("depthEffect", enabled).apply()
    }

    fun updateSurfaceTint(color: Color) {
        state = state.copy(surfaceTint = color)
    }

    fun updateSurfaceOpacity(value: Float) {
        state = state.copy(surfaceOpacity = value.coerceIn(0f, 1f))
        prefs.edit().putFloat("surfaceOpacity", state.surfaceOpacity).apply()
    }

    fun updateGlassTextColor(color: Color) {
        state = state.copy(glassTextColor = color)
    }

    fun setGlassPlayer(enabled: Boolean) {
        state = state.copy(glassPlayer = enabled)
        prefs.edit().putBoolean("glassPlayer", enabled).apply()
    }

    fun setGlassMiniPlayer(enabled: Boolean) {
        state = state.copy(glassMiniPlayer = enabled)
        prefs.edit().putBoolean("glassMiniPlayer", enabled).apply()
    }

    fun setGlassNavigationBar(enabled: Boolean) {
        state = state.copy(glassNavigationBar = enabled)
        prefs.edit().putBoolean("glassNavigationBar", enabled).apply()
    }

    fun resetToDefaults() {
        state = LiquidGlassOpticsState()
        prefs.edit().clear().apply()
    }
}

val LocalLiquidGlassOptics = staticCompositionLocalOf<LiquidGlassOpticsState> {
    LiquidGlassOpticsState()
}

/**
 * Dynamic Canvas Drawing Extension that renders realistic Liquid Glass Refraction,
 * Chromatic Aberration Dispersion halos, Specular Sheen Arc, and Internal Caustic Rings.
 */
fun DrawScope.drawLiquidGlassRefraction(
    optics: LiquidGlassOpticsState,
    accentColor: Color,
    isPill: Boolean = true
) {
    val width = size.width
    val height = size.height
    if (width <= 0f || height <= 0f) return

    val refractionHeight = optics.lensRefractionHeight
    val vibrancy = optics.vibrancy
    val blurRadius = optics.blurRadius
    val cornerRadius = if (isPill) height / 2f else 24f

    // 1. Ultra-Translucent See-Through Glass Tint (Allows underlying elements to show through and overlap clearly)
    val glassAlpha = (optics.surfaceOpacity * 0.35f).coerceIn(0.02f, 0.14f)
    val glassTopTint = Color.White.copy(alpha = (0.08f * vibrancy).coerceIn(0.03f, 0.14f))
    val glassMidTint = optics.surfaceTint.copy(alpha = glassAlpha)
    val glassBottomTint = Color.White.copy(alpha = 0.03f)

    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                glassTopTint,
                glassMidTint,
                glassBottomTint
            )
        ),
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
    )

    // 2. Translucent Glass Surface Luminance & Micro-Frost Layer
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = (0.14f * vibrancy).coerceIn(0.06f, 0.24f)),
                Color.White.copy(alpha = (0.04f * blurRadius).coerceIn(0.01f, 0.08f)),
                Color.Transparent,
                Color.White.copy(alpha = (0.06f * vibrancy).coerceIn(0.02f, 0.12f))
            )
        ),
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
    )

    // 3. Chromatic Aberration Dispersion Edge Glows on the curved extremities
    if (optics.chromaticAberration && isPill && width > height) {
        val arcRadius = height / 2f
        // Left curve chromatic cyan/blue edge
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF80D8FF).copy(alpha = 0.22f * vibrancy),
                    Color(0xFF00B0FF).copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(arcRadius * 0.7f, height / 2f),
                radius = arcRadius * 1.1f
            )
        )
        // Right curve chromatic violet/magenta edge
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.26f * vibrancy),
                    Color(0xFFE040FB).copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = Offset(width - arcRadius * 0.7f, height / 2f),
                radius = arcRadius * 1.1f
            )
        )
    }

    // 4. Apple Authentic Curved Specular Reflection Highlight (Follows rounded geometry)
    val sheenAlpha = (0.65f + 0.30f * refractionHeight).coerceIn(0.40f, 0.95f)
    drawRoundRect(
        brush = Brush.horizontalGradient(
            0.0f to Color.Transparent,
            0.15f to Color.White.copy(alpha = sheenAlpha * 0.45f),
            0.50f to Color.White.copy(alpha = sheenAlpha),
            0.85f to Color.White.copy(alpha = sheenAlpha * 0.45f),
            1.0f to Color.Transparent
        ),
        topLeft = Offset(1.2f, 1.2f),
        size = androidx.compose.ui.geometry.Size(width - 2.4f, height - 2.4f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
            (cornerRadius - 1.2f).coerceAtLeast(0f),
            (cornerRadius - 1.2f).coerceAtLeast(0f)
        ),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.0f)
    )

    // 5. Subtle Bottom Glass Caustic Accent (Curved along bottom contour)
    if (optics.depthEffect) {
        drawRoundRect(
            brush = Brush.horizontalGradient(
                0.0f to Color.Transparent,
                0.25f to Color.White.copy(alpha = 0.15f * vibrancy),
                0.50f to accentColor.copy(alpha = 0.20f * vibrancy),
                0.75f to Color.White.copy(alpha = 0.15f * vibrancy),
                1.0f to Color.Transparent
            ),
            topLeft = Offset(2f, 2f),
            size = androidx.compose.ui.geometry.Size(width - 4f, height - 4f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                (cornerRadius - 2f).coerceAtLeast(0f),
                (cornerRadius - 2f).coerceAtLeast(0f)
            ),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.8f)
        )
    }
}

/**
 * Settings UI Component matching the uploaded reference screenshot:
 * Dark squircle container, icon badge on the left, title, description, and slider / switch control.
 */
@Composable
fun LiquidGlassSettingItem(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = null,
            tint = Color(0xFFD5C6A9),
            modifier = Modifier.size(20.dp)
        )
    },
    control: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = Color.Black.copy(alpha = 0.4f),
                ambientColor = Color.Transparent
            )
            .border(
                width = 1.dp,
                color = Color(0x18FFFFFF),
                shape = RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp)),
        color = Color(0xFF1B1915) // Dark warm surface matching reference Screenshot 1
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Squircle Icon Badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            Color(0xFF2E2A22),
                            RoundedCornerShape(14.dp)
                        )
                        .border(
                            1.dp,
                            Color(0x18FFFFFF),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Title and Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color(0xFFEDE7DC),
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        color = Color(0xFF9E978C),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Control (Switch or Action)
                control()
            }
        }
    }
}

/**
 * Slider Setting Item for continuous Liquid Glass optical parameters (Vibrancy, Blur, Lens Height, Lens Amount).
 */
@Composable
fun LiquidGlassSliderItem(
    title: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    accentColor: Color = Color(0xFFD5C6A9)
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = Color.Black.copy(alpha = 0.4f),
                ambientColor = Color.Transparent
            )
            .border(
                width = 1.dp,
                color = Color(0x18FFFFFF),
                shape = RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp)),
        color = Color(0xFF1B1915)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Squircle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            Color(0xFF2E2A22),
                            RoundedCornerShape(14.dp)
                        )
                        .border(
                            1.dp,
                            Color(0x18FFFFFF),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color(0xFFD5C6A9),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            color = Color(0xFFEDE7DC),
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0x28D6C683), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0x40D6C683), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${(value * 100).toInt()}%",
                                color = Color(0xFFD6C683),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        color = Color(0xFF9E978C),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // Interactive Slider Area
            Spacer(modifier = Modifier.height(10.dp))
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0.0f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFEDE7DC),
                    activeTrackColor = Color(0xFFD6C683),
                    inactiveTrackColor = Color(0xFF2E2A22)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Toggle Switch Setting Item matching Screenshot 1 (Chromatic Aberration, Depth Effect).
 */
@Composable
fun LiquidGlassSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color = Color(0xFFD5C6A9)
) {
    LiquidGlassSettingItem(
        title = title,
        description = description,
        icon = {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = Color(0xFFD5C6A9),
                modifier = Modifier.size(20.dp)
            )
        },
        control = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF352F1C),
                    checkedTrackColor = Color(0xFFD6C683),
                    uncheckedThumbColor = Color(0xFF8A8376),
                    uncheckedTrackColor = Color(0xFF2A2722)
                ),
                thumbContent = if (checked) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFFD6C683),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                } else null
            )
        }
    )
}

/**
 * Palette Item with interactive Color Picker for Surface Tint & Glass Text Colour (Screenshots 2 & 3).
 */
@Composable
fun LiquidGlassColorItem(
    title: String,
    description: String,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    accentColor: Color = Color(0xFFD0BCFF)
) {
    var showDialog by remember { mutableStateOf(false) }

    val presetColors = listOf(
        Color(0xFF1E1528) to "Midnight Plum",
        Color(0xFF101828) to "Dark Slate",
        Color(0xFF0F172A) to "Deep Blue",
        Color(0xFF1E1E24) to "Zinc Charcoal",
        Color(0xFF281018) to "Burgundy Noir",
        Color(0xFF09090B) to "Pitch Black",
        Color(0xFFFFFFFF) to "Pure White",
        Color(0xFF38BDF8) to "Sky Cyan",
        Color(0xFFF472B6) to "Neon Rose",
        Color(0xFFE4E4E7) to "Platinum"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = accentColor.copy(alpha = 0.12f),
                ambientColor = Color.Transparent
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.14f),
                        accentColor.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.03f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .clip(RoundedCornerShape(22.dp))
            .clickable { showDialog = true },
        color = Color(0xFF16141D)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Squircle Icon Badge with Palette Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        Color(0x33282236),
                        RoundedCornerShape(14.dp)
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.12f),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = Color(0xFFA59FB3),
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Color Indicator Circle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(selectedColor, CircleShape)
                    .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose a palette preset or custom tint:",
                        color = Color(0xFFA59FB3),
                        fontSize = 13.sp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        presetColors.take(5).forEach { (c, _) ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(c, CircleShape)
                                    .border(
                                        width = if (selectedColor == c) 2.5.dp else 1.dp,
                                        color = if (selectedColor == c) accentColor else Color.White.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onColorSelected(c)
                                        showDialog = false
                                    }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        presetColors.drop(5).forEach { (c, _) ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(c, CircleShape)
                                    .border(
                                        width = if (selectedColor == c) 2.5.dp else 1.dp,
                                        color = if (selectedColor == c) accentColor else Color.White.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onColorSelected(c)
                                        showDialog = false
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Close", color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1E1A29),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

/**
 * Full Interactive Liquid Glass Optics Studio Settings Panel.
 * Directly matches the exact layout, categories, and switches from Screenshots 2 & 3.
 */
@Composable
fun LiquidGlassOpticsStudio(
    opticsManager: LiquidGlassOpticsManager,
    accentColor: Color = Color(0xFFD0BCFF),
    onDismiss: (() -> Unit)? = null
) {
    val state = opticsManager.state

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Reset & Info Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Liquid Glass Engine",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            TextButton(
                onClick = { opticsManager.resetToDefaults() }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = accentColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset Defaults", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Live Optical Capsule Preview with Crisp White Border
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(
                    elevation = if (state.depthEffect) 12.dp else 0.dp,
                    shape = CircleShape,
                    spotColor = Color.Black.copy(alpha = 0.45f),
                    ambientColor = Color.Transparent
                )
                .border(
                    width = 1.3.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0.40f),
                            Color.White.copy(alpha = 0.70f)
                        )
                    ),
                    shape = CircleShape
                )
                .clip(CircleShape),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawLiquidGlassRefraction(state, accentColor, isPill = true)
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = state.glassTextColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Live Optics Preview", color = state.glassTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = if (state.chromaticAberration) "Prism Refraction ON" else "Refraction Standard",
                        color = state.glassTextColor.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ================= SECTION 1: EFFECTS =================
        Text(
            text = "Effects",
            color = Color.White.copy(alpha = 0.90f),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )

        // 1. Lens Refraction Height (Screenshot 1: First item)
        LiquidGlassSliderItem(
            title = "Lens Refraction Height",
            description = "Manage Lens Refraction Height settings",
            value = state.lensRefractionHeight,
            onValueChange = { opticsManager.updateLensRefractionHeight(it) },
            accentColor = accentColor
        )

        // 2. Lens Refraction Amount (Screenshot 1: Second item)
        LiquidGlassSliderItem(
            title = "Lens Refraction Amount",
            description = "Manage Lens Refraction Amount settings",
            value = state.lensRefractionAmount,
            onValueChange = { opticsManager.updateLensRefractionAmount(it) },
            accentColor = accentColor
        )

        // 3. Chromatic Aberration (Screenshot 1: Third item)
        LiquidGlassSwitchItem(
            title = "Chromatic Aberration",
            description = "Manage Chromatic Aberration settings",
            checked = state.chromaticAberration,
            onCheckedChange = { opticsManager.setChromaticAberration(it) },
            accentColor = accentColor
        )

        // 4. Depth Effect (Screenshot 1: Fourth item)
        LiquidGlassSwitchItem(
            title = "Depth Effect",
            description = "Manage Depth Effect settings",
            checked = state.depthEffect,
            onCheckedChange = { opticsManager.setDepthEffect(it) },
            accentColor = accentColor
        )

        // 5. Blur Radius (Gaussian blur amount)
        LiquidGlassSliderItem(
            title = "Blur Radius",
            description = "Amount of blur on the glass surface",
            value = state.blurRadius,
            onValueChange = { opticsManager.updateBlurRadius(it) },
            accentColor = accentColor
        )

        // 6. Vibrancy
        LiquidGlassSliderItem(
            title = "Vibrancy",
            description = "Boost the saturation of the glass backdrop",
            value = state.vibrancy,
            onValueChange = { opticsManager.updateVibrancy(it) },
            accentColor = accentColor
        )

        // ================= SECTION 2: APPEARANCE =================
        Text(
            text = "Appearance",
            color = Color.White.copy(alpha = 0.90f),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 12.dp, start = 4.dp)
        )

        // 7. Surface Tint
        LiquidGlassColorItem(
            title = "Surface Tint",
            description = "Tint colour applied to the glass surface",
            selectedColor = state.surfaceTint,
            onColorSelected = { opticsManager.updateSurfaceTint(it) },
            accentColor = accentColor
        )

        // 8. Surface Opacity
        LiquidGlassSliderItem(
            title = "Surface Opacity",
            description = "Opacity of the glass tint overlay for readability",
            value = state.surfaceOpacity,
            onValueChange = { opticsManager.updateSurfaceOpacity(it) },
            accentColor = accentColor
        )

        // 9. Glass Text Colour
        LiquidGlassColorItem(
            title = "Glass Text Colour",
            description = "Text colour used on glass surfaces",
            selectedColor = state.glassTextColor,
            onColorSelected = { opticsManager.updateGlassTextColor(it) },
            accentColor = accentColor
        )

        // ================= SECTION 3: PER COMPONENT =================
        Text(
            text = "Per Component",
            color = Color.White.copy(alpha = 0.90f),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 12.dp, start = 4.dp)
        )

        // 10. Glass Player
        LiquidGlassSettingItem(
            title = "Glass Player",
            description = "Manage Glass Player settings",
            icon = {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (state.glassPlayer) accentColor else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            },
            control = {
                Switch(
                    checked = state.glassPlayer,
                    onCheckedChange = { opticsManager.setGlassPlayer(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF8B5CF6),
                        uncheckedThumbColor = Color.LightGray,
                        uncheckedTrackColor = Color(0x33282236)
                    ),
                    thumbContent = if (state.glassPlayer) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF8B5CF6),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    } else null
                )
            }
        )

        // 11. Glass Mini Player
        LiquidGlassSettingItem(
            title = "Glass Mini Player",
            description = "Manage Glass Mini Player settings",
            icon = {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (state.glassMiniPlayer) accentColor else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            },
            control = {
                Switch(
                    checked = state.glassMiniPlayer,
                    onCheckedChange = { opticsManager.setGlassMiniPlayer(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF8B5CF6),
                        uncheckedThumbColor = Color.LightGray,
                        uncheckedTrackColor = Color(0x33282236)
                    ),
                    thumbContent = if (state.glassMiniPlayer) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF8B5CF6),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    } else null
                )
            }
        )

        // 12. Glass Navigation Bar
        LiquidGlassSettingItem(
            title = "Glass Navigation Bar",
            description = "Manage Glass Navigation Bar settings",
            icon = {
                Icon(
                    imageVector = Icons.Default.WebAsset,
                    contentDescription = null,
                    tint = if (state.glassNavigationBar) accentColor else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            },
            control = {
                Switch(
                    checked = state.glassNavigationBar,
                    onCheckedChange = { opticsManager.setGlassNavigationBar(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF8B5CF6),
                        uncheckedThumbColor = Color.LightGray,
                        uncheckedTrackColor = Color(0x33282236)
                    ),
                    thumbContent = if (state.glassNavigationBar) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF8B5CF6),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    } else null
                )
            }
        )

        // ================= SECTION 3: APPLE DESIGN & LIQUID GLASS ELEMENTS =================
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Apple Fluid Materials & Physics",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = (-0.2).sp
                )
                Text(
                    text = "WWDC fluid springs, specular sheen & translucent depth",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 11.sp
                )
            }
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
        }

        var selectedMaterialIndex by remember { mutableIntStateOf(2) } // Default: REGULAR
        val materialTiers = remember { listOf("UltraThin", "Thin", "Regular", "Thick") }
        val currentMaterial = remember(selectedMaterialIndex) {
            when (selectedMaterialIndex) {
                0 -> AppleGlassMaterial.ULTRA_THIN
                1 -> AppleGlassMaterial.THIN
                2 -> AppleGlassMaterial.REGULAR
                else -> AppleGlassMaterial.THICK
            }
        }
        var showSheetDemo by remember { mutableStateOf(false) }

        // Apple Segmented Control for Translucent Hierarchy
        AppleLiquidGlassSegmentedControl(
            items = materialTiers,
            selectedIndex = selectedMaterialIndex,
            onSelectIndex = { selectedMaterialIndex = it },
            accentTint = accentColor
        )

        // Apple Liquid Glass Dynamic Island Pill
        AppleDynamicIslandCapsule(
            title = "Velorix Liquid Core",
            subtitle = "${materialTiers[selectedMaterialIndex]} Material Active • Damping 1.0",
            accentTint = accentColor,
            isActive = true,
            leadingIcon = Icons.Default.BlurOn
        )

        // Apple Liquid Glass Card Preview with Specular Light-Catching Rim
        AppleLiquidGlassCard(
            material = currentMaterial,
            accentTint = accentColor
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Fluid Specular Card",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = (-0.2).sp
                    )
                    Text(
                        text = "1px top light-catching bevel • Critically damped touch",
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 11.5.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons with instant pointer-down scale (0.96f) and haptic feedback
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppleLiquidGlassButton(
                    text = "Open Fluid Sheet",
                    icon = Icons.Default.VerticalAlignTop,
                    accentTint = accentColor,
                    isPrimary = true,
                    onClick = { showSheetDemo = true },
                    modifier = Modifier.weight(1f)
                )

                AppleLiquidGlassButton(
                    text = "Reset Optics",
                    icon = Icons.Default.Refresh,
                    accentTint = accentColor,
                    isPrimary = false,
                    onClick = { opticsManager.resetToDefaults() },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Live Liquid Glass Sheet with Rubber-Banding & Momentum Projection
        if (showSheetDemo) {
            AppleLiquidGlassSheet(
                title = "Fluid Glass Sheet",
                accentTint = accentColor,
                onDismissRequest = { showSheetDemo = false }
            ) {
                Text(
                    text = "Physics Behavior over Animation",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Interruptible continuous tracking with exponential momentum projection.\n• Soft boundary rubber-banding damping on over-drag.\n• 1px specular light-catching top edge and dual-gradient caustic substrate.",
                    color = Color.White.copy(alpha = 0.80f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                AppleLiquidGlassButton(
                    text = "Close Fluid Sheet",
                    icon = Icons.Default.Check,
                    accentTint = accentColor,
                    isPrimary = true,
                    onClick = { showSheetDemo = false },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (state.glassPlayer) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "LIVE GLASS MEDIA PLAYER PREVIEW",
                color = VelorixAccentLight,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                letterSpacing = 0.5.sp
            )
            LiquidGlassMusicPlayerPill(
                optics = state,
                accentColor = accentColor
            )
        }
    }
}

/**
 * Liquid Glass Music / Media Pill as depicted in Screenshot 2:
 * Features album cover avatar, track title "Faster n Harder", "2:04" timestamp,
 * playback timeline waveform, and interactive glass transport controls.
 */
@Composable
fun LiquidGlassMusicPlayerPill(
    optics: LiquidGlassOpticsState,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    trackTitle: String = "Fell For You",
    trackDuration: String = "3:12",
    accentColor: Color = Color(0xFFD0BCFF)
) {
    val playlist = remember {
        listOf(
            "Fell For You" to "3:12",
            "Faster n Harder" to "2:04",
            "Velorix Champions Theme" to "3:18",
            "Free Fire Apex Beats" to "2:45",
            "Adrenaline Rush Arena" to "3:02"
        )
    }
    var currentTrackIndex by remember { mutableIntStateOf(0) }
    val currentTrack = playlist[currentTrackIndex % playlist.size]
    var isPlaying by remember { mutableStateOf(true) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(CircleShape)
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.75f),
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.50f)
                    )
                ),
                shape = CircleShape
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Real-Time Optical Backdrop Sampling (Gaussian Blur, Lens Refraction & Chromatic Aberration)
        if (backdrop != null) {
            val pillShape = remember { CircleShape }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { pillShape },
                        effects = {
                            blur(radius = (optics.blurRadius * 32.dp.value).dp.toPx())
                            colorControls(
                                brightness = 0.05f * optics.vibrancy,
                                contrast = 1.05f,
                                saturation = 1.0f + (0.35f * optics.vibrancy)
                            )
                            vibrancy()
                            if (optics.lensRefractionHeight > 0f) {
                                lens(
                                    refractionHeight = (optics.lensRefractionHeight * 28.dp.value).dp.toPx(),
                                    refractionAmount = (optics.lensRefractionAmount * 36.dp.value).dp.toPx(),
                                    chromaticAberration = optics.chromaticAberration
                                )
                            }
                            opacity(alpha = 1.0f)
                        },
                        highlight = { Highlight.Ambient },
                        shadow = if (optics.depthEffect) {
                            { com.kashif_e.backdrop.shadow.Shadow(radius = 12.dp) }
                        } else null
                    )
            )
        }

        // Glass Specular Sheen Layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    drawLiquidGlassRefraction(optics, accentColor, isPill = true)
                }
        )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art / Game Avatar Icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(4.dp, CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFF1744), Color(0xFF7C4DFF), Color(0xFF00E5FF))
                            ),
                            CircleShape
                        )
                        .border(1.2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Track Art",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Track Title & Timeline
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentTrack.first,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (currentTrack.first == "Fell For You") "Shubh • 3:12" else currentTrack.second,
                            color = Color.White.copy(alpha = 0.70f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        // Mini waveform / animated equalizer bar
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(6) { index ->
                                val barHeight = if (isPlaying) (8 + (index * 3) % 10).dp else 4.dp
                                Box(
                                    modifier = Modifier
                                        .width(2.5.dp)
                                        .height(barHeight)
                                        .background(
                                            if (index % 2 == 0) accentColor else Color.White.copy(alpha = 0.8f),
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Play / Pause & Skip Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Color.White.copy(alpha = 0.15f),
                                CircleShape
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            currentTrackIndex = (currentTrackIndex + 1) % playlist.size
                            android.widget.Toast.makeText(context, "Now playing: ${playlist[currentTrackIndex % playlist.size].first}", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Color.White.copy(alpha = 0.08f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

/**
 * Real-time KMP Liquid Glass Backdrop helper composable and modifier integration.
 * Connects the `io.github.kashif-mehmood-km:backdrop` SDK directly into Compose UI.
 */
@Composable
fun KmpLiquidGlassBox(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    optics: LiquidGlassOpticsState = LocalLiquidGlassOptics.current,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    blur(radius = (optics.blurRadius * 32.dp.value).dp.toPx())
                    colorControls(
                        brightness = 0.05f * optics.vibrancy,
                        contrast = 1.1f,
                        saturation = 1.0f + (0.5f * optics.vibrancy)
                    )
                    vibrancy()
                    if (optics.lensRefractionHeight > 0f) {
                        lens(
                            refractionHeight = (optics.lensRefractionHeight * 28.dp.value).dp.toPx(),
                            refractionAmount = (optics.lensRefractionAmount * 36.dp.value).dp.toPx(),
                            chromaticAberration = optics.chromaticAberration
                        )
                    }
                    opacity(alpha = 1.0f)
                },
                highlight = { Highlight.Ambient },
                shadow = if (optics.depthEffect) {
                    { com.kashif_e.backdrop.shadow.Shadow(radius = 12.dp) }
                } else null
            )
    ) {
        content()
    }
}
