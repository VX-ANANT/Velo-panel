package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CashoutRequest
import com.example.domain.model.Tournament
import com.example.domain.model.UserProfile
import com.example.ui.common.GlassCard
import com.example.ui.theme.*

object FinancialAuditExporter {
    fun generateFullAuditCsv(
        tournaments: List<Tournament>,
        users: List<UserProfile>,
        cashouts: List<CashoutRequest>
    ): String {
        val sb = StringBuilder()
        sb.append("=== VELORIX TOURNAMENT PLATFORM FINANCIAL AUDIT REPORT ===\n")
        sb.append("Generated At: ${java.util.Date()}\n\n")

        // 1. TOURNAMENT ENTRY FEE & PRIZE COLLECTION
        sb.append("--- 1. TOURNAMENT REVENUE & PRIZES ---\n")
        sb.append("Tournament ID,Title,Game,Status,Entry Fee (INR),Max Players,Joined,Gross Revenue (INR),Prize Pool (INR),Net House Margin (INR)\n")
        var totalGrossRevenue = 0.0
        var totalPrizePools = 0.0

        tournaments.forEach { t ->
            val gross = t.entryFee.toDouble() * t.registeredPlayers
            val netMargin = gross - t.prizePool.toDouble()
            totalGrossRevenue += gross
            totalPrizePools += t.prizePool.toDouble()

            sb.append("\"${t.id}\",\"${t.title}\",\"${t.game}\",\"${t.status}\",${t.entryFee},${t.maxPlayers},${t.registeredPlayers},$gross,${t.prizePool},$netMargin\n")
        }
        sb.append("TOTALS,,,,,,,$totalGrossRevenue,$totalPrizePools,${totalGrossRevenue - totalPrizePools}\n\n")

        // 2. PLAYER WALLET BALANCES & EARNINGS
        sb.append("--- 2. PLAYER TREASURY & LIABILITY ---\n")
        sb.append("User ID,Username,Phone/Email,Wallet Balance (INR),Total Earnings (INR),Matches Played,Wins,Kills\n")
        var totalUserWalletLiability = 0.0
        var totalPlayerEarnings = 0.0

        users.forEach { u ->
            totalUserWalletLiability += u.walletBalance
            totalPlayerEarnings += u.totalEarnings
            sb.append("\"${u.id}\",\"${u.username}\",\"${u.phoneNumber.ifBlank { u.email }}\",${u.walletBalance},${u.totalEarnings},${u.matchesPlayed},${u.matchesWon},${u.totalKills}\n")
        }
        sb.append("TOTALS,,,${totalUserWalletLiability},${totalPlayerEarnings},,,\n\n")

        // 3. CASHOUT & PAYOUT DISBURSEMENTS
        sb.append("--- 3. PAYOUT & WITHDRAWAL DISBURSEMENTS ---\n")
        sb.append("Cashout ID,User ID,Username,Amount (INR),Payment Method,UPI ID,Status,Created At\n")
        var totalApprovedPayouts = 0.0
        var totalPendingPayouts = 0.0

        cashouts.forEach { c ->
            if (c.status.equals("APPROVED", ignoreCase = true) || c.status.equals("COMPLETED", ignoreCase = true)) {
                totalApprovedPayouts += c.amount
            } else if (c.status.equals("PENDING", ignoreCase = true)) {
                totalPendingPayouts += c.amount
            }
            sb.append("\"${c.id}\",\"${c.userId}\",\"${c.username}\",${c.amount},\"${c.paymentMethod}\",\"${c.upiId}\",\"${c.status}\",\"${c.createdAt}\"\n")
        }
        sb.append("TOTAL APPROVED PAYOUTS: INR $totalApprovedPayouts\n")
        sb.append("TOTAL PENDING PAYOUTS: INR $totalPendingPayouts\n\n")

        // 4. PLATFORM NET CASHFLOW SUMMARY
        val netPlatformOperatingIncome = totalGrossRevenue - totalApprovedPayouts
        sb.append("--- 4. PLATFORM NET POSITION ---\n")
        sb.append("Gross Tournament Entry Collections: INR $totalGrossRevenue\n")
        sb.append("Total Cashouts Disbursed: INR $totalApprovedPayouts\n")
        sb.append("Net Platform Retained Operating Margin: INR $netPlatformOperatingIncome\n")
        sb.append("Current Outstanding User Wallet Liability: INR $totalUserWalletLiability\n")

        return sb.toString()
    }
}

@Composable
fun FinancialAuditDialog(
    tournaments: List<Tournament>,
    users: List<UserProfile>,
    cashouts: List<CashoutRequest>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val totalGrossRevenue = remember(tournaments) {
        tournaments.sumOf { (it.entryFee.toDouble() * it.registeredPlayers) }
    }
    val totalPrizePools = remember(tournaments) {
        tournaments.sumOf { it.prizePool.toDouble() }
    }
    val totalApprovedPayouts = remember(cashouts) {
        cashouts.filter { it.status.equals("APPROVED", ignoreCase = true) || it.status.equals("COMPLETED", ignoreCase = true) }.sumOf { it.amount }
    }
    val totalPendingPayouts = remember(cashouts) {
        cashouts.filter { it.status.equals("PENDING", ignoreCase = true) }.sumOf { it.amount }
    }
    val totalUserWallets = remember(users) {
        users.sumOf { it.walletBalance }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = VelorixAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Financial & Payout Audit Report", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Gross Metrics Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AuditMetricBox(
                        modifier = Modifier.weight(1f),
                        label = "GROSS COLLECTIONS",
                        value = "₹${totalGrossRevenue.toInt()}",
                        color = VelorixAccent
                    )
                    AuditMetricBox(
                        modifier = Modifier.weight(1f),
                        label = "PRIZE POOLS",
                        value = "₹${totalPrizePools.toInt()}",
                        color = Color(0xFFFFD54F)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AuditMetricBox(
                        modifier = Modifier.weight(1f),
                        label = "PAID CASHOUTS",
                        value = "₹${totalApprovedPayouts.toInt()}",
                        color = Color(0xFF81C784)
                    )
                    AuditMetricBox(
                        modifier = Modifier.weight(1f),
                        label = "PENDING CASHOUTS",
                        value = "₹${totalPendingPayouts.toInt()}",
                        color = Color(0xFFFF8A65)
                    )
                }

                Surface(
                    color = Color(0xFF1E1A2C),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF7F56D9).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("TREASURY SUMMARY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD0BCFF), letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Player Wallet Liabilities:", fontSize = 11.sp, color = VelorixTextSecondary)
                            Text("₹${totalUserWallets.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Net Operating Cashflow:", fontSize = 11.sp, color = VelorixTextSecondary)
                            val net = totalGrossRevenue - totalApprovedPayouts
                            Text(
                                "₹${net.toInt()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (net >= 0) VelorixAccent else Color(0xFFFF5252)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val csv = FinancialAuditExporter.generateFullAuditCsv(tournaments, users, cashouts)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Velorix Financial Audit CSV", csv))
                    Toast.makeText(context, "Financial Audit CSV copied to clipboard!", Toast.LENGTH_LONG).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = VelorixAccent)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy Full CSV Audit", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = VelorixTextSecondary)
            }
        },
        containerColor = CardVerifyBg
    )
}

@Composable
fun AuditMetricBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = CardVerifyBg,
        border = BorderStroke(1.dp, CardVerifyBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = VelorixTextSecondary, letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}
