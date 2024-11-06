package com.arny.allfy.data.remote

import android.util.Log
import com.arny.allfy.data.mapper.ConversationMapper
import com.arny.allfy.data.model.ConversationEntity
import com.arny.allfy.domain.model.Conversation
import com.arny.allfy.domain.repository.ConversationRepository
import com.arny.allfy.domain.repository.MessageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ConversationRepositoryImpl @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val conversationMapper: ConversationMapper,
    private val messageRepository: MessageRepository
) : ConversationRepository {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        ?: throw IllegalStateException("Current user ID not found")

    override fun getConversations(): Flow<List<Conversation>> = callbackFlow {
        Log.d("DEBUG", "Start")
        val conversationsRef = firebaseDatabase.reference
            .child("conversations")
            .child(currentUserId)

        val listener = conversationsRef
            .orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    launch {
                        if (!snapshot.exists()) {
                            Log.d("DEBUG", "Conversations node does not exist")
                            trySend(emptyList())
                            return@launch
                        }

                        if (!snapshot.hasChildren()) {
                            Log.d("DEBUG", "Conversations node exists but empty")
                            trySend(emptyList())
                            return@launch
                        }

                        val conversations = snapshot.children.mapNotNull { child ->
                            try {
                                val entity = child.getValue(ConversationEntity::class.java)
                                entity?.let { e ->
                                    val lastMessage = messageRepository.getMessages(
                                        senderId = currentUserId,
                                        receiverId = e.participantId
                                    ).firstOrNull()?.lastOrNull()

                                    conversationMapper.toDomain(e, lastMessage)
                                }
                            } catch (e: Exception) {
                                Log.d("DEBUG", "Error processing conversation: ${e.message}")
                                null
                            }
                        }.sortedByDescending { it.timestamp }

                        if (conversations.isEmpty()) {
                            Log.d("DEBUG", "No valid conversations after processing")
                        }

                        trySend(conversations)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("DEBUG", "Firebase query cancelled: ${error.message}")
                    close(error.toException())
                }
            })

        awaitClose {
            conversationsRef.removeEventListener(listener)
        }
    }


    override suspend fun getConversationByParticipant(
        participantId: String
    ): Result<Conversation> {
        return try {
            val conversationSnapshot = firebaseDatabase.reference
                .child("conversations")
                .child(currentUserId)
                .child(participantId)
                .get()
                .await()

            val conversationEntity = conversationSnapshot.getValue(ConversationEntity::class.java)

            if (conversationEntity != null) {
                val lastMessage = messageRepository.getMessages(
                    senderId = currentUserId,
                    receiverId = participantId
                ).firstOrNull()?.lastOrNull()

                Result.success(conversationMapper.toDomain(conversationEntity, lastMessage))
            } else {
                Result.failure(NoSuchElementException("Conversation not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateConversation(
        conversation: Conversation
    ): Result<Unit> {
        return try {
            val conversationEntity = conversationMapper.toEntity(conversation)

            firebaseDatabase.reference
                .child("conversations")
                .child(currentUserId)
                .child(conversation.participantId)
                .setValue(conversationEntity)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
