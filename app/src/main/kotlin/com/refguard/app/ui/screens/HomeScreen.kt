package com.refguard.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.refguard.app.domain.MismatchStatus
import com.refguard.app.domain.RiskLevel
import com.refguard.app.ui.theme.*
import com.refguard.app.viewmodel.ScanUiState
import com.refguard.app.viewmodel.ScanViewModel
import com.refguard.platform.models.ContentType
import com.refguard.platform.models.ScanRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ScanViewModel,
    onNavigateToQrScan: () -> Unit
) {
    val context = LocalContext.current
    val scanState by viewModel.scanState.collectAsState()
    val reportState by viewModel.reportState.collectAsState()
    val queueSize by viewModel.offlineQueueSize.collectAsState()

    var manualInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    when (val state = scanState) {
        is ScanUiState.Success -> {
            ResultScreen(
                result = state.result,
                viewModel = viewModel,
                reportState = reportState,
                onScanAnother = { viewModel.resetScanState() }
            )
        }
        else -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = "RefGuard Shield",
                                    tint = ColorBrand,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("RefGuard", fontWeight = FontWeight.Bold)
                            }
                        },
                        actions = {
                            if (queueSize > 0) {
                                Surface(
                                    color = ColorBrandSurface,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    Text(
                                        "⚡ $queueSize Queued",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ColorBrand,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(Modifier.height(12.dp))

                    // ── Hero Banner ──
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ColorBrandSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Payment Intent & Threat Guard",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorBrand
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Real-time edge analysis to detect payment inversions, deceptive QR codes, electricity bill scams, and credential harvesting.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Primary Actions ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onNavigateToQrScan,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorBrand)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR")
                            Spacer(Modifier.width(8.dp))
                            Text("Scan QR")
                        }

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val text = clip.getItemAt(0).text?.toString() ?: ""
                                    if (text.isNotBlank()) {
                                        manualInput = text
                                        viewModel.submitScan(
                                            ScanRequest(
                                                contentType = ContentType.CLIPBOARD,
                                                contentValue = text,
                                                sourceContext = "CLIPBOARD_INGRESS"
                                            )
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                            Spacer(Modifier.width(8.dp))
                            Text("Paste Clip")
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Manual Input Analysis ──
                    Text(
                        "Analyze UPI URI, SMS, or Message",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = { manualInput = it },
                        placeholder = { Text("Paste upi://pay URI, bill reminder, or cashback message...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (manualInput.isNotBlank()) {
                                viewModel.submitScan(
                                    ScanRequest(
                                        contentType = ContentType.TEXT,
                                        contentValue = manualInput.trim(),
                                        sourceContext = "MANUAL_INPUT"
                                    )
                                )
                            }
                        },
                        enabled = manualInput.isNotBlank() && scanState !is ScanUiState.Scanning,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (scanState is ScanUiState.Scanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Search, contentDescription = "Analyze")
                            Spacer(Modifier.width(8.dp))
                            Text("Analyze Content")
                        }
                    }

                    if (scanState is ScanUiState.Error) {
                        Spacer(Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ColorCriticalContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, contentDescription = "Error", tint = ColorCritical)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    (scanState as ScanUiState.Error).message,
                                    color = ColorCritical,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    // ── Emergency Cyber Assistance ──
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = "Helpline", tint = ColorHigh)
                                Spacer(Modifier.width(8.dp))
                                Text("National Cyber Fraud Helpline", fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "If you suspect financial loss or entered a PIN on a fraudulent request, dial 1930 immediately or file a report on cybercrime.gov.in within the golden hour.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
