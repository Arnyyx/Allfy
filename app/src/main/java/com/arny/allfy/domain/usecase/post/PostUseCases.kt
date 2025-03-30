package com.arny.allfy.domain.usecase.post

class PostUseCases(
    val getFeedPosts: GetFeedPosts,
    val uploadPost: UploadPost,
    val deletePost: DeletePost,
    val getPostByID: GetPostByID,
    val getPostsByIds: GetPostsByIDs,
    val toggleLikePost: ToggleLikePost,
    val getComments: GetComments,
    val addComment: AddComment,
    val toggleLikeComment: ToggleLikeComment
)