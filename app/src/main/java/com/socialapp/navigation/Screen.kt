package com.socialapp.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Profile : Screen("profile?uid={uid}") {
        fun createRoute(uid: String? = null) = if (uid != null) "profile?uid=$uid" else "profile"
    }
    object CreatePost : Screen("create_post")
    object CreateStory : Screen("create_story")
    object ViewStory : Screen("view_story/{storyId}") {
        fun createRoute(storyId: String) = "view_story/$storyId"
    }
    object Comments : Screen("comments/{postId}") {
        fun createRoute(postId: String) = "comments/$postId"
    }
    object ChatList : Screen("chat_list")
    object Chat : Screen("chat/{uid}/{name}") {
        fun createRoute(uid: String, name: String) = "chat/$uid/$name"
    }
    object ChatInfo : Screen("chat_info/{uid}/{name}") {
        fun createRoute(uid: String, name: String) = "chat_info/$uid/$name"
    }
    object Explore : Screen("explore")
    object Group : Screen("group")
    object GroupDetail : Screen("group_detail/{groupId}") {
        fun createRoute(groupId: String) = "group_detail/$groupId"
    }
    object CategoryFeed : Screen("category_feed/{tag}") {
        fun createRoute(tag: String) = "category_feed/$tag"
    }
    object Settings : Screen("settings")
    object Search : Screen("search/{query}") {
        fun createRoute(query: String) = "search/$query"
    }
    object Admin : Screen("admin")
    object Notifications : Screen("notifications")
    object Premium : Screen("premium") // Màn hình mới
}