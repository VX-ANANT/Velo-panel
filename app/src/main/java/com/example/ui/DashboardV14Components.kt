package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.DepositRequest
import com.example.domain.model.PayoutRequest
import com.example.domain.model.Tournament
import com.example.ui.common.GeminiLogo
import com.example.ui.theme.VelorixEmerald
import com.example.ui.theme.VelorixAmber
import com.example.ui.theme.VelorixYoutubeCrimson
import com.example.ui.theme.VelorixYoutubePinkRed
import com.example.ui.theme.VelorixYoutubeRed
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale

// ============================================================================
// DASHBOARD V14 — MODERN MINIMAL MOBILE BENTO TOKENS (AMOLED + YOUTUBE PINK-RED)
// ============================================================================
internal val V14Bg = Color.Black
internal val V14CardSurface = Color(0xFF0C0C10)
internal val V14CardElevated = Color(0xFF14141B)
internal val V14Border = Color(0xFF1E1E27)
internal val V14TextPrimary = Color(0xFFF5F5F7)
internal val V14TextSecondary = Color(0xFF9A9AA6)
internal val V14TextMuted = Color(0xFF6B6B76)
internal val V14AccentPrimary = VelorixYoutubeRed       // #FF0033
internal val V14AccentBright = VelorixYoutubePinkRed    // #FF2E5B
internal val V14AccentDeep = VelorixYoutubeCrimson      // #C8103E
internal val V14BarMuted = Color(0xFF282834)

/**
 * Staggered spring entrance animation mirroring Dashboard V14's fluid motion choreography.
 */
@Composable
fun V14StaggeredEntrance(
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)) +
            slideInVertically(
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                initialOffsetY = { it / 6 }
            )
    ) {
        content()
    }
}

@Composable
internal fun Modifier.v14PressScale(
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "v14PressScale"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

// ============================================================================
// 1. V14 TOP BAR + REAL-TIME GREETING HEADER
// ============================================================================
@Composable
fun V14DashboardHeader(
    onProfileClick: () -> Unit = {},
    onNavClick: (String) -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
    unreadNotificationCount: Int = 0,
    currentUserEmail: String? = null,
    isSyncing: Boolean = false
) {
    val pulseTransition = rememberInfiniteTransition(label = "v14LivePulse")
    val liveDotAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveDotAlpha"
    )
    val syncRotation by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "syncSpin"
    )

    val greetingText = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val adminDisplayName = remember(currentUserEmail) {
        val raw = currentUserEmail?.substringBefore("@").orEmpty().trim()
        if (raw.isBlank()) "Admin"
        else raw.replaceFirstChar { it.uppercaseChar() }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(V14Bg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Brand & Circular Action Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(V14AccentPrimary, V14AccentBright)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.velorix_logo_transparent),
                        contentDescription = "Velorix Logo",
                        modifier = Modifier.size(22.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Column {
                    Text(
                        text = "Velorix",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = V14TextPrimary,
                        letterSpacing = 0.3.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .graphicsLayer { alpha = liveDotAlpha }
                                .background(
                                    if (isSyncing) V14AccentBright else VelorixEmerald,
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (isSyncing) "Syncing RTDB" else "Firebase Live",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSyncing) V14AccentBright else VelorixEmerald
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(V14CardSurface)
                        .border(0.75.dp, V14Border, CircleShape)
                        .testTag("v14_refresh_button")
                        .clickable { onRefreshClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync Live Data",
                        tint = if (isSyncing) V14AccentBright else V14TextSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(if (isSyncing) syncRotation else 0f)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(V14CardSurface)
                        .border(0.75.dp, V14Border, CircleShape)
                        .testTag("v14_notifications_button")
                        .clickable { onNotificationsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = if (unreadNotificationCount > 0) V14TextPrimary else V14TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    if (unreadNotificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 8.dp)
                                .size(7.dp)
                                .background(V14AccentPrimary, CircleShape)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(V14CardSurface)
                        .border(0.75.dp, V14Border, CircleShape)
                        .testTag("v14_ai_button")
                        .clickable { onNavClick("chatbot") },
                    contentAlignment = Alignment.Center
                ) {
                    GeminiLogo(size = 17.dp)
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(V14AccentPrimary.copy(alpha = 0.16f))
                        .border(0.75.dp, V14AccentBright.copy(alpha = 0.5f), CircleShape)
                        .testTag("v14_profile_button")
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = adminDisplayName.take(1).uppercase(),
                        color = V14AccentBright,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // V14 Minimal Editorial Greeting
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "$greetingText, $adminDisplayName",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = V14TextPrimary,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Live overview of your tournaments, wallet ledger & requests.",
                fontSize = 12.sp,
                color = V14TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================================
// 2. V14 QUICK HUB PILL STRIP (HORIZONTAL NAV PILLS LIKE V14 TOP BAR)
// ============================================================================
@Composable
fun V14QuickHubPillStrip(
    pendingWalletCount: Int,
    openTicketsCount: Int,
    pendingChatReportsCount: Int,
    onOpenHubSection: (IntegrationHubSection) -> Unit,
    onBroadcastClick: () -> Unit
) {
    val items = listOf(
        Triple("Admin Hub", Icons.Default.GridView, 0) to { onOpenHubSection(IntegrationHubSection.TOURNAMENTS_RESULTS) },
        Triple("Wallet & UPI", Icons.Default.AccountBalanceWallet, pendingWalletCount) to { onOpenHubSection(IntegrationHubSection.WALLET_PAYOUTS) },
        Triple("Redeem Vault", Icons.Default.ConfirmationNumber, 0) to { onOpenHubSection(IntegrationHubSection.REDEEM_VAULT) },
        Triple("12-Ch Tickets", Icons.Default.SupportAgent, openTicketsCount) to { onOpenHubSection(IntegrationHubSection.UNIFIED_REQUESTS) },
        Triple("Chat & Players", Icons.Default.Forum, pendingChatReportsCount) to { onOpenHubSection(IntegrationHubSection.USERS_AND_CHAT) },
        Triple("Broadcast", Icons.AutoMirrored.Filled.Send, 0) to onBroadcastClick,
        Triple("Config & DPDP", Icons.Default.Tune, 0) to { onOpenHubSection(IntegrationHubSection.SYSTEM_CONFIG_DPDP) }
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { idx, (meta, action) ->
            val (label, icon, badge) = meta
            val isPrimary = idx == 0
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = 38.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isPrimary) V14AccentPrimary
                        else V14CardSurface
                    )
                    .border(
                        width = 0.75.dp,
                        color = if (isPrimary) V14AccentBright else V14Border,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .v14PressScale(onClick = action)
                    .padding(horizontal = 13.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isPrimary) Color.White else V14TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = label,
                    fontSize = 11.5.sp,
                    fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isPrimary) Color.White else V14TextPrimary,
                    maxLines = 1,
                    softWrap = false
                )
                if (badge > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(V14AccentPrimary.copy(alpha = 0.22f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$badge",
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = V14AccentBright
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 3. V14 PLATFORM LEDGER HERO CARD (LIKE V14 TOTAL BALANCE + TRANSFER/REQUEST + PROGRESS)
// ============================================================================
@Composable
fun V14PlatformLedgerCard(
    totalPlatformBalance: Double,
    totalPaidOut: Double,
    totalPendingOut: Double,
    occupiedSlots: Int,
    totalSlots: Int,
    onCreateMatchClick: () -> Unit,
    onWalletClick: () -> Unit,
    onLedgerClick: () -> Unit
) {
    val animatedBalance by animateFloatAsState(
        targetValue = totalPlatformBalance.toFloat(),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "v14BalanceRoll"
    )
    val fillRatio = remember(occupiedSlots, totalSlots) {
        if (totalSlots > 0) (occupiedSlots.toFloat() / totalSlots.toFloat()).coerceIn(0f, 1f) else 0f
    }
    val animatedFillRatio by animateFloatAsState(
        targetValue = fillRatio,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "v14SlotFill"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(V14CardSurface)
            .border(0.75.dp, V14Border, RoundedCornerShape(22.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Platform Wallet Ledger",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = V14TextSecondary
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(V14CardElevated)
                    .border(0.5.dp, V14Border, RoundedCornerShape(12.dp))
                    .clickable { onLedgerClick() }
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "INR / VT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = V14TextPrimary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open Ledger",
                    tint = V14AccentBright,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // Big animated balance
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "₹${String.format(Locale.US, "%,.2f", animatedBalance)}",
                fontSize = 31.sp,
                fontWeight = FontWeight.ExtraBold,
                color = V14TextPrimary,
                letterSpacing = (-0.8).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Paid Out ₹${String.format(Locale.US, "%,.0f", totalPaidOut)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = VelorixEmerald
                )
                Text(text = "•", fontSize = 11.sp, color = V14TextMuted)
                Text(
                    text = "Pending ₹${String.format(Locale.US, "%,.0f", totalPendingOut)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (totalPendingOut > 0.0) VelorixAmber else V14TextSecondary
                )
            }
        }

        // V14 Dual Pill CTA Row (Solid YouTube Pink-Red + Outlined Dark Pill)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(V14AccentPrimary, V14AccentBright)
                        )
                    )
                    .testTag("v14_new_match_button")
                    .v14PressScale(onClick = onCreateMatchClick),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = "New Match",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(V14CardElevated)
                    .border(0.75.dp, V14Border, RoundedCornerShape(22.dp))
                    .testTag("v14_wallet_payouts_button")
                    .v14PressScale(onClick = onWalletClick),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = V14TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Wallet & UPI",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = V14TextPrimary
                    )
                }
            }
        }

        // V14 Capacity Progress Bar (Real Live Slot Occupancy)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Tournament Slot Occupancy",
                    fontSize = 11.sp,
                    color = V14TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$occupiedSlots / $totalSlots slots",
                    fontSize = 11.sp,
                    color = V14TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(V14BarMuted)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = animatedFillRatio)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(V14AccentDeep, V14AccentPrimary, V14AccentBright)
                            )
                        )
                )
            }
        }
    }
}

// ============================================================================
// 4. V14 2x2 ANIMATED BENTO KPI GRID (TOP-LEFT YOUTUBE PINK-RED GRADIENT HERO CARD)
// ============================================================================
@Composable
fun V14BentoKpiGrid(
    activeMatchesCount: Int,
    totalMatchesCount: Int,
    registeredPlayersCount: Int,
    activePlayersCount: Int,
    actionQueueCount: Int,
    pendingPayoutsCount: Int,
    openTicketsCount: Int,
    totalPrizePool: Double,
    completedMatchesCount: Int,
    onMatchesClick: () -> Unit,
    onPlayersClick: () -> Unit,
    onActionQueueClick: () -> Unit,
    onRevenueClick: () -> Unit
) {
    val animActiveMatches by animateIntAsState(
        targetValue = activeMatchesCount,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "animActiveMatches"
    )
    val animPlayers by animateIntAsState(
        targetValue = registeredPlayersCount,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "animPlayers"
    )
    val animQueue by animateIntAsState(
        targetValue = actionQueueCount,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "animQueue"
    )
    val animPrizePool by animateFloatAsState(
        targetValue = totalPrizePool.toFloat(),
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "animPrizePool"
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // HERO CARD 1: YouTube Pinkish-Red Gradient Card (Matches V14's signature hero KPI tile)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(132.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                V14AccentBright,
                                V14AccentPrimary,
                                V14AccentDeep
                            )
                        )
                    )
                    .testTag("v14_kpi_matches")
                    .v14PressScale(onClick = onMatchesClick)
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Matches",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "$animActiveMatches",
                        fontSize = 27.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-0.6).sp
                    )
                    Text(
                        text = "$totalMatchesCount total synced",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // CARD 2: Registered Players
            V14DarkBentoTile(
                modifier = Modifier.weight(1f),
                title = "Players",
                value = "$animPlayers",
                subtitle = "$activePlayersCount active accounts",
                icon = Icons.Default.Group,
                accentTint = V14AccentBright,
                tag = "v14_kpi_players",
                onClick = onPlayersClick
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // CARD 3: Action Queue
            V14DarkBentoTile(
                modifier = Modifier.weight(1f),
                title = "Action Queue",
                value = "$animQueue",
                subtitle = "$pendingPayoutsCount payouts • $openTicketsCount tickets",
                icon = Icons.Default.NotificationsActive,
                accentTint = if (actionQueueCount > 0) VelorixAmber else V14TextSecondary,
                tag = "v14_kpi_queue",
                onClick = onActionQueueClick
            )

            // CARD 4: Prize Pools
            V14DarkBentoTile(
                modifier = Modifier.weight(1f),
                title = "Prize Pools",
                value = "₹${String.format(Locale.US, "%,.0f", animPrizePool)}",
                subtitle = "$completedMatchesCount completed",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                accentTint = VelorixEmerald,
                tag = "v14_kpi_prizes",
                onClick = onRevenueClick
            )
        }
    }
}

@Composable
private fun V14DarkBentoTile(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentTint: Color,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(132.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(V14CardSurface)
            .border(0.75.dp, V14Border, RoundedCornerShape(20.dp))
            .testTag(tag)
            .v14PressScale(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = V14TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentTint,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = value,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                color = V14TextPrimary,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = V14TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================================
// 5. V14 LIVE ANALYTICS DUAL-BAR CHART (100% REAL FIREBASE DATA ONLY, ZERO MOCK)
// ============================================================================
private data class V14BarPoint(
    val label: String,
    val primaryVal: Float,
    val secondaryVal: Float
)

@Composable
fun V14LiveVolumeChartCard(
    tournaments: List<Tournament>,
    deposits: List<DepositRequest>,
    withdrawals: List<PayoutRequest>,
    onInspectAnalyticsClick: () -> Unit
) {
    // Build real data series strictly from live Firebase records
    val (chartTitle, primaryLegend, secondaryLegend, points) = remember(tournaments, deposits, withdrawals) {
        val tourneyPoints = tournaments
            .take(6)
            .reversed()
            .map { t ->
                val collected = (t.registeredPlayers * t.entryFee).toFloat().coerceAtLeast(0f)
                val prize = t.prizePool.toFloat().coerceAtLeast(0f)
                val shortLabel = t.title.trim().ifBlank { t.id }.take(6)
                V14BarPoint(shortLabel, collected, prize)
            }
        val hasTourneyVolume = tourneyPoints.any { it.primaryVal > 0f || it.secondaryVal > 0f }

        if (hasTourneyVolume) {
            Quadruple("Live Match Volume", "Entry Collected", "Prize Pool", tourneyPoints)
        } else {
            // Fallback to real wallet deposits vs real withdrawals if no matches have monetary figures yet
            val depSum = deposits.sumOf { it.amount }.toFloat()
            val wdrSum = withdrawals.filter { !it.isDeposit }.sumOf { it.vtAmount }.toFloat()
            val walletPoints = if (depSum > 0f || wdrSum > 0f) {
                listOf(
                    V14BarPoint("Dep", depSum, 0f),
                    V14BarPoint("Wdr", 0f, wdrSum)
                )
            } else {
                emptyList()
            }
            Quadruple("Live Ledger Volume", "Deposits", "Withdrawals", walletPoints)
        }
    }

    var playChartAnim by remember { mutableStateOf(false) }
    LaunchedEffect(points) {
        playChartAnim = false
        delay(80)
        playChartAnim = true
    }

    val barProgress by animateFloatAsState(
        targetValue = if (playChartAnim) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 210f),
        label = "v14ChartBars"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(V14CardSurface)
            .border(0.75.dp, V14Border, RoundedCornerShape(22.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = chartTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = V14TextPrimary
                )
                Text(
                    text = "Real-time Firebase metrics",
                    fontSize = 11.sp,
                    color = V14TextMuted
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(V14AccentPrimary.copy(alpha = 0.14f))
                    .clickable { onInspectAnalyticsClick() }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Analytics",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = V14AccentBright
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = V14AccentBright,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // Legend pills
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(modifier = Modifier.size(8.dp).background(V14AccentPrimary, CircleShape))
                Text(text = primaryLegend, fontSize = 11.sp, color = V14TextSecondary)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFF525260), CircleShape))
                Text(text = secondaryLegend, fontSize = 11.sp, color = V14TextSecondary)
            }
        }

        if (points.isEmpty()) {
            // Strict No-Mock Empty Baseline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF07070A))
                    .border(0.5.dp, V14Border, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = V14TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "No live match or ledger volume yet",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = V14TextSecondary
                    )
                    Text(
                        text = "Real bars appear automatically as Firebase records arrive.",
                        fontSize = 10.5.sp,
                        color = V14TextMuted
                    )
                }
            }
        } else {
            val maxVal = remember(points) {
                points.maxOfOrNull { maxOf(it.primaryVal, it.secondaryVal) }?.coerceAtLeast(1f) ?: 1f
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(126.dp)
                ) {
                    val gridColor = Color.White.copy(alpha = 0.06f)
                    for (i in 0..3) {
                        val y = size.height * (i / 3f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                    }

                    val count = points.size
                    val slotWidth = size.width / count.coerceAtLeast(1)
                    val barW = (slotWidth * 0.24f).coerceIn(10f, 24f)
                    val gap = 6f
                    val radius = CornerRadius(8f, 8f)

                    points.forEachIndexed { idx, pt ->
                        val centerX = slotWidth * idx + slotWidth / 2f

                        val h1 = (pt.primaryVal / maxVal).coerceIn(0f, 1f) * (size.height - 10f) * barProgress
                        if (h1 > 2f) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(V14AccentBright, V14AccentPrimary, V14AccentDeep),
                                    startY = size.height - h1,
                                    endY = size.height
                                ),
                                topLeft = Offset(centerX - barW - gap / 2f, size.height - h1),
                                size = Size(barW, h1),
                                cornerRadius = radius
                            )
                        }

                        val h2 = (pt.secondaryVal / maxVal).coerceIn(0f, 1f) * (size.height - 10f) * barProgress
                        if (h2 > 2f) {
                            drawRoundRect(
                                color = Color(0xFF454554),
                                topLeft = Offset(centerX + gap / 2f, size.height - h2),
                                size = Size(barW, h2),
                                cornerRadius = radius
                            )
                        }
                    }
                }

                // X-axis real labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    points.forEach { pt ->
                        Text(
                            text = pt.label,
                            fontSize = 10.sp,
                            color = V14TextMuted,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// ============================================================================
// 6. V14 MATCH ACTIVITY ROW (CLEAN BENTO CARD WITH NON-OVERLAPPING PILL ACTIONS)
// ============================================================================
@Composable
fun V14MatchActivityRow(
    tournament: Tournament,
    onOpenClick: () -> Unit,
    onWinnersClick: () -> Unit,
    onBracketClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val statusUpper = tournament.status.trim().uppercase().ifBlank { "UPCOMING" }
    val statusColor = when (statusUpper) {
        "LIVE", "ONGOING", "ACTIVE" -> VelorixEmerald
        "COMPLETED" -> V14TextSecondary
        "CANCELLED" -> Color(0xFFF87171)
        else -> V14AccentBright
    }
    val fillFraction = if (tournament.maxPlayers > 0) {
        (tournament.registeredPlayers.toFloat() / tournament.maxPlayers.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val animFill by animateFloatAsState(
        targetValue = fillFraction,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow),
        label = "matchRowFill"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(V14CardElevated)
            .border(0.75.dp, V14Border, RoundedCornerShape(18.dp))
            .v14PressScale(onClick = onOpenClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Full-width Match Title + Compact Right-Aligned Status Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = tournament.title.ifBlank { "Untitled Tournament" },
                fontSize = 14.5.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
                color = V14TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(statusColor.copy(alpha = 0.14f))
                    .border(0.5.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(statusColor, CircleShape)
                )
                Text(
                    text = statusUpper,
                    fontSize = 9.5.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Row 2: Clean Format / Map Metadata + Highlighted Prize Amount
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = tournament.format.ifBlank { "SOLO" }.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = V14TextPrimary.copy(alpha = 0.86f),
                    maxLines = 1
                )
                Text(
                    text = "•",
                    fontSize = 11.sp,
                    color = V14TextMuted
                )
                Text(
                    text = tournament.map.ifBlank { "Bermuda" },
                    fontSize = 11.5.sp,
                    color = V14TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "Prize ₹${String.format(Locale.US, "%,d", tournament.prizePool.toInt())}",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = VelorixEmerald,
                maxLines = 1,
                softWrap = false
            )
        }

        // Row 3: Animated YouTube Pink-Red Slot Occupancy Bar + Count
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(V14BarMuted)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animFill)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(V14AccentDeep, V14AccentPrimary, V14AccentBright)
                            )
                        )
                )
            }
            Text(
                text = "${tournament.registeredPlayers}/${tournament.maxPlayers} slots",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = V14TextSecondary,
                maxLines = 1,
                softWrap = false
            )
        }

        val roomIdVal = tournament.roomDetails?.roomId.orEmpty()
        val roomPassVal = tournament.roomDetails?.roomPassword.orEmpty()
        if (roomIdVal.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(VelorixEmerald.copy(alpha = 0.08f))
                    .border(0.5.dp, VelorixEmerald.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = null,
                    tint = VelorixEmerald,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Room $roomIdVal  •  Pass ${roomPassVal.ifBlank { "None" }}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VelorixEmerald,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Hairline Separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(V14Border)
        )

        // Row 4: Equal-Width Sleek Action Pill Bar (Zero Overlap Guaranteed)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            V14MatchActionPill(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.EmojiEvents,
                label = "Winners",
                tint = VelorixAmber,
                background = VelorixAmber.copy(alpha = 0.10f),
                borderColor = VelorixAmber.copy(alpha = 0.28f),
                testTag = "v14_match_winners_${tournament.id}",
                onClick = onWinnersClick
            )
            V14MatchActionPill(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AccountTree,
                label = "Bracket",
                tint = V14TextPrimary,
                background = Color.White.copy(alpha = 0.05f),
                borderColor = V14Border,
                testTag = "v14_match_bracket_${tournament.id}",
                onClick = onBracketClick
            )
            V14MatchActionPill(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Edit,
                label = "Edit",
                tint = V14AccentBright,
                background = V14AccentPrimary.copy(alpha = 0.14f),
                borderColor = V14AccentBright.copy(alpha = 0.34f),
                testTag = "v14_match_edit_${tournament.id}",
                onClick = onEditClick
            )
        }
    }
}

@Composable
private fun V14MatchActionPill(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    tint: Color,
    background: Color,
    borderColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .border(0.6.dp, borderColor, RoundedCornerShape(10.dp))
            .testTag(testTag)
            .v14PressScale(onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
