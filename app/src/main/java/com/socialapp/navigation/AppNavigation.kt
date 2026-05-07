package com.socialapp.navigation

import androidx.compose.runtime.Composable
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
import com.socialapp.ui.explore.ExploreScreen
import com.socialapp.ui.search.SearchScreen
import com.socialapp.ui.settings.SettingsScreen
import com.socialapp.ui.admin.AdminDashboardScreen
import com.socialapp.ui.admin.AdminViewModel

@Composable
fun AppNavigation(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    val authState = authViewModel.state

    // Xác định màn hình bắt đầu
    val startDest = when {
        !authState.isLoggedIn -> Screen.Login.route
        authState.userRole == "admin" -> Screen.Admin.route
        else -> Screen.Home.route
    }

    // Chờ lấy role từ Firebase
    if (authState.isLoggedIn && authState.userRole == null) return

    NavHost(navController = navController, startDestination = startDest) {
        composable(Screen.Login.route) {
            LoginScreen(viewModel = authViewModel, onNavigateToRegister = {
                navController.navigate(Screen.Register.route)
            })
        }

        composable(Screen.Register.route) {
            RegisterScreen(viewModel = authViewModel, onNavigateToLogin = { navController.popBackStack() })
        }

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
                onNavigateToAdmin = {}
            )
        }

        composable(Screen.Admin.route) {
            val adminVM: AdminViewModel = viewModel()
            AdminDashboardScreen(
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) { popUpTo(0) }
                },
                vm = adminVM
            )
        }

        composable(Screen.CreatePost.route) {
            CreatePostScreen(viewModel = viewModel(), onBack = { navController.popBackStack() }, onPostCreated = { navController.popBackStack() })
        }

        composable(route = Screen.Comments.route, arguments = listOf(navArgument("postId") { type = NavType.StringType })) { bse ->
            val pid = bse.arguments?.getString("postId") ?: return@composable
            CommentScreen(postId = pid, viewModel = viewModel(), onBack = { navController.popBackStack() })
        }

        composable(Screen.ChatList.route) {
            ChatListScreen(viewModel = viewModel(), onBack = { navController.popBackStack() }, onOpenChat = { u, n ->
                navController.navigate(Screen.Chat.createRoute(u, n))
            })
        }

        composable(route = Screen.Chat.route, arguments = listOf(navArgument("uid") { type = NavType.StringType }, navArgument("name") { type = NavType.StringType })) { bse ->
            val uid = bse.arguments?.getString("uid") ?: ""
            val name = bse.arguments?.getString("name") ?: ""
            ChatScreen(otherUid = uid, otherName = name, viewModel = viewModel(), onBack = { navController.popBackStack() })
        }

        composable(route = Screen.Profile.route, arguments = listOf(navArgument("uid") { type = NavType.StringType; nullable = true })) { bse ->
            ProfileScreen(viewModel = viewModel(), uid = bse.arguments?.getString("uid"), onBack = { navController.popBackStack() })
        }

        composable(Screen.Explore.route) { ExploreScreen(onNavigateBack = { navController.popBackStack() }) }

        composable(Screen.Settings.route) { SettingsScreen(onBack = { navController.popBackStack() }, onNavigateToAdmin = {}) }

        composable(route = Screen.Search.route, arguments = listOf(navArgument("query") { type = NavType.StringType })) { bse ->
            SearchScreen(query = bse.arguments?.getString("query") ?: "", onNavigateToProfile = { u -> navController.navigate(Screen.Profile.createRoute(u)) }, onNavigateBack = { navController.popBackStack() })
        }
    }
}