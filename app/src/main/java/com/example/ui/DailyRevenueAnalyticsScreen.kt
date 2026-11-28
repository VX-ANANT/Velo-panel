package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CashoutRequest
import com.example.domain.model.PayoutRequest
import com.example.domain.model.Tournament
import com.example.domain.model.UserProfile
import com.example.ui.common.GlassCard
import com.example.ui.common.GlassTokens
import com.example.ui.common.bounceClick
import com.example.ui.theme.VelorixAccent
import com.example.ui.theme.VelorixTextPrimary
import com.example.ui.theme.VelorixTextSecondary
import java.text.SimpleDateFormat
import java.util.*

enum class RevenueTimeframe(val label: String, val days: Int) {
    TODAY("Today", 1),
    YESTERDAY("Yesterday", 1),
    THIS_WEEK("7 Days", 7),
    THIS_MONTH("30 Days", 30),
    ALL_TIME("All Time", 3650)
}

@Composable
fun DailyRevenueAnalyticsScreen(
    tournaments: List<Tournament>,
    users: List<UserProfile>,
    payouts: List<PayoutRequest>,
    onBack: () -> Unit,
    embeddedInTab: Boolean = false,
    onTournamentClick: (String) -> Unit = {},
    onBroadcastRoomCredentials: ((tournamentId: String, roomId: String, roomPass: String) -> Unit)? = null
) {
    if (!embeddedInTab) {
        BackHandler { onBack() }
    }

    val context = LocalContext.current
    var selectedTimeframe by remember { mutableStateOf(RevenueTimeframe.ALL_TIME) }
    var searchQuery by remember { mutableStateOf("") }
    var showRoomDispatchDialog by remember { mutableStateOf<Tournament?>(null) }
    var filterCategory by remember { mutableStateOf("ALL") }

    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val startOfToday = cal.timeInMillis
    val startOfYesterday = startOfToday - (24 * 60 * 60 * 1000L)
    val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000L)
    val thirtyDaysAgo = now - (30 * 24 * 60 * 60 * 1000L)

    // Filter tournaments based on timeframe and Free Fire mode category
    val filteredTournaments = remember(tournaments, selectedTimeframe, searchQuery, filterCategory) {
        tournaments.filter { t ->
            val matchTime = try {
                t.parsedStartsAtMillis ?: 0L
            } catch (_: Exception) { 0L }

            val inTimeframe = when (selectedTimeframe) {
                RevenueTimeframe.TODAY -> matchTime >= startOfToday || matchTime == 0L
                RevenueTimeframe.YESTERDAY -> matchTime in startOfYesterday until startOfToday
                RevenueTimeframe.THIS_WEEK -> matchTime >= sevenDaysAgo || matchTime == 0L
                RevenueTimeframe.THIS_MONTH -> matchTime >= thirtyDaysAgo || matchTime == 0L
                RevenueTimeframe.ALL_TIME -> true
            }

            val matchesQuery = searchQuery.isBlank() ||
                t.title.contains(searchQuery, ignoreCase = true) ||
                t.id.contains(searchQuery, ignoreCase = true) ||
                t.category.contains(searchQuery, ignoreCase = true)

            val modeCombined = "${t.category} ${t.format} ${t.title}".lowercase()
            val matchesCategory = when (filterCategory) {
                "ALL" -> true
                "BR" -> modeCombined.contains("br") || modeCombined.contains("battle royale") || modeCombined.contains("solo") || modeCombined.contains("duo") || modeCombined.contains("squad")
                "CS" -> modeCombined.contains("cs") || modeCombined.contains("clash")
                "Lone Wolf" -> modeCombined.contains("lone") || modeCombined.contains("1v1")
                else -> true
            }

            inTimeframe && matchesQuery && matchesCategory
        }
    }

    // Calculations
    val totalGrossRevenue = remember(filteredTournaments) {
        filteredTournaments.sumOf { (it.entryFee.toDouble() * it.registeredPlayers) }
    }
    val totalPrizePoolAllocated = remember(filteredTournaments) {
        filteredTournaments.sumOf { it.prizePool.toDouble() }
    }
    val netAdminOperatingMargin = remember(totalGrossRevenue, totalPrizePoolAllocated) {
        totalGrossRevenue - totalPrizePoolAllocated
    }
    val totalRegistrations = remember(filteredTournaments) {
        filteredTournaments.sumOf { it.registeredPlayers }
    }
    val completedMatchesCount = remember(filteredTournaments) {
        filteredTournaments.count { it.status.equals("COMPLETED", ignoreCase = true) }
    }
    val liveMatchesCount = remember(filteredTournaments) {
        filteredTournaments.count { it.status.equals("LIVE", ignoreCase = true) }
    }

    // Filtered cashouts
    val approvedPayoutsSum = remember(payouts, selectedTimeframe) {
        payouts.filter { it.status.equals("approved", ignoreCase = true) }.sumOf { it.vtAmount }
    }
    val pendingPayoutsSum = remember(payouts) {
        payouts.filter { it.status.equals("pending", ignoreCase = true) }.sumOf { it.vtAmount }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(V14Bg)
                    .then(if (embeddedInTab) Modifier else Modifier.statusBarsPadding())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!embeddedInTab) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(V14CardSurface)
                                    .border(1.dp, V14Border, CircleShape)
                                    .clickable(onClick = onBack),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Column {
                            Text(
                                text = "Earnings & Revenue",
                                color = V14TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "FREE FIRE FINANCIAL LEDGER",
                                color = V14TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(V14CardSurface)
                            .border(1.dp, V14Border, CircleShape)
                            .clickable {
                                val report = buildFinancialSummary(
                                    timeframe = selectedTimeframe.label,
                                    gross = totalGrossRevenue,
                                    netMargin = netAdminOperatingMargin,
                                    prizePool = totalPrizePoolAllocated,
                                    registrations = totalRegistrations,
                                    matches = filteredTournaments.size,
                                    approvedPayouts = approvedPayoutsSum,
                                    pendingPayouts = pendingPayoutsSum
                                )
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Financial Audit", report))
                                Toast.makeText(context, "Financial summary copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share Report",
                            tint = V14AccentBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = V14Border)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Timeframe Underline Selector
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        RevenueTimeframe.values().forEach { tf ->
                            val isSelected = selectedTimeframe == tf
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTimeframe = tf }
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = tf.label,
                                    color = if (isSelected) Color.White else VelorixTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .height(if (isSelected) 1.5.dp else 0.5.dp)
                                        .background(if (isSelected) Color(0xFFFF2E5B) else Color.Transparent)
                                )
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                }
            }

            // Primary Profit & Revenue Cards (Solid #0C0C0E System Ledger — Zero Gradients, Zero Mint/Purple)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0C0C0E),
                    border = BorderStroke(0.5.dp, Color(0xFF222226))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Gross Revenue Column
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFFA1A1AA), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("GROSS COLLECTION", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF71717A), letterSpacing = 0.6.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "₹${"%.0f".format(totalGrossRevenue)}",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFF4F4F5)
                            )
                            Text(
                                text = "$totalRegistrations entry fees",
                                fontSize = 11.sp,
                                color = Color(0xFFA1A1AA)
                            )
                        }

                        VerticalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                        // Net Margin / House Profit Column
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color(0xFFFF2E5B), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("NET ADMIN MARGIN", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF71717A), letterSpacing = 0.6.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val isProfit = netAdminOperatingMargin >= 0
                            Text(
                                text = "${if (isProfit) "+" else ""}₹${"%.0f".format(netAdminOperatingMargin)}",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isProfit) Color(0xFF34D399) else Color(0xFFFF4D6D)
                            )
                            Text(
                                text = if (totalGrossRevenue > 0) "${"%.1f".format((netAdminOperatingMargin / totalGrossRevenue) * 100)}% Margin" else "0% Margin",
                                fontSize = 11.sp,
                                color = if (isProfit) Color(0xFF34D399) else Color(0xFFFF4D6D)
                            )
                        }
                    }
                }
            }

            // Secondary Stats Bar
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0C0C0E),
                    border = BorderStroke(0.5.dp, Color(0xFF222226))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatColumn(label = "Matches", value = "${filteredTournaments.size}", subValue = "$liveMatchesCount Live")
                        Box(modifier = Modifier.width(0.5.dp).height(30.dp).background(Color.White.copy(alpha = 0.08f)))
                        StatColumn(label = "Prize Pools", value = "₹${"%.0f".format(totalPrizePoolAllocated)}", subValue = "$completedMatchesCount Paid")
                        Box(modifier = Modifier.width(0.5.dp).height(30.dp).background(Color.White.copy(alpha = 0.08f)))
                        StatColumn(label = "Pending Payout", value = "₹${"%.0f".format(pendingPayoutsSum)}", subValue = "Cashout Queue", isWarning = pendingPayoutsSum > 0)
                    }
                }
            }

            // Section Header & Search
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MATCH FINANCIAL AUDIT (${filteredTournaments.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF71717A),
                        letterSpacing = 1.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("ALL", "BR", "CS", "Lone Wolf").forEach { cat ->
                            val isSel = filterCategory == cat
                            Column(
                                modifier = Modifier
                                    .clickable { filterCategory = cat }
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSel) Color.White else Color(0xFF71717A),
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .height(if (isSel) 1.5.dp else 0.dp)
                                        .background(if (isSel) Color(0xFFFF2E5B) else Color.Transparent)
                                )
                            }
                        }
                    }
                }
            }

            // Tournaments List
            if (filteredTournaments.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.EventBusy, contentDescription = null, tint = VelorixTextSecondary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No tournaments found in this timeframe", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Change the filter above or create a new tournament.", color = VelorixTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                items(filteredTournaments, key = { it.id }) { tourney ->
                    val gross = tourney.entryFee.toDouble() * tourney.registeredPlayers
                    val profit = gross - tourney.prizePool.toDouble()
                    val hasCredentials = tourney.roomDetails?.roomId?.isNotBlank() == true

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        brush = Brush.linearGradient(listOf(Color(0xFF0E0E12), Color(0xFF09090C))),
                        borderBrush = GlassTokens.GlassBorderSubtle,
                        onClick = { onTournamentClick(tourney.id) }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(
                                                when (tourney.status.uppercase()) {
                                                    "LIVE" -> Color(0xFF34D399)
                                                    "COMPLETED" -> V14TextSecondary
                                                    "CANCELLED" -> V14AccentBright
                                                    else -> Color(0xFFFBBF24)
                                                },
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tourney.status.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (tourney.status.uppercase()) {
                                            "LIVE" -> Color(0xFF34D399)
                                            "COMPLETED" -> V14TextSecondary
                                            "CANCELLED" -> V14AccentBright
                                            else -> Color(0xFFFBBF24)
                                        }
                                    )
                                }

                                Text(
                                    text = "${tourney.game} • ${tourney.map}",
                                    fontSize = 11.sp,
                                    color = VelorixTextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = tourney.title,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Financial breakdown row divided by hairlines (no inner box)
                            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Entry / Players", fontSize = 9.5.sp, color = VelorixTextSecondary)
                                    Text("₹${tourney.entryFee.toInt()} × ${tourney.registeredPlayers}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Prize Pool", fontSize = 9.5.sp, color = VelorixTextSecondary)
                                    Text("₹${tourney.prizePool.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Net Profit", fontSize = 9.5.sp, color = VelorixTextSecondary)
                                    Text(
                                        text = "${if (profit >= 0) "+" else ""}₹${profit.toInt()}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (profit >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                            // Action strip divided by vertical hairline (no inner bordered boxes)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showRoomDispatchDialog = tourney }
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.VpnKey,
                                        contentDescription = null,
                                        tint = if (hasCredentials) Color(0xFF10B981) else VelorixAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (hasCredentials) "ROOM: ${tourney.roomDetails?.roomId}" else "SET ROOM ID & PASS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasCredentials) Color(0xFF10B981) else VelorixAccent
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .width(0.5.dp)
                                        .height(16.dp)
                                        .background(Color.White.copy(alpha = 0.14f))
                                )

                                Box(
                                    modifier = Modifier
                                        .clickable { onTournamentClick(tourney.id) }
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("MANAGE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(140.dp)) }
        }
    }

    // Room Dispatch Dialog
    val currentRoomTourney = showRoomDispatchDialog
    if (currentRoomTourney != null) {
        var roomIdInput by remember { mutableStateOf(currentRoomTourney.roomDetails?.roomId ?: "") }
        var roomPassInput by remember { mutableStateOf(currentRoomTourney.roomDetails?.password ?: "") }

        AlertDialog(
            onDismissRequest = { showRoomDispatchDialog = null },
            title = {
                Text(
                    text = "Room ID & Password Dispatcher",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Broadcast credentials to ${currentRoomTourney.registeredPlayers} registered players in ${currentRoomTourney.title}:",
                        color = VelorixTextSecondary,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = roomIdInput,
                        onValueChange = { roomIdInput = it },
                        label = { Text("Custom Room ID", color = V14TextMuted) },
                        placeholder = { Text("e.g. 5829103", color = V14TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = V14TextPrimary,
                            unfocusedTextColor = V14TextPrimary,
                            focusedContainerColor = Color(0xFF08080B),
                            unfocusedContainerColor = Color(0xFF08080B),
                            focusedBorderColor = V14AccentBright,
                            unfocusedBorderColor = V14Border,
                            cursorColor = V14AccentBright
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = roomPassInput,
                        onValueChange = { roomPassInput = it },
                        label = { Text("Room Password", color = V14TextMuted) },
                        placeholder = { Text("e.g. 1234", color = V14TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = V14TextPrimary,
                            unfocusedTextColor = V14TextPrimary,
                            focusedContainerColor = Color(0xFF08080B),
                            unfocusedContainerColor = Color(0xFF08080B),
                            focusedBorderColor = V14AccentBright,
                            unfocusedBorderColor = V14Border,
                            cursorColor = V14AccentBright
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBroadcastRoomCredentials?.invoke(currentRoomTourney.id, roomIdInput.trim(), roomPassInput.trim())
                        Toast.makeText(context, "Room credentials broadcasted to players!", Toast.LENGTH_SHORT).show()
                        showRoomDispatchDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = V14AccentPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Broadcast to Players", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRoomDispatchDialog = null }) {
                    Text("Cancel", color = V14TextSecondary)
                }
            },
            containerColor = V14CardSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    subValue: String,
    isWarning: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = VelorixTextSecondary, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isWarning) Color(0xFFF59E0B) else Color.White)
        Text(subValue, fontSize = 9.5.sp, color = if (isWarning) Color(0xFFF59E0B).copy(alpha = 0.8f) else Color(0xFF10B981))
    }
}

private fun isSelectedGame(current: String, target: String): Boolean = current == target

private fun buildFinancialSummary(
    timeframe: String,
    gross: Double,
    netMargin: Double,
    prizePool: Double,
    registrations: Int,
    matches: Int,
    approvedPayouts: Double,
    pendingPayouts: Double
): String {
    return """
        === VELORIX ADMIN FINANCIAL SUMMARY ===
        Timeframe: $timeframe
        Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}
        
        Gross Collections: ₹${"%.2f".format(gross)}
        Net Admin Margin: ₹${"%.2f".format(netMargin)}
        Prize Pool Allocated: ₹${"%.2f".format(prizePool)}
        Total Registered Slots: $registrations
        Total Matches: $matches
        
        Approved Cashouts: ₹${"%.2f".format(approvedPayouts)}
        Pending Cashouts: ₹${"%.2f".format(pendingPayouts)}
        ========================================
    """.trimIndent()
}
