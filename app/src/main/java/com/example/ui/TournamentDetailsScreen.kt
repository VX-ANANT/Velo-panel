package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.domain.model.MatchProofSubmission
import com.example.domain.model.PlayerRegistration
import com.example.domain.model.Tournament
import com.example.domain.model.UserProfile
import com.example.ui.common.FrostedGlassAlertDialog
import com.example.ui.common.PrizeDistributionPieChart
import com.example.ui.theme.*
import com.example.util.TournamentImageUploader
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailsScreen(
    tournament: Tournament,
    @Suppress("UNUSED_PARAMETER") currentUserProfile: UserProfile? = null,
    matchProofs: List<MatchProofSubmission> = emptyList(),
    onNavigateBack: () -> Unit,
    onEditTournament: (String) -> Unit = {},
    onEditRules: (String) -> Unit = {},
    onCancelTournament: (String, String) -> Unit = { _, _ -> },
    onDeleteTournament: (String) -> Unit = {},
    onViewBracket: (String) -> Unit = {},
    onPublishRoomCredentials: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onApproveMatchProof: (String, Double, Int) -> Unit = { _, _, _ -> },
    onRejectMatchProof: (String, String) -> Unit = { _, _ -> },
    onAddParticipant: ((String, PlayerRegistration) -> Unit)? = null,
    onUpdateTournament: ((Tournament) -> Unit)? = null,
    viewModel: com.example.ui.viewmodel.TournamentDashboardViewModel? = null
) {
    val context = LocalContext.current

    var selectedSection by remember { mutableIntStateOf(0) } // 0=Overview, 1=Roster, 2=Prizes, 3=Rules
    var showOverflowMenu by remember { mutableStateOf(false) }

    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReasonInput by remember { mutableStateOf("Insufficient player registrations - Match cancelled manually.") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var showRoomBroadcastDialog by remember { mutableStateOf(false) }
    var showInspectProofsDialog by remember { mutableStateOf(false) }
    var showAddParticipantDialog by remember { mutableStateOf(false) }
    var showEditPrizeDialog by remember { mutableStateOf(false) }
    var showLobbyChatModal by remember { mutableStateOf(false) }
    var showDeclareStandingsModal by remember { mutableStateOf(false) }

    // Dynamic Prize Editing states
    var editPrizePoolInput by remember(tournament.prizePool) { mutableStateOf(tournament.prizePool.toInt().toString()) }
    var editFirstPlaceInput by remember(tournament.firstPlacePrize, tournament.prizePool) {
        val defaultFirst = if (tournament.firstPlacePrize > 0f) tournament.firstPlacePrize else tournament.prizePool * 0.50f
        mutableStateOf(defaultFirst.toInt().toString())
    }
    var editSecondPlaceInput by remember(tournament.secondPlacePrize, tournament.prizePool) {
        val defaultSecond = if (tournament.secondPlacePrize > 0f) tournament.secondPlacePrize else tournament.prizePool * 0.25f
        mutableStateOf(defaultSecond.toInt().toString())
    }
    var editThirdPlaceInput by remember(tournament.thirdPlacePrize, tournament.prizePool) {
        val defaultThird = if (tournament.thirdPlacePrize > 0f) tournament.thirdPlacePrize else tournament.prizePool * 0.15f
        mutableStateOf(defaultThird.toInt().toString())
    }
    var editPerKillInput by remember(tournament.perKillPrize) {
        val defaultKill = if (tournament.perKillPrize > 0f) tournament.perKillPrize else 20f
        mutableStateOf(defaultKill.toInt().toString())
    }

    // Participant manual add states (Admin VIP slot assignment)
    var manualPlayerIgn by remember { mutableStateOf("") }
    var manualPlayerUid by remember { mutableStateOf("") }
    var manualPlayerAge by remember { mutableStateOf("18") }
    var manualPlayerIsMinor by remember { mutableStateOf(false) }

    var roomIdInput by remember(tournament.roomDetails?.roomId) { mutableStateOf(tournament.roomDetails?.roomId ?: "") }
    var roomPassInput by remember(tournament.roomDetails?.roomPassword) { mutableStateOf(tournament.roomDetails?.roomPassword ?: "") }
    var bypass5MinCheck by remember { mutableStateOf(false) }

    val statusUpper = tournament.status.trim().uppercase().ifBlank { "UPCOMING" }
    val isCancelled = statusUpper == "CANCELLED"
    val hasRoomCredentials = tournament.roomDetails?.roomId?.isNotBlank() == true
    val pendingProofsCount = remember(matchProofs) { matchProofs.count { it.status.equals("pending", ignoreCase = true) } }
    val isMoneyMatch = (tournament.entryFee > 0f) || (tournament.prizePool > 0.0)

    val statusColor = when (statusUpper) {
        "LIVE", "ONGOING", "ACTIVE" -> VelorixEmerald
        "COMPLETED", "ENDED" -> V14TextSecondary
        "CANCELLED" -> Color(0xFFF87171)
        else -> V14AccentBright
    }

    val startsAtMillis = remember(tournament.startsAt) { tournament.parsedStartsAtMillis ?: 0L }
    val minutesUntilStart = remember(startsAtMillis) {
        if (startsAtMillis > 0L) ((startsAtMillis - System.currentTimeMillis()) / 60000L).coerceAtLeast(0L) else 0L
    }
    val isWithin5Minutes = minutesUntilStart <= 5L

    val slotProgress = if (tournament.maxPlayers > 0) {
        (tournament.registeredPlayers.toFloat() / tournament.maxPlayers.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val animSlotProgress by animateFloatAsState(
        targetValue = slotProgress,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow),
        label = "detailSlotFill"
    )

    BackHandler { onNavigateBack() }

    Scaffold(
        containerColor = V14Bg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(V14Bg)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(end = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(V14CardSurface)
                                .border(0.75.dp, V14Border, CircleShape)
                                .testTag("tournament_details_back_button")
                                .v14PressScale(onClick = onNavigateBack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = V14TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Match Console",
                                color = V14TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                                Text(
                                    text = "$statusUpper · ${tournament.map.ifBlank { "Bermuda" }} · ${tournament.format.ifBlank { "SOLO" }}",
                                    color = V14TextSecondary,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                                .testTag("tournament_details_edit_button")
                                .v14PressScale { onEditTournament(tournament.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Tournament",
                                tint = V14TextPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Box {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(V14CardSurface)
                                    .border(0.75.dp, V14Border, CircleShape)
                                    .testTag("tournament_details_menu_button")
                                    .v14PressScale { showOverflowMenu = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Match Options",
                                    tint = V14TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false },
                                containerColor = V14CardElevated,
                                border = BorderStroke(0.75.dp, V14Border)
                            ) {
                                if (viewModel != null) {
                                    DropdownMenuItem(
                                        text = { Text("Lobby Chat Moderation", color = V14TextPrimary, fontSize = 13.sp) },
                                        leadingIcon = { Icon(Icons.Default.Forum, contentDescription = null, tint = VelorixEmerald, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showLobbyChatModal = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Declare Standings & Payouts", color = V14TextPrimary, fontSize = 13.sp) },
                                        leadingIcon = { Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = VelorixAmber, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showDeclareStandingsModal = true
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Rules & Loadout Manager", color = V14TextPrimary, fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.Gavel, contentDescription = null, tint = V14TextSecondary, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onEditRules(tournament.id)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("View Brackets", color = V14TextPrimary, fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.AccountTree, contentDescription = null, tint = V14TextSecondary, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onViewBracket(tournament.id)
                                    }
                                )
                                HorizontalDivider(thickness = 0.5.dp, color = V14Border)
                                if (!isCancelled) {
                                    DropdownMenuItem(
                                        text = { Text("Cancel Match & Refund", color = VelorixAmber, fontSize = 13.sp) },
                                        leadingIcon = { Icon(Icons.Default.Cancel, contentDescription = null, tint = VelorixAmber, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showCancelDialog = true
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Delete Permanently", color = Color(0xFFF87171), fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = V14Border)
            }
        },
        bottomBar = {
            // V14 Dual Pill Action Dock (Matches Home V14DualPillCta)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(V14Bg)
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(thickness = 0.5.dp, color = V14Border)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCancelled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(Color(0xFF230E12))
                                .border(0.75.dp, Color(0xFFF87171).copy(alpha = 0.35f), RoundedCornerShape(23.dp)),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Match Cancelled · Refunds Issued",
                                color = Color(0xFFFCA5A5),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Primary V14 Gradient Pill: Set / Edit Room Pass
                        Row(
                            modifier = Modifier
                                .weight(1.25f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(
                                    if (hasRoomCredentials) {
                                        Brush.horizontalGradient(listOf(Color(0xFF10B981), VelorixEmerald))
                                    } else {
                                        Brush.horizontalGradient(listOf(V14AccentPrimary, V14AccentBright))
                                    }
                                )
                                .testTag("tournament_set_room_button")
                                .v14PressScale { showRoomBroadcastDialog = true }
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = if (hasRoomCredentials) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hasRoomCredentials) "Edit Room Pass" else "Set Room Pass",
                                color = if (hasRoomCredentials) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Secondary V14 Dark Pill: Declare Results (NO loud yellow solid fill!)
                        if (viewModel != null) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(23.dp))
                                    .background(V14CardElevated)
                                    .border(0.75.dp, V14Border, RoundedCornerShape(23.dp))
                                    .testTag("tournament_results_button")
                                    .v14PressScale { showDeclareStandingsModal = true }
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = VelorixAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Results",
                                    color = V14TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // Compact Circular Brackets Action
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(V14CardElevated)
                                .border(0.75.dp, V14Border, CircleShape)
                                .testTag("tournament_brackets_button")
                                .v14PressScale { onViewBracket(tournament.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountTree,
                                contentDescription = "View Brackets",
                                tint = V14TextPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(V14Bg),
            contentPadding = PaddingValues(top = 14.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cancellation Alert Bento Card
            if (isCancelled) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1F0C10))
                            .border(0.75.dp, Color(0xFFF87171).copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(17.dp))
                            Text(
                                text = "TOURNAMENT CANCELLED",
                                color = Color(0xFFFCA5A5),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp
                            )
                        }
                        Text(
                            text = tournament.cancellationReason.ifBlank { "Insufficient player registrations. Entry fees refunded to player wallets." },
                            color = V14TextPrimary.copy(alpha = 0.9f),
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // =================================================================
            // 1. V14 HERO BENTO CARD
            // =================================================================
            item {
                V14StaggeredEntrance(delayMillis = 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(V14CardSurface)
                            .border(0.75.dp, V14Border, RoundedCornerShape(22.dp))
                    ) {
                        // Dark-scrimmed Hero Banner (eliminates harsh white rectangle cutouts)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(166.dp)
                                .background(V14CardElevated)
                        ) {
                            if (tournament.bannerUrl.isNotBlank()) {
                                AsyncImage(
                                    model = TournamentImageUploader.resolveBannerModel(tournament.bannerUrl),
                                    contentDescription = tournament.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF240912), V14CardSurface, V14CardElevated)
                                            )
                                        )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0.0f to Color(0xB3000000),
                                            0.40f to Color(0x75000000),
                                            0.76f to Color(0xEB0C0C10),
                                            1.0f to V14CardSurface
                                        )
                                    )
                            )

                            // Top Status & Policy Capsules
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xCC0C0C10))
                                        .border(0.6.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(50))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                                    Text(
                                        text = statusUpper,
                                        color = statusColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xCC0C0C10))
                                        .border(
                                            0.6.dp,
                                            if (isMoneyMatch) VelorixAmber.copy(alpha = 0.42f) else VelorixEmerald.copy(alpha = 0.42f),
                                            RoundedCornerShape(50)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isMoneyMatch) "18+ CASH POLICY" else "OPEN TRAINING",
                                        color = if (isMoneyMatch) VelorixAmber else VelorixEmerald,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            // Bottom High-Contrast Format/Map Pills + Match Title
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(V14AccentPrimary.copy(alpha = 0.22f))
                                            .border(0.6.dp, V14AccentBright.copy(alpha = 0.5f), RoundedCornerShape(50))
                                            .padding(horizontal = 9.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = tournament.format.ifBlank { "SOLO" }.uppercase(),
                                            color = Color(0xFFFFB3C1),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(V14CardElevated)
                                            .border(0.6.dp, V14Border, RoundedCornerShape(50))
                                            .padding(horizontal = 9.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = tournament.map.ifBlank { "Bermuda" },
                                            color = V14TextPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = tournament.title.ifBlank { "Untitled Tournament" },
                                    color = V14TextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 25.sp,
                                    letterSpacing = (-0.4).sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Description + 4-Column Telemetry + V14 Animated Slot Bar
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = tournament.description.ifBlank { "Official Velorix Free Fire competitive tournament." },
                                color = V14TextSecondary,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )

                            HorizontalDivider(thickness = 0.5.dp, color = V14Border)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                V14TelemetryCell(
                                    label = "PRIZE POOL",
                                    value = "₹${String.format(Locale.US, "%,d", tournament.prizePool.toInt())}",
                                    valueColor = VelorixEmerald
                                )
                                V14TelemetryCell(
                                    label = "ENTRY",
                                    value = if (tournament.entryFee > 0f) "₹${tournament.entryFee.toInt()}" else "FREE",
                                    valueColor = V14TextPrimary
                                )
                                V14TelemetryCell(
                                    label = "SLOTS",
                                    value = "${tournament.registeredPlayers}/${tournament.maxPlayers}",
                                    valueColor = V14TextPrimary
                                )
                                V14TelemetryCell(
                                    label = "SCHEDULE",
                                    value = tournament.startsAt?.take(10) ?: "TBA",
                                    valueColor = V14TextSecondary
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Slot Capacity", fontSize = 11.5.sp, color = V14TextSecondary)
                                    Text(
                                        text = "${tournament.registeredPlayers}/${tournament.maxPlayers} (${(slotProgress * 100).toInt()}%)",
                                        fontSize = 11.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (slotProgress >= 1f) VelorixEmerald else V14AccentBright,
                                        fontWeight = FontWeight.Bold
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
                                            .fillMaxWidth(animSlotProgress)
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
                }
            }

            // =================================================================
            // 2. CLEAN UNDERLINE SECTION SWITCHER (Overview | Roster | Prizes | Rules)
            // =================================================================
            item {
                val tabs = listOf(
                    "Overview",
                    "Roster (${tournament.registeredPlayers})",
                    "Prizes",
                    "Rules"
                )
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        tabs.forEachIndexed { index, label ->
                            val isSelected = selectedSection == index
                            Column(
                                modifier = Modifier
                                    .clickable { selectedSection = index }
                                    .padding(top = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) V14TextPrimary else V14TextMuted
                                )
                                Spacer(modifier = Modifier.height(9.dp))
                                Box(
                                    modifier = Modifier
                                        .width(30.dp)
                                        .height(2.dp)
                                        .background(
                                            if (isSelected) V14AccentPrimary else Color.Transparent,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = V14Border)
                }
            }

            // =================================================================
            // TAB 0: OVERVIEW — 100% ADMIN OPERATIONS (ZERO USER-PANEL JOIN UI!)
            // =================================================================
            if (selectedSection == 0) {
                // Room Credentials Bento Card
                item {
                    V14BentoSectionCard(
                        icon = Icons.Default.Key,
                        iconTint = if (hasRoomCredentials) VelorixEmerald else V14AccentBright,
                        title = "Room Credentials",
                        subtitle = if (hasRoomCredentials) "Live & synced to confirmed players"
                        else if (minutesUntilStart > 5L) "Broadcast window opens in ${minutesUntilStart}m"
                        else "Ready to broadcast to players",
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(V14CardElevated)
                                        .border(0.75.dp, V14Border, RoundedCornerShape(16.dp))
                                        .v14PressScale {
                                            com.example.notification.VelorixFcmManager.dispatchMatchReminderPush(
                                                context = context,
                                                tournament = tournament,
                                                minutesLeft = 15
                                            )
                                            Toast.makeText(context, "15-Min Reminder pushed to players!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.NotificationsActive, contentDescription = "Remind", tint = V14TextSecondary, modifier = Modifier.size(13.dp))
                                        Text("Remind", color = V14TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(V14AccentPrimary.copy(alpha = 0.16f))
                                        .border(0.75.dp, V14AccentBright.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                        .v14PressScale { showRoomBroadcastDialog = true }
                                        .padding(horizontal = 11.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (hasRoomCredentials) "Edit" else "Set Pass",
                                        color = V14AccentBright,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        if (hasRoomCredentials) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(V14CardElevated)
                                    .border(0.75.dp, V14Border, RoundedCornerShape(14.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                V14CredentialCell(
                                    label = "ROOM ID",
                                    value = tournament.roomDetails?.roomId ?: "-",
                                    modifier = Modifier.weight(1f),
                                    onCopy = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Room ID", tournament.roomDetails?.roomId ?: ""))
                                        Toast.makeText(context, "Room ID copied!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                Box(modifier = Modifier.width(0.5.dp).height(42.dp).background(V14Border))
                                V14CredentialCell(
                                    label = "PASSWORD",
                                    value = tournament.roomDetails?.roomPassword?.ifBlank { "None" } ?: "None",
                                    modifier = Modifier.weight(1f),
                                    onCopy = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Room Password", tournament.roomDetails?.roomPassword ?: ""))
                                        Toast.makeText(context, "Password copied!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(V14CardElevated)
                                    .border(0.75.dp, V14Border, RoundedCornerShape(14.dp))
                                    .v14PressScale { showRoomBroadcastDialog = true }
                                    .padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.LockClock, contentDescription = null, tint = VelorixAmber, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (minutesUntilStart > 5L) "Room Locked (${minutesUntilStart}m left)" else "Broadcast Room Credentials",
                                            color = V14TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Tap to publish custom Room ID & Password",
                                            color = V14TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = V14TextSecondary, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }

                // Match Proofs & Anti-Cheat Bento Card
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(V14CardSurface)
                            .border(0.75.dp, V14Border, RoundedCornerShape(20.dp))
                            .v14PressScale { showInspectProofsDialog = true }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background((if (pendingProofsCount > 0) VelorixAmber else V14TextSecondary).copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = if (pendingProofsCount > 0) VelorixAmber else V14TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Match Proofs & Claims", color = V14TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    if (pendingProofsCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(VelorixAmber)
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text("$pendingProofsCount PENDING", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (matchProofs.isEmpty()) "No scoreboard proofs submitted yet"
                                    else "${matchProofs.size} submission(s) ready for verification",
                                    color = V14TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = V14TextMuted, modifier = Modifier.size(16.dp))
                    }
                }

                // Admin Operations Bento Card (Single Clean Card, Hairline-Divided Rows)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(V14CardSurface)
                            .border(0.75.dp, V14Border, RoundedCornerShape(20.dp))
                    ) {
                        Text(
                            text = "ADMIN OPERATIONS",
                            color = V14TextMuted,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
                        )
                        if (viewModel != null) {
                            V14AdminOpRow(
                                icon = Icons.Default.Forum,
                                iconTint = VelorixEmerald,
                                title = "Lobby Chat Moderation",
                                subtitle = "Pin announcements, mute players, or broadcast room alerts",
                                onClick = { showLobbyChatModal = true }
                            )
                        }
                        V14AdminOpRow(
                            icon = Icons.Default.PersonAdd,
                            iconTint = V14TextPrimary,
                            title = "Add Participant Manually",
                            subtitle = "${tournament.registeredPlayers} of ${tournament.maxPlayers} slots occupied · Assign VIP slot",
                            onClick = { showAddParticipantDialog = true }
                        )
                        V14AdminOpRow(
                            icon = Icons.Default.PieChart,
                            iconTint = VelorixAmber,
                            title = "Customize Prize Split",
                            subtitle = "1st ₹${(if (tournament.firstPlacePrize > 0f) tournament.firstPlacePrize else tournament.prizePool * 0.5f).toInt()} · Kill ₹${(if (tournament.perKillPrize > 0f) tournament.perKillPrize else 20f).toInt()}",
                            onClick = { showEditPrizeDialog = true }
                        )
                        V14AdminOpRow(
                            icon = Icons.Default.Gavel,
                            iconTint = V14AccentBright,
                            title = "Rules & Loadout Settings",
                            subtitle = if (tournament.emulatorAllowed) "Emulator allowed · ${tournament.safeZoneShrinkSpeed}" else "Mobile only · ${if (tournament.limitedAmmo) "Limited ammo" else "Unlimited ammo"}",
                            onClick = { onEditRules(tournament.id) },
                            showDivider = false
                        )
                    }
                }
            }

            // =================================================================
            // TAB 1: ROSTER (Full Admin Roster Management)
            // =================================================================
            if (selectedSection == 1) {
                item {
                    V14BentoSectionCard(
                        icon = Icons.Default.Groups,
                        iconTint = V14TextPrimary,
                        title = "Registered Roster",
                        subtitle = "${tournament.registeredPlayers} of ${tournament.maxPlayers} slots filled",
                        trailingContent = {
                            Row(
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(17.dp))
                                    .background(V14AccentPrimary.copy(alpha = 0.16f))
                                    .border(0.75.dp, V14AccentBright.copy(alpha = 0.4f), RoundedCornerShape(17.dp))
                                    .v14PressScale { showAddParticipantDialog = true }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = V14AccentBright, modifier = Modifier.size(14.dp))
                                Text("Add Player", color = V14AccentBright, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    ) {
                        if (tournament.registrations.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = V14TextMuted, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No players registered yet", color = V14TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text("Use 'Add Player' to assign VIP or offline slots.", color = V14TextSecondary, fontSize = 11.5.sp)
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                tournament.registrations.forEachIndexed { idx, reg ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(V14CardElevated)
                                                    .border(0.6.dp, V14Border, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${idx + 1}",
                                                    color = V14TextSecondary,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = reg.playerName.ifBlank { reg.gameUsername.ifBlank { "Player #${idx + 1}" } },
                                                    color = V14TextPrimary,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "UID: ${reg.gameId.ifBlank { reg.playerId }}",
                                                    color = V14TextMuted,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        val isPaid = reg.paymentStatus.equals("PAID", ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(if (isPaid) VelorixEmerald.copy(alpha = 0.12f) else V14CardElevated)
                                                .border(
                                                    0.6.dp,
                                                    if (isPaid) VelorixEmerald.copy(alpha = 0.4f) else V14Border,
                                                    RoundedCornerShape(50)
                                                )
                                                .padding(horizontal = 9.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = reg.paymentStatus.ifBlank { "CONFIRMED" },
                                                color = if (isPaid) VelorixEmerald else V14TextSecondary,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    if (idx < tournament.registrations.lastIndex) {
                                        HorizontalDivider(thickness = 0.5.dp, color = V14Border)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // TAB 2: PRIZES (Single Non-Duplicated V14 Bento Card)
            // =================================================================
            if (selectedSection == 2) {
                item {
                    V14BentoSectionCard(
                        icon = Icons.Default.EmojiEvents,
                        iconTint = VelorixAmber,
                        title = "Prize Distribution",
                        subtitle = "Tap any slice or card to inspect payout share",
                        trailingContent = {
                            Row(
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(17.dp))
                                    .background(V14AccentPrimary.copy(alpha = 0.16f))
                                    .border(0.75.dp, V14AccentBright.copy(alpha = 0.4f), RoundedCornerShape(17.dp))
                                    .v14PressScale { showEditPrizeDialog = true }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = V14AccentBright, modifier = Modifier.size(13.dp))
                                Text("Edit Split", color = V14AccentBright, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    ) {
                        PrizeDistributionPieChart(
                            prizePool = tournament.prizePool,
                            firstPlacePrize = tournament.firstPlacePrize,
                            secondPlacePrize = tournament.secondPlacePrize,
                            thirdPlacePrize = tournament.thirdPlacePrize,
                            perKillPrize = tournament.perKillPrize,
                            maxPlayers = tournament.maxPlayers,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = V14Border)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Payouts disburse automatically to player wallets after match standings audit.",
                            color = V14TextMuted,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // =================================================================
            // TAB 3: RULES (Clean V14 Bento Loadout + Official Regulations)
            // =================================================================
            if (selectedSection == 3) {
                item {
                    V14BentoSectionCard(
                        icon = Icons.Default.Gavel,
                        iconTint = V14AccentBright,
                        title = "Loadout & Match Rules",
                        subtitle = if (isMoneyMatch) "18+ Cash Compliance · Competitive Lobby" else "Open Training Lobby",
                        trailingContent = {
                            Row(
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(17.dp))
                                    .background(V14CardElevated)
                                    .border(0.75.dp, V14Border, RoundedCornerShape(17.dp))
                                    .v14PressScale { onEditRules(tournament.id) }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = V14TextPrimary, modifier = Modifier.size(13.dp))
                                Text("Edit", color = V14TextPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    ) {
                        V14RuleLine("Allowed Weapons", tournament.allowedGuns.ifBlank { "All Standard Weapons" }, VelorixEmerald)
                        V14RuleLine("Banned Weapons", tournament.bannedGuns.ifBlank { "M79, M82B, Crossbow" }, Color(0xFFF87171))
                        V14RuleLine("Allowed Character Skills", tournament.allowedSkills.ifBlank { "All Active & Passive Skills" }, V14TextPrimary)
                        if (tournament.bannedSkills.isNotBlank() && !tournament.bannedSkills.equals("None", ignoreCase = true)) {
                            V14RuleLine("Banned Character Skills", tournament.bannedSkills, Color(0xFFF87171))
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            V14ToggleRuleTile("Gun Attributes", if (tournament.gunAttributesAllowed) "ENABLED" else "OFF", tournament.gunAttributesAllowed, Modifier.weight(1f))
                            V14ToggleRuleTile("Ammo Mode", if (tournament.limitedAmmo) "LIMITED" else "UNLIMITED", tournament.limitedAmmo, Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            V14ToggleRuleTile("Character Skills", if (tournament.characterSkillsAllowed) "ALLOWED" else "DISABLED", tournament.characterSkillsAllowed, Modifier.weight(1f))
                            V14ToggleRuleTile("Emulator / PC", if (tournament.emulatorAllowed) "ALLOWED" else "MOBILE ONLY", !tournament.emulatorAllowed, Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            V14ToggleRuleTile("Headshot Mode", if (tournament.headshotOnly) "HEADSHOT ONLY" else "STANDARD", !tournament.headshotOnly, Modifier.weight(1f))
                            V14ToggleRuleTile("Revival System", if (tournament.revivalAllowed) "ENABLED" else "PERMADEATH", tournament.revivalAllowed, Modifier.weight(1f))
                        }

                        if (tournament.customMatchSettings.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            V14RuleLine(
                                title = "Room Modifiers & Safe Zone",
                                details = "${tournament.customMatchSettings} · Safe Zone: ${tournament.safeZoneShrinkSpeed}",
                                titleColor = V14AccentBright,
                                showDivider = false
                            )
                        }
                    }
                }

                item {
                    V14BentoSectionCard(
                        icon = Icons.Default.MenuBook,
                        iconTint = V14TextSecondary,
                        title = "Official Regulations",
                        subtitle = "Displayed to registered participants"
                    ) {
                        Text(
                            text = tournament.rules.ifBlank {
                                "1. Players must enter the custom room with their registered Free Fire UID.\n" +
                                    "2. Teaming or third-party cheats result in permanent ban.\n" +
                                    "3. Final scoreboard screenshot is required for prize claim.\n" +
                                    "4. Room ID and password are confidential."
                            },
                            color = V14TextSecondary,
                            fontSize = 12.5.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }

    // =========================================================================
    // ADMIN DIALOGS (All Firebase Sync & Workflows Preserved)
    // =========================================================================

    // 1. Broadcast Room ID & Password Dialog
    if (showRoomBroadcastDialog) {
        FrostedGlassAlertDialog(
            onDismissRequest = { showRoomBroadcastDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = V14AccentBright)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Broadcast Room ID & Pass", color = V14TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (minutesUntilStart > 5L && !bypass5MinCheck) {
                        Surface(
                            color = Color(0xFF23180E),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(0.75.dp, VelorixAmber.copy(alpha = 0.45f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = VelorixAmber, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("5-Minute Window Enforced", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VelorixAmber)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "$minutesUntilStart minutes remaining until match start. Enable bypass to broadcast early.",
                                    fontSize = 11.sp,
                                    color = V14TextPrimary.copy(alpha = 0.85f),
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Admin Emergency Bypass", fontSize = 11.sp, color = V14TextSecondary)
                                    Switch(
                                        checked = bypass5MinCheck,
                                        onCheckedChange = { bypass5MinCheck = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = V14AccentPrimary)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = roomIdInput,
                        onValueChange = { roomIdInput = it },
                        label = { Text("Free Fire Custom Room ID") },
                        placeholder = { Text("e.g. 84920194") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = V14CardElevated,
                            unfocusedContainerColor = V14CardElevated,
                            focusedBorderColor = V14AccentPrimary,
                            unfocusedBorderColor = V14Border,
                            focusedTextColor = V14TextPrimary,
                            unfocusedTextColor = V14TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = roomPassInput,
                        onValueChange = { roomPassInput = it },
                        label = { Text("Room Password") },
                        placeholder = { Text("e.g. 1234 or velorix") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = V14CardElevated,
                            unfocusedContainerColor = V14CardElevated,
                            focusedBorderColor = V14AccentPrimary,
                            unfocusedBorderColor = V14Border,
                            focusedTextColor = V14TextPrimary,
                            unfocusedTextColor = V14TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Credentials sync in real time to all confirmed participants.",
                        fontSize = 11.sp,
                        color = V14TextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (roomIdInput.isBlank()) {
                            Toast.makeText(context, "Please enter Room ID", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val cleanedRoomId = roomIdInput.trim()
                        val cleanedRoomPass = roomPassInput.trim()
                        onPublishRoomCredentials(cleanedRoomId, cleanedRoomPass, bypass5MinCheck)
                        viewModel?.sendManualNotification(
                            title = "Room ID & Password Live: ${tournament.title}",
                            message = "Room ID: $cleanedRoomId | Password: ${cleanedRoomPass.ifBlank { "Open" }}. Join lobby now!",
                            type = "ROOM_CREDENTIALS",
                            targetType = "TOURNAMENT",
                            tournamentId = tournament.id,
                            roomId = cleanedRoomId,
                            roomPassword = cleanedRoomPass
                        )
                        com.example.notification.VelorixFcmManager.dispatchRoomDetailsPush(
                            context = context,
                            tournament = tournament,
                            roomId = cleanedRoomId,
                            roomPassword = cleanedRoomPass
                        )
                        com.example.analytics.VelorixAnalytics.logRoomCredentialsViewed(tournament.id, cleanedRoomId)
                        showRoomBroadcastDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = V14AccentPrimary, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    enabled = isWithin5Minutes || bypass5MinCheck
                ) {
                    Text("Broadcast", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRoomBroadcastDialog = false }) {
                    Text("Cancel", color = V14TextSecondary)
                }
            }
        )
    }

    // 2. Add Participant Dialog (Admin Manual Slot Assignment)
    if (showAddParticipantDialog) {
        val parsedAge = manualPlayerAge.toIntOrNull() ?: 18
        val isPlayerUnder18 = manualPlayerIsMinor || (parsedAge in 1..17)
        val canSubmit = !(isMoneyMatch && isPlayerUnder18)

        FrostedGlassAlertDialog(
            onDismissRequest = { showAddParticipantDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = V14AccentBright)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Player to Roster", color = V14TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Manually register a VIP or invited player to this match.", fontSize = 12.sp, color = V14TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = manualPlayerIgn,
                        onValueChange = { manualPlayerIgn = it },
                        label = { Text("In-Game Name (IGN)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = V14CardElevated,
                            unfocusedContainerColor = V14CardElevated,
                            focusedBorderColor = V14AccentPrimary,
                            unfocusedBorderColor = V14Border,
                            focusedTextColor = V14TextPrimary,
                            unfocusedTextColor = V14TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualPlayerUid,
                        onValueChange = { manualPlayerUid = it },
                        label = { Text("Free Fire UID") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = V14CardElevated,
                            unfocusedContainerColor = V14CardElevated,
                            focusedBorderColor = V14AccentPrimary,
                            unfocusedBorderColor = V14Border,
                            focusedTextColor = V14TextPrimary,
                            unfocusedTextColor = V14TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = manualPlayerAge,
                            onValueChange = { if (it.length <= 3 && it.all { ch -> ch.isDigit() }) manualPlayerAge = it },
                            label = { Text("Age") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = V14CardElevated,
                                unfocusedContainerColor = V14CardElevated,
                                focusedBorderColor = V14AccentPrimary,
                                unfocusedBorderColor = V14Border,
                                focusedTextColor = V14TextPrimary,
                                unfocusedTextColor = V14TextPrimary
                            )
                        )
                        Row(
                            modifier = Modifier.weight(1.3f).clickable { manualPlayerIsMinor = !manualPlayerIsMinor },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isPlayerUnder18,
                                onCheckedChange = { manualPlayerIsMinor = it },
                                colors = CheckboxDefaults.colors(checkedColor = if (isMoneyMatch) Color(0xFFEF4444) else V14AccentPrimary)
                            )
                            Text("Under 18", fontSize = 11.5.sp, color = if (isMoneyMatch && isPlayerUnder18) Color(0xFFFCA5A5) else V14TextSecondary)
                        }
                    }
                    if (isMoneyMatch && isPlayerUnder18) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Under-18 players cannot participate in real-money matches.",
                            color = Color(0xFFFCA5A5),
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualPlayerIgn.isBlank() || manualPlayerUid.isBlank()) {
                            Toast.makeText(context, "Please enter Player IGN & UID", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (!canSubmit) {
                            Toast.makeText(context, "Minors cannot join cash matches.", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        val newReg = PlayerRegistration(
                            playerId = "adm_${System.currentTimeMillis()}",
                            playerName = manualPlayerIgn.trim(),
                            gameUsername = manualPlayerIgn.trim(),
                            gameId = manualPlayerUid.trim(),
                            age = parsedAge,
                            paymentStatus = if (isMoneyMatch) "ADMIN_ADDED" else "FREE_TRAINING",
                            registeredAt = System.currentTimeMillis()
                        )
                        onAddParticipant?.invoke(tournament.id, newReg)
                        manualPlayerIgn = ""
                        manualPlayerUid = ""
                        manualPlayerAge = "18"
                        manualPlayerIsMinor = false
                        showAddParticipantDialog = false
                        Toast.makeText(context, "Player added to roster!", Toast.LENGTH_SHORT).show()
                    },
                    enabled = canSubmit,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = V14AccentPrimary, contentColor = Color.White)
                ) {
                    Text(if (canSubmit) "Add Player" else "Restricted", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddParticipantDialog = false }) {
                    Text("Cancel", color = V14TextSecondary)
                }
            }
        )
    }

    // 3. Match Proofs Inspector Dialog
    if (showInspectProofsDialog) {
        AlertDialog(
            onDismissRequest = { showInspectProofsDialog = false },
            title = {
                Text("Match Proofs (${matchProofs.size})", color = V14TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                if (matchProofs.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VelorixEmerald, modifier = Modifier.size(34.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No proof submissions yet", color = V14TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Player scoreboard claims will appear here for review.", color = V14TextSecondary, fontSize = 11.5.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(matchProofs) { proof ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(V14CardElevated)
                                    .border(0.75.dp, V14Border, RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(proof.username.ifBlank { "Player" }, fontWeight = FontWeight.Bold, color = V14TextPrimary, fontSize = 13.sp)
                                        Text("UID: ${proof.gameAccountId}", fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, color = V14TextMuted)
                                    }
                                    Text(
                                        text = proof.status.uppercase(),
                                        color = when (proof.status.lowercase()) {
                                            "approved" -> VelorixEmerald
                                            "rejected" -> Color(0xFFF87171)
                                            else -> VelorixAmber
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Rank #${proof.claimedRank} · ${proof.claimedKills} Kills", color = V14TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                                if (proof.status.equals("pending", ignoreCase = true)) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                val prizeAmt = when (proof.claimedRank) {
                                                    1 -> if (tournament.firstPlacePrize > 0f) tournament.firstPlacePrize.toDouble() else tournament.prizePool * 0.50
                                                    2 -> if (tournament.secondPlacePrize > 0f) tournament.secondPlacePrize.toDouble() else tournament.prizePool * 0.25
                                                    3 -> if (tournament.thirdPlacePrize > 0f) tournament.thirdPlacePrize.toDouble() else tournament.prizePool * 0.15
                                                    else -> 0.0
                                                } + (proof.claimedKills * (if (tournament.perKillPrize > 0f) tournament.perKillPrize.toDouble() else 20.0))
                                                onApproveMatchProof(proof.id, prizeAmt, proof.claimedKills)
                                            },
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = VelorixEmerald, contentColor = Color.Black),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("Approve", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { onRejectMatchProof(proof.id, "Scoreboard mismatch or suspicious gameplay.") },
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                                            border = BorderStroke(0.75.dp, Color(0xFFF87171).copy(alpha = 0.55f)),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("Reject", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInspectProofsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = V14TextPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = V14CardSurface
        )
    }

    // 4. Edit Prize Split Dialog
    if (showEditPrizeDialog) {
        val currPool = editPrizePoolInput.toFloatOrNull() ?: tournament.prizePool
        AlertDialog(
            onDismissRequest = { showEditPrizeDialog = false },
            title = { Text("Customize Prize Split", color = V14TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editPrizePoolInput,
                        onValueChange = { editPrizePoolInput = it },
                        label = { Text("Total Prize Pool (₹)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = V14CardElevated,
                            unfocusedContainerColor = V14CardElevated,
                            focusedBorderColor = V14AccentPrimary,
                            unfocusedBorderColor = V14Border,
                            focusedTextColor = V14TextPrimary,
                            unfocusedTextColor = V14TextPrimary
                        )
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        V14SplitPresetPill("50/25/15", Modifier.weight(1f)) {
                            editFirstPlaceInput = (currPool * 0.50f).toInt().toString()
                            editSecondPlaceInput = (currPool * 0.25f).toInt().toString()
                            editThirdPlaceInput = (currPool * 0.15f).toInt().toString()
                            editPerKillInput = "20"
                        }
                        V14SplitPresetPill("60/25/15", Modifier.weight(1f)) {
                            editFirstPlaceInput = (currPool * 0.60f).toInt().toString()
                            editSecondPlaceInput = (currPool * 0.25f).toInt().toString()
                            editThirdPlaceInput = (currPool * 0.15f).toInt().toString()
                            editPerKillInput = "0"
                        }
                        V14SplitPresetPill("100% #1", Modifier.weight(1f)) {
                            editFirstPlaceInput = currPool.toInt().toString()
                            editSecondPlaceInput = "0"
                            editThirdPlaceInput = "0"
                            editPerKillInput = "0"
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editFirstPlaceInput,
                            onValueChange = { editFirstPlaceInput = it },
                            label = { Text("1st (₹)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = V14CardElevated, unfocusedContainerColor = V14CardElevated, focusedBorderColor = VelorixAmber, unfocusedBorderColor = V14Border, focusedTextColor = V14TextPrimary, unfocusedTextColor = V14TextPrimary)
                        )
                        OutlinedTextField(
                            value = editSecondPlaceInput,
                            onValueChange = { editSecondPlaceInput = it },
                            label = { Text("2nd (₹)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = V14CardElevated, unfocusedContainerColor = V14CardElevated, focusedBorderColor = V14TextPrimary, unfocusedBorderColor = V14Border, focusedTextColor = V14TextPrimary, unfocusedTextColor = V14TextPrimary)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editThirdPlaceInput,
                            onValueChange = { editThirdPlaceInput = it },
                            label = { Text("3rd (₹)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = V14CardElevated, unfocusedContainerColor = V14CardElevated, focusedBorderColor = Color(0xFFFB923C), unfocusedBorderColor = V14Border, focusedTextColor = V14TextPrimary, unfocusedTextColor = V14TextPrimary)
                        )
                        OutlinedTextField(
                            value = editPerKillInput,
                            onValueChange = { editPerKillInput = it },
                            label = { Text("Per Kill (₹)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = V14CardElevated, unfocusedContainerColor = V14CardElevated, focusedBorderColor = V14AccentPrimary, unfocusedBorderColor = V14Border, focusedTextColor = V14TextPrimary, unfocusedTextColor = V14TextPrimary)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedTournament = tournament.copy(
                            prizePool = editPrizePoolInput.toFloatOrNull() ?: tournament.prizePool,
                            firstPlacePrize = editFirstPlaceInput.toFloatOrNull() ?: 0f,
                            secondPlacePrize = editSecondPlaceInput.toFloatOrNull() ?: 0f,
                            thirdPlacePrize = editThirdPlaceInput.toFloatOrNull() ?: 0f,
                            perKillPrize = editPerKillInput.toFloatOrNull() ?: 0f
                        )
                        onUpdateTournament?.invoke(updatedTournament)
                        showEditPrizeDialog = false
                        Toast.makeText(context, "Prize split updated!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = V14AccentPrimary, contentColor = Color.White)
                ) {
                    Text("Save Split", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPrizeDialog = false }) {
                    Text("Cancel", color = V14TextSecondary)
                }
            },
            containerColor = V14CardSurface
        )
    }

    // 5. Manual Cancellation Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Match & Refund", color = V14TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Sets status to CANCELLED and notifies players with your reason.",
                        fontSize = 12.5.sp,
                        color = V14TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = cancelReasonInput,
                        onValueChange = { cancelReasonInput = it },
                        label = { Text("Cancellation Reason") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = V14CardElevated,
                            unfocusedContainerColor = V14CardElevated,
                            focusedBorderColor = Color(0xFFF87171),
                            unfocusedBorderColor = V14Border,
                            focusedTextColor = V14TextPrimary,
                            unfocusedTextColor = V14TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelTournament(tournament.id, cancelReasonInput.trim().ifBlank { "Insufficient players joined. Match cancelled manually." })
                        showCancelDialog = false
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White)
                ) {
                    Text("Confirm Cancel", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Match", color = V14TextSecondary)
                }
            },
            containerColor = V14CardSurface
        )
    }

    // 6. Delete Tournament Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = { Text("Delete Tournament?", color = V14TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Permanently delete \"${tournament.title}\" along with all slots, proofs, and registrations? This cannot be undone.",
                    fontSize = 12.5.sp,
                    color = V14TextSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isDeleting) {
                            isDeleting = true
                            Toast.makeText(context, "Deleting tournament...", Toast.LENGTH_SHORT).show()
                            onDeleteTournament(tournament.id)
                            showDeleteDialog = false
                        }
                    },
                    enabled = !isDeleting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, enabled = !isDeleting) {
                    Text("Cancel", color = V14TextSecondary)
                }
            },
            containerColor = V14CardSurface
        )
    }

    // 7. Lobby Chat Modal
    if (showLobbyChatModal && viewModel != null) {
        Dialog(
            onDismissRequest = { showLobbyChatModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.88f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(V14Bg)
                    .border(0.75.dp, V14Border, RoundedCornerShape(22.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LOBBY CHAT · ${tournament.title.uppercase()}",
                        color = V14TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showLobbyChatModal = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = V14TextPrimary)
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = V14Border)
                TournamentLobbyChatModeratorSection(
                    viewModel = viewModel,
                    tournaments = listOf(tournament),
                    initialTournamentId = tournament.id
                )
            }
        }
    }

    // 8. Declare Standings & Prizes Modal
    if (showDeclareStandingsModal && viewModel != null) {
        DeclareStandingsAndPrizesDialog(
            tournament = tournament,
            viewModel = viewModel,
            onDismiss = { showDeclareStandingsModal = false }
        )
    }
}

@Composable
private fun V14BentoSectionCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(V14CardSurface)
            .border(0.75.dp, V14Border, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                        .background(iconTint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        color = V14TextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        color = V14TextSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (trailingContent != null) {
                trailingContent()
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun V14TelemetryCell(label: String, value: String, valueColor: Color) {
    Column {
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = V14TextMuted,
            letterSpacing = 0.6.sp
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = valueColor,
            maxLines = 1
        )
    }
}

@Composable
private fun V14CredentialCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onCopy: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable(onClick = onCopy)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 9.5.sp, color = V14TextMuted, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy $label", tint = V14AccentBright, modifier = Modifier.size(13.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = V14TextPrimary,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun V14AdminOpRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, color = V14TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, color = V14TextSecondary, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = V14TextMuted, modifier = Modifier.size(15.dp))
        }
        if (showDivider) {
            HorizontalDivider(thickness = 0.5.dp, color = V14Border, modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
private fun V14RuleLine(
    title: String,
    details: String,
    titleColor: Color,
    showDivider: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(title, color = titleColor, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(3.dp))
        Text(details, color = V14TextPrimary.copy(alpha = 0.88f), fontSize = 12.5.sp, lineHeight = 18.sp)
        if (showDivider) {
            Spacer(modifier = Modifier.height(9.dp))
            HorizontalDivider(thickness = 0.5.dp, color = V14Border)
        }
    }
}

@Composable
private fun V14ToggleRuleTile(
    label: String,
    value: String,
    positive: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(V14CardElevated)
            .border(0.6.dp, V14Border, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, color = V14TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = if (positive) VelorixEmerald else Color(0xFFF87171),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun V14SplitPresetPill(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = V14CardElevated,
        border = BorderStroke(0.75.dp, V14Border)
    ) {
        Text(
            text = label,
            color = V14TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}
