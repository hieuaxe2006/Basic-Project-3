package com.socialapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.socialapp.utils.t

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToPremium: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("settings"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(t("Basic setting"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            // Dark Mode
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(t("Dark mode"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = viewModel.isDarkMode, onCheckedChange = { viewModel.toggleDarkMode(it) })
            }

            // Language Selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLanguageDialog = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(t("language"), style = MaterialTheme.typography.bodyLarge)
                    Text(if (viewModel.language == "en") t("english") else t("vietnamese"), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Icon(Icons.Default.Language, contentDescription = "Language", tint = MaterialTheme.colorScheme.primary)
            }

            // Private Account
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(t("Private account"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = viewModel.privateAccount, onCheckedChange = { viewModel.togglePrivateAccount(it) })
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToPremium() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (viewModel.isPremium) Color(0xFFD4AF37) else Color(0xFF4E2319)
                )
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (viewModel.isPremium) Icons.Default.Verified else Icons.Default.WorkspacePremium,
                        contentDescription = "Premium",
                        tint = if (viewModel.isPremium) Color.White else Color(0xFFFFD700),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (viewModel.isPremium) "GymHub Premium Active" else "GymHub Premium",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (viewModel.isPremium) "GOLD Active!" else "Unleash your potential!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    if (viewModel.isPremium) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color.White)
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(onDismissRequest = { showLanguageDialog = false }, title = { Text(t("change_language")) }, text = {
            Column {
                Row(Modifier.fillMaxWidth().clickable { viewModel.setAppLanguage("vi"); showLanguageDialog = false }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(viewModel.language == "vi", { viewModel.setAppLanguage("vi"); showLanguageDialog = false }); Spacer(Modifier.width(8.dp)); Text(t("vietnamese")) }
                Row(Modifier.fillMaxWidth().clickable { viewModel.setAppLanguage("en"); showLanguageDialog = false }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(viewModel.language == "en", { viewModel.setAppLanguage("en"); showLanguageDialog = false }); Spacer(Modifier.width(8.dp)); Text(t("english")) }
            }
        }, confirmButton = {}, dismissButton = { TextButton({ showLanguageDialog = false }) { Text(t("cancel")) } })
    }
}