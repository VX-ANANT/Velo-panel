package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.*
import com.example.ui.viewmodel.TournamentDashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AmoledBg = Color.Black
private val SurfaceSubtle = Color(0xFF0C0C10)
private val DividerHairline = Color(0xFF1E1E27)
private val PrimaryText = Color(0xFFF5F5F7)
private val SecondaryText = Color(0xFF9A9AA6)
private val BlueAccent = Color(0xFFFF2E5B) // Signature YouTube Pinkish-Red Primary Accent
private val GreenAccent = Color(0xFF34D399)
private val AmberAccent = Color(0xFFFBBF24)
private val RedAccent = Color(0xFFFF4D6D)

private fun formatShortTs(ts: Long): String {
    if (ts <= 0L) return "--"
    return try {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(ts))
    } catch (_: Exception) {
        "--"
    }
}

@Composable
private fun PillActionButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 36.dp)
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
            .border(0.75.dp, color.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

// ============================================================================
// MODULE 2: WALLET, UPI PRE-VERIFY UTR & DUAL WITHDRAWAL DESK (UPI + VOUCHERS)
// ============================================================================
@Composable
fun WalletUpiVoucherModuleSection(
    viewModel: TournamentDashboardViewModel,
    depositRequests: List<DepositRequest>,
    withdrawals: List<PayoutRequest>,
    verifiedUtrs: List<VerifiedBankDeposit>
) {
    val systemConfig by viewModel.systemConfig.collectAsStateWithLifecycle()
    var dynamicUpiId by remember(systemConfig.upiId) { mutableStateOf(systemConfig.upiId.ifBlank { "veloxyra.anant@fam" }) }
    var dynamicMerchantName by remember(systemConfig.upiMerchantName) { mutableStateOf(systemConfig.upiMerchantName.ifBlank { "VeloRix Esports" }) }

    var preVerifyUtrInput by remember { mutableStateOf("") }
    var preVerifyAmountInput by remember { mutableStateOf("") }

    var filterStatus by remember { mutableStateOf("ALL") }
    var activeSubTab by remember { mutableStateOf("WITHDRAWALS") } // WITHDRAWALS | DEPOSITS | PRE_UTR

    // Per-withdrawal input state maps
    var redeemCodeInputs by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var upiNoteInputs by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var rejectReasonInputs by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val filteredDeposits = remember(depositRequests, filterStatus) {
        if (filterStatus == "ALL") depositRequests
        else depositRequests.filter { it.status.equals(filterStatus, ignoreCase = true) }
    }
    val filteredWithdrawals = remember(withdrawals, filterStatus) {
        val wOnly = withdrawals.filter { !it.isDeposit }
        if (filterStatus == "ALL") wOnly
        else wOnly.filter { it.status.equals(filterStatus, ignoreCase = true) }
    }

    val pendingDepositsCount = remember(depositRequests) { depositRequests.count { it.status.equals("PENDING", true) } }
    val pendingWithdrawalsCount = remember(withdrawals) { withdrawals.count { !it.isDeposit && it.status.equals("PENDING", true) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 2.0 Dynamic Merchant UPI Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceSubtle, RoundedCornerShape(10.dp))
                .border(0.5.dp, DividerHairline, RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                    Text("Dynamic Merchant UPI", color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                PillActionButton(
                    label = "Sync UPI",
                    color = GreenAccent,
                    icon = Icons.Default.Sync,
                    modifier = Modifier.testTag("sync_merchant_upi_button"),
                    onClick = {
                        viewModel.saveSystemConfig(
                            systemConfig.copy(
                                upiId = dynamicUpiId.trim().ifBlank { "veloxyra.anant@fam" },
                                upiMerchantName = dynamicMerchantName.trim().ifBlank { "VeloRix Esports" },
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = dynamicUpiId,
                    onValueChange = { dynamicUpiId = it },
                    label = { Text("Merchant VPA", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(0.58f)
                )
                OutlinedTextField(
                    value = dynamicMerchantName,
                    onValueChange = { dynamicMerchantName = it },
                    label = { Text("Payee Name", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(0.42f)
                )
            }
        }

        // Sub-Tab Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple("WITHDRAWALS", "Withdrawals ($pendingWithdrawalsCount)", AmberAccent),
                Triple("DEPOSITS", "Deposits ($pendingDepositsCount)", GreenAccent),
                Triple("PRE_UTR", "Pre-Verify UTR (${verifiedUtrs.size})", BlueAccent)
            ).forEach { (key, label, accent) ->
                val selected = activeSubTab == key
                Box(
                    modifier = Modifier
                        .background(if (selected) accent.copy(alpha = 0.16f) else SurfaceSubtle, RoundedCornerShape(10.dp))
                        .border(0.75.dp, if (selected) accent else DividerHairline, RoundedCornerShape(10.dp))
                        .clickable { activeSubTab = key }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        color = if (selected) accent else SecondaryText,
                        fontSize = 11.5.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        if (activeSubTab != "PRE_UTR") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("ALL", "PENDING", "APPROVED", "REJECTED").forEach { st ->
                    val sel = filterStatus == st
                    Text(
                        text = st,
                        color = if (sel) PrimaryText else SecondaryText,
                        fontSize = 11.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .clickable { filterStatus = st }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = DividerHairline)

        when (activeSubTab) {
            "PRE_UTR" -> {
                Text("Pre-Verify Bank UTR (/verified_bank_deposits)", color = PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Log incoming bank UTRs so users auto-claim deposits instantly without waiting.",
                    color = SecondaryText,
                    fontSize = 11.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = preVerifyUtrInput,
                        onValueChange = { preVerifyUtrInput = it },
                        label = { Text("12-Digit UTR Number", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(0.6f).testTag("pre_verify_utr_input")
                    )
                    OutlinedTextField(
                        value = preVerifyAmountInput,
                        onValueChange = { preVerifyAmountInput = it },
                        label = { Text("Amount (₹)", fontSize = 10.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.4f).testTag("pre_verify_amount_input")
                    )
                }
                PillActionButton(
                    label = "Pre-Verify UTR Now",
                    color = GreenAccent,
                    icon = Icons.Default.Verified,
                    modifier = Modifier.testTag("pre_verify_utr_submit"),
                    onClick = {
                        val amt = preVerifyAmountInput.toDoubleOrNull() ?: 0.0
                        viewModel.preVerifyBankUtr(preVerifyUtrInput, amt)
                        if (preVerifyUtrInput.trim().length >= 6 && amt > 0.0) {
                            preVerifyUtrInput = ""
                            preVerifyAmountInput = ""
                        }
                    }
                )
                HorizontalDivider(thickness = 0.5.dp, color = DividerHairline)
                if (verifiedUtrs.isEmpty()) {
                    Text("No pre-verified UTR entries yet.", color = SecondaryText, fontSize = 12.sp)
                } else {
                    verifiedUtrs.take(30).forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "UTR: ${entry.utr} • ₹${entry.amount.toInt()}",
                                    color = PrimaryText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${formatShortTs(entry.timestamp)}${if (entry.claimedBy.isNotBlank()) " • Claimed by ${entry.claimedBy.take(10)}" else ""}",
                                    color = SecondaryText,
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                text = if (entry.claimed) "CLAIMED" else "READY",
                                color = if (entry.claimed) SecondaryText else GreenAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = DividerHairline)
                    }
                }
            }

            "DEPOSITS" -> {
                Text(
                    "Deposits & UPI Orders (${filteredDeposits.size})",
                    color = PrimaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (filteredDeposits.isEmpty()) {
                    Text("No deposits matching '$filterStatus'.", color = SecondaryText, fontSize = 12.sp)
                } else {
                    filteredDeposits.forEach { req ->
                        val stColor = when (req.status.uppercase()) {
                            "APPROVED", "COMPLETED", "SUCCESS" -> GreenAccent
                            "REJECTED", "FAILED" -> RedAccent
                            else -> AmberAccent
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "₹${"%.0f".format(req.amount)} VT • ${req.username.ifBlank { req.effectiveUserId.take(12) }}",
                                    color = PrimaryText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = req.status.uppercase(),
                                    color = stColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "${req.paymentMethod} • UTR: ${req.utrNumber.ifBlank { req.orderId.ifBlank { req.id } }} • ${formatShortTs(req.createdAt)}",
                                color = SecondaryText,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            if (req.status.equals("PENDING", ignoreCase = true)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PillActionButton(
                                        label = "Approve & Credit",
                                        color = GreenAccent,
                                        icon = Icons.Default.Check,
                                        onClick = { viewModel.approveDepositRequest(req) }
                                    )
                                    PillActionButton(
                                        label = "Reject UTR",
                                        color = RedAccent,
                                        icon = Icons.Default.Close,
                                        onClick = { viewModel.rejectDepositRequest(req, "Invalid or unverifiable UTR reference") }
                                    )
                                }
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = DividerHairline)
                    }
                }
            }

            else -> {
                Text(
                    "Dual Withdrawal Desk — UPI & Redeem Vouchers (${filteredWithdrawals.size})",
                    color = PrimaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Statutory 30% TDS shown on every payout. Delivering a Redeem Code auto-syncs to user's My Tickets.",
                    color = SecondaryText,
                    fontSize = 11.sp
                )
                if (filteredWithdrawals.isEmpty()) {
                    Text("No withdrawal requests matching '$filterStatus'.", color = SecondaryText, fontSize = 12.sp)
                } else {
                    filteredWithdrawals.forEach { wReq ->
                        val stColor = when (wReq.status.uppercase()) {
                            "APPROVED", "COMPLETED", "SUCCESS" -> GreenAccent
                            "REJECTED", "FAILED" -> RedAccent
                            else -> AmberAccent
                        }
                        val isVoucher = wReq.isRedeemVoucher
                        val isPending = wReq.status.equals("PENDING", ignoreCase = true)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
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
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                (if (isVoucher) AmberAccent else BlueAccent).copy(alpha = 0.16f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isVoucher) "REDEEM VOUCHER" else "UPI PAYOUT",
                                            color = if (isVoucher) AmberAccent else BlueAccent,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        text = wReq.inGameName.ifBlank { wReq.username.ifBlank { wReq.uid.take(10) } },
                                        color = PrimaryText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = wReq.status.uppercase(),
                                    color = stColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Statutory 30% TDS & Net Breakdown
                            Text(
                                text = "Gross ₹${"%.0f".format(wReq.vtAmount)}  |  30% TDS -₹${"%.0f".format(wReq.effectiveTdsAmount)}  |  NET ₹${"%.0f".format(wReq.effectiveNetAmount)}",
                                color = GreenAccent,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )

                            Text(
                                text = if (isVoucher) {
                                    "Voucher: ${wReq.voucherSpec.ifBlank { wReq.paymentId.ifBlank { "Google Play" } }} • ID: ${wReq.id} • ${formatShortTs(wReq.requestedAt)}"
                                } else {
                                    "UPI ID: ${wReq.paymentId} • ID: ${wReq.id} • ${formatShortTs(wReq.requestedAt)}"
                                },
                                color = SecondaryText,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            if (wReq.deliveredRedeemCode.isNotBlank()) {
                                Text(
                                    text = "Delivered Code: ${wReq.deliveredRedeemCode}",
                                    color = AmberAccent,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            if (isPending) {
                                if (isVoucher) {
                                    val codeInput = redeemCodeInputs[wReq.id] ?: ""
                                    OutlinedTextField(
                                        value = codeInput,
                                        onValueChange = { redeemCodeInputs = redeemCodeInputs + (wReq.id to it) },
                                        label = { Text("Paste 16-Digit Redeem Voucher Code", fontSize = 10.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    val noteInput = upiNoteInputs[wReq.id] ?: ""
                                    OutlinedTextField(
                                        value = noteInput,
                                        onValueChange = { upiNoteInputs = upiNoteInputs + (wReq.id to it) },
                                        label = { Text("Payout Bank UTR / Note (Optional)", fontSize = 10.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                val rejReason = rejectReasonInputs[wReq.id] ?: ""
                                OutlinedTextField(
                                    value = rejReason,
                                    onValueChange = { rejectReasonInputs = rejectReasonInputs + (wReq.id to it) },
                                    label = { Text("Rejection Reason (only if rejecting)", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isVoucher) {
                                        PillActionButton(
                                            label = "Deliver Code & Resolve",
                                            color = AmberAccent,
                                            icon = Icons.Default.CardGiftcard,
                                            onClick = {
                                                viewModel.approveRedeemVoucherWithdrawal(wReq, redeemCodeInputs[wReq.id] ?: "")
                                            }
                                        )
                                    } else {
                                        PillActionButton(
                                            label = "Approve UPI Payout",
                                            color = GreenAccent,
                                            icon = Icons.Default.Check,
                                            onClick = {
                                                viewModel.approveUpiWithdrawal(wReq, upiNoteInputs[wReq.id] ?: "")
                                            }
                                        )
                                    }
                                    PillActionButton(
                                        label = "Reject & Refund VT",
                                        color = RedAccent,
                                        icon = Icons.Default.Undo,
                                        onClick = {
                                            viewModel.rejectWithdrawalAndRefund(
                                                wReq,
                                                rejReason.ifBlank { "Invalid payout details — Refunded to wallet" }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = DividerHairline)
                    }
                }
            }
        }
    }
}

// ============================================================================
// MODULE 3: REDEEM CODE VAULT (PROMO & EVENT CODES + CLAIM AUDIT LOG)
// ============================================================================
@Composable
fun RedeemCodeVaultModuleSection(
    viewModel: TournamentDashboardViewModel,
    redeemCodes: List<RedeemCodeItem>,
    redeemClaims: List<RedeemClaimRecord>
) {
    var codeInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }
    var rewardVtInput by remember { mutableStateOf("50") }
    var rewardTokensInput by remember { mutableStateOf("100") }
    var maxUsesInput by remember { mutableStateOf("100") }
    var expiresDaysInput by remember { mutableStateOf("7") }
    var targetUserIdInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(18.dp))
            Text("Redeem Code Vault (/redeem_codes)", color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "Create promo & event codes synced to RTDB & Firestore. Use Max Uses = 0 for unlimited, Expiry Days = 0 for never.",
            color = SecondaryText,
            fontSize = 11.sp
        )

        // Create / Edit Form
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceSubtle, RoundedCornerShape(10.dp))
                .border(0.5.dp, DividerHairline, RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it.uppercase() },
                    label = { Text("CODE (e.g. VELORIX50)", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(0.5f).testTag("redeem_code_input")
                )
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Event / Title", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(0.5f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rewardVtInput,
                    onValueChange = { rewardVtInput = it },
                    label = { Text("Reward VT (₹)", fontSize = 10.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = rewardTokensInput,
                    onValueChange = { rewardTokensInput = it },
                    label = { Text("Reward Tokens", fontSize = 10.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = maxUsesInput,
                    onValueChange = { maxUsesInput = it },
                    label = { Text("Max Uses (0=∞)", fontSize = 10.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.5f)
                )
                OutlinedTextField(
                    value = expiresDaysInput,
                    onValueChange = { expiresDaysInput = it },
                    label = { Text("Expire Days (0=Never)", fontSize = 10.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.5f)
                )
            }
            OutlinedTextField(
                value = targetUserIdInput,
                onValueChange = { targetUserIdInput = it },
                label = { Text("Target User UID (Leave blank for all players)", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            PillActionButton(
                label = "Create / Update Redeem Code",
                color = GreenAccent,
                icon = Icons.Default.Add,
                modifier = Modifier.testTag("save_redeem_code_button"),
                onClick = {
                    val days = expiresDaysInput.toLongOrNull() ?: 0L
                    val expMillis = if (days > 0L) System.currentTimeMillis() + (days * 86_400_000L) else 0L
                    val existing = redeemCodes.find { it.code.equals(codeInput.trim(), true) }
                    viewModel.saveRedeemCode(
                        RedeemCodeItem(
                            code = codeInput.trim().uppercase(),
                            title = titleInput.trim().ifBlank { codeInput.trim().uppercase() },
                            active = true,
                            status = "ACTIVE",
                            rewardVt = rewardVtInput.toDoubleOrNull() ?: 0.0,
                            rewardTokens = rewardTokensInput.toIntOrNull() ?: 0,
                            maxUses = maxUsesInput.toIntOrNull() ?: 0,
                            usedCount = existing?.usedCount ?: 0,
                            expiresAt = expMillis,
                            targetUserId = targetUserIdInput.trim(),
                            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    if (codeInput.trim().length >= 3) {
                        codeInput = ""
                        titleInput = ""
                        targetUserIdInput = ""
                    }
                }
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = DividerHairline)

        Text("Active & Archived Codes (${redeemCodes.size})", color = PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        if (redeemCodes.isEmpty()) {
            Text("No promo codes created yet.", color = SecondaryText, fontSize = 12.sp)
        } else {
            redeemCodes.forEach { item ->
                val isActive = item.active && item.status.equals("ACTIVE", true)
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.code} — ${item.title}",
                            color = PrimaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (isActive) "ACTIVE" else "DISABLED",
                            color = if (isActive) GreenAccent else RedAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "Reward: ₹${item.rewardVt.toInt()} VT + ${item.rewardTokens} Tokens • Uses: ${item.usedCount}/${if (item.maxUses <= 0) "∞" else item.maxUses} • Exp: ${if (item.expiresAt <= 0L) "Never" else formatShortTs(item.expiresAt)}",
                        color = SecondaryText,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (item.targetUserId.isNotBlank()) {
                        Text("Locked to UID: ${item.targetUserId}", color = AmberAccent, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PillActionButton(
                            label = if (isActive) "Disable" else "Enable",
                            color = if (isActive) AmberAccent else GreenAccent,
                            icon = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            onClick = { viewModel.toggleRedeemCodeActive(item.code, !isActive) }
                        )
                        PillActionButton(
                            label = "Edit",
                            color = BlueAccent,
                            icon = Icons.Default.Edit,
                            onClick = {
                                codeInput = item.code
                                titleInput = item.title
                                rewardVtInput = item.rewardVt.toInt().toString()
                                rewardTokensInput = item.rewardTokens.toString()
                                maxUsesInput = item.maxUses.toString()
                                targetUserIdInput = item.targetUserId
                            }
                        )
                        PillActionButton(
                            label = "Delete",
                            color = RedAccent,
                            icon = Icons.Default.DeleteOutline,
                            onClick = { viewModel.deleteRedeemCode(item.code) }
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = DividerHairline)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text("Claim Audit Log (/redeem_claims — ${redeemClaims.size})", color = PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        if (redeemClaims.isEmpty()) {
            Text("No player claims recorded yet.", color = SecondaryText, fontSize = 12.sp)
        } else {
            redeemClaims.take(40).forEach { cl ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${cl.code} • ${cl.username} (${cl.userId.take(10)})",
                            color = PrimaryText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = formatShortTs(cl.claimedAt),
                            color = SecondaryText,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = "+₹${cl.rewardVt.toInt()} VT / +${cl.rewardTokens}T",
                        color = GreenAccent,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = DividerHairline)
            }
        }
    }
}

// ============================================================================
// MODULE 4: UNIFIED 12-CHANNEL REQUEST HUB & "MY TICKETS" DESK
// ============================================================================
@Composable
fun UnifiedRequestHubModuleSection(
    viewModel: TournamentDashboardViewModel,
    unifiedRequests: List<UnifiedAdminRequest>
) {
    var selectedChannel by remember { mutableStateOf(UnifiedRequestChannel.ALL) }
    var statusFilter by remember { mutableStateOf("ALL") }

    var draftStatusMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var draftReplyMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var draftRedeemCodeMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val channelCounts = remember(unifiedRequests) {
        val counts = mutableMapOf<UnifiedRequestChannel, Int>()
        counts[UnifiedRequestChannel.ALL] = unifiedRequests.count {
            !it.status.equals("RESOLVED", true) && !it.status.equals("REJECTED", true)
        }
        UnifiedRequestChannel.entries.filter { it != UnifiedRequestChannel.ALL }.forEach { ch ->
            counts[ch] = unifiedRequests.count {
                it.resolvedChannel == ch && !it.status.equals("RESOLVED", true) && !it.status.equals("REJECTED", true)
            }
        }
        counts
    }

    val filtered = remember(unifiedRequests, selectedChannel, statusFilter) {
        unifiedRequests.filter { req ->
            val matchCh = selectedChannel == UnifiedRequestChannel.ALL || req.resolvedChannel == selectedChannel
            val matchSt = statusFilter == "ALL" || req.status.equals(statusFilter, ignoreCase = true)
            matchCh && matchSt
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.SupportAgent, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(18.dp))
            Text("Unified 12-Channel Request Hub", color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "Replies & status updates sync instantly across RTDB & Firestore to the player's My Tickets inbox with push notification.",
            color = SecondaryText,
            fontSize = 11.sp
        )

        // 12-Channel Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            UnifiedRequestChannel.entries.forEach { ch ->
                val isSelected = selectedChannel == ch
                val openCount = channelCounts[ch] ?: 0
                Row(
                    modifier = Modifier
                        .background(if (isSelected) BlueAccent.copy(alpha = 0.16f) else SurfaceSubtle, RoundedCornerShape(10.dp))
                        .border(0.75.dp, if (isSelected) BlueAccent else DividerHairline, RoundedCornerShape(10.dp))
                        .clickable { selectedChannel = ch }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = if (ch == UnifiedRequestChannel.ALL) "ALL" else ch.channelTag,
                        color = if (isSelected) PrimaryText else BlueAccent,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (openCount > 0) {
                        Text(
                            text = "$openCount",
                            color = AmberAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Status Filter Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("ALL", "PENDING", "IN_REVIEW", "INVESTIGATING", "RESOLVED", "REJECTED").forEach { st ->
                val sel = statusFilter == st
                Text(
                    text = st,
                    color = if (sel) PrimaryText else SecondaryText,
                    fontSize = 10.5.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.clickable { statusFilter = st }.padding(vertical = 3.dp)
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = DividerHairline)

        if (filtered.isEmpty()) {
            Text(
                "No tickets in ${selectedChannel.label} matching '$statusFilter'.",
                color = SecondaryText,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            filtered.take(40).forEach { req ->
                val ch = req.resolvedChannel
                val chosenStatus = draftStatusMap[req.id] ?: req.status.uppercase().ifBlank { "PENDING" }
                val draftReply = draftReplyMap[req.id] ?: req.adminReply
                val draftCode = draftRedeemCodeMap[req.id] ?: req.deliveredRedeemCode
                val statusColor = when (req.status.uppercase()) {
                    "RESOLVED" -> GreenAccent
                    "REJECTED" -> RedAccent
                    "IN_REVIEW", "INVESTIGATING" -> BlueAccent
                    else -> AmberAccent
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
                            Box(
                                modifier = Modifier
                                    .background(BlueAccent.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(ch.channelTag, color = BlueAccent, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Text(
                                text = req.subject.ifBlank { ch.label },
                                color = PrimaryText,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = req.status.uppercase(),
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "${req.username} (${req.userId.take(10)}) • ${req.userEmail.ifBlank { req.freeFireId.ifBlank { req.id } }} • ${formatShortTs(req.createdAt)}",
                        color = SecondaryText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    if (req.description.isNotBlank()) {
                        Text(req.description, color = PrimaryText.copy(alpha = 0.88f), fontSize = 11.5.sp)
                    }

                    if (req.amount > 0.0 || req.utrNumber.isNotBlank() || req.voucherSpec.isNotBlank()) {
                        Text(
                            text = listOfNotNull(
                                if (req.amount > 0.0) "₹${req.amount.toInt()}" else null,
                                req.utrNumber.takeIf { it.isNotBlank() }?.let { "UTR: $it" },
                                req.voucherSpec.takeIf { it.isNotBlank() }?.let { "Voucher: $it" }
                            ).joinToString(" • "),
                            color = AmberAccent,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Status Selector Pills
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("PENDING", "IN_REVIEW", "INVESTIGATING", "RESOLVED", "REJECTED").forEach { st ->
                            val active = chosenStatus.equals(st, true)
                            val c = when (st) {
                                "RESOLVED" -> GreenAccent
                                "REJECTED" -> RedAccent
                                "IN_REVIEW", "INVESTIGATING" -> BlueAccent
                                else -> AmberAccent
                            }
                            Box(
                                modifier = Modifier
                                    .background(if (active) c.copy(alpha = 0.18f) else SurfaceSubtle, RoundedCornerShape(8.dp))
                                    .border(0.6.dp, if (active) c else DividerHairline, RoundedCornerShape(8.dp))
                                    .clickable { draftStatusMap = draftStatusMap + (req.id to st) }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(st, color = if (active) c else SecondaryText, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = draftReply,
                        onValueChange = { draftReplyMap = draftReplyMap + (req.id to it) },
                        label = { Text("Admin Reply (visible in player's My Tickets)", fontSize = 10.sp) },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (ch == UnifiedRequestChannel.REDEEM_CODE_REQUEST || ch == UnifiedRequestChannel.PAYMENT_WITHDRAW || draftCode.isNotBlank()) {
                        OutlinedTextField(
                            value = draftCode,
                            onValueChange = { draftRedeemCodeMap = draftRedeemCodeMap + (req.id to it) },
                            label = { Text("Optional Delivered Redeem Code (Golden Card)", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    PillActionButton(
                        label = "Send Reply & Update Ticket",
                        color = GreenAccent,
                        icon = Icons.Default.Send,
                        onClick = {
                            viewModel.replyAndUpdateUnifiedTicket(
                                req = req,
                                newStatus = chosenStatus,
                                adminReply = draftReply,
                                deliveredRedeemCode = draftCode
                            )
                        }
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = DividerHairline)
            }
        }
    }
}

// ============================================================================
// MODULE 6: BANNERS & GLOBAL BROADCASTS
// ============================================================================
@Composable
fun BannersAndBroadcastsModuleSection(
    viewModel: TournamentDashboardViewModel,
    systemConfig: SystemConfig,
    banners: List<AppAnnouncementBanner>
) {
    var bannerTitle by remember { mutableStateOf("") }
    var bannerSubtitle by remember { mutableStateOf("") }
    var bannerBadge by remember { mutableStateOf("OFFICIAL") }
    var bannerCta by remember { mutableStateOf("OPEN") }
    var bannerActionType by remember { mutableStateOf("ANNOUNCEMENT") }
    var bannerImageUrl by remember { mutableStateOf("") }
    var bannerTargetId by remember { mutableStateOf("") }
    var bannerOrder by remember { mutableStateOf("1") }

    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Master showBanners Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceSubtle, RoundedCornerShape(10.dp))
                .border(0.5.dp, if (systemConfig.showBanners) GreenAccent.copy(alpha = 0.5f) else DividerHairline, RoundedCornerShape(10.dp))
                .clickable { viewModel.toggleMasterShowBanners(!systemConfig.showBanners) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Master Home Banners Toggle (showBanners)", color = PrimaryText, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                Text("Synced to /app_config, /system_config & app_config/global", color = SecondaryText, fontSize = 10.sp)
            }
            Icon(
                imageVector = if (systemConfig.showBanners) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                contentDescription = "Toggle Home Banners",
                tint = if (systemConfig.showBanners) GreenAccent else SecondaryText,
                modifier = Modifier.size(28.dp)
            )
        }

        // Global Push Broadcast
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceSubtle, RoundedCornerShape(10.dp))
                .border(0.5.dp, DividerHairline, RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Campaign, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                Text("Global Push Broadcast (/notifications)", color = PrimaryText, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedTextField(
                value = broadcastTitle,
                onValueChange = { broadcastTitle = it },
                label = { Text("Notification Title", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = broadcastMessage,
                onValueChange = { broadcastMessage = it },
                label = { Text("Notification Message", fontSize = 10.sp) },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            PillActionButton(
                label = "Send Global Broadcast",
                color = BlueAccent,
                icon = Icons.Default.Send,
                modifier = Modifier.testTag("send_global_broadcast_btn"),
                onClick = {
                    if (broadcastTitle.isNotBlank() && broadcastMessage.isNotBlank()) {
                        viewModel.sendGlobalBroadcastNotification(broadcastTitle.trim(), broadcastMessage.trim(), "ANNOUNCEMENT")
                        broadcastTitle = ""
                        broadcastMessage = ""
                    }
                }
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = DividerHairline)

        // Banner Creator
        Text("Promotional Banners (/banners)", color = PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ANNOUNCEMENT", "MATCH", "WALLET", "SUPPORT", "LEADERBOARD").forEach { act ->
                val sel = bannerActionType == act
                Box(
                    modifier = Modifier
                        .background(if (sel) BlueAccent.copy(alpha = 0.16f) else SurfaceSubtle, RoundedCornerShape(8.dp))
                        .border(0.6.dp, if (sel) BlueAccent else DividerHairline, RoundedCornerShape(8.dp))
                        .clickable { bannerActionType = act }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(act, color = if (sel) BlueAccent else SecondaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = bannerTitle, onValueChange = { bannerTitle = it }, label = { Text("Banner Title", fontSize = 10.sp) }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(value = bannerSubtitle, onValueChange = { bannerSubtitle = it }, label = { Text("Subtitle", fontSize = 10.sp) }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = bannerBadge, onValueChange = { bannerBadge = it }, label = { Text("Badge", fontSize = 10.sp) }, singleLine = true, modifier = Modifier.weight(0.35f))
            OutlinedTextField(value = bannerCta, onValueChange = { bannerCta = it }, label = { Text("CTA", fontSize = 10.sp) }, singleLine = true, modifier = Modifier.weight(0.35f))
            OutlinedTextField(value = bannerOrder, onValueChange = { bannerOrder = it }, label = { Text("Order", fontSize = 10.sp) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(0.3f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = bannerImageUrl, onValueChange = { bannerImageUrl = it }, label = { Text("Image URL", fontSize = 10.sp) }, singleLine = true, modifier = Modifier.weight(0.55f))
            OutlinedTextField(value = bannerTargetId, onValueChange = { bannerTargetId = it }, label = { Text("Target ID / URL", fontSize = 10.sp) }, singleLine = true, modifier = Modifier.weight(0.45f))
        }
        PillActionButton(
            label = "Publish Banner",
            color = GreenAccent,
            icon = Icons.Default.Add,
            onClick = {
                if (bannerTitle.isNotBlank()) {
                    viewModel.savePromotionalBanner(
                        AppAnnouncementBanner(
                            id = "banner_${System.currentTimeMillis()}",
                            title = bannerTitle.trim(),
                            subtitle = bannerSubtitle.trim(),
                            message = bannerSubtitle.trim(),
                            badgeText = bannerBadge.trim().ifBlank { "OFFICIAL" },
                            ctaText = bannerCta.trim().ifBlank { "OPEN" },
                            actionType = bannerActionType,
                            targetId = bannerTargetId.trim(),
                            imageUrl = bannerImageUrl.trim(),
                            actionUrl = bannerTargetId.trim(),
                            targetUrl = bannerTargetId.trim(),
                            isActive = true,
                            priority = bannerOrder.toIntOrNull() ?: 1
                        )
                    )
                    bannerTitle = ""
                    bannerSubtitle = ""
                    bannerImageUrl = ""
                    bannerTargetId = ""
                }
            }
        )

        HorizontalDivider(thickness = 0.5.dp, color = DividerHairline)

        banners.forEach { b ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "[${b.actionType}] ${b.title} (#${b.priority})",
                        color = PrimaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${b.subtitle.ifBlank { b.message }} • ${if (b.isActive) "ACTIVE" else "HIDDEN"}",
                        color = if (b.isActive) SecondaryText else RedAccent,
                        fontSize = 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PillActionButton(
                        label = if (b.isActive) "Hide" else "Show",
                        color = AmberAccent,
                        onClick = { viewModel.savePromotionalBanner(b.copy(isActive = !b.isActive)) }
                    )
                    PillActionButton(
                        label = "Delete",
                        color = RedAccent,
                        icon = Icons.Default.DeleteOutline,
                        onClick = { viewModel.deletePromotionalBanner(b.id) }
                    )
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = DividerHairline)
        }
    }
}

// ============================================================================
// MODULE 5B: CHAT REPORTS QUEUE BLOCK (/chat_reports)
// ============================================================================
@Composable
fun ChatReportsQueueBlock(
    viewModel: TournamentDashboardViewModel,
    chatReports: List<ChatReportItem>
) {
    val pendingReports = remember(chatReports) {
        chatReports.filter { !it.status.equals("RESOLVED", true) }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Flag, contentDescription = null, tint = RedAccent, modifier = Modifier.size(16.dp))
            Text(
                text = "Chat Reports Queue (${pendingReports.size} Pending / ${chatReports.size} Total)",
                color = PrimaryText,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (chatReports.isEmpty()) {
            Text("No player chat reports in /chat_reports.", color = SecondaryText, fontSize = 11.sp)
        } else {
            chatReports.take(20).forEach { rep ->
                val isResolved = rep.status.equals("RESOLVED", true)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceSubtle, RoundedCornerShape(8.dp))
                        .border(0.5.dp, DividerHairline, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "${rep.reportedUserName} (${rep.reportedUserId.take(10)})",
                            color = PrimaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = rep.status,
                            color = if (isResolved) GreenAccent else AmberAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "\"${rep.messageText}\" — Reason: ${rep.reason}",
                        color = SecondaryText,
                        fontSize = 11.sp
                    )
                    if (!isResolved) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PillActionButton(
                                label = "Delete Message",
                                color = AmberAccent,
                                icon = Icons.Default.DeleteOutline,
                                onClick = { viewModel.resolveChatReportAndDeleteMessage(rep) }
                            )
                            PillActionButton(
                                label = "Ban Sender",
                                color = RedAccent,
                                icon = Icons.Default.Block,
                                onClick = { viewModel.resolveChatReportAndBanSender(rep, rep.reason.ifBlank { "Toxic Lobby Chat" }) }
                            )
                        }
                    }
                }
            }
        }
    }
}
