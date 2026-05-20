package com.socialapp.ui.settings

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.socialapp.data.repository.SocialRepository
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = SocialRepository()
    private val prefs = application.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    var isDarkMode by mutableStateOf(prefs.getBoolean("dark_mode", false))
        private set
    var privateAccount by mutableStateOf(prefs.getBoolean("private_account", false))
        private set
    var isPremium by mutableStateOf(false)
        private set
    var isAdmin by mutableStateOf(false)
        private set

    var language by mutableStateOf(prefs.getString("language", "vi") ?: "vi")
        private set

    init { loadUserSettings() }

    private fun loadUserSettings() {
        viewModelScope.launch {
            val uid = repo.currentUid ?: return@launch
            val user = repo.getUser(uid)
            isPremium = user?.is_premium ?: false
            isAdmin = user?.role == "admin"
            privateAccount = user?.is_private ?: false
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        isDarkMode = enabled
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun togglePrivateAccount(enabled: Boolean) {
        viewModelScope.launch {
            repo.updatePrivateStatus(enabled).onSuccess { privateAccount = enabled }
        }
    }

    fun togglePremium(enabled: Boolean) {
        viewModelScope.launch {
            repo.updatePremiumStatus(enabled).onSuccess { isPremium = enabled }
        }
    }

    fun setAppLanguage(lang: String) {
        language = lang
        prefs.edit().putString("language", lang).apply()
    }
}