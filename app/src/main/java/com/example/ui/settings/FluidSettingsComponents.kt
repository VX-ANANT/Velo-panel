package com.example.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.common.bounceClick

/**
 * Modern fluid toggle switch with smooth spring translation on pure AMOLED black.
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
    val trackWidth = 48.dp
    val trackHeight = 26.dp
    val thumbSize = 20.dp
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
        targetValue = if (checked) accentColor else Color(0xFF1A1A22),
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "trackColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (checked) accentColor else Color.White.copy(alpha = 0.14f),
        animationSpec = tween(durationMillis = 200),
        label = "borderColor"
    )

    Surface(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .bounceClick(scaleDown = 0.94f) { onCheckedChange(!checked) },
        shape = RoundedCornerShape(14.dp),
        color = trackColor,
        border = BorderStroke(0.5.dp, borderColor)
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
                color = if (checked) Color.Black else Color.White
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
                            tint = if (checked) accentColor else Color(0xFF52525B),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Flat AMOLED underline mode selector replacing boxed segmented controls (zero box-in-box).
 */
@Composable
fun <T> FluidSegmentedControl(
    items: List<T>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF8B5CF6),
    shape: Shape = RoundedCornerShape(0.dp),
    itemContent: @Composable (item: T, isSelected: Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
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

            // Sliding active underline bar (zero box-in-box)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = indicatorOffset)
                    .width(segmentWidth)
                    .padding(horizontal = 12.dp)
                    .height(2.dp)
                    .background(accentColor)
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.1f))
    }
}

/**
 * Flat AMOLED bottom action bar showing unsaved changes count with Discard & Review actions.
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            HorizontalDivider(thickness = 0.5.dp, color = accentColor.copy(alpha = 0.4f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$changesCount",
                        color = accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (changesCount == 1) "unsaved change" else "unsaved changes",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

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
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = onReview,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.bounceClick(scaleDown = 0.94f)
                    ) {
                        Text(
                            text = "Review",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Flat AMOLED Settings Section container divided by a 0.5.dp bottom hairline (zero box-in-box).
 */
@Composable
fun FluidSettingsCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            content = content
        )
        HorizontalDivider(
            thickness = 0.5.dp,
            color = Color.White.copy(alpha = 0.08f)
        )
    }
}

/**
 * Flat Settings Navigation Row with title, subtitle, and forward chevron arrow (no nested icon box).
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
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
            modifier = Modifier.size(12.dp)
        )
    }
}
