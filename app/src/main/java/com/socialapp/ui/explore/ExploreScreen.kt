package com.socialapp.ui.explore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

data class Category(val name: String, val postCount: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExploreViewModel = viewModel()
) {
    val state = viewModel.state

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Explore") })
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.categories.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("No hashtags found. Make a post with a hashtag!")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.categories) { category ->
                        val alpha = if (category.postCount >= 1) 1f else 0.5f
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = category.postCount >= 1) { }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    category.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                                )
                                Text(
                                    "${category.postCount} posts",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
