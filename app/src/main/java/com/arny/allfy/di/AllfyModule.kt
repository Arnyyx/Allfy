package com.arny.allfy.di

import android.app.Application
import android.content.Context
import com.arny.allfy.data.remote.AuthenticationRepositoryImpl
import com.arny.allfy.data.remote.CallRepositoryImpl
import com.arny.allfy.data.remote.GoogleAuthClient
import com.arny.allfy.data.remote.MessageRepositoryImpl
import com.arny.allfy.data.remote.PostRepositoryImpl
import com.arny.allfy.data.remote.UserRepositoryImpl
import com.arny.allfy.domain.repository.AuthenticationRepository
import com.arny.allfy.domain.repository.CallRepository
import com.arny.allfy.domain.repository.MessageRepository
import com.arny.allfy.domain.repository.PostRepository
import com.arny.allfy.domain.repository.UserRepository
import com.arny.allfy.domain.usecase.authentication.AuthenticationUseCases
import com.arny.allfy.domain.usecase.authentication.GetCurrentUserId
import com.arny.allfy.domain.usecase.authentication.SignInWithEmail
import com.arny.allfy.domain.usecase.authentication.SignOut
import com.arny.allfy.domain.usecase.authentication.SignUp
import com.arny.allfy.domain.usecase.authentication.IsAuthenticated
import com.arny.allfy.domain.usecase.authentication.SignInWithGoogle
import com.arny.allfy.domain.usecase.message.DeleteMessage
import com.arny.allfy.domain.usecase.message.GetMessages
import com.arny.allfy.domain.usecase.message.InitializeConversation
import com.arny.allfy.domain.usecase.message.LoadConversations
import com.arny.allfy.domain.usecase.message.MarkMessageAsRead
import com.arny.allfy.domain.usecase.message.MessageUseCases
import com.arny.allfy.domain.usecase.message.SendImages
import com.arny.allfy.domain.usecase.message.SendMessage
import com.arny.allfy.domain.usecase.message.SendVoiceMessage
import com.arny.allfy.domain.usecase.post.AddComment
import com.arny.allfy.domain.usecase.post.DeletePost
import com.arny.allfy.domain.usecase.post.GetComments
import com.arny.allfy.domain.usecase.post.GetFeedPosts
import com.arny.allfy.domain.usecase.post.GetPostByID
import com.arny.allfy.domain.usecase.post.GetPostsByIDs
import com.arny.allfy.domain.usecase.post.LogPostView
import com.arny.allfy.domain.usecase.post.PostUseCases
import com.arny.allfy.domain.usecase.post.ToggleLikeComment
import com.arny.allfy.domain.usecase.post.ToggleLikePost
import com.arny.allfy.domain.usecase.post.UploadPost
import com.arny.allfy.domain.usecase.user.CheckIfFollowing
import com.arny.allfy.domain.usecase.user.FollowUser
import com.arny.allfy.domain.usecase.user.GetFollowersCount
import com.arny.allfy.domain.usecase.user.GetFollowers
import com.arny.allfy.domain.usecase.user.GetFollowingCount
import com.arny.allfy.domain.usecase.user.GetPostIds
import com.arny.allfy.domain.usecase.user.GetUserDetails
import com.arny.allfy.domain.usecase.user.GetUsersByIDs
import com.arny.allfy.domain.usecase.user.SetUserDetails
import com.arny.allfy.domain.usecase.user.UnfollowUser
import com.arny.allfy.domain.usecase.user.UserUseCases
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AllfyModule {
    @Singleton
    @Provides
    fun provideAuthentication(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Singleton
    @Provides
    fun provideFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Singleton
    @Provides
    fun provideFirebaseDatabase(): FirebaseDatabase {
        return FirebaseDatabase.getInstance()
    }

    @Singleton
    @Provides
    fun provideStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    @Singleton
    @Provides
    fun provideFunctions(): FirebaseFunctions {
        return FirebaseFunctions.getInstance()
    }

    @Provides
    @Singleton
    fun provideGoogleAuthClient(
        @ApplicationContext context: Context
    ): GoogleAuthClient {
        return GoogleAuthClient(context)
    }

    @Singleton
    @Provides
    fun provideAuthenticationRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
    ): AuthenticationRepository {
        return AuthenticationRepositoryImpl(auth, firestore)
    }

    @Singleton
    @Provides
    fun provideUserRepository(
        firestore: FirebaseFirestore,
        storage: FirebaseStorage
    ): UserRepository {
        return UserRepositoryImpl(firestore, storage)
    }

    @Singleton
    @Provides
    fun providePostRepository(
        firestore: FirebaseFirestore,
        storage: FirebaseStorage,
        @ApplicationContext context: Context
    ): PostRepository {
        return PostRepositoryImpl(firestore, storage, context)
    }

    @Singleton
    @Provides
    fun provideMessageRepository(
        firebaseDatabase: FirebaseDatabase,
        storage: FirebaseStorage
    ): MessageRepository {
        return MessageRepositoryImpl(firebaseDatabase, storage)
    }

    @Singleton
    @Provides
    fun provideAuthUseCases(
        repositoryImpl: AuthenticationRepositoryImpl
    ) = AuthenticationUseCases(
        isAuthenticated = IsAuthenticated(repositoryImpl),
        signOut = SignOut(repositoryImpl),
        signInWithEmail = SignInWithEmail(repositoryImpl),
        signUp = SignUp(repositoryImpl),
        signInWithGoogle = SignInWithGoogle(repositoryImpl),
        getCurrentUserId = GetCurrentUserId(repositoryImpl)
    )

    @Singleton
    @Provides
    fun provideUserUseCases(repository: UserRepository) = UserUseCases(
        getUserDetails = GetUserDetails(repository),
        setUserDetails = SetUserDetails(repository),
        followUser = FollowUser(repository),
        unfollowUser = UnfollowUser(repository),
        getUsersByIDs = GetUsersByIDs(repository),
        getFollowers = GetFollowers(repository),
        getFollowingCount = GetFollowingCount(repository),
        getFollowersCount = GetFollowersCount(repository),
        getPostIds = GetPostIds(repository),
        checkIfFollowing = CheckIfFollowing(repository)
    )


    @Singleton
    @Provides
    fun providePostUseCases(repository: PostRepository) = PostUseCases(
        getFeedPosts = GetFeedPosts(repository),
        uploadPost = UploadPost(repository),
        deletePost = DeletePost(repository),
        getPostByID = GetPostByID(repository),
        getPostsByIds = GetPostsByIDs(repository),
        toggleLikePost = ToggleLikePost(repository),
        addComment = AddComment(repository),
        getComments = GetComments(repository),
        toggleLikeComment = ToggleLikeComment(repository),
        logPostView = LogPostView(repository)
    )

    @Singleton
    @Provides
    fun provideMessageUseCases(repository: MessageRepository) = MessageUseCases(
        sendMessage = SendMessage(repository),
        sendImages = SendImages(repository),
        getMessages = GetMessages(repository),
        initializeConversation = InitializeConversation(repository),
        markMessageAsRead = MarkMessageAsRead(repository),
        loadConversations = LoadConversations(repository),
        sendVoiceMessage = SendVoiceMessage(repository),
        deleteMessage = DeleteMessage(repository)
    )

    @Provides
    @Singleton
    fun provideCallRepository(firebaseDatabase: FirebaseDatabase): CallRepository {
        return CallRepositoryImpl(firebaseDatabase)
    }
}