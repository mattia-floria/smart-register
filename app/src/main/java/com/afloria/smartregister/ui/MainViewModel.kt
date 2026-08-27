package com.afloria.smartregister.ui

import android.app.Application
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.afloria.smartregister.ai.data.Model
import com.afloria.smartregister.ai.data.ModelDownloadStatusType
import com.afloria.smartregister.ai.download.ModelDownloadManager
import com.afloria.smartregister.ai.models.AiModels
import com.afloria.smartregister.ai.runtime.LlmChatModelHelper
import com.afloria.smartregister.data.local.AuthStorage
import com.afloria.smartregister.data.local.*
import com.afloria.smartregister.data.remote.SpaggiariApi
import com.afloria.smartregister.data.remote.model.*
import com.afloria.smartregister.ui.theme.ThemeMode
import android.content.Context
import com.afloria.smartregister.ai.runtime.SmartyToolSet
import com.afloria.smartregister.ai.utils.DataPreprocessor
import com.google.ai.edge.litertlm.*
import com.google.ai.edge.litertlm.tool
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response as OkHttpResponse
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

sealed class AppState {
    object Landing : AppState()
    object ThemeSelection : AppState()
    object Login : AppState()
    data class LoggedIn(val response: LoginResponse) : AppState()
    data class SelectProfile(val choices: List<LoginChoice>) : AppState()
}

enum class UpdateResult {
    IDLE, NO_UPDATES, UPDATED_FOUND, ERROR
}

data class FlappyPipe(val x: Float, val gapY: Float, val width: Float = 60f, val gapHeight: Float = 250f)
data class FlappyGameState(
    val birdY: Float = 500f,
    val birdVelocity: Float = 0f,
    val pipes: List<FlappyPipe> = emptyList(),
    val score: Int = 0,
    val isGameOver: Boolean = false,
    val isPlaying: Boolean = false
)

data class ChatMessage(val text: String, val isUser: Boolean, val image: android.graphics.Bitmap? = null)

sealed class DownloadEvent {
    data class Success(val uri: Uri, val fileName: String, val mimeType: String) : DownloadEvent()
    data class Error(val message: String) : DownloadEvent()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val authStorage = AuthStorage(application)
    private val context = application
    private val modelDownloadManager = ModelDownloadManager(application)

    private val _downloadEvents = MutableSharedFlow<DownloadEvent>()
    val downloadEvents: SharedFlow<DownloadEvent> = _downloadEvents.asSharedFlow()
    
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    var selectedSeedColor by mutableStateOf<Color?>(null)
    var selectedSecondaryColor by mutableStateOf<Color?>(null)
    var selectedTertiaryColor by mutableStateOf<Color?>(null)
    var selectedFontFamily by mutableStateOf("DEFAULT")
    var selectedFontWeight by mutableFloatStateOf(400f)
    var selectedFontWidth by mutableFloatStateOf(100f)
    var selectedFontOpsz by mutableFloatStateOf(14f)
    var selectedFontGrad by mutableFloatStateOf(0f)
    var selectedFontRond by mutableFloatStateOf(0f)

    // Update state
    var isSearchingForUpdates by mutableStateOf(false)
    var updateCheckResult by mutableStateOf<UpdateResult?>(null)
    var isNightlyEnabled by mutableStateOf(false)
    var isUpdateNotificationsEnabled by mutableStateOf(true)
    var updateCheckCount by mutableIntStateOf(0)
    var dynamicUpdateMessage by mutableStateOf("")
    var isEasterEggActive by mutableStateOf(false)
    
    // Flappy Game State
    var flappyGame by mutableStateOf(FlappyGameState())
    private var gameJob: Job? = null

    fun checkForUpdates() {
        viewModelScope.launch {
            isSearchingForUpdates = true
            updateCheckResult = null
            
            // Simulate GitHub release check delay
            kotlinx.coroutines.delay(3500)
            
            updateCheckCount++
            
            dynamicUpdateMessage = when (updateCheckCount) {
                1 -> "Paga lo sviluppatore e ci saranno più aggiornamenti"
                2 -> "Ti posso assicurare che non ci sono aggiornamenti e hai già l'ultima versione"
                3 -> "Stai tranquillo che anche se aspetti 30 secondi non ci sarà un aggiornamento"
                4 -> "Hai veramente così tanto bisogno di un aggiornamento?"
                5 -> "Piuttosto che continuare a provarci vai a studiare"
                else -> {
                    isEasterEggActive = true
                    "VA BENE, ECCO IL TUO AGGIORNAMENTO SEGRETO! 🚀"
                }
            }
            
            isSearchingForUpdates = false
            updateCheckResult = UpdateResult.NO_UPDATES
        }
    }

    fun startFlappyGame() {
        flappyGame = FlappyGameState(isPlaying = true)
        gameJob?.cancel()
        gameJob = viewModelScope.launch {
            val gravity = 0.7f
            val pipeSpeed = 7f
            var frameCount = 0
            
            while (flappyGame.isPlaying && !flappyGame.isGameOver) {
                kotlinx.coroutines.delay(16) // ~60fps
                
                val current = flappyGame
                val newBirdY = current.birdY + current.birdVelocity
                val newBirdVel = current.birdVelocity + gravity
                
                val newPipes = current.pipes.map { it.copy(x = it.x - pipeSpeed) }.filter { it.x > -100 }
                val spawnedPipes = if (frameCount % 80 == 0) {
                    newPipes + FlappyPipe(x = 1100f, gapY = (300..900).random().toFloat(), gapHeight = 350f)
                } else newPipes
                
                var newScore = current.score
                current.pipes.forEach { pipe ->
                    if (pipe.x + pipe.width < 150 && pipe.x + pipe.width >= 150 - pipeSpeed) {
                        newScore++
                    }
                }

                // Simple collision
                val hitPipe = spawnedPipes.any { pipe ->
                    val birdX = 150f
                    val birdSize = 60f
                    val inPipeX = birdX + birdSize > pipe.x && birdX < pipe.x + pipe.width
                    val hitTop = newBirdY < pipe.gapY
                    val hitBottom = newBirdY + birdSize > pipe.gapY + pipe.gapHeight
                    inPipeX && (hitTop || hitBottom)
                }
                
                val outOfBounds = newBirdY < 0 || newBirdY > 1800
                
                if (hitPipe || outOfBounds) {
                    flappyGame = current.copy(isGameOver = true, birdY = newBirdY, pipes = spawnedPipes, score = newScore)
                } else {
                    flappyGame = current.copy(
                        birdY = newBirdY,
                        birdVelocity = newBirdVel,
                        pipes = spawnedPipes,
                        score = newScore
                    )
                }
                frameCount++
            }
        }
    }

    fun flapBird() {
        if (flappyGame.isGameOver) {
            startFlappyGame()
        } else {
            flappyGame = flappyGame.copy(birdVelocity = -13f)
        }
    }

    fun resetEasterEgg() {
        isEasterEggActive = false
        updateCheckCount = 0
        updateCheckResult = null
        flappyGame = FlappyGameState()
        gameJob?.cancel()
    }

    fun toggleNightly(enabled: Boolean) {
        isNightlyEnabled = enabled
    }

    fun toggleUpdateNotifications(enabled: Boolean) {
        isUpdateNotificationsEnabled = enabled
    }

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

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val spaggiariClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .build()

    private val spaggiariRetrofit = Retrofit.Builder()
        .baseUrl("https://web.spaggiari.eu/rest/v1/")
        .client(spaggiariClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val api = spaggiariRetrofit.create(SpaggiariApi::class.java)

    private val _appState = MutableStateFlow<AppState>(AppState.Landing)
    val appState: StateFlow<AppState> = _appState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress

    var grades by mutableStateOf<List<GradeRemoteModel>>(emptyList())
    var notes by mutableStateOf<NotesResponse?>(null)
    var agenda by mutableStateOf<List<AgendaEventRemoteModel>>(emptyList())
    var notices by mutableStateOf<List<NoticeRemoteModel>>(emptyList())
    var teachersMaterials by mutableStateOf<List<TeacherRemoteModel>>(emptyList())
    var absences by mutableStateOf<List<AbsenceRemoteModel>>(emptyList())
    var finalGrades by mutableStateOf<List<SchoolReportRemoteModel>>(emptyList())
    var studentName by mutableStateOf("")

    private val _timetableData = MutableStateFlow(TimetableData())
    val timetableData: StateFlow<TimetableData> = _timetableData

    private val _modernDashboardConfig = MutableStateFlow(
        authStorage.getModernDashboardConfig() ?: ModernDashboardConfig(
            widgets = listOf(
                DashboardWidgetState("ai_brief", WidgetType.AI_BRIEF, 0, width = 2, height = 2),
                DashboardWidgetState("gpa", WidgetType.RECOVERY_STATUS, 1, width = 1, height = 1),
                DashboardWidgetState("countdown", WidgetType.COUNTDOWN, 2, width = 1, height = 1),
                DashboardWidgetState("weekly_chart", WidgetType.WEEKLY_CHART, 3, width = 2, height = 1),
                DashboardWidgetState("agenda", WidgetType.TOMORROW_AGENDA, 4, width = 2, height = 1)
            )
        )
    )
    val modernDashboardConfig: StateFlow<ModernDashboardConfig> = _modernDashboardConfig

    private val _dashboardConfig = MutableStateFlow(
        authStorage.getDashboardConfig() ?: DashboardConfig(
            widgets = listOf(
                DashboardWidget(WidgetType.AI_BRIEF, isFullWidth = true),
                DashboardWidget(WidgetType.COUNTDOWN, isFullWidth = false),
                DashboardWidget(WidgetType.RECOVERY_STATUS, isFullWidth = false),
                DashboardWidget(WidgetType.WEEKLY_CHART, isFullWidth = true),
                DashboardWidget(WidgetType.TOMORROW_AGENDA, isFullWidth = true)
            )
        )
    )
    val dashboardConfig: StateFlow<DashboardConfig> = _dashboardConfig

    // AI State
    var selectedAiModelName by mutableStateOf(authStorage.getAiModel())
    val currentModel: Model?
        get() = AiModels.ALL_MODELS.find { it.name == selectedAiModelName }

    var isLlmReady by mutableStateOf(false)
    var isLlmInitializing by mutableStateOf(false)
    var isModelDownloading by mutableStateOf(false)
    var modelDownloadProgress by mutableStateOf(0f)
    var modelDownloadError by mutableStateOf<String?>(null)
    val chatMessages = mutableStateListOf<ChatMessage>()
    var isChatLoading by mutableStateOf(false)
    var isChatOpen by mutableStateOf(false)
    var isChatEnabled by mutableStateOf(true)
    var isExperimentalEnabled by mutableStateOf(false)
    var isAiBriefEnabled by mutableStateOf(false)
    var aiBriefSummary by mutableStateOf<String?>(null)
    var isAiBriefLoading by mutableStateOf(false)

    private var downloadStatusJob: Job? = null

    private var tempPass: String? = null

    init {
        loadThemeSettings()
        loadTimetable()
        loadCachedData()
        checkInitialState()
        initOrDownloadModel()
    }

    private fun loadCachedData() {
        val cachedLogin = authStorage.getLoginResponse()
        if (cachedLogin != null) {
            studentName = cachedLogin.firstName ?: ""
            grades = authStorage.getGrades()
            notes = authStorage.getNotes()
            agenda = authStorage.getAgenda()
            notices = authStorage.getNotices()
            teachersMaterials = authStorage.getMaterials()
            absences = authStorage.getAbsences()
            finalGrades = authStorage.getFinalGrades()
            aiBriefSummary = authStorage.getAiBriefSummary()
            
            // If we have cached data, we can start in LoggedIn state
            if (_appState.value == AppState.Landing && !authStorage.isFirstLaunch()) {
                _appState.value = AppState.LoggedIn(cachedLogin)
            }
        }
    }

    fun switchAiModel(modelName: String) {
        // Clean up previous model before switching
        currentModel?.let { LlmChatModelHelper.cleanUp(it) {} }
        
        selectedAiModelName = modelName
        authStorage.saveAiModel(modelName)
        
        // Reset state for the new model
        isLlmReady = false
        isModelDownloading = false
        modelDownloadProgress = 0f
        modelDownloadError = null
        
        initOrDownloadModel()
    }

    private fun initOrDownloadModel() {
        val model = currentModel ?: return
        
        downloadStatusJob?.cancel()
        downloadStatusJob = viewModelScope.launch {
            modelDownloadManager.getDownloadStatus(model).collectLatest { status ->
                when (status.status) {
                    ModelDownloadStatusType.NOT_DOWNLOADED -> {
                        isModelDownloading = false
                        isLlmReady = false
                        modelDownloadError = null
                    }
                    ModelDownloadStatusType.IN_PROGRESS -> {
                        isModelDownloading = true
                        isLlmReady = false
                        modelDownloadError = null
                        if (status.totalBytes > 0) {
                            modelDownloadProgress = status.receivedBytes.toFloat() / status.totalBytes
                        }
                    }
                    ModelDownloadStatusType.SUCCEEDED -> {
                        isModelDownloading = false
                        modelDownloadError = null
                        if (!isLlmReady && !isLlmInitializing) {
                            setupLocalLlm(model)
                        }
                    }
                    ModelDownloadStatusType.FAILED -> {
                        isModelDownloading = false
                        modelDownloadError = status.errorMessage
                        Log.e("AI", "Download failed: ${status.errorMessage}")
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalApi::class)
    private fun setupLocalLlm(model: Model) {
        viewModelScope.launch(Dispatchers.Default) {
            if (isLlmInitializing) return@launch
            isLlmInitializing = true
            modelDownloadError = null 
            
            val tools: List<ToolProvider> = emptyList()

            LlmChatModelHelper.initialize(
                context = context,
                model = model,
                supportImage = model.llmSupportImage,
                supportAudio = model.llmSupportAudio,
                tools = tools,
                systemInstruction = null, // Rimosso contesto sistema
                onDone = { error ->
                    viewModelScope.launch {
                        isLlmInitializing = false
                        if (error.isEmpty()) {
                            isLlmReady = true
                            modelDownloadError = null
                            Log.d("AI", "Model ${model.name} ready")
                            if (isAiBriefEnabled && aiBriefSummary == null) {
                                generateAiBrief()
                            }
                        } else {
                            isLlmReady = false
                            modelDownloadError = "Errore inizializzazione: $error"
                            Log.e("AI", "Model initialization failed: $error")
                        }
                    }
                }
            )
        }
    }

    private fun loadThemeSettings() {
        val settings = authStorage.getThemeSettings()
        themeMode = settings.mode
        selectedSeedColor = settings.seed
        selectedSecondaryColor = settings.secondary
        selectedTertiaryColor = settings.tertiary
        selectedFontFamily = settings.fontFamily
        selectedFontWeight = settings.fontWeight
        selectedFontWidth = settings.fontWidth
        selectedFontOpsz = settings.fontOpsz
        selectedFontGrad = settings.fontGrad
        selectedFontRond = settings.fontRond
        isChatEnabled = authStorage.isChatEnabled()
        isExperimentalEnabled = authStorage.isExperimentalEnabled()
        isAiBriefEnabled = authStorage.isAiBriefEnabled()
    }

    private fun loadTimetable() {
        _timetableData.value = authStorage.getTimetable()
    }

    fun saveTimetableEntry(entry: TimetableEntry) {
        val currentEntries = _timetableData.value.entries.toMutableList()
        val index = currentEntries.indexOfFirst { it.id == entry.id }
        if (index != -1) {
            currentEntries[index] = entry
        } else {
            currentEntries.add(entry)
        }
        val newData = TimetableData(currentEntries)
        _timetableData.value = newData
        authStorage.saveTimetable(newData)
    }

    fun deleteTimetableEntry(entry: TimetableEntry) {
        val currentEntries = _timetableData.value.entries.toMutableList()
        currentEntries.removeAll { it.id == entry.id }
        val newData = TimetableData(currentEntries)
        _timetableData.value = newData
        authStorage.saveTimetable(newData)
    }

    fun generateTimetableFromAgenda() {
        if (agenda.isEmpty()) {
            viewModelScope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Carica prima l'agenda!", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        )
        val cal = Calendar.getInstance()
        val daySlotFreq = mutableMapOf<Int, MutableMap<Int, MutableMap<String, Int>>>()

        agenda.forEach { event ->
            val dateStr = event.evtDatetimeBegin
            val subjectRaw = event.subjectDesc
            if (!dateStr.isNullOrBlank() && !subjectRaw.isNullOrBlank()) {
                var date: Date? = null
                for (format in formats) {
                    try {
                        date = format.parse(dateStr)
                        if (date != null) break
                    } catch (e: Exception) {}
                }

                if (date != null) {
                    cal.time = date
                    val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
                    if (dayOfWeek <= 6) {
                        val hour = cal.get(Calendar.HOUR_OF_DAY)
                        val period = if (hour >= 8) hour - 7 else 1 
                        val subject = subjectRaw.split("(").first().trim().uppercase()
                        val dayMap = daySlotFreq.getOrPut(dayOfWeek) { mutableMapOf() }
                        val slotMap = dayMap.getOrPut(period) { mutableMapOf() }
                        slotMap[subject] = (slotMap[subject] ?: 0) + 1
                    }
                }
            }
        }

        val newEntries = mutableListOf<TimetableEntry>()
        daySlotFreq.forEach { (day, slots) ->
            slots.forEach { (period, subjects) ->
                val bestSubject = subjects.maxByOrNull { it.value }?.key
                if (bestSubject != null) {
                    newEntries.add(TimetableEntry(dayOfWeek = day, period = period, subjectName = bestSubject))
                }
            }
        }

        if (newEntries.isNotEmpty()) {
            val newData = TimetableData(newEntries)
            _timetableData.value = newData
            authStorage.saveTimetable(newData)
            viewModelScope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Orario rigenerato!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateTheme(mode: ThemeMode) {
        themeMode = mode
        authStorage.saveThemeSettings(themeMode, selectedSeedColor, selectedSecondaryColor, selectedTertiaryColor, selectedFontFamily, selectedFontWeight, selectedFontWidth, selectedFontOpsz, selectedFontGrad, selectedFontRond)
    }

    fun updatePalette(seed: Color?, secondary: Color?, tertiary: Color?) {
        selectedSeedColor = seed
        selectedSecondaryColor = secondary
        selectedTertiaryColor = tertiary
        authStorage.saveThemeSettings(themeMode, selectedSeedColor, selectedSecondaryColor, selectedTertiaryColor, selectedFontFamily, selectedFontWeight, selectedFontWidth, selectedFontOpsz, selectedFontGrad, selectedFontRond)
    }

    fun updateFont(font: String) {
        selectedFontFamily = font
        authStorage.saveThemeSettings(themeMode, selectedSeedColor, selectedSecondaryColor, selectedTertiaryColor, selectedFontFamily, selectedFontWeight, selectedFontWidth, selectedFontOpsz, selectedFontGrad, selectedFontRond)
    }

    fun updateFontWeight(weight: Float) {
        selectedFontWeight = weight
        authStorage.saveThemeSettings(themeMode, selectedSeedColor, selectedSecondaryColor, selectedTertiaryColor, selectedFontFamily, selectedFontWeight, selectedFontWidth, selectedFontOpsz, selectedFontGrad, selectedFontRond)
    }

    fun updateFontWidth(width: Float) {
        selectedFontWidth = width
        authStorage.saveThemeSettings(themeMode, selectedSeedColor, selectedSecondaryColor, selectedTertiaryColor, selectedFontFamily, selectedFontWeight, selectedFontWidth, selectedFontOpsz, selectedFontGrad, selectedFontRond)
    }

    fun updateFontOpsz(opsz: Float) {
        selectedFontOpsz = opsz
        authStorage.saveThemeSettings(themeMode, selectedSeedColor, selectedSecondaryColor, selectedTertiaryColor, selectedFontFamily, selectedFontWeight, selectedFontWidth, selectedFontOpsz, selectedFontGrad, selectedFontRond)
    }

    fun updateFontGrad(grad: Float) {
        selectedFontGrad = grad
        authStorage.saveThemeSettings(themeMode, selectedSeedColor, selectedSecondaryColor, selectedTertiaryColor, selectedFontFamily, selectedFontWeight, selectedFontWidth, selectedFontOpsz, selectedFontGrad, selectedFontRond)
    }

    fun updateFontRond(rond: Float) {
        selectedFontRond = rond
        authStorage.saveThemeSettings(themeMode, selectedSeedColor, selectedSecondaryColor, selectedTertiaryColor, selectedFontFamily, selectedFontWeight, selectedFontWidth, selectedFontOpsz, selectedFontGrad, selectedFontRond)
    }

    fun toggleChat(enabled: Boolean) {
        isChatEnabled = enabled
        authStorage.setChatEnabled(enabled)
        if (!enabled) chatMessages.clear()
        checkAiStatus()
    }

    fun toggleExperimental(enabled: Boolean) {
        isExperimentalEnabled = enabled
        authStorage.setExperimentalEnabled(enabled)
        if (!enabled) {
            toggleAiBrief(false)
        }
    }

    fun toggleAiBrief(enabled: Boolean) {
        isAiBriefEnabled = enabled
        if (enabled && aiBriefSummary == null) {
            generateAiBrief()
        }
        authStorage.setAiBriefEnabled(enabled)
    }

    private fun checkAiStatus() {
        if (!isChatEnabled) {
            currentModel?.let { LlmChatModelHelper.cleanUp(it) {} }
            isLlmReady = false
        } else if (!isLlmReady && !isLlmInitializing && !isModelDownloading) {
            initOrDownloadModel()
        }
    }

    fun deleteSelectedModel() {
        val model = currentModel ?: return
        viewModelScope.launch {
            LlmChatModelHelper.cleanUp(model) {
                modelDownloadManager.deleteModel(model)
                isLlmReady = false
                modelDownloadError = null
                viewModelScope.launch(Dispatchers.Main) {
                    Toast.makeText(context, "Modello ${model.displayName} eliminato", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkInitialState() {
        if (authStorage.isFirstLaunch()) {
            _appState.value = AppState.Landing
        } else {
            val creds = authStorage.getCredentials()
            if (creds.first != null && creds.second != null) {
                login(creds.first!!, creds.second!!, isAutoLogin = true)
            } else {
                _appState.value = AppState.Login
            }
        }
    }

    fun nextFromLanding() {
        _appState.value = AppState.ThemeSelection
    }

    fun nextFromTheme() {
        authStorage.setFirstLaunchCompleted()
        _appState.value = AppState.Login
    }

    fun login(ident: String, pass: String, isAutoLogin: Boolean = false) {
        viewModelScope.launch {
            if (!isAutoLogin) _isLoading.value = true
            _errorMessage.value = null
            tempPass = pass
            val cleanIdent = ident.trim()
            try {
                val response = api.login(LoginRequest(ident = cleanIdent, pass = pass, uid = cleanIdent))
                
                if (response.requestedAction == "select_customer" || (response.token == null && response.choices != null)) {
                    _appState.value = AppState.SelectProfile(response.choices!!)
                } else if (response.token != null) {
                    authStorage.saveCredentials(cleanIdent, pass)
                    onLoginSuccess(response)
                } else {
                    _errorMessage.value = "Accesso non riuscito."
                    _appState.value = AppState.Login
                }
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    authStorage.clear()
                    _appState.value = AppState.Login
                    _errorMessage.value = "Sessione scaduta o credenziali errate."
                } else {
                    _errorMessage.value = "Errore server (${e.code()})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Nessuna connessione."
                if (isAutoLogin) {
                    val cachedLogin = authStorage.getLoginResponse()
                    if (cachedLogin != null) {
                        onLoginSuccess(cachedLogin)
                    } else {
                        _appState.value = AppState.Login
                    }
                }
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
                    authStorage.saveCredentials(profileIdent, pass)
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
        studentName = response.firstName ?: ""
        authStorage.saveLoginResponse(response)
        _appState.value = AppState.LoggedIn(response)
        viewModelScope.launch {
            fetchAllData(response.token!!, response.ident!!)
        }
    }

    fun refreshData() {
        val state = appState.value
        if (state is AppState.LoggedIn) {
            viewModelScope.launch {
                _isRefreshing.value = true
                fetchAllData(state.response.token!!, state.response.ident!!)
                if (isAiBriefEnabled) {
                    generateAiBrief()
                }
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun fetchAllData(token: String, ident: String) {
        try {
            // Try both filtered and raw ID as some endpoints might behave differently
            val studentIdDigits = ident.filter { it.isDigit() }
            val studentId = if (studentIdDigits.isEmpty()) ident else studentIdDigits
            
            // For range endpoints, use yyyy-MM-dd format as seen in reference repo
            val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val rangeCal = Calendar.getInstance()
            
            // Academic year logic: 
            // If month is < 9 (Jan-Aug), the year started in Sept of PREVIOUS year.
            // If month is >= 9 (Sept-Dec), the year started in Sept of CURRENT year.
            val currentYear = rangeCal.get(Calendar.YEAR)
            val currentMonth = rangeCal.get(Calendar.MONTH) // 0-indexed
            
            val academicStartYear = if (currentMonth < Calendar.SEPTEMBER) currentYear - 1 else currentYear
            
            val startCal = Calendar.getInstance()
            startCal.set(academicStartYear, Calendar.SEPTEMBER, 1)
            val startDateStr = apiDateFormat.format(startCal.time)
            
            val endCal = Calendar.getInstance()
            endCal.set(academicStartYear + 1, Calendar.AUGUST, 31)
            val endDateStr = apiDateFormat.format(endCal.time)

            Log.d("CV_SYNC", "Fetching for ID: $studentId, Range: $startDateStr to $endDateStr")

            // Wrap each call in a safe try-catch to allow partial successes
            try {
                val gradesResponse = api.getGrades(token, studentId)
                grades = gradesResponse.grades.sortedByDescending { it.evtDate }
                authStorage.saveGrades(grades)
            } catch (e: Exception) {
                Log.e("CV_GRADES", "Grades fetch failed", e)
            }

            try {
                notes = api.getNotes(token, studentId)
                notes?.let { authStorage.saveNotes(it) }
            } catch (e: Exception) { Log.e("CV_NOTES", "Notes fetch failed", e) }

            try {
                val lessonsResponse = api.getLessons(token, studentId, startDateStr, endDateStr)
                // We could merge lessons into agenda or keep them separate
            } catch (e: Exception) { Log.e("CV_LESSONS", "Lessons fetch failed", e) }

            try {
                val agendaResponse = api.getAgenda(token, studentId, startDateStr, endDateStr)
                agenda = agendaResponse.agenda
                authStorage.saveAgenda(agenda)
            } catch (e: Exception) { Log.e("CV_AGENDA", "Agenda fetch failed", e) }

            try {
                notices = api.getNoticeboard(token, studentId).items
                authStorage.saveNotices(notices)
            } catch (e: Exception) { Log.e("CV_NOTICES", "Notices fetch failed", e) }

            try {
                teachersMaterials = api.getDidactics(token, studentId).teachers ?: emptyList()
                authStorage.saveMaterials(teachersMaterials)
            } catch (e: Exception) { Log.e("CV_DIDACTICS", "Materials fetch failed", e) }

            try {
                absences = api.getAbsences(token, studentId).events.sortedByDescending { it.evtDate }
                authStorage.saveAbsences(absences)
            } catch (e: Exception) { Log.e("CV_ABSENCES", "Absences fetch failed", e) }
            
            try {
                finalGrades = api.getDocuments(token, studentId).schoolReports ?: emptyList()
                authStorage.saveFinalGrades(finalGrades)
            } catch (e: Exception) {
                Log.e("SCRUTINI_FETCH", "Failed to fetch final grades", e)
            }
            
            authStorage.saveLastUpdateTimestamp(System.currentTimeMillis())

            if (agenda.isNotEmpty() && _timetableData.value.entries.isEmpty()) {
                generateTimetableFromAgenda()
            }
        } catch (e: Exception) {
            Log.e("CV_DATA_ALL", "Global data fetch failed", e)
            withContext(Dispatchers.Main) {
                showOfflineMessage()
            }
        }
    }

    private fun showOfflineMessage() {
        val lastUpdate = authStorage.getLastUpdateTimestamp()
        if (lastUpdate > 0) {
            val diff = System.currentTimeMillis() - lastUpdate
            val minutes = diff / (1000 * 60)
            val hours = minutes / 60
            val days = hours / 24

            val timeStr = when {
                days > 0 -> "$days giorn${if (days == 1L) "o" else "i"} fa"
                hours > 0 -> "$hours or${if (hours == 1L) "a" else "e"} fa"
                minutes > 0 -> "$minutes minut${if (minutes == 1L) "o" else "i"} fa"
                else -> "poco fa"
            }
            Toast.makeText(context, "Dati aggiornati l'ultima volta $timeStr", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Errore di connessione e nessun dato salvato", Toast.LENGTH_LONG).show()
        }
    }

    fun viewFinalReport(report: SchoolReportRemoteModel) {
        val state = appState.value as? AppState.LoggedIn ?: return
        val token = state.response.token ?: return
        val url = report.viewLink ?: return

        viewModelScope.launch {
            _isDownloading.value = true
            _downloadProgress.value = 0f
            try {
                val response = api.downloadFile(token, url)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val fileName = "${report.desc ?: "scrutinio"}.pdf"
                        val contentType = "application/pdf"
                        val uri = saveFileWithProgress(fileName, body, contentType)
                        if (uri != null) {
                            _downloadEvents.emit(DownloadEvent.Success(uri, fileName, contentType))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("VIEW_REPORT_ERROR", "Failed to download school report", e)
                _downloadEvents.emit(DownloadEvent.Error("Errore durante il download."))
            } finally {
                _isDownloading.value = false
            }
        }
    }

    fun downloadDidacticFile(content: ContentRemoteModel) {
        val state = appState.value as? AppState.LoggedIn ?: return
        val token = state.response.token ?: return
        val studentId = state.response.ident?.filter { it.isDigit() } ?: return
        val contentId = content.contentId ?: return

        viewModelScope.launch {
            _isDownloading.value = true
            _downloadProgress.value = 0f
            try {
                val response = api.getDidacticItem(token, studentId, contentId)
                if (response.isSuccessful) {
                    val body = response.body()
                    val contentType = response.headers()["Content-Type"] ?: "application/pdf"
                    if (body != null) {
                        var fileName = content.contentName ?: "documento"
                        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType) ?: "pdf"
                        if (!fileName.lowercase().endsWith(".$extension")) {
                            fileName = "$fileName.$extension"
                        }
                        val uri = saveFileWithProgress(fileName, body, contentType)
                        if (uri != null) {
                            _downloadEvents.emit(DownloadEvent.Success(uri, fileName, contentType))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DOWNLOAD_ERROR", "Failed to download didactic file", e)
                _downloadEvents.emit(DownloadEvent.Error("Errore durante il download."))
            } finally {
                _isDownloading.value = false
            }
        }
    }

    fun downloadNoticeAttachment(notice: NoticeRemoteModel, attachment: NoticeAttachmentRemoteModel) {
        val state = appState.value as? AppState.LoggedIn ?: return
        val token = state.response.token ?: return
        val studentId = state.response.ident?.filter { it.isDigit() } ?: return
        val evtCode = notice.evtCode ?: "CF"
        val pubId = notice.pubId ?: return
        val attachNum = attachment.attachNum ?: return

        viewModelScope.launch {
            _isDownloading.value = true
            _downloadProgress.value = 0f
            try {
                val response = api.getNoticeboardAttachment(token, studentId, evtCode, pubId, attachNum)
                if (response.isSuccessful) {
                    val body = response.body()
                    val contentType = response.headers()["Content-Type"] ?: "application/pdf"
                    if (body != null) {
                        val fileName = attachment.fileName ?: "allegato.pdf"
                        val uri = saveFileWithProgress(fileName, body, contentType)
                        if (uri != null) {
                            _downloadEvents.emit(DownloadEvent.Success(uri, fileName, contentType))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DOWNLOAD_ERROR", "Failed to download notice attachment", e)
                _downloadEvents.emit(DownloadEvent.Error("Errore durante il download."))
            } finally {
                _isDownloading.value = false
            }
        }
    }

    private suspend fun saveFileWithProgress(fileName: String, body: ResponseBody, mimeType: String): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri).use { outputStream ->
                        val inputStream = body.byteStream()
                        val totalBytes = body.contentLength()
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var downloadedBytes: Long = 0
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream!!.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            if (totalBytes > 0) {
                                _downloadProgress.value = downloadedBytes.toFloat() / totalBytes.toFloat()
                            }
                        }
                        outputStream!!.flush()
                    }
                    uri
                } else null
            } catch (e: Exception) {
                Log.e("SAVE_FILE_ERROR", "Failed to save file", e)
                null
            }
        }
    }

    fun openFile(uri: Uri, mimeType: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Apri con...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(chooser)
        } catch (e: Exception) {
            viewModelScope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Impossibile aprire il file.", Toast.LENGTH_SHORT).show()
            }
            Log.e("OPEN_FILE_ERROR", "Failed to open file", e)
        }
    }

    fun sendChatMessage(text: String, image: android.graphics.Bitmap? = null) {
        if (text.isBlank() && image == null) return
        if (isChatLoading) return
        
        if (chatMessages.isEmpty() || chatMessages.last().text != text) {
            chatMessages.add(ChatMessage(text, true, image))
        }
        
        val model = currentModel
        if (model != null && isLlmReady) {
            viewModelScope.launch(Dispatchers.Default) {
                isChatLoading = true
                try {
                    val prompt = text // Rimosso buildComplexQueryContext

                    var accumulatedResponse = ""
                    LlmChatModelHelper.runInference(
                        model = model,
                        input = prompt,
                        images = if (image != null) listOf(image) else emptyList(),
                        resultListener = { part, done, _ ->
                            if (done) {
                                viewModelScope.launch(Dispatchers.Main) {
                                    isChatLoading = false
                                }
                            } else {
                                // Rileva se il modello restituisce l'intero messaggio (cumulativo) o solo l'ultima parte (delta)
                                if (part.startsWith(accumulatedResponse) && accumulatedResponse.isNotEmpty()) {
                                    accumulatedResponse = part
                                } else {
                                    accumulatedResponse += part
                                }
                                
                                // Filtra eventuali token di padding fastidiosi come <pad>
                                val cleanResponse = accumulatedResponse
                                    .replace("<pad>", "")
                                    .replace("pad pad", "pad")
                                    .trim()
                                
                                if (cleanResponse.isNotEmpty()) {
                                    viewModelScope.launch(Dispatchers.Main) {
                                        if (chatMessages.isNotEmpty() && !chatMessages.last().isUser) {
                                            chatMessages[chatMessages.size - 1] = ChatMessage(cleanResponse, false)
                                        } else {
                                            chatMessages.add(ChatMessage(cleanResponse, false))
                                        }
                                    }
                                }
                            }
                        },
                        cleanUpListener = {},
                        onError = { error ->
                            viewModelScope.launch(Dispatchers.Main) {
                                chatMessages.add(ChatMessage("Errore: $error", false))
                                isChatLoading = false
                            }
                        }
                    )
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        chatMessages.add(ChatMessage("Errore durante l'elaborazione.", false))
                        isChatLoading = false
                    }
                }
            }
        } else {
            chatMessages.add(ChatMessage("L'assistente locale non è pronto.", false))
        }
    }

    fun setCustomModelPath(path: String) {
        context.getSharedPreferences("ai_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("custom_model_path", path)
            .apply()
        
        // Forza il ricaricamento del modello se necessario
        currentModel?.let { LlmChatModelHelper.cleanUp(it) {} }
        isLlmReady = false
        initOrDownloadModel()
    }

    private fun getNextWeekEvents(): List<AgendaEventRemoteModel> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val events = mutableListOf<AgendaEventRemoteModel>()
        // Include anche oggi
        for (i in 0..7) {
            val dateStr = sdf.format(cal.time)
            events.addAll(agenda.filter { it.evtDatetimeBegin?.startsWith(dateStr) == true })
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return events
    }

    fun getTomorrowEvents(): List<AgendaEventRemoteModel> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val tomorrowStr = sdf.format(cal.time)
        return agenda.filter { it.evtDatetimeBegin?.startsWith(tomorrowStr) == true }
    }

    fun downloadCurrentModel() {
        currentModel?.let { 
            modelDownloadManager.downloadModel(it)
            initOrDownloadModel()
        }
    }

    fun clearChat() {
        chatMessages.clear()
        currentModel?.let { model ->
            LlmChatModelHelper.resetConversation(
                model = model,
                supportImage = model.llmSupportImage,
                supportAudio = model.llmSupportAudio,
                tools = emptyList()
            )
        }
    }

    fun explainWithSmarty(event: AgendaEventRemoteModel) {
        val prompt = "Spiegami meglio questo evento in agenda: \"${event.notes}\" della materia ${event.subjectDesc} in data ${event.evtDatetimeBegin}."
        isChatOpen = true
        sendChatMessage(prompt)
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Smart Register", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copiato negli appunti", Toast.LENGTH_SHORT).show()
    }

    fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, "Condividi con...")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun generateAiBrief() {
        if (!isLlmReady || isAiBriefLoading) return
        
        val nextEvents = getNextWeekEvents()

        viewModelScope.launch(Dispatchers.Default) {
            isAiBriefLoading = true
            
            // RESET per evitare che eventi passati inquinino il brief
            currentModel?.let { model ->
                LlmChatModelHelper.resetConversation(
                    model = model,
                    supportImage = model.llmSupportImage,
                    supportAudio = model.llmSupportAudio,
                    tools = emptyList()
                )
            }
            
            val prompt = DataPreprocessor.buildAiBriefPrompt(nextEvents, studentName)
            var accumulatedBrief = ""

            currentModel?.let { model ->
                LlmChatModelHelper.runInference(
                    model = model,
                    input = prompt,
                    resultListener = { part, done, _ ->
                        if (done) {
                            viewModelScope.launch {
                                isAiBriefLoading = false
                                // Salva nel DB solo a generazione completata per performance
                                aiBriefSummary?.let { authStorage.saveAiBriefSummary(it) }
                            }
                        } else {
                            if (part.startsWith(accumulatedBrief) && accumulatedBrief.isNotEmpty()) {
                                accumulatedBrief = part
                            } else {
                                accumulatedBrief += part
                            }
                            
                            val cleanBrief = accumulatedBrief
                                .replace("<pad>", "")
                                .trim()
                            
                            viewModelScope.launch {
                                aiBriefSummary = cleanBrief
                            }
                        }
                    },
                    cleanUpListener = {},
                    onError = { error ->
                        viewModelScope.launch {
                            isAiBriefLoading = false
                            Log.e("AI", "Brief error: $error")
                        }
                    }
                )
            }
        }
    }

    fun importModelFromUri(uri: Uri) {
        val model = currentModel ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val destFile = File(model.getPath(context))
                val success = com.afloria.smartregister.utils.FileUtils.copyUriToFile(context, uri, destFile)
                
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(context, "Modello importato con successo", Toast.LENGTH_SHORT).show()
                        initOrDownloadModel()
                    } else {
                        Toast.makeText(context, "Errore durante l'importazione", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun getCredentials() = authStorage.getCredentials()

    fun updateModernDashboardConfig(newConfig: ModernDashboardConfig) {
        _modernDashboardConfig.value = newConfig
        authStorage.saveModernDashboardConfig(newConfig)
    }

    fun updateWidgetState(id: String, transform: (DashboardWidgetState) -> DashboardWidgetState) {
        val currentWidgets = _modernDashboardConfig.value.widgets.map { 
            if (it.id == id) transform(it) else it
        }
        updateModernDashboardConfig(ModernDashboardConfig(currentWidgets))
    }

    fun cycleWidgetColor(id: String) {
        updateWidgetState(id) { state ->
            val nextColor = when (state.colorType) {
                WidgetColorType.SURFACE -> WidgetColorType.PRIMARY
                WidgetColorType.PRIMARY -> WidgetColorType.SECONDARY
                WidgetColorType.SECONDARY -> WidgetColorType.TERTIARY
                WidgetColorType.TERTIARY -> WidgetColorType.SURFACE
            }
            state.copy(colorType = nextColor)
        }
    }

    fun updateWidgetSize(id: String, width: Int, height: Int) {
        updateWidgetState(id) { it.copy(width = width, height = height) }
    }

    fun moveWidget(fromId: String, toId: String) {
        val currentWidgets = _modernDashboardConfig.value.widgets.toMutableList()
        val fromIndex = currentWidgets.indexOfFirst { it.id == fromId }
        val toIndex = currentWidgets.indexOfFirst { it.id == toId }
        
        if (fromIndex != -1 && toIndex != -1) {
            val item = currentWidgets.removeAt(fromIndex)
            currentWidgets.add(toIndex, item)
            
            val updatedWidgets = currentWidgets.mapIndexed { index, widget ->
                widget.copy(position = index)
            }
            updateModernDashboardConfig(ModernDashboardConfig(updatedWidgets))
        }
    }

    fun updateDashboardConfig(newConfig: DashboardConfig) {
        _dashboardConfig.value = newConfig
        authStorage.saveDashboardConfig(newConfig)
    }

    fun logout() {
        authStorage.clear()
        _appState.value = AppState.Login
        currentModel?.let { LlmChatModelHelper.cleanUp(it) {} }
        isLlmReady = false
        chatMessages.clear()
    }
}
