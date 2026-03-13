package com.example.aibookreader.domain.model

sealed class ReaderBlock {

    data class Paragraph(
        val text: String
    ) : ReaderBlock()

    data class Title(
        val text: String,
        val level: Int
    ) : ReaderBlock()

    data class Image(
        val src: String
    ) : ReaderBlock()

    data class Quote(
        val text: String
    ) : ReaderBlock()

}