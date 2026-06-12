package com.socialapp.ui.settings

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.socialapp.data.repository.SocialRepository
import com.socialapp.data.repository.UserRepository
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = SocialRepository()
    private val userRepo = UserRepository()
    private val prefs = application.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    var isDarkMode by mutableStateOf(prefs.getBoolean("dark_mode", false))
        private set

    var privateAccount by mutableStateOf(prefs.getBoolean("private_account", false))
        private set

    var language by mutableStateOf(prefs.getString("language", "vi") ?: "vi")
        private set

    var isPremium by mutableStateOf(false)
        private set

    init {
        observeUserSettings()
    }

    private fun observeUserSettings() {
        val uid = userRepo.currentUid ?: return
        viewModelScope.launch {
            userRepo.getUserSnapshot(uid).collect { user ->
                isPremium = user.is_premium
                privateAccount = user.is_private
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        isDarkMode = enabled
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun togglePrivateAccount(enabled: Boolean) {
        viewModelScope.launch {
            repo.updatePrivateStatus(enabled).onSuccess {
                privateAccount = enabled
            }
        }
    }

    fun setAppLanguage(lang: String) {
        language = lang
        prefs.edit().putString("language", lang).apply()
        // Lưu ý: MainActivity sẽ lắng nghe sự thay đổi này qua SharedPreferences
    }
}