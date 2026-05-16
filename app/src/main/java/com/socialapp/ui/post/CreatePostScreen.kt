package com.socialapp.ui.post

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(
    viewModel: CreatePostViewModel,
    onBack: () -> Unit,
    onPostCreated: () -> Unit
) {
    var content by remember { mutableStateOf("") }
    var showCodeInput by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }
    var codeInput by remember { mutableStateOf("") }
    var selectedLang by remember { mutableStateOf("Kotlin") }

    val state = viewModel.state
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val domains = listOf("Code", "Life", "Study", "Animal", "Food", "Exercise", "Music", "Travel")

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        viewModel.setImageUri(uri)
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) { viewModel.reset(); onPostCreated() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tạo bài viết", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    Button(onClick = { viewModel.createPost(content, context) }, enabled = !state.isLoading) {
                        Text("Đăng")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.weight(1f).verticalScroll(scrollState).padding(16.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(contentAlignment = Alignment.Center) { Text("U") }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Người dùng", fontWeight = FontWeight.Bold)
                            if (state.selectedTaggedUsers.isNotEmpty()) {
                                Text(" cùng với ", fontSize = 12.sp)
                                Text("${state.selectedTaggedUsers[0].username}${if(state.selectedTaggedUsers.size > 1) " và ${state.selectedTaggedUsers.size-1} người khác" else ""}", fontWeight = FontWeight.Bold, color = Color(0xFF1877F2), fontSize = 12.sp)
                            }
                        }
                        Text("Công khai", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Spacer(Modifier.height(16.dp))
                TextField(value = content, onValueChange = { content = it }, placeholder = { Text("Bạn đang nghĩ gì?") }, modifier = Modifier.fillMaxWidth(), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent))

                if (showCodeInput) {
                    Spacer(Modifier.height(16.dp))
                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF1E1E1E)).padding(8.dp)) {
                        Text("Mã nguồn ($selectedLang)", color = Color.Cyan, fontSize = 11.sp)
                        TextField(value = codeInput, onValueChange = { codeInput = it; viewModel.updateCode(it, selectedLang) }, modifier = Modifier.fillMaxWidth().height(150.dp), textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = Color.White), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent))
                    }
                }

                state.imageUri?.let { AsyncImage(model = it, contentDescription = null, modifier = Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(8.dp))) }

                Spacer(Modifier.height(16.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    domains.forEach { tag -> FilterChip(selected = tag in state.selectedTags, onClick = { viewModel.toggleTag(tag) }, label = { Text(tag) }) }
                }
            }

            Surface(tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Thêm vào bài viết của bạn", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        IconButton(onClick = { imagePicker.launch("image/*") }) { Icon(Icons.Outlined.Image, null, tint = Color(0xFF45BD62)) }
                        IconButton(onClick = { showCodeInput = !showCodeInput }) { Icon(Icons.Default.Code, null, tint = if (showCodeInput) Color(0xFF1877F2) else Color(0xFFF7B928)) }
                        IconButton(onClick = { viewModel.loadFriends(); showTagSheet = true }) { Icon(Icons.Outlined.PersonAdd, null, tint = Color(0xFF1877F2)) }
                    }
                }
            }
        }
    }

    if (showTagSheet) {
        ModalBottomSheet(onDismissRequest = { showTagSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 400.dp)) {
                Text("Gắn thẻ bạn bè", fontWeight = FontWeight.Bold)
                LazyColumn {
                    items(state.friends) { friend ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleTagUser(friend) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = state.selectedTaggedUsers.any { it.id == friend.id }, onCheckedChange = { viewModel.toggleTagUser(friend) })
                            Text(friend.username)
                        }
                    }
                }
                Button(onClick = { showTagSheet = false }, modifier = Modifier.fillMaxWidth()) { Text("Xong") }
            }
        }
    }
}