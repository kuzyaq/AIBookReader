package com.example.aibookreader.domain.model

data class PendingAiFailure(
    val prompt: String,
    val userMessage: String,
    val errorMessage: String
)
