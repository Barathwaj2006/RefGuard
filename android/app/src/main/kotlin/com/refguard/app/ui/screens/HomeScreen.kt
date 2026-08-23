package com.refguard.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    onNavigateToScan: () -> Unit,       // trigger QR scanner
    onNavigateToImagePicker: () -> Unit // trigger image picker
) {
    val context = LocalContext.current
    var manualInput by remember { mutableStateOf("") }
    var isUpiInput by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── Header ────────────────────────────────────────
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "RefGuard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorBrand
                )
                Text(
                    "Real-time payment scam protection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = ColorSafeContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = ColorSafe,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Protected",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ColorSafe
                    )
                }
            }
        }

        if (offlineQueueSize > 0) {
            Spacer(Modifier.height(12.dp))
            Surface(
                color = ColorWarningContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudQueue, null, tint = ColorWarning, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            " scans queued offline",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = ColorWarning
                        )
                    }
                    TextButton(onClick = { viewModel.flushOfflineQueue() }) {
                        Text("Sync Now", fontWeight = FontWeight.Bold, color = ColorBrand)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Manual Input Card ────────────────────────────
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Enter UPI ID, Payment Link, or Text",
                    style = MaterialTheme.typography.labelLarge,
                    color = ColorBrand
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    placeholder = { Text("e.g. merchant@upi or upi://pay?...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (manualInput.isNotEmpty()) {
                            IconButton(onClick = { manualInput = "" }) {
                                Icon(Icons.Default.Clear, "Clear")
                            }
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isUpiInput, onCheckedChange = { isUpiInput = it })
                    Text("Treat strictly as UPI VPA", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (manualInput.isNotBlank()) {
                            val request = ScanRequest(
                                contentType = if (isUpiInput) ContentType.UPI_VPA
                                              else if (manualInput.startsWith("http")) ContentType.URL
                                              else ContentType.TEXT,
                                contentValue = manualInput,
                                sourceContext = "com.refguard.manual",
                                timestamp = Instant.now().toString()
                            )
                            viewModel.handleIngressResult(IngressResult.Success(request))
                        }
                    },
                    enabled = manualInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorBrand)
                ) {
                    Icon(Icons.Default.Search, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Analyze Threat Risk")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Quick Actions ────────────────────────────────
        Text(
            "Instant Ingress Channels",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.QrCodeScanner,
                label = "Scan QR",
                tint = ColorBrand
            ) { onNavigateToScan() }

            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ContentPaste,
                label = "Paste Clip",
                tint = ColorBrandLight
            ) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                if (!text.isNullOrBlank()) {
                    val request = ScanRequest(
                        contentType = if (text.startsWith("http")) ContentType.URL else ContentType.TEXT,
                        contentValue = text,
                        sourceContext = "com.android.clipboard",
                        timestamp = Instant.now().toString()
                    )
                    viewModel.handleIngressResult(IngressResult.Success(request))
                } else {
                    viewModel.handleIngressResult(
                        IngressResult.Failure(com.refguard.platform.models.IngressError.EmptyContent)
                    )
                }
            }

            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Image,
                label = "Screenshot",
                tint = ColorSafe
            ) { onNavigateToImagePicker() }
        }

        Spacer(Modifier.height(24.dp))

        // ── Security Notice ──────────────────────────────
        Card(
            colors = CardDefaults.cardColors(containerColor = ColorBrandSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = ColorBrand,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Zero-Credential Guarantee: RefGuard never collects or reads UPI PINs, passwords, or bank OTPs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorBrand
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                color = tint
            )
        }
    }
}
