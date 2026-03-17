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
    currentPage   : Int,
    totalPages    : Int,
    onPreviousPage: () -> Unit,
    onNextPage    : () -> Unit,
    onAiClick     : () -> Unit
) {
    Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.primary) {
        Row(
            modifier            = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPreviousPage, enabled = currentPage > 1) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(
                        alpha = if (currentPage > 1) 1f else 0.6f))
            }

            IconButton(onClick = onAiClick) {
                Icon(Icons.Default.TextFields, "",
                    tint = MaterialTheme.colorScheme.onPrimary)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("$currentPage / $totalPages",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
            }

            IconButton(onClick = onAiClick) {
                Icon(Icons.Default.AutoAwesome, "ИИ",
                    tint = MaterialTheme.colorScheme.onPrimary)
            }

            IconButton(onClick = onNextPage, enabled = currentPage < totalPages) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(
                        alpha = if (currentPage < totalPages) 1f else 0.6f))
            }
        }
    }
}