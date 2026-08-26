package com.refguard.app.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refguard.app.ui.theme.*

data class CyberThreatAdvisory(
    val id: String,
    val title: String,
    val authority: String,
    val date: String,
    val summary: String,
    val preventionTip: String,
    val severity: String
)

val ADVISORIES = listOf(
    CyberThreatAdvisory(
        id = "adv_1",
        title = "Malicious APK Sideloading via WhatsApp (.apk files)",
        authority = "CERT-In / I4C Alert",
        date = "Active Wave 2026",
        summary = "Scammers send WhatsApp messages disguised as Wedding Invitations or Electricity Department apps ('BijliBill.apk'). Once installed, they read SMS OTPs and perform unauthorized UPI transfers.",
        preventionTip = "Never install APK files received over WhatsApp, Telegram, or SMS. Always install only from Google Play Store.",
        severity = "CRITICAL"
    ),
    CyberThreatAdvisory(
        id = "adv_2",
        title = "UPI 'Receive Money' PIN Scam (Inversion Fraud)",
        authority = "NPCI Advisory",
        date = "Continuous Vector",
        summary = "Scammers send a payment link or QR code claiming victim has won ₹5,000 lottery or cashback. They instruct victim to 'enter UPI PIN to accept reward in bank'. Entering PIN instantly debits the money.",
        preventionTip = "UPI PIN is NEVER needed to receive money. Entering PIN ALWAYS transfers money out of your account.",
        severity = "CRITICAL"
    ),
    CyberThreatAdvisory(
        id = "adv_3",
        title = "Fake Digital Arrest / Courier Customs Extortion",
        authority = "Ministry of Home Affairs",
        date = "High Alert",
        summary = "Impostors pose as Police, CBI, or Customs officers over video call claiming an illegal parcel was registered under victim's Aadhaar. They demand urgent RTGS/UPI transfer to 'verify funds'.",
        preventionTip = "Indian police & courts NEVER conduct digital arrests or demand money transfer over Skype/WhatsApp calls.",
        severity = "HIGH"
    ),
    CyberThreatAdvisory(
        id = "adv_4",
        title = "Search Engine Customer Care Number Spoofing",
        authority = "RBI Cyber Alert",
        date = "Ongoing",
        summary = "Fraudsters create fake Google Search listings for airline, bank, or courier support. Calling these numbers connects to scammers who request remote screen sharing apps (AnyDesk, TeamViewer).",
        preventionTip = "Never dial helpline numbers from random search results. Obtain verified contact info exclusively from official bank apps.",
        severity = "HIGH"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreatRadarScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Threat Radar & Intel", fontWeight = FontWeight.Bold, color = ColorBrand)
                        Text("Indian Cybercrime Advisory Feed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))

                // Official Portal Quick Dial Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = ColorBrand),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Emergency Cyber Helpline",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Dial 1930 within the 'Golden Hour' (first 2 hours) of financial fraud to freeze funds in transit.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930"))
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ColorCritical),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Phone, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("DIAL 1930", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://cybercrime.gov.in"))
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("GOV PORTAL", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Trending Threat Modus Operandi (India)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(ADVISORIES, key = { it.id }) { advisory ->
                AdvisoryCard(advisory = advisory)
            }

            item {
                // Interactive Safety Red Flags Checklist
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "The 5 Universal Scam Red Flags",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ColorBrand
                        )
                        Spacer(Modifier.height(10.dp))

                        CheckFlagRow(title = "1. Artificial Urgency", desc = "'Pay in 10 mins or power cutoff / SIM block / parcel return'")
                        CheckFlagRow(title = "2. PIN for Credit", desc = "Asking you to enter PIN to claim rewards, refund or lottery")
                        CheckFlagRow(title = "3. Sideload APKs", desc = "Sending .apk files over WhatsApp to 'update bank KYC'")
                        CheckFlagRow(title = "4. Remote Screen Sharing", desc = "Asking you to install AnyDesk/RustDesk for 'tech support'")
                        CheckFlagRow(title = "5. Unknown Beneficiary Name", desc = "Bank confirmation screen shows individual name instead of company")
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AdvisoryCard(advisory: CyberThreatAdvisory) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (advisory.severity == "CRITICAL") ColorCriticalContainer else ColorWarningContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        advisory.authority,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (advisory.severity == "CRITICAL") ColorCritical else ColorWarning
                    )
                }

                Text(
                    advisory.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                advisory.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(6.dp))

            Text(
                advisory.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Surface(
                        color = ColorSafeContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, null, tint = ColorSafe, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                advisory.preventionTip,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorSafe
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckFlagRow(title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Default.WarningAmber, null, tint = ColorWarning, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
