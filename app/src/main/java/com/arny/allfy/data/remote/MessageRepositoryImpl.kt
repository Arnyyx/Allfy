package com.arny.allfy.data.remote

import com.arny.allfy.data.mapper.MessageMapper
import com.arny.allfy.data.model.MessageEntity
import com.arny.allfy.domain.model.Message
import com.arny.allfy.domain.repository.MessageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val messageMapper: MessageMapper
) : MessageRepository {

    override suspend fun sendMessage(message: Message): Result<Unit> {
        return try {
            val messageEntity = messageMapper.toEntity(message)
            firebaseDatabase.reference
                .child("messages")
                .child(message.id)
                .setValue(messageEntity)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getMessages(
        senderId: String,
        receiverId: String
    ): Flow<List<Message>> = callbackFlow {
        val messagesRef = firebaseDatabase.reference.child("messages")

        val listener = messagesRef
            .orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = snapshot.children.mapNotNull {
                        it.getValue(MessageEntity::class.java)?.let { entity ->
                            messageMapper.toDomain(entity)
                        }
                    }.filter { message ->
                        (message.senderId == senderId && message.receiverId == receiverId) ||
                                (message.senderId == receiverId && message.receiverId == senderId)
                    }.sortedBy { it.timestamp }

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
}
