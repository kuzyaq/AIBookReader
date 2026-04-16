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
import com.example.aibookreader.domain.model.ReaderSettings
import com.example.aibookreader.presentation.screens.reader.views.SelectableTextView

@Composable
fun EpubPage(
    blocks: List<ReaderBlock>,
    settings: ReaderSettings = ReaderSettings(),
    onTap: () -> Unit = {},
    onAiSelected: (String) -> Unit,
    selectionKey: Int = 0
) {
    val scroll = rememberScrollState()

    val bodyFontSize = settings.fontSize.sp
    val titleFontSize = settings.titleFontSize.sp
    val bodyLineHeight = (settings.fontSize * settings.lineHeightMultiplier).sp
    val titleLineHeight = (settings.titleFontSize * settings.lineHeightMultiplier).sp
    val paragraphPadding = settings.paragraphSpacing.dp

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
                            fontSize = titleFontSize,
                            lineHeight = titleLineHeight
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = titleFontSize,
                        lineHeight = titleLineHeight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = paragraphPadding),
                        onAiSelected = onAiSelected
                    )

                    is ReaderBlock.Paragraph -> SelectableTextView(
                        text = block.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = bodyFontSize,
                            lineHeight = bodyLineHeight
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                        fontSize = bodyFontSize,
                        lineHeight = bodyLineHeight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = paragraphPadding),
                        onAiSelected = onAiSelected
                    )

                    is ReaderBlock.Image -> AsyncImage(
                        model = block.src,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = paragraphPadding)
                    )

                    is ReaderBlock.Quote -> SelectableTextView(
                        text = block.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = (settings.fontSize - 2f).sp,
                            lineHeight = ((settings.fontSize - 2f) * settings.lineHeightMultiplier).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = (settings.fontSize - 2f).sp,
                        lineHeight = ((settings.fontSize - 2f) * settings.lineHeightMultiplier).sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = paragraphPadding),
                        onAiSelected = onAiSelected
                    )
                }
            }
        }
    }
}
