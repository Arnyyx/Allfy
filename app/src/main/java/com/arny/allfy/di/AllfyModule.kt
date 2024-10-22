package com.arny.allfy.di

import com.arny.allfy.data.remote.AuthenticationRepositoryImpl
import com.arny.allfy.data.remote.PostRepositoryImpl
import com.arny.allfy.data.remote.UserRepositoryImpl
import com.arny.allfy.domain.repository.AuthenticationRepository
import com.arny.allfy.domain.repository.PostRepository
import com.arny.allfy.domain.repository.UserRepository
import com.arny.allfy.domain.usecase.Authentication.AuthenticationUseCases
import com.arny.allfy.domain.usecase.Authentication.FirebaseAuthState
import com.arny.allfy.domain.usecase.Authentication.FirebaseSignIn
import com.arny.allfy.domain.usecase.Authentication.FirebaseSignOut
import com.arny.allfy.domain.usecase.Authentication.FirebaseSignUp
import com.arny.allfy.domain.usecase.Authentication.IsUserAuthenticated
import com.arny.allfy.domain.usecase.Post.GetAllPosts
import com.arny.allfy.domain.usecase.Post.GetPost
import com.arny.allfy.domain.usecase.Post.PostUseCases
import com.arny.allfy.domain.usecase.Post.UploadPost
import com.arny.allfy.domain.usecase.User.GetUserDetails
import com.arny.allfy.domain.usecase.User.SetUserDetails
import com.arny.allfy.domain.usecase.User.UserUseCases
import com.google.firebase.auth.FirebaseAuth
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
        firebaseSignUp = FirebaseSignUp(repositoryImpl)
    )

    @Singleton
    @Provides
    fun provideUserUseCases(repository: UserRepository) = UserUseCases(
        getUserDetails = GetUserDetails(repository),
        setUserDetails = SetUserDetails(repository)
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
        getAllPosts = GetAllPosts(repository),
        uploadPost = UploadPost(repository),
        getPost = GetPost(repository)
    )
}