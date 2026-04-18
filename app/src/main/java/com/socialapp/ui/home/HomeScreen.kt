package com.socialapp.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Explore
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNavigateToProfile: (String?) -> Unit,
    onNavigateToCreatePost: () -> Unit,
    onNavigateToComments: (String) -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: (String) -> Unit,
    feedViewModel: FeedViewModel = viewModel()
) {
    val state = feedViewModel.state
    var searchQuery by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    LaunchedEffect(Unit) {
        feedViewModel.loadFeed()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "SocialApp",
                        modifier = Modifier.clickable { feedViewModel.loadFeed() }
                    ) 
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                    TextButton(onClick = onLogout) {
                        Text("Logout")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, "Home") },
                    label = { Text("Home") },
                    selected = true,
                    onClick = { feedViewModel.loadFeed() }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Explore, "Explore") },
                    label = { Text("Explore") },
                    selected = false,
                    onClick = onNavigateToExplore
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.MailOutline, "Chat") },
                    label = { Text("Chat") },
                    selected = false,
                    onClick = onNavigateToChat
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, "Profile") },
                    label = { Text("Profile") },
                    selected = false,
                    onClick = { onNavigateToProfile(null) }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreatePost) {
                Icon(Icons.Default.Add, "Create Post")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search post, user...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { 
                        if (searchQuery.isNotBlank()) onNavigateToSearch(searchQuery) 
                    }
                )
            )
            
            if (searchQuery.isNotBlank() && state.recommendedUsers.isNotEmpty()) {
                val recommendationText = "Recommended: " + state.recommendedUsers.take(3).joinToString(", ") { "@${it.username}" }
                Text(
                    recommendationText, 
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading && state.posts.isEmpty() -> {
                        Box(
                            Modifier.fillMaxSize().padding(padding),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    state.error != null && state.posts.isEmpty() -> {
                        Box(
                            Modifier.fillMaxSize().padding(padding),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(state.error, color = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { feedViewModel.loadFeed() }) { Text("Retry") }
                            }
                        }
                    }

                    state.posts.isEmpty() -> {
                        Box(
                            Modifier.fillMaxSize().padding(padding),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No posts yet. Create one!",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(state.posts, key = { it.id }) { post ->
                                PostItem(
                                    post = post,
                                    user = state.userMap[post.user_id],
                                    isLiked = post.id in state.likedIds,
                                    onLike = { feedViewModel.toggleLike(post.id) },
                                    onComment = { onNavigateToComments(post.id) },
                                    onUserClick = { uid -> onNavigateToProfile(uid) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}