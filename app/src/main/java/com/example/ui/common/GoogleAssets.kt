package com.example.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Official Google 4-Color G Icon component.
 * Uses the exact Google Favicon 2025 asset uploaded by user.
 */
@Composable
fun GoogleGIcon(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Image(
        painter = painterResource(id = R.drawable.google_favicon),
        contentDescription = "Google",
        modifier = modifier.size(size)
    )
}

/**
 * Official Google Full Wordmark Logo component.
 * Uses the exact Google 2015 Wordmark asset uploaded by user.
 */
@Composable
fun GoogleWordmarkLogo(
    modifier: Modifier = Modifier,
    height: Dp = 20.dp
) {
    val width = height * (272f / 92f)
    Image(
        painter = painterResource(id = R.drawable.google_full_logo),
        contentDescription = "Google",
        modifier = modifier.size(width = width, height = height)
    )
}

/**
 * Official Google Gemini AI Sparkle/Diamond 4-Point Logo.
 */
@Composable
fun GeminiLogo(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Image(
        painter = painterResource(id = R.drawable.ic_gemini_logo),
        contentDescription = "Google Gemini AI",
        modifier = modifier.size(size)
    )
}

/**
 * Official Firebase Logo Component.
 */
@Composable
fun FirebaseLogo(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Image(
        painter = painterResource(id = R.drawable.ic_firebase_logo),
        contentDescription = "Firebase",
        modifier = modifier.size(size)
    )
}

/**
 * Official BGMI Crest / Esports Logo.
 */
@Composable
fun BgmiLogo(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Image(
        painter = painterResource(id = R.drawable.ic_bgmi_logo),
        contentDescription = "BGMI",
        modifier = modifier.size(size)
    )
}

/**
 * Official Free Fire Flame / Phoenix Wings Logo.
 */
@Composable
fun FreeFireLogo(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Image(
        painter = painterResource(id = R.drawable.ic_freefire_logo),
        contentDescription = "Free Fire",
        modifier = modifier.size(size)
    )
}

/**
 * Dynamic Game Logo Badge (Switches between Free Fire and BGMI vector assets).
 */
@Composable
fun GameLogoBadge(
    gameName: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    if (gameName.contains("Free", ignoreCase = true) || gameName.contains("FF", ignoreCase = true)) {
        FreeFireLogo(modifier = modifier, size = size)
    } else {
        BgmiLogo(modifier = modifier, size = size)
    }
}

/**
 * Official & High-Polish Google Sign In Button for Admin Auth.
 * Includes smooth micro-interaction scaling, authentic Google typography and branding.
 */
@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String = "Continue with Google",
    subtitle: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "google_btn_scale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color(0xFF4285F4).copy(alpha = 0.25f)
            ),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF131722),
        border = BorderStroke(1.2.dp, Color(0xFF2A3447))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF181F2F),
                            Color(0xFF131722),
                            Color(0xFF161E2E)
                        )
                    )
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Official Google G with white circular capsule container
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        GoogleGIcon(size = 18.dp)
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

