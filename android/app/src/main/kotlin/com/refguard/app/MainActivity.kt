package com.refguard.app

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.refguard.app.api.ApiClient
import com.refguard.app.queue.OfflineScanQueue
import com.refguard.app.ui.screens.*
import com.refguard.app.ui.theme.RefGuardTheme
import com.refguard.app.viewmodel.ScanUiState
import com.refguard.app.viewmodel.ScanViewModel
import com.refguard.platform.models.ContentType
import com.refguard.platform.models.IngressResult
import com.refguard.platform.models.ScanRequest
import java.time.Instant

/**
 * Single-Activity host.
 *
 * Navigation is state-driven (no Fragment Manager, no NavController overhead
 * for this MVP). The ViewModel owns the state; Compose reacts.
 *
 * Ingress paths:
 *  1. Launcher → HomeScreen manual input / paste / QR / image picker
 *  2. Share Sheet → text intent routed here → scan submitted automatically
 */
class MainActivity : ComponentActivity() {

    private lateinit var viewModel: ScanViewModel
    private lateinit var offlineQueue: OfflineScanQueue

    /** Nav destination (simple enum — keeps things straightforward) */
    private enum class Screen { HOME, QR_SCANNER }
    private val _currentScreen = mutableStateOf(Screen.HOME)

    // Image picker launcher (user-initiated)
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handleImageUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        offlineQueue = OfflineScanQueue(this)

        viewModel = ViewModelProvider(
            this,
            ScanViewModel.Factory(
                apiService = ApiClient.service,
                offlineQueue = offlineQueue,
                isNetworkAvailable = { isNetworkAvailable() }
            )
        )[ScanViewModel::class.java]

        // Handle initial share intent
        handleIntent(intent)

        setContent {
            RefGuardTheme {
                val scanState by viewModel.scanState.collectAsStateWithLifecycle()
                val reportState by viewModel.reportState.collectAsStateWithLifecycle()
                val offlineQueueSize by viewModel.offlineQueueSize.collectAsStateWithLifecycle()
                val currentScreen by _currentScreen

                // Animate between states
                AnimatedContent(
                    targetState = Triple(scanState, currentScreen, offlineQueueSize),
                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) { (state, screen, queueSize) ->

                    when {
                        // QR scanner replaces home
                        screen == Screen.QR_SCANNER -> {
                            QRScannerScreen(
                                onQRScanned = { ingressResult ->
                                    _currentScreen.value = Screen.HOME
                                    viewModel.handleIngressResult(ingressResult)
                                },
                                onBack = { _currentScreen.value = Screen.HOME }
                            )
                        }

                        state is ScanUiState.Loading -> LoadingScreen()

                        state is ScanUiState.Success -> ResultScreen(
                            result = state.result,
                            viewModel = viewModel,
                            reportState = reportState,
                            onScanAnother = { viewModel.resetScanState() }
                        )

                        state is ScanUiState.Queued -> QueuedScreen(
                            queueSize = queueSize,
                            onGoHome = { viewModel.resetScanState() }
                        )

                        state is ScanUiState.Error -> ErrorScreen(
                            error = state,
                            onRetry = { pending ->
                                if (pending != null) viewModel.retry(pending)
                                else viewModel.resetScanState()
                            },
                            onGoHome = { viewModel.resetScanState() }
                        )

                        else -> HomeScreen(
                            viewModel = viewModel,
                            offlineQueueSize = queueSize,
                            onNavigateToScan = { _currentScreen.value = Screen.QR_SCANNER },
                            onNavigateToImagePicker = { imagePickerLauncher.launch("image/*") }
                        )
                    }
                }
            }
        }

        // Flush offline queue when app resumes with network
        if (isNetworkAvailable() && offlineQueue.size() > 0) {
            viewModel.flushOfflineQueue()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /**
     * Route incoming intents (launcher or share sheet) to the appropriate ingress path.
     */
    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val mimeType = intent.type ?: return
                when {
                    mimeType == "text/plain" -> {
                        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
                        val pkg = intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME)
                            ?: callingPackage
                        val request = ScanRequest(
                            contentType = if (text.startsWith("http")) ContentType.URL else ContentType.TEXT,
                            contentValue = text,
                            sourceContext = pkg ?: "unknown_share",
                            timestamp = Instant.now().toString()
                        )
                        viewModel.handleIngressResult(IngressResult.Success(request))
                    }
                    mimeType.startsWith("image/") -> {
                        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: return
                        handleImageUri(uri)
                    }
                }
            }
        }
    }

    /**
     * Encode a user-selected image URI as Base64 and submit as a scan.
     * Max 2 MB to prevent out-of-memory on low-end devices.
     */
    private fun handleImageUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val bytes = inputStream.readBytes()
            inputStream.close()

            if (bytes.size > 2 * 1024 * 1024) {
                viewModel.handleIngressResult(
                    IngressResult.Failure(com.refguard.platform.models.IngressError.UnsupportedContent)
                )
                return
            }

            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            val request = ScanRequest(
                contentType = ContentType.IMAGE,
                contentValue = base64,
                sourceContext = "com.android.gallery",
                timestamp = Instant.now().toString()
            )
            viewModel.handleIngressResult(IngressResult.Success(request))
        } catch (e: Exception) {
            viewModel.handleIngressResult(
                IngressResult.Failure(
                    com.refguard.platform.models.IngressError.MalformedContent("Could not read image: ${e.message}")
                )
            )
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
