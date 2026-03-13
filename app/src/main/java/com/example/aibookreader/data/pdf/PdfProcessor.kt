package com.example.aibookreader.data.pdf

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface PdfProcessor {
    data class PdfData(
        val title: String?,
        val author: String?,
        val content: String?,
        val totalPages: Int,
        val coverImagePath: String? = null
    )

    suspend fun processPdf(filePath: String, context: Context): PdfData
}

class PdfProcessorImpl @Inject constructor() : PdfProcessor {

    companion object {
        private const val TAG = "PdfProcessorImpl"
    }

    override suspend fun processPdf(
        filePath: String,
        context: Context
    ): PdfProcessor.PdfData = withContext(Dispatchers.IO) {
        PdfProcessor.PdfData(
            title = null,
            author = null,
            content = "",
            totalPages = 1,
            coverImagePath = null
        )
    }
}