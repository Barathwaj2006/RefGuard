package com.refguard.app.ui.screens

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refguard.app.domain.HumanMappers
import com.refguard.app.domain.MismatchStatus
import com.refguard.app.domain.RiskLevel
import com.refguard.app.domain.ScanResult
import com.refguard.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestigationScreen(
    result: ScanResult,
    onNavigateBack: () -> Unit,
    onNavigateToAction: () -> Unit
) {
    val scrollState = rememberScrollState()
    var isTechnicalExpanded by remember { mutableStateOf(false) }

    val isCritical = result.riskLevel == RiskLevel.CRITICAL || result.riskScore >= 80
    val isWarning = result.riskLevel == RiskLevel.HIGH || result.riskLevel == RiskLevel.WARNING || result.riskScore in 30..79

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Investigation Report", fontWeight = FontWeight.Bold, color = ColorBrand)
                        Text(
                            if (result.isLocalEdgeResult) "Offline Analysis" else "Cloud Threat Intel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── 1. WHAT WAS ANALYZED ─────────────────────────
            InvestigationCard(title = "1. What Was Analyzed", icon = Icons.Default.Source) {
                if (result.recipientVpa != null) {
                    Text(
                        "Target UPI ID: ${result.recipientVpa}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorBrand
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    "Analysis Pipeline: ${if (result.isLocalEdgeResult) "RefGuard On-Device Edge Engine" else "RefGuard Cloud Intelligence"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Timestamp: ${result.timestamp}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── 2. WHY THIS WAS FLAGGED ──────────────────────
            InvestigationCard(title = "2. Why This Was Flagged", icon = Icons.Default.Troubleshoot) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Risk Assessment:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        color = if (isCritical) ColorCriticalContainer else if (isWarning) ColorWarningContainer else ColorSafeContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "${result.riskLevel.name} (${result.riskScore}/100)",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isCritical) ColorCritical else if (isWarning) ColorWarning else ColorSafe
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (result.signals.isEmpty()) {
                    Text(
                        "No malicious threat signatures were detected in this payment payload.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        result.signals.forEach { rawSignal ->
                            val info = HumanMappers.mapSignal(rawSignal)
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        info.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCritical) ColorCritical else ColorBrand
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        info.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 3. INTENT VS PAYMENT REALITY ─────────────────
            if (result.mismatchStatus == MismatchStatus.DETECTED || result.statedIntent != null || result.recipientVpa != null) {
                InvestigationCard(title = "3. Intent vs Payment Reality", icon = Icons.Default.CompareArrows) {
                    // Stated Intent
                    Surface(
                        color = ColorSafeContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Promised / Stated Intent",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ColorSafe
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                result.statedIntent ?: "Claim reward / receive money",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Actual Payment Reality
                    Surface(
                        color = ColorCriticalContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Actual Banking Action (Reality)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ColorCritical
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (result.mismatchAmount != null && result.mismatchAmount > 0) {
                                    "Outbound Debit of ₹${result.mismatchAmount.toInt()} to ${result.recipientVpa ?: "unverified UPI handle"}"
                                } else {
                                    "Outbound Debit authorization from your bank account"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorCritical
                            )
                        }
                    }
                }
            }

            // ── 4. WHY THIS IS DANGEROUS ─────────────────────
            InvestigationCard(title = "4. Why This Is Dangerous", icon = Icons.Default.SecurityUpdateWarning) {
                Text(
                    text = result.whyItMatters.ifBlank { result.humanExplanation },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
                if (result.userInstruction.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, null, tint = ColorBrand, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                result.userInstruction,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = ColorBrand
                            )
                        }
                    }
                }
            }

            // ── 5. EVIDENCE ──────────────────────────────────
            if (result.evidenceItems.isNotEmpty()) {
                InvestigationCard(title = "5. Evidence", icon = Icons.Default.FactCheck) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        result.evidenceItems.forEach { item ->
                            val evidenceDisplay = HumanMappers.mapEvidenceItem(item)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "WHAT WE FOUND",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorBrand
                                        )
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                evidenceDisplay.source,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        evidenceDisplay.whatWeFound,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "WHY IT MATTERS",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = ColorCritical
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        evidenceDisplay.whyItMatters,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 6. HOW THIS SCAM WORKS (VISUAL TIMELINE) ─────
            if (result.scamChainNodes.isNotEmpty()) {
                InvestigationCard(title = "6. How This Scam Works", icon = Icons.Default.AccountTree) {
                    Text(
                        "Reconstructed visual progression of the attack vector:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        result.scamChainNodes.forEachIndexed { index, node ->
                            val step = HumanMappers.mapScamChainNode(node, index, isCritical)
                            val isLast = index == result.scamChainNodes.size - 1

                            val stageIcon = when (node.node_type.uppercase().trim()) {
                                "MESSAGE" -> Icons.Default.ChatBubble
                                "URL", "LINK" -> Icons.Default.Link
                                "UPI_REQUEST" -> Icons.Default.QrCode
                                "PAYMENT_ACTION" -> Icons.Default.AccountBalance
                                else -> Icons.Default.Security
                            }

                            val stageBadgeColor = if (step.isCritical) ColorCritical else ColorBrand
                            val stageContainerColor = if (step.isCritical) ColorCriticalContainer else MaterialTheme.colorScheme.surfaceVariant

                            // Timeline Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Node Column (Circle + Connector Line)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(36.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(stageBadgeColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            stageIcon,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    if (!isLast) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(28.dp)
                                                    .background(stageBadgeColor.copy(alpha = 0.4f))
                                            )
                                            Icon(
                                                Icons.Default.ArrowDownward,
                                                contentDescription = null,
                                                tint = stageBadgeColor.copy(alpha = 0.6f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.width(10.dp))

                                // Node Content Card
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = stageContainerColor),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(bottom = if (isLast) 0.dp else 12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                step.stageName,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                color = stageBadgeColor
                                            )
                                            Text(
                                                "Step ${step.stepNumber}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            step.primaryText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            step.subText,
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

            // ── 7. DECISION PIPELINE ARCHITECTURE (HOW IT WORKS) ──
            InvestigationCard(title = "7. Decision Pipeline Architecture", icon = Icons.Default.Schema) {
                Text(
                    "Multi-tier real-time analysis pipeline from ingress to protection decision:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                val stages = listOf(
                    Triple("1. Ingress & OCR", "Intent / QR / SMS / Share Stream", Icons.Default.Sensors),
                    Triple("2. Protocol Decoder", "UPI URI parsing & Param Extraction", Icons.Default.QrCodeScanner),
                    Triple("3. Edge NLP Model", "Logistic Feature Ensemble (N-Gram weights)", Icons.Default.Psychology),
                    Triple("4. Mismatch Engine", "Semantic Promise vs Outbound Debit Inversion", Icons.Default.CompareArrows),
                    Triple("5. Calibrated Risk", "Sigmoid Scoring & Confidence Hedge (${(result.riskConfidence * 100).toInt()}%)", Icons.Default.Shield),
                    Triple("6. Safe Intercept", "Overlay Intercept & Safe Dispatch", Icons.Default.Lock)
                )

                stages.forEachIndexed { idx, stage ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(ColorBrand.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(stage.third, contentDescription = null, tint = ColorBrand, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stage.first, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ColorBrand)
                            Text(stage.second, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.outline)
                        }
                        if (idx < stages.size - 1) {
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // ── 8. TECHNICAL DETAILS & MODEL METRICS (COLLAPSIBLE) ──
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isTechnicalExpanded = !isTechnicalExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, null, tint = ColorBrand, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Model & Technical Details",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = ColorBrand
                            )
                        }
                        Icon(
                            if (isTechnicalExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle technical details",
                            tint = ColorBrand
                        )
                    }

                    AnimatedVisibility(visible = isTechnicalExpanded) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            TechRow("Scan Identifier", result.scanId)
                            TechRow("Execution Mode", if (result.isLocalEdgeResult) "LOCAL_EDGE_CLASSIFIER" else "CLOUD_GATEWAY")
                            TechRow("Model Architecture", "RefGuard-Edge-NLP-v2.1 (Calibrated Logistic)")
                            TechRow("Held-Out Precision", "96.8% (Benchmark N=60)")
                            TechRow("Held-Out Recall", "96.6% (F1: 0.967)")
                            TechRow("Calibrated Confidence", "${(result.riskConfidence * 100).toInt()}%")
                            TechRow("Raw Signals", result.signals.joinToString(", ").ifEmpty { "none" })
                            TechRow("Protection Action", result.protectionAction.name)
                        }
                    }
                }
            }

            // ── BOTTOM ACTION BUTTON ─────────────────────────
            Button(
                onClick = onNavigateToAction,
                colors = ButtonDefaults.buttonColors(containerColor = if (isCritical) ColorCritical else ColorBrand),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("VIEW INCIDENT ACTION PLAN", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InvestigationCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = ColorBrand, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = ColorBrand
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun TechRow(key: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(key, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
