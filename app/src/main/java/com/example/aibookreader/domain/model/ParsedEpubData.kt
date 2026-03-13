package com.example.aibookreader.domain.model

data class ParsedEpubData (
    val title: String?,
    val author: String?,
    val coverImageBytes: ByteArray?,
    val pages: List<String>,
    val fullText: String
)