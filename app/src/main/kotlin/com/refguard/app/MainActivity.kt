package com.refguard.app

import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.refguard.app.api.ApiClient
import com.refguard.app.domain.ScanResult
import com.refguard.app.history.InvestigationHistoryManager
import com.refguard.app.queue.OfflineScanQueue
import com.refguard.app.ui.screens.*
import com.refguard.app.ui.theme.RefGuardTheme
import com.refguard.app.viewmodel.ScanUiState
import com.refguard.app.viewmodel.ScanViewModel
import com.refguard.platform.models.ContentType
import com.refguard.platform.models.IngressError
import com.refguard.platform.models.IngressResult
import com.refguard.platform.models.ScanRequest
import java.io.FileNotFoundException
import java.time.Instant

/**
 * Navigation destinations for RefGuard product hierarchy:
 * HOME → ANALYZE → RESULT → INVESTIGATION → ACTION
 */
enum class AppScreen {
    HOME,
    ANALYZE,
    RESULT,
    INVESTIGATION,
    ACTION,
    QR_SCANNER
}

/**
 * Single-Activity host and Android OS Sharesheet Target.
 *
 * Ingress paths:
 *  1. Android Sharesheet (ACTION_SEND / ACTION_SEND_MULTIPLE):
 *     - WhatsApp / SMS text chats, viral links
 *     - Payment QR screenshots, gallery receipts
 *     - Multiple shared images (with automatic QR detection)
 *  2. Direct Launch:
 *     - QR Camera scanner (Torch toggle, Live viewfinder)
 *     - Explicit Clipboard Scan
 *     - Manual input & Threat Lab scenarios
 */
class MainActivity : ComponentActivity() {

    private lateinit var viewModel: ScanViewModel
    private lateinit var offlineQueue: OfflineScanQueue
    private lateinit var historyManager: InvestigationHistoryManager

    private val _currentScreen = mutableStateOf(AppScreen.HOME)
    private var _previousScreenBeforeScanner = AppScreen.HOME
    private var _activeResult = mutableStateOf<ScanResult?>(null)

    // User-initiated gallery picker
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handleImageUri(uri, "com.android.gallery.picker")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        offlineQueue = OfflineScanQueue(this)
        historyManager = InvestigationHistoryManager(this)

        viewModel = ViewModelProvider(
            this,
            ScanViewModel.Factory(
                apiService = ApiClient.service,
                offlineQueue = offlineQueue,
                isNetworkAvailable = { isNetworkAvailable() },
                historyManager = historyManager
            )
        )[ScanViewModel::class.java]

        // Setup automatic background sync on network recovery
        setupNetworkCallback()

        // Handle initial share intent immediately
        handleIntent(intent)

        setContent {
            RefGuardTheme {
                val scanState by viewModel.scanState.collectAsStateWithLifecycle()
                val offlineQueueSize by viewModel.offlineQueueSize.collectAsStateWithLifecycle()
                val currentScreen by _currentScreen
                val activeResult by _activeResult

                // Synchronize ScanUiState.Success with current screen navigation
                LaunchedEffect(scanState) {
                    when (val state = scanState) {
                        is ScanUiState.Success -> {
                            _activeResult.value = state.result
                            if (_currentScreen.value != AppScreen.INVESTIGATION && _currentScreen.value != AppScreen.ACTION) {
                                _currentScreen.value = AppScreen.RESULT
                            }
                        }
                        is ScanUiState.Loading -> {
                            if (_currentScreen.value != AppScreen.ANALYZE) {
                                _currentScreen.value = AppScreen.ANALYZE
                            }
                        }
                        else -> {}
                    }
                }

                // ── BACK NAVIGATION CONTROLLER ───────────────
                // Enforces: ACTION → INVESTIGATION → RESULT → ANALYZE → HOME
                BackHandler(enabled = currentScreen != AppScreen.HOME) {
                    when (currentScreen) {
                        AppScreen.ACTION -> _currentScreen.value = AppScreen.INVESTIGATION
                        AppScreen.INVESTIGATION -> _currentScreen.value = AppScreen.RESULT
                        AppScreen.RESULT -> {
                            viewModel.resetScanState()
                            _currentScreen.value = AppScreen.ANALYZE
                        }
                        AppScreen.ANALYZE -> {
                            viewModel.resetScanState()
                            _currentScreen.value = AppScreen.HOME
                        }
                        AppScreen.QR_SCANNER -> _currentScreen.value = _previousScreenBeforeScanner
                        AppScreen.HOME -> finish()
                    }
                }

                // Smooth transitions between product screens
                AnimatedContent(
                    targetState = Triple(currentScreen, scanState, offlineQueueSize),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ScreenTransition"
                ) { (screen, state, queueSize) ->

                    when {
                        screen == AppScreen.QR_SCANNER -> {
                            QRScannerScreen(
                                onQRScanned = { ingressResult ->
                                    _currentScreen.value = AppScreen.ANALYZE
                                    viewModel.handleIngressResult(ingressResult)
                                },
                                onBack = { _currentScreen.value = _previousScreenBeforeScanner }
                            )
                        }

                        screen == AppScreen.ACTION && activeResult != null -> {
                            ActionScreen(
                                result = activeResult!!,
                                viewModel = viewModel,
                                onNavigateBack = { _currentScreen.value = AppScreen.INVESTIGATION },
                                onReturnHome = {
                                    viewModel.resetScanState()
                                    _currentScreen.value = AppScreen.HOME
                                }
                            )
                        }

                        screen == AppScreen.INVESTIGATION && activeResult != null -> {
                            InvestigationScreen(
                                result = activeResult!!,
                                onNavigateBack = { _currentScreen.value = AppScreen.RESULT },
                                onNavigateToAction = { _currentScreen.value = AppScreen.ACTION }
                            )
                        }

                        screen == AppScreen.RESULT && activeResult != null -> {
                            ResultScreen(
                                result = activeResult!!,
                                viewModel = viewModel,
                                onNavigateBack = {
                                    viewModel.resetScanState()
                                    _currentScreen.value = AppScreen.ANALYZE
                                },
                                onNavigateToInvestigation = { _currentScreen.value = AppScreen.INVESTIGATION },
                                onNavigateToAction = { _currentScreen.value = AppScreen.ACTION },
                                onCheckAnother = {
                                    viewModel.resetScanState()
                                    _currentScreen.value = AppScreen.ANALYZE
                                }
                            )
                        }

                        state is ScanUiState.Queued -> {
                            QueuedScreen(
                                queueSize = queueSize,
                                onGoHome = {
                                    viewModel.resetScanState()
                                    _currentScreen.value = AppScreen.HOME
                                }
                            )
                        }

                        state is ScanUiState.Error -> {
                            ErrorScreen(
                                error = state,
                                onRetry = { pending ->
                                    if (pending != null) viewModel.retry(pending)
                                    else viewModel.resetScanState()
                                },
                                onGoHome = {
                                    viewModel.resetScanState()
                                    _currentScreen.value = AppScreen.HOME
                                }
                            )
                        }

                        screen == AppScreen.ANALYZE -> {
                            AnalyzeScreen(
                                viewModel = viewModel,
                                isAnalyzing = state is ScanUiState.Loading,
                                onNavigateBack = {
                                    viewModel.resetScanState()
                                    _currentScreen.value = AppScreen.HOME
                                },
                                onNavigateToScan = {
                                    _previousScreenBeforeScanner = AppScreen.ANALYZE
                                    _currentScreen.value = AppScreen.QR_SCANNER
                                },
                                onNavigateToImagePicker = { imagePickerLauncher.launch("image/*") }
                            )
                        }

                        else -> {
                            HomeScreen(
                                viewModel = viewModel,
                                offlineQueueSize = queueSize,
                                onNavigateToAnalyze = {
                                    viewModel.resetScanState()
                                    _currentScreen.value = AppScreen.ANALYZE
                                },
                                onNavigateToScan = {
                                    _previousScreenBeforeScanner = AppScreen.HOME
                                    _currentScreen.value = AppScreen.QR_SCANNER
                                },
                                onNavigateToImagePicker = { imagePickerLauncher.launch("image/*") },
                                onOpenInvestigation = { scanId ->
                                    viewModel.openHistoryInvestigation(scanId)
                                    val histResult = historyManager.getResult(scanId)
                                    if (histResult != null) {
                                        _activeResult.value = histResult
                                        _currentScreen.value = AppScreen.INVESTIGATION
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Flush offline queue on start if network is available
        if (isNetworkAvailable() && offlineQueue.size() > 0) {
            viewModel.flushOfflineQueue()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun setupNetworkCallback() {
        try {
            val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    runOnUiThread {
                        if (offlineQueue.size() > 0) {
                            viewModel.flushOfflineQueue()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            // Non-critical fallback
        }
    }

    /**
     * Route incoming intents (Launcher, Single Share, Multiple Shares)
     */
    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return

        when (action) {
            Intent.ACTION_SEND -> {
                val mimeType = intent.type ?: ""
                val sourcePkg = intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME) ?: callingPackage ?: "android_sharesheet"

                when {
                    mimeType == "text/plain" || intent.hasExtra(Intent.EXTRA_TEXT) -> {
                        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                        if (!text.isNullOrBlank()) {
                            val request = ScanRequest(
                                contentType = if (text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true)) {
                                    ContentType.URL
                                } else {
                                    ContentType.SHARE_INTENT
                                },
                                contentValue = text.trim(),
                                sourceContext = sourcePkg,
                                timestamp = Instant.now().toString()
                            )
                            _currentScreen.value = AppScreen.ANALYZE
                            viewModel.handleIngressResult(IngressResult.Success(request))
                        } else {
                            viewModel.handleIngressResult(IngressResult.Failure(IngressError.EmptyContent))
                        }
                    }
                    mimeType.startsWith("image/") || intent.hasExtra(Intent.EXTRA_STREAM) -> {
                        @Suppress("DEPRECATION")
                        val uri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                        if (uri != null) {
                            _currentScreen.value = AppScreen.ANALYZE
                            handleImageUri(uri, sourcePkg)
                        } else {
                            viewModel.handleIngressResult(
                                IngressResult.Failure(IngressError.MalformedContent("No image URI attached in share"))
                            )
                        }
                    }
                }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                val sourcePkg = intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME) ?: callingPackage ?: "android_sharesheet_multiple"
                @Suppress("DEPRECATION")
                val uris: java.util.ArrayList<Uri>? = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                val validUris = uris?.filterNotNull() ?: emptyList()

                if (validUris.isNotEmpty()) {
                    _currentScreen.value = AppScreen.ANALYZE
                    handleMultipleImages(validUris, sourcePkg)
                } else {
                    viewModel.handleIngressResult(
                        IngressResult.Failure(IngressError.EmptyContent)
                    )
                }
            }
        }
    }

    /**
     * Process multiple images: Scans images in sequence to detect payment QR codes first;
     * falls back to the primary screenshot for deep text OCR / image inspection.
     */
    private fun handleMultipleImages(uris: List<Uri>, sourcePkg: String) {
        findQrCodeInUris(uris, 0) { qrPayload ->
            if (qrPayload != null) {
                val request = ScanRequest(
                    contentType = if (qrPayload.startsWith("http://") || qrPayload.startsWith("https://")) ContentType.URL else ContentType.QR,
                    contentValue = qrPayload,
                    sourceContext = sourcePkg,
                    timestamp = Instant.now().toString()
                )
                viewModel.handleIngressResult(IngressResult.Success(request))
            } else {
                handleImageUri(uris.first(), sourcePkg)
            }
        }
    }

    private fun findQrCodeInUris(uris: List<Uri>, index: Int, onComplete: (String?) -> Unit) {
        if (index >= uris.size) {
            onComplete(null)
            return
        }
        val uri = uris[index]
        try {
            val image = InputImage.fromFilePath(this, uri)
            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val qrCode = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                    if (qrCode != null) {
                        onComplete(qrCode.rawValue)
                    } else {
                        findQrCodeInUris(uris, index + 1, onComplete)
                    }
                }
                .addOnFailureListener {
                    findQrCodeInUris(uris, index + 1, onComplete)
                }
        } catch (e: Exception) {
            findQrCodeInUris(uris, index + 1, onComplete)
        }
    }

    /**
     * Safely reads and processes an image URI with ML Kit QR extraction and base64 fallback.
     */
    private fun handleImageUri(uri: Uri, sourcePkg: String = "com.android.gallery") {
        try {
            val image = InputImage.fromFilePath(this, uri)
            val scanner = BarcodeScanning.getClient()

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val qrCode = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                    if (qrCode != null) {
                        val raw = qrCode.rawValue!!
                        val request = ScanRequest(
                            contentType = if (raw.startsWith("http://") || raw.startsWith("https://")) ContentType.URL else ContentType.QR,
                            contentValue = raw,
                            sourceContext = sourcePkg,
                            timestamp = Instant.now().toString()
                        )
                        viewModel.handleIngressResult(IngressResult.Success(request))
                    } else {
                        submitBase64Image(uri, sourcePkg)
                    }
                }
                .addOnFailureListener {
                    submitBase64Image(uri, sourcePkg)
                }
        } catch (e: SecurityException) {
            viewModel.handleIngressResult(
                IngressResult.Failure(IngressError.MalformedContent("Permission to read shared image was denied or revoked."))
            )
        } catch (e: FileNotFoundException) {
            viewModel.handleIngressResult(
                IngressResult.Failure(IngressError.MalformedContent("Shared image file was not found."))
            )
        } catch (e: Exception) {
            submitBase64Image(uri, sourcePkg)
        }
    }

    private fun submitBase64Image(uri: Uri, sourcePkg: String) {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: run {
                viewModel.handleIngressResult(
                    IngressResult.Failure(IngressError.MalformedContent("Could not open image stream."))
                )
                return
            }
            val bytes = inputStream.use { it.readBytes() }

            if (bytes.size > 2 * 1024 * 1024) {
                viewModel.handleIngressResult(
                    IngressResult.Failure(IngressError.UnsupportedContent)
                )
                return
            }

            if (bytes.isEmpty()) {
                viewModel.handleIngressResult(
                    IngressResult.Failure(IngressError.EmptyContent)
                )
                return
            }

            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            val request = ScanRequest(
                contentType = ContentType.IMAGE,
                contentValue = base64,
                sourceContext = sourcePkg,
                timestamp = Instant.now().toString()
            )
            viewModel.handleIngressResult(IngressResult.Success(request))
        } catch (e: SecurityException) {
            viewModel.handleIngressResult(
                IngressResult.Failure(IngressError.MalformedContent("Permission to read shared image was denied or revoked."))
            )
        } catch (e: OutOfMemoryError) {
            viewModel.handleIngressResult(
                IngressResult.Failure(IngressError.UnsupportedContent)
            )
        } catch (e: Exception) {
            viewModel.handleIngressResult(
                IngressResult.Failure(
                    IngressError.MalformedContent("Could not read image: " + e.message)
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
