package com.arny.allfy.utils

import com.arny.allfy.domain.model.User
import kotlinx.serialization.Serializable
import org.webrtc.SurfaceViewRenderer

@Serializable
sealed class Screen {
    @Serializable
    object SplashScreen : Screen()

    @Serializable
    object SignUpScreen : Screen()

    @Serializable
    object LoginScreen : Screen()

    @Serializable
    object FeedScreen : Screen()

    @Serializable
    object SearchScreen : Screen()

    @Serializable
    object EditProfileScreen : Screen()

    @Serializable
    object CreatePostScreen : Screen()

    @Serializable
    object SettingsScreen : Screen()

    @Serializable
    object ConversationsScreen : Screen()

    @Serializable
    object QRScannerScreen : Screen()

    @Serializable
    data class ProfileScreen(val userId: String? = null) : Screen()

    @Serializable
    data class PostDetailScreen(val postID: String) : Screen()

    @Serializable
    data class FollowScreen(val userId: String, val initialTab: Int) : Screen()

    @Serializable
    data class ChatScreen(
        val conversationId: String? = null,
        val otherUserId: String
    ) : Screen()


    @Serializable
    data class CallScreen(
        val conversationId: String,
        val isCaller: Boolean,
        val otherUserId: String
    ) : Screen()

    @Serializable
    data class StoryViewerScreen(
        val userId: String,
        val isCurrentUser: Boolean,
        val userIdsWithStories: List<String> = emptyList()
    ) : Screen()

    @Serializable
    data object CreateStoryScreen : Screen()

}