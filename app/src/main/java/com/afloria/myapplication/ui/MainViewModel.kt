package com.afloria.myapplication.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.afloria.myapplication.data.local.AuthStorage
import com.afloria.myapplication.data.remote.SpaggiariApi
import com.afloria.myapplication.data.remote.model.*
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import java.text.SimpleDateFormat
import java.util.*

sealed class AppState {
    object Login : AppState()
    data class LoggedIn(val response: LoginResponse) : AppState()
    data class SelectProfile(val choices: List<LoginChoice>) : AppState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val authStorage = try { AuthStorage(application) } catch (e: Exception) { null }
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }
    
    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("User-Agent", "CVVS/std/4.2.3")
            .header("Z-Dev-Apikey", "Tg1NWEwNGIgIC0K")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build()
        chain.proceed(request)
    }

    // Riduco il livello di log a HEADERS per non intasare ADB/Wifi
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
    }
    
    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("https://web.spaggiari.eu/rest/v1/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(SpaggiariApi::class.java)

    private val _appState = MutableStateFlow<AppState>(AppState.Login)
    val appState: StateFlow<AppState> = _appState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    var grades by mutableStateOf<List<GradeRemoteModel>>(emptyList())
    var notes by mutableStateOf<NotesResponse?>(null)
    var agenda by mutableStateOf<List<AgendaEventRemoteModel>>(emptyList())
    var notices by mutableStateOf<List<NoticeRemoteModel>>(emptyList())
    var teachersMaterials by mutableStateOf<List<TeacherRemoteModel>>(emptyList())

    private var tempPass: String? = null

    init {
        checkAutoLogin()
    }

    private fun checkAutoLogin() {
        val creds = authStorage?.getCredentials()
        val savedIdent = creds?.first
        val savedPass = creds?.second
        
        if (savedIdent != null && savedPass != null) {
            login(savedIdent, savedPass, isAutoLogin = true)
        }
    }

    fun login(ident: String, pass: String, isAutoLogin: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            tempPass = pass
            val cleanIdent = if (ident.contains("@")) ident.trim() else ident.trim().uppercase()
            try {
                val response = api.login(LoginRequest(ident = cleanIdent, pass = pass, uid = cleanIdent))
                
                if (response.requestedAction == "select_customer" || (response.token == null && response.choices != null)) {
                    _appState.value = AppState.SelectProfile(response.choices!!)
                } else if (response.token != null) {
                    if (!isAutoLogin) authStorage?.saveCredentials(cleanIdent, pass)
                    onLoginSuccess(response)
                } else {
                    _errorMessage.value = "Accesso non riuscito."
                    if (isAutoLogin) _appState.value = AppState.Login
                }
            } catch (e: HttpException) {
                // Se la password è scaduta o errata (401), puliamo i dati salvati
                if (e.code() == 401) {
                    authStorage?.clear()
                    _appState.value = AppState.Login
                    _errorMessage.value = "Sessione scaduta o credenziali errate."
                } else {
                    _errorMessage.value = "Errore server (${e.code()})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Nessuna connessione."
                // NON cancelliamo le credenziali per un semplice errore di rete!
                if (isAutoLogin) _appState.value = AppState.Login
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectProfile(choice: LoginChoice) {
        val pass = tempPass ?: return
        val profileIdent = choice.ident ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.login(LoginRequest(ident = profileIdent, pass = pass, uid = profileIdent))
                if (response.token != null) {
                    authStorage?.saveCredentials(profileIdent, pass)
                    onLoginSuccess(response)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Errore selezione."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun onLoginSuccess(response: LoginResponse) {
        _appState.value = AppState.LoggedIn(response)
        fetchAllData(response.token!!, response.ident!!)
    }

    private fun fetchAllData(token: String, ident: String) {
        viewModelScope.launch {
            try {
                val studentId = ident.filter { it.isDigit() }
                val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -15)
                val startDate = sdf.format(cal.time)
                cal.add(Calendar.DAY_OF_YEAR, 45)
                val endDate = sdf.format(cal.time)

                grades = api.getGrades(token, studentId).grades
                notes = api.getNotes(token, studentId)
                agenda = api.getAgenda(token, studentId, startDate, endDate).agenda
                notices = api.getNoticeboard(token, studentId).items
                teachersMaterials = api.getDidactics(token, studentId).teachers ?: emptyList()
            } catch (e: Exception) {
                Log.e("CV_DATA", "Data fetch failed", e)
            }
        }
    }

    fun logout() {
        authStorage?.clear()
        _appState.value = AppState.Login
        tempPass = null
        grades = emptyList()
        notes = null
        agenda = emptyList()
        notices = emptyList()
        teachersMaterials = emptyList()
    }
}
