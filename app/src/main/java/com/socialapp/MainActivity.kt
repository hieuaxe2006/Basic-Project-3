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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return isDark
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDark = rememberDarkModePreference()
            val context = LocalContext.current

            // Xin quyền cho Android 13+
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                // Xử lý nếu cần
            }

            LaunchedEffect(Unit) {
                // Kiểm tra và xin quyền POST_NOTIFICATIONS
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                // Lấy FCM Token và cập nhật lên Firestore (chỉ khi đã đăng nhập)
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

            GymHubTheme(darkTheme = isDark) {
                AppNavigation()
            }
        }
    }
}