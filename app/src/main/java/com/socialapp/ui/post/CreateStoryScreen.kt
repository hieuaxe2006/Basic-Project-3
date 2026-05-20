package com.socialapp.ui.post

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.socialapp.utils.t

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryScreen(
    onBack: () -> Unit,
    onStoryCreated: () -> Unit,
    viewModel: CreateStoryViewModel = viewModel()
) {
    val state = viewModel.state

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.updateImage(uri)
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onStoryCreated()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(t("create_story"), fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Image, contentDescription = "Pick Image", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color(0xFF1C1C1E)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            StoryCreator(
                text = state.textContent,
                onTextChange = { viewModel.updateText(it) },
                backgroundColor = state.backgroundColor,
                onColorChange = { viewModel.updateColor(it) },
                selectedImageUri = state.selectedImageUri,
                visibility = state.visibility,
                onVisibilityChange = { viewModel.updateVisibility(it) },
                onPost = { viewModel.createStory() },
                isLoading = state.isLoading
            )
        }
    }
}

@Composable
fun StoryCreator(
    text: String,
    onTextChange: (String) -> Unit,
    backgroundColor: String,
    onColorChange: (String) -> Unit,
    selectedImageUri: Uri?,
    visibility: String,
    onVisibilityChange: (String) -> Unit,
    onPost: () -> Unit,
    isLoading: Boolean
) {
    val colors = listOf("#1877F2", "#F44336", "#9C27B0", "#4CAF50", "#FF9800", "#000000")
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (selectedImageUri == null) Color(android.graphics.Color.parseColor(backgroundColor)) else Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (selectedImageUri != null) {
            AsyncImage(
                model = selectedImageUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Overlay for text
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
        }

        TextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text(t("start_typing"), color = Color.White.copy(alpha = 0.5f), fontSize = 24.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                textAlign = TextAlign.Center,
                color = Color.White
            )
        )

        // Bottom Controls
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Visibility Selector
            Row(
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val visibilityOptions = listOf(
                    Triple("public", Icons.Default.Public, "Công khai"),
                    Triple("friends", Icons.Default.Group, "Bạn bè"),
                    Triple("private", Icons.Default.Lock, "Chỉ mình tôi")
                )
                
                visibilityOptions.forEach { (v, icon, label) ->
                    val isSelected = visibility == v
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onVisibilityChange(v) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Color selectors
            if (selectedImageUri == null) {
                Row(
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
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onPost,
                enabled = (text.isNotBlank() || selectedImageUri != null) && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.Black)
                } else {
                    Text(t("share"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
