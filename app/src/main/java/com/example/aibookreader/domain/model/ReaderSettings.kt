package com.example.aibookreader.domain.model

data class ReaderSettings(
    val fontSize: Float = 18f,
    val titleFontSize: Float = 28f,
    val lineHeightMultiplier: Float = 1.5f,
    val paragraphSpacing: Float = 8f
) {
    companion object {
        const val MIN_FONT_SIZE = 12f
        const val MAX_FONT_SIZE = 32f
        const val MIN_LINE_HEIGHT = 1.0f
        const val MAX_LINE_HEIGHT = 2.5f
    }
}
