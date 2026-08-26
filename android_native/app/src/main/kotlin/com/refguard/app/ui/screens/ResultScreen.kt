package com.refguard.app.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.refguard.app.domain.*
import com.refguard.app.ui.theme.*
import com.refguard.app.viewmodel.ReportUiState
import com.refguard.app.viewmodel.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    result: ScanResult,
    viewModel: ScanViewModel,
    reportState: ReportUiState,
    onScanAnother: () -> Unit
) {
    val palette = riskPalette(result.riskLevel)
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var showReportDialog by remember { mutableStateOf(false) }
    var showGuidanceDialog by remember { mutableStateOf(false) }
    var showStoppedDialog by remember { mutableStateOf(false) }
    var isSimpleMode by remember { mutableStateOf(false) }
    var evidenceExpanded by remember { mutableStateOf(false) }
    var feedbackRecorded by remember { mutableStateOf(false) }
    val hasPaymentData = result.recipientVpa != null || result.mismatchAmount != null || result.mismatchStatus == MismatchStatus.DETECTED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        // ── Mode & Edge Badge Bar ────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (result.isLocalEdgeResult) {
                Surface(
                    color = ColorBrandSurface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Offline Analysis",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorBrand,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Surface(
                    color = ColorSafeContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Cloud Analysis",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorSafe,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Simple Mode Toggle Button
            FilledTonalButton(
                onClick = { isSimpleMode = !isSimpleMode },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    if (isSimpleMode) Icons.Default.Visibility else Icons.Default.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (isSimpleMode) "Technical View" else "Explain Simply",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Risk Level Badge ─────────────────────────────
        var riskVisible by remember { mutableStateOf(false) }
        LaunchedEffect(result.scanId) { riskVisible = true }

        AnimatedVisibility(
            visible = riskVisible,
            enter = fadeIn() + slideInVertically { it / 2 }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        palette.label,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primary
                    )
                    Text(
                        "Threat Score: ${result.riskScore}/100",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.primary.copy(alpha = 0.8f)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(palette.container),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        palette.icon,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Simple Mode Banner (if toggled) ──────────────
        if (isSimpleMode) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, null, tint = ColorBrand)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Plain Language Explanation",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ColorBrand
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (result.mismatchStatus == MismatchStatus.DETECTED) {
                            "They said you would receive ${result.statedIntent ?: "funds"}.\n\n" +
                            "But this request actually asks you to PAY ${result.mismatchAmount?.let { "₹$it" } ?: "money"}.\n\n" +
                            "👉 DO NOT enter your UPI PIN. Entering your PIN always sends money, it never receives money."
                        } else if (result.riskLevel == RiskLevel.CRITICAL || result.riskLevel == RiskLevel.HIGH) {
                            "This message/link matches known fraud patterns.\n\n" +
                            "👉 Do not send any money, do not share OTPs, and do not click suspicious links."
                        } else {
                            "This payment/link appears to be a normal transaction.\n\n" +
                            "👉 Make sure you recognize the merchant or recipient before proceeding."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── PAYMENT DETAILS CARD ──
        if (hasPaymentData) {
            val isMismatch = result.mismatchStatus == MismatchStatus.DETECTED
            val cardColor = if (isMismatch) ColorCriticalContainer else palette.container
            val textColor = if (isMismatch) ColorCritical else palette.primary
            val titleText = if (isMismatch) "PAYMENT-INTENT MISMATCH" else "Payment Request Detected"

            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            titleText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor
                        )
                        if (isMismatch) {
                            Surface(
                                color = ColorCritical,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "YOU SEND MONEY",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    result.recipientVpa?.let { vpa ->
                        Text("Recipient VPA: $vpa", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    if (result.mismatchAmount != null) {
                        Text("Amount: ₹${result.mismatchAmount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Amount: Unavailable", style = MaterialTheme.typography.bodyMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(8.dp))

                    result.statedIntent?.let { intent ->
                        Text("You were told: $intent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    if (isMismatch) {
                        val actionStr = result.actualPaymentAction ?: "Outbound Debit"
                        val dirStr = result.paymentDirection?.let { " ($it)" } ?: ""
                        Text(
                            "Payment actually does: $actionStr$dirStr",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = ColorCritical
                        )

                        Spacer(Modifier.height(12.dp))
                        Surface(
                            color = ColorCritical,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Dangerous, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "INVERSION TRAP: They told you that you would receive money, but this request asks you to send money.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        Text(
                            "Standard Payment Action: Outbound Debit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Protection Action Card ────────────────────────
        Card(
            colors = CardDefaults.cardColors(containerColor = palette.container),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    result.detectedSummary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.primary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    result.userInstruction,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    result.whyItMatters,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Detection Analysis ───────────────────────────
        SectionCard(title = "Why This Was Flagged") {
            Text(
                result.humanExplanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (result.signals.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Identified Signals:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                result.signals.forEach { signal ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text("• ", color = palette.primary, fontWeight = FontWeight.Bold)
                        Text(signal.toDisplayName(), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // ── Scam Chain Stages ────────────────────────────
        if (result.scamChainNodes.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            SectionCard(title = "Attack Timeline (${result.scamChainNodes.size} steps)", tint = ColorHigh) {
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    result.scamChainNodes.forEachIndexed { index, node ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = ColorHigh,
                                shape = CircleShape,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("${index + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                val nodeLabel = node.entity_reference ?: node.node_id
                                Text(
                                    node.node_type.toDisplayName().uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorHigh
                                )
                                Text(
                                    nodeLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (index < result.scamChainNodes.size - 1) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 11.dp, top = 4.dp, bottom = 4.dp)
                                    .width(2.dp)
                                    .height(20.dp)
                                    .background(ColorHigh.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            }
        }

        // ── Expandable Evidence Pack ─────────────────────
        if (result.evidenceItems.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Technical Evidence (${result.evidenceItems.size} items)",
                            style = MaterialTheme.typography.labelLarge,
                            color = ColorBrand,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = { evidenceExpanded = !evidenceExpanded }) {
                            Icon(
                                if (evidenceExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Evidence"
                            )
                        }
                    }
                    if (evidenceExpanded) {
                        Spacer(Modifier.height(8.dp))
                        result.evidenceItems.forEach { item ->
                            Column(Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    item.evidence_type.toDisplayName(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorBrand
                                )
                                Text(
                                    item.explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "Data: ${item.data}",
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── INCIDENT RESPONSE ACTIONS ────────────────────
        Text(
            "Incident Response Actions",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        if (result.riskLevel == RiskLevel.CRITICAL || result.riskLevel == RiskLevel.HIGH) {
            Button(
                onClick = { showStoppedDialog = true },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorCritical)
            ) {
                Icon(Icons.Default.StopCircle, null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("DO NOT PROCEED — STOP TRANSACTION", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))

            FilledTonalButton(
                onClick = { showGuidanceDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Help, null)
                Spacer(Modifier.width(8.dp))
                Text("WHAT SHOULD I DO? (Step-by-Step)")
            }

            Spacer(Modifier.height(8.dp))
        }

        // ── VERDICT CALIBRATION FEEDBACK ────────────────
        if (result.riskLevel != RiskLevel.SAFE) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (!feedbackRecorded) {
                    Text(
                        "Was this accurate?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Was this safety verdict accurate?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.submitFeedback(result.scanId, result.recipientVpa, isConfirmedFraud = true)
                                feedbackRecorded = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Yes, this was a scam", style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.submitFeedback(result.scanId, result.recipientVpa, isConfirmedFraud = false)
                                feedbackRecorded = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("No, false alarm", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = ColorSafe, modifier = Modifier.size(20.dp))
                        Text(
                            "Thank you for calibrating RefGuard!",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorSafe,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

        Spacer(Modifier.height(16.dp))

        // ── Action Buttons ───────────────────────────────
        Button(
            onClick = onScanAnother,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ColorBrand)
        ) {
            Icon(Icons.Default.RestartAlt, null)
            Spacer(Modifier.width(8.dp))
            Text("Scan Another")
        }

        Spacer(Modifier.height(8.dp))

        // ── Mode 1: Share Warning Advisory (Family/Groups)
        OutlinedButton(
            onClick = {
                val shareText = buildString {
                    append("🛡️ RefGuard Scam Advisory\n")
                    append("Risk: ${result.riskLevel} (${result.riskScore}/100)\n")
                    append("Warning: ${result.detectedSummary}\n")
                    append("Action: ${result.userInstruction}\n\n")
                    append("Why: ${result.whyItMatters}\n")
                    append("\nVerified by RefGuard India UPI Protection Shield.")
                }
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share Scam Warning"))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Share, null)
            Spacer(Modifier.width(8.dp))
            Text("Share Warning (Family / Groups)")
        }

        Spacer(Modifier.height(8.dp))

        // ── Mode 2: Share Evidence Summary (Cybercrime / Bank Support)
        OutlinedButton(
            onClick = {
                val evidenceSummary = buildString {
                    append("🛡️ RefGuard Incident Investigation Report\n")
                    append("Incident ID: ${result.scanId}\n")
                    append("Timestamp: ${result.timestamp}\n")
                    append("Risk Severity: ${result.riskLevel} (${result.riskScore}/100)\n")
                    append("Summary: ${result.detectedSummary}\n")
                    if (result.recipientVpa != null) append("Target VPA: ${result.recipientVpa}\n")
                    if (result.mismatchAmount != null) append("Amount: ₹${result.mismatchAmount}\n")
                    if (result.signals.isNotEmpty()) append("Signals: ${result.signals.joinToString(", ")}\n")
                    append("\nEvidence Pack Hash: ${result.evidenceItems.size} items attached.\n")
                    append("Generated for submission to National Cybercrime Helpline 1930 / cybercrime.gov.in")
                }
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, evidenceSummary)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share Formal Evidence Summary"))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Description, null)
            Spacer(Modifier.width(8.dp))
            Text("Share Evidence Summary (Cybercrime 1930)")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                viewModel.saveInvestigationManually(result)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.BookmarkBorder, null)
            Spacer(Modifier.width(8.dp))
            Text("Save Investigation to History")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { showReportDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Flag, null)
            Spacer(Modifier.width(8.dp))
            Text("Report Indicator to Community")
        }

        Spacer(Modifier.height(24.dp))
    }

    // ── Transaction Stopped Safely Dialog ─────────────
    if (showStoppedDialog) {
        AlertDialog(
            onDismissRequest = { showStoppedDialog = false },
            title = { Text("Transaction Stopped Safely") },
            text = {
                Text(
                    "You have decided not to proceed. RefGuard has recorded this threat signature to keep you and your network protected.\n\n" +
                    "Remember: Never enter your UPI PIN on unverified requests.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showStoppedDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // ── What Should I Do? Guidance Dialog ────────────
    if (showGuidanceDialog) {
        AlertDialog(
            onDismissRequest = { showGuidanceDialog = false },
            title = { Text("What Should I Do Now?") },
            text = {
                Column {
                    Text("1. ${result.recommendedAction.ifBlank { "DO NOT Enter UPI PIN" }}", fontWeight = FontWeight.Bold, color = ColorCritical)
                    Text(result.userInstruction.ifBlank { "Entering your PIN authorizes outgoing payments from your bank account." }, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text("2. Block & Report Sender", fontWeight = FontWeight.Bold)
                    Text("Block the contact in WhatsApp, SMS, or Telegram immediately.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text("3. Why It Matters", fontWeight = FontWeight.Bold)
                    Text(result.whyItMatters.ifBlank { "Immediately call National Cybercrime Helpline 1930 or visit cybercrime.gov.in within 24 hours." }, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showGuidanceDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }

    // ── Community Report Dialog ───────────────────────
    if (showReportDialog) {
        ReportDialog(
            scanResult = result,
            reportState = reportState,
            onSubmit = { indicator, category, desc ->
                viewModel.submitReport(indicator, category, desc)
            },
            onDismiss = {
                showReportDialog = false
                viewModel.resetReportState()
            }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    tint: Color = ColorBrand,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = tint,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportDialog(
    scanResult: ScanResult,
    reportState: ReportUiState,
    onSubmit: (indicator: String, category: String, desc: String) -> Unit,
    onDismiss: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    val indicator = scanResult.recipientVpa ?: scanResult.evidenceItems.firstOrNull()?.data ?: "unknown"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Scam to Community") },
        text = {
            Column {
                when (reportState) {
                    is ReportUiState.Submitting -> {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is ReportUiState.Success -> {
                        Text(
                            "✓ Report submitted (ID: ${(reportState as ReportUiState.Success).reportId}). Thank you for helping protect the community.",
                            color = ColorSafe
                        )
                    }
                    is ReportUiState.Error -> {
                        Text("Failed: ${(reportState as ReportUiState.Error).message}", color = MaterialTheme.colorScheme.error)
                    }
                    else -> {
                        Text(
                            "Describe what you observed (do NOT include PINs, OTPs, or passwords).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it.take(500) },
                            placeholder = { Text("e.g. Pretended to be electricity department...") },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (reportState is ReportUiState.Idle || reportState is ReportUiState.Error) {
                TextButton(
                    onClick = { onSubmit(indicator, "PAYMENT_SCAM", description) },
                    enabled = description.isNotBlank()
                ) { Text("Submit") }
            } else if (reportState is ReportUiState.Success) {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = {
            if (reportState !is ReportUiState.Success) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

// ── Risk palette helper ──────────────────────────────────
data class RiskPalette(
    val label: String,
    val primary: Color,
    val container: Color,
    val icon: ImageVector
)

fun riskPalette(level: RiskLevel): RiskPalette = when (level) {
    RiskLevel.SAFE -> RiskPalette("Safe", ColorSafe, ColorSafeContainer, Icons.Default.CheckCircle)
    RiskLevel.WARNING -> RiskPalette("Caution", ColorWarning, ColorWarningContainer, Icons.Default.Warning)
    RiskLevel.HIGH -> RiskPalette("High Risk", ColorHigh, ColorHighContainer, Icons.Default.Error)
    RiskLevel.CRITICAL -> RiskPalette("CRITICAL — SCAM", ColorCritical, ColorCriticalContainer, Icons.Default.Dangerous)
    RiskLevel.UNKNOWN -> RiskPalette("Unknown", Color.Gray, Color.LightGray, Icons.AutoMirrored.Filled.HelpOutline)
}

fun String.toDisplayName(): String {
    return this.lowercase().replace("_", " ").split(" ").joinToString(" ") {
        it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else it }
    }
}

