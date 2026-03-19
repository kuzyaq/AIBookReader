package com.example.aibookreader.presentation.screens.reader.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.aibookreader.domain.model.ReaderBlock
import com.example.aibookreader.presentation.screens.reader.views.SelectableTextView
import kotlin.collections.forEach

@Composable
fun EpubPage(
    blocks: List<ReaderBlock>,
    onTap: () -> Unit = {},
    onAiSelected: (String) -> Unit,
    selectionKey: Int = 0
) {
    val scroll = rememberScrollState()

    key(selectionKey) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(16.dp)
        ) {
            blocks.forEach { block ->
                when (block) {
                    is ReaderBlock.Title -> SelectableTextView(
                        text = block.text,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 28.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        onAiSelected = onAiSelected
                    )

                    is ReaderBlock.Paragraph -> SelectableTextView(
                        text = block.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 18.sp,
                            lineHeight = 28.sp,
                            letterSpacing = 0.3.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                        fontSize = 18.sp,
                        lineHeight = 28.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        onAiSelected = onAiSelected
                    )

                    is ReaderBlock.Image -> AsyncImage(
                        model = block.src,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )

                    is ReaderBlock.Quote -> SelectableTextView(
                        text = block.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        onAiSelected = onAiSelected
                    )
                }
            }
        }
    }
}
