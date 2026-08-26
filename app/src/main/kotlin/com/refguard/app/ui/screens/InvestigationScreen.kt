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
                            "ID: ${result.scanId.take(16)}",
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── 1. WHAT DID I PROVIDE? ───────────────────────
            InvestigationCard(title = "1. WHAT WAS ANALYZED", icon = Icons.Default.Source) {
                Text(
                    "Input Source: ${if (result.isLocalEdgeResult) "Direct Ingress / Manual Input" else "Protected Android Pipeline"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Timestamp: ${result.timestamp}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (result.recipientVpa != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Target VPA: ${result.recipientVpa}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorBrand
                    )
                }
            }

            // ── 2. WHAT DID REFGUARD FIND? ───────────────────
            InvestigationCard(title = "2. SECURITY FINDINGS & SIGNALS", icon = Icons.Default.Troubleshoot) {
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
                        "No suspicious threat signals observed in this payload.",
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

            // ── 3. WHAT IS THE PAYMENT ACTUALLY DOING? ────────
            if (result.mismatchStatus == MismatchStatus.DETECTED || result.statedIntent != null || result.recipientVpa != null) {
                InvestigationCard(title = "3. INTENT VS PAYMENT REALITY", icon = Icons.Default.CompareArrows) {
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
                                    "Outbound Debit of ₹${result.mismatchAmount} to ${result.recipientVpa ?: "unverified VPA"}"
                                } else {
                                    "Outgoing Debit authorization from your bank account"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorCritical
                            )
                        }
                    }
                }
            }

            // ── 4. WHY IS THIS DANGEROUS? ────────────────────
            InvestigationCard(title = "4. WHY THIS IS DANGEROUS", icon = Icons.Default.SecurityUpdateWarning) {
                Text(
                    text = result.whyItMatters.ifBlank { result.humanExplanation },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (result.userInstruction.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
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

            // ── 5. WHAT EVIDENCE SUPPORTS THIS? ──────────────
            if (result.evidenceItems.isNotEmpty()) {
                InvestigationCard(title = "5. VERIFIED EVIDENCE ARTIFACTS", icon = Icons.Default.FactCheck) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        result.evidenceItems.forEach { item ->
                            val evidenceDisplay = HumanMappers.mapEvidenceItem(item)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            evidenceDisplay.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorBrand
                                        )
                                        Text(
                                            evidenceDisplay.source,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Value: ${evidenceDisplay.content}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
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

            // ── 6. HOW THIS SCAM WORKS (SCAMCHAIN) ───────────
            if (result.scamChainNodes.isNotEmpty()) {
                InvestigationCard(title = "6. SCAM EXECUTION CHAIN", icon = Icons.Default.AccountTree) {
                    Text(
                        "Observed step-by-step progression of the threat:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        result.scamChainNodes.forEachIndexed { index, node ->
                            val step = HumanMappers.mapScamChainNode(node, index, isCritical)
                            val isLast = index == result.scamChainNodes.size - 1

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(32.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (step.isCritical) ColorCritical else ColorBrand),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${step.stepNumber}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    if (!isLast) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(36.dp)
                                                .background(if (step.isCritical) ColorCritical.copy(alpha = 0.4f) else ColorBrand.copy(alpha = 0.2f))
                                        )
                                    }
                                }

                                Spacer(Modifier.width(8.dp))

                                Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 12.dp)) {
                                    Text(
                                        step.stageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (step.isCritical) ColorCritical else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        step.detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 7. TECHNICAL DETAILS & PROVENANCE (COLLAPSIBLE)
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
                                "Technical Details & Provenance",
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
                            TechRow("Confidence", "${(result.riskConfidence * 100).toInt()}%")
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
