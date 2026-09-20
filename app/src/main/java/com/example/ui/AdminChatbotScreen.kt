package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.GeminiModelOption
import com.example.data.repository.GeminiModelRegistry
import com.example.data.repository.GeminiRepository
import com.example.data.repository.GeminiRepositoryImpl
import com.example.data.repository.TournamentRepositoryImpl
import com.example.data.validation.SecuritySanitizer
import com.example.data.validation.UserRateLimiter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.*
import com.example.ui.common.GeminiLogo
import com.example.ui.theme.CardLiveBg
import com.example.ui.theme.CardVerifyBorder
import com.example.ui.theme.VelorixAccent
import com.example.ui.theme.VelorixAccentLight
import com.example.ui.theme.VelorixBg
import com.example.ui.theme.VelorixTextPrimary
import com.example.ui.theme.VelorixTextSecondary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(
    val id: String = System.currentTimeMillis().toString() + "_" + (0..999).random(),
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val modelUsed: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val queryCategory: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminChatbotScreen(
    onNavigateBack: () -> Unit,
    tournamentRepository: TournamentRepositoryImpl = remember { TournamentRepositoryImpl() },
    geminiRepository: GeminiRepository = remember { GeminiRepositoryImpl() }
) {
    val initialMessage = remember {
        ChatMessage(
            text = "**Welcome to Velorix Gemini Admin Assistant**!\n\nI am connected directly to your real-time database. You can query live **Tournaments**, inspect **System Audit Logs**, evaluate **Dispute Tickets**, or review **Pending Payouts**.\n\n*Select any quick action below or type your custom admin query.*",
            isUser = false
        )
    }

    var messages by remember { mutableStateOf(listOf(initialMessage)) }
    var currentInput by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf(GeminiModelRegistry.AVAILABLE_MODELS.first()) }
    var enableThinking by remember { mutableStateOf(false) }
    var showModelBottomSheet by remember { mutableStateOf(false) }
    var showLiveInspectorSheet by remember { mutableStateOf(false) }
    var activeInspectorTab by remember { mutableIntStateOf(0) }

    val chatCooldownSeconds by UserRateLimiter.observeCooldownSeconds(UserRateLimiter.ActionType.GEMINI_CHAT_MESSAGE).collectAsStateWithLifecycle()

    // Live Real-Time State streams
    var liveTournaments by remember { mutableStateOf<List<Tournament>>(emptyList()) }
    var liveAuditLogs by remember { mutableStateOf<List<AuditLogEntry>>(emptyList()) }
    var liveSupportTickets by remember { mutableStateOf<List<SupportTicket>>(emptyList()) }
    var livePayoutRequests by remember { mutableStateOf<List<PayoutRequest>>(emptyList()) }
    var liveUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var liveBannedUsers by remember { mutableStateOf<Map<String, BannedUserRecord>>(emptyMap()) }
    var isRealtimeConnected by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    // Multi-turn conversation history
    val conversationHistory = remember { mutableStateListOf<Content>() }

    // Collect real-time streams
    LaunchedEffect(Unit) {
        launch {
            try {
                tournamentRepository.getLiveTournamentsStream().collectLatest { tournaments ->
                    liveTournaments = tournaments
                }
            } catch (_: Exception) {}
        }
        launch {
            try {
                tournamentRepository.getLiveAuditLogsStream().collectLatest { logs ->
                    liveAuditLogs = logs
                }
            } catch (_: Exception) {}
        }
        launch {
            try {
                tournamentRepository.getLiveSupportTicketsStream().collectLatest { tickets ->
                    liveSupportTickets = tickets
                }
            } catch (_: Exception) {}
        }
        launch {
            try {
                tournamentRepository.getLivePayoutRequestsStream().collectLatest { payouts ->
                    livePayoutRequests = payouts
                }
            } catch (_: Exception) {}
        }
        launch {
            try {
                tournamentRepository.getLiveUsersStream().collectLatest { users ->
                    liveUsers = users
                }
            } catch (_: Exception) {}
        }
        launch {
            try {
                tournamentRepository.getLiveBannedUsersStream().collectLatest { banned ->
                    liveBannedUsers = banned
                }
            } catch (_: Exception) {}
        }
    }

    // Real-Time Context Generator to supply Gemini with ground-truth system state
    fun buildRealtimeSystemContext(): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val upcomingTournaments = liveTournaments.filter { it.status.equals("UPCOMING", ignoreCase = true) }
        val liveCount = liveTournaments.count { it.status.equals("LIVE", ignoreCase = true) }
        val completedCount = liveTournaments.count { it.status.equals("COMPLETED", ignoreCase = true) }
        val openTickets = liveSupportTickets.filter { it.status.equals("open", ignoreCase = true) || it.status.equals("in_progress", ignoreCase = true) }
        val pendingPayouts = livePayoutRequests.filter { it.status.equals("pending", ignoreCase = true) }
        val totalPendingAmount = pendingPayouts.sumOf { it.amount }

        val tournamentSummaries = liveTournaments.take(15).joinToString("\n") { t ->
            "- [${t.status}] \"${t.title}\" | Map: ${t.map} | Format: ${t.format} | Fee: ₹${t.entryFee} | Prize: ₹${t.prizePool} | Slots: ${t.registeredPlayers}/${t.maxPlayers} | Room: ${if (t.roomDetails?.roomId?.isNotBlank() == true) "Configured (ID: ${t.roomDetails?.roomId})" else "Not Set"}"
        }

        val auditLogSummaries = liveAuditLogs.take(10).joinToString("\n") { log ->
            val time = try { dateFormat.format(Date(log.timestamp)) } catch (_: Exception) { "${log.timestamp}" }
            "- [$time] [${log.category}] ${log.action} by ${log.performedBy}: ${log.details}"
        }

        val ticketSummaries = openTickets.take(8).joinToString("\n") { tk ->
            "- Ticket ${tk.id}: [${tk.issueCategory}] Priority: ${if (tk.isHighPriority) "URGENT" else "Normal"} | User: ${tk.userEmail} | Description: ${tk.description.take(80)}"
        }

        val payoutSummaries = pendingPayouts.take(8).joinToString("\n") { p ->
            "- Payout ${p.id}: ₹${p.amount} via ${p.paymentMethod} (${p.paymentId.ifBlank { p.upiId }}) for ${p.username.ifBlank { p.email }}"
        }

        return """
            You are the official Velorix Real-Time eSports Admin AI Assistant.
            You have direct read-access to the live Firebase database. Here is the verified real-time platform snapshot right now:

            === REAL-TIME PLATFORM SNAPSHOT ===
            • Total Tournaments: ${liveTournaments.size} (Live: $liveCount, Upcoming: ${upcomingTournaments.size}, Completed: $completedCount)
            • Total Registered Players: ${liveUsers.size}
            • Banned Accounts: ${liveBannedUsers.size}
            • Open Dispute/Support Tickets: ${openTickets.size}
            • Pending Cashout Requests: ${pendingPayouts.size} (Total Value: ₹$totalPendingAmount)
            • System Audit Logs Count: ${liveAuditLogs.size}

            === RECENT TOURNAMENTS LIST ===
            ${tournamentSummaries.ifBlank { "No active tournaments created yet." }}

            === RECENT SYSTEM AUDIT LOGS ===
            ${auditLogSummaries.ifBlank { "No security or system audit logs recorded recently." }}

            === OPEN DISPUTES & TICKETS ===
            ${ticketSummaries.ifBlank { "No open complaints or dispute tickets currently pending." }}

            === PENDING CASHOUT REQUESTS ===
            ${payoutSummaries.ifBlank { "No pending withdrawal requests." }}

            === INSTRUCTIONS ===
            1. Answer the administrator's questions using the verified real-time data provided above.
            2. When queried about tournament statuses, schedules, rules, or room IDs, provide accurate details directly from the snapshot.
            3. When queried about system logs, provide clear timestamps, actions taken, and who performed them.
            4. Format output using clean Markdown with bolding, bullet points, and high-clarity structure.
        """.trimIndent()
    }

    fun sendPrompt(userText: String, category: String? = null, isDisputeLogging: Boolean = false) {
        if (userText.isBlank()) return
        val rateCheck = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.GEMINI_CHAT_MESSAGE)
        if (!rateCheck.isAllowed) {
            messages = messages + ChatMessage(
                text = "⏳ Rate Limit Active: ${rateCheck.reasonMessage}",
                isUser = false,
                isError = true
            )
            return
        }
        val activeModel = selectedModel
        val cleanText = SecuritySanitizer.sanitizeInput(userText.trim(), maxLength = 2000)
        if (cleanText.isBlank()) return
        
        val promptToSend = cleanText
        currentInput = ""
        
        messages = messages + ChatMessage(
            text = promptToSend,
            isUser = true,
            queryCategory = category
        )
        val loadingMessageId = "loading_${System.currentTimeMillis()}"
        messages = messages + ChatMessage(
            id = loadingMessageId,
            text = "",
            isUser = false,
            isLoading = true,
            modelUsed = activeModel.displayName
        )
        
        conversationHistory.add(Content(parts = listOf(Part(text = promptToSend)), role = "user"))
        
        // Log action to Realtime Database Audit Trail
        coroutineScope.launch {
            try {
                tournamentRepository.logAuditAction(
                    action = "AI Admin Query: ${category ?: "Custom Query"}",
                    details = promptToSend.take(120),
                    category = "AI_HELPER"
                )
            } catch (_: Exception) {}
        }

        if (isDisputeLogging) {
            val currUser = FirebaseAuth.getInstance().currentUser
            val userEmail = currUser?.email?.ifBlank { null } ?: "anantisback47@gmail.com"
            val disputeRateCheck = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.SUPPORT_TICKET_SUBMISSION, userEmail)
            if (disputeRateCheck.isAllowed) {
                val ticketId = "TK-CB-${System.currentTimeMillis().toString().takeLast(5)}"
                val username = currUser?.displayName?.ifBlank { null } ?: userEmail.substringBefore("@")
                val ticket = ComplaintTicket(
                    id = ticketId,
                    userEmail = SecuritySanitizer.sanitizeEmail(userEmail),
                    username = SecuritySanitizer.sanitizeInput(username, maxLength = 40),
                    gameId = "IGN_ADMIN_DESK",
                    tournamentTitle = "Gemini Support Desk",
                    issueCategory = "Urgent Dispute Ticket",
                    description = promptToSend,
                    status = "open",
                    updatedAt = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis(),
                    isHighPriority = true
                )
                try {
                    val db = try {
                        FirebaseDatabase.getInstance("https://velorix-tournaments-default-rtdb.asia-southeast1.firebasedatabase.app").reference
                    } catch (_: Exception) {
                        FirebaseDatabase.getInstance().reference
                    }
                    val fs = FirebaseFirestore.getInstance()
                    val safeTicketId = SecuritySanitizer.sanitizeDatabaseKey(ticketId)
                    db.child("complaints").child(safeTicketId).setValue(ticket)
                    db.child("chatbot_complaints").child(safeTicketId).setValue(ticket)
                    fs.collection("complaints").document(safeTicketId).set(ticket)
                } catch (_: Exception) {}
            }
        }

        coroutineScope.launch {
            listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0))
            val realtimeInstruction = buildRealtimeSystemContext()
            
            val result = geminiRepository.sendChatMessage(
                history = conversationHistory.toList(),
                systemInstruction = realtimeInstruction,
                model = activeModel.id,
                enableThinking = enableThinking
            )

            result.fold(
                onSuccess = { reply ->
                    conversationHistory.add(Content(parts = listOf(Part(text = reply)), role = "model"))
                    messages = messages.filterNot { it.id == loadingMessageId } + ChatMessage(
                        text = reply,
                        isUser = false,
                        modelUsed = activeModel.displayName,
                        queryCategory = category
                    )
                },
                onFailure = { error ->
                    val errorMsg = error.message ?: "Failed to get response from Gemini."
                    messages = messages.filterNot { it.id == loadingMessageId } + ChatMessage(
                        text = errorMsg,
                        isUser = false,
                        isError = true,
                        modelUsed = activeModel.displayName
                    )
                }
            )
            listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GeminiLogo(size = 20.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gemini Helper Bot",
                                color = VelorixTextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(Color(0xFF10B981), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "LIVE DATA",
                                        color = Color(0xFF34D399),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showModelBottomSheet = true }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "${selectedModel.displayName}${if (enableThinking) " • Thinking Mode" else ""}",
                                color = VelorixAccentLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Change Model",
                                tint = VelorixAccentLight,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = VelorixTextPrimary)
                    }
                },
                actions = {
                    // Live Database Inspector button
                    IconButton(
                        onClick = { showLiveInspectorSheet = true },
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Inspect Live System Context",
                            tint = VelorixAccent
                        )
                    }
                    // Toggle Thinking mode
                    IconButton(
                        onClick = { enableThinking = !enableThinking },
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Toggle Thinking Mode",
                            tint = if (enableThinking) VelorixAccent else Color.Gray
                        )
                    }
                    // Clear conversation
                    IconButton(onClick = {
                        conversationHistory.clear()
                        messages = listOf(initialMessage)
                        android.widget.Toast.makeText(context, "Chat history reset", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0F12))
            )
        },
        containerColor = Color(0xFF0B0B0E)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Snapshot Status Bar
            Surface(
                color = CardLiveBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardVerifyBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBadge(label = "Tournaments", value = "${liveTournaments.size}", color = VelorixAccent)
                        StatBadge(label = "Audit Logs", value = "${liveAuditLogs.size}", color = Color(0xFF818CF8))
                        StatBadge(label = "Open Tickets", value = "${liveSupportTickets.count { it.status.equals("open", ignoreCase = true) }}", color = Color(0xFFF87171))
                    }
                    TextButton(
                        onClick = { showLiveInspectorSheet = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text("View Data", color = VelorixAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Real-Time Query Hub Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    ActionQueryChip(
                        icon = Icons.Default.EmojiEvents,
                        title = "Live Tournaments",
                        color = Color(0xFFFBBF24),
                        onClick = {
                            sendPrompt(
                                userText = "Analyze and summarize all active, upcoming, and completed Free Fire tournaments from real-time database. Include slot fill percentages, formats, prize pool allocations, and room ID statuses.",
                                category = "Tournaments"
                            )
                        }
                    )
                }
                item {
                    ActionQueryChip(
                        icon = Icons.Default.Description,
                        title = "System Logs",
                        color = Color(0xFF818CF8),
                        onClick = {
                            sendPrompt(
                                userText = "Query and summarize the latest 10 system audit logs and administrator actions from database. Highlight any security events, role grants, bans, or config changes.",
                                category = "Audit Logs"
                            )
                        }
                    )
                }
                item {
                    ActionQueryChip(
                        icon = Icons.Default.Warning,
                        title = "Open Disputes",
                        color = Color(0xFFF87171),
                        onClick = {
                            sendPrompt(
                                userText = "Give me a breakdown of all open support tickets and player complaints. Highlight high-priority issues, room ID errors, and recommended adjudications.",
                                category = "Disputes"
                            )
                        }
                    )
                }
                item {
                    ActionQueryChip(
                        icon = Icons.Default.Payments,
                        title = "Cashout Audit",
                        color = Color(0xFF34D399),
                        onClick = {
                            sendPrompt(
                                userText = "Audit all pending payout and withdrawal requests. Summarize total pending VT amounts, payment channels (UPI/Paytm), and flag any suspicious cashout attempts.",
                                category = "Payouts"
                            )
                        }
                    )
                }
                item {
                    ActionQueryChip(
                        icon = Icons.Default.Shield,
                        title = "Anti-Cheat Roster",
                        color = Color(0xFFE879F9),
                        onClick = {
                            sendPrompt(
                                userText = "Review the banned player directory and security blacklist. List banned UIDs, reasons, and recommend anti-cheat enforcement checks.",
                                category = "Anti-Cheat"
                            )
                        }
                    )
                }
                item {
                    ActionQueryChip(
                        icon = Icons.Default.Bolt,
                        title = "Platform Health",
                        color = Color(0xFF38BDF8),
                        onClick = {
                            sendPrompt(
                                userText = "Perform a complete system health-check on platform metrics: active matches, player registration capacity, and database sync status.",
                                category = "Health Check"
                            )
                        }
                    )
                }
            }

            // Message Stream List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { "${it.id}_${messages.indexOf(it)}" }) { message ->
                    EnhancedChatBubble(
                        message = message,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(message.text))
                            android.widget.Toast.makeText(context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onRetry = {
                            sendPrompt(message.text, category = message.queryCategory)
                        }
                    )
                }
            }

            // Bottom Input Bar
            Surface(
                color = Color(0xFF141418),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26262E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (chatCooldownSeconds > 0L) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1F1F2C))
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Rate Limit Protection Active: Next prompt in ${chatCooldownSeconds}s",
                                color = VelorixAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = currentInput,
                            onValueChange = { currentInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    "Query tournaments, logs, tickets (${selectedModel.displayName})...",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VelorixAccent,
                                unfocusedBorderColor = Color(0xFF33333F),
                                focusedTextColor = VelorixTextPrimary,
                                unfocusedTextColor = VelorixTextPrimary,
                                focusedContainerColor = Color(0xFF1A1A22),
                                unfocusedContainerColor = Color(0xFF1A1A22)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (currentInput.isNotBlank() && chatCooldownSeconds == 0L) {
                                    sendPrompt(currentInput)
                                }
                            },
                            enabled = currentInput.isNotBlank() && chatCooldownSeconds == 0L,
                            modifier = Modifier
                                .size(46.dp)
                                .background(
                                    if (chatCooldownSeconds > 0L) Brush.linearGradient(listOf(Color(0xFF374151), Color(0xFF1F2937)))
                                    else Brush.linearGradient(listOf(VelorixAccent, Color(0xFF6366F1))),
                                    CircleShape
                                )
                        ) {
                            if (chatCooldownSeconds > 0L) {
                                Text("${chatCooldownSeconds}s", color = Color(0xFF9CA3AF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color.Black, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Live Database Context Inspector Sheet
    if (showLiveInspectorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLiveInspectorSheet = false },
            containerColor = Color(0xFF121217),
            scrimColor = Color.Black.copy(alpha = 0.7f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Real-Time System Inspector",
                            color = VelorixTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { showLiveInspectorSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
                Text(
                    text = "Live database snapshot currently accessible to Gemini Helper Bot.",
                    color = VelorixTextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Inspector Tabs
                TabRow(
                    selectedTabIndex = activeInspectorTab,
                    containerColor = Color(0xFF1E1E26),
                    contentColor = VelorixAccent,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeInspectorTab]),
                            color = VelorixAccent
                        )
                    }
                ) {
                    Tab(
                        selected = activeInspectorTab == 0,
                        onClick = { activeInspectorTab = 0 },
                        text = { Text("Tournaments (${liveTournaments.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeInspectorTab == 1,
                        onClick = { activeInspectorTab = 1 },
                        text = { Text("Audit Logs (${liveAuditLogs.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeInspectorTab == 2,
                        onClick = { activeInspectorTab = 2 },
                        text = { Text("Tickets (${liveSupportTickets.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeInspectorTab == 3,
                        onClick = { activeInspectorTab = 3 },
                        text = { Text("Payouts (${livePayoutRequests.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    when (activeInspectorTab) {
                        0 -> {
                            if (liveTournaments.isEmpty()) {
                                EmptyInspectorState("No tournaments found in Realtime Database.")
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(liveTournaments) { t ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF1B1B24),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B2B36)),
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                showLiveInspectorSheet = false
                                                sendPrompt("Provide complete details and rule setup for tournament: ${t.title} (ID: ${t.id})", category = "Tournament Query")
                                            }
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(t.title, color = VelorixTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text(
                                                        t.status,
                                                        color = if (t.status == "LIVE") Color(0xFF10B981) else Color(0xFFF59E0B),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    "Map: ${t.map} • Format: ${t.format} • Prize: ₹${t.prizePool} • Slots: ${t.registeredPlayers}/${t.maxPlayers}",
                                                    color = VelorixTextSecondary,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            if (liveAuditLogs.isEmpty()) {
                                EmptyInspectorState("No audit logs recorded yet.")
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(liveAuditLogs) { log ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF1B1B24),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B2B36)),
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                showLiveInspectorSheet = false
                                                sendPrompt("Explain security audit log entry: ${log.action} performed by ${log.performedBy} (${log.details})", category = "Log Analysis")
                                            }
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(log.action, color = Color(0xFF818CF8), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(log.category, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(log.details, color = VelorixTextPrimary, fontSize = 12.sp)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("By: ${log.performedBy}", color = VelorixTextSecondary, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            if (liveSupportTickets.isEmpty()) {
                                EmptyInspectorState("No support tickets submitted.")
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(liveSupportTickets) { tk ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF1B1B24),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B2B36)),
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                showLiveInspectorSheet = false
                                                sendPrompt("Help me adjudicate dispute ticket ${tk.id}: Issue '${tk.issueCategory}' from ${tk.userEmail} - ${tk.description}", category = "Dispute Resolution")
                                            }
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("Ticket ${tk.id}: ${tk.issueCategory}", color = VelorixTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(
                                                        tk.status.uppercase(),
                                                        color = if (tk.status == "open") Color(0xFFF87171) else Color(0xFF34D399),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(tk.description, color = VelorixTextSecondary, fontSize = 12.sp, maxLines = 2)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        3 -> {
                            if (livePayoutRequests.isEmpty()) {
                                EmptyInspectorState("No payout requests pending.")
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(livePayoutRequests) { p ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF1B1B24),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B2B36)),
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                showLiveInspectorSheet = false
                                                sendPrompt("Verify payout request ${p.id} of ₹${p.amount} via ${p.paymentMethod} for user ${p.username.ifBlank { p.email }}", category = "Payout Verification")
                                            }
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("₹${p.amount} • ${p.paymentMethod}", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text(p.status.uppercase(), color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Recipient: ${p.paymentId.ifBlank { p.upiId }} (${p.username})", color = VelorixTextSecondary, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Model Selector Bottom Sheet
    if (showModelBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModelBottomSheet = false },
            containerColor = Color(0xFF14141A),
            scrimColor = Color.Black.copy(alpha = 0.7f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Select Gemini AI Model",
                    color = VelorixTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose from Google DeepMind's ultra-fast Flash models or deep Pro reasoning models.",
                    color = VelorixTextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(GeminiModelRegistry.AVAILABLE_MODELS) { modelOption ->
                        val isSelected = selectedModel.id == modelOption.id
                        Surface(
                            onClick = {
                                selectedModel = modelOption
                                showModelBottomSheet = false
                                android.widget.Toast.makeText(context, "Switched to ${modelOption.displayName}", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) VelorixAccent.copy(alpha = 0.15f) else Color(0xFF1E1E26),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) VelorixAccent else Color.DarkGray.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = modelOption.displayName,
                                            color = if (isSelected) VelorixAccentLight else VelorixTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF2E2E3A), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = modelOption.category,
                                                color = Color.LightGray,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = modelOption.description,
                                        color = VelorixTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = VelorixAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ActionQueryChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF181822),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Text(
                text = title,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatBadge(label: String, value: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$label:",
            color = VelorixTextSecondary,
            fontSize = 11.sp
        )
        Text(
            text = value,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

@Composable
fun EmptyInspectorState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color.Gray,
            fontSize = 13.sp
        )
    }
}

@Composable
fun EnhancedChatBubble(
    message: ChatMessage,
    onCopy: () -> Unit,
    onRetry: () -> Unit
) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = when {
        message.isUser -> VelorixAccent
        message.isError -> Color(0xFF3E1F1F)
        else -> Color(0xFF1E1E26)
    }
    val textColor = when {
        message.isUser -> Color.Black
        message.isError -> Color(0xFFFF8A80)
        else -> VelorixTextPrimary
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 330.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        bgColor,
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (message.isUser) 16.dp else 4.dp,
                            bottomEnd = if (message.isUser) 4.dp else 16.dp
                        )
                    )
                    .then(
                        if (message.isError) {
                            Modifier.border(1.dp, Color(0xFFE57373), RoundedCornerShape(16.dp))
                        } else Modifier.border(1.dp, if (message.isUser) Color.Transparent else Color(0xFF2C2C38), RoundedCornerShape(16.dp))
                    )
                    .padding(14.dp)
            ) {
                if (message.isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = VelorixAccent,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Querying live database & generating...",
                            color = VelorixTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Column {
                        if (!message.isUser && message.modelUsed != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = message.modelUsed,
                                    color = if (message.isError) Color(0xFFFFAB91) else VelorixAccentLight,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (message.queryCategory != null) {
                                    Text(
                                        text = message.queryCategory,
                                        color = Color.LightGray,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Text(
                            text = message.text,
                            color = textColor,
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp,
                            fontFamily = if (message.text.contains("```") || message.text.contains("===")) FontFamily.Monospace else FontFamily.Default
                        )
                    }
                }
            }

            if (!message.isLoading && !message.isUser) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy text",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (message.isError) {
                        IconButton(
                            onClick = onRetry,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = VelorixAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
