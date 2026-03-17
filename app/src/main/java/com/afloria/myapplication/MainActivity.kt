package com.afloria.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.afloria.myapplication.ui.*
import com.afloria.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                val appState by viewModel.appState.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val errorMessage by viewModel.errorMessage.collectAsState()

                when (val state = appState) {
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
