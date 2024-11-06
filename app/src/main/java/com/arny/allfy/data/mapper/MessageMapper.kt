package com.arny.allfy.data.mapper

import com.arny.allfy.data.model.MessageEntity
import com.arny.allfy.domain.model.Message
import com.arny.allfy.domain.model.MessageType
import javax.inject.Inject

class MessageMapper @Inject constructor() {
    fun toEntity(message: Message): MessageEntity {
        return MessageEntity(
            id = message.id,
            senderId = message.senderId,
            receiverId = message.receiverId,
            content = message.content,
            timestamp = message.timestamp,
            isRead = message.isRead,
            type = message.type.name
        )
    }

    fun toDomain(entity: MessageEntity): Message {
        return Message(
            id = entity.id,
            senderId = entity.senderId,
            receiverId = entity.receiverId,
            content = entity.content,
            timestamp = entity.timestamp,
            isRead = entity.isRead,
            type = MessageType.valueOf(entity.type)
        )
    }
}
