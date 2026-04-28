package com.socialapp.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.socialapp.ui.auth.AuthViewModel
import com.socialapp.ui.auth.LoginScreen
import com.socialapp.ui.auth.RegisterScreen
import com.socialapp.ui.chat.ChatListScreen
import com.socialapp.ui.chat.ChatListViewModel
import com.socialapp.ui.chat.ChatScreen
import com.socialapp.ui.chat.ChatViewModel
import com.socialapp.ui.comment.CommentScreen
import com.socialapp.ui.comment.CommentViewModel
import com.socialapp.ui.home.HomeScreen
import com.socialapp.ui.post.CreatePostScreen
import com.socialapp.ui.post.CreatePostViewModel
import com.socialapp.ui.profile.ProfileScreen
import com.socialapp.ui.profile.ProfileViewModel
import com.socialapp.ui.explore.ExploreScreen
import com.socialapp.ui.search.SearchScreen
import com.socialapp.ui.settings.SettingsScreen
// Import màn hình Admin
import com.socialapp.ui.admin.AdminDashboardScreen
import com.socialapp.ui.admin.AdminViewModel

@Composable
fun AppNavigation(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    val startDest = if (authViewModel.state.isLoggedIn) Screen.Home.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDest) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToProfile = { uid ->
                    navController.navigate(Screen.Profile.createRoute(uid))
                },
                onNavigateToCreatePost = {
                    navController.navigate(Screen.CreatePost.route)
                },
                onNavigateToComments = { postId ->
                    navController.navigate(Screen.Comments.createRoute(postId))
                },
                onNavigateToChat = {
                    navController.navigate(Screen.ChatList.route)
                },
                onNavigateToExplore = {
                    navController.navigate(Screen.Explore.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToSearch = { query ->
                    navController.navigate(Screen.Search.createRoute(query))
                },
                // Thêm sự kiện để chuyển đến trang Admin nếu cần từ Home
                onNavigateToAdmin = {
                    navController.navigate(Screen.Admin.route)
                }
            )
        }

        composable(Screen.CreatePost.route) {
            val createPostViewModel: CreatePostViewModel = viewModel()
            CreatePostScreen(
                viewModel = createPostViewModel,
                onBack = { navController.popBackStack() },
                onPostCreated = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Comments.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
            val commentViewModel: CommentViewModel = viewModel()
            CommentScreen(
                postId = postId,
                viewModel = commentViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ChatList.route) {
            val chatListViewModel: ChatListViewModel = viewModel()
            ChatListScreen(
                viewModel = chatListViewModel,
                onBack = { navController.popBackStack() },
                onOpenChat = { uid, name ->
                    navController.navigate(Screen.Chat.createRoute(uid, name))
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("uid") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val chatViewModel: ChatViewModel = viewModel()
            ChatScreen(
                otherUid = uid,
                otherName = name,
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Profile.route,
            arguments = listOf(navArgument("uid") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid")
            val profileViewModel: ProfileViewModel = viewModel()
            ProfileScreen(
                viewModel = profileViewModel,
                uid = uid,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Explore.route) {
            ExploreScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                // Có thể thêm nút vào Admin từ Settings
                onNavigateToAdmin = {
                    navController.navigate(Screen.Admin.route)
                }
            )
        }

        composable(
            route = Screen.Search.route,
            arguments = listOf(navArgument("query") { type = NavType.StringType })
        ) { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query") ?: ""
            SearchScreen(
                query = query,
                onNavigateToProfile = { uid ->
                    navController.navigate(Screen.Profile.createRoute(uid))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- Màn hình quản trị (Admin) ---
        composable(Screen.Admin.route) {
            val adminViewModel: AdminViewModel = viewModel()
            AdminDashboardScreen(
                vm = adminViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }

    // Tự động chuyển hướng về Home nếu đã đăng nhập và đang ở Login/Register
    if (authViewModel.state.isLoggedIn &&
        (navController.currentDestination?.route == Screen.Login.route ||
                navController.currentDestination?.route == Screen.Register.route)) {
        navController.navigate(Screen.Home.route) {
            popUpTo(0) { inclusive = true }
        }
    }
}