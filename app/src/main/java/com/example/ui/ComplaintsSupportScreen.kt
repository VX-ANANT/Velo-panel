package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.ComplaintTicket
import com.example.domain.model.TicketMessage
import com.example.domain.model.UserProfile
import com.example.ui.theme.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintsSupportScreen(
    users: List<UserProfile>,
    initialTickets: List<ComplaintTicket> = emptyList(),
    onAddFundsToUser: (UserProfile, Double) -> Unit,
    onAddFundsBucket: ((UserProfile, Double, com.example.domain.model.WalletBucket, String) -> Unit)? = null,
    onSaveTicket: ((ComplaintTicket) -> Unit)? = null,
    onDeleteTicket: ((String) -> Unit)? = null,
    onSendTicketMessage: ((ticketId: String, message: String, onComplete: (Boolean) -> Unit) -> Unit)? = null,
    getTicketMessagesStream: (suspend (String) -> Flow<List<TicketMessage>>)? = null,
    onIssueTicketCompensation: ((ticket: ComplaintTicket, amount: Double, reason: String, onComplete: (Boolean) -> Unit) -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current

    var tickets by remember(initialTickets) {
        mutableStateOf(initialTickets)
    }

    var filterStatus by remember { mutableStateOf("ALL") }
    var selectedTicketForDetails by remember { mutableStateOf<ComplaintTicket?>(null) }
    var selectedTicketForChat by remember { mutableStateOf<ComplaintTicket?>(null) }
    var showRefundDialog by remember { mutableStateOf<ComplaintTicket?>(null) }
    var ticketToDissolve by remember { mutableStateOf<ComplaintTicket?>(null) }
    var refundAmountInput by remember { mutableStateOf("250") }
    var refundReasonInput by remember { mutableStateOf("Support ticket compensation resolution") }
    var selectedCompensationBucket by remember { mutableStateOf(com.example.domain.model.WalletBucket.DEPOSIT) }

    val filteredTickets = remember(tickets, filterStatus) {
        when (filterStatus) {
            "PENDING" -> tickets.filter { it.status == "PENDING" || it.status == "INVESTIGATING" }
            "RESOLVED" -> tickets.filter { it.status == "RESOLVED" || it.status == "REFUNDED" || it.status == "CLOSED" }
            else -> tickets
        }
    }

    val pendingCount = tickets.count { it.status == "PENDING" || it.status == "INVESTIGATING" }

    // Dissolve Ticket Confirmation Dialog
    ticketToDissolve?.let { ticket ->
        AlertDialog(
            onDismissRequest = { ticketToDissolve = null },
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Dissolve Support Ticket?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        "Are you sure you want to permanently dissolve and delete Ticket #${ticket.id} submitted by ${ticket.username}?",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tournament / Subject: ${ticket.tournamentTitle.ifEmpty { ticket.issueCategory }}",
                        color = VelorixAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "This will purge the ticket from live support queues and databases.",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ticketId = ticket.id
                        tickets = tickets.filter { it.id != ticketId }
                        onDeleteTicket?.invoke(ticketId)
                        ticketToDissolve = null
                        if (selectedTicketForDetails?.id == ticketId) {
                            selectedTicketForDetails = null
                        }
                        if (selectedTicketForChat?.id == ticketId) {
                            selectedTicketForChat = null
                        }
                        Toast.makeText(context, "Ticket #$ticketId permanently dissolved.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Yes, Dissolve Ticket", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { ticketToDissolve = null },
                    border = BorderStroke(1.dp, Color.Gray)
                ) {
                    Text("Cancel / Keep Ticket", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF18181B)
        )
    }

    // Direct Refund / Compensation Dialog
    showRefundDialog?.let { ticket ->
        AlertDialog(
            onDismissRequest = { showRefundDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = VelorixAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Issue Direct Compensation", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Issue compensation directly to ${ticket.username} (${ticket.userEmail})", color = Color.LightGray, fontSize = 12.5.sp)
                    Text("Select Wallet Bucket:", color = Color(0xFFA1A1AA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        com.example.domain.model.WalletBucket.values().forEach { bucketOpt ->
                            val isSel = selectedCompensationBucket == bucketOpt
                            val bucketAccent = when (bucketOpt) {
                                com.example.domain.model.WalletBucket.WINNING -> Color(0xFF30D158)
                                com.example.domain.model.WalletBucket.DEPOSIT -> Color(0xFF0A84FF)
                                com.example.domain.model.WalletBucket.BONUS -> Color(0xFFFFD60A)
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedCompensationBucket = bucketOpt }
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = bucketOpt.shortLabel.uppercase(),
                                    color = if (isSel) bucketAccent else Color(0xFF71717A),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = if (bucketOpt.isWithdrawable) "Withdrawable" else "Locked",
                                    color = if (isSel) Color.White else Color(0xFF52525B),
                                    fontSize = 9.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.75f)
                                        .height(1.5.dp)
                                        .background(if (isSel) bucketAccent else Color.Transparent)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = refundAmountInput,
                        onValueChange = { refundAmountInput = it },
                        label = { Text("Amount in INR (₹) -> ${selectedCompensationBucket.shortLabel}", color = V14TextMuted) },
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
                        value = refundReasonInput,
                        onValueChange = { refundReasonInput = it },
                        label = { Text("Compensation Reason", color = V14TextMuted) },
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
                        val amount = refundAmountInput.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            val matchedUser = users.find { it.email.equals(ticket.userEmail, ignoreCase = true) || it.id == ticket.userId }
                            if (matchedUser != null && onAddFundsBucket != null) {
                                onAddFundsBucket(matchedUser, amount, selectedCompensationBucket, "$refundReasonInput (Ticket #${ticket.id})")
                                val updated = ticket.copy(
                                    status = "RESOLVED",
                                    adminNote = "Compensated ₹$amount to ${selectedCompensationBucket.displayName}: $refundReasonInput"
                                )
                                tickets = tickets.map { if (it.id == ticket.id) updated else it }
                                onSaveTicket?.invoke(updated)
                            } else if (onIssueTicketCompensation != null) {
                                onIssueTicketCompensation(ticket, amount, "$refundReasonInput [${selectedCompensationBucket.shortLabel}]") { ok ->
                                    if (ok) {
                                        val updated = ticket.copy(status = "RESOLVED", adminNote = "Compensated ₹$amount (${selectedCompensationBucket.shortLabel})")
                                        tickets = tickets.map { if (it.id == ticket.id) updated else it }
                                    }
                                }
                            } else if (matchedUser != null) {
                                onAddFundsToUser(matchedUser, amount)
                                val updated = ticket.copy(status = "REFUNDED", adminNote = "Refunded ₹$amount: $refundReasonInput")
                                tickets = tickets.map { if (it.id == ticket.id) updated else it }
                                onSaveTicket?.invoke(updated)
                            }
                            Toast.makeText(context, "₹$amount credited to ${selectedCompensationBucket.displayName} for ${ticket.username}!", Toast.LENGTH_LONG).show()
                        }
                        showRefundDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = V14AccentPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Credit ${selectedCompensationBucket.shortLabel}", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRefundDialog = null }) {
                    Text("Cancel", color = V14TextSecondary)
                }
            },
            containerColor = V14CardSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Real-Time Live Chat Dialog
    selectedTicketForChat?.let { ticket ->
        TicketLiveChatDialog(
            ticket = ticket,
            user = users.find { it.email.equals(ticket.userEmail, ignoreCase = true) || it.id == ticket.userId },
            getTicketMessagesStream = getTicketMessagesStream,
            onSendMessage = { text ->
                onSendTicketMessage?.invoke(ticket.id, text) { success ->
                    if (success) {
                        val updated = ticket.copy(status = if (ticket.status == "PENDING") "INVESTIGATING" else ticket.status)
                        tickets = tickets.map { if (it.id == ticket.id) updated else it }
                        onSaveTicket?.invoke(updated)
                    }
                }
            },
            onDismiss = { selectedTicketForChat = null },
            onIssueRefund = {
                showRefundDialog = ticket
            },
            onUpdateStatus = { nextStatus ->
                val updated = ticket.copy(status = nextStatus)
                tickets = tickets.map { if (it.id == ticket.id) updated else it }
                onSaveTicket?.invoke(updated)
                selectedTicketForChat = updated
            }
        )
    }

    // Comprehensive Ticket Detail & Adjudication Dialog
    selectedTicketForDetails?.let { ticket ->
        ComplaintTicketDetailDialog(
            ticket = ticket,
            user = users.find { it.email.equals(ticket.userEmail, ignoreCase = true) },
            onDismiss = { selectedTicketForDetails = null },
            onOpenLiveChat = {
                selectedTicketForDetails = null
                selectedTicketForChat = ticket
            },
            onDirectEmail = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:${ticket.userEmail}")
                    putExtra(Intent.EXTRA_SUBJECT, "Velorix Esports Support - Ticket #${ticket.id}")
                    putExtra(Intent.EXTRA_TEXT, "Hello ${ticket.username},\n\nRegarding your ticket for ${ticket.tournamentTitle} (${ticket.issueCategory}):\n\n")
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "No email client app found.", Toast.LENGTH_SHORT).show()
                }
            },
            onIssueRefund = {
                selectedTicketForDetails = null
                showRefundDialog = ticket
            },
            onDissolveTicket = {
                selectedTicketForDetails = null
                ticketToDissolve = ticket
            },
            onUpdateStatus = { nextStatus, note ->
                val updated = ticket.copy(status = nextStatus, adminNote = note)
                tickets = tickets.map {
                    if (it.id == ticket.id) updated else it
                }
                onSaveTicket?.invoke(updated)
                selectedTicketForDetails = updated
                Toast.makeText(context, "Ticket updated to $nextStatus", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Robust Header with perfect alignment
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onNavigateBack != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(V14CardSurface)
                            .border(1.dp, V14Border, CircleShape)
                            .clickable(onClick = onNavigateBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column {
                    Text(
                        text = "Support Complaints Desk",
                        color = V14TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Real-time ticket resolution & live support chat",
                        color = V14TextSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = V14AccentPrimary.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, V14AccentBright.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "$pendingCount Pending",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = V14AccentBright,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Quick Stats row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatBadge(modifier = Modifier.weight(1f), label = "Total Tickets", value = "${tickets.size}", icon = Icons.Default.ConfirmationNumber, color = V14AccentBright)
            StatBadge(modifier = Modifier.weight(1f), label = "Pending Action", value = "$pendingCount", icon = Icons.Default.Warning, color = VelorixAmber)
            StatBadge(modifier = Modifier.weight(1f), label = "Resolved", value = "${tickets.count { it.status == "RESOLVED" || it.status == "REFUNDED" }}", icon = Icons.Default.CheckCircle, color = VelorixEmerald)
        }

        // Filter tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipTab("ALL", filterStatus == "ALL") { filterStatus = "ALL" }
            FilterChipTab("PENDING ($pendingCount)", filterStatus == "PENDING") { filterStatus = "PENDING" }
            FilterChipTab("RESOLVED", filterStatus == "RESOLVED") { filterStatus = "RESOLVED" }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 90.dp)
        ) {
            if (filteredTickets.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No tickets found in this category.", color = Color.Gray)
                    }
                }
            }
            items(filteredTickets) { ticket ->
                ComplaintCard(
                    ticket = ticket,
                    onClick = {
                        selectedTicketForDetails = ticket
                    },
                    onLiveChat = {
                        selectedTicketForChat = ticket
                    },
                    onDirectEmail = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:${ticket.userEmail}")
                            putExtra(Intent.EXTRA_SUBJECT, "Velorix Esports Support - Ticket #${ticket.id}")
                            putExtra(Intent.EXTRA_TEXT, "Hello ${ticket.username},\n\nRegarding your complaint for ${ticket.tournamentTitle} (${ticket.issueCategory}):\n\n")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No email app found to send email", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onIssueRefund = {
                        showRefundDialog = ticket
                    },
                    onToggleResolve = {
                        val isResolved = ticket.status.equals("RESOLVED", ignoreCase = true) || ticket.status.equals("CLOSED", ignoreCase = true)
                        val nextStatus = if (isResolved) "PENDING" else "RESOLVED"
                        val updated = ticket.copy(status = nextStatus)
                        tickets = tickets.map {
                            if (it.id == ticket.id) updated else it
                        }
                        onSaveTicket?.invoke(updated)
                        Toast.makeText(context, if (nextStatus == "RESOLVED") "Ticket #${ticket.id} Resolved!" else "Ticket #${ticket.id} Reopened as Pending.", Toast.LENGTH_SHORT).show()
                    },
                    onDissolve = {
                        ticketToDissolve = ticket
                    }
                )
            }
        }
    }
}

@Composable
fun StatBadge(modifier: Modifier = Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Column(
        modifier = modifier
            .background(Color.Black)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label.uppercase(), color = Color.Gray, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
    }
}

@Composable
fun FilterChipTab(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.uppercase(),
            color = if (isSelected) VelorixAccent else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 0.6.sp
        )
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(2.dp)
                .background(if (isSelected) VelorixAccent else Color.Transparent)
        )
    }
}

@Composable
fun ComplaintCard(
    ticket: ComplaintTicket,
    onClick: () -> Unit,
    onLiveChat: () -> Unit,
    onDirectEmail: () -> Unit,
    onIssueRefund: () -> Unit,
    onToggleResolve: () -> Unit,
    onDissolve: () -> Unit
) {
    val isResolved = ticket.status.equals("RESOLVED", ignoreCase = true) || ticket.status.equals("CLOSED", ignoreCase = true)
    val statusTextColor = when {
        isResolved -> Color(0xFF4ADE80)
        ticket.status == "INVESTIGATING" -> Color(0xFFFFB74D)
        ticket.status == "REFUNDED" -> VelorixAccent
        else -> Color(0xFFF87171)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#${ticket.id}", color = VelorixAccent, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    if (ticket.isHighPriority) {
                        Text(
                            text = "  •  HIGH PRIORITY",
                            color = Color(0xFFEF4444),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                Text(
                    text = ticket.status.uppercase(),
                    color = statusTextColor,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${ticket.username} (${ticket.gameId.ifEmpty { "No IGN" }})",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(ticket.userEmail, color = Color.Gray, fontSize = 11.5.sp)

            Spacer(modifier = Modifier.height(8.dp))

            // Flat inline tournament + category row (zero nested box)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = ticket.tournamentTitle.ifEmpty { "General Tournament Inquiry" },
                        color = VelorixTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = ticket.issueCategory.uppercase(),
                    color = V14TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = ticket.description,
                color = V14TextSecondary,
                fontSize = 12.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Flat inline action bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.clickable { onLiveChat() }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = V14TextPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("LIVE CHAT", fontSize = 11.sp, color = V14TextPrimary, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.clickable { onIssueRefund() }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("₹ CREDIT", fontSize = 11.sp, color = VelorixAccent, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.clickable { onToggleResolve() }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isResolved) Icons.Default.Refresh else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isResolved) Color.Gray else Color(0xFF4ADE80),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isResolved) "REOPEN" else "RESOLVE",
                            fontSize = 11.sp,
                            color = if (isResolved) Color.Gray else Color(0xFF4ADE80),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onDissolve,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Dissolve Ticket", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
    }
}

/**
 * Real-Time Ticket Live Chat Dialog with flat pure AMOLED black layout
 */
@Composable
fun TicketLiveChatDialog(
    ticket: ComplaintTicket,
    user: UserProfile?,
    getTicketMessagesStream: (suspend (String) -> Flow<List<TicketMessage>>)?,
    onSendMessage: (String) -> Unit,
    onDismiss: () -> Unit,
    onIssueRefund: () -> Unit,
    onUpdateStatus: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf<List<TicketMessage>>(emptyList()) }

    LaunchedEffect(ticket.id) {
        if (getTicketMessagesStream != null) {
            getTicketMessagesStream(ticket.id).collect { newMessages ->
                messages = newMessages
                if (newMessages.isNotEmpty()) {
                    listState.animateScrollToItem(newMessages.size - 1)
                }
            }
        }
    }

    val quickReplies = listOf(
        "Checking match logs now...",
        "Room ID & Pass sent to your game mailbox.",
        "Screenshot verified. Thank you!",
        "Compensation credited to your wallet.",
        "Ticket resolved. Best of luck for next matches!"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            color = Color.Black,
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // Top Chat Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = V14AccentBright, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = ticket.username.ifEmpty { "Player" },
                                    color = V14TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "  •  ${ticket.status}",
                                    color = when (ticket.status) {
                                        "RESOLVED" -> VelorixEmerald
                                        "INVESTIGATING" -> VelorixAmber
                                        else -> V14AccentBright
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Text(
                                text = "Ticket #${ticket.id} • ${ticket.issueCategory}",
                                color = V14TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onIssueRefund, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Payments, contentDescription = "Refund", tint = V14AccentBright, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = V14TextSecondary, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = V14Border)

                // Initial Complaint Context Section (flat with bottom hairline)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("INITIAL REPORT", color = V14AccentBright, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                        Text(ticket.userEmail, color = V14TextMuted, fontSize = 10.5.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ticket.description,
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                // Real-Time Chat Message Stream
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No messages in this ticket yet.", color = Color.Gray, fontSize = 12.sp)
                                    Text("Type a response below to connect live with ${ticket.username}.", color = Color.DarkGray, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    items(messages) { msg ->
                        ChatMessageBubble(message = msg)
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                // Quick Response Row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(quickReplies) { reply ->
                        Text(
                            text = reply,
                            color = VelorixAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable { onSendMessage(reply) }
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                // Text Input & Send Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Type reply to player...", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = VelorixAccent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            val text = inputText.trim()
                            if (text.isNotBlank()) {
                                onSendMessage(text)
                                inputText = ""
                            }
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = VelorixAccent, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: TicketMessage) {
    val isAdminOrSystem = message.senderRole == "ADMIN" || message.senderRole == "SUPPORT" || message.senderRole == "SYSTEM"
    val timeFormatted = remember(message.timestamp) {
        try {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            sdf.format(Date(message.timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isAdminOrSystem) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (isAdminOrSystem) message.senderName.ifEmpty { "Velorix Support" } else message.senderName.ifEmpty { "Player" },
                color = if (isAdminOrSystem) V14AccentBright else V14TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            if (message.senderRole.isNotBlank()) {
                Text(
                    text = message.senderRole.uppercase(),
                    color = Color.Gray,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (timeFormatted.isNotEmpty()) {
                Text(timeFormatted, color = Color.DarkGray, fontSize = 9.sp)
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = message.messageText,
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier.padding(vertical = 2.dp)
        )
        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.06f))
    }
}

/**
 * Full Detailed Ticket Adjudication Modal Dialog (Flat pure AMOLED black with 0.5.dp hairline dividers)
 */
@Composable
fun ComplaintTicketDetailDialog(
    ticket: ComplaintTicket,
    user: UserProfile?,
    onDismiss: () -> Unit,
    onOpenLiveChat: () -> Unit,
    onDirectEmail: () -> Unit,
    onIssueRefund: () -> Unit,
    onDissolveTicket: () -> Unit,
    onUpdateStatus: (String, String) -> Unit
) {
    val context = LocalContext.current
    var adminNote by remember(ticket.id) { mutableStateOf(ticket.adminNote) }

    val isResolved = ticket.status.equals("RESOLVED", ignoreCase = true) || ticket.status.equals("CLOSED", ignoreCase = true)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.Black,
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.14f))
        ) {
            Column(
                modifier = Modifier
                    .background(Color.Black)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Ticket #${ticket.id}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text("Detailed Adjudication View", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                // Priority & Status Inline Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STATUS: ${ticket.status.uppercase()}",
                        color = when {
                            isResolved -> Color(0xFF4ADE80)
                            ticket.status == "INVESTIGATING" -> Color(0xFFFFB74D)
                            ticket.status == "REFUNDED" -> VelorixAccent
                            else -> Color(0xFFF87171)
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.6.sp
                    )

                    if (ticket.isHighPriority) {
                        Text(
                            text = "CRITICAL / HIGH PRIORITY",
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                // Player Information Section (flat, no nested box)
                Text("PLAYER INFORMATION", color = VelorixAccent, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Name / User", color = Color.Gray, fontSize = 12.sp)
                    Text(ticket.username, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Email", color = Color.Gray, fontSize = 12.sp)
                    Text(ticket.userEmail, color = Color.White, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Game ID / IGN", color = Color.Gray, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(ticket.gameId.ifEmpty { "Not linked" }, color = VelorixAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        if (ticket.gameId.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Game ID", ticket.gameId))
                                    Toast.makeText(context, "Copied Game ID: ${ticket.gameId}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
                if (user != null) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Wallet Balance", color = Color.Gray, fontSize = 12.sp)
                        Text("₹${user.totalWalletBalance.toInt()} (Dep ₹${user.depositFunds.toInt()} | Win ₹${user.winningFunds.toInt()})", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                // Tournament & Issue Description (flat, no nested box)
                Text("ISSUE REPORT", color = V14AccentBright, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Category: ${ticket.issueCategory}", color = V14TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                if (ticket.tournamentTitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Tournament: ${ticket.tournamentTitle}", color = V14TextSecondary, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(ticket.description, color = V14TextSecondary, fontSize = 12.5.sp, lineHeight = 18.sp)

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = V14Border)

                // Admin Adjudication Note Input
                OutlinedTextField(
                    value = adminNote,
                    onValueChange = { adminNote = it },
                    label = { Text("Admin Adjudication Note / Remarks", color = V14TextMuted, fontSize = 12.sp) },
                    placeholder = { Text("Enter internal remarks or resolution justification...", color = V14TextMuted, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
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
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Actions Section
                Button(
                    onClick = onOpenLiveChat,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = V14AccentPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Real-Time Live Support Chat", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onDirectEmail,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = V14TextPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SEND EMAIL", color = V14TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = onIssueRefund,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = V14AccentBright, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("₹ COMPENSATION", color = V14AccentBright, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { onUpdateStatus("INVESTIGATING", adminNote) }) {
                        Text("INVESTIGATING", color = Color(0xFFFFB74D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = {
                            val next = if (isResolved) "PENDING" else "RESOLVED"
                            onUpdateStatus(next, adminNote)
                        }
                    ) {
                        Text(
                            if (isResolved) "REOPEN TICKET" else "MARK RESOLVED",
                            color = if (isResolved) Color.Gray else Color(0xFF4ADE80),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(onClick = onDissolveTicket) {
                        Text("DELETE", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
