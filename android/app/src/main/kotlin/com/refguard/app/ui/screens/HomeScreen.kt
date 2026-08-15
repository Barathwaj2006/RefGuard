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
    onNavigateToScan: () -> Unit,       // trigger QR scanner activity-level flow
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "RefGuard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorBrand
                )
                Text(
                    "Scam detection, before you pay.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (offlineQueueSize > 0) {
                Badge(containerColor = ColorWarning) {
                    Text("$offlineQueueSize queued", color = MaterialTheme.colorScheme.onError)
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        // ── Manual Input Card ────────────────────────────
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Enter UPI ID or URL",
                    style = MaterialTheme.typography.labelLarge,
                    color = ColorBrand
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    placeholder = { Text("e.g. user@bank or https://...") },
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
                    Text("This is a UPI VPA", style = MaterialTheme.typography.bodySmall)
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
                    Text("Scan")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Quick Actions ────────────────────────────────
        Text(
            "Quick Actions",
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
                label = "Paste",
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
                    "RefGuard never collects your UPI PIN, OTP, or bank credentials.",
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
