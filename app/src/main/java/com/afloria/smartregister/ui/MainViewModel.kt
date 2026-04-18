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
import com.afloria.smartregister.data.remote.SpaggiariApi
import com.afloria.smartregister.data.remote.model.*
import com.afloria.smartregister.ui.theme.ThemeMode
import com.google.ai.edge.litertlm.Contents
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
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

data class ChatMessage(val text: String, val isUser: Boolean, val image: android.graphics.Bitmap? = null)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val authStorage = AuthStorage(application)
    private val context = application
    private val modelDownloadManager = ModelDownloadManager(application)
    
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    var selectedSeedColor by mutableStateOf<Color?>(null)
    var selectedSecondaryColor by mutableStateOf<Color?>(null)
    var selectedTertiaryColor by mutableStateOf<Color?>(null)

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

    // AI State
    var selectedAiModelName by mutableStateOf(authStorage.getAiModel())
    val currentModel: Model?
        get() = AiModels.ALL_MODELS.find { it.name == selectedAiModelName }

    var isLlmReady by mutableStateOf(false)
    var isLlmInitializing by mutableStateOf(false)
    var isModelDownloading by mutableStateOf(false)
    var modelDownloadProgress by mutableStateOf(0f)
    val chatMessages = mutableStateListOf<ChatMessage>()
    var isChatLoading by mutableStateOf(false)
    var isChatOpen by mutableStateOf(false)
    var isChatEnabled by mutableStateOf(true)
    var isExperimentalEnabled by mutableStateOf(false)
    var isAiBriefEnabled by mutableStateOf(false)

    private var downloadStatusJob: Job? = null

    private var tempPass: String? = null

    init {
        loadThemeSettings()
        loadTimetable()
        checkInitialState()
        initOrDownloadModel()
    }

    fun switchAiModel(modelName: String) {
        selectedAiModelName = modelName
        authStorage.saveAiModel(modelName)
        
        // Reset AI state
        currentModel?.let { LlmChatModelHelper.cleanUp(it) {} }
        isLlmReady = false
        
        initOrDownloadModel()
    }

    private fun initOrDownloadModel() {
        val model = currentModel ?: return
        
        downloadStatusJob?.cancel()
        downloadStatusJob = viewModelScope.launch {
            modelDownloadManager.getDownloadStatus(model).collectLatest { status ->
                when (status.status) {
                    ModelDownloadStatusType.NOT_DOWNLOADED -> {
                        // Do nothing, wait for user to start download or automatically start if needed
                        // Removing auto-download from init to allow re-downloading
                    }
                    ModelDownloadStatusType.IN_PROGRESS -> {
                        isModelDownloading = true
                        isLlmReady = false
                        if (status.totalBytes > 0) {
                            modelDownloadProgress = status.receivedBytes.toFloat() / status.totalBytes
                        }
                    }
                    ModelDownloadStatusType.SUCCEEDED -> {
                        isModelDownloading = false
                        if (!isLlmReady && !isLlmInitializing) {
                            setupLocalLlm(model)
                        }
                    }
                    ModelDownloadStatusType.FAILED -> {
                        isModelDownloading = false
                        Log.e("AI", "Download failed: ${status.errorMessage}")
                    }
                }
            }
        }
    }

    private fun setupLocalLlm(model: Model) {
        viewModelScope.launch(Dispatchers.Default) {
            isLlmInitializing = true
            LlmChatModelHelper.initialize(
                context = context,
                model = model,
                supportImage = model.llmSupportImage,
                supportAudio = model.llmSupportAudio,
                onDone = { error ->
                    viewModelScope.launch(Dispatchers.Main) {
                        isLlmInitializing = false
                        if (error.isEmpty()) {
                            isLlmReady = true
                            Log.d("LLM", "Local model loaded successfully")
                        } else {
                            Log.e("LLM", "Failed to initialize: $error")
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
        authStorage.saveThemeSettings(themeMode, selectedSeedColor, selectedSecondaryColor, selectedTertiaryColor)
    }

    fun updatePalette(seed: Color?, secondary: Color?, tertiary: Color?) {
        selectedSeedColor = seed
        selectedSecondaryColor = secondary
        selectedTertiaryColor = tertiary
        authStorage.saveThemeSettings(themeMode, selectedSeedColor, selectedSecondaryColor, selectedTertiaryColor)
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
            val cleanIdent = if (ident.contains("@")) ident.trim() else ident.trim().uppercase()
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
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun fetchAllData(token: String, ident: String) {
        try {
            val studentId = ident.filter { it.isDigit() }
            val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val rangeCal = Calendar.getInstance()
            rangeCal.set(Calendar.MONTH, Calendar.SEPTEMBER)
            rangeCal.set(Calendar.DAY_OF_MONTH, 1)
            if (rangeCal.after(Calendar.getInstance())) rangeCal.add(Calendar.YEAR, -1)
            val startDate = sdf.format(rangeCal.time)
            rangeCal.add(Calendar.YEAR, 1)
            rangeCal.set(Calendar.MONTH, Calendar.JUNE)
            rangeCal.set(Calendar.DAY_OF_MONTH, 30)
            val endDate = sdf.format(rangeCal.time)

            grades = api.getGrades(token, studentId).grades.sortedByDescending { it.evtDate }
            notes = api.getNotes(token, studentId)
            agenda = api.getAgenda(token, studentId, startDate, endDate).agenda
            notices = api.getNoticeboard(token, studentId).items
            teachersMaterials = api.getDidactics(token, studentId).teachers ?: emptyList()
            absences = api.getAbsences(token, studentId).events.sortedByDescending { it.evtDate }
            
            try {
                finalGrades = api.getDocuments(token, studentId).schoolReports ?: emptyList()
            } catch (e: Exception) {
                Log.e("SCRUTINI_FETCH", "Failed to fetch final grades", e)
            }
            
            if (_timetableData.value.entries.isEmpty()) {
                generateTimetableFromAgenda()
            }
        } catch (e: Exception) {
            Log.e("CV_DATA", "Data fetch failed", e)
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
                        val uri = saveFileWithProgress(fileName, body, "application/pdf")
                        if (uri != null) openFile(uri, "application/pdf")
                    }
                }
            } catch (e: Exception) {
                Log.e("VIEW_REPORT_ERROR", "Failed to download school report", e)
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
                        if (uri != null) openFile(uri, contentType)
                    }
                }
            } catch (e: Exception) {
                Log.e("DOWNLOAD_ERROR", "Failed to download didactic file", e)
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
                        if (uri != null) openFile(uri, contentType)
                    }
                }
            } catch (e: Exception) {
                Log.e("DOWNLOAD_ERROR", "Failed to download notice attachment", e)
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

    private fun openFile(uri: Uri, mimeType: String) {
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
        if (chatMessages.isEmpty() || chatMessages.last().text != text) {
            chatMessages.add(ChatMessage(text, true, image))
        }
        
        val model = currentModel
        if (model != null && isLlmReady) {
            viewModelScope.launch(Dispatchers.Default) {
                isChatLoading = true
                try {
                    val prompt = """
                        <|system|>
                        Sei un assistente virtuale utile e conciso. Rispondi sempre in italiano.
                        <|user|>
                        $text
                        <|assistant|>
                    """.trimIndent()
                    
                    var response = ""
                    var lastHapticTime = 0L
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
                                response += part
                                viewModelScope.launch(Dispatchers.Main) {
                                    if (chatMessages.isNotEmpty() && !chatMessages.last().isUser) {
                                        chatMessages[chatMessages.size - 1] = ChatMessage(response.trim(), false)
                                    } else {
                                        chatMessages.add(ChatMessage(response.trim(), false))
                                    }
                                    
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastHapticTime > 50) { 
                                        // We'll handle haptics in the UI layer if possible, 
                                        // or trigger it here if we had a reference to HapticFeedback.
                                        // Since we don't have it easily here, we'll signal it via a state if needed.
                                        lastHapticTime = currentTime
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

    private fun getNextWeekEvents(): List<AgendaEventRemoteModel> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val events = mutableListOf<AgendaEventRemoteModel>()
        for (i in 1..7) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val dateStr = sdf.format(cal.time)
            events.addAll(agenda.filter { it.evtDatetimeBegin?.startsWith(dateStr) == true })
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
        currentModel?.let { modelDownloadManager.downloadModel(it) }
    }

    fun clearChat() {
        chatMessages.clear()
        currentModel?.let { model ->
            LlmChatModelHelper.resetConversation(model)
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

    fun logout() {
        authStorage.clear()
        _appState.value = AppState.Login
        currentModel?.let { LlmChatModelHelper.cleanUp(it) {} }
        isLlmReady = false
        chatMessages.clear()
    }
}
