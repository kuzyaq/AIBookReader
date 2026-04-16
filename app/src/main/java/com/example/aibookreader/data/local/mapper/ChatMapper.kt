package com.example.aibookreader.data.local.mapper

import com.example.aibookreader.data.local.entity.ChatMessageEntity
import com.example.aibookreader.domain.model.ChatMessage

object ChatMapper {

    fun toDomain(entity: ChatMessageEntity): ChatMessage {
        return ChatMessage(
            id = entity.id,
            bookId = entity.bookId,
            message = entity.message,
            isUser = entity.isUser,
            timeStamp = entity.timestamp
        )
    }

    fun toEntity(domain: ChatMessage): ChatMessageEntity {
        return ChatMessageEntity(
            id = domain.id,
            bookId = domain.bookId,
            message = domain.message,
            isUser = domain.isUser,
            timestamp = domain.timeStamp
        )
    }

    fun toDomainList(entities: List<ChatMessageEntity>): List<ChatMessage> {
        return entities.map {toDomain(it)}
    }
}