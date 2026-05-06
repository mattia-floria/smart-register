package com.afloria.smartregister.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afloria.smartregister.ui.theme.SmartRegisterTheme

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(512.dp) // Large size for preview
            .clip(RoundedCornerShape(120.dp)) // Squircle shape
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Book background
        Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.6f),
            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
        )
        
        // AI Sparkle overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, end = 80.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(110.dp),
                tint = Color.White
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 512, heightDp = 512)
@Composable
fun AppLogoPreview() {
    SmartRegisterTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AppLogo()
        }
    }
}
