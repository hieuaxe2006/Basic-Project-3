package com.socialapp.ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.socialapp.utils.t

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryScreen(
    onBack: () -> Unit,
    onStoryCreated: () -> Unit,
    viewModel: CreateStoryViewModel = viewModel()
) {
    val state = viewModel.state

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
            TextStoryCreator(
                text = state.textContent,
                onTextChange = { viewModel.updateText(it) },
                backgroundColor = state.backgroundColor,
                onColorChange = { viewModel.updateColor(it) },
                onPost = { viewModel.createStory() },
                isLoading = state.isLoading
            )
        }
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
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Color.Black)
            } else {
                Text(t("share"), fontWeight = FontWeight.Bold)
            }
        }
    }
}
