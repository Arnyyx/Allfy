package com.arny.allfy.domain.usecase.Post

class PostUseCases(
    val getAllPosts: GetAllPosts,
    val uploadPost: UploadPost,
    val getPostByID: GetPostByID,
    val toggleLikePost: ToggleLikePostUseCase
) {
}