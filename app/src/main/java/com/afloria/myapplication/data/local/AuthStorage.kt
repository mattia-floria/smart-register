package com.afloria.myapplication.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

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

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}
