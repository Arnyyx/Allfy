package com.arny.allfy.data.remote

import android.net.Uri
import com.arny.allfy.domain.model.Conversation
import com.arny.allfy.domain.model.Message
import com.arny.allfy.domain.model.MessageType
import com.arny.allfy.domain.repository.MessageRepository
import com.arny.allfy.utils.Response
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val storage: FirebaseStorage,
) : MessageRepository {

    override suspend fun getMessages(conversationID: String): Flow<List<Message>> = callbackFlow {
        val messagesRef = firebaseDatabase.reference
            .child("conversations")
            .child(conversationID)
            .child("messages")

        val listener = messagesRef
            .orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        trySend(emptyList())
                        return
                    }
                    val messages = snapshot.children.mapNotNull { messageSnapshot ->
                        messageSnapshot.getValue(Message::class.java)?.apply {
                            type = MessageType.valueOf(
                                messageSnapshot.child("type").getValue(String::class.java) ?: "TEXT"
                            )
                        }
                    }
                    trySend(messages)
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(emptyList())
                }
            })

        awaitClose {
            messagesRef.removeEventListener(listener)
        }
    }

    override suspend fun loadConversations(userId: String): Flow<Response<List<Conversation>>> =
        callbackFlow {
            trySend(Response.Loading)
            val conversationRef = firebaseDatabase.reference.child("conversations")

            val listener = conversationRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val conversations = snapshot.children.mapNotNull { convoSnapshot ->
                        val participants = convoSnapshot.child("participants")
                            .children.map { it.value as String }
                        if (!participants.contains(userId)) return@mapNotNull null

                        val conversationId = convoSnapshot.key ?: return@mapNotNull null
                        val lastMessageSnapshot = convoSnapshot.child("lastMessage")
                        val lastMessage = lastMessageSnapshot.getValue(Message::class.java)?.apply {
                            type = MessageType.valueOf(
                                lastMessageSnapshot.child("type").getValue(String::class.java)
                                    ?: "TEXT"
                            )
                        }
                        val timestamp =
                            convoSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                        val createdAt =
                            convoSnapshot.child("createdAt").getValue(Long::class.java) ?: 0L
                        val unreadCountSnapshot = convoSnapshot.child("unreadCount")
                        val unreadCount = unreadCountSnapshot.children.associate {
                            it.key!! to (it.getValue(Int::class.java) ?: 0)
                        }

                        Conversation(
                            id = conversationId,
                            participants = participants,
                            lastMessage = lastMessage,
                            unreadCount = unreadCount,
                            timestamp = timestamp,
                            createdAt = createdAt
                        )
                    }.sortedByDescending { it.timestamp }

                    trySend(Response.Success(conversations))
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(Response.Error(error.message))
//                    close(error.toException())
                }
            })
            awaitClose { conversationRef.removeEventListener(listener) }
        }

    override suspend fun sendMessage(
        conversationID: String,
        message: Message
    ): Flow<Response<Boolean>> =
        flow {
            emit(Response.Loading)
            try {
                val messageRef = firebaseDatabase.reference
                    .child("conversations")
                    .child(conversationID)
                    .child("messages")
                    .push()

                val messageWithId = message.copy(
                    id = messageRef.key ?: throw Exception("Failed to generate message ID")
                )

                firebaseDatabase.reference
                    .child("conversations")
                    .child(conversationID)
                    .updateChildren(
                        mapOf(
                            "lastMessage" to messageWithId,
                            "timestamp" to ServerValue.TIMESTAMP
                        )
                    ).await()

                messageRef.setValue(messageWithId).await()
                emit(Response.Success(true))
            } catch (e: Exception) {
                emit(Response.Error(e.message ?: "Unknown error"))
            }
        }

    override suspend fun sendImages(
        conversationID: String,
        imageUris: List<Uri>
    ): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            val imageUrls = mutableListOf<String>()
            val senderId = FirebaseAuth.getInstance().currentUser?.uid
                ?: throw Exception("User not authenticated")
            for (uri in imageUris) {
                val storageRef =
                    storage.reference.child("chat_images/${System.currentTimeMillis()}_${uri.lastPathSegment}")
                val uploadTask = storageRef.putFile(uri).await()
                val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
                imageUrls.add(downloadUrl)

                val message = Message(
                    id = "",
                    senderId = senderId,
                    content = downloadUrl,
                    timestamp = System.currentTimeMillis(),
                    type = MessageType.IMAGE
                )
                val messageRef = firebaseDatabase.reference
                    .child("conversations")
                    .child(conversationID)
                    .child("messages")
                    .push()

                val messageWithId = message.copy(
                    id = messageRef.key ?: throw Exception("Failed to generate message ID")
                )

                firebaseDatabase.reference
                    .child("conversations")
                    .child(conversationID)
                    .updateChildren(
                        mapOf(
                            "lastMessage" to messageWithId,
                            "timestamp" to ServerValue.TIMESTAMP
                        )
                    ).await()

                messageRef.setValue(messageWithId).await()
            }
            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: "Failed to upload images"))
        }
    }

    override suspend fun markMessageAsRead(
        conversationId: String,
        userId: String,
        messageId: String
    ): Result<Unit> {
        return try {
            firebaseDatabase.reference
                .child("conversations")
                .child(conversationId)
                .child("readStatus")
                .child(userId)
                .setValue(messageId)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> {
        return try {
            firebaseDatabase.reference
                .child("conversations")
                .child(conversationId)
                .child("messages")
                .child(messageId)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun initializeConversation(
        userIds: List<String>
    ): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            val conversationId = createConversationId(userIds)
            val conversationRef = firebaseDatabase.reference
                .child("conversations")
                .child(conversationId)

            val newConversation = mapOf(
                "participants" to userIds,
                "createdAt" to ServerValue.TIMESTAMP,
            )
            conversationRef.setValue(newConversation).await()


            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: "Unknown error"))
        }
    }

    override suspend fun sendVoiceMessage(
        conversationID: String,
        audioUri: Uri
    ): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            val senderId = FirebaseAuth.getInstance().currentUser?.uid
                ?: throw Exception("User not authenticated")

            val storageRef =
                storage.reference.child("chat_audio/${System.currentTimeMillis()}_${audioUri.lastPathSegment}")
            val uploadTask = storageRef.putFile(audioUri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

            val message = Message(
                id = "",
                senderId = senderId,
                content = downloadUrl,
                timestamp = System.currentTimeMillis(),
                type = MessageType.VOICE
            )

            val messageRef = firebaseDatabase.reference
                .child("conversations")
                .child(conversationID)
                .child("messages")
                .push()

            val messageWithId = message.copy(
                id = messageRef.key ?: throw Exception("Failed to generate message ID")
            )
            firebaseDatabase.reference
                .child("conversations")
                .child(conversationID)
                .updateChildren(
                    mapOf(
                        "lastMessage" to messageWithId,
                        "timestamp" to ServerValue.TIMESTAMP
                    )
                ).await()

            messageRef.setValue(messageWithId).await()
            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: "Failed to send voice message"))
        }
    }

    private fun createConversationId(participants: List<String>): String {
        return participants.sorted().joinToString("_")
    }
}