package com.socialapp.ui.post

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryScreen(
    onBack: () -> Unit,
    onStoryCreated: () -> Unit,
    viewModel: CreateStoryViewModel = viewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImageSelected(it) }
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onStoryCreated()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tạo tin", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color(0xFF1C1C1E) // Dark background like the image
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top Selection Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StoryOptionButton(
                    icon = Icons.Default.TextFields,
                    label = "Văn bản",
                    gradient = Brush.verticalGradient(listOf(Color(0xFF8E44AD), Color(0xFF3498DB))),
                    onClick = { viewModel.onTextTypeSelected() },
                    modifier = Modifier.weight(1f)
                )
                StoryOptionButton(
                    icon = Icons.Default.MusicNote,
                    label = "Nhạc",
                    gradient = Brush.verticalGradient(listOf(Color(0xFFF1C40F), Color(0xFFE67E22))),
                    onClick = { /* Handle music */ },
                    modifier = Modifier.weight(1f)
                )
                StoryOptionButton(
                    icon = Icons.Default.GridView,
                    label = "Nhóm ảnh",
                    gradient = Brush.verticalGradient(listOf(Color(0xFF2ECC71), Color(0xFF27AE60))),
                    onClick = { /* Handle grid */ },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Selection Area
            if (state.storyType == "text") {
                TextStoryCreator(
                    text = state.textContent,
                    onTextChange = { viewModel.updateText(it) },
                    backgroundColor = state.backgroundColor,
                    onColorChange = { viewModel.updateColor(it) },
                    onPost = { viewModel.createStory() },
                    isLoading = state.isLoading
                )
            } else if (state.selectedUri != null) {
                ImageStoryPreview(
                    uri = state.selectedUri,
                    onPost = { viewModel.createStory() },
                    onCancel = { viewModel.reset() },
                    isLoading = state.isLoading
                )
            } else {
                // Placeholder for Gallery view (simplified)
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Thư viện", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Surface(
                            onClick = { imagePicker.launch("image/*") },
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Chọn nhiều file", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Simulated Gallery Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(1.dp),
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        items(15) { index ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .background(Color.DarkGray)
                                    .clickable { imagePicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Image, null, tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoryOptionButton(
    icon: ImageVector,
    label: String,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(gradient)
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TextStoryCreator(
    text: String,
    onTextChange: (String) -> Unit,
    backgroundColor: String,
    onColorChange: (String) -> Unit,
    onPost: () -> Unit,
    isLoading: Boolean
) {
    val colors = listOf("#1877F2", "#F44336", "#9C27B0", "#4CAF50", "#FF9800", "#000000")
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(android.graphics.Color.parseColor(backgroundColor))),
        contentAlignment = Alignment.Center
    ) {
        TextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Bắt đầu nhập", color = Color.White.copy(alpha = 0.5f), fontSize = 24.sp) },
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = Color.White
            )
        )

        // Color selectors
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colors.forEach { hex ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(hex)))
                        .border(
                            width = if (backgroundColor == hex) 2.dp else 0.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                        .clickable { onColorChange(hex) }
                )
            }
        }

        Button(
            onClick = onPost,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            enabled = text.isNotBlank() && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.Black)
            else Text("Chia sẻ", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ImageStoryPreview(
    uri: Uri,
    onPost: () -> Unit,
    onCancel: () -> Unit,
    isLoading: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f))
            ) {
                Text("Hủy")
            }
            Button(
                onClick = onPost,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                else Text("Chia sẻ ngay", fontWeight = FontWeight.Bold)
            }
        }
    }
}
