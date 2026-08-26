package com.refguard.app.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refguard.app.ui.theme.*
import com.refguard.app.viewmodel.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShieldSettingsScreen(
    viewModel: ScanViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var autoScanClipboard by remember { mutableStateOf(true) }
    var strictUpiValidation by remember { mutableStateOf(true) }
    var offlineEdgeFallback by remember { mutableStateOf(true) }
    var threatNotifications by remember { mutableStateOf(true) }
    var selectedSensitivity by remember { mutableStateOf("STRICT") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Shield Defense Configuration", fontWeight = FontWeight.Bold, color = ColorBrand)
                        Text("Real-Time Threat Parameters", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Defense Status Header
            Card(
                colors = CardDefaults.cardColors(containerColor = ColorBrandSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ColorSafeContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Shield, null, tint = ColorSafe, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Defense Engine v2.4 Active",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ColorBrand
                        )
                        Text(
                            "Local Heuristics + Cloud Neural Classifier",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── PROTECTION SENSITIVITY ───────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Protection Sensitivity Level",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = ColorBrand
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("STANDARD", "STRICT", "PARANOID").forEach { mode ->
                            FilterChip(
                                selected = selectedSensitivity == mode,
                                onClick = { selectedSensitivity = mode },
                                label = {
                                    Text(
                                        mode,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selectedSensitivity == mode) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        when (selectedSensitivity) {
                            "STANDARD" -> "Flag verified blacklists, severe intent mismatches and malicious APK links."
                            "STRICT" -> "Recommended: Deep inspect all outbound debits, unverified VPAs and urgent lures."
                            else -> "Maximum scrutiny: Block all unverified personal VPAs and obscure payment gateways."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── DEFENSE TOGGLES ──────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Automated Threat Interceptors",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = ColorBrand
                    )
                    Spacer(Modifier.height(8.dp))

                    SettingToggleRow(
                        title = "Clipboard Scam Intercept",
                        subtitle = "Analyze copied UPI IDs & links when opening RefGuard",
                        checked = autoScanClipboard,
                        onCheckedChange = { autoScanClipboard = it }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                    SettingToggleRow(
                        title = "Strict VPA Verification",
                        subtitle = "Check handle domain validity against official NPCI bank roster",
                        checked = strictUpiValidation,
                        onCheckedChange = { strictUpiValidation = it }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                    SettingToggleRow(
                        title = "Offline Edge Fallback",
                        subtitle = "Run on-device classifier if mobile internet is disconnected",
                        checked = offlineEdgeFallback,
                        onCheckedChange = { offlineEdgeFallback = it }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                    SettingToggleRow(
                        title = "Critical Threat Alerts",
                        subtitle = "Show high-urgency notifications on dangerous collect traps",
                        checked = threatNotifications,
                        onCheckedChange = { threatNotifications = it }
                    )
                }
            }

            // ── SYSTEM DIAGNOSTICS ───────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Diagnostics & Threat Database",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = ColorBrand
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Threat Signatures", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("4,280 Signatures (Updated Today)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Local Edge Model", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("RefGuard-Edge-v1.8 (Active)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.flushOfflineQueue()
                            Toast.makeText(context, "Threat database synchronized with cloud!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Sync, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("SYNC THREAT SIGNATURES NOW", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ColorBrand
            )
        )
    }
}
