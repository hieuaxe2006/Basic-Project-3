package com.socialapp

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.socialapp.data.repository.UserRepository
import com.socialapp.navigation.AppNavigation
import com.socialapp.ui.theme.GymHubTheme
import com.socialapp.utils.LocalLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun rememberAppPreferences(): Pair<Boolean, String> {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
    val systemTheme = isSystemInDarkTheme()

    var isDark by remember { mutableStateOf(prefs.getBoolean("dark_mode", systemTheme)) }
    var language by remember { mutableStateOf(prefs.getString("language", "vi") ?: "vi") }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            when (key) {
                "dark_mode" -> isDark = p.getBoolean("dark_mode", systemTheme)
                "language" -> language = p.getString("language", "vi") ?: "vi"
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return Pair(isDark, language)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val (isDark, currentLang) = rememberAppPreferences()
            val context = LocalContext.current

            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = androidx.activity.SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { isDark },
                    navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { isDark }
                )
                onDispose {}
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { _ -> }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val repo = UserRepository()
                if (repo.currentUid != null) {
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val token = task.result
                            CoroutineScope(Dispatchers.IO).launch {
                                repo.updateFcmToken(token)
                            }
                        }
                    }
                }
            }

            // BAO BỌC TOÀN BỘ APP TRONG PROVIDER NGÔN NGỮ
            CompositionLocalProvider(LocalLanguage provides currentLang) {
                GymHubTheme(darkTheme = isDark) {
                    AppNavigation()
                }
            }
        }
    }
}