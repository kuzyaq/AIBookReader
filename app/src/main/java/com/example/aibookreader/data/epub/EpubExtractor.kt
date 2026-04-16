package com.example.aibookreader.data.epub

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject

class EpubExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun extract(epubFilePath: String): File = withContext(Dispatchers.IO) {
        val epubFile = File(epubFilePath)
        val destDir = File(context.filesDir, "epub_books/${epubFile.nameWithoutExtension}_${System.currentTimeMillis()}")

        if (destDir.exists()) destDir.deleteRecursively()
        destDir.mkdirs()

        ZipInputStream(FileInputStream(epubFile)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { out -> zip.copyTo(out) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        destDir
    }
}
