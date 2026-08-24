package com.afloria.smartregister.data.local

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.afloria.smartregister.data.remote.model.*
import com.afloria.smartregister.ui.theme.ThemeMode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AuthStorage(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = try {
        createEncryptedPrefs()
    } catch (e: Exception) {
        // Fallback for corrupted Keystore/Prefs
        context.deleteSharedPreferences("auth_prefs")
        createEncryptedPrefs()
    }

    private fun createEncryptedPrefs() = EncryptedSharedPreferences.create(
        context,
        "auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCredentials(ident: String, pass: String) {
        sharedPreferences.edit()
            .putString("ident", ident)
            .putString("pass", pass)
            .apply()
    }

    fun getCredentials(): Pair<String?, String?> {
        val ident = sharedPreferences.getString("ident", null)
        val pass = sharedPreferences.getString("pass", null)
        return Pair(ident, pass)
    }

    fun isFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean("is_first_launch", true)
    }

    fun setFirstLaunchCompleted() {
        sharedPreferences.edit().putBoolean("is_first_launch", false).apply()
    }

    fun saveThemeSettings(mode: ThemeMode, seed: Color?, secondary: Color?, tertiary: Color?, fontFamily: String = "DEFAULT", fontWeight: Float = 400f, fontWidth: Float = 100f, fontOpsz: Float = 14f, fontGrad: Float = 0f, fontRond: Float = 0f) {
        sharedPreferences.edit().apply {
            putString("theme_mode", mode.name)
            putString("theme_font", fontFamily)
            putFloat("theme_font_weight", fontWeight)
            putFloat("theme_font_width", fontWidth)
            putFloat("theme_font_opsz", fontOpsz)
            putFloat("theme_font_grad", fontGrad)
            putFloat("theme_font_rond", fontRond)
            if (seed != null) putInt("theme_seed", seed.toArgb()) else remove("theme_seed")
            if (secondary != null) putInt("theme_secondary", secondary.toArgb()) else remove("theme_secondary")
            if (tertiary != null) putInt("theme_tertiary", tertiary.toArgb()) else remove("theme_tertiary")
        }.apply()
    }

    fun saveAiModel(model: String) {
        sharedPreferences.edit().putString("selected_ai_model", model).apply()
    }

    fun getAiModel(): String {
        return sharedPreferences.getString("selected_ai_model", null) ?: "gemma-3-1b-it"
    }

    fun isChatEnabled(): Boolean {
        return sharedPreferences.getBoolean("chat_enabled", true)
    }

    fun setChatEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("chat_enabled", enabled).apply()
    }

    fun isExperimentalEnabled(): Boolean {
        return sharedPreferences.getBoolean("experimental_enabled", false)
    }

    fun setExperimentalEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("experimental_enabled", enabled).apply()
    }

    fun isAiBriefEnabled(): Boolean {
        return sharedPreferences.getBoolean("ai_brief_enabled", false)
    }

    fun setAiBriefEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("ai_brief_enabled", enabled).apply()
    }

    fun saveAiBriefSummary(summary: String) {
        sharedPreferences.edit().putString("ai_brief_summary", summary).apply()
    }

    fun getAiBriefSummary(): String? {
        return sharedPreferences.getString("ai_brief_summary", null)
    }

    fun getThemeSettings(): ThemeSettings {
        val mode = try {
            ThemeMode.valueOf(sharedPreferences.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
        
        val seed = if (sharedPreferences.contains("theme_seed")) Color(sharedPreferences.getInt("theme_seed", 0)) else null
        val secondary = if (sharedPreferences.contains("theme_secondary")) Color(sharedPreferences.getInt("theme_secondary", 0)) else null
        val tertiary = if (sharedPreferences.contains("theme_tertiary")) Color(sharedPreferences.getInt("theme_tertiary", 0)) else null
        val fontFamily = sharedPreferences.getString("theme_font", "DEFAULT") ?: "DEFAULT"
        val fontWeight = sharedPreferences.getFloat("theme_font_weight", 400f)
        val fontWidth = sharedPreferences.getFloat("theme_font_width", 100f)
        val fontOpsz = sharedPreferences.getFloat("theme_font_opsz", 14f)
        val fontGrad = sharedPreferences.getFloat("theme_font_grad", 0f)
        val fontRond = sharedPreferences.getFloat("theme_font_rond", 0f)
        
        return ThemeSettings(mode, seed, secondary, tertiary, fontFamily, fontWeight, fontWidth, fontOpsz, fontGrad, fontRond)
    }

    fun saveTimetable(data: TimetableData) {
        val jsonString = Json.encodeToString(data)
        sharedPreferences.edit().putString("timetable_data", jsonString).apply()
    }

    fun getTimetable(): TimetableData {
        val jsonString = sharedPreferences.getString("timetable_data", null)
        return if (jsonString != null) {
            try {
                Json.decodeFromString<TimetableData>(jsonString)
            } catch (e: Exception) {
                TimetableData()
            }
        } else {
            TimetableData()
        }
    }

    fun clear() {
        sharedPreferences.edit()
            .remove("ident")
            .remove("pass")
            .remove("cached_login")
            .remove("cached_grades")
            .remove("cached_notes")
            .remove("cached_agenda")
            .remove("cached_notices")
            .remove("cached_materials")
            .remove("cached_absences")
            .remove("cached_final_grades")
            .remove("last_update_ts")
            .apply()
    }

    fun saveLoginResponse(response: LoginResponse) {
        sharedPreferences.edit().putString("cached_login", json.encodeToString(response)).apply()
    }

    fun getLoginResponse(): LoginResponse? {
        val s = sharedPreferences.getString("cached_login", null) ?: return null
        return try { json.decodeFromString<LoginResponse>(s) } catch (e: Exception) { null }
    }

    fun saveGrades(data: List<GradeRemoteModel>) {
        sharedPreferences.edit().putString("cached_grades", json.encodeToString(data)).apply()
    }

    fun getGrades(): List<GradeRemoteModel> {
        val s = sharedPreferences.getString("cached_grades", null) ?: return emptyList()
        return try { json.decodeFromString<List<GradeRemoteModel>>(s) } catch (e: Exception) { emptyList() }
    }

    fun saveNotes(data: NotesResponse) {
        sharedPreferences.edit().putString("cached_notes", json.encodeToString(data)).apply()
    }

    fun getNotes(): NotesResponse? {
        val s = sharedPreferences.getString("cached_notes", null) ?: return null
        return try { json.decodeFromString<NotesResponse>(s) } catch (e: Exception) { null }
    }

    fun saveAgenda(data: List<AgendaEventRemoteModel>) {
        sharedPreferences.edit().putString("cached_agenda", json.encodeToString(data)).apply()
    }

    fun getAgenda(): List<AgendaEventRemoteModel> {
        val s = sharedPreferences.getString("cached_agenda", null) ?: return emptyList()
        return try { json.decodeFromString<List<AgendaEventRemoteModel>>(s) } catch (e: Exception) { emptyList() }
    }

    fun saveNotices(data: List<NoticeRemoteModel>) {
        sharedPreferences.edit().putString("cached_notices", json.encodeToString(data)).apply()
    }

    fun getNotices(): List<NoticeRemoteModel> {
        val s = sharedPreferences.getString("cached_notices", null) ?: return emptyList()
        return try { json.decodeFromString<List<NoticeRemoteModel>>(s) } catch (e: Exception) { emptyList() }
    }

    fun saveMaterials(data: List<TeacherRemoteModel>) {
        sharedPreferences.edit().putString("cached_materials", json.encodeToString(data)).apply()
    }

    fun getMaterials(): List<TeacherRemoteModel> {
        val s = sharedPreferences.getString("cached_materials", null) ?: return emptyList()
        return try { json.decodeFromString<List<TeacherRemoteModel>>(s) } catch (e: Exception) { emptyList() }
    }

    fun saveAbsences(data: List<AbsenceRemoteModel>) {
        sharedPreferences.edit().putString("cached_absences", json.encodeToString(data)).apply()
    }

    fun getAbsences(): List<AbsenceRemoteModel> {
        val s = sharedPreferences.getString("cached_absences", null) ?: return emptyList()
        return try { json.decodeFromString<List<AbsenceRemoteModel>>(s) } catch (e: Exception) { emptyList() }
    }

    fun saveFinalGrades(data: List<SchoolReportRemoteModel>) {
        sharedPreferences.edit().putString("cached_final_grades", json.encodeToString(data)).apply()
    }

    fun getFinalGrades(): List<SchoolReportRemoteModel> {
        val s = sharedPreferences.getString("cached_final_grades", null) ?: return emptyList()
        return try { json.decodeFromString<List<SchoolReportRemoteModel>>(s) } catch (e: Exception) { emptyList() }
    }

    fun saveLastUpdateTimestamp(ts: Long) {
        sharedPreferences.edit().putLong("last_update_ts", ts).apply()
    }

    fun getLastUpdateTimestamp(): Long {
        return sharedPreferences.getLong("last_update_ts", 0L)
    }

    fun saveModernDashboardConfig(config: ModernDashboardConfig) {
        sharedPreferences.edit().putString("modern_dashboard_config", json.encodeToString(config)).apply()
    }

    fun getModernDashboardConfig(): ModernDashboardConfig? {
        val s = sharedPreferences.getString("modern_dashboard_config", null) ?: return null
        return try { json.decodeFromString<ModernDashboardConfig>(s) } catch (e: Exception) { null }
    }

    fun saveDashboardConfig(config: DashboardConfig) {
        sharedPreferences.edit().putString("dashboard_config", json.encodeToString(config)).apply()
    }

    fun getDashboardConfig(): DashboardConfig? {
        val s = sharedPreferences.getString("dashboard_config", null) ?: return null
        return try { json.decodeFromString<DashboardConfig>(s) } catch (e: Exception) { null }
    }
}

@kotlinx.serialization.Serializable
enum class WidgetType {
    AI_BRIEF, RECOVERY_STATUS, WEEKLY_CHART, TOMORROW_AGENDA, COUNTDOWN,
    GRADES_SUMMARY, ABSENCES_COUNT, NOTES_PREVIEW
}

@kotlinx.serialization.Serializable
data class DashboardWidget(
    val type: WidgetType,
    val isVisible: Boolean = true,
    val isFullWidth: Boolean = true
)

@kotlinx.serialization.Serializable
data class DashboardConfig(
    val widgets: List<DashboardWidget>
)

data class ThemeSettings(
    val mode: ThemeMode,
    val seed: Color?,
    val secondary: Color?,
    val tertiary: Color?,
    val fontFamily: String = "DEFAULT",
    val fontWeight: Float = 400f,
    val fontWidth: Float = 100f,
    val fontOpsz: Float = 14f,
    val fontGrad: Float = 0f,
    val fontRond: Float = 0f
)
