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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
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
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
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
                title = { Text("Create Post") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.createPost(content, context) },
                        enabled = !state.isLoading && (content.isNotBlank() || state.imageUri != null)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Post")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Content Input with optional background
            val backgroundColor = if (state.backgroundColor.isNotBlank()) Color(android.graphics.Color.parseColor(state.backgroundColor)) else MaterialTheme.colorScheme.surface
            val onBackgroundColor = if (state.backgroundColor.isNotBlank() && state.backgroundColor != "#FFFFFF") Color.White else MaterialTheme.colorScheme.onSurface

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("What's on your mind?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = onBackgroundColor,
                    unfocusedTextColor = onBackgroundColor,
                    focusedPlaceholderColor = onBackgroundColor.copy(alpha = 0.6f),
                    unfocusedPlaceholderColor = onBackgroundColor.copy(alpha = 0.6f),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                maxLines = 15
            )

            Spacer(Modifier.height(16.dp))

            // Domain Tags
            Text("Select Domain (Mandatory)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
                        label = { Text(tag) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Theme Selection
            Text("Select Theme", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(colors) { hex ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(hex)))
                            .border(
                                width = if (state.backgroundColor == hex) 3.dp else 1.dp,
                                color = if (state.backgroundColor == hex) MaterialTheme.colorScheme.primary else Color.LightGray,
                                shape = CircleShape
                            )
                            .clickable { viewModel.setBackgroundColor(hex) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Image Preview
            state.imageUri?.let { uri ->
                Box {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Selected image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { viewModel.removeImage() },
                        modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, "Remove", tint = Color.White)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { imagePicker.launch("image/*") }) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add Image")
                }
                OutlinedButton(onClick = { content += " #AI_Suggested_Tag" }) {
                    Text("✨ Auto Tag")
                }
            }

            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}
