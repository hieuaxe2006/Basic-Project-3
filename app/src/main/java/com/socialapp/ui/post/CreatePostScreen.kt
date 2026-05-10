package com.socialapp.ui.post

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.socialapp.data.model.User

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(
    viewModel: CreatePostViewModel,
    onBack: () -> Unit,
    onPostCreated: () -> Unit
) {
    var content by remember { mutableStateOf("") }
    val state = viewModel.state
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val domains = listOf("Code", "Life", "Study", "Animal", "Food", "Exercise", "Music", "Travel")
    val colors = listOf(
        "#FFFFFF", "#F44336", "#E91E63", "#9C27B0", "#673AB7", 
        "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4", "#009688", 
        "#4CAF50", "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107"
    )

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setImageUri(uri)
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            viewModel.reset()
            onPostCreated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tạo bài viết", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.createPost(content, context) },
                        enabled = !state.isLoading && (content.isNotBlank() || state.imageUri != null),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text("Đăng", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                // User Info Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("U", fontWeight = FontWeight.Bold) // Simplified placeholder
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Người dùng", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Public, null, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Công khai", style = MaterialTheme.typography.labelSmall)
                                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Content Input Area
                val backgroundColor = if (state.backgroundColor.isNotBlank() && state.backgroundColor != "#FFFFFF") 
                    Color(android.graphics.Color.parseColor(state.backgroundColor)) 
                else MaterialTheme.colorScheme.surface
                
                val onBackgroundColor = if (state.backgroundColor.isNotBlank() && state.backgroundColor != "#FFFFFF") 
                    Color.White 
                else MaterialTheme.colorScheme.onSurface

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(backgroundColor),
                    contentAlignment = if (backgroundColor != MaterialTheme.colorScheme.surface) Alignment.Center else Alignment.TopStart
                ) {
                    TextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = { 
                            Text(
                                "Bạn đang nghĩ gì?", 
                                fontSize = if (backgroundColor != MaterialTheme.colorScheme.surface) 24.sp else 18.sp,
                                color = onBackgroundColor.copy(alpha = 0.6f)
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = onBackgroundColor,
                            unfocusedTextColor = onBackgroundColor,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = if (backgroundColor != MaterialTheme.colorScheme.surface) 24.sp else 18.sp,
                            textAlign = if (backgroundColor != MaterialTheme.colorScheme.surface) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start
                        )
                    )
                }

                // Domain Selection
                if (backgroundColor == MaterialTheme.colorScheme.surface) {
                    Spacer(Modifier.height(24.dp))
                    Text("Chọn chủ đề (Bắt buộc)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        domains.forEach { tag ->
                            FilterChip(
                                selected = tag in state.selectedTags,
                                onClick = { viewModel.toggleTag(tag) },
                                label = { Text(tag) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                // Image Preview
                state.imageUri?.let { uri ->
                    Spacer(Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Selected image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { viewModel.removeImage() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, "Gỡ bỏ", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                
                // Theme/Color selection row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewModel.setBackgroundColor("#FFFFFF") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Palette, null, modifier = Modifier.size(20.dp), tint = Color.Gray)
                        }
                    }
                    items(colors.filter { it != "#FFFFFF" }) { hex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .border(
                                    width = if (state.backgroundColor == hex) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable { viewModel.setBackgroundColor(hex) }
                        )
                    }
                }
            }

            // Bottom Toolbar "Add to post"
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Thêm vào bài viết của bạn", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { imagePicker.launch("image/*") }) {
                            Icon(Icons.Outlined.Image, null, tint = Color(0xFF45BD62))
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Outlined.PersonAdd, null, tint = Color(0xFF1877F2))
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Outlined.Mood, null, tint = Color(0xFFF7B928))
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Outlined.LocationOn, null, tint = Color(0xFFF5533D))
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.MoreHoriz, null, tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
