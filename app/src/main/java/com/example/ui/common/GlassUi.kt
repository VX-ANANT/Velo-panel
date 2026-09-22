package com.example.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
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
import com.example.ui.theme.VelorixBg
import com.example.ui.theme.VelorixTextPrimary
import com.example.ui.theme.VelorixTextSecondary

// Glass Theme Color Palette
object GlassTokens {
    // Pitch Black / Zinc Minimalist Base
    val DeepCanvas = Color(0xFF000000)
    
    // Clean solid-feel translucent surfaces (Vercel dark mode aesthetic)
    val GlassSurfacePrimary = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0F0F11),
            Color(0xFF09090B)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val GlassSurfaceBright = Brush.linearGradient(
        colors = listOf(
            Color(0xFF18181B),
            Color(0xFF0F0F11)
        )
    )

    val GlassSurfaceSubtle = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0D0D0E),
            Color(0xFF050506)
        )
    )

    // Crisp high-precision Zinc borders
    val GlassBorderGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF3F3F46),
            Color(0xFF27272A),
            Color(0xFF18181B)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val GlassBorderSubtle = Brush.linearGradient(
        colors = listOf(
            Color(0xFF27272A),
            Color(0xFF18181B)
        )
    )

    val GlassBorderActive = Brush.linearGradient(
        colors = listOf(
            Color(0xFF8B5CF6),
            Color(0xFF6D28D9),
            Color(0xFF3F3F46)
        )
    )

    // Active Glow Brushes
    val ActivePillGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFE4E4E7)
        )
    )

    val FloatingOrbGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF18181B),
            Color(0xFF09090B)
        )
    )
}

/**
 * Minimalist Dark Canvas with subtle ambient monochrome/violet bloom (Vercel/Lovable inspired).
 */
@Composable
fun GlassBackgroundBox(
    modifier: Modifier = Modifier,
    accentColor: Color = VelorixAccent,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // 1. Deep Pitch Black Canvas
                drawRect(Color(0xFF000000))

                // 2. Subtle Top Ambient Bloom
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1E1E24).copy(alpha = 0.45f),
                            Color(0xFF121216).copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, -size.height * 0.05f),
                        radius = size.width * 1.1f
                    )
                )

                // 3. Subtle Violet Accent Glow in corner
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6).copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.9f, size.height * 0.25f),
                        radius = size.width * 0.7f
                    )
                )

                // 4. Luminous Bottom Ambient Glow Mesh behind the floating dock (illuminates translucent glass)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.28f),
                            Color(0xFF6366F1).copy(alpha = 0.16f),
                            Color(0xFF0F172A).copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.92f),
                        radius = size.width * 0.95f
                    )
                )
            },
        content = content
    )
}

/**
 * Premium Minimalist Dark Card with crisp Zinc borders and subtle gradient.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    brush: Brush = GlassTokens.GlassSurfacePrimary,
    borderBrush: Brush = GlassTokens.GlassBorderGradient,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.bounceClick(scaleDown = 0.98f, onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .border(borderWidth, borderBrush, shape)
            .clip(shape)
            .then(clickModifier),
        shape = shape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(brush)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

data class GlassNavItem(
    val id: String,
    val label: String,
    val icon: ImageVector
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
 * Draws a luminous background light glow behind the active selected navigation item.
 * Pure light bloom with ONLY background glow — NO selection box, NO borders, and NO dark accents.
 */
fun DrawScope.drawSelectionBackgroundGlow(
    intensity: Float = 1.0f
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val glowRadius = (size.width.coerceAtLeast(size.height)) * 0.72f

    // Soft luminous ambient light bloom radiating smoothly behind the selected item
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f * intensity),
                Color.White.copy(alpha = 0.11f * intensity),
                Color.White.copy(alpha = 0.03f * intensity),
                Color.Transparent
            ),
            center = center,
            radius = glowRadius
        )
    )
}

/**
 * Floating Liquid Glass Navigation Bar directly replicating the reference design:
 * - 100% translucent, see-through frosted glass body without opaque black or dark tint.
 * - Genuine optical glass lens specular reflections, catch-light bevels, and light sheens.
 * - Pure optical clarity: no rainbow or colored tints.
 * - Active tab with convex magnifying glass lens dome.
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
    if (!optics.glassNavigationBar) return

    val navItems = listOf(
        GlassNavItem("dashboard", "Home", Icons.Default.Home),
        GlassNavItem("tournaments_list", "Tourneys", Icons.Default.EmojiEvents),
        GlassNavItem("operations", "Ops", Icons.Default.VerifiedUser),
        GlassNavItem("payouts", "Finance", Icons.Default.Payments),
        GlassNavItem("system", "System", Icons.Default.Settings)
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
                    .height(64.dp)
                    .clip(RoundedCornerShape(32.dp))
            ) {
                // Real-Time Optical Backdrop Sampling (2% Gaussian Blur, Lens Refraction & Chromatic Aberration)
                if (backdrop != null) {
                    val capsuleShape = remember { RoundedCornerShape(32.dp) }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { capsuleShape },
                                effects = {
                                    blur(radius = (optics.blurRadius * 2.dp.value * blurProgress).coerceAtLeast(0.5f).dp.toPx())
                                    colorControls(
                                        brightness = 0.05f * optics.vibrancy,
                                        contrast = 1.05f,
                                        saturation = 1.0f + (0.35f * optics.vibrancy)
                                    )
                                    vibrancy()
                                    if (optics.lensRefractionHeight > 0f) {
                                        lens(
                                            refractionHeight = (optics.lensRefractionHeight * 24.dp.value).dp.toPx(),
                                            refractionAmount = (optics.lensRefractionAmount * 32.dp.value).dp.toPx(),
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

                // Glass Substrate: 100% Clear Translucent Glass with 2% (2.dp) Gaussian Blur
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .blur(radius = 2.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.12f),
                                    Color.White.copy(alpha = 0.06f),
                                    Color.White.copy(alpha = 0.09f)
                                )
                            )
                        )
                )

                // Glass Layer: Optical Specular Reflections & Bevels (Reflecting light from behind)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            drawLiquidGlassOpticReflections(
                                cornerRadius = size.height / 2f,
                                intensity = optics.vibrancy
                            )
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navItems.forEach { item ->
                            val isSelected = when (item.id) {
                                "operations" -> selectedTab in listOf("operations", "users", "complaints", "verification")
                                "payouts" -> selectedTab in listOf("payouts")
                                "system" -> selectedTab in listOf("system", "admins", "lowcode", "settings", "glass_optics", "leaderboard", "tournament_rules")
                                else -> selectedTab == item.id
                            }

                            val animatedWeight by animateFloatAsState(
                                targetValue = if (isSelected) 2.4f else 0.85f,
                                animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
                                label = "navWeight"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(animatedWeight)
                                    .fillMaxHeight()
                                    .padding(vertical = 5.dp, horizontal = 2.dp)
                                    .bounceClick(scaleDown = 0.94f) {
                                        onNavClick(item.id)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Elongated horizontal illuminated glass pill capsule for active tab
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(26.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        accentColor.copy(alpha = 0.32f),
                                                        Color.White.copy(alpha = 0.20f),
                                                        accentColor.copy(alpha = 0.26f)
                                                    )
                                                )
                                            )
                                            .border(
                                                width = 1.2.dp,
                                                brush = Brush.horizontalGradient(
                                                    listOf(
                                                        Color.White.copy(alpha = 0.55f),
                                                        accentColor.copy(alpha = 0.75f),
                                                        Color.White.copy(alpha = 0.40f)
                                                    )
                                                ),
                                                shape = RoundedCornerShape(26.dp)
                                            )
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = if (isSelected) 10.dp else 2.dp)
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.55f),
                                        modifier = Modifier.size(20.dp)
                                    )

                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item.label,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
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
                    // Real-Time Optical Backdrop Sampling (Gaussian Blur, Lens Refraction & Chromatic Aberration)
                    if (backdrop != null) {
                        val orbShape = remember { CircleShape }
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { orbShape },
                                    effects = {
                                        blur(radius = (optics.blurRadius * 32.dp.value * blurProgress).coerceAtLeast(1f).dp.toPx())
                                        colorControls(
                                            brightness = 0.05f * optics.vibrancy,
                                            contrast = 1.05f,
                                            saturation = 1.0f + (0.35f * optics.vibrancy)
                                        )
                                        vibrancy()
                                        if (optics.lensRefractionHeight > 0f) {
                                            lens(
                                                refractionHeight = (optics.lensRefractionHeight * 24.dp.value).dp.toPx(),
                                                refractionAmount = (optics.lensRefractionAmount * 32.dp.value).dp.toPx(),
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

                    // Translucent Clear Glass Substrate with 5.dp Gaussian blur (NO BLACK OR DARK ACCENTS!)
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .blur(radius = 5.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.14f),
                                        Color.White.copy(alpha = 0.07f)
                                    )
                                )
                            )
                    )

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
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        borderBrush = GlassTokens.GlassBorderSubtle,
        onClick = onClick
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
                letterSpacing = 1.sp
            )
            Surface(
                modifier = Modifier.size(28.dp),
                shape = RoundedCornerShape(8.dp),
                color = accentColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            fontSize = 10.sp,
            color = VelorixTextSecondary.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Vercel / Lovable Minimalist Primary Button (Solid High-Contrast)
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
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .bounceClick(scaleDown = 0.95f) { if (enabled) onClick() },
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFE4E4E7))
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp),
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
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = text,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    letterSpacing = 0.3.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Vercel / Lovable Minimalist Secondary Button (Dark Zinc Minimalist)
 */
@Composable
fun GlassOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector? = null,
    borderColor: Color = Color(0xFF27272A)
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .bounceClick(scaleDown = 0.95f) { onClick() },
        color = Color(0xFF141416),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF27272A))
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp),
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
                        tint = Color(0xFFE4E4E7),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    letterSpacing = 0.3.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
