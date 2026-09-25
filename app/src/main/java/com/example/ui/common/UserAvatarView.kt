package com.example.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.VelorixAccent
import kotlin.math.abs

// 8 Rich and Vibrant Gradients for Fallback Avatars
private val VibrantAvatarGradients = listOf(
    listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)), // Sunset Flame
    listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)), // Poly Gradient
    listOf(Color(0xFF00c6ff), Color(0xFF0072ff)), // Electric Blue
    listOf(Color(0xFF11998e), Color(0xFF38ef7d)), // Emerald Teal
    listOf(Color(0xFF7F00FF), Color(0xFFE100FF)), // Neon Violet
    listOf(Color(0xFFFF8008), Color(0xFFFFC837)), // Solar Amber
    listOf(Color(0xFF4776E6), Color(0xFF8E54E9)), // Royal Indigo
    listOf(Color(0xFFFF0844), Color(0xFFFFB199))  // Crimson Peach
)

@Composable
fun UserAvatarView(
    avatarUrl: String?,
    name: String,
    size: Dp = 46.dp,
    modifier: Modifier = Modifier,
    isBanned: Boolean = false,
    isSuspended: Boolean = false,
    role: String = "player",
    isActiveRecently: Boolean = false,
    showGlow: Boolean = true
) {
    val cleanUrl = avatarUrl?.trim().takeIf {
        !it.isNullOrBlank() && (it.startsWith("http://") || it.startsWith("https://") || it.startsWith("content://") || it.startsWith("data:"))
    }

    // Gradient selector based on name / ID hash
    val gradientColors = remember(name) {
        val hash = abs(name.hashCode())
        VibrantAvatarGradients[hash % VibrantAvatarGradients.size]
    }

    // Micro-animation: Infinite transition for pulsing glow
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    val borderColor = when {
        isBanned -> Color(0xFFFF334B)
        isSuspended -> Color(0xFFFF9800)
        role.contains("super", ignoreCase = true) -> Color(0xFFFFD700)
        role.contains("admin", ignoreCase = true) -> Color(0xFF00E5FF)
        else -> VelorixAccent
    }

    val initials = remember(name) {
        val trimmed = name.trim()
        val parts = trimmed.split(" ").filter { it.isNotBlank() }
        when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
            trimmed.isNotEmpty() -> trimmed.take(2).uppercase()
            else -> "U"
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Subtle outer pulse glow if active
        if (showGlow && isActiveRecently && !isBanned) {
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(pulseScale)
                    .background(Color(0xFF00E676).copy(alpha = pulseAlpha), CircleShape)
            )
        }

        // Main Avatar Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Brush.linearGradient(gradientColors))
                .border(
                    width = if (size > 50.dp) 2.5.dp else 1.8.dp,
                    color = borderColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (cleanUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(cleanUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile photo of $name",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value * 0.38f).sp,
                    letterSpacing = 0.5.sp
                )
            }

            // Dark ban overlay if banned
            if (isBanned) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Banned",
                        tint = Color(0xFFFF334B),
                        modifier = Modifier.size(size * 0.5f)
                    )
                }
            }
        }

        // Online / Active Indicator Dot or Status Badge
        if (isBanned) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 1.dp, y = 1.dp)
                    .size((size * 0.32f).coerceAtLeast(14.dp))
                    .background(Color(0xFFFF1744), CircleShape)
                    .border(1.dp, Color(0xFF14171E), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        } else if (role.contains("super", ignoreCase = true) || role.contains("admin", ignoreCase = true)) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size((size * 0.35f).coerceAtLeast(15.dp))
                    .background(
                        if (role.contains("super", ignoreCase = true)) Color(0xFFFFB300) else Color(0xFF00B0FF),
                        CircleShape
                    )
                    .border(1.dp, Color(0xFF14171E), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (role.contains("super", ignoreCase = true)) Icons.Default.Star else Icons.Default.Security,
                    contentDescription = "Admin",
                    tint = Color.Black,
                    modifier = Modifier.size(10.dp)
                )
            }
        } else if (isActiveRecently) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 1.dp, y = 1.dp)
                    .size((size * 0.28f).coerceAtLeast(11.dp))
                    .background(Color(0xFF00E676), CircleShape)
                    .border(1.5.dp, Color(0xFF14171E), CircleShape)
            )
        }
    }
}
