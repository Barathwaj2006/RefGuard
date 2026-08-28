package com.refguard.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.refguard.app.ui.screens.HomeScreen
import com.refguard.app.ui.screens.QRScannerScreen
import com.refguard.app.ui.theme.RefGuardTheme
import com.refguard.app.viewmodel.ScanViewModel
import com.refguard.platform.models.ContentType
import com.refguard.platform.models.ScanRequest

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: ScanViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as RefGuardApplication
        viewModel = ScanViewModel(
            apiService = app.apiService,
            offlineQueue = app.offlineQueue
        )

        handleIntent(intent)

        setContent {
            RefGuardTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf("home") }

                    if (currentScreen == "qr_scan") {
                        QRScannerScreen(
                            onQRScanned = { ingressResult ->
                                viewModel.handleIngressResult(ingressResult)
                                currentScreen = "home"
                            },
                            onBack = {
                                currentScreen = "home"
                            }
                        )
                    } else {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToQrScan = { currentScreen = "qr_scan" }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrBlank()) {
                        viewModel.submitScan(
                            ScanRequest(
                                contentType = ContentType.TEXT,
                                contentValue = sharedText,
                                sourceContext = "SHARESHEET_TEXT"
                            )
                        )
                    }
                }
            }
            Intent.ACTION_VIEW -> {
                val data: Uri? = intent.data
                if (data != null) {
                    viewModel.submitScan(
                        ScanRequest(
                            contentType = ContentType.UPI_URI,
                            contentValue = data.toString(),
                            sourceContext = "INTENT_VIEW_URI"
                        )
                    )
                }
            }
        }
    }
}
