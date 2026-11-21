package com.example.ui.common

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.kashif_e.backdrop.*
import com.kashif_e.backdrop.backdrops.*
import com.kashif_e.backdrop.effects.*
import com.kashif_e.backdrop.highlight.*
import com.kashif_e.backdrop.shadow.*
import com.example.ui.theme.VelorixAccent
import com.example.ui.theme.VelorixAccentLight
import com.example.ui.theme.VelorixBg
import com.example.ui.theme.VelorixTextPrimary
import com.example.ui.theme.VelorixTextSecondary

// Glass Theme Color Palette (Pure AMOLED Black Base & Neutral Surfaces)
object GlassTokens {
    // Pure AMOLED Pitch Black Base
    val DeepCanvas = Color(0xFF000000)
    
    // Neutral dark system surfaces (zero blue tint)
    val GlassSurfacePrimary = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0A0A0D),
            Color(0xFF0A0A0D)
        )
    )

    val GlassSurfaceBright = Brush.linearGradient(
        colors = listOf(
            Color(0xFF121216),
            Color(0xFF121216)
        )
    )

    val GlassSurfaceSubtle = Brush.linearGradient(
        colors = listOf(
            Color(0xFF060608),
            Color(0xFF060608)
        )
    )

    // Subtle neutral hairline dividers (8% white, zero blue tint)
    val GlassBorderGradient = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.08f)
        )
    )

    val GlassBorderSubtle = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.07f),
            Color.White.copy(alpha = 0.07f)
        )
    )

    val GlassBorderActive = Brush.linearGradient(
        colors = listOf(
            Color(0xFF38BDF8),
            Color.White.copy(alpha = 0.14f)
        )
    )

    val ActivePillGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFE6EDF3),
            Color(0xFFD4D4D8)
        )
    )

    val FloatingOrbGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0A0A0D),
            Color(0xFF0A0A0D)
        )
    )
}

/**
 * Pure AMOLED Black (#000000) Canvas in Dark Mode.
 */
@Composable
fun GlassBackgroundBox(
    modifier: Modifier = Modifier,
    accentColor: Color = VelorixAccent,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = com.example.ui.theme.LocalIsDarkTheme.current
    val canvasBg by animateColorAsState(
        targetValue = if (isDark) Color(0xFF000000) else Color(0xFFF4F6F9),
        animationSpec = tween(durationMillis = 350), label = "canvasBg"
    )
    val topBloomColor by animateColorAsState(
        targetValue = if (isDark) Color(0xFF000000) else Color(0xFFE2E8F0),
        animationSpec = tween(durationMillis = 350), label = "topBloom"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // 1. Pure AMOLED Black (#000000) in dark mode
                drawRect(if (isDark) Color.Black else canvasBg)

                if (!isDark) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                topBloomColor.copy(alpha = 0.55f),
                                topBloomColor.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, -size.height * 0.05f),
                            radius = size.width * 1.1f
                        )
                    )

                    // Subtle Accent Glow in corner (Light mode only)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF6366F1).copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.9f, size.height * 0.25f),
                            radius = size.width * 0.7f
                        )
                    )

                    // Luminous Bottom Ambient Glow Mesh behind dock (Light mode only)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF7C3AED).copy(alpha = 0.14f),
                                Color(0xFF38BDF8).copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.92f),
                            radius = size.width * 0.95f
                        )
                    )
                }
            },
        content = content
    )
}

/**
 * Major Card Container with subtle neutral dark depth and 0.5.dp perimeter hairline.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    brush: Brush = GlassTokens.GlassSurfacePrimary,
    borderBrush: Brush = GlassTokens.GlassBorderGradient,
    borderWidth: Dp = 0.5.dp,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = com.example.ui.theme.LocalIsDarkTheme.current
    val isActiveHighlight = borderBrush == GlassTokens.GlassBorderActive
    val strokeColor = if (isDark) {
        if (isActiveHighlight) Color(0xFF38BDF8).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f)
    } else {
        Color(0xFFCBD5E1)
    }

    val clickModifier = if (onClick != null) {
        Modifier.bounceClick(scaleDown = 0.99f, onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .then(clickModifier),
        shape = shape,
        color = if (isDark) Color(0xFF0A0A0D) else Color.White,
        border = BorderStroke(borderWidth, strokeColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            content = content
        )
    }
}

data class GlassNavItem(
    val id: String,
    val label: String,
    val outlineResId: Int = 0,
    val filledResId: Int = 0,
    val icon: ImageVector? = null
)

/**
 * Draws realistic optical glass specular reflections, lens catch-lights, and bevel highlights
 * on liquid glass capsules and pill geometries.
 * The glass is completely clear/uncolored, with realistic physical light reflections.
 */
fun DrawScope.drawLiquidGlassOpticReflections(
    cornerRadius: Float,
    intensity: Float = 1.0f
) {
    val width = size.width
    val height = size.height
    if (width <= 0f || height <= 0f) return

    // 1. Crisp Top Bevel Specular Catch-Light (Brilliant white reflection along curved upper rim)
    drawRoundRect(
        brush = Brush.horizontalGradient(
            0.0f to Color.Transparent,
            0.12f to Color.White.copy(alpha = 0.22f * intensity),
            0.50f to Color.White.copy(alpha = 0.48f * intensity),
            0.88f to Color.White.copy(alpha = 0.22f * intensity),
            1.0f to Color.Transparent
        ),
        topLeft = Offset(1.2f, 1.2f),
        size = Size(width - 2.4f, height - 2.4f),
        cornerRadius = CornerRadius(
            (cornerRadius - 1.2f).coerceAtLeast(0f),
            (cornerRadius - 1.2f).coerceAtLeast(0f)
        ),
        style = Stroke(width = 1.6f)
    )

    // 2. Optical Glass Bevel Perimeter Stroke (Bright top edge, soft translucent bottom rim)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.35f * intensity),
                Color.White.copy(alpha = 0.10f * intensity),
                Color.White.copy(alpha = 0.05f * intensity),
                Color.White.copy(alpha = 0.18f * intensity)
            )
        ),
        topLeft = Offset(0.6f, 0.6f),
        size = Size(width - 1.2f, height - 1.2f),
        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
        style = Stroke(width = 1.2f)
    )

    // 3. Diagonal Optical Glass Surface Specular Sheen (Curved glass reflection glint across the face)
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.06f * intensity),
                Color.White.copy(alpha = 0.015f * intensity),
                Color.White.copy(alpha = 0.08f * intensity),
                Color.Transparent
            ),
            start = Offset(0f, 0f),
            end = Offset(width * 0.65f, height * 1.5f)
        ),
        topLeft = Offset(2f, 2f),
        size = Size(width - 4f, height - 4f),
        cornerRadius = CornerRadius(
            (cornerRadius - 2f).coerceAtLeast(0f),
            (cornerRadius - 2f).coerceAtLeast(0f)
        )
    )

    // 4. Subtle Bottom Inner Glass Caustic Catch-Light
    drawRoundRect(
        brush = Brush.horizontalGradient(
            0.0f to Color.Transparent,
            0.25f to Color.White.copy(alpha = 0.08f * intensity),
            0.50f to Color.White.copy(alpha = 0.18f * intensity),
            0.75f to Color.White.copy(alpha = 0.08f * intensity),
            1.0f to Color.Transparent
        ),
        topLeft = Offset(2f, height - 3.5f),
        size = Size(width - 4f, 2f),
        style = Stroke(width = 1f)
    )
}

/**
 * Draws clear circular glass lens specular reflections and crescent highlights on the Search Orb.
 */
fun DrawScope.drawCircularGlassSpecularOrb(
    intensity: Float = 1.0f
) {
    val diameter = size.minDimension
    val radius = diameter / 2f
    val center = Offset(size.width / 2f, size.height / 2f)

    // 1. Outer Glass Bevel Rim
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.42f * intensity),
                Color.White.copy(alpha = 0.10f * intensity),
                Color.White.copy(alpha = 0.18f * intensity)
            )
        ),
        radius = radius - 0.8f,
        center = center,
        style = Stroke(width = 1.2f)
    )

    // 2. Top-Left Specular Crescent Catch-Light Arc (Authentic curved spherical glass reflection)
    drawArc(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.35f * intensity),
                Color.White.copy(alpha = 0.48f * intensity),
                Color.Transparent
            ),
            start = Offset(radius * 0.2f, radius * 0.2f),
            end = Offset(radius * 1.8f, radius * 0.4f)
        ),
        startAngle = 190f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(2f, 2f),
        size = Size(diameter - 4f, diameter - 4f),
        style = Stroke(width = 2.2f)
    )

    // 3. Inner Radial Specular Gloss Sheen
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.12f * intensity),
                Color.White.copy(alpha = 0.025f * intensity),
                Color.Transparent
            ),
            center = Offset(center.x - radius * 0.3f, center.y - radius * 0.35f),
            radius = radius * 0.85f
        )
    )
}

/**
 * Draws crisp optical specular edge glow catch-lights ONLY on the two side edges of the active pill.
 * The body and center remain transparent frosted glass — strictly NO glow over the whole pill or nav bar.
 */
fun DrawScope.drawPillTwoSidedEdgeGlow(
    cornerRadius: Float,
    intensity: Float = 1.0f,
    accentColor: Color = Color.White
) {
    val width = size.width
    val height = size.height
    if (width <= 0f || height <= 0f) return

    val edgeGlowWidth = (height * 0.40f).coerceAtMost(width * 0.28f)

    // 1. Left Curved Side Edge Glow
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.50f * intensity),
                accentColor.copy(alpha = 0.18f * intensity),
                Color.Transparent
            ),
            startX = 0f,
            endX = edgeGlowWidth
        ),
        topLeft = Offset(0.8f, 0.8f),
        size = Size(edgeGlowWidth, height - 1.6f),
        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
        style = Stroke(width = 1.6f)
    )

    // 2. Right Curved Side Edge Glow
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                accentColor.copy(alpha = 0.18f * intensity),
                accentColor.copy(alpha = 0.50f * intensity)
            ),
            startX = width - edgeGlowWidth,
            endX = width
        ),
        topLeft = Offset(width - edgeGlowWidth, 0.8f),
        size = Size(edgeGlowWidth, height - 1.6f),
        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
        style = Stroke(width = 1.6f)
    )
}

/**
 * Backward compatibility: Draws optical two-sided edge glow catch-lights on active pill elements.
 */
fun DrawScope.drawSelectionBackgroundGlow(
    intensity: Float = 1.0f
) {
    drawPillTwoSidedEdgeGlow(
        cornerRadius = size.height / 2f,
        intensity = intensity
    )
}

/**
 * Floating Liquid Glass Navigation Bar directly replicating the reference design:
 * - 100% translucent, see-through frosted glass body without opaque black or dark tint.
 * - Genuine optical glass lens specular reflections, catch-light bevels, and light sheens.
 * - Pure optical clarity: no rainbow or colored tints.
 * - Active tab with two-sided specular edge reflections.
 * - Floating circular glass search orb side-by-side with the navigation capsule.
 */
@Composable
fun FloatingGlassNavBar(
    selectedTab: String,
    onNavClick: (String) -> Unit,
    onQuickSearchClick: (() -> Unit)? = null,
    accentColor: Color = Color.White,
    optics: LiquidGlassOpticsState = LiquidGlassOpticsState(),
    backdrop: Backdrop? = null,
    blurProgress: Float = 1f,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        GlassNavItem(
            id = "dashboard",
            label = "Home",
            outlineResId = R.drawable.ic_iconsax_home_outline,
            filledResId = R.drawable.ic_iconsax_home_filled,
            icon = Icons.Outlined.Home
        ),
        GlassNavItem(
            id = "tournaments_list",
            label = "Tourneys",
            outlineResId = R.drawable.ic_iconsax_game_outline,
            filledResId = R.drawable.ic_iconsax_game_filled,
            icon = Icons.Outlined.SportsEsports
        ),
        GlassNavItem(
            id = "operations",
            label = "Ops",
            outlineResId = R.drawable.ic_iconsax_tick_square_outline,
            filledResId = R.drawable.ic_iconsax_tick_square_filled,
            icon = Icons.Outlined.TaskAlt
        ),
        GlassNavItem(
            id = "payouts",
            label = "Finance",
            outlineResId = R.drawable.ic_iconsax_wallet_outline,
            filledResId = R.drawable.ic_iconsax_wallet_filled,
            icon = Icons.Outlined.AccountBalanceWallet
        ),
        GlassNavItem(
            id = "system",
            label = "Settings",
            outlineResId = R.drawable.ic_iconsax_settings_outline,
            filledResId = R.drawable.ic_iconsax_settings_filled,
            icon = Icons.Outlined.Settings
        )
    )

    // Floating Glass Navigation Dock (NO OPAQUE BLACK BACKGROUND!)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp, top = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main Liquid Glass Capsule Pill Dock
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(62.dp)
                    .clip(RoundedCornerShape(31.dp))
            ) {
                // Real-Time Optical Backdrop Sampling (Reduced Blur + Clear Untinted Lens Refraction, Zero Blue Tint)
                if (backdrop != null && optics.glassNavigationBar) {
                    val capsuleShape = remember { RoundedCornerShape(31.dp) }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { capsuleShape },
                                effects = {
                                    blur(radius = (optics.blurRadius * 12.dp.value * blurProgress.coerceAtLeast(0.75f)).coerceIn(2f, 9f).dp.toPx())
                                    colorControls(
                                        brightness = 0.02f,
                                        contrast = 1.02f,
                                        saturation = 1.0f
                                    )
                                    if (optics.lensRefractionHeight > 0f) {
                                        lens(
                                            refractionHeight = (optics.lensRefractionHeight * 22.dp.value).dp.toPx(),
                                            refractionAmount = (optics.lensRefractionAmount * 28.dp.value).dp.toPx(),
                                            chromaticAberration = false
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

                // Glass Layer: Pure Untinted Optical Specular Reflections & Bevels
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            drawLiquidGlassOpticReflections(
                                cornerRadius = size.height / 2f,
                                intensity = optics.vibrancy.coerceIn(0.45f, 0.75f)
                            )
                        }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navItems.forEach { item ->
                            val isSelected = when (item.id) {
                                "dashboard" -> selectedTab == "dashboard"
                                "tournaments_list" -> selectedTab == "tournaments_list"
                                "operations" -> selectedTab in listOf("operations", "users", "complaints", "verification")
                                "payouts" -> selectedTab in listOf("payouts")
                                "system" -> selectedTab in listOf("system", "admins", "lowcode", "settings", "glass_optics", "leaderboard", "tournament_rules")
                                else -> selectedTab == item.id
                            }

                            // Bouncy expanding weight: sleeker than the old oversized pill, wide enough for full label text
                            val itemWeight by animateFloatAsState(
                                targetValue = if (isSelected) 1.75f else 0.85f,
                                animationSpec = spring(
                                    dampingRatio = 0.60f,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "navItemWeight"
                            )

                            val pillAlpha by animateFloatAsState(
                                targetValue = if (isSelected) 1f else 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "navPillAlpha"
                            )

                            val pillScaleX by animateFloatAsState(
                                targetValue = if (isSelected) 1f else 0.82f,
                                animationSpec = spring(
                                    dampingRatio = 0.56f,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "navPillScaleX"
                            )

                            val pillScaleY by animateFloatAsState(
                                targetValue = if (isSelected) 1f else 0.86f,
                                animationSpec = spring(
                                    dampingRatio = 0.56f,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "navPillScaleY"
                            )

                            val iconScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.05f else 0.94f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "navIconScale"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(itemWeight)
                                    .fillMaxHeight()
                                    .padding(vertical = 2.dp, horizontal = 2.dp)
                                    .bounceClick(scaleDown = 0.88f) {
                                        onNavClick(item.id)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Pure Untinted Frosted Pill with Bouncy Scale & Two-Sided White Specular Edge Glow
                                if (pillAlpha > 0.01f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                alpha = pillAlpha
                                                scaleX = pillScaleX
                                                scaleY = pillScaleY
                                            }
                                            .clip(RoundedCornerShape(23.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = 0.14f),
                                                        Color.White.copy(alpha = 0.08f),
                                                        Color.White.copy(alpha = 0.03f)
                                                    )
                                                )
                                            )
                                            .drawBehind {
                                                drawPillTwoSidedEdgeGlow(
                                                    cornerRadius = size.height / 2f,
                                                    intensity = optics.vibrancy.coerceIn(0.45f, 0.75f),
                                                    accentColor = Color.White
                                                )
                                            }
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 6.dp)
                                ) {
                                    // Iconsax Icon: Filled when selected, Outline when unselected
                                    val iconRes = if (isSelected) item.filledResId else item.outlineResId
                                    if (iconRes != 0) {
                                        Icon(
                                            painter = painterResource(id = iconRes),
                                            contentDescription = item.label,
                                            tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.58f),
                                            modifier = Modifier
                                                .size(19.dp)
                                                .graphicsLayer {
                                                    scaleX = iconScale
                                                    scaleY = iconScale
                                                }
                                        )
                                    } else if (item.icon != null) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label,
                                            tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.58f),
                                            modifier = Modifier
                                                .size(19.dp)
                                                .graphicsLayer {
                                                    scaleX = iconScale
                                                    scaleY = iconScale
                                                }
                                        )
                                    }

                                    // Full label visible when selected with bouncy horizontal expansion
                                    AnimatedVisibility(
                                        visible = isSelected,
                                        enter = fadeIn(
                                            animationSpec = tween(durationMillis = 150, easing = LinearOutSlowInEasing)
                                        ) + expandHorizontally(
                                            animationSpec = spring(dampingRatio = 0.68f, stiffness = 380f),
                                            expandFrom = Alignment.Start
                                        ),
                                        exit = fadeOut(
                                            animationSpec = tween(durationMillis = 90, easing = FastOutLinearInEasing)
                                        ) + shrinkHorizontally(
                                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f),
                                            shrinkTowards = Alignment.Start
                                        )
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = item.label,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Visible
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Companion Floating Circular Glass Search Orb
            if (onQuickSearchClick != null) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                ) {
                    // Real-Time Optical Backdrop Sampling (Reduced Blur + Untinted Lens Refraction)
                    if (backdrop != null) {
                        val orbShape = remember { CircleShape }
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { orbShape },
                                    effects = {
                                        blur(radius = (optics.blurRadius * 12.dp.value * blurProgress.coerceAtLeast(0.75f)).coerceIn(2f, 9f).dp.toPx())
                                        colorControls(
                                            brightness = 0.02f,
                                            contrast = 1.02f,
                                            saturation = 1.0f
                                        )
                                        if (optics.lensRefractionHeight > 0f) {
                                            lens(
                                                refractionHeight = (optics.lensRefractionHeight * 22.dp.value).dp.toPx(),
                                                refractionAmount = (optics.lensRefractionAmount * 28.dp.value).dp.toPx(),
                                                chromaticAberration = false
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

                    // Specular Crescent Light Reflection & Glass Bevel
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawBehind {
                                drawCircularGlassSpecularOrb(intensity = optics.vibrancy)
                            }
                            .bounceClick(scaleDown = 0.88f) { onQuickSearchClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * iOS-style Progressive Blur Navbar Component implementing the guide from the screenshots:
 * - Tracks scroll position
 * - Computes blur progress
 * - Applies progressive blur modifier and fading alpha
 * - Frosted glass finish with specular highlights
 */
@Composable
fun ProgressiveBlurBottomBar(
    blurProgress: Float,
    selectedTab: String,
    onNavClick: (String) -> Unit,
    onQuickSearchClick: (() -> Unit)? = null,
    accentColor: Color = Color.White,
    optics: LiquidGlassOpticsState = LiquidGlassOpticsState(),
    modifier: Modifier = Modifier
) {
    FloatingGlassNavBar(
        selectedTab = selectedTab,
        onNavClick = onNavClick,
        onQuickSearchClick = onQuickSearchClick,
        accentColor = accentColor,
        optics = optics,
        blurProgress = blurProgress,
        modifier = modifier
    )
}

/**
 * Glass Stat Metric Chip with glass refraction and neon highlight
 */
@Composable
fun GlassStatChip(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color = VelorixAccent,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null) {
        Modifier.bounceClick(scaleDown = 0.99f, onClick = onClick)
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .background(VelorixBg)
            .then(clickModifier)
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = VelorixTextSecondary,
                letterSpacing = 0.8.sp
            )
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = VelorixTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            fontSize = 10.sp,
            color = VelorixTextSecondary.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(
            thickness = 0.5.dp,
            color = Color.White.copy(alpha = 0.08f)
        )
    }
}

/**
 * Sleek Borderless Primary Action (No harsh inner box border)
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector? = null,
    gradient: Brush? = null,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) VelorixAccent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.04f))
            .bounceClick(scaleDown = 0.96f) { if (enabled) onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) VelorixAccent else Color(0xFF64748B),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = if (enabled) VelorixAccent else Color(0xFF64748B),
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                letterSpacing = 0.3.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Sleek Borderless Secondary Action (No nested box border)
 */
@Composable
fun GlassOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector? = null,
    borderColor: Color = Color.Transparent
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A2232))
            .bounceClick(scaleDown = 0.96f) { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = VelorixTextPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = VelorixTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                letterSpacing = 0.3.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Minimal theme toggle with animated rotation and haptic response.
 */
@Composable
fun ThemeToggleSwitch(
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isDark = com.example.ui.theme.LocalIsDarkTheme.current
    val rotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isDark) 0f else 180f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "themeRotation"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .bounceClick(scaleDown = 0.92f) {
                com.example.ui.theme.ThemeManager.toggleTheme(context)
            },
        color = if (isDark) Color(0xFF131924) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF222C3C) else Color(0xFFCBD5E1)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = if (isDark) "Switch to Light Theme" else "Switch to Dark Theme",
                tint = if (isDark) Color(0xFF818CF8) else Color(0xFFF59E0B),
                modifier = Modifier
                    .size(15.dp)
                    .graphicsLayer(rotationZ = rotation)
            )
            Text(
                text = if (isDark) "Dark" else "Light",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) VelorixTextPrimary else Color(0xFF0F172A)
            )
        }
    }
}

