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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardVerifyBorder
import com.example.ui.theme.VelorixAccent
import com.example.ui.theme.VelorixBg
import com.example.ui.theme.VelorixTextPrimary
import com.example.ui.theme.VelorixTextSecondary
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

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
    // Total prize pool baseline
    val total = if (prizePool > 0f) prizePool else 1000f

    // Calculate rank shares respecting user overrides or sensible percentages
    val first = if (firstPlacePrize > 0f) firstPlacePrize else total * 0.50f
    val second = if (secondPlacePrize > 0f) secondPlacePrize else total * 0.25f
    val third = if (thirdPlacePrize > 0f) thirdPlacePrize else total * 0.15f

    // Remainder allocated to bounties or runner-ups so sum equals total exactly
    val allocatedRanks = first + second + third
    val remainder = (total - allocatedRanks).coerceAtLeast(0f)

    val slices = remember(total, first, second, third, perKillPrize, remainder) {
        val list = mutableListOf<PrizeSliceData>()
        
        if (first > 0f) {
            list.add(
                PrizeSliceData(
                    id = "1st",
                    title = "1st Place Champion",
                    amount = first,
                    color = Color(0xFFFFD700),
                    gradientColors = listOf(Color(0xFFFFE082), Color(0xFFFFB300)),
                    icon = Icons.Default.EmojiEvents,
                    description = "Champion jackpot winner"
                )
            )
        }
        if (second > 0f) {
            list.add(
                PrizeSliceData(
                    id = "2nd",
                    title = "2nd Place Runner-Up",
                    amount = second,
                    color = Color(0xFFC0C0C0),
                    gradientColors = listOf(Color(0xFFECEFF1), Color(0xFF90A4AE)),
                    icon = Icons.Default.MilitaryTech,
                    description = "Silver placement reward"
                )
            )
        }
        if (third > 0f) {
            list.add(
                PrizeSliceData(
                    id = "3rd",
                    title = "3rd Place Podium",
                    amount = third,
                    color = Color(0xFFCD7F32),
                    gradientColors = listOf(Color(0xFFFFCC80), Color(0xFFBCAAA4)),
                    icon = Icons.Default.MilitaryTech,
                    description = "Bronze placement reward"
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
                        color = Color(0xFFD0BCFF),
                        gradientColors = listOf(Color(0xFFE8DEF8), Color(0xFF9C27B0)),
                        icon = Icons.Default.GpsFixed,
                        description = "₹${perKillPrize.toInt()} bounty for each verified kill"
                    )
                )
            } else {
                list.add(
                    PrizeSliceData(
                        id = "bonus",
                        title = "Top 4-10 / Bonus",
                        amount = remainder,
                        color = Color(0xFF81C784),
                        gradientColors = listOf(Color(0xFFA5D6A7), Color(0xFF388E3C)),
                        icon = Icons.Default.PieChart,
                        description = "Placement runner-ups & tier pool"
                    )
                )
            }
        }
        list
    }

    val sumSlices = slices.sumOf { it.amount.toDouble() }.toFloat().coerceAtLeast(1f)

    var selectedSliceId by remember { mutableStateOf<String?>(slices.firstOrNull()?.id) }
    val selectedSlice = slices.find { it.id == selectedSliceId } ?: slices.firstOrNull()

    // Animation progress for chart entry
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Chart Canvas Area with Center Donut Hole
        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(12.dp),
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
                                var angle = (atan2(dy, dx) * 180f / PI.toFloat() + 360f) % 360f
                                // Angle starts from top (-90 degrees)
                                var adjustedAngle = (angle + 90f) % 360f
                                
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
                val strokeWidth = size.width * 0.22f
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
                        startAngle = startAngle + 1.5f,
                        sweepAngle = (sweepAngle - 3f).coerceAtLeast(0f),
                        useCenter = false,
                        style = Stroke(width = currentStrokeWidth, cap = StrokeCap.Round)
                    )

                    // Draw subtle separator glow dot at slice apex if selected
                    if (isSelected && animationProgress.value > 0.8f) {
                        val midAngleRad = ((startAngle + sweepAngle / 2f) * PI / 180f).toFloat()
                        val dotX = centerOffset.x + (chartRadius) * cos(midAngleRad)
                        val dotY = centerOffset.y + (chartRadius) * sin(midAngleRad)
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                    }

                    startAngle += rawSweep
                }
            }

            // Donut Center Hub (Interactive Display)
            Surface(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .border(1.dp, CardVerifyBorder, CircleShape)
                    .shadow(8.dp, CircleShape),
                color = VelorixBg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = selectedSlice?.color ?: Color(0xFFFFD700),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "PRIZE POOL",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelorixTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "₹${total.toInt()}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }

        // Active Selected Slice Highlight Card
        if (selectedSlice != null) {
            val percentage = ((selectedSlice.amount / sumSlices) * 100).toInt()
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = selectedSlice.color.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, selectedSlice.color.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(selectedSlice.color.copy(alpha = 0.25f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = selectedSlice.icon,
                                    contentDescription = null,
                                    tint = selectedSlice.color,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = selectedSlice.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = selectedSlice.description,
                                    fontSize = 10.sp,
                                    color = VelorixTextSecondary
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "₹${selectedSlice.amount.toInt()}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = selectedSlice.color
                            )
                            Text(
                                text = "$percentage% share",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VelorixTextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Legend Grid Chips
        Text(
            text = "TAP SLICE OR CHIP TO INSPECT DETAILS",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = VelorixTextSecondary,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

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
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) slice.color.copy(alpha = 0.18f) else VelorixBg,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) slice.color else CardVerifyBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(slice.color, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = slice.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) Color.White else VelorixTextPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "₹${slice.amount.toInt()}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = slice.color
                                    )
                                    Text(
                                        text = "$percentage% share",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = VelorixTextSecondary
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
