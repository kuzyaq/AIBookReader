package com.example.aibookreader.presentation.screens.reader

data class PendingAiRetry(
    val prompt: String,
    val userMessage: String
)

data class AiChatErrorUi(
    val message: String,
    val retry: PendingAiRetry
)
