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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.model.UserProfile
import com.example.domain.model.ComplaintTicket
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintsSupportScreen(
    users: List<UserProfile>,
    initialTickets: List<ComplaintTicket> = emptyList(),
    onAddFundsToUser: (UserProfile, Double) -> Unit,
    onSaveTicket: ((ComplaintTicket) -> Unit)? = null,
    onDeleteTicket: ((String) -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current

    var tickets by remember(initialTickets) {
        mutableStateOf(initialTickets)
    }

    var filterStatus by remember { mutableStateOf("ALL") }
    var selectedTicketForDetails by remember { mutableStateOf<ComplaintTicket?>(null) }
    var showRefundDialog by remember { mutableStateOf<ComplaintTicket?>(null) }
    var ticketToDissolve by remember { mutableStateOf<ComplaintTicket?>(null) }
    var refundAmountInput by remember { mutableStateOf("250") }

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

    // Direct Refund Dialog
    showRefundDialog?.let { ticket ->
        AlertDialog(
            onDismissRequest = { showRefundDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = VelorixAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Issue Direct Refund / Credit", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("Issue compensation directly to ${ticket.username} (${ticket.userEmail})", color = Color.LightGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = refundAmountInput,
                        onValueChange = { refundAmountInput = it },
                        label = { Text("Amount in INR (₹)", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = VelorixAccent,
                            unfocusedBorderColor = CardVerifyBorder
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
                            val matchedUser = users.find { it.email.equals(ticket.userEmail, ignoreCase = true) }
                            if (matchedUser != null) {
                                onAddFundsToUser(matchedUser, amount)
                            }
                            // Update ticket status to REFUNDED
                            val updated = ticket.copy(status = "REFUNDED")
                            tickets = tickets.map {
                                if (it.id == ticket.id) updated else it
                            }
                            onSaveTicket?.invoke(updated)
                            Toast.makeText(context, "₹$amount credited to ${ticket.username}!", Toast.LENGTH_LONG).show()
                        }
                        showRefundDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent)
                ) {
                    Text("Grant ₹ Credit", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRefundDialog = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = CardLiveBg
        )
    }

    // Comprehensive Ticket Detail & Adjudication Dialog
    selectedTicketForDetails?.let { ticket ->
        ComplaintTicketDetailDialog(
            ticket = ticket,
            user = users.find { it.email.equals(ticket.userEmail, ignoreCase = true) },
            onDismiss = { selectedTicketForDetails = null },
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
        // Robust Header with perfect alignment & no awkward badge wrapping
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
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Column {
                    Text(
                        text = "Support Complaints Desk",
                        color = VelorixTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Direct contact & instant resolution control",
                        color = Color.Gray,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (pendingCount > 0) Color(0xFFEF4444).copy(alpha = 0.16f) else VelorixAccent.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, if (pendingCount > 0) Color(0xFFEF4444).copy(alpha = 0.5f) else VelorixAccent.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "$pendingCount Pending",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = if (pendingCount > 0) Color(0xFFEF4444) else VelorixAccent,
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
            StatBadge(modifier = Modifier.weight(1f), label = "Total Tickets", value = "${tickets.size}", icon = Icons.Default.ConfirmationNumber, color = Color(0xFF64B5F6))
            StatBadge(modifier = Modifier.weight(1f), label = "Pending Action", value = "$pendingCount", icon = Icons.Default.Warning, color = Color(0xFFFFB74D))
            StatBadge(modifier = Modifier.weight(1f), label = "Resolved Today", value = "${tickets.count { it.status == "RESOLVED" || it.status == "REFUNDED" }}", icon = Icons.Default.CheckCircle, color = Color(0xFF81C784))
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
                        Toast.makeText(context, if (nextStatus == "RESOLVED") "Ticket #${ticket.id} Resolved Successfully!" else "Ticket #${ticket.id} Reopened as Pending.", Toast.LENGTH_SHORT).show()
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
            .background(CardLiveBg, RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = Color.Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun FilterChipTab(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) VelorixAccent else CardLiveBg,
                RoundedCornerShape(20.dp)
            )
            .border(1.dp, if (isSelected) VelorixAccent else CardVerifyBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else Color.LightGray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp
        )
    }
}

@Composable
fun ComplaintCard(
    ticket: ComplaintTicket,
    onClick: () -> Unit,
    onDirectEmail: () -> Unit,
    onIssueRefund: () -> Unit,
    onToggleResolve: () -> Unit,
    onDissolve: () -> Unit
) {
    val isResolved = ticket.status.equals("RESOLVED", ignoreCase = true) || ticket.status.equals("CLOSED", ignoreCase = true)
    val statusBg = when {
        isResolved -> Color.Green.copy(alpha = 0.2f)
        ticket.status == "INVESTIGATING" -> Color(0xFFFFB74D).copy(alpha = 0.2f)
        ticket.status == "REFUNDED" -> VelorixAccent.copy(alpha = 0.2f)
        else -> Color.Red.copy(alpha = 0.2f)
    }
    val statusTextColor = when {
        isResolved -> Color.Green
        ticket.status == "INVESTIGATING" -> Color(0xFFFFB74D)
        ticket.status == "REFUNDED" -> VelorixAccent
        else -> Color.Red
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = CardLiveBg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (ticket.isHighPriority) Color.Red.copy(alpha = 0.6f) else CardVerifyBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ticket.id, color = VelorixAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    if (ticket.isHighPriority) {
                        Box(
                            modifier = Modifier
                                .background(Color.Red, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("HIGH PRIORITY", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .background(statusBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(ticket.status, color = statusTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${ticket.username} (${ticket.gameId.ifEmpty { "No IGN" }})",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(ticket.userEmail, color = Color.Gray, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF181818), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SportsEsports, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = ticket.tournamentTitle.ifEmpty { "General Tournament Inquiry" },
                            color = VelorixTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(ticket.issueCategory, color = Color(0xFF64B5F6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(ticket.description, color = Color.LightGray, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tap card to inspect and adjudicate details", color = Color.Gray, fontSize = 10.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Email Direct Contact
                OutlinedButton(
                    onClick = onDirectEmail,
                    modifier = Modifier.weight(1f).height(34.dp),
                    border = BorderStroke(1.dp, Color(0xFF64B5F6)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Email", fontSize = 10.sp, color = Color(0xFF64B5F6))
                    }
                }

                // Issue Refund
                Button(
                    onClick = onIssueRefund,
                    modifier = Modifier.weight(1f).height(34.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Refund", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                // Resolve / Reopen Toggle
                Button(
                    onClick = onToggleResolve,
                    modifier = Modifier.weight(1.1f).height(34.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isResolved) Color(0xFF374151) else Color(0xFF16A34A)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isResolved) Icons.Default.Refresh else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isResolved) "Reopen" else "Resolve",
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Dissolve / Delete Button
                OutlinedButton(
                    onClick = onDissolve,
                    modifier = Modifier.height(34.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Dissolve Ticket", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Dissolve", fontSize = 10.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Full Detailed Ticket Adjudication Modal Dialog
 */
@Composable
fun ComplaintTicketDetailDialog(
    ticket: ComplaintTicket,
    user: UserProfile?,
    onDismiss: () -> Unit,
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
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF121215),
            border = BorderStroke(1.dp, CardVerifyBorder)
        ) {
            Column(
                modifier = Modifier
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
                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = VelorixAccent, modifier = Modifier.size(24.dp))
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF27272A))

                // Priority & Status Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isResolved -> Color.Green.copy(alpha = 0.2f)
                            ticket.status == "INVESTIGATING" -> Color(0xFFFFB74D).copy(alpha = 0.2f)
                            ticket.status == "REFUNDED" -> VelorixAccent.copy(alpha = 0.2f)
                            else -> Color.Red.copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = "STATUS: ${ticket.status}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = when {
                                isResolved -> Color.Green
                                ticket.status == "INVESTIGATING" -> Color(0xFFFFB74D)
                                ticket.status == "REFUNDED" -> VelorixAccent
                                else -> Color.Red
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    if (ticket.isHighPriority) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEF4444)
                        ) {
                            Text(
                                text = "CRITICAL / HIGH PRIORITY",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Player Information Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1C1C22),
                    border = BorderStroke(1.dp, Color(0xFF2E2E38)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Player Information", color = VelorixAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Name / User:", color = Color.Gray, fontSize = 12.sp)
                            Text(ticket.username, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Email:", color = Color.Gray, fontSize = 12.sp)
                            Text(ticket.userEmail, color = Color.White, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Game ID / IGN:", color = Color.Gray, fontSize = 12.sp)
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
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Current Wallet Balance:", color = Color.Gray, fontSize = 12.sp)
                                Text("₹${user.totalWalletBalance.toInt()} (Deposit ₹${user.depositFunds.toInt()} | Win ₹${user.winningFunds.toInt()})", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tournament & Issue Description
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1C1C22),
                    border = BorderStroke(1.dp, Color(0xFF2E2E38)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Category: ${ticket.issueCategory}", color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        if (ticket.tournamentTitle.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tournament: ${ticket.tournamentTitle}", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF2E2E38))
                        Text("Player Report Details:", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(ticket.description, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Admin Adjudication Note Input
                OutlinedTextField(
                    value = adminNote,
                    onValueChange = { adminNote = it },
                    label = { Text("Admin Adjudication Note / Remarks", color = Color.Gray, fontSize = 12.sp) },
                    placeholder = { Text("Enter internal remarks or resolution justification...", color = Color.DarkGray, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = VelorixAccent,
                        unfocusedBorderColor = Color(0xFF2E2E38)
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Actions Section
                Text("Action & Adjudication Controls", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onDirectEmail,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send Email", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onIssueRefund,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("₹ Compensation", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Status Change Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onUpdateStatus("INVESTIGATING", adminNote) },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color(0xFFFFB74D))
                    ) {
                        Text("Investigating", color = Color(0xFFFFB74D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val next = if (isResolved) "PENDING" else "RESOLVED"
                            onUpdateStatus(next, adminNote)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isResolved) Color(0xFF374151) else Color(0xFF16A34A))
                    ) {
                        Icon(
                            if (isResolved) Icons.Default.Refresh else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isResolved) "Reopen Ticket" else "Mark Resolved", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dissolve / Permanently Delete Action Button
                Button(
                    onClick = onDissolveTicket,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Permanently Dissolve / Delete Ticket", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
