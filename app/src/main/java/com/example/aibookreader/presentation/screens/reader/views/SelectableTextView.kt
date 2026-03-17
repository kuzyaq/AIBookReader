package com.example.aibookreader.presentation.screens.reader.views

import android.R
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.Selection
import android.text.Spannable
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Selectable TextView с кастомным ActionMode для поддержки AiTextToolbar
 */
@Composable
fun SelectableTextView(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    onAiSelected: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    AndroidView(
        modifier = modifier,
        factory = { context ->
            createSelectableTextView(
                context = context,
                onAiSelected = onAiSelected,
                getClipboard = { clipboardManager.getText()?.text }
            )
        },
        update = { textView ->
            textView.text = text

            if (color != Color.Unspecified) {
                textView.setTextColor(color.toArgb())
            }

            if (fontSize != TextUnit.Unspecified) {
                textView.textSize = fontSize.value
            }

            if (lineHeight != TextUnit.Unspecified) {
                textView.setLineSpacing(0f, lineHeight.value / fontSize.value)
            }
        }
    )
}

private fun createSelectableTextView(
    context: Context,
    onAiSelected: (String) -> Unit,
    getClipboard: () -> String?
): TextView {
    return TextView(context).apply {
        // Включаем выделение текста
        setTextIsSelectable(true)

        // Сохраняем ссылку на TextView для использования внутри callback
        val textView = this

        // Устанавливаем кастомный ActionMode.Callback
        customSelectionActionModeCallback = object : ActionMode.Callback {

            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                Log.d("SelectableTextView", "onCreateActionMode called")
                return buildMenu(menu)
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                Log.d("SelectableTextView", "onPrepareActionMode called")
                menu.clear()
                return buildMenu(menu)
            }

            private fun buildMenu(menu: Menu): Boolean {
                Log.d("SelectableTextView", "buildMenu called")
                var order = 0

                // Добавляем стандартные действия
                menu.add(0, R.id.copy, order++, "Копировать")
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

                menu.add(0, R.id.selectAll, order++, "Выделить всё")
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)

                // Добавляем кнопку ИИ
                menu.add(0, AI_MENU_ID, order, "✨ ИИ")
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

                Log.d("SelectableTextView", "Menu built with ${menu.size()} items")
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                Log.d("SelectableTextView", "onActionItemClicked: ${item.title} (id: ${item.itemId})")

                return when (item.itemId) {
                    R.id.copy -> {
                        // Копируем выделенный текст
                        val start = textView.selectionStart
                        val end = textView.selectionEnd
                        val selectedText = textView.text.subSequence(start, end).toString()

                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                            as ClipboardManager
                        val clip = ClipData.newPlainText("text", selectedText)
                        clipboard.setPrimaryClip(clip)

                        mode.finish()
                        true
                    }

                    R.id.selectAll -> {
                        // Выделяем весь текст через Selection API
                        val text = textView.text
                        if (text is Spannable) {
                            Selection.setSelection(text, 0, text.length)
                        }
                        true
                    }

                    AI_MENU_ID -> {
                        Log.d("SelectableTextView", "AI button clicked")

                        // Получаем выделенный текст
                        val start = textView.selectionStart
                        val end = textView.selectionEnd
                        val selectedText = textView.text.subSequence(start, end).toString()

                        Log.d("SelectableTextView", "Selected text: $selectedText")

                        if (selectedText.isNotBlank()) {
                            onAiSelected(selectedText)
                        }

                        mode.finish()
                        true
                    }

                    else -> false
                }
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                Log.d("SelectableTextView", "onDestroyActionMode called")
            }
        }
    }
}

private const val AI_MENU_ID = 999
