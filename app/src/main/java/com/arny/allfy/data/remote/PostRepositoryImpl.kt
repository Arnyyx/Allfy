package com.arny.allfy.data.remote

import android.net.Uri
import android.util.Log
import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.model.User
import com.arny.allfy.domain.repository.PostRepository
import com.arny.allfy.utils.Constants
import com.arny.allfy.utils.Response
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : PostRepository {

    override fun getAllPosts(
        userID: String,
        lastVisible: Post?,
        limit: Int
    ): Flow<Response<List<Post>>> = flow {
        emit(Response.Loading)
        try {
            val query = firestore.collection(Constants.COLLECTION_NAME_POSTS)
                .whereNotEqualTo("userID", userID)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())

            val finalQuery = if (lastVisible != null) {
                query.startAfter(lastVisible.timestamp)
            } else {
                query
            }

            val snapshot = finalQuery.get().await()
            val posts = snapshot.toObjects(Post::class.java)
            emit(Response.Success(posts))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun uploadPost(post: Post, imageUris: List<Uri>): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            val postID = firestore.collection(Constants.COLLECTION_NAME_POSTS).document().id

            val imageUrls = mutableListOf<String>()
            for (uri in imageUris) {
                val imageUrl = uploadImageToFirebase(postID, uri)
                imageUrls.add(imageUrl)
            }

            val postWithImages = post.copy(imageUrls = imageUrls)

            firestore.collection(Constants.COLLECTION_NAME_POSTS)
                .document(postID)
                .set(postWithImages.copy(id = postID))
                .await()

            firestore.collection(Constants.COLLECTION_NAME_USERS).document(post.userID)
                .update("postsIDs", FieldValue.arrayUnion(postID))
                .await()

            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun getPostByID(postID: String): Flow<Response<Post>> = callbackFlow {
        Response.Loading
        val snapshotListener =
            firestore.collection(Constants.COLLECTION_NAME_POSTS).document(postID)
                .addSnapshotListener { snapshot, e ->
                    val response = if (snapshot != null) {
                        val post = snapshot.toObject(Post::class.java)
                        Response.Success(post!!)
                    } else {
                        Response.Error(e?.message ?: e.toString())
                    }
                    trySend(response).isSuccess
                }
        awaitClose {
            snapshotListener.remove()
        }
    }

    override suspend fun updatePost(post: Post) {
        try {
            firestore.collection(Constants.COLLECTION_NAME_POSTS)
                .document(post.id) // Sử dụng ID của post để xác định tài liệu cần cập nhật
                .set(post) // Cập nhật toàn bộ tài liệu với dữ liệu từ đối tượng post
                .await() // Chờ cho quá trình cập nhật hoàn thành
            Log.d("PostRepositoryImpl", "Post updated successfully")
        } catch (e: Exception) {
            Log.e("PostRepositoryImpl", "Error updating post: ${e.localizedMessage}")
            throw e // Ném ngoại lệ để xử lý tiếp tục ở nơi gọi hàm nếu cần
        }
    }


    private suspend fun uploadImageToFirebase(postID: String, uri: Uri): String {
        try {
            val storageRef =
                storage.reference.child(Constants.COLLECTION_NAME_POSTS + "/$postID/${System.currentTimeMillis()}_${uri.lastPathSegment}")
            storageRef.putFile(uri).await()
            return storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("PostRepositoryImpl", "uploadImageToFirebase: $e")
            throw e
        }
    }
}