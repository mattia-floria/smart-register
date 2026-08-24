package com.afloria.smartregister.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afloria.smartregister.ui.components.ExpressiveCard
import com.afloria.smartregister.ui.components.ModernBackground
import com.afloria.smartregister.ui.theme.ExpressiveShapes
import com.afloria.smartregister.ui.theme.ThemeMode

@Composable
fun LandingScreen(
    viewModel: MainViewModel
) {
    val appState by viewModel.appState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        ModernBackground(seedColor = viewModel.selectedSeedColor ?: MaterialTheme.colorScheme.primary)

        AnimatedContent(
            targetState = appState,
            transitionSpec = {
                (fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.95f))
                    .togetherWith(fadeOut(animationSpec = tween(400)))
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
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = ExpressiveShapes.ExtraLargeSquircle,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 6.dp
        ) {
            AppLogo(modifier = Modifier.fillMaxSize().padding(24.dp))
        }

        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "Smart Register",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Il registro che stavi aspettando.\nModerno, intuitivo, veloce.",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )

        Spacer(modifier = Modifier.height(64.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = ExpressiveShapes.Pill,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            )
        ) {
            Text("Inizia l'esperienza", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ThemeSelectionContent(
    viewModel: MainViewModel, 
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(80.dp))
        
        Text(
            text = "Personalizza",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        
        Text(
            text = "Scegli il look che preferisci.",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        ExpressiveCard(
            shape = ExpressiveShapes.ExtraLargeSquircle,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
        ) {
            Text("Modalità", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThemeModeItem(ThemeMode.SYSTEM, viewModel.themeMode == ThemeMode.SYSTEM) { viewModel.updateTheme(ThemeMode.SYSTEM) }
                ThemeModeItem(ThemeMode.LIGHT, viewModel.themeMode == ThemeMode.LIGHT) { viewModel.updateTheme(ThemeMode.LIGHT) }
                ThemeModeItem(ThemeMode.DARK, viewModel.themeMode == ThemeMode.DARK) { viewModel.updateTheme(ThemeMode.DARK) }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = ExpressiveShapes.Pill,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Configurazione completata", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun ThemeModeItem(mode: ThemeMode, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(height = 56.dp, width = 80.dp),
        shape = ExpressiveShapes.Squircle,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(mode.name.take(1) + mode.name.drop(1).lowercase(), style = MaterialTheme.typography.labelMedium)
        }
    }
}
