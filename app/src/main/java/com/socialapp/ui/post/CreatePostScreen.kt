package com.socialapp.ui.post

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import com.socialapp.utils.t

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(
    viewModel: CreatePostViewModel,
    onBack: () -> Unit,
    onPostCreated: () -> Unit,
    onNavigateToPremium: () -> Unit
) {
    var content by remember { mutableStateOf("") }
    var showCodeInput by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }
    var codeInput by remember { mutableStateOf("") }
    var selectedLang by remember { mutableStateOf("Push") }

    val state = viewModel.state
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val domains = listOf("Workout", "Nutrition", "Supplements", "Transformation", "Motivation", "Q&A")

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        viewModel.setImageUri(uri)
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            Toast.makeText(context, "Success", Toast.LENGTH_LONG).show()
            viewModel.reset()
            onPostCreated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("create_post_title"), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    Button(onClick = { viewModel.createPost(content, context) }, enabled = !state.isLoading) {
                        if (state.isLoading) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text(t("post_btn"))
                        }
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
                            Text("User", fontWeight = FontWeight.Bold)
                            if (state.selectedTaggedUsers.isNotEmpty()) {
                                Text(" cùng với ", fontSize = 12.sp)
                                Text("${state.selectedTaggedUsers[0].username}${if(state.selectedTaggedUsers.size > 1) " và ${state.selectedTaggedUsers.size-1} người khác" else ""}", fontWeight = FontWeight.Bold, color = Color(0xFF1877F2), fontSize = 12.sp)
                            }
                        }
                        Text(t("post_status_pending"), fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Spacer(Modifier.height(16.dp))
                TextField(value = content, onValueChange = { content = it }, placeholder = { Text(t("quick_post_hint")) }, modifier = Modifier.fillMaxWidth(), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent))

                state.error?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            if (it.contains("Premium")) {
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = onNavigateToPremium,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("UPGRADE PREMIUM", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { viewModel.autoTagContent(content) },
                        label = {
                            if (state.isAutoTagging) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("🤖 " + t("ai_suggest_tag"))
                            }
                        },
                        enabled = content.isNotBlank() && !state.isAutoTagging
                    )
                    AssistChip(
                        onClick = { viewModel.getAiSuggestion(content) },
                        label = {
                            if (state.isAnalyzingAi) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("🤖 " + t("ai_analyze_post"))
                            }
                        },
                        enabled = content.isNotBlank() && !state.isAnalyzingAi
                    )
                }

                state.aiSuggestion?.let { suggestion ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, Color(0xFF00E676))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🤖", fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "AI Analysis:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF00E676)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = suggestion,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (showCodeInput) {
                    Spacer(Modifier.height(16.dp))
                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF1E1E1E)).padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(t("workout_log"), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                Text(
                                    text = "Type: $selectedLang ▾",
                                    color = Color.LightGray,
                                    fontSize = 13.sp,
                                    modifier = Modifier.clickable { expanded = true }
                                )
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    listOf("Push", "Pull", "Legs", "Cardio", "FullBody").forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type) },
                                            onClick = {
                                                selectedLang = type
                                                viewModel.updateCode(codeInput, type)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        TextField(
                            value = codeInput,
                            onValueChange = { codeInput = it; viewModel.updateCode(it, selectedLang) },
                            placeholder = { Text("Bench Press - 4x10 @ 80kg...", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = Color.White),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                cursorColor = Color.White
                            )
                        )
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
                    Text(t("add_to_your_post"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        IconButton(onClick = { imagePicker.launch("image/*") }) { Icon(Icons.Outlined.Image, null, tint = Color(0xFF45BD62)) }
                        IconButton(onClick = { showCodeInput = !showCodeInput }) { Icon(Icons.Default.FitnessCenter, null, tint = if (showCodeInput) MaterialTheme.colorScheme.primary else Color.Gray) }
                        IconButton(onClick = { viewModel.loadFriends(); showTagSheet = true }) { Icon(Icons.Outlined.PersonAdd, null, tint = Color(0xFF1877F2)) }
                    }
                }
            }
        }
    }

    if (showTagSheet) {
        ModalBottomSheet(onDismissRequest = { showTagSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 400.dp)) {
                Text(t("tag_friends"), fontWeight = FontWeight.Bold)
                if (state.isLoadingFriends) Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                LazyColumn {
                    items(state.friends) { friend ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleTagUser(friend) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = state.selectedTaggedUsers.any { it.id == friend.id }, onCheckedChange = { viewModel.toggleTagUser(friend) })
                            Text(friend.username)
                        }
                    }
                }
                Button(onClick = { showTagSheet = false }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text(t("done")) }
            }
        }
    }
}