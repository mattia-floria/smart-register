package com.afloria.smartregister

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.afloria.smartregister.ui.*
import com.afloria.smartregister.ui.theme.SmartRegisterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            
            SmartRegisterTheme(
                themeMode = viewModel.themeMode,
                seedColor = viewModel.selectedSeedColor,
                secondaryColor = viewModel.selectedSecondaryColor,
                tertiaryColor = viewModel.selectedTertiaryColor
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
