package com.socialapp.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Profile : Screen("profile?uid={uid}") {
        fun createRoute(uid: String? = null) = if (uid != null) "profile?uid=$uid" else "profile"
    }
    object CreatePost : Screen("create_post")
    object Comments : Screen("comments/{postId}") {
        fun createRoute(postId: String) = "comments/$postId"
    }
    object ChatList : Screen("chat_list")
    object Chat : Screen("chat/{uid}/{name}") {
        fun createRoute(uid: String, name: String) = "chat/$uid/$name"
    }
    object Explore : Screen("explore")
    // THÊM MỚI
    object CategoryFeed : Screen("category_feed/{tag}") {
        fun createRoute(tag: String) = "category_feed/$tag"
    }
    object Settings : Screen("settings")
    object Search : Screen("search/{query}") {
        fun createRoute(query: String) = "search/$query"
    }
    object Admin : Screen("admin")
}