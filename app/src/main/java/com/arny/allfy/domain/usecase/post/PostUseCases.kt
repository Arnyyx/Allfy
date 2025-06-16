package com.arny.allfy.domain.usecase.post

class PostUseCases(
    val getFeedPosts: GetFeedPosts,
    val uploadPost: UploadPost,
    val deletePost: DeletePost,
    val getPostByID: GetPostByID,
    val getPostsByIds: GetPostsByIDs,
    val getComments: GetComments,
    val addComment: AddComment,
    val toggleLikePost: ToggleLikePost,
    val toggleLikeComment: ToggleLikeComment,
    val logPostView: LogPostView,
    val editPost: EditPost
)