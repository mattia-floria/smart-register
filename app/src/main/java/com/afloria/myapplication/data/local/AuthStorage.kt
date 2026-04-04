package com.afloria.smartregister.data.local

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.afloria.smartregister.data.remote.model.TimetableData
import com.afloria.smartregister.ui.theme.ThemeMode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AuthStorage(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
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

    fun saveThemeSettings(mode: ThemeMode, seed: Color?, secondary: Color?, tertiary: Color?) {
        sharedPreferences.edit().apply {
            putString("theme_mode", mode.name)
            if (seed != null) putInt("theme_seed", seed.toArgb()) else remove("theme_seed")
            if (secondary != null) putInt("theme_secondary", secondary.toArgb()) else remove("theme_secondary")
            if (tertiary != null) putInt("theme_tertiary", tertiary.toArgb()) else remove("theme_tertiary")
        }.apply()
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
        
        return ThemeSettings(mode, seed, secondary, tertiary)
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
            .apply()
    }
}

data class ThemeSettings(
    val mode: ThemeMode,
    val seed: Color?,
    val secondary: Color?,
    val tertiary: Color?
)
