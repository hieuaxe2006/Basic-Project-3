package com.socialapp.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.socialapp.utils.t
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit,
    vm: AdminViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    // Đồng bộ các tab
    val tabs = listOf("Stats", "Users", "Posts")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (vm.state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(modifier = Modifier.padding(padding)) {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                    }
                }
                when (selectedTab) {
                    0 -> StatsTab(vm.state)
                    1 -> UsersTab(vm.state, onToggleBlock = { uid, status -> vm.toggleBlockUser(uid, status) })
                    2 -> PostsTab(vm.state, onDelete = { vm.deletePost(it) }, onApprove = { vm.approvePost(it) }, onReject = { vm.rejectPost(it) })
                }
            }
        }
    }
}

@Composable
fun StatsTab(state: AdminState) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    val formattedRevenue = formatter.format(state.revenue)

    Column(modifier = Modifier.padding(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Revenue (Premium)", style = MaterialTheme.typography.titleMedium)
                Text(formattedRevenue, style = MaterialTheme.typography.headlineMedium, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            }
        }
        Text("Follower Leaderboard", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        state.topUsers.forEach { user ->
            ListItem(headlineContent = { Text(user.username) }, trailingContent = { Text("${user.followers_count} fl", style = MaterialTheme.typography.bodySmall) })
        }
    }
}

@Composable
fun UsersTab(state: AdminState, onToggleBlock: (String, Boolean) -> Unit) {
    LazyColumn {
        items(state.users) { user ->
            ListItem(
                headlineContent = { Text(user.username, color = if(user.is_blocked) Color.Gray else Color.Unspecified) },
                supportingContent = { Text(user.email) },
                trailingContent = {
                    Button(onClick = { onToggleBlock(user.id, user.is_blocked) }, colors = ButtonDefaults.buttonColors(containerColor = if (user.is_blocked) Color(0xFF4CAF50) else Color(0xFFD32F2F))) {
                        Icon(if (user.is_blocked) Icons.Default.CheckCircle else Icons.Default.Block, null, Modifier.size(16.dp))
                        Text(if (user.is_blocked) " Unblock" else " Block")
                    }
                }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun PostsTab(
    state: AdminState,
    onDelete: (String) -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    LazyColumn {
        items(state.posts) { post ->
            val statusColor = when (post.status) {
                "approved" -> Color(0xFF2E7D32)
                "rejected" -> Color(0xFFC62828)
                else -> Color(0xFFEF6C00)
            }
            val statusText = when (post.status) {
                "approved" -> "Approved"
                "rejected" -> "Rejected"
                else -> "Pending"
            }
            ListItem(
                headlineContent = { Text(post.content, maxLines = 2) },
                supportingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Author: ${post.user_id.take(8)}")
                        Surface(
                            color = statusColor.copy(alpha = 0.1f),
                            contentColor = statusColor,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                statusText,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (post.status == "pending") {
                            IconButton(onClick = { onApprove(post.id) }) {
                                Icon(Icons.Default.CheckCircle, tint = Color(0xFF2E7D32), contentDescription = "Approve")
                            }
                            IconButton(onClick = { onReject(post.id) }) {
                                Icon(Icons.Default.Block, tint = Color(0xFFC62828), contentDescription = "Reject")
                            }
                        } else if (post.status == "approved") {
                            IconButton(onClick = { onReject(post.id) }) {
                                Icon(Icons.Default.Block, tint = Color(0xFFC62828), contentDescription = "Reject")
                            }
                        }
                        IconButton(onClick = { onDelete(post.id) }) {
                            Icon(Icons.Default.Delete, tint = Color.Red, contentDescription = "Delete")
                        }
                    }
                }
            )
            HorizontalDivider()
        }
    }
}