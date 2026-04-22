package com.socialapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.socialapp.navigation.AppNavigation
import com.socialapp.ui.theme.SocialAppTheme

@Composable
fun rememberDarkModePreference(): Boolean {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
    val systemTheme = isSystemInDarkTheme()
    var isDark by remember { mutableStateOf(prefs.getBoolean("dark_mode", systemTheme)) }
    
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "dark_mode") {
                isDark = sharedPreferences.getBoolean("dark_mode", systemTheme)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    return isDark
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDark = rememberDarkModePreference()
            SocialAppTheme(darkTheme = isDark) {
                AppNavigation()
            }
        }
    }
}
