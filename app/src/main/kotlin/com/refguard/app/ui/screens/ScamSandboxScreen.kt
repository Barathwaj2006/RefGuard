package com.refguard.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refguard.app.ui.theme.*
import com.refguard.app.viewmodel.ScanViewModel
import com.refguard.platform.models.ContentType
import com.refguard.platform.models.IngressResult
import com.refguard.platform.models.ScanRequest
import java.time.Instant

data class ThreatScenario(
    val id: String,
    val title: String,
    val category: String,
    val dangerLevel: String,
    val icon: ImageVector,
    val payload: String,
    val contentType: ContentType,
    val description: String,
    val scamMechanism: String
)

val PRESET_SCENARIOS = listOf(
    ThreatScenario(
        id = "electricity_scam",
        title = "Electricity Disconnection Phishing",
        category = "Utility Bill Fraud",
        dangerLevel = "CRITICAL",
        icon = Icons.Default.Bolt,
        payload = "Dear Customer, Your Electricity power will be disconnected tonight at 9:30 PM because your previous month bill was not updated. Please immediately update and pay via upi://pay?pa=powerbill.discom@axisbank&pn=ElectricityDiscom&am=1450&cu=INR or call officer on 9876543210 to prevent disconnection.",
        contentType = ContentType.TEXT,
        description = "Scammer uses artificial panic & urgency (power cutoff) to make victim pay an unauthorized VPA without verifying.",
        scamMechanism = "Urgency trigger + Fake utility VPA + Malicious debit authorization."
    ),
    ThreatScenario(
        id = "cashback_inversion",
        title = "Cashback QR Inversion Trap",
        category = "Debit Collect Trap",
        dangerLevel = "CRITICAL",
        icon = Icons.Default.CardGiftcard,
        payload = "upi://pay?pa=rewards.claim.hub@paytm&pn=RewardsHub&am=5000&cu=INR&tn=ClaimCashback5000",
        contentType = ContentType.QR,
        description = "Lures victim with a ₹5,000 reward QR code, but the QR actually issues an outbound DEBIT collect from victim's account.",
        scamMechanism = "Intent mismatch (Stated reward vs. Actual bank debit)."
    ),
    ThreatScenario(
        id = "telegram_job",
        title = "Telegram Part-Time Task Scam",
        category = "Job / Crypto Trap",
        dangerLevel = "HIGH",
        icon = Icons.Default.WorkOutline,
        payload = "Earn ₹3,000 - ₹8,000 daily from home! Simply like YouTube videos and submit review screenshots. Join official Telegram channel: https://t.me/fake_job_rewards and deposit ₹500 security deposit to upi://pay?pa=taskverify@okaxis&am=500 to activate daily earnings.",
        contentType = ContentType.TEXT,
        description = "Classic task fraud where victims are persuaded to pay small 'activation deposits' that escalate into large financial losses.",
        scamMechanism = "Unrealistic income lure + Ponzi task deposit + Fake Telegram gateway."
    ),
    ThreatScenario(
        id = "india_post_kyc",
        title = "India Post / Courier Address Update",
        category = "Phishing SMS",
        dangerLevel = "HIGH",
        icon = Icons.Default.LocalShipping,
        payload = "India Post Alert: Your parcel #IN88923019 cannot be delivered due to incomplete house address. Update address within 12 hours to avoid return: https://indiapost-parcel-kyc.com/track and pay ₹5 re-delivery charge via upi://pay?pa=postcharge@ibl&am=5",
        contentType = ContentType.TEXT,
        description = "Impersonates postal services demanding nominal re-delivery fees (₹5) to harvest banking credentials & OTPs.",
        scamMechanism = "Fake delivery alert + Typosquatted domain + Micro-debit credential trap."
    ),
    ThreatScenario(
        id = "customer_care_qr",
        title = "Fake Bank / App Support QR",
        category = "Tech Support Fraud",
        dangerLevel = "CRITICAL",
        icon = Icons.Default.HeadsetMic,
        payload = "upi://pay?pa=support.refund.desk@yesbank&pn=GPayRefundHelpdesk&am=9999&cu=INR&tn=FailedTxnRefund",
        contentType = ContentType.QR,
        description = "Fraudulent customer care number directs customer to scan a 'Refund QR' which debits ₹9,999 instead.",
        scamMechanism = "Impersonation of banking support + Remote collect debit."
    ),
    ThreatScenario(
        id = "safe_swiggy",
        title = "Verified Merchant Payment (Safe)",
        category = "Legitimate Transaction",
        dangerLevel = "SAFE",
        icon = Icons.Default.Storefront,
        payload = "upi://pay?pa=swiggy@icici&pn=BundlTechnologies&am=420&cu=INR&tn=FoodOrder_92810",
        contentType = ContentType.QR,
        description = "Benchmark test: A genuine, verified food delivery merchant transaction with valid registered VPA parameters.",
        scamMechanism = "Valid merchant identifier, standard consumer payment."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScamSandboxScreen(
    viewModel: ScanViewModel,
    onNavigateBack: () -> Unit,
    onLaunchScenario: (ThreatScenario) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Threat Simulator Lab",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorBrand
                        )
                        Text(
                            "Interactive Judge & Security Sandbox",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ColorBrand)
                    }
                },
                actions = {
                    Surface(
                        color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Science, null, tint = Color(0xFF0D1B48), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Live Engine",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0D1B48)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))

                // Hero Info Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = ColorBrand),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.BugReport, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Cyber Threat Laboratory",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Select any real-world scam vector below to run an instant forensic analysis through RefGuard's ScamChain and heuristic detection pipeline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            item {
                Text(
                    "Select Attack Scenario to Execute",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(PRESET_SCENARIOS, key = { it.id }) { scenario ->
                ScenarioCard(
                    scenario = scenario,
                    onExecute = {
                        val request = ScanRequest(
                            contentType = scenario.contentType,
                            contentValue = scenario.payload,
                            sourceContext = "com.refguard.app.sandbox.${scenario.id}",
                            timestamp = Instant.now().toString()
                        )
                        viewModel.handleIngressResult(IngressResult.Success(request))
                        onLaunchScenario(scenario)
                    }
                )
            }

            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ScenarioCard(
    scenario: ThreatScenario,
    onExecute: () -> Unit
) {
    val isCritical = scenario.dangerLevel == "CRITICAL"
    val isWarning = scenario.dangerLevel == "HIGH"
    val isSafe = scenario.dangerLevel == "SAFE"

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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onExecute() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(badgeContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(scenario.icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            scenario.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            scenario.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = badgeContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        scenario.dangerLevel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                scenario.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onExecute,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSafe) ColorSafe else ColorBrand
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("RUN LIVE SIMULATION", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
