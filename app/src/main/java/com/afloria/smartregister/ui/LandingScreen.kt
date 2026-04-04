package com.afloria.smartregister.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afloria.smartregister.ui.theme.ThemeMode
import kotlin.math.cos
import kotlin.math.sin

data class ExpressivePalette(
    val seed: Color,
    val secondary: Color,
    val tertiary: Color
)

@Composable
fun LandingScreen(
    viewModel: MainViewModel
) {
    val appState by viewModel.appState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF111114))) {
        PixelBackground(state = appState)

        AnimatedContent(
            targetState = appState,
            transitionSpec = {
                fadeIn(animationSpec = tween(700)) togetherWith fadeOut(animationSpec = tween(700))
            },
            label = "LandingContent"
        ) { state ->
            when (state) {
                is AppState.Landing -> WelcomeContent { viewModel.nextFromLanding() }
                is AppState.ThemeSelection -> ThemeSelectionContent(
                    viewModel = viewModel,
                    onContinue = { viewModel.nextFromTheme() }
                )
                else -> Unit
            }
        }
    }
}

@Composable
fun WelcomeContent(onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "Benvenuto in\nSmart Register",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Normal,
                color = Color.White,
                lineHeight = 52.sp
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Il registro elettronico come non l'hai mai visto.",
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Light
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onContinue,
                modifier = Modifier.align(Alignment.CenterEnd).height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD0BCFF), 
                    contentColor = Color(0xFF381E72)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Continua", modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}

@Composable
fun ThemeSelectionContent(
    viewModel: MainViewModel, 
    showTitle: Boolean = true,
    showButton: Boolean = true,
    onContinue: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        if (showTitle) {
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "Tema e Colori",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            ThemePreview(viewModel = viewModel)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (showTitle) Color(0xFF1D1B20) else MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Modalità Tema", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ThemeModeItem(ThemeMode.SYSTEM, viewModel.themeMode == ThemeMode.SYSTEM) { viewModel.updateTheme(ThemeMode.SYSTEM) }
                    ThemeModeItem(ThemeMode.LIGHT, viewModel.themeMode == ThemeMode.LIGHT) { viewModel.updateTheme(ThemeMode.LIGHT) }
                    ThemeModeItem(ThemeMode.DARK, viewModel.themeMode == ThemeMode.DARK) { viewModel.updateTheme(ThemeMode.DARK) }
                    ThemeModeItem(ThemeMode.PURE_BLACK, viewModel.themeMode == ThemeMode.PURE_BLACK) { viewModel.updateTheme(ThemeMode.PURE_BLACK) }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Tavolozza Colori", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PaletteItem(
                        icon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(24.dp)) },
                        isSelected = viewModel.selectedSeedColor == null,
                        onClick = { viewModel.updatePalette(null, null, null) }
                    )

                    val expressivePalettes = listOf(
                        ExpressivePalette(Color(0xFF8C1D18), Color(0xFFF9DEDC), Color(0xFF601410)),
                        ExpressivePalette(Color(0xFF7D5260), Color(0xFFFFD8E4), Color(0xFF31111D)),
                        ExpressivePalette(Color(0xFF6750A4), Color(0xFFEADDFF), Color(0xFF21005D)),
                        ExpressivePalette(Color(0xFF381E72), Color(0xFFD0BCFF), Color(0xFF4F378B)),
                        ExpressivePalette(Color(0xFF004A77), Color(0xFFC2E8FF), Color(0xFF003355))
                    )

                    expressivePalettes.forEach { palette ->
                        PaletteItem(
                            palette = palette,
                            isSelected = viewModel.selectedSeedColor == palette.seed,
                            onClick = { viewModel.updatePalette(palette.seed, palette.secondary, palette.tertiary) }
                        )
                    }
                }
            }
        }

        if (showButton) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Inizia", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ThemeModeItem(mode: ThemeMode, isSelected: Boolean, onClick: () -> Unit) {
    val color = when(mode) {
        ThemeMode.SYSTEM -> Color.Transparent
        ThemeMode.LIGHT -> Color.White
        ThemeMode.DARK -> Color(0xFF313033)
        ThemeMode.PURE_BLACK -> Color.Black
    }
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF49454F), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (mode == ThemeMode.SYSTEM) {
            Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun PaletteItem(
    palette: ExpressivePalette? = null,
    icon: @Composable (() -> Unit)? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (palette != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(color = palette.seed, startAngle = 180f, sweepAngle = 180f, useCenter = true)
                drawArc(color = palette.secondary, startAngle = 90f, sweepAngle = 90f, useCenter = true)
                drawArc(color = palette.tertiary, startAngle = 0f, sweepAngle = 90f, useCenter = true)
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                icon?.invoke()
            }
        }
    }
}

@Composable
fun ThemePreview(viewModel: MainViewModel) {
    Surface(
        modifier = Modifier.size(200.dp, 350.dp),
        shape = RoundedCornerShape(32.dp),
        color = if (viewModel.themeMode == ThemeMode.PURE_BLACK) Color.Black else MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)))
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f).height(120.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)))
                Box(modifier = Modifier.weight(1f).height(120.dp).background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(16.dp)))
            }
        }
    }
}

@Composable
fun PixelBackground(state: AppState) {
    val infiniteTransition = rememberInfiniteTransition(label = "Infinite")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "Time"
    )

    val isThemeSelection = state is AppState.ThemeSelection
    val morphProgress by animateFloatAsState(targetValue = if (isThemeSelection) 1f else 0f, animationSpec = tween(1500), label = "Morph")

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val t = time.toDouble()
        val wave1 = (sin(t * 2.0 * Math.PI).toFloat()) * 40f
        val wave2 = (cos(t * 2.0 * Math.PI).toFloat()) * 30f
        
        val path1 = Path().apply {
            moveTo(w * (0.1f + morphProgress * 0.2f) + wave1, -100f)
            cubicTo(
                w * (0.8f - morphProgress * 0.3f) + wave2, h * (0.2f + morphProgress * 0.1f) + wave1,
                w * (-0.2f + morphProgress * 0.5f) - wave2, h * (0.5f - morphProgress * 0.2f) - wave1,
                w * (1.2f - morphProgress * 0.4f), h * 1.1f
            )
        }
        
        val path2 = Path().apply {
            moveTo(-100f, h * (0.7f - morphProgress * 0.4f) + wave2)
            quadraticTo(
                w * (0.5f + wave1 / 100f), h * (0.4f + morphProgress * 0.3f) + wave2,
                w + 100f, h * (0.9f - morphProgress * 0.2f)
            )
        }

        drawPath(path1, Color(0xFFD0BCFF).copy(alpha = 0.25f), style = Stroke(width = 2f))
        drawPath(path2, Color(0xFFD0BCFF).copy(alpha = 0.15f), style = Stroke(width = 1.5f))
        
        val circleCenter = Offset(
            w * (0.85f - morphProgress * 0.4f) + wave1 * 0.3f,
            h * (0.15f + morphProgress * 0.5f) + wave2 * 0.3f
        )
        
        drawCircle(
            color = Color(0xFFD0BCFF).copy(alpha = 0.1f),
            radius = 220f + morphProgress * 150f + wave1,
            center = circleCenter,
            style = Stroke(width = 1.2f)
        )
        
        drawCircle(
            color = Color(0xFFD0BCFF).copy(alpha = 0.06f),
            radius = 100f + wave2,
            center = Offset(w * (0.2f + morphProgress * 0.6f), h * (0.85f - morphProgress * 0.7f)),
            style = Stroke(width = 1f)
        )
    }
}
