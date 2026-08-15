package com.refguard.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.refguard.app.ui.theme.ColorBrand

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            CircularProgressIndicator(
                color = ColorBrand,
                strokeWidth = 4.dp,
                modifier = Modifier.size(56.dp)
            )
            Text(
                "Analyzing for scams...",
                style = MaterialTheme.typography.titleMedium,
                color = ColorBrand,
                textAlign = TextAlign.Center
            )
            Text(
                "Checking threat intelligence, payment patterns,\nand scam databases.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
