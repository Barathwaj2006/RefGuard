package com.refguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    var showReportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        // ── Risk Level Badge ─────────────────────────────
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
                    "Risk Score: ${result.riskScore}/100",
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

        Spacer(Modifier.height(16.dp))

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

        // ── AI Explanation ───────────────────────────────
        SectionCard(title = "What We Found") {
            Text(
                result.humanExplanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (result.signals.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Signals:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                result.signals.forEach { signal ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text("• ", color = palette.primary, fontWeight = FontWeight.Bold)
                        Text(signal, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // ── Payment Mismatch (if relevant) ───────────────
        if (result.mismatchStatus == MismatchStatus.DETECTED) {
            Spacer(Modifier.height(12.dp))
            SectionCard(title = "Payment Intent Mismatch Detected", tint = ColorWarning) {
                result.statedIntent?.let {
                    Text("Stated intent: $it", style = MaterialTheme.typography.bodySmall)
                }
                result.actualPaymentAction?.let {
                    Text("Actual action: $it", style = MaterialTheme.typography.bodySmall, color = ColorHigh)
                }
                result.recipientVpa?.let {
                    Text("Recipient VPA: $it", style = MaterialTheme.typography.bodySmall)
                }
                result.mismatchAmount?.let {
                    Text("Amount: ₹$it", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // ── Scam Chain (if present) ───────────────────────
        if (result.scamChainNodes.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            SectionCard(title = "Scam Network (${result.scamChainNodes.size} nodes)", tint = ColorHigh) {
                Text(
                    "This entity is connected to a known scam network.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorHigh
                )
                result.scamChainNodes.take(3).forEach { node ->
                    Text("• ${node.node_type}: ${node.entity_reference ?: node.node_id}",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // ── Recommended Action ───────────────────────────
        Spacer(Modifier.height(12.dp))
        SectionCard(title = "Recommended Action") {
            Text(
                result.recommendedAction,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }

        // ── Confidence ───────────────────────────────────
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Analysis Confidence",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${(result.riskConfidence * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = palette.primary
            )
        }
        LinearProgressIndicator(
            progress = { result.riskConfidence.toFloat() },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
            color = palette.primary,
            trackColor = palette.container
        )

        Spacer(Modifier.height(24.dp))

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

        OutlinedButton(
            onClick = { showReportDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Flag, null)
            Spacer(Modifier.width(8.dp))
            Text("Report to Community")
        }

        Spacer(Modifier.height(24.dp))
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
    val indicator = scanResult.evidenceItems.firstOrNull()?.data ?: "unknown"

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
                            "✓ Report submitted (ID: ${reportState.reportId}). Thank you for helping protect the community.",
                            color = ColorSafe
                        )
                    }
                    is ReportUiState.Error -> {
                        Text("Failed: ${reportState.message}", color = MaterialTheme.colorScheme.error)
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
    RiskLevel.UNKNOWN -> RiskPalette("Unknown", Color.Gray, Color.LightGray, Icons.Default.HelpOutline)
}
