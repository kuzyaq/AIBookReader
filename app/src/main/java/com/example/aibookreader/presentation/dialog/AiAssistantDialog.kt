package com.example.aibookreader.presentation.dialog

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================
// Модели для AI-диалога
// ============================================

sealed class AiAction {
    object Explain : AiAction()
    object Quiz : AiAction()
    object Summarize : AiAction()
    object Translate : AiAction()
    object Define : AiAction()
    object Chat : AiAction()
}

data class AiMessage(
    val role: AiRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AiRole {
    User,
    Assistant,
    System
}

data class AiDialogState(
    val isLoading: Boolean = false,
    val messages: List<AiMessage> = emptyList(),
    val error: String? = null,
    val activeAction: AiAction? = null
)

data class AiActionItem(
    val action: AiAction,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String,
    val color: Color
)

// ============================================
// Основной компонент диалога
// ============================================

@Composable
fun AiAssistantDialog(
    selectedText: String,
    onDismiss: () -> Unit,
    onExplain: (String) -> Unit,
    onQuiz: (String) -> Unit,
    onSummarize: (String) -> Unit,
    onTranslate: (String, String) -> Unit = { _, _ -> },
    onDefine: (String) -> Unit = {},
    onChat: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(true) }
    var showChat by remember { mutableStateOf(false) }
    var chatInput by remember { mutableStateOf("") }
    val dialogState = remember { mutableStateOf(AiDialogState()) }
    val scope = rememberCoroutineScope()

    if (showDialog) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 8.dp)
                ) {
                    // Заголовок
                    AiDialogHeader(
                        onClose = {
                            showDialog = false
                            onDismiss()
                        },
                        onToggleChat = { showChat = !showChat }
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    if (showChat) {
                        // Режим чата с ИИ
                        AiChatInterface(
                            messages = dialogState.value.messages,
                            isLoading = dialogState.value.isLoading,
                            onSendMessage = { message ->
                                chatInput = ""
                                dialogState.value = dialogState.value.copy(
                                    isLoading = true,
                                    messages = dialogState.value.messages + AiMessage(AiRole.User, message)
                                )
                                // Симуляция ответа (удалить при интеграции с API)
                                scope.launch {
                                    delay(1500)
                                    dialogState.value = dialogState.value.copy(
                                        isLoading = false,
                                        messages = dialogState.value.messages + AiMessage(
                                            AiRole.Assistant,
                                            "Это ответ на ваш вопрос: $message"
                                        )
                                    )
                                }
                            },
                            inputText = chatInput,
                            onInputTextChange = { chatInput = it }
                        )
                    } else {
                        // Режим выбора действия
                        AiActionSelector(
                            selectedText = selectedText,
                            onActionSelected = { action ->
                                dialogState.value = dialogState.value.copy(
                                    activeAction = action,
                                    isLoading = true
                                )
                                when (action) {
                                    is AiAction.Explain -> onExplain(selectedText)
                                    is AiAction.Quiz -> onQuiz(selectedText)
                                    is AiAction.Summarize -> onSummarize(selectedText)
                                    is AiAction.Translate -> onTranslate(selectedText, "ru")
                                    is AiAction.Define -> onDefine(selectedText)
                                    is AiAction.Chat -> showChat = true
                                }
                                // Симуляция загрузки (удалить при интеграции с API)
                                scope.launch {
                                    delay(1500)
                                    dialogState.value = dialogState.value.copy(
                                        isLoading = false,
                                        messages = dialogState.value.messages + AiMessage(
                                            AiRole.Assistant,
                                            getMockResponse(action, selectedText)
                                        )
                                    )
                                }
                            },
                            isLoading = dialogState.value.isLoading
                        )
                    }
                }
            }
        }
    }
}

// ============================================
// Заголовок диалога
// ============================================

@Composable
private fun AiDialogHeader(
    onClose: () -> Unit,
    onToggleChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "🤖 AI Ассистент",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Готов помочь с книгой",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row {
            IconButton(onClick = onToggleChat) {
                Icon(
                    Icons.Default.Chat,
                    contentDescription = "Чат",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Закрыть",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================
// Выбор действия AI
// ============================================

@Composable
private fun AiActionSelector(
    selectedText: String,
    onActionSelected: (AiAction) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        AiActionItem(
            action = AiAction.Explain,
            icon = Icons.Default.School,
            title = "📚 Объяснить",
            description = "Простым языком о сложном",
            color = Color(0xFF4CAF50)
        ),
        AiActionItem(
            action = AiAction.Quiz,
            icon = Icons.Default.Quiz,
            title = "❓ Составить тест",
            description = "Проверь свои знания",
            color = Color(0xFF2196F3)
        ),
        AiActionItem(
            action = AiAction.Summarize,
            icon = Icons.Default.Summarize,
            title = "📝 Пересказать",
            description = "Краткое содержание",
            color = Color(0xFF9C27B0)
        ),
        AiActionItem(
            action = AiAction.Translate,
            icon = Icons.Default.Translate,
            title = "🌐 Перевести",
            description = "На другой язык",
            color = Color(0xFF00BCD4)
        ),
        AiActionItem(
            action = AiAction.Define,
            icon = Icons.Default.MenuBook,
            title = "📖 Определить термин",
            description = "Значение слова или понятия",
            color = Color(0xFFFF9800)
        ),
        AiActionItem(
            action = AiAction.Chat,
            icon = Icons.Default.ChatBubble,
            title = "💬 Спросить о чём угодно",
            description = "Свободный диалог с ИИ",
            color = Color(0xFFE91E63)
        )
    )

    Column(modifier = modifier.fillMaxSize()) {
        // Превью выделенного текста
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TextFields,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Выделенный фрагмент:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = selectedText.take(300) + if (selectedText.length > 300) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопки действий
        Text(
            text = "Что сделать с текстом?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(actions) { action ->
                AiActionCard(
                    action = action,
                    onClick = { onActionSelected(action.action) },
                    enabled = !isLoading
                )
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "🤔 Думаю...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiActionCard(
    action: AiActionItem,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(action.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    action.icon,
                    contentDescription = null,
                    tint = action.color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = action.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// ============================================
// Чат-интерфейс с ИИ
// ============================================

@Composable
private fun AiChatInterface(
    messages: List<AiMessage>,
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Список сообщений
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Задайте вопрос о выделенном тексте",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            items(messages.reversed()) { message ->
                AiMessageBubble(message = message)
            }

            if (isLoading) {
                item {
                    AiMessageBubble(
                        AiMessage(AiRole.Assistant, "🤔", System.currentTimeMillis()),
                        isTyping = true
                    )
                }
            }
        }

        // Поле ввода
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    placeholder = { Text("Введите сообщение...") },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    maxLines = 3
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                        }
                    },
                    enabled = inputText.isNotBlank() && !isLoading
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Отправить",
                        tint = if (inputText.isNotBlank() && !isLoading)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AiMessageBubble(
    message: AiMessage,
    isTyping: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == AiRole.User
    val bubbleColor = if (isUser)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(bubbleColor)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .widthIn(max = 280.dp)
        ) {
            if (isTyping) {
                TypingIndicator()
            } else {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun TypingIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "typing")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = index * 200
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "typingAlpha"
            )

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                        RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

// ============================================
// Заглушки для AI-ответов (удалить при интеграции)
// ============================================

private fun getMockResponse(action: AiAction, text: String): String {
    return when (action) {
        is AiAction.Explain -> """
            📚 **Объяснение:**
            
            Выделенный текст описывает концепцию, которую можно упростить так:
            
            • **Основная идея**: [Краткая суть]
            • **Ключевые моменты**:
              - Момент 1
              - Момент 2
            • **Почему это важно**: [Значение в контексте]
            
            💡 *Подсказка: Попробуйте связать это с предыдущими главами!*
        """.trimIndent()

        is AiAction.Quiz -> """
            ❓ **Тест по тексту:**
            
            **Вопрос 1:** Что является основной темой выделенного фрагмента?
            ○ Вариант A
            ○ Вариант B  
            ○ Вариант C
            ○ Вариант D
            
            **Вопрос 2:** Какой вывод можно сделать из прочитанного?
            ○ Вариант A
            ○ Вариант B
            
            *Нажмите на вариант для проверки ответа*
        """.trimIndent()

        is AiAction.Summarize -> """
            📝 **Краткий пересказ:**
            
            В этом фрагменте автор рассказывает о:
            
            1. **Контекст**: [О чём речь]
            2. **Развитие**: [Что происходит]
            3. **Вывод**: [Главная мысль]
            
            🔑 **Ключевая мысль**: [Одно предложение]
            
            ⏱️ *Время чтения оригинала: ~2 мин, пересказа: ~30 сек*
        """.trimIndent()

        is AiAction.Translate -> "🌐 *Перевод будет доступен после подключения API перевода*"
        is AiAction.Define -> "📖 *Определение термина будет сгенерировано после подключения к LLM*"
        is AiAction.Chat -> "💬 *Привет! Я готов ответить на ваши вопросы об этом тексте. Что вас интересует?*"
    }
}