package com.socialapp.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.socialapp.ui.auth.*
import com.socialapp.ui.chat.*
import com.socialapp.ui.comment.*
import com.socialapp.ui.home.HomeScreen
import com.socialapp.ui.post.*
import com.socialapp.ui.profile.*
import com.socialapp.ui.explore.*
import com.socialapp.ui.search.SearchScreen
import com.socialapp.ui.settings.SettingsScreen
import com.socialapp.ui.admin.AdminDashboardScreen
import com.socialapp.ui.admin.AdminViewModel
import com.socialapp.ui.notification.NotificationScreen

@Composable
fun AppNavigation(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    val authState = authViewModel.state

    // Hiển thị loading khi đang xác thực phiên đăng nhập (cold start)
    if (authState.isSessionLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Đang tải dữ liệu...", style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    // Xác định màn hình bắt đầu cố định lúc khởi tạo
    val startDest = androidx.compose.runtime.remember {
        when {
            !authState.isLoggedIn -> Screen.Login.route
            authState.userRole == "admin" -> Screen.Admin.route
            else -> Screen.Home.route
        }
    }

    // Lắng nghe thay đổi trạng thái đăng nhập để điều hướng
    androidx.compose.runtime.LaunchedEffect(authState.isLoggedIn, authState.userRole) {
        if (authState.isLoggedIn && authState.userRole != null) {
            val target = if (authState.userRole == "admin") Screen.Admin.route else Screen.Home.route
            navController.navigate(target) {
                popUpTo(0) { inclusive = true }
            }
        } else if (!authState.isLoggedIn) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != Screen.Login.route && currentRoute != Screen.Register.route) {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDest) {
        // --- Màn hình Đăng nhập ---
        composable(Screen.Login.route) {
            LoginScreen(viewModel = authViewModel, onNavigateToRegister = {
                navController.navigate(Screen.Register.route)
            })
        }

        // --- Màn hình Đăng ký ---
        composable(Screen.Register.route) {
            RegisterScreen(viewModel = authViewModel, onNavigateToLogin = {
                navController.popBackStack()
            })
        }

        // --- Màn hình Trang chủ (Feed) ---
        composable(Screen.Home.route) {
            HomeScreen(
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) { popUpTo(0) }
                },
                onNavigateToProfile = { uid -> navController.navigate(Screen.Profile.createRoute(uid)) },
                onNavigateToCreatePost = { navController.navigate(Screen.CreatePost.route) },
                onNavigateToComments = { postId -> navController.navigate(Screen.Comments.createRoute(postId)) },
                onNavigateToChat = { navController.navigate(Screen.ChatList.route) },
                onNavigateToExplore = { navController.navigate(Screen.Explore.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToSearch = { query -> navController.navigate(Screen.Search.createRoute(query)) },
                onNavigateToAdmin = {},
                onNavigateToCreateStory = { navController.navigate(Screen.CreateStory.route) },
                onNavigateToViewStory = { storyId -> navController.navigate(Screen.ViewStory.createRoute(storyId)) },
                onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) }
            )
        }

        // --- Màn hình Thông báo ---
        composable(Screen.Notifications.route) {
            NotificationScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPost = { postId -> navController.navigate(Screen.Comments.createRoute(postId)) },
                onNavigateToProfile = { uid -> navController.navigate(Screen.Profile.createRoute(uid)) }
            )
        }

        // --- Màn hình Admin ---
        composable(Screen.Admin.route) {
            AdminDashboardScreen(
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) { popUpTo(0) }
                }
            )
        }

        // --- Màn hình Tạo bài viết ---
        composable(Screen.CreatePost.route) {
            CreatePostScreen(
                viewModel = viewModel(),
                onBack = { navController.popBackStack() },
                onPostCreated = { navController.popBackStack() }
            )
        }

        // --- Màn hình Tạo tin (Story) ---
        composable(Screen.CreateStory.route) {
            CreateStoryScreen(
                onBack = { navController.popBackStack() },
                onStoryCreated = { navController.popBackStack() }
            )
        }

        // --- Màn hình Xem tin (Story) ---
        composable(
            route = Screen.ViewStory.route,
            arguments = listOf(navArgument("storyId") { type = NavType.StringType })
        ) { bse ->
            val storyId = bse.arguments?.getString("storyId") ?: return@composable
            StoryViewScreen(
                storyId = storyId,
                onClose = { navController.popBackStack() }
            )
        }

        // --- Màn hình Bình luận ---
        composable(
            route = Screen.Comments.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { bse ->
            val pid = bse.arguments?.getString("postId") ?: return@composable
            CommentScreen(postId = pid, viewModel = viewModel(), onBack = { navController.popBackStack() })
        }

        // --- Danh sách Chat ---
        composable(Screen.ChatList.route) {
            ChatListScreen(
                viewModel = viewModel(),
                onBack = { navController.popBackStack() },
                onOpenChat = { uid, name ->
                    navController.navigate(Screen.Chat.createRoute(uid, name))
                }
            )
        }

        // --- Màn hình Chat chi tiết ---
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("uid") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { bse ->
            val uid = bse.arguments?.getString("uid") ?: ""
            val name = bse.arguments?.getString("name") ?: ""
            ChatScreen(otherUid = uid, otherName = name, viewModel = viewModel(), onBack = { navController.popBackStack() })
        }

        // --- Màn hình Trang cá nhân ---
        composable(
            route = Screen.Profile.route,
            arguments = listOf(navArgument("uid") { type = NavType.StringType; nullable = true })
        ) { bse ->
            ProfileScreen(
                viewModel = viewModel(),
                uid = bse.arguments?.getString("uid"),
                onBack = { navController.popBackStack() },
                onNavigateToChat = { uid, name ->
                    navController.navigate(Screen.Chat.createRoute(uid, name))
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) { popUpTo(0) }
                }
            )
        }

        // --- Màn hình Explore (Khám phá) ---
        composable(Screen.Explore.route) {
            ExploreScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCategory = { tag ->
                    navController.navigate(Screen.CategoryFeed.createRoute(tag))
                }
            )
        }

        // --- Màn hình Feed theo Chủ đề (Tag) ---
        composable(
            route = Screen.CategoryFeed.route,
            arguments = listOf(navArgument("tag") { type = NavType.StringType })
        ) { bse ->
            val tag = bse.arguments?.getString("tag") ?: ""
            CategoryFeedScreen(
                tag = tag,
                onBack = { navController.popBackStack() },
                onNavigateToProfile = { uid -> navController.navigate(Screen.Profile.createRoute(uid)) },
                onNavigateToComments = { pid -> navController.navigate(Screen.Comments.createRoute(pid)) }
            )
        }

        // --- Màn hình Cài đặt ---
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() }, onNavigateToAdmin = {})
        }

        // --- Màn hình Tìm kiếm ---
        composable(
            route = Screen.Search.route,
            arguments = listOf(navArgument("query") { type = NavType.StringType })
        ) { bse ->
            val query = bse.arguments?.getString("query") ?: ""
            SearchScreen(
                query = query,
                onNavigateToProfile = { u -> navController.navigate(Screen.Profile.createRoute(u)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
