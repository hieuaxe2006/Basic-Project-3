package com.socialapp.ui.chat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
    }

    LaunchedEffect(chatInfoViewModel.message) {
        chatInfoViewModel.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            chatInfoViewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
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
            Spacer(Modifier.height(20.dp))
            
            // Avatar
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(name.take(1).uppercase(), fontSize = 40.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            
            Spacer(Modifier.height(24.dp))
            
            // Action Buttons (Facebook style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoActionButton(icon = Icons.Default.Person, label = "Trang cá nhân") { onViewProfile(uid) }
                InfoActionButton(
                    icon = if (chatInfoViewModel.isMuted) Icons.Default.NotificationsOff else Icons.Default.Notifications, 
                    label = if (chatInfoViewModel.isMuted) "Đã tắt" else "Tắt thông báo"
                ) { 
                    chatInfoViewModel.muteUser(uid) 
                }
                InfoActionButton(icon = Icons.Default.Search, label = "Tìm kiếm") { 
                    Toast.makeText(context, "Chức năng tìm kiếm tin nhắn đang được phát triển", Toast.LENGTH_SHORT).show()
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            // Options List
            Column(modifier = Modifier.fillMaxWidth()) {
                InfoItem(icon = Icons.Default.Image, text = "Xem file phương tiện, file và liên kết") {
                    Toast.makeText(context, "Chức năng xem file đang được phát triển", Toast.LENGTH_SHORT).show()
                }
                InfoItem(icon = Icons.Default.PushPin, text = "Xem tin nhắn đã ghim") {
                    Toast.makeText(context, "Chức năng tin nhắn ghim đang được phát triển", Toast.LENGTH_SHORT).show()
                }
                InfoItem(icon = Icons.Default.Block, text = if (chatInfoViewModel.isBlocked) "Đã chặn" else "Chặn", textColor = Color.Red) {
                    chatInfoViewModel.blockUser(uid)
                }
                InfoItem(icon = Icons.Default.Report, text = "Báo cáo", textColor = Color.Red) {
                    showReportDialog = true
                }
            }
        }
    }

    if (showReportDialog) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Báo cáo người dùng") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    placeholder = { Text("Lý do báo cáo...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (reason.isNotBlank()) {
                        chatInfoViewModel.reportUser(uid, reason)
                        showReportDialog = false
                    }
                }) {
                    Text("Gửi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun InfoActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.width(80.dp)
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun InfoItem(
    icon: ImageVector, 
    text: String, 
    textColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, 
            null, 
            modifier = Modifier.size(24.dp), 
            tint = if (textColor != Color.Unspecified) textColor else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = textColor)
    }
}
