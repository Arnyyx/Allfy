package com.arny.allfy.data.remote

import com.arny.allfy.data.mapper.MessageMapper
import com.arny.allfy.data.model.MessageEntity
import com.arny.allfy.domain.model.Conversation
import com.arny.allfy.domain.model.Message
import com.arny.allfy.domain.model.User
import com.arny.allfy.domain.repository.MessageRepository
import com.arny.allfy.utils.Response
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val messageMapper: MessageMapper,
) : MessageRepository {

    override fun loadConversations(userId: String): Flow<Response<List<Conversation>>> =
        callbackFlow {
            trySend(Response.Loading)
            val conversationRef = firebaseDatabase.reference.child("conversations")

            val listener = conversationRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val conversations = snapshot.children.mapNotNull { convoSnapshot ->
                        val participants =
                            convoSnapshot.child("participants").children.map { it.value as String }
                        if (participants.contains(userId) && participants.size == 2) {
                            val otherUserId =
                                participants.firstOrNull { it != userId } ?: return@mapNotNull null
                            val conversationId = convoSnapshot.key ?: return@mapNotNull null
                            val lastMessage =
                                convoSnapshot.child("lastMessage").getValue(Message::class.java)
                            val timestamp =
                                convoSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                            Conversation(
                                id = conversationId,
                                otherUserID = otherUserId,
                                lastMessage = lastMessage,
                                timestamp = timestamp
                            )
                        } else null
                    }.sortedByDescending { it.timestamp }

                    trySend(Response.Success(conversations))
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(Response.Error(error.message))
                    close(error.toException())
                }
            })

            awaitClose { conversationRef.removeEventListener(listener) }
        }


    override fun sendMessage(conversationID: String, message: Message): Flow<Response<Boolean>> =
        flow {
            emit(Response.Loading)
            try {
                val messageEntity = messageMapper.toEntity(message)
                val messageRef = firebaseDatabase.reference
                    .child("conversations")
                    .child(conversationID)
                    .child("messages")
                    .push()

                messageEntity.id = messageRef.key ?: ""

                firebaseDatabase.reference
                    .child("conversations")
                    .child(conversationID).updateChildren(
                        mapOf("lastMessage" to messageEntity)
                    ).await()

                messageRef.setValue(messageEntity).await()
                emit(Response.Success(true))
            } catch (e: Exception) {
                emit(Response.Error(e.message ?: "Unknown error"))
            }
        }

    override fun getMessagesByConversationId(
        conversationID: String,
    ): Flow<List<Message>> = callbackFlow {
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
                    val messages = snapshot.children.mapNotNull {
                        it.getValue(MessageEntity::class.java)?.let { entity ->
                            messageMapper.toDomain(entity)
                        }
                    }
                    trySend(messages)
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            })

        awaitClose {
            messagesRef.removeEventListener(listener)
        }
    }

    override suspend fun markMessageAsRead(messageId: String): Result<Unit> {
        return try {
            firebaseDatabase.reference
                .child("messages")
                .child(messageId)
                .child("isRead")
                .setValue(true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            firebaseDatabase.reference
                .child("messages")
                .child(messageId)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getOrCreateConversation(
        currentUserId: String,
        recipientId: String
    ): Flow<Response<Conversation>> = flow {
        emit(Response.Loading)
        try {
            val participants = listOf(currentUserId, recipientId)
            val conversationId = createConversationId(participants)

            val conversationRef = firebaseDatabase.reference
                .child("conversations")
                .child(conversationId)
            val snapshot = conversationRef.get().await()

            if (!snapshot.exists()) {
                val newConversation = mapOf(
                    "participants" to participants.map { it },
                    "createdAt" to ServerValue.TIMESTAMP
                )
                conversationRef.setValue(newConversation).await()
            }
            val conversation = Conversation(
                id = conversationId,
                otherUserID = recipientId
            )

            emit(Response.Success(conversation))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: "Unknown error"))
        }
    }

    private fun createConversationId(participants: List<String>): String {
        return participants.sorted().joinToString("_")
    }
}
