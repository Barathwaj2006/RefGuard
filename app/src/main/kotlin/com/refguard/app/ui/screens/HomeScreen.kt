package com.refguard.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.refguard.app.history.HistoryItem
import com.refguard.app.ui.theme.*
import com.refguard.app.viewmodel.ScanViewModel
import com.refguard.platform.models.ContentType
import com.refguard.platform.models.IngressResult
import com.refguard.platform.models.ScanRequest
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ScanViewModel,
    offlineQueueSize: Int,
    onNavigateToAnalyze: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToImagePicker: () -> Unit,
    onNavigateToSandbox: () -> Unit,
    onNavigateToRadar: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenInvestigation: (String) -> Unit
) {
    val context = LocalContext.current
    val historyItems by viewModel.history.collectAsStateWithLifecycle()

    // Smooth pulse for active shield badge
    val infiniteTransition = rememberInfiniteTransition(label = "ShieldGlow")
    val shieldPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ShieldPulse"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ColorBrand),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "RefGuard",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = ColorBrand
                            )
                            Text(
                                "Pre-Payment Scam Defense",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        color = ColorSafeContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable { onNavigateToSettings() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(ColorSafe.copy(alpha = shieldPulse), CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "SHIELD ON",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorSafe
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(Modifier.height(2.dp))

                // ── Offline Queue Notice (if items pending) ──
                if (offlineQueueSize > 0) {
                    Surface(
                        color = ColorWarningContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudQueue, null, tint = ColorWarning, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "$offlineQueueSize checks saved offline",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorWarning
                                )
                            }
                            TextButton(
                                onClick = { viewModel.flushOfflineQueue() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Sync Now", fontWeight = FontWeight.Bold, color = ColorBrand)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // ── HERO PRIMARY SCAN BANNER ─────────────────
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onNavigateToAnalyze() },
                    colors = CardDefaults.cardColors(containerColor = ColorBrand),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "ZERO-TRUST SCANNER",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E5FF)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Inspect Before You Pay",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Paste WhatsApp messages, UPI payment links, or QR codes to intercept fraudulent debit requests.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Check a message",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        Button(
                            onClick = onNavigateToAnalyze,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E5FF),
                                contentColor = Color(0xFF0D1B48)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("CHECK MESSAGE / UPI ID NOW", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // ── QUICK ACCESS 4-WAY GRID ──────────────────────
            item {
                Text(
                    "Quick Threat Actions",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.QrCodeScanner,
                        label = "Scan QR Code",
                        badge = "Camera",
                        tint = ColorBrand
                    ) { onNavigateToScan() }

                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ContentPaste,
                        label = "Paste Clipboard",
                        badge = "Auto",
                        tint = ColorBrandLight
                    ) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                        if (!text.isNullOrBlank()) {
                            val request = ScanRequest(
                                contentType = if (text.startsWith("http://", true) || text.startsWith("https://", true)) {
                                    ContentType.URL
                                } else if (text.contains("@")) {
                                    ContentType.UPI_VPA
                                } else {
                                    ContentType.TEXT
                                },
                                contentValue = text.trim(),
                                sourceContext = "com.android.clipboard",
                                timestamp = Instant.now().toString()
                            )
                            viewModel.handleIngressResult(IngressResult.Success(request))
                        } else {
                            onNavigateToAnalyze()
                        }
                    }

                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Image,
                        label = "Scan Image",
                        badge = "Gallery",
                        tint = ColorSafe
                    ) { onNavigateToImagePicker() }

                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Science,
                        label = "Threat Lab",
                        badge = "Demo",
                        tint = ColorWarning
                    ) { onNavigateToSandbox() }
                }
            }

            // ── SECURITY HIGHLIGHT & THREAT RADAR PREVIEW ────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToRadar() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(ColorCriticalContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CrisisAlert, null, tint = ColorCritical, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Active Threat Alert",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorCritical
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("• CERT-In", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Beware of WhatsApp APKs masquerading as Power Bills.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            // ── RECENT CHECKS HEADER ─────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Scam Checks",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (historyItems.isNotEmpty()) {
                        Text(
                            "${historyItems.size} recorded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // ── RECENT CHECKS LIST / CLEAN EMPTY STATE ───────
            if (historyItems.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.FactCheck,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "No scam checks yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Paste any suspicious SMS, payment link, or try our Threat Lab scenarios to test the shield.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                            TextButton(onClick = onNavigateToSandbox) {
                                Icon(Icons.Default.Science, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Try Threat Lab Scenarios", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(historyItems.take(8), key = { it.scanId }) { item ->
                    RecentCheckItem(
                        item = item,
                        onClick = { onOpenInvestigation(item.scanId) }
                    )
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    badge: String,
    tint: Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 6.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(tint.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentCheckItem(
    item: HistoryItem,
    onClick: () -> Unit
) {
    val isCritical = item.riskLevelName.equals("CRITICAL", true) || item.riskScore >= 80
    val isWarning = item.riskLevelName.equals("HIGH", true) || item.riskLevelName.equals("WARNING", true) || item.riskScore in 30..79

    val badgeColor = when {
        isCritical -> ColorCritical
        isWarning -> ColorWarning
        else -> ColorSafe
    }
    val badgeContainer = when {
        isCritical -> ColorCriticalContainer
        isWarning -> ColorWarningContainer
        else -> ColorSafeContainer
    }
    val badgeLabel = when {
        isCritical -> "CRITICAL"
        isWarning -> "CAUTION"
        else -> "SAFE"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title.ifBlank { "Scam Check Record" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    item.subtitle.ifBlank { "Threat Score: ${item.riskScore}/100" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(12.dp))

            Surface(
                color = badgeContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "$badgeLabel (${item.riskScore})",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
        }
    }
}
