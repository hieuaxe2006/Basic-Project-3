package com.socialapp.ui.chat

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.socialapp.data.model.User
import com.socialapp.data.repository.UserRepository
import com.socialapp.utils.t
import kotlinx.coroutines.flow.firstOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInfoScreen(
    uid: String,
    name: String,
    onBack: () -> Unit,
    onViewProfile: (String) -> Unit,
    chatInfoViewModel: ChatInfoViewModel = viewModel()
) {
    var user by remember { mutableStateOf<User?>(null) }
    val repo = UserRepository()
    val context = LocalContext.current
    var showReportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        user = repo.getUserSnapshot(uid).firstOrNull()
        chatInfoViewModel.checkBlockStatus(uid)
    }

    LaunchedEffect(chatInfoViewModel.message) {
        chatInfoViewModel.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            chatInfoViewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(t("conversation_info"), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(name.take(1).uppercase(), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = Color(0xFF4CAF50),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                ) {}
            }

            Spacer(Modifier.height(20.dp))

            Text(name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("@${user?.username ?: ""}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(40.dp))

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    InfoItem(
                        icon = Icons.Default.AccountCircle,
                        text = t("view_profile"),
                        description = "..."
                    ) { onViewProfile(uid) }

                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    InfoItem(
                        icon = if (chatInfoViewModel.isMuted) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                        text = t("notifications"),
                        description = if (chatInfoViewModel.isMuted) "Muted" else "Enabled"
                    ) {
                        chatInfoViewModel.muteUser(uid)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                t("privacy_support"),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    InfoItem(
                        icon = Icons.Default.Block,
                        text = if (chatInfoViewModel.isBlocked) t("unblock_user") else t("block_user"),
                        textColor = Color.Red,
                        description = "..."
                    ) {
                        chatInfoViewModel.toggleBlockUser(uid)
                    }

                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    InfoItem(
                        icon = Icons.Default.Report,
                        text = t("report_user"),
                        textColor = Color.Red,
                        description = "..."
                    ) {
                        showReportDialog = true
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showReportDialog) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text(t("report_user")) },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    placeholder = { Text(t("search_hint")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reason.isNotBlank()) {
                            chatInfoViewModel.reportUser(uid, reason)
                            showReportDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(t("share")) // Hoặc nút riêng cho Báo cáo
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text(t("cancel"))
                }
            }
        )
    }
}

@Composable
private fun InfoItem(icon: ImageVector, text: String, description: String? = null, textColor: Color = Color.Unspecified, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = if (textColor == Color.Red) Color.Red.copy(alpha = 0.1f) else MaterialTheme.colorScheme.secondaryContainer) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, modifier = Modifier.size(22.dp), tint = if (textColor != Color.Unspecified) textColor else MaterialTheme.colorScheme.onSecondaryContainer) }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = text, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = if (textColor != Color.Unspecified) textColor else MaterialTheme.colorScheme.onSurface)
            if (description != null) { Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
    }
}