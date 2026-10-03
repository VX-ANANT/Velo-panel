package com.example.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.common.HyperPhysics
import com.example.ui.common.bounceClick

/**
 * Modern fluid toggle switch matching iOS / HyperOS design with smooth spring translation,
 * illuminated active track, and custom micro-icons inside the sliding thumb.
 */
@Composable
fun FluidSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF8B5CF6),
    thumbIcon: ImageVector? = null,
    checkedThumbIcon: ImageVector? = null,
    uncheckedThumbIcon: ImageVector? = null
) {
    val trackWidth = 50.dp
    val trackHeight = 30.dp
    val thumbSize = 24.dp
    val padding = 3.dp

    val travelDistance = trackWidth - thumbSize - (padding * 2)

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) travelDistance else 0.dp,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = 500f
        ),
        label = "thumbOffset"
    )

    val trackColor by animateColorAsState(
        targetValue = if (checked) accentColor else Color(0xFF24242E),
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "trackColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (checked) accentColor.copy(alpha = 0.8f) else Color(0xFF383848),
        animationSpec = tween(durationMillis = 240),
        label = "borderColor"
    )

    Surface(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .bounceClick(scaleDown = 0.94f) { onCheckedChange(!checked) },
        shape = RoundedCornerShape(16.dp),
        color = trackColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.CenterStart
        ) {
            Surface(
                modifier = Modifier
                    .size(thumbSize)
                    .offset(x = thumbOffset),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val currentIcon = if (checked) {
                        checkedThumbIcon ?: thumbIcon
                    } else {
                        uncheckedThumbIcon ?: thumbIcon
                    }

                    if (currentIcon != null) {
                        Icon(
                            imageVector = currentIcon,
                            contentDescription = null,
                            tint = if (checked) accentColor else Color(0xFF71717A),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fluid Segmented Control with a sliding background pill indicator driven by spring physics.
 */
@Composable
fun <T> FluidSegmentedControl(
    items: List<T>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF8B5CF6),
    shape: Shape = RoundedCornerShape(14.dp),
    itemContent: @Composable (item: T, isSelected: Boolean) -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp),
        shape = shape,
        color = Color(0xFF16161D),
        border = BorderStroke(1.dp, Color(0xFF262633))
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val totalWidth = maxWidth
            val segmentWidth = totalWidth / items.size.coerceAtLeast(1)
            val indicatorOffset by animateDpAsState(
                targetValue = segmentWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = 450f
                ),
                label = "segmentedIndicator"
            )

            // Sliding active pill indicator
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.28f),
                                accentColor.copy(alpha = 0.16f)
                            )
                        )
                    )
                    .border(1.2.dp, accentColor, RoundedCornerShape(10.dp))
            )

            // Segments clickable row
            Row(modifier = Modifier.fillMaxSize()) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .bounceClick(scaleDown = 0.96f) { onItemSelected(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        itemContent(item, isSelected)
                    }
                }
            }
        }
    }
}

/**
 * Floating Action Pill Dock showing unsaved changes count with Discard & Review buttons.
 * Perfectly replicates the fluid bottom pill from the user's video.
 */
@Composable
fun FluidFloatingReviewBar(
    changesCount: Int,
    onDiscard: () -> Unit,
    onReview: () -> Unit,
    accentColor: Color = Color(0xFF8B5CF6),
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = changesCount > 0,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.76f, stiffness = 420f)
        ) + fadeIn(tween(180)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f)
        ) + fadeOut(tween(160)),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(26.dp),
                    spotColor = accentColor.copy(alpha = 0.35f),
                    ambientColor = Color.Black
                ),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xEB13131A),
            border = BorderStroke(1.2.dp, Color(0xFF2C2C3C))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Change counter badge pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, accentColor)
                    ) {
                        Text(
                            text = "$changesCount",
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = if (changesCount == 1) "1 change" else "$changesCount changes",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Actions: Discard & Review
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDiscard,
                        modifier = Modifier.bounceClick(scaleDown = 0.94f)
                    ) {
                        Text(
                            text = "Discard",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = onReview,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                        modifier = Modifier.bounceClick(scaleDown = 0.94f)
                    ) {
                        Text(
                            text = "Review",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Settings Group Card container matching video's sleek matte glass styling.
 */
@Composable
fun FluidSettingsCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = Color(0xFF13131A),
        border = BorderStroke(1.dp, Color(0xFF22222E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

/**
 * Settings Navigation Row with title, dynamic subtitle pill, and forward chevron arrow.
 */
@Composable
fun FluidSettingsRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = Color(0xFF8B5CF6),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .bounceClick(scaleDown = 0.98f) { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (icon != null) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = iconTint.copy(alpha = 0.15f),
                        border = BorderStroke(0.8.dp, iconTint.copy(alpha = 0.35f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(17.dp))
                        }
                    }
                }
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            color = Color(0xFF8E8EA0),
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFF555566),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}
