package com.example.aibookreader.presentation.screens.home.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.aibookreader.data.local.entity.BookStatus
import com.example.aibookreader.domain.model.Book
import com.valentinilk.shimmer.shimmer

/**
 * Карточка книги с удалением через SwipeToDismiss.
 *
 * Пользователь свайпает влево → появляется красный фон с иконкой корзины
 * → при полном свайпе показывается диалог подтверждения → книга удаляется.
 *
 * Это намного более интуитивно чем долгое нажатие, которое не имеет
 * визуального намёка на возможность удаления.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookCard(
    book        : Book,
    onClick     : () -> Unit,
    onDelete    : () -> Unit,
    modifier    : Modifier = Modifier
) {
    // Состояние диалога подтверждения удаления
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Состояние свайпа
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            // Свайп вправо/влево до конца → показываем диалог, сбрасываем свайп
            if (value == SwipeToDismissBoxValue.EndToStart) {
                showDeleteDialog = true
            }
            // Возвращаем false — карточка возвращается на место.
            // Реальное удаление происходит только через диалог подтверждения.
            false
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.4f }
    )

    // Диалог подтверждения удаления
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            bookTitle = book.title,
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    SwipeToDismissBox(
        state            = dismissState,
        modifier         = modifier,
        // Разрешаем свайп только влево (к удалению)
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            // Красный фон с иконкой корзины — появляется при свайпе влево
            val progress = dismissState.progress
            val triggered = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (triggered)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint     = if (triggered) Color.White
                        else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Удалить",
                        color      = if (triggered) Color.White
                        else MaterialTheme.colorScheme.onErrorContainer,
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) {
        // Сама карточка книги
        Card(
            onClick   = onClick,
            shape     = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors    = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            when (book.status) {
                BookStatus.IMPORTING -> ImportingBookContent()
                BookStatus.READY     -> ReadyBookContent(book)
                BookStatus.FAILED    -> FailedBookContent()
            }
        }
    }
}

// ─── Диалог подтверждения ─────────────────────────────────────────

@Composable
private fun DeleteConfirmationDialog(
    bookTitle : String,
    onConfirm : () -> Unit,
    onDismiss : () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.DeleteForever,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text("Удалить книгу?", fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                "«$bookTitle» будет удалена из библиотеки. " +
                        "История чата с ИИ по этой книге также будет удалена.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors  = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Удалить", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// ─── Состояние загрузки (shimmer) ────────────────────────────────

@Composable
private fun ImportingBookContent() {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(16.dp).shimmer(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(Modifier.size(200.dp, 16.dp).background(MaterialTheme.colorScheme.primaryContainer))
            Spacer(Modifier.height(8.dp))
            Box(Modifier.size(120.dp, 12.dp).background(MaterialTheme.colorScheme.primaryContainer))
            Spacer(Modifier.height(12.dp))
            Box(Modifier.size(230.dp, 4.dp).background(MaterialTheme.colorScheme.primaryContainer))
        }
    }
}

// ─── Готовая книга ────────────────────────────────────────────────

@Composable
private fun ReadyBookContent(book: Book) {
    Column(modifier = Modifier.fillMaxWidth()) {

        Row(
            modifier          = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Обложка
            Box(
                modifier         = Modifier
                    .size(width = 56.dp, height = 80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (book.hasCover() && book.coverImage != null) {
                    AsyncImage(
                        model              = book.coverImage,
                        contentDescription = null,
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    book.title,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    book.author,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))

                // Прогресс чтения
                val progress = (book.getProgressPercentage() / 100f).coerceIn(0f, 1f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress  = { progress },
                        modifier  = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color     = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        drawStopIndicator = {},
                        gapSize   = 0.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${book.getProgressPercentage().toInt()}%",
                        style      = MaterialTheme.typography.labelSmall,
                        color      = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Подсказка удаления (помогает пользователю найти функцию)
            Icon(
                Icons.Default.SwipeLeft,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.size(18.dp).padding(start = 8.dp)
            )
        }

        HorizontalDivider(
            modifier  = Modifier.padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )

        // AI-чипы (только "Объяснить" и "Чат" — без теста и пересказа)
        Row(
            modifier              = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AiQuickChip("Объяснить", Icons.Default.Lightbulb) { }
            AiQuickChip("Чат с ИИ",  Icons.AutoMirrored.Filled.Chat) { }
        }
    }
}

@Composable
private fun FailedBookContent() {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.ErrorOutline, null,
            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("Ошибка импорта", color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium)
    }
}