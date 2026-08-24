package com.afloria.smartregister.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    alpha: Float = 0.15f,
    borderAlpha: Float = 0.3f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = alpha + 0.05f),
                        Color.White.copy(alpha = alpha)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = borderAlpha + 0.1f),
                        Color.White.copy(alpha = borderAlpha),
                        Color.White.copy(alpha = borderAlpha - 0.05f)
                    )
                ),
                RoundedCornerShape(cornerRadius)
            )
    ) {
        content()
    }
}

@Composable
fun ModernBackground(
    seedColor: Color = Color(0xFF6750A4),
    isPureBlack: Boolean = false
) {
    val backgroundColor = if (isPureBlack) Color.Black else MaterialTheme.colorScheme.background
    val secondaryColor = MaterialTheme.colorScheme.secondaryContainer
    
    val topGradient = remember(seedColor, isPureBlack) {
        Brush.verticalGradient(
            colors = listOf(
                seedColor.copy(alpha = 0.15f),
                seedColor.copy(alpha = 0.05f),
                Color.Transparent
            )
        )
    }
    
    val bottomGradient = remember(secondaryColor, isPureBlack) {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                if (isPureBlack) Color.Transparent else secondaryColor.copy(alpha = 0.05f)
            ),
            startY = 1000f
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .graphicsLayer { clip = false }
    ) {
        if (!isPureBlack) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(topGradient)
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bottomGradient)
        )
    }
}
