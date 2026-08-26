package com.refguard.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
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
import com.refguard.app.ui.theme.*
import com.refguard.app.viewmodel.ScanViewModel
import com.refguard.platform.models.ContentType
import com.refguard.platform.models.IngressResult
import com.refguard.platform.models.ScanRequest
import kotlinx.coroutines.delay
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzeScreen(
    viewModel: ScanViewModel,
    isAnalyzing: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToImagePicker: () -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var isStrictUpi by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Meaningful progress step sequence during analysis
    var progressStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(isAnalyzing) {
        if (isAnalyzing) {
            progressStep = 1 // Reading content
            delay(350)
            progressStep = 2 // Checking payment intent
            delay(400)
            progressStep = 3 // Checking threat signals
            delay(400)
            progressStep = 4 // Building investigation
        } else {
            progressStep = 0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analyze Content", fontWeight = FontWeight.Bold, color = ColorBrand) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Home", tint = ColorBrand)
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
            // ── Instructions & Helper ────────────────────────
            Text(
                "Provide the suspicious message, payment request link, or UPI ID you received before authorizing payment.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── MAIN INPUT CARD ──────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Content to Inspect",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ColorBrand
                        )
                        if (inputText.isNotEmpty()) {
                            TextButton(
                                onClick = { inputText = "" },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Clear", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                "Paste message text (e.g., \"Dear customer, electricity bill pending, pay here: upi://pay?pa=fake@upi&am=500&pn=PowerGov\")...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(10.dp))

                    // Ingress Shortcuts inside input card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                if (!clipText.isNullOrBlank()) {
                                    inputText = clipText.trim()
                                }
                            },
                            label = { Text("Paste Clipboard", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Default.ContentPaste, null, modifier = Modifier.size(16.dp)) }
                        )

                        AssistChip(
                            onClick = { onNavigateToScan() },
                            label = { Text("Scan QR", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(16.dp)) }
                        )

                        AssistChip(
                            onClick = { onNavigateToImagePicker() },
                            label = { Text("Screenshot", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Default.Image, null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isStrictUpi = !isStrictUpi }
                    ) {
                        Checkbox(
                            checked = isStrictUpi,
                            onCheckedChange = { isStrictUpi = it }
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Treat input strictly as direct UPI VPA handle",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // ── SAMPLE PRESETS (FOR RAPID TESTING & DEMOS) ───
            Text(
                "Select Test Attack Vector",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = false,
                    onClick = {
                        inputText = "Dear Customer, Your Electricity power will be disconnected tonight at 9:30 PM because your previous month bill was not updated. Please immediately update and pay via upi://pay?pa=powerbill.discom@axisbank&pn=ElectricityDiscom&am=1450&cu=INR or call officer on 9876543210."
                        isStrictUpi = false
                    },
                    label = { Text("⚡ Power Bill Cutoff", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = false,
                    onClick = {
                        inputText = "Congratulations! You won ₹5000 cashback. Click here to claim your reward in bank: upi://pay?pa=rewards.claim.hub@paytm&am=5000&pn=RewardsHub&tn=ClaimReward5000"
                        isStrictUpi = false
                    },
                    label = { Text("🎁 ₹5,000 Reward QR", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = false,
                    onClick = {
                        inputText = "Earn ₹5,000 daily from home! Like YouTube videos. Join Telegram: https://t.me/earn_rewards and deposit ₹500 security to upi://pay?pa=taskverify@okaxis&am=500"
                        isStrictUpi = false
                    },
                    label = { Text("💼 Telegram Job Trap", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = false,
                    onClick = {
                        inputText = "India Post Alert: Your parcel #IN88923 cannot be delivered due to address error. Update address & pay ₹5 fee: upi://pay?pa=postcharge@ibl&am=5"
                        isStrictUpi = false
                    },
                    label = { Text("📦 Postal KYC Phishing", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = false,
                    onClick = {
                        inputText = "upi://pay?pa=swiggy@icici&pn=Swiggy&am=340&cu=INR&tn=FoodOrder"
                        isStrictUpi = false
                    },
                    label = { Text("✅ Safe Merchant UPI", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = false,
                    onClick = {
                        inputText = "fraudulent-collect@ybl"
                        isStrictUpi = true
                    },
                    label = { Text("⚠️ Malicious VPA Handle", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── PRIMARY CTA ──────────────────────────────────
            Button(
                onClick = {
                    if (inputText.isNotBlank() && !isAnalyzing) {
                        val trimmed = inputText.trim()
                        val contentType = when {
                            isStrictUpi -> ContentType.UPI_VPA
                            trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true) -> ContentType.URL
                            trimmed.startsWith("upi://pay", true) -> ContentType.TEXT
                            trimmed.contains("@") && !trimmed.contains(" ") -> ContentType.UPI_VPA
                            else -> ContentType.TEXT
                        }
                        val request = ScanRequest(
                            contentType = contentType,
                            contentValue = trimmed,
                            sourceContext = "com.refguard.app.analyze",
                            timestamp = Instant.now().toString()
                        )
                        viewModel.handleIngressResult(IngressResult.Success(request))
                    }
                },
                enabled = inputText.isNotBlank() && !isAnalyzing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorBrand)
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Investigating...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("CHECK FOR SCAM", fontWeight = FontWeight.Bold)
                }
            }

            // ── SUBTLE PROGRESS SEQUENCE (WHEN ANALYZING) ────
            AnimatedVisibility(
                visible = isAnalyzing,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ColorBrandSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Investigation Pipeline",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorBrand
                        )

                        AnalysisProgressRow(
                            stepNumber = 1,
                            title = "Reading content & extracting parameters",
                            active = progressStep >= 1,
                            completed = progressStep > 1
                        )
                        AnalysisProgressRow(
                            stepNumber = 2,
                            title = "Checking payment intent & direction",
                            active = progressStep >= 2,
                            completed = progressStep > 2
                        )
                        AnalysisProgressRow(
                            stepNumber = 3,
                            title = "Checking threat signals & blacklist database",
                            active = progressStep >= 3,
                            completed = progressStep > 3
                        )
                        AnalysisProgressRow(
                            stepNumber = 4,
                            title = "Building investigation & evidence pack",
                            active = progressStep >= 4,
                            completed = progressStep >= 4
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AnalysisProgressRow(
    stepNumber: Int,
    title: String,
    active: Boolean,
    completed: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    when {
                        completed -> ColorSafe
                        active -> ColorBrand
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (completed) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
            } else {
                Text(
                    "$stepNumber",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (active) Color.White else MaterialTheme.colorScheme.outline
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
