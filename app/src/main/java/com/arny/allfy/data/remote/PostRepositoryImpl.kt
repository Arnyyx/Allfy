package com.arny.allfy.data.remote

import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.repository.PostRepository
import com.arny.allfy.utils.Constants
import com.arny.allfy.utils.Response
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : PostRepository {
    override fun getAllPosts(userID: String): Flow<Response<List<Post>>> = callbackFlow {
        Response.Loading
        val snapshotListener =
            firestore.collection("posts").whereNotEqualTo("userId", userID)
                .addSnapshotListener { snapshot, e ->
                    val response = if (snapshot != null) {
                        val posts = snapshot.toObjects(Post::class.java)
                        Response.Success<List<Post>>(posts)
                    } else {
                        Response.Error(e?.message ?: e.toString())
                    }
                    trySend(response).isSuccess
                }
        awaitClose {
            snapshotListener.remove()
        }

    }

    override fun uploadPost(post: Post): Flow<Response<Boolean>> = flow {
        try {
            val postID = firestore.collection("posts").document().id
            firestore.collection(Constants.COLLECTION_NAME_POSTS).document(postID).set(post).await()
            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }
}