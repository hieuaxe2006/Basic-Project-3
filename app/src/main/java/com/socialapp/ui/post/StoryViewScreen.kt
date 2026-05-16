package com.socialapp.ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.socialapp.data.model.Story
import com.socialapp.data.model.User
import kotlinx.coroutines.delay

@Composable
fun StoryViewScreen(
    storyId: String,
    onClose: () -> Unit,
    viewModel: StoryViewViewModel = viewModel()
) {
    val story = viewModel.story
    val user = viewModel.user
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(storyId) {
        viewModel.loadStory(storyId)
    }

    LaunchedEffect(story) {
        if (story != null) {
            val duration = 5000L // 5 seconds
            val steps = 100
            val stepDuration = duration / steps
            for (i in 1..steps) {
                delay(stepDuration)
                progress = i.toFloat() / steps
            }
            onClose()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (story != null) {
            // Content
            if (story.type == "text") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(android.graphics.Color.parseColor(story.backgroundColor))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = story.text,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                AsyncImage(
                    model = story.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            // Top overlay (User info & Progress bar)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                )
                
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (user?.avatar?.isNotBlank() == true) {
                        AsyncImage(
                            model = user.avatar,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).background(Color.Gray, shape = androidx.compose.foundation.shape.CircleShape).padding(2.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(40.dp).background(Color.Gray, shape = androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(user?.username?.take(1)?.uppercase() ?: "?", color = Color.White)
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = user?.username ?: "Loading...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(Modifier.weight(1f))

                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        } else {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
        }
    }
}
