package com.refguard.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.refguard.app.domain.RiskLevel
import com.refguard.app.domain.ScanResult
import com.refguard.app.ui.theme.*
import com.refguard.app.viewmodel.ReportUiState
import com.refguard.app.viewmodel.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionScreen(
    result: ScanResult,
    viewModel: ScanViewModel,
    onNavigateBack: () -> Unit,
    onReturnHome: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val reportState by viewModel.reportState.collectAsStateWithLifecycle()

    var showReportDialog by remember { mutableStateOf(false) }
    var reportCategory by remember { mutableStateOf("PAYMENT_MISMATCH") }
    var reportDescription by remember { mutableStateOf("") }

    val isCritical = result.riskLevel == RiskLevel.CRITICAL || result.riskScore >= 80
    val isWarning = result.riskLevel == RiskLevel.HIGH || result.riskLevel == RiskLevel.WARNING || result.riskScore in 30..79

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incident Action Plan", fontWeight = FontWeight.Bold, color = ColorBrand) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ColorBrand)
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

            if (isCritical || isWarning) {
                // ── CRITICAL STOP BANNER ─────────────────────
                Card(
                    colors = CardDefaults.cardColors(containerColor = ColorCritical),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.FrontHand,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "STOP — DO NOT AUTHORIZE PAYMENT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            "Do not enter your UPI PIN or approve any payment prompt.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // ── THREE GOLDEN PROTECTIVE RULES ────────────
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Immediate Protective Rules",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ColorBrand
                        )
                        Spacer(Modifier.height(12.dp))

                        GoldenRuleRow(
                            number = 1,
                            title = "Never Enter Your UPI PIN",
                            description = "UPI PIN is ONLY used to send money. You never need to enter a PIN to receive cashback or rewards."
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                        GoldenRuleRow(
                            number = 2,
                            title = "Never Share OTPs or Passwords",
                            description = "Banks and authentic companies will never call or message asking for one-time passwords."
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                        GoldenRuleRow(
                            number = 3,
                            title = "Reject the Payment in Your UPI App",
                            description = "Decline or delete the collect request if it appears inside Google Pay, PhonePe, or Paytm."
                        )
                    }
                }

                // ── INCIDENT RESPONSE ACTIONS ────────────────
                Text(
                    "Incident Response Actions",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ActionTile(
                    icon = Icons.Default.Phone,
                    title = "Call National Cybercrime Helpline (1930)",
                    subtitle = "Official Indian Ministry of Home Affairs fraud helpline",
                    tint = ColorBrand
                ) {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930"))
                    context.startActivity(intent)
                }

                ActionTile(
                    icon = Icons.Default.ContentCopy,
                    title = "Copy Incident Evidence for Cybercrime Report",
                    subtitle = "Formats report for reporting on cybercrime.gov.in",
                    tint = ColorBrandLight
                ) {
                    val reportText = buildEvidenceReportText(result)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("RefGuard Evidence Report", reportText))
                    Toast.makeText(context, "Evidence copied to clipboard!", Toast.LENGTH_SHORT).show()
                }

                ActionTile(
                    icon = Icons.Default.Share,
                    title = "Share Scam Warning to Family & Groups",
                    subtitle = "Warn contacts about this UPI handle and deceptive lure",
                    tint = ColorSafe
                ) {
                    val advisory = buildShareAdvisory(result)
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, advisory)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Scam Warning"))
                }

                ActionTile(
                    icon = Icons.Default.Campaign,
                    title = "Report to RefGuard Community Shield",
                    subtitle = "Contribute to fraud threat database & protect others",
                    tint = ColorWarning
                ) {
                    showReportDialog = true
                }
            } else {
                // Safe Transaction Guidance
                Card(
                    colors = CardDefaults.cardColors(containerColor = ColorSafeContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = null,
                            tint = ColorSafe,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Transaction Verified Safe",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorSafe
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "No malicious intent or fraudulent signatures were detected. You may proceed with standard vigilance.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Standard Pre-Payment Checklist",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ColorBrand
                        )
                        Spacer(Modifier.height(12.dp))
                        ChecklistRow(text = "Verify the recipient name on your bank's final confirmation screen.")
                        ChecklistRow(text = "Confirm the transaction amount before entering your UPI PIN.")
                        ChecklistRow(text = "Ensure you initiated this payment deliberately.")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onReturnHome,
                colors = ButtonDefaults.buttonColors(containerColor = ColorBrand),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("DONE — RETURN TO HOME", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ── COMMUNITY REPORT DIALOG ──────────────────────────
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = {
                showReportDialog = false
                viewModel.resetReportState()
            },
            title = {
                Text(
                    "Report Scam Indicator",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorBrand
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Submit this handle to the community threat shield. Never include PINs or bank passwords.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = result.recipientVpa ?: result.scanId,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Reported Indicator") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = reportDescription,
                        onValueChange = { reportDescription = it },
                        placeholder = { Text("Optional notes (e.g. Scammer contacted via WhatsApp offering fake electricity bill refund)") },
                        label = { Text("Incident Context") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    when (reportState) {
                        is ReportUiState.Submitting -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Submitting to threat network...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        is ReportUiState.Success -> {
                            Text(
                                "✓ Report submitted successfully (ID: ${(reportState as ReportUiState.Success).reportId})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = ColorSafe
                            )
                        }
                        is ReportUiState.Error -> {
                            Text(
                                (reportState as ReportUiState.Error).message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {}
                    }
                }
            },
            confirmButton = {
                if (reportState is ReportUiState.Success) {
                    TextButton(onClick = {
                        showReportDialog = false
                        viewModel.resetReportState()
                    }) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.submitReport(
                                reportedIndicator = result.recipientVpa ?: "threat_${result.scanId}",
                                category = reportCategory,
                                description = reportDescription.ifBlank { result.detectedSummary }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorBrand)
                    ) {
                        Text("Submit Report")
                    }
                }
            },
            dismissButton = {
                if (reportState !is ReportUiState.Success) {
                    TextButton(onClick = {
                        showReportDialog = false
                        viewModel.resetReportState()
                    }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
private fun GoldenRuleRow(
    number: Int,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(ColorCritical),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$number",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun ChecklistRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ColorSafe, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun buildEvidenceReportText(result: ScanResult): String {
    return """
REFGUARD SCAM INCIDENT REPORT
------------------------------------------------
Scan ID: ${result.scanId}
Timestamp: ${result.timestamp}
Threat Severity: ${result.riskLevel.name} (${result.riskScore}/100)
Target VPA: ${result.recipientVpa ?: "N/A"}
Amount: ${result.mismatchAmount ?: "N/A"}
Detected Mechanism: ${result.detectedSummary}
Why It Matters: ${result.whyItMatters}

Signals:
${result.signals.joinToString("\n") { "- $it" }}

Generated via RefGuard Payment Threat Shield.
Report to https://cybercrime.gov.in or Helpline 1930.
    """.trimIndent()
}

private fun buildShareAdvisory(result: ScanResult): String {
    val vpa = result.recipientVpa?.let { " Target VPA: $it." } ?: ""
    return "🚨 SCAM WARNING: Be careful! A fraudulent payment request was detected.$vpa RefGuard verified this as an outgoing debit scam. Never enter your UPI PIN to receive money! Report scams to 1930."
}
