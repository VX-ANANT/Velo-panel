package com.example.ui.common

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VelorixAccent
import com.example.ui.theme.VelorixTextPrimary
import com.example.ui.theme.VelorixTextSecondary

/**
 * Untitled UI System Design Kit & Vector Icons
 * Styled in the exact aesthetics of Untitled UI (untitledui.com):
 * - Refined 1px subtle hairline borders
 * - Slate/Obsidian high-contrast dark palette (#09090B, #121216, #18181B)
 * - Micro-icon containers with soft accent alpha tints
 * - Structured grid card layouts with tight vertical rhythm and zero wasted whitespace
 */
object UntitledIcons {
    // 1. Trophy / Tournament Icon (Untitled UI Trophy-01)
    val Trophy: ImageVector by lazy {
        ImageVector.Builder(
            name = "UntitledTrophy",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 9f)
                curveTo(6f, 9f, 6f, 4f, 12f, 4f)
                curveTo(18f, 4f, 18f, 9f, 18f, 9f)
                curveTo(18f, 14f, 12f, 15f, 12f, 15f)
                curveTo(12f, 15f, 6f, 14f, 6f, 9f)
                close()
                moveTo(6f, 6f)
                lineTo(3f, 6f)
                curveTo(2.45f, 6f, 2f, 6.45f, 2f, 7f)
                curveTo(2f, 9.5f, 3.5f, 11f, 6f, 11f)
                moveTo(18f, 6f)
                lineTo(21f, 6f)
                curveTo(21.55f, 6f, 22f, 6.45f, 22f, 7f)
                curveTo(22f, 9.5f, 20.5f, 11f, 18f, 11f)
                moveTo(12f, 15f)
                lineTo(12f, 19f)
                moveTo(8f, 21f)
                lineTo(16f, 21f)
            }
        }.build()
    }

    // 2. Users / Players Group Icon (Untitled UI Users-01)
    val Users: ImageVector by lazy {
        ImageVector.Builder(
            name = "UntitledUsers",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Central User
                moveTo(16f, 21f)
                verticalLineTo(19f)
                curveTo(16f, 16.79f, 14.21f, 15f, 12f, 15f)
                curveTo(9.79f, 15f, 8f, 16.79f, 8f, 19f)
                verticalLineTo(21f)
                moveTo(12f, 11f)
                curveTo(13.66f, 11f, 15f, 9.66f, 15f, 8f)
                curveTo(15f, 6.34f, 13.66f, 5f, 12f, 5f)
                curveTo(10.34f, 5f, 9f, 6.34f, 9f, 8f)
                curveTo(9f, 9.66f, 10.34f, 11f, 12f, 11f)
                close()
                // Left user wing
                moveTo(3f, 21f)
                verticalLineTo(19f)
                curveTo(3f, 17.34f, 4f, 15.93f, 5.5f, 15.34f)
                moveTo(7.5f, 5.34f)
                curveTo(6.64f, 5.86f, 6f, 6.84f, 6f, 8f)
                curveTo(6f, 9.16f, 6.64f, 10.14f, 7.5f, 10.66f)
                // Right user wing
                moveTo(21f, 21f)
                verticalLineTo(19f)
                curveTo(21f, 17.34f, 20f, 15.93f, 18.5f, 15.34f)
                moveTo(16.5f, 5.34f)
                curveTo(17.36f, 5.86f, 18f, 6.84f, 18f, 8f)
                curveTo(18f, 9.16f, 17.36f, 10.14f, 16.5f, 10.66f)
            }
        }.build()
    }

    // 3. Shield Check (Untitled UI Shield-Check)
    val ShieldCheck: ImageVector by lazy {
        ImageVector.Builder(
            name = "UntitledShieldCheck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 22f)
                curveTo(12f, 22f, 20f, 18f, 20f, 12f)
                verticalLineTo(5f)
                lineTo(12f, 2f)
                lineTo(4f, 5f)
                verticalLineTo(12f)
                curveTo(4f, 18f, 12f, 22f, 12f, 22f)
                close()
                moveTo(9f, 12f)
                lineTo(11f, 14f)
                lineTo(15f, 10f)
            }
        }.build()
    }

    // 4. Coins / Financials Stack (Untitled UI Coins-Stacked-01)
    val Coins: ImageVector by lazy {
        ImageVector.Builder(
            name = "UntitledCoins",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 6f)
                curveTo(16.42f, 6f, 20f, 4.66f, 20f, 3f)
                curveTo(20f, 1.34f, 16.42f, 0f, 12f, 0f)
                curveTo(7.58f, 0f, 4f, 1.34f, 4f, 3f)
                curveTo(4f, 4.66f, 7.58f, 6f, 12f, 6f)
                moveTo(4f, 3f)
                verticalLineTo(9f)
                curveTo(4f, 10.66f, 7.58f, 12f, 12f, 12f)
                curveTo(16.42f, 12f, 20f, 10.66f, 20f, 9f)
                verticalLineTo(3f)
                moveTo(4f, 9f)
                verticalLineTo(15f)
                curveTo(4f, 16.66f, 7.58f, 18f, 12f, 18f)
                curveTo(16.42f, 18f, 20f, 16.66f, 20f, 15f)
                verticalLineTo(9f)
                moveTo(4f, 15f)
                verticalLineTo(21f)
                curveTo(4f, 22.66f, 7.58f, 24f, 12f, 24f)
                curveTo(16.42f, 24f, 20f, 22.66f, 20f, 21f)
                verticalLineTo(15f)
            }
        }.build()
    }

    // 5. Zap Fast / Instant Action (Untitled UI Zap-Fast)
    val Zap: ImageVector by lazy {
        ImageVector.Builder(
            name = "UntitledZap",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13f, 2f)
                lineTo(3f, 14f)
                horizontalLineTo(12f)
                lineTo(11f, 22f)
                lineTo(21f, 10f)
                horizontalLineTo(12f)
                lineTo(13f, 2f)
                close()
            }
        }.build()
    }

    // 6. Git Branch / Bracket Engine (Untitled UI Git-Branch-01)
    val Bracket: ImageVector by lazy {
        ImageVector.Builder(
            name = "UntitledBracket",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 3f)
                verticalLineTo(15f)
                moveTo(18f, 9f)
                curveTo(18f, 6.79f, 16.21f, 5f, 14f, 5f)
                horizontalLineTo(6f)
                moveTo(6f, 18f)
                curveTo(7.66f, 18f, 9f, 19.34f, 9f, 21f)
                curveTo(9f, 22.66f, 7.66f, 24f, 6f, 24f)
                curveTo(4.34f, 24f, 3f, 22.66f, 3f, 21f)
                curveTo(3f, 19.34f, 4.34f, 18f, 6f, 18f)
                close()
                moveTo(6f, 0f)
                curveTo(7.66f, 0f, 9f, 1.34f, 9f, 3f)
                curveTo(9f, 4.66f, 7.66f, 6f, 6f, 6f)
                curveTo(4.34f, 6f, 3f, 4.66f, 3f, 3f)
                curveTo(3f, 1.34f, 4.34f, 0f, 6f, 0f)
                close()
                moveTo(18f, 6f)
                curveTo(19.66f, 6f, 21f, 7.34f, 21f, 9f)
                curveTo(21f, 10.66f, 19.66f, 12f, 18f, 12f)
                curveTo(16.34f, 12f, 15f, 10.66f, 15f, 9f)
                curveTo(15f, 7.34f, 16.34f, 6f, 18f, 6f)
                close()
            }
        }.build()
    }

    // 7. Sliders / Rules Config (Untitled UI Sliders-02)
    val Sliders: ImageVector by lazy {
        ImageVector.Builder(
            name = "UntitledSliders",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 21f)
                verticalLineTo(14f)
                moveTo(4f, 10f)
                verticalLineTo(3f)
                moveTo(12f, 21f)
                verticalLineTo(12f)
                moveTo(12f, 8f)
                verticalLineTo(3f)
                moveTo(20f, 21f)
                verticalLineTo(16f)
                moveTo(20f, 12f)
                verticalLineTo(3f)
                moveTo(1f, 14f)
                horizontalLineTo(7f)
                moveTo(9f, 8f)
                horizontalLineTo(15f)
                moveTo(17f, 16f)
                horizontalLineTo(23f)
            }
        }.build()
    }

    // 8. Key / Token PIN (Untitled UI Key-01)
    val Key: ImageVector by lazy {
        ImageVector.Builder(
            name = "UntitledKey",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 2f)
                lineTo(10.5f, 12.5f)
                moveTo(15.5f, 7.5f)
                lineTo(18.5f, 10.5f)
                moveTo(13.5f, 9.5f)
                lineTo(15.5f, 11.5f)
                moveTo(11.5f, 11.5f)
                lineTo(13.5f, 13.5f)
                moveTo(10.5f, 12.5f)
                curveTo(9.5f, 11.5f, 8f, 11f, 6.5f, 11f)
                curveTo(3.46f, 11f, 1f, 13.46f, 1f, 16.5f)
                curveTo(1f, 19.54f, 3.46f, 22f, 6.5f, 22f)
                curveTo(9.54f, 22f, 12f, 19.54f, 12f, 16.5f)
                curveTo(12f, 15f, 11.5f, 13.5f, 10.5f, 12.5f)
                close()
            }
        }.build()
    }

    // 9. Bar Chart Square (Untitled UI Bar-Chart-Square-02)
    val ChartSquare: ImageVector by lazy {
        ImageVector.Builder(
            name = "UntitledChartSquare",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 3f)
                horizontalLineTo(21f)
                verticalLineTo(21f)
                horizontalLineTo(3f)
                close()
                moveTo(18f, 17f)
                verticalLineTo(9f)
                moveTo(12f, 17f)
                verticalLineTo(13f)
                moveTo(6f, 17f)
                verticalLineTo(7f)
            }
        }.build()
    }

    // 10. Sparkles / AI / Optimization (Untitled UI Sparkles)
    val Sparkles: ImageVector by lazy {
        ImageVector.Builder(
            name = "UntitledSparkles",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2f)
                lineTo(14.5f, 9.5f)
                lineTo(22f, 12f)
                lineTo(14.5f, 14.5f)
                lineTo(12f, 22f)
                lineTo(9.5f, 14.5f)
                lineTo(2f, 12f)
                lineTo(9.5f, 9.5f)
                close()
            }
        }.build()
    }
}

/**
 * Untitled UI Card Container
 * Precision container with dark obsidian fill, subtle 1px border, and clean padding.
 */
@Composable
fun UntitledCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp),
    backgroundColor: Color = Color(0xFF0D0D10),
    borderColor: Color = Color(0xFF222228),
    borderWidth: Dp = 1.dp,
    padding: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.bounceClick(scaleDown = 0.98f) { onClick() }
    } else Modifier

    Box(
        modifier = modifier
            .then(clickModifier)
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape)
            .padding(padding),
        content = content
    )
}

/**
 * Untitled UI Clean Metric Tile
 * Used in 2-column or 4-column metric overviews with micro-icons and trend indicators.
 */
@Composable
fun UntitledMetricTile(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    badgeColor: Color = iconTint,
    onClick: (() -> Unit)? = null
) {
    UntitledCard(
        modifier = modifier,
        onClick = onClick,
        padding = 12.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        modifier = Modifier.size(26.dp),
                        shape = RoundedCornerShape(7.dp),
                        color = iconTint.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, iconTint.copy(alpha = 0.25f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        color = VelorixTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (badgeText != null) {
                    UntitledBadge(
                        text = badgeText,
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                color = VelorixTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Untitled UI Action Grid Tile
 * Square / Rectangle interactive tool card for tournament operations.
 */
@Composable
fun UntitledActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    counterBadge: String? = null,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    val borderColor = if (highlight) iconTint.copy(alpha = 0.45f) else Color(0xFF222228)
    val bgColor = if (highlight) iconTint.copy(alpha = 0.05f) else Color(0xFF0E0E12)

    UntitledCard(
        modifier = modifier,
        backgroundColor = bgColor,
        borderColor = borderColor,
        onClick = onClick,
        padding = 12.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = iconTint.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, iconTint.copy(alpha = 0.3f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (counterBadge != null) {
                    UntitledBadge(
                        text = counterBadge,
                        color = iconTint
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                color = VelorixTextSecondary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Untitled UI Refined Badge Pill
 */
@Composable
fun UntitledBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    hasDot: Boolean = true
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (hasDot) {
                Box(
                    modifier = Modifier
                        .size(4.5.dp)
                        .background(color, CircleShape)
                )
            }
            Text(
                text = text,
                color = color,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
