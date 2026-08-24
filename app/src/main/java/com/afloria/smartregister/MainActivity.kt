package com.afloria.smartregister

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkManager
import com.afloria.smartregister.ai.sync.AiBriefWorker
import com.afloria.smartregister.ui.*
import com.afloria.smartregister.ui.theme.SmartRegisterTheme

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Cancel legacy WorkManager tasks
        WorkManager.getInstance(applicationContext).cancelAllWorkByTag("legacy_model_download")
        
        // Schedule AI Brief background generation
        AiBriefWorker.schedule(applicationContext)

        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            
            SmartRegisterTheme(
                themeMode = viewModel.themeMode,
                seedColor = viewModel.selectedSeedColor,
                secondaryColor = viewModel.selectedSecondaryColor,
                tertiaryColor = viewModel.selectedTertiaryColor,
                fontFamily = viewModel.selectedFontFamily,
                fontWeight = viewModel.selectedFontWeight,
                                fontWidth = viewModel.selectedFontWidth,
                fontOpsz = viewModel.selectedFontOpsz,
                fontGrad = viewModel.selectedFontGrad,
                fontRond = viewModel.selectedFontRond
            ) {
                val appState by viewModel.appState.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val errorMessage by viewModel.errorMessage.collectAsState()

                when (val state = appState) {
                    is AppState.Landing, is AppState.ThemeSelection -> {
                        LandingScreen(viewModel = viewModel)
                    }
                    is AppState.Login -> {
                        LoginScreen(
                            onLoginClick = { user, pass -> viewModel.login(user, pass) },
                            isLoading = isLoading,
                            errorMessage = errorMessage
                        )
                    }
                    is AppState.SelectProfile -> {
                        SelectChildScreen(
                            choices = state.choices,
                            onChildSelected = { viewModel.selectProfile(it) },
                            isLoading = isLoading
                        )
                    }
                    is AppState.LoggedIn -> {
                        MainScreen(
                            viewModel = viewModel,
                            onLogout = { viewModel.logout() }
                        )
                    }
                }
            }
        }
    }
}
