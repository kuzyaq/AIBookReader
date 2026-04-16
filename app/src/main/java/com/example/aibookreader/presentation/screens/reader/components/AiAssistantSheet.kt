package com.example.aibookreader.presentation.screens.reader.components

import androidx.compose.foundation.ExperimentalFoundationApi
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aibookreader.domain.model.ChatMessage
import com.example.aibookreader.presentation.screens.reader.AiChatErrorUi
import com.example.aibookreader.presentation.screens.reader.ReaderUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AiAssistantSheetContent(
    uiState: ReaderUiState,
    onActionClick: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onClearHistory: () -> Unit,
    onSwitchToChat: () -> Unit = {},
    onSwitchToActions: () -> Unit = {},
    onRetryAi: () -> Unit = {}
) {
    var chatInput by remember { mutableStateOf("") }
    var actionPromptInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val screenHeight = LocalConfiguration.current.screenHeightDp
    val chatHeight = (screenHeight * 0.55f).dp

    val headerLeadWidth by animateDpAsState(
        targetValue = if (uiState.isActionMode) 0.dp else 48.dp,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "header_lead"
    )

    val lastListIndex = remember(
        uiState.chatMessages.size,
        uiState.aiChatError,
        uiState.isAiLoading
    ) {
        val err = if (uiState.aiChatError != null) 1 else 0
        val typing = if (uiState.isAiLoading) 1 else 0
        uiState.chatMessages.size + err + typing - 1
    }

    LaunchedEffect(uiState.chatMessages.size, uiState.aiChatError, uiState.isAiLoading) {
        if (lastListIndex >= 0) {
            listState.animateScrollToItem(lastListIndex)
        }
    }

    fun sendFromChatField() {
        if (chatInput.isNotBlank() && !uiState.isAiLoading) {
            onSendMessage(chatInput)
            chatInput = ""
        }
    }

    fun sendFromActionField() {
        if (actionPromptInput.isNotBlank() && !uiState.isAiLoading) {
            onSendMessage(actionPromptInput)
            actionPromptInput = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 16.dp)
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(headerLeadWidth),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = !uiState.isActionMode,
                    enter = fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(220))
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
                }
            }

            AnimatedContent(
                targetState = uiState.isActionMode,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val enterDur = 340
                    val exitDur = 280
                    val frac = 5
                    if (targetState) {
                        (
                            slideInHorizontally(
                                tween(enterDur, easing = FastOutSlowInEasing)
                            ) { -it / frac } + fadeIn(tween(enterDur, easing = FastOutSlowInEasing))
                            ) togetherWith (
                            slideOutHorizontally(
                                tween(exitDur, easing = LinearOutSlowInEasing)
                            ) { it / frac } + fadeOut(tween(exitDur, easing = LinearOutSlowInEasing))
                            )
                    } else {
                        (
                            slideInHorizontally(
                                tween(enterDur, easing = FastOutSlowInEasing)
                            ) { it / frac } + fadeIn(tween(enterDur, easing = FastOutSlowInEasing))
                            ) togetherWith (
                            slideOutHorizontally(
                                tween(exitDur, easing = LinearOutSlowInEasing)
                            ) { -it / frac } + fadeOut(tween(exitDur, easing = LinearOutSlowInEasing))
                            )
                    }
                },
                label = "header_title"
            ) { isActions ->
                Text(
                    text = if (isActions) "Анализ текста" else "Чат с ИИ",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(
                modifier = Modifier
                    .widthIn(min = 108.dp)
                    .height(40.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                AnimatedContent(
                    targetState = uiState.isActionMode to uiState.chatMessages.isNotEmpty(),
                    transitionSpec = {
                        fadeIn(
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) togetherWith fadeOut(
                            animationSpec = tween(220)
                        )
                    },
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

                        else -> Spacer(Modifier.size(1.dp))
                    }
                }
            }
        }

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

        AnimatedContent(
            targetState = uiState.isActionMode,
            transitionSpec = {
                val enterSlide = 14
                val exitSlide = 12
                if (targetState) {
                    (
                        fadeIn(tween(420, easing = FastOutSlowInEasing)) +
                            slideInHorizontally(tween(420, easing = FastOutSlowInEasing)) { -it / enterSlide }
                        ) togetherWith (
                        fadeOut(tween(300, easing = LinearOutSlowInEasing)) +
                            slideOutHorizontally(tween(300, easing = LinearOutSlowInEasing)) { it / exitSlide }
                        )
                } else {
                    (
                        fadeIn(tween(420, easing = FastOutSlowInEasing)) +
                            slideInHorizontally(tween(420, easing = FastOutSlowInEasing)) { it / enterSlide }
                        ) togetherWith (
                        fadeOut(tween(300, easing = LinearOutSlowInEasing)) +
                            slideOutHorizontally(tween(300, easing = LinearOutSlowInEasing)) { -it / exitSlide }
                        )
                }
            },
            label = "mode_transition"
        ) { isActions ->
            if (isActions) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Что сделать с текстом?",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    AiActionRow(
                        "Объяснить", "Простыми словами", Icons.Default.Lightbulb,
                        enabled = !uiState.isAiLoading
                    ) { onActionClick("explain") }
                    AiActionRow(
                        "Пересказать", "Краткое содержание", Icons.Default.Summarize,
                        enabled = !uiState.isAiLoading
                    ) { onActionClick("summary") }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Свой вопрос ИИ",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = actionPromptInput,
                            onValueChange = { actionPromptInput = it },
                            placeholder = { Text("Напишите сообщение…", fontSize = 14.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 4,
                            enabled = !uiState.isAiLoading,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { sendFromActionField() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = { sendFromActionField() },
                            enabled = actionPromptInput.isNotBlank() && !uiState.isAiLoading,
                            modifier = Modifier.size(52.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth().height(chatHeight)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 4.dp)
                    ) {
                        items(
                            items = uiState.chatMessages,
                            key = { m -> m.id.takeIf { it != 0 } ?: m.timeStamp.hashCode() }
                        ) { msg ->
                            ChatBubble(message = msg, context = context)
                        }
                        uiState.aiChatError?.let { chatErr ->
                            item(key = "ai_chat_error") {
                                AiErrorBubble(
                                    error = chatErr,
                                    onRetry = onRetryAi,
                                    context = context
                                )
                            }
                        }
                        if (uiState.isAiLoading) {
                            item(key = "typing") { TypingBubble() }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            placeholder = { Text("Спросить у ИИ…", fontSize = 14.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 4,
                            enabled = !uiState.isAiLoading,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { sendFromChatField() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = { sendFromChatField() },
                            enabled = chatInput.isNotBlank() && !uiState.isAiLoading,
                            modifier = Modifier.size(52.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send, null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AiErrorBubble(
    error: AiChatErrorUi,
    onRetry: () -> Unit,
    context: Context
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            tonalElevation = 1.dp,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .combinedClickable(
                    onClick = onRetry,
                    onLongClick = {
                        copyChatText(context, error.message, showToast = true)
                    }
                )
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Не удалось получить ответ",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    error.message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Нажмите, чтобы повторить",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
fun AiActionRow(
    title: String,
    subtitle: String = "",
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun formatChatTimestamp(epochMs: Long): String {
    val zone = ZoneId.of("Europe/Moscow")
    val zdt = Instant.ofEpochMilli(epochMs).atZone(zone)
    val today = LocalDate.now(zone)
    val msgDate = zdt.toLocalDate()
    val hm = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).format(zdt)
    return when {
        msgDate == today -> hm
        msgDate == today.minusDays(1) -> "Вчера, $hm"
        else -> DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("ru")).format(zdt)
    }
}

private fun copyChatText(context: Context, text: String, showToast: Boolean) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("chat", text))
    if (showToast) {
        Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(message: ChatMessage, context: Context) {
    val isUser = message.isUser
    val timeLabel = formatChatTimestamp(message.timeStamp)
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        if (isUser) {
            Column(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .combinedClickable(
                        onClick = { },
                        onLongClick = {
                            copyChatText(context, message.message, showToast = true)
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    message.message,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .combinedClickable(
                        onClick = { },
                        onLongClick = {
                            copyChatText(context, message.message, showToast = true)
                        }
                    )
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        message.message,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@Composable
fun TypingBubble() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Surface(
            shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val transition = rememberInfiniteTransition(label = "dot$index")
                    val alpha by transition.animateFloat(
                        0.35f, 1f,
                        infiniteRepeatable(
                            tween(650, delayMillis = index * 180),
                            RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = alpha * 0.55f),
                                RoundedCornerShape(50)
                            )
                    )
                }
            }
        }
    }
}
