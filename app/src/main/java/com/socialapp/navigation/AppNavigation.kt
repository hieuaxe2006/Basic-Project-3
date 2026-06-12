package com.socialapp.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.socialapp.ui.auth.*
import com.socialapp.ui.chat.*
import com.socialapp.ui.comment.*
import com.socialapp.ui.home.HomeScreen
import com.socialapp.ui.post.*
import com.socialapp.ui.profile.*
import com.socialapp.ui.explore.CategoryFeedScreen
import com.socialapp.ui.group.*
import com.socialapp.ui.search.SearchScreen
import com.socialapp.ui.settings.SettingsScreen
import com.socialapp.ui.admin.AdminDashboardScreen
import com.socialapp.ui.notification.NotificationScreen
import com.socialapp.ui.premium.PremiumScreen
import com.socialapp.ui.home.FeedViewModel
import com.socialapp.utils.t // IMPORT HÀM DỊCH

@Composable
fun AppNavigation(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    val authState = authViewModel.state
    val feedViewModel: FeedViewModel = viewModel()
    val feedState = feedViewModel.state

    if (authState.isSessionLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(t("loading_data"), style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    val startDest = remember {
        when {
            !authState.isLoggedIn -> Screen.Login.route
            authState.userRole == "admin" -> Screen.Admin.route
            else -> Screen.Home.route
        }
    }

    LaunchedEffect(authState.isLoggedIn, authState.userRole) {
        if (authState.isLoggedIn && authState.userRole != null) {
            val target = if (authState.userRole == "admin") Screen.Admin.route else Screen.Home.route
            navController.navigate(target) { popUpTo(0) { inclusive = true } }
        } else if (!authState.isLoggedIn) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != Screen.Login.route && currentRoute != Screen.Register.route) {
                navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
            }
        }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val mainRoutes = listOf(Screen.Home.route, Screen.Group.route, Screen.CreatePost.route, Screen.ChatList.route)
    val showBottomBar = currentRoute in mainRoutes || (currentRoute?.startsWith("profile") == true && currentBackStackEntry?.arguments?.getString("uid") == null) || (currentRoute?.startsWith("profile") == true && currentBackStackEntry?.arguments?.getString("uid") == feedViewModel.currentUid)

    Scaffold(
        bottomBar = {
            if (showBottomBar && authState.isLoggedIn && authState.userRole != "admin") {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text(t("home_tab")) }, // DÙNG t()
                        selected = currentRoute == Screen.Home.route,
                        onClick = {
                            if (currentRoute != Screen.Home.route) {
                                navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = false } }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.FitnessCenter, null) },
                        label = { Text(t("training_tab")) }, // DÙNG t()
                        selected = currentRoute == Screen.Group.route,
                        onClick = {
                            if (currentRoute != Screen.Group.route) {
                                navController.navigate(Screen.Group.route) { popUpTo(Screen.Home.route) { inclusive = false } }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.AddCircle, modifier = Modifier.size(32.dp), contentDescription = null) },
                        label = { Text(t("post_tab")) }, // DÙNG t()
                        selected = currentRoute == Screen.CreatePost.route,
                        onClick = {
                            if (currentRoute != Screen.CreatePost.route) {
                                navController.navigate(Screen.CreatePost.route) { popUpTo(Screen.Home.route) { inclusive = false } }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            BadgedBox(badge = { if (feedState.unreadChatCount > 0) Badge(containerColor = Color.Red) { Text(feedState.unreadChatCount.toString(), color = Color.White) } }) {
                                Icon(Icons.Default.Chat, "Chat")
                            }
                        },
                        label = { Text(t("chat_tab")) }, // DÙNG t()
                        selected = currentRoute == Screen.ChatList.route,
                        onClick = {
                            if (currentRoute != Screen.ChatList.route) {
                                navController.navigate(Screen.ChatList.route) { popUpTo(Screen.Home.route) { inclusive = false } }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, null) },
                        label = { Text(t("profile_tab")) }, // DÙNG t()
                        selected = currentRoute?.startsWith("profile") == true,
                        onClick = {
                            if (currentRoute?.startsWith("profile") != true) {
                                navController.navigate(Screen.Profile.createRoute(null)) { popUpTo(Screen.Home.route) { inclusive = false } }
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(navController = navController, startDestination = startDest) {
                composable(Screen.Login.route) { LoginScreen(viewModel = authViewModel, onNavigateToRegister = { navController.navigate(Screen.Register.route) }) }
                composable(Screen.Register.route) { RegisterScreen(viewModel = authViewModel, onNavigateToLogin = { navController.popBackStack() }) }
                composable(Screen.Home.route) { HomeScreen(onLogout = { authViewModel.logout(); navController.navigate(Screen.Login.route) { popUpTo(0) } }, onNavigateToProfile = { uid -> navController.navigate(Screen.Profile.createRoute(uid)) }, onNavigateToCreatePost = { navController.navigate(Screen.CreatePost.route) }, onNavigateToComments = { postId -> navController.navigate(Screen.Comments.createRoute(postId)) }, onNavigateToChat = { navController.navigate(Screen.ChatList.route) }, onNavigateToExplore = { navController.navigate(Screen.Group.route) }, onNavigateToSettings = { navController.navigate(Screen.Settings.route) }, onNavigateToSearch = { query -> navController.navigate(Screen.Search.createRoute(query)) }, onNavigateToAdmin = {}, onNavigateToCreateStory = { navController.navigate(Screen.CreateStory.route) }, onNavigateToViewStory = { storyId -> navController.navigate(Screen.ViewStory.createRoute(storyId)) }, onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) }) }
                composable(Screen.Notifications.route) { NotificationScreen(onBack = { navController.popBackStack() }, onNavigateToPost = { postId -> navController.navigate(Screen.Comments.createRoute(postId)) }, onNavigateToProfile = { uid -> navController.navigate(Screen.Profile.createRoute(uid)) }) }
                composable(Screen.Admin.route) { AdminDashboardScreen(onLogout = { authViewModel.logout(); navController.navigate(Screen.Login.route) { popUpTo(0) } }) }
                composable(Screen.CreatePost.route) { CreatePostScreen(viewModel = viewModel(), onBack = { navController.popBackStack() }, onPostCreated = { navController.popBackStack() }, onNavigateToPremium = { navController.navigate(Screen.Premium.route) }) }
                composable(Screen.Premium.route) { PremiumScreen(onBack = { navController.popBackStack() }) }
                composable(Screen.CreateStory.route) { CreateStoryScreen(onBack = { navController.popBackStack() }, onStoryCreated = { navController.popBackStack() }) }
                composable(route = Screen.ViewStory.route, arguments = listOf(navArgument("storyId") { type = NavType.StringType })) { bse -> val storyId = bse.arguments?.getString("storyId") ?: return@composable
                    StoryViewScreen(storyId = storyId, onClose = { navController.popBackStack() }) }
                composable(route = Screen.Comments.route, arguments = listOf(navArgument("postId") { type = NavType.StringType })) { bse -> val pid = bse.arguments?.getString("postId") ?: return@composable
                    CommentScreen(postId = pid, viewModel = viewModel(), onBack = { navController.popBackStack() }) }
                composable(Screen.ChatList.route) { ChatListScreen(viewModel = viewModel(), onBack = { navController.popBackStack() }, onOpenChat = { uid, name -> navController.navigate(Screen.Chat.createRoute(uid, name)) }) }
                composable(route = Screen.Chat.route, arguments = listOf(navArgument("uid") { type = NavType.StringType }, navArgument("name") { type = NavType.StringType })) { bse -> val uid = bse.arguments?.getString("uid") ?: ""; val name = bse.arguments?.getString("name") ?: ""; ChatScreen(otherUid = uid, otherName = name, viewModel = viewModel(), onBack = { navController.popBackStack() }, onShowInfo = { u, n -> navController.navigate(Screen.ChatInfo.createRoute(u, n)) }) }
                composable(route = Screen.ChatInfo.route, arguments = listOf(navArgument("uid") { type = NavType.StringType }, navArgument("name") { type = NavType.StringType })) { bse -> val uid = bse.arguments?.getString("uid") ?: ""; val name = bse.arguments?.getString("name") ?: ""; ChatInfoScreen(uid = uid, name = name, onBack = { navController.popBackStack() }, onViewProfile = { u -> navController.navigate(Screen.Profile.createRoute(u)) }) }
                composable(route = Screen.Profile.route, arguments = listOf(navArgument("uid") { type = NavType.StringType; nullable = true })) { bse -> ProfileScreen(viewModel = viewModel(), uid = bse.arguments?.getString("uid"), onBack = { navController.popBackStack() }, onNavigateToChat = { uid, name -> navController.navigate(Screen.Chat.createRoute(uid, name)) }, onLogout = { authViewModel.logout(); navController.navigate(Screen.Login.route) { popUpTo(0) } }) }
                composable(Screen.Group.route) { com.socialapp.ui.training.TrainingScreen() }
                composable(route = Screen.GroupDetail.route, arguments = listOf(navArgument("groupId") { type = NavType.StringType })) { bse -> val groupId = bse.arguments?.getString("groupId") ?: ""; GroupDetailScreen(groupId = groupId, onBack = { navController.popBackStack() }, onNavigateToComments = { pid -> navController.navigate(Screen.Comments.createRoute(pid)) }, onNavigateToProfile = { uid -> navController.navigate(Screen.Profile.createRoute(uid)) }) }
                composable(route = Screen.CategoryFeed.route, arguments = listOf(navArgument("tag") { type = NavType.StringType })) { bse -> val tag = bse.arguments?.getString("tag") ?: ""; CategoryFeedScreen(tag = tag, onBack = { navController.popBackStack() }, onNavigateToProfile = { uid -> navController.navigate(Screen.Profile.createRoute(uid)) }, onNavigateToComments = { pid -> navController.navigate(Screen.Comments.createRoute(pid)) }) }
                composable(Screen.Settings.route) { SettingsScreen(onBack = { navController.popBackStack() }, onNavigateToAdmin = {}, onNavigateToPremium = { navController.navigate(Screen.Premium.route) }) }
                composable(route = Screen.Search.route, arguments = listOf(navArgument("query") { type = NavType.StringType })) { bse -> val query = bse.arguments?.getString("query") ?: ""; SearchScreen(query = query, onNavigateToProfile = { u -> navController.navigate(Screen.Profile.createRoute(u)) }, onNavigateBack = { navController.popBackStack() }) }
            }
        }
    }
}