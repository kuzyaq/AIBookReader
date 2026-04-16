package com.example.aibookreader.presentation.screens.addbook

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File

@Composable
fun AddBookScreen(viewModel: AddBookViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var selectedSection by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.importEvents.collect { event ->
            when (event) {
                is ImportEvent.Started -> snackbarHostState.showSnackbar(
                    message = "Импорт книги начался...",
                    duration = SnackbarDuration.Short
                )
                is ImportEvent.Success -> snackbarHostState.showSnackbar(
                    message = "Книга успешно импортирована!",
                    duration = SnackbarDuration.Short
                )
                is ImportEvent.Error -> snackbarHostState.showSnackbar(
                    message = "Ошибка импорта: ${event.message}",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    val pickBookLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
        copyUriToCache(uri, context)?.let { viewModel.importBook(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {

        // Сегментированные кнопки
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            SegmentedButton(
                selected = selectedSection == 0,
                onClick = { selectedSection = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = {
                    Icon(Icons.Default.PhoneAndroid, null, modifier = Modifier.size(16.dp))
                }
            ) {
                Text("С устройства")
            }
            SegmentedButton(
                selected = selectedSection == 1,
                onClick = { selectedSection = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = {
                    Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(16.dp))
                }
            ) {
                Text("Из библиотеки")
            }
        }

        // ── Контент секции ───────────────────────────────────────
        when (selectedSection) {
            0 -> LocalFileSection(onPickFile = {
                pickBookLauncher.launch(
                    arrayOf("application/epub+zip", "application/pdf")
                )
            })

            1 -> ServerBooksSection()
        }
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter)
    )
    }
}

// Секция: загрузка с устройства

@Composable
private fun LocalFileSection(onPickFile: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Upload,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Загрузить книгу",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Поддерживаются форматы EPUB и PDF",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onPickFile,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            Icon(Icons.Default.FolderOpen, null)
            Spacer(Modifier.width(10.dp))
            Text("Выбрать файл", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// Секция: готовые книги с сервера

@Composable
private fun ServerBooksSection() {
    // Заглушка
    val mockBooks = listOf(
        "Преступление и наказание" to "Фёдор Достоевский",
        "Война и мир" to "Лев Толстой",
        "Мастер и Маргарита" to "Михаил Булгаков",
        "Отцы и дети" to "Иван Тургенев",
        "Анна Каренина" to "Лев Толстой"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Классическая литература",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(mockBooks.size) { index ->
                val (title, author) = mockBooks[index]
                ServerBookItem(
                    title = title,
                    author = author,
                    onDownload = { /* TODO: вызов API */ }
                )
            }
        }
    }
}

@Composable
private fun ServerBookItem(
    title: String,
    author: String,
    onDownload: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    author,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }

            FilledTonalIconButton(
                onClick = onDownload,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// Вспомогательная функция

private fun copyUriToCache(uri: Uri, context: android.content.Context): String? {
    return try {
        val mime = context.contentResolver.getType(uri)
        val ext = when (mime) {
            "application/epub+zip" -> "epub"
            "application/pdf" -> "pdf"
            else -> "tmp"
        }
        val file = File(context.cacheDir, "import_${System.currentTimeMillis()}.$ext")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        file.absolutePath
    } catch (_: Exception) {
        null
    }
}