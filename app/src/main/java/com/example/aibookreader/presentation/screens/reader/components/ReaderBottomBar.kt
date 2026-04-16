package com.example.aibookreader.presentation.screens.reader.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReaderBottomBar(
    pageLabel: String,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onAiClick: () -> Unit,
    onTextSettingsClick: () -> Unit = {}
) {
    Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.primary) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPreviousPage, enabled = hasPrevious) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(
                        alpha = if (hasPrevious) 1f else 0.4f
                    )
                )
            }

            IconButton(onClick = onTextSettingsClick) {
                Icon(
                    Icons.Default.TextFields, "Настройки текста",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Text(
                pageLabel,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
            )

            IconButton(onClick = onAiClick) {
                Icon(
                    Icons.Default.AutoAwesome, "ИИ",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            IconButton(onClick = onNextPage, enabled = hasNext) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward, null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(
                        alpha = if (hasNext) 1f else 0.4f
                    )
                )
            }
        }
    }
}
