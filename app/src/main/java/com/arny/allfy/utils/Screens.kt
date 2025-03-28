package com.arny.allfy.utils

sealed class Screens(val route: String) {
    object SplashScreen : Screens("splash_screen")
    object LoginScreen : Screens("login_screen")
    object SignUpScreen : Screens("sign_up_screen")
    object FeedScreen : Screens("feed_screen")
    object SearchScreen : Screens("search_screen")
    object ProfileScreen : Screens("profile_screen")
    object EditProfileScreen : Screens("edit_profile_screen")
    object CreatePostScreen : Screens("create_post_screen")
    object SettingsScreen : Screens("settings_screen")
    object ChatScreen : Screens("chat_screen")
    object ConversationsScreen : Screens("conversations_screen")

}