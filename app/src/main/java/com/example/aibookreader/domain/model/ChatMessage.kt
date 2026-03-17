package com.example.aibookreader.domain.model

data class ChatMessage(
    val id: Int = 0,
    val bookId: Int,
    val message: String,
    val isUser: Boolean,
    val timeStamp: Long = System.currentTimeMillis()
)