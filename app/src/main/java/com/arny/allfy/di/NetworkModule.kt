package com.arny.allfy.di

import com.arny.allfy.data.remote.RecommendationApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideRecommendationApi(): RecommendationApi {
        return Retrofit.Builder()
            .baseUrl("https://proper-lenient-wallaby.ngrok-free.app/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RecommendationApi::class.java)
    }
}