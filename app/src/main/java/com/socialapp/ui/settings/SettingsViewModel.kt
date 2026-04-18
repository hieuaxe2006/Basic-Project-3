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

    init {
        loadPremiumStatus()
    }

    private fun loadPremiumStatus() {
        viewModelScope.launch {
            val uid = repo.currentUid ?: return@launch
            val user = repo.getUser(uid)
            isPremium = user?.is_premium ?: false
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        isDarkMode = enabled
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun togglePrivateAccount(enabled: Boolean) {
        privateAccount = enabled
        prefs.edit().putBoolean("private_account", enabled).apply()
    }

    fun togglePremium(enabled: Boolean) {
        viewModelScope.launch {
            val originalState = isPremium
            isPremium = enabled // optimistic update
            repo.updatePremiumStatus(enabled).onFailure {
                isPremium = originalState // revert on failure
            }
        }
    }
}
