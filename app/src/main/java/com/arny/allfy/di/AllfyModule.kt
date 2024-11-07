package com.arny.allfy.di

import com.arny.allfy.data.mapper.MessageMapper
import com.arny.allfy.data.remote.AuthenticationRepositoryImpl
import com.arny.allfy.data.remote.MessageRepositoryImpl
import com.arny.allfy.data.remote.PostRepositoryImpl
import com.arny.allfy.data.remote.UserRepositoryImpl
import com.arny.allfy.domain.repository.AuthenticationRepository
import com.arny.allfy.domain.repository.MessageRepository
import com.arny.allfy.domain.repository.PostRepository
import com.arny.allfy.domain.repository.UserRepository
import com.arny.allfy.domain.usecase.authentication.AuthenticationUseCases
import com.arny.allfy.domain.usecase.authentication.FirebaseAuthState
import com.arny.allfy.domain.usecase.authentication.FirebaseSignIn
import com.arny.allfy.domain.usecase.authentication.FirebaseSignOut
import com.arny.allfy.domain.usecase.authentication.FirebaseSignUp
import com.arny.allfy.domain.usecase.authentication.GetCurrentUserID
import com.arny.allfy.domain.usecase.authentication.IsUserAuthenticated
import com.arny.allfy.domain.usecase.message.GetOrCreateConversationUseCase
import com.arny.allfy.domain.usecase.message.MarkMessageAsReadUseCase
import com.arny.allfy.domain.usecase.message.SendMessageUseCase
import com.arny.allfy.domain.usecase.post.AddComment
import com.arny.allfy.domain.usecase.post.GetComments
import com.arny.allfy.domain.usecase.post.GetFeedPosts
import com.arny.allfy.domain.usecase.post.GetPostByID
import com.arny.allfy.domain.usecase.post.PostUseCases
import com.arny.allfy.domain.usecase.post.ToggleLikePost
import com.arny.allfy.domain.usecase.post.UploadPost
import com.arny.allfy.domain.usecase.user.FollowUserUseCase
import com.arny.allfy.domain.usecase.user.GetFollowersUseCase
import com.arny.allfy.domain.usecase.user.GetUserDetails
import com.arny.allfy.domain.usecase.user.GetUsersByIDsUseCase
import com.arny.allfy.domain.usecase.user.SetUserDetails
import com.arny.allfy.domain.usecase.user.UnfollowUserUseCase
import com.arny.allfy.domain.usecase.user.UserUseCases
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
    fun provideStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
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
    fun provideAuthUseCases(
        repositoryImpl: AuthenticationRepositoryImpl
    ) = AuthenticationUseCases(
        isUserAuthenticated = IsUserAuthenticated(repositoryImpl),
        firebaseAuthState = FirebaseAuthState(repositoryImpl),
        firebaseSignOut = FirebaseSignOut(repositoryImpl),
        firebaseSignIn = FirebaseSignIn(repositoryImpl),
        firebaseSignUp = FirebaseSignUp(repositoryImpl),
        getCurrentUserID = GetCurrentUserID(repositoryImpl)
    )

    @Singleton
    @Provides
    fun provideUserUseCases(repository: UserRepository) = UserUseCases(
        getUserDetails = GetUserDetails(repository),
        setUserDetails = SetUserDetails(repository),
        followUser = FollowUserUseCase(repository),
        unfollowUser = UnfollowUserUseCase(repository),
        getFollowers = GetFollowersUseCase(repository),
        getUsersByIDs = GetUsersByIDsUseCase(repository)
    )

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
        storage: FirebaseStorage
    ): PostRepository {
        return PostRepositoryImpl(firestore, storage)
    }

    @Singleton
    @Provides
    fun providePostUseCases(repository: PostRepository) = PostUseCases(
        getFeedPosts = GetFeedPosts(repository),
        uploadPost = UploadPost(repository),
        getPostByID = GetPostByID(repository),
        toggleLikePost = ToggleLikePost(repository),
        addComment = AddComment(repository),
        getComments = GetComments(repository)
    )


    @Singleton
    @Provides
    fun provideFirebaseDatabase(): FirebaseDatabase {
        return FirebaseDatabase.getInstance()
    }

    @Provides
    @Singleton
    fun provideMessageRepository(
        firebaseDatabase: FirebaseDatabase,
        messageMapper: MessageMapper
    ): MessageRepository {
        return MessageRepositoryImpl(firebaseDatabase, messageMapper)
    }

    @Singleton
    @Provides
    fun provideSendMessageUseCase(
        messageRepository: MessageRepository
    ): SendMessageUseCase {
        return SendMessageUseCase(messageRepository)
    }

    @Singleton
    @Provides
    fun provideMarkMessageAsReadUseCase(
        messageRepository: MessageRepository
    ): MarkMessageAsReadUseCase {
        return MarkMessageAsReadUseCase(messageRepository)
    }

    @Provides
    @Singleton
    fun provideGetOrCreateConversationUseCase(
        messageRepository: MessageRepository
    ): GetOrCreateConversationUseCase {
        return GetOrCreateConversationUseCase(messageRepository)
    }
}