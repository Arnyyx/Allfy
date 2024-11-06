package com.arny.allfy.data.mapper

import com.arny.allfy.data.model.ConversationEntity
import com.arny.allfy.domain.model.Conversation
import com.arny.allfy.domain.model.Message
import javax.inject.Inject

class ConversationMapper @Inject constructor() {
    fun toEntity(conversation: Conversation): ConversationEntity {
        return ConversationEntity(
            id = conversation.id,
            participantId = conversation.participantId,
            participantName = conversation.participantName,
            participantAvatar = conversation.participantAvatar,
            lastMessageId = conversation.lastMessage?.id ?: "",
            unreadCount = conversation.unreadCount,
            timestamp = conversation.timestamp
        )
    }

    fun toDomain(entity: ConversationEntity, lastMessage: Message?): Conversation {
        return Conversation(
            id = entity.id,
            participantId = entity.participantId,
            participantName = entity.participantName,
            participantAvatar = entity.participantAvatar,
            lastMessage = lastMessage,
            unreadCount = entity.unreadCount,
            timestamp = entity.timestamp
        )
    }
}