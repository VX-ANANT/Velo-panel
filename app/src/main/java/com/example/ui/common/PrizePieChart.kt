package com.example.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private val ChartSurface = Color(0xFF0C0C10)
private val ChartElevated = Color(0xFF14141B)
private val ChartBorder = Color(0xFF1E1E27)
private val ChartTextPrimary = Color(0xFFF5F5F7)
private val ChartTextSecondary = Color(0xFF9A9AA6)
private val ChartTextMuted = Color(0xFF6B6B76)

data class PrizeSliceData(
    val id: String,
    val title: String,
    val amount: Float,
    val color: Color,
    val gradientColors: List<Color>,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String
)

@Composable
fun PrizeDistributionPieChart(
    prizePool: Float,
    firstPlacePrize: Float,
    secondPlacePrize: Float,
    thirdPlacePrize: Float,
    perKillPrize: Float,
    maxPlayers: Int = 48,
    modifier: Modifier = Modifier
) {
    val total = if (prizePool > 0f) prizePool else 1000f

    val first = if (firstPlacePrize > 0f) firstPlacePrize else total * 0.50f
    val second = if (secondPlacePrize > 0f) secondPlacePrize else total * 0.25f
    val third = if (thirdPlacePrize > 0f) thirdPlacePrize else total * 0.15f

    val allocatedRanks = first + second + third
    val remainder = (total - allocatedRanks).coerceAtLeast(0f)

    val slices = remember(total, first, second, third, perKillPrize, remainder) {
        val list = mutableListOf<PrizeSliceData>()

        if (first > 0f) {
            list.add(
                PrizeSliceData(
                    id = "1st",
                    title = "1st Place",
                    amount = first,
                    color = Color(0xFFFBBF24),
                    gradientColors = listOf(Color(0xFFFDE68A), Color(0xFFF59E0B)),
                    icon = Icons.Default.EmojiEvents,
                    description = "Champion jackpot reward"
                )
            )
        }
        if (second > 0f) {
            list.add(
                PrizeSliceData(
                    id = "2nd",
                    title = "2nd Place",
                    amount = second,
                    color = Color(0xFFCBD5E1),
                    gradientColors = listOf(Color(0xFFF1F5F9), Color(0xFF94A3B8)),
                    icon = Icons.Default.MilitaryTech,
                    description = "Runner-up placement reward"
                )
            )
        }
        if (third > 0f) {
            list.add(
                PrizeSliceData(
                    id = "3rd",
                    title = "3rd Place",
                    amount = third,
                    color = Color(0xFFFB923C),
                    gradientColors = listOf(Color(0xFFFDBA74), Color(0xFFEA580C)),
                    icon = Icons.Default.MilitaryTech,
                    description = "Podium finisher reward"
                )
            )
        }
        if (remainder > 0f) {
            if (perKillPrize > 0f) {
                list.add(
                    PrizeSliceData(
                        id = "kills",
                        title = "Kill Bounties",
                        amount = remainder,
                        color = Color(0xFFFF2E5B),
                        gradientColors = listOf(Color(0xFFFF6B8B), Color(0xFFFF0033)),
                        icon = Icons.Default.GpsFixed,
                        description = "₹${perKillPrize.toInt()} bounty per verified kill"
                    )
                )
            } else {
                list.add(
                    PrizeSliceData(
                        id = "bonus",
                        title = "Top 4-10 Pool",
                        amount = remainder,
                        color = Color(0xFF34D399),
                        gradientColors = listOf(Color(0xFF6EE7B7), Color(0xFF10B981)),
                        icon = Icons.Default.PieChart,
                        description = "Placement runner-ups & tier pool"
                    )
                )
            }
        }
        list
    }

    val sumSlices = slices.sumOf { it.amount.toDouble() }.toFloat().coerceAtLeast(1f)

    var selectedSliceId by remember(slices) { mutableStateOf<String?>(slices.firstOrNull()?.id) }
    val selectedSlice = slices.find { it.id == selectedSliceId } ?: slices.firstOrNull()

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 820, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Sleek V14 Donut Chart
        Box(
            modifier = Modifier
                .size(214.dp)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(slices, sumSlices) {
                        detectTapGestures { tapOffset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = tapOffset.x - center.x
                            val dy = tapOffset.y - center.y
                            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                            val outerRadius = size.width / 2f
                            val innerRadius = outerRadius * 0.55f

                            if (distance in innerRadius..outerRadius) {
                                val angle = (atan2(dy, dx) * 180f / PI.toFloat() + 360f) % 360f
                                val adjustedAngle = (angle + 90f) % 360f

                                var currentAngle = 0f
                                for (slice in slices) {
                                    val sweep = (slice.amount / sumSlices) * 360f
                                    if (adjustedAngle in currentAngle..(currentAngle + sweep)) {
                                        selectedSliceId = slice.id
                                        break
                                    }
                                    currentAngle += sweep
                                }
                            }
                        }
                    }
            ) {
                val strokeWidth = size.width * 0.19f
                val chartRadius = (size.width - strokeWidth) / 2f
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                var startAngle = -90f

                slices.forEach { slice ->
                    val rawSweep = (slice.amount / sumSlices) * 360f
                    val sweepAngle = rawSweep * animationProgress.value
                    val isSelected = slice.id == selectedSliceId

                    val currentStrokeWidth = if (isSelected) strokeWidth * 1.15f else strokeWidth
                    val drawBrush = Brush.sweepGradient(
                        colors = slice.gradientColors,
                        center = centerOffset
                    )

                    drawArc(
                        brush = drawBrush,
                        startAngle = startAngle + 2f,
                        sweepAngle = (sweepAngle - 4f).coerceAtLeast(0f),
                        useCenter = false,
                        style = Stroke(width = currentStrokeWidth, cap = StrokeCap.Round)
                    )

                    if (isSelected && animationProgress.value > 0.8f) {
                        val midAngleRad = ((startAngle + sweepAngle / 2f) * PI / 180f).toFloat()
                        val dotX = centerOffset.x + chartRadius * cos(midAngleRad)
                        val dotY = centerOffset.y + chartRadius * sin(midAngleRad)
                        drawCircle(
                            color = Color.White,
                            radius = 3.5.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                    }

                    startAngle += rawSweep
                }
            }

            // Center Donut Hub
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .clip(CircleShape)
                    .background(ChartSurface)
                    .border(0.75.dp, ChartBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = selectedSlice?.color ?: Color(0xFFFBBF24),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "TOTAL POOL",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChartTextMuted,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "₹${String.format(Locale.US, "%,d", total.toInt())}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = ChartTextPrimary
                    )
                }
            }
        }

        // Active Selected Slice Inspector Row
        if (selectedSlice != null) {
            val percentage = ((selectedSlice.amount / sumSlices) * 100).toInt()
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ChartElevated)
                        .border(0.75.dp, selectedSlice.color.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(selectedSlice.color.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = selectedSlice.icon,
                                contentDescription = null,
                                tint = selectedSlice.color,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = selectedSlice.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChartTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = selectedSlice.description,
                                fontSize = 11.sp,
                                color = ChartTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "₹${String.format(Locale.US, "%,d", selectedSlice.amount.toInt())}",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = selectedSlice.color
                        )
                        Text(
                            text = "$percentage% share",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = ChartTextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive 2x2 Legend Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slices.chunked(2).forEach { rowSlices ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowSlices.forEach { slice ->
                        val isSelected = slice.id == selectedSliceId
                        val percentage = ((slice.amount / sumSlices) * 100).toInt()
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedSliceId = slice.id },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) slice.color.copy(alpha = 0.12f) else ChartElevated,
                            border = BorderStroke(
                                0.75.dp,
                                if (isSelected) slice.color.copy(alpha = 0.5f) else ChartBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(slice.color, CircleShape)
                                    )
                                    Text(
                                        text = slice.title,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) ChartTextPrimary else ChartTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "₹${String.format(Locale.US, "%,d", slice.amount.toInt())}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = slice.color
                                    )
                                    Text(
                                        text = "$percentage%",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = ChartTextMuted
                                    )
                                }
                            }
                        }
                    }
                    if (rowSlices.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
