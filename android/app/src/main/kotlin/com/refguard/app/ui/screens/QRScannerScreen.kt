package com.refguard.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.refguard.app.ui.theme.ColorBrand
import com.refguard.platform.models.ContentType
import com.refguard.platform.models.IngressResult
import com.refguard.platform.models.ScanRequest
import java.time.Instant
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    onQRScanned: (IngressResult) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    var scanConsumed by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan QR Code") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorBrand, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                // Camera Preview
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { previewView ->
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val analyzer = ImageAnalysis.Builder()
                                    .setTargetResolution(Size(1280, 720))
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                val scanner = BarcodeScanning.getClient()
                                analyzer.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null && !scanConsumed) {
                                        val image = InputImage.fromMediaImage(
                                            mediaImage, imageProxy.imageInfo.rotationDegrees
                                        )
                                        scanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                for (barcode in barcodes) {
                                                    val raw = barcode.rawValue
                                                    if (!raw.isNullOrBlank() && !scanConsumed) {
                                                        scanConsumed = true
                                                        val request = ScanRequest(
                                                            contentType = if (barcode.valueType == Barcode.TYPE_URL) ContentType.URL else ContentType.QR,
                                                            contentValue = raw,
                                                            sourceContext = "com.android.camera",
                                                            timestamp = Instant.now().toString()
                                                        )
                                                        onQRScanned(IngressResult.Success(request))
                                                    }
                                                }
                                            }
                                            .addOnCompleteListener { imageProxy.close() }
                                    } else {
                                        imageProxy.close()
                                    }
                                }

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analyzer
                                    )
                                } catch (e: Exception) {
                                    onQRScanned(IngressResult.Failure(com.refguard.platform.models.IngressError.HardwareUnavailable))
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Viewfinder overlay
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .border(3.dp, Color.White, RoundedCornerShape(16.dp))
                )
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.QrCodeScanner, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Point at a QR code or payment barcode",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Permission denied
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text("Camera permission required to scan QR codes.", textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Permission")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onBack) { Text("Go Back") }
                }
            }
        }
    }
}
