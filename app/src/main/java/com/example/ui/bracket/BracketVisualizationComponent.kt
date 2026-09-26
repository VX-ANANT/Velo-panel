package com.example.ui.bracket

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.domain.model.Match
import com.example.ui.common.GlassCard
import com.example.ui.common.GlassTokens
import com.example.ui.theme.VelorixAccent
import kotlin.math.roundToInt

/**
 * State holder for tracking Drag and Drop player progression across tournament bracket rounds.
 */
class DragDropBracketState {
    var isDragging by mutableStateOf(false)
    var draggedPlayerId by mutableStateOf<String?>(null)
    var sourceMatchId by mutableStateOf<String?>(null)
    var dragPositionInWindow by mutableStateOf(Offset.Zero)
    var activeDropTarget by mutableStateOf<Pair<String, Int>?>(null) // Target Match ID to Slot (1 or 2)
    
    // Bounds of all available drop targets registered via onGloballyPositioned
    val dropTargetBounds = mutableStateMapOf<Pair<String, Int>, Rect>()

    fun startDrag(playerId: String, sourceMatch: String, startPos: Offset) {
        draggedPlayerId = playerId
        sourceMatchId = sourceMatch
        dragPositionInWindow = startPos
        isDragging = true
        activeDropTarget = null
    }

    fun updateDrag(currentPos: Offset) {
        dragPositionInWindow = currentPos
        // Check collision with registered drop targets
        var foundTarget: Pair<String, Int>? = null
        for ((targetKey, rect) in dropTargetBounds) {
            // Expand hit target slightly for easier mobile touch dropping
            val expandedRect = Rect(
                left = rect.left - 20f,
                top = rect.top - 20f,
                right = rect.right + 20f,
                bottom = rect.bottom + 20f
            )
            if (expandedRect.contains(currentPos)) {
                // Don't drop into the exact same source slot
                if (targetKey.first != sourceMatchId) {
                    foundTarget = targetKey
                    break
                }
            }
        }
        activeDropTarget = foundTarget
    }

    fun endDrag(onDrop: (sourceMatchId: String?, targetMatchId: String, playerId: String, slot: Int) -> Unit) {
        val target = activeDropTarget
        val player = draggedPlayerId
        if (target != null && player != null) {
            onDrop(sourceMatchId, target.first, player, target.second)
        }
        isDragging = false
        draggedPlayerId = null
        sourceMatchId = null
        activeDropTarget = null
    }
}

@Composable
fun rememberDragDropBracketState(): DragDropBracketState {
    return remember { DragDropBracketState() }
}

/**
 * Comprehensive Bracket Visualization Component with Real-Time Firebase Firestore observation
 * and Drag-and-Drop match advancement.
 */
@Composable
fun BracketVisualizationComponent(
    tournamentId: String,
    matches: List<Match>,
    onAdvancePlayer: (sourceMatchId: String?, targetMatchId: String, playerId: String, targetSlot: Int) -> Unit,
    onUpdateMatchStatus: (match: Match, newStatus: String, winnerId: String?) -> Unit,
    onRecordScore: ((matchId: String, winnerId: String, score1: Int, score2: Int) -> Unit)? = null,
    onGenerateBracket: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dragDropState = rememberDragDropBracketState()
    var selectedMatchForScore by remember { mutableStateOf<Match?>(null) }

    // Group matches into sorted rounds
    val roundsGrouped = remember(matches) {
        matches.groupBy { it.round }.toSortedMap()
    }

    // Glowing Firestore observer pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "firestore_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Live Firestore Real-Time Sync Header Pill
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0D0D11).copy(alpha = 0.90f),
                border = BorderStroke(1.dp, Color(0xFF27272A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pulsing Green Live Firestore Indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E).copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LIVE FIRESTORE REALTIME SYNC",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF4ADE80),
                            letterSpacing = 0.6.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                        ) {
                            val completedCount = matches.count { it.status.equals("completed", true) }
                            Text(
                                text = "$completedCount/${matches.size} Completed",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (onGenerateBracket != null && matches.isEmpty()) {
                            Button(
                                onClick = onGenerateBracket,
                                colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Auto-Generate", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }

            // Drag & Drop Instruction Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0x3300E5FF),
                border = BorderStroke(1.dp, Color(0x6600E5FF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Swipe,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DRAG & DROP: Press & drag any player onto a next round slot to advance them instantly in Firestore!",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE0F7FA),
                        lineHeight = 14.sp
                    )
                }
            }

            if (matches.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        borderBrush = GlassTokens.GlassBorderGradient
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(VelorixAccent.copy(alpha = 0.15f), CircleShape)
                                    .border(1.dp, VelorixAccent.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountTree, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(28.dp))
                            }
                            Text(
                                "No Bracket Matches Found",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Generate a single-elimination tournament tree from registered players to observe and advance matches in real-time.",
                                color = Color(0xFFA1A1AA),
                                fontSize = 12.5.sp,
                                textAlign = TextAlign.Center
                            )
                            if (onGenerateBracket != null) {
                                Button(
                                    onClick = onGenerateBracket,
                                    colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Generate Elimination Bracket", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                // Interactive Horizontal Multi-Stage Bracket Layout with Canvas connector lines
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    roundsGrouped.forEach { (roundNumber, roundMatches) ->
                        val isFinalRound = roundNumber == roundsGrouped.lastKey()
                        val roundTitle = when {
                            isFinalRound -> "CHAMPIONSHIP FINALS"
                            roundNumber == roundsGrouped.lastKey() - 1 -> "SEMI-FINALS"
                            roundNumber == 1 -> "ROUND 1 • QUARTER-FINALS"
                            else -> "ROUND $roundNumber"
                        }

                        Column(
                            modifier = Modifier
                                .width(310.dp)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Round Column Header
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF131317),
                                border = BorderStroke(1.dp, if (isFinalRound) Color(0xFFF59E0B).copy(alpha = 0.5f) else Color(0xFF27272A))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isFinalRound) {
                                            Icon(
                                                imageVector = Icons.Default.EmojiEvents,
                                                contentDescription = null,
                                                tint = Color(0xFFFBBF24),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = roundTitle,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isFinalRound) Color(0xFFFBBF24) else VelorixAccent,
                                            letterSpacing = 0.5.sp
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White.copy(alpha = 0.08f)
                                    ) {
                                        Text(
                                            text = "${roundMatches.size} Match${if (roundMatches.size > 1) "es" else ""}",
                                            fontSize = 10.sp,
                                            color = Color(0xFFA1A1AA),
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Match Cards in this round
                            roundMatches.forEach { match ->
                                BracketMatchNode(
                                    match = match,
                                    isFinalRound = isFinalRound,
                                    dragDropState = dragDropState,
                                    onUpdateMatchStatus = onUpdateMatchStatus,
                                    onAdvancePlayer = onAdvancePlayer,
                                    onScoreClick = { selectedMatchForScore = match }
                                )
                            }

                            Spacer(modifier = Modifier.height(48.dp))
                        }
                    }
                }
            }
        }

        // Floating Drag Overlay that follows finger position across the entire window
        if (dragDropState.isDragging && dragDropState.draggedPlayerId != null) {
            val density = LocalDensity.current
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(9999f)
            ) {
                Surface(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (dragDropState.dragPositionInWindow.x - 70).roundToInt(),
                                y = (dragDropState.dragPositionInWindow.y - 45).roundToInt()
                            )
                        }
                        .shadow(16.dp, RoundedCornerShape(12.dp))
                        .graphicsLayer {
                            scaleX = 1.08f
                            scaleY = 1.08f
                            rotationZ = 3f
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E1B4B),
                    border = BorderStroke(2.dp, Color(0xFF00E5FF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = dragDropState.draggedPlayerId ?: "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = if (dragDropState.activeDropTarget != null) "Ready to Drop!" else "Drop on Next Round Slot",
                                fontSize = 9.sp,
                                color = if (dragDropState.activeDropTarget != null) Color(0xFF4ADE80) else Color(0xFF93C5FD)
                            )
                        }
                    }
                }
            }
        }

        // Score Recording Dialog
        selectedMatchForScore?.let { match ->
            ScoreRecordDialog(
                match = match,
                onDismiss = { selectedMatchForScore = null },
                onSave = { winnerId, s1, s2 ->
                    onRecordScore?.invoke(match.id, winnerId, s1, s2)
                    selectedMatchForScore = null
                }
            )
        }
    }
}

/**
 * Individual Match Node within a Tournament Bracket Round
 */
@Composable
fun BracketMatchNode(
    match: Match,
    isFinalRound: Boolean,
    dragDropState: DragDropBracketState,
    onUpdateMatchStatus: (match: Match, newStatus: String, winnerId: String?) -> Unit,
    onAdvancePlayer: (sourceMatchId: String?, targetMatchId: String, playerId: String, targetSlot: Int) -> Unit,
    onScoreClick: () -> Unit
) {
    val isCompleted = match.status.equals("completed", true)
    val statusColor = when {
        isCompleted -> Color(0xFF22C55E)
        match.status.equals("live", true) -> Color(0xFFEF4444)
        match.status.equals("ready", true) -> Color(0xFF00E5FF)
        else -> Color(0xFF94A3B8)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0E0E12),
        border = BorderStroke(1.dp, if (isCompleted) Color(0xFF22C55E).copy(alpha = 0.4f) else Color(0xFF27272A))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Match Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(Color.White.copy(alpha = 0.08f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${match.matchNumber}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MATCH ${match.id}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFA1A1AA),
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = match.status.uppercase(),
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Player 1 Slot
            ParticipantSlotRow(
                matchId = match.id,
                slotNumber = 1,
                playerId = match.player1Id,
                score = match.score1,
                isWinner = match.player1Id != null && match.player1Id == match.winnerId,
                dragDropState = dragDropState,
                onDropPlayer = { playerId ->
                    onAdvancePlayer(dragDropState.sourceMatchId, match.id, playerId, 1)
                },
                onQuickAdvance = {
                    match.player1Id?.let { pId ->
                        onUpdateMatchStatus(match, "completed", pId)
                    }
                }
            )

            // VS Divider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color(0xFF27272A))
                )
                Text(
                    text = "VS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF71717A),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color(0xFF27272A))
                )
            }

            // Player 2 Slot
            ParticipantSlotRow(
                matchId = match.id,
                slotNumber = 2,
                playerId = match.player2Id,
                score = match.score2,
                isWinner = match.player2Id != null && match.player2Id == match.winnerId,
                dragDropState = dragDropState,
                onDropPlayer = { playerId ->
                    onAdvancePlayer(dragDropState.sourceMatchId, match.id, playerId, 2)
                },
                onQuickAdvance = {
                    match.player2Id?.let { pId ->
                        onUpdateMatchStatus(match, "completed", pId)
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Bar (Score & Reset)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onScoreClick,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Scoreboard, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Score: ${match.score1 ?: 0} - ${match.score2 ?: 0}", fontSize = 11.sp, color = VelorixAccent, fontWeight = FontWeight.Bold)
                }

                if (isCompleted) {
                    TextButton(
                        onClick = { onUpdateMatchStatus(match, "pending", null) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Reset", fontSize = 10.5.sp, color = Color(0xFFEF4444))
                    }
                }
            }
        }
    }
}

/**
 * Participant Slot supporting both Dragging (to advance to future rounds)
 * and Dropping (to receive an advancing player from earlier rounds).
 */
@Composable
fun ParticipantSlotRow(
    matchId: String,
    slotNumber: Int,
    playerId: String?,
    score: Int?,
    isWinner: Boolean,
    dragDropState: DragDropBracketState,
    onDropPlayer: (playerId: String) -> Unit,
    onQuickAdvance: () -> Unit
) {
    val isTargeted = dragDropState.activeDropTarget == Pair(matchId, slotNumber)
    val hasPlayer = !playerId.isNullOrBlank()

    var slotWindowOffset by remember { mutableStateOf(Offset.Zero) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                dragDropState.dropTargetBounds[Pair(matchId, slotNumber)] = bounds
                slotWindowOffset = bounds.topLeft
            }
            .then(
                if (hasPlayer) {
                    // Draggable behavior when player is present
                    Modifier.pointerInput(playerId, matchId) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragDropState.startDrag(playerId, matchId, slotWindowOffset + offset)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragDropState.updateDrag(dragDropState.dragPositionInWindow + dragAmount)
                            },
                            onDragEnd = {
                                dragDropState.endDrag { sourceMatch, targetMatch, pId, targetSlot ->
                                    if (targetMatch == matchId && targetSlot == slotNumber) {
                                        onDropPlayer(pId)
                                    }
                                }
                            },
                            onDragCancel = {
                                dragDropState.endDrag { _, _, _, _ -> }
                            }
                        )
                    }
                } else Modifier
            ),
        shape = RoundedCornerShape(10.dp),
        color = when {
            isTargeted -> Color(0xFF00E5FF).copy(alpha = 0.25f)
            isWinner -> Color(0xFF14532D).copy(alpha = 0.35f)
            hasPlayer -> Color(0xFF18181C)
            else -> Color(0xFF121215)
        },
        border = BorderStroke(
            width = if (isTargeted) 2.dp else 1.dp,
            color = when {
                isTargeted -> Color(0xFF00E5FF)
                isWinner -> Color(0xFF22C55E)
                hasPlayer -> Color(0xFF27272A)
                else -> Color(0xFF27272A).copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasPlayer) {
                    // Drag grip handle
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = "Drag to advance",
                        tint = if (isWinner) Color(0xFF4ADE80) else Color(0xFF71717A),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (isWinner) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Winner",
                        tint = Color(0xFF4ADE80),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Text(
                    text = playerId ?: "Drop Player Here (Slot $slotNumber)",
                    fontSize = 11.5.sp,
                    fontWeight = if (hasPlayer) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isTargeted -> Color(0xFF00E5FF)
                        isWinner -> Color(0xFF4ADE80)
                        hasPlayer -> Color.White
                        else -> Color(0xFF71717A)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasPlayer && !isWinner) {
                    // Quick-advance promotion button
                    IconButton(
                        onClick = onQuickAdvance,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Advance Player",
                            tint = VelorixAccent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                if (score != null && hasPlayer) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.White.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = score.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Score Recording Dialog for updating round match scores
 */
@Composable
fun ScoreRecordDialog(
    match: Match,
    onDismiss: () -> Unit,
    onSave: (winnerId: String, score1: Int, score2: Int) -> Unit
) {
    var score1Text by remember { mutableStateOf(match.score1?.toString() ?: "0") }
    var score2Text by remember { mutableStateOf(match.score2?.toString() ?: "0") }
    var selectedWinnerId by remember { mutableStateOf(match.winnerId ?: match.player1Id ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Match #${match.matchNumber} Score & Winner", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter match scores and choose the winning participant to record in Firestore:", fontSize = 12.sp, color = Color(0xFFA1A1AA))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(match.player1Id ?: "Player 1", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = score1Text,
                            onValueChange = { score1Text = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = VelorixAccent,
                                unfocusedBorderColor = Color(0xFF27272A)
                            )
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(match.player2Id ?: "Player 2", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = score2Text,
                            onValueChange = { score2Text = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = VelorixAccent,
                                unfocusedBorderColor = Color(0xFF27272A)
                            )
                        )
                    }
                }

                Text("Select Winner:", fontSize = 11.sp, color = Color(0xFFA1A1AA), fontWeight = FontWeight.Bold)
                if (match.player1Id != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedWinnerId = match.player1Id!! }
                    ) {
                        RadioButton(
                            selected = selectedWinnerId == match.player1Id,
                            onClick = { selectedWinnerId = match.player1Id!! },
                            colors = RadioButtonDefaults.colors(selectedColor = VelorixAccent)
                        )
                        Text(match.player1Id!!, color = Color.White, fontSize = 12.sp)
                    }
                }
                if (match.player2Id != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedWinnerId = match.player2Id!! }
                    ) {
                        RadioButton(
                            selected = selectedWinnerId == match.player2Id,
                            onClick = { selectedWinnerId = match.player2Id!! },
                            colors = RadioButtonDefaults.colors(selectedColor = VelorixAccent)
                        )
                        Text(match.player2Id!!, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s1 = score1Text.toIntOrNull() ?: 0
                    val s2 = score2Text.toIntOrNull() ?: 0
                    onSave(selectedWinnerId, s1, s2)
                },
                colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent)
            ) {
                Text("Save to Firestore", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFA1A1AA))
            }
        },
        containerColor = Color(0xFF141418),
        shape = RoundedCornerShape(16.dp)
    )
}
