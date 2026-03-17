package com.example.aibookreader.presentation.screens.reader.views

import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.graphics.Rect as AndroidRect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus

/**
 * Использование в ReaderScreen:
 *
 *   val toolbar = rememberAiTextToolbar { selectedText -> /* открыть sheet */ }
 *   CompositionLocalProvider(LocalTextToolbar provides toolbar) { ... }
 */
@Composable
fun rememberAiTextToolbar(onAiSelected: (String) -> Unit): TextToolbar {
    val view             = LocalView.current
    val clipboardManager = LocalClipboardManager.current

    // Wrapper позволяет всегда иметь актуальный callback
    // без пересоздания объекта тулбара при перекомпозиции
    val callbackRef = remember { Wrapper(onAiSelected) }
    callbackRef.value = onAiSelected

    return remember(view) {
        Log.d("AiTextToolbar", "Creating new AiTextToolbar instance")
        AiTextToolbar(
            view         = view,
            getClipboard = { clipboardManager.getText()?.text },
            onAiSelected = { callbackRef.value(it) }
        ).also {
            Log.d("AiTextToolbar", "AiTextToolbar created successfully")
        }
    }
}

private class Wrapper<T>(var value: T)

// ─────────────────────────────────────────────────────────────────────────────

class AiTextToolbar(
    private val view         : View,
    private val getClipboard : () -> String?,
    private val onAiSelected : (String) -> Unit
) : TextToolbar {

    // Статус ОБЯЗАН отражать реальное состояние:
    // если вернуть Hidden, SelectionManager не поймёт, что меню показано
    private var _status = TextToolbarStatus.Hidden
    override val status: TextToolbarStatus get() = _status

    private var actionMode   : ActionMode?   = null
    private var copyCallback : (() -> Unit)? = null
    private var cutCallback  : (() -> Unit)? = null
    private var pasteCallback: (() -> Unit)? = null
    private var selectAll    : (() -> Unit)? = null
    private var lastRect     : Rect          = Rect.Zero

    override fun showMenu(
        rect                : Rect,
        onCopyRequested     : (() -> Unit)?,
        onPasteRequested    : (() -> Unit)?,
        onCutRequested      : (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        Log.d("AiTextToolbar", "showMenu called with rect: $rect")
        lastRect      = rect
        copyCallback  = onCopyRequested
        cutCallback   = onCutRequested
        pasteCallback = onPasteRequested
        selectAll     = onSelectAllRequested

        if (actionMode == null) {
            Log.d("AiTextToolbar", "Creating new ActionMode")
            // Используем TYPE_PRIMARY вместо TYPE_FLOATING для лучшего перехвата событий
            actionMode = view.startActionMode(modeCallback, ActionMode.TYPE_PRIMARY)
            Log.d("AiTextToolbar", "ActionMode created: ${actionMode != null}")
        } else {
            Log.d("AiTextToolbar", "Invalidating existing ActionMode")
            actionMode?.invalidate()
        }
        _status = TextToolbarStatus.Shown
    }

    override fun hide() {
        Log.d("AiTextToolbar", "hide called")
        actionMode?.finish()
        actionMode = null
        _status = TextToolbarStatus.Hidden
    }

    // ── ActionMode.Callback2 ─────────────────────────────────────────────────

    private val modeCallback = object : ActionMode.Callback2() {

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            Log.d("AiTextToolbar", "onCreateActionMode called")
            return buildMenu(menu)
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            Log.d("AiTextToolbar", "onPrepareActionMode called")
            menu.clear()
            return buildMenu(menu)
        }

        private fun buildMenu(menu: Menu): Boolean {
            Log.d("AiTextToolbar", "buildMenu called")
            var order = 0
            if (copyCallback  != null) menu.add(0, COPY,       order++, "Копировать").showAlways()
            if (cutCallback   != null) menu.add(0, CUT,        order++, "Вырезать").showIfRoom()
            if (pasteCallback != null) menu.add(0, PASTE,      order++, "Вставить").showIfRoom()
            if (selectAll     != null) menu.add(0, SELECT_ALL, order++, "Выделить всё").showIfRoom()
            menu.add(0, AI, order, "✨ ИИ").showAlways()
            Log.d("AiTextToolbar", "Menu built with ${menu.size()} items")
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            Log.d("AiTextToolbar", "onActionItemClicked: ${item.title} (id: ${item.itemId})")
            when (item.itemId) {
                COPY       -> { copyCallback?.invoke();  mode.finish() }
                CUT        -> { cutCallback?.invoke();   mode.finish() }
                PASTE      -> { pasteCallback?.invoke(); mode.finish() }
                SELECT_ALL -> { selectAll?.invoke() }
                AI         -> {
                    Log.d("AiTextToolbar", "AI button clicked")
                    // Сначала копируем выделенный текст в буфер обмена
                    copyCallback?.invoke()
                    // view.post — читаем буфер только после того,
                    // как copyCallback обновил его (исправляет race condition)
                    view.post {
                        val text = getClipboard()
                        Log.d("AiTextToolbar", "Clipboard text: $text")
                        if (!text.isNullOrBlank()) {
                            onAiSelected(text)
                        }
                    }
                    mode.finish()
                }
                else -> return false
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null
            _status = TextToolbarStatus.Hidden
        }

        override fun onGetContentRect(mode: ActionMode, view: View, outRect: AndroidRect) {
            outRect.set(
                lastRect.left.toInt(),
                lastRect.top.toInt(),
                lastRect.right.toInt(),
                lastRect.bottom.toInt()
            )
        }
    }

    companion object {
        private const val COPY       = 1
        private const val CUT        = 2
        private const val PASTE      = 3
        private const val SELECT_ALL = 4
        private const val AI         = 5

        private fun MenuItem.showAlways() = setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        private fun MenuItem.showIfRoom() = setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
    }
}