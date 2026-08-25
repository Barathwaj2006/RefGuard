package com.refguard.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.refguard.app.ui.theme.ColorBrand
import com.refguard.app.viewmodel.ScanUiState
import com.refguard.platform.models.ScanRequest

@Composable
fun ErrorScreen(
    error: ScanUiState.Error,
    onRetry: (ScanRequest?) -> Unit,
    onGoHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            if (error.isNetwork) Icons.Default.CloudOff else Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text(
            if (error.isNetwork) "No Connection" else "Something Went Wrong",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(8.dp))
        Text(
            error.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        if (!error.isMalformed && error.pendingRequest != null) {
            Button(
                onClick = { onRetry(error.pendingRequest) },
                colors = ButtonDefaults.buttonColors(containerColor = ColorBrand)
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Retry")
            }
            Spacer(Modifier.height(8.dp))
        }

        OutlinedButton(onClick = onGoHome) {
            Icon(Icons.Default.Home, null)
            Spacer(Modifier.width(8.dp))
            Text("Go Home")
        }
    }
}

@Composable
fun QueuedScreen(
    queueSize: Int,
    onGoHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CloudQueue,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Saved for Later",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "No network connection. Your scan has been queued ($queueSize queued total) and will be submitted automatically when you reconnect.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onGoHome,
            colors = ButtonDefaults.buttonColors(containerColor = ColorBrand)
        ) {
            Text("Back to Home")
        }
    }
}
