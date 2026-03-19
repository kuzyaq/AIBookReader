package com.example.aibookreader.presentation.screens.reader.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aibookreader.domain.model.ChatMessage
import com.example.aibookreader.presentation.screens.reader.ReaderUiState

@Composable
fun AiAssistantSheetContent(
    uiState: ReaderUiState,
    onActionClick: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onClearHistory: () -> Unit,
    onSwitchToChat: () -> Unit = {},
    onSwitchToActions: () -> Unit = {}
) {
    var chatInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.chatMessages.size, uiState.isAiLoading) {
        val index = if (uiState.isAiLoading) uiState.chatMessages.size
        else (uiState.chatMessages.size - 1).coerceAtLeast(0)
        if (uiState.chatMessages.isNotEmpty() || uiState.isAiLoading) {
            listState.animateScrollToItem(index)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 16.dp)
            .navigationBarsPadding()
            .imePadding()
    ) {

        // Заголовок
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка Назад
            AnimatedVisibility(
                visible = !uiState.isActionMode,
                enter = fadeIn() + slideInHorizontally { -it },
                exit = fadeOut() + slideOutHorizontally { -it }
            ) {
                IconButton(
                    onClick = onSwitchToActions,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад к действиям",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.width(4.dp))
            }

            // заголовок
            AnimatedContent(
                targetState = uiState.isActionMode,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn(tween(300)) + slideInVertically { -it / 2 }) togetherWith
                            (fadeOut(tween(200)) + slideOutVertically { it / 2 })
                },
                label = "header_title"
            ) { isActions ->
                Text(
                    text = if (isActions) "Анализ текста" else "Чат с ИИ",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Правая кнопка
            AnimatedContent(
                targetState = uiState.isActionMode to uiState.chatMessages.isNotEmpty(),
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "header_action"
            ) { (isActions, hasMessages) ->
                when {
                    isActions && hasMessages -> FilledTonalButton(
                        onClick = onSwitchToChat,
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Chat, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Чат с ИИ", fontSize = 12.sp)
                    }

                    !isActions && hasMessages -> TextButton(
                        onClick = onClearHistory,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Очистить", fontSize = 12.sp)
                    }

                    else -> Box(Modifier.size(1.dp))
                }
            }
        }

        // Превью текста
        AnimatedVisibility(
            visible = uiState.isActionMode,
            enter = fadeIn(tween(300)) + expandVertically(tween(300)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
        ) {
            uiState.selectedText?.let { selected ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = selected,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Ошибка
        AnimatedVisibility(
            visible = uiState.aiError != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            uiState.aiError?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline, null,
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            error, color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Основной контент
        AnimatedContent(
            targetState = uiState.isActionMode,
            transitionSpec = {
                if (targetState) {
                    // Переход к действиям (назад) — входим слева
                    (slideInHorizontally(tween(320)) { -it / 4 } + fadeIn(tween(320))) togetherWith
                            (slideOutHorizontally(tween(280)) { it / 4 } + fadeOut(tween(280)))
                } else {
                    // Переход к чату (вперёд) — входим справа
                    (slideInHorizontally(tween(320)) { it / 4 } + fadeIn(tween(320))) togetherWith
                            (slideOutHorizontally(tween(280)) { -it / 4 } + fadeOut(tween(280)))
                }
            },
            label = "mode_transition"
        ) { isActions ->
            if (isActions) {
                //Режим быстрых действий
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Что сделать с текстом?",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    AiActionRow(
                        "Объяснить",
                        "Простыми словами",
                        Icons.Default.Lightbulb
                    ) { onActionClick("explain") }
                    AiActionRow(
                        "Пересказать",
                        "Краткое содержание",
                        Icons.Default.Summarize
                    ) { onActionClick("summary") }
                    AiActionRow(
                        "Создать тест",
                        "Проверь понимание",
                        Icons.Default.Quiz
                    ) { onActionClick("quiz") }
                    Spacer(Modifier.height(4.dp))
                }
            } else {
                // Режим чата
                Column {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 4.dp)
                    ) {
                        items(items = uiState.chatMessages, key = { it.id }) { msg ->
                            ChatBubble(message = msg)
                        }
                        if (uiState.isAiLoading) {
                            item { TypingBubble() }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            placeholder = { Text("Спросить у ИИ...", fontSize = 14.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                if (chatInput.isNotBlank()) {
                                    onSendMessage(chatInput)
                                    chatInput = ""
                                }
                            },
                            enabled = chatInput.isNotBlank() && !uiState.isAiLoading,
                            modifier = Modifier.size(52.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.1f
                                )
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiActionRow(title: String, subtitle: String = "", icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        if (isUser) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .widthIn(max = 290.dp)
            ) {
                Text(message.message, color = Color.White, fontSize = 15.sp, lineHeight = 22.sp)
            }
        } else {
            Surface(
                shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                modifier = Modifier.widthIn(max = 290.dp)
            ) {
                Text(
                    message.message,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontSize = 15.sp, lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
fun TypingBubble() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Surface(
            shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
            color = MaterialTheme.colorScheme.inverseSurface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val transition = rememberInfiniteTransition(label = "dot$index")
                    val alpha by transition.animateFloat(
                        0.3f, 1f,
                        infiniteRepeatable(
                            tween(600, delayMillis = index * 200),
                            RepeatMode.Reverse
                        ), "alpha"
                    )
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = alpha),
                                RoundedCornerShape(50)
                            )
                    )
                }
            }
        }
    }
}