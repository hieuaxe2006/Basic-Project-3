package com.socialapp.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Basic Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Dark Mode", modifier = Modifier.weight(1f))
                Switch(checked = viewModel.isDarkMode, onCheckedChange = { viewModel.toggleDarkMode(it) })
            }
            HorizontalDivider()

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Premium Account", modifier = Modifier.weight(1f))
                Switch(checked = viewModel.isPremium, onCheckedChange = {
                    viewModel.togglePremium(it)
                })
            }
            HorizontalDivider()

            // PHÂN QUYỀN: Chỉ Admin mới thấy mục này
            if (viewModel.isAdmin) {
                Text("Admin Tools", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAdmin() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Admin Dashboard", modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
                HorizontalDivider()
            }

            Text("User Settings", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp, bottom = 16.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Private Account", modifier = Modifier.weight(1f))
                Switch(checked = viewModel.privateAccount, onCheckedChange = { viewModel.togglePrivateAccount(it) })
            }
            HorizontalDivider()
        }
    }
}