package com.example.aibookreader.presentation.screens.reader.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopAppBar(
    onNavigateBack: () -> Unit,
    onToggleTheme: () -> Unit,
    bookTitle: String
) {
    TopAppBar(
        title = { Text(bookTitle, color = MaterialTheme.colorScheme.onPrimary) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        actions = {
            IconButton(onClick = onToggleTheme) {
                Icon(
                    Icons.Default.DarkMode, null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        colors = topAppBarColors().copy(containerColor = MaterialTheme.colorScheme.primary)
    )
}