package com.refguard.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refguard.app.domain.MismatchStatus
import com.refguard.app.domain.RiskLevel
import com.refguard.app.domain.ScanResult
import com.refguard.app.ui.theme.*
import com.refguard.app.viewmodel.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    result: ScanResult,
    viewModel: ScanViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToInvestigation: () -> Unit,
    onNavigateToAction: () -> Unit,
    onCheckAnother: () -> Unit
) {
    val scrollState = rememberScrollState()
    var feedbackGiven by remember { mutableStateOf<Boolean?>(null) }

    val isCritical = result.riskLevel == RiskLevel.CRITICAL || result.riskScore >= 80
    val isWarning = result.riskLevel == RiskLevel.HIGH || result.riskLevel == RiskLevel.WARNING || result.riskScore in 30..79
    val isSafe = !isCritical && !isWarning
    val isMismatch = result.mismatchStatus == MismatchStatus.DETECTED || result.signals.contains("payment_intent_inversion")

    val riskTheme = when {
        isCritical -> RiskThemeColors(ColorCritical, ColorCriticalContainer, "CRITICAL RISK", Icons.Default.GppBad)
        isWarning -> RiskThemeColors(ColorWarning, ColorWarningContainer, "HIGH RISK / CAUTION", Icons.Default.Warning)
        else -> RiskThemeColors(ColorSafe, ColorSafeContainer, "SAFE TRANSACTION", Icons.Default.VerifiedUser)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Verdict", fontWeight = FontWeight.Bold, color = ColorBrand) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ColorBrand)
                    }
                },
                actions = {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            if (result.isLocalEdgeResult) "Offline Analysis" else "Cloud Analysis",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── TOP VERDICT HERO (CRITICAL MISMATCH OR STANDARD VERDICT) ──
            if (isMismatch && isCritical) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ColorCritical),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "PAYMENT-INTENT MISMATCH",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            Text(
                                "CRITICAL (${result.riskScore}/100)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // What they told you vs What payment does
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "THEY TOLD YOU:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF555555)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    result.statedIntent ?: "Receive cashback / reward in your bank account",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFE2E8F0))

                                Text(
                                    "BUT THE PAYMENT DOES:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorCritical
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (result.mismatchAmount != null && result.mismatchAmount > 0) {
                                        "OUTBOUND DEBIT OF ₹${result.mismatchAmount.toInt()} FROM YOUR ACCOUNT"
                                    } else {
                                        "OUTBOUND DEBIT FROM YOUR BANK ACCOUNT"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Black,
                                    color = ColorCritical
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            "Entering your UPI PIN authorizes money to leave your account. You NEVER need to enter a PIN to receive money.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = riskTheme.containerColor),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(riskTheme.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                riskTheme.icon,
                                contentDescription = null,
                                tint = riskTheme.color,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            riskTheme.badgeTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = riskTheme.color,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            "Threat Score: ${result.riskScore}/100",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = riskTheme.color
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = result.detectedSummary.ifBlank { result.humanExplanation },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        if (result.userInstruction.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = result.userInstruction,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ── PAYMENT DETAILS SECTION ──────────────────────
            if (result.recipientVpa != null || result.mismatchAmount != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Payment Details",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ColorBrand
                        )
                        Spacer(Modifier.height(12.dp))

                        DetailRow(
                            label = "Target UPI ID",
                            value = result.recipientVpa ?: "Not specified"
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                        DetailRow(
                            label = "Transaction Amount",
                            value = if (result.mismatchAmount != null && result.mismatchAmount > 0) "₹${result.mismatchAmount.toInt()}" else "Amount not specified in request"
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                        DetailRow(
                            label = "Payment Direction",
                            value = if (result.paymentDirection?.contains("DEBIT", true) == true || result.actualPaymentAction?.contains("DEBIT", true) == true) {
                                "Outgoing Debit (Money leaves your account)"
                            } else {
                                "Standard Transaction"
                            },
                            highlightColor = if (result.paymentDirection?.contains("DEBIT", true) == true || isMismatch) ColorCritical else null
                        )
                    }
                }
            }

            // ── PRIMARY ACTION BUTTONS ───────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isCritical || isWarning) {
                    Button(
                        onClick = onNavigateToAction,
                        colors = ButtonDefaults.buttonColors(containerColor = ColorCritical),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("STOP TRANSACTION & PROTECT", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onNavigateToInvestigation,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorBrand)
                    ) {
                        Icon(Icons.Default.Troubleshoot, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("VIEW INVESTIGATION DETAILS", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onNavigateToInvestigation,
                        colors = ButtonDefaults.buttonColors(containerColor = ColorBrand),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Troubleshoot, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("VIEW INVESTIGATION DETAILS", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onCheckAnother,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("CHECK ANOTHER MESSAGE", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── ACCURACY TELEMETRY / USER FEEDBACK ───────────
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Was this accurate?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (feedbackGiven == null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    feedbackGiven = true
                                    viewModel.submitFeedback(result.scanId, result.recipientVpa, isConfirmedFraud = isCritical)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Yes", fontWeight = FontWeight.Bold, color = ColorSafe)
                            }
                            TextButton(
                                onClick = {
                                    feedbackGiven = false
                                    viewModel.submitFeedback(result.scanId, result.recipientVpa, isConfirmedFraud = !isCritical)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("False Alarm", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
                        Text(
                            "Thank you for your feedback",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ColorBrand
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    highlightColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = highlightColor ?: MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

private data class RiskThemeColors(
    val color: Color,
    val containerColor: Color,
    val badgeTitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
