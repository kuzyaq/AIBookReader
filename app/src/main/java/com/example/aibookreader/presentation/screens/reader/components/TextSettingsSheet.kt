package com.example.aibookreader.presentation.screens.reader.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aibookreader.domain.model.ReaderSettings

@Composable
fun TextSettingsSheetContent(
    settings: ReaderSettings,
    onSettingsChange: (ReaderSettings) -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 16.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Настройки текста",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onReset) {
                Text("Сбросить", fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Предпросмотр",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Пример текста книги для предпросмотра настроек шрифта.",
            fontSize = settings.fontSize.sp,
            lineHeight = (settings.fontSize * settings.lineHeightMultiplier).sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(20.dp))

        SettingsSlider(
            icon = Icons.Default.FormatSize,
            label = "Размер шрифта",
            value = settings.fontSize,
            valueLabel = "${settings.fontSize.toInt()} sp",
            min = ReaderSettings.MIN_FONT_SIZE,
            max = ReaderSettings.MAX_FONT_SIZE,
            onValueChange = { onSettingsChange(settings.copy(fontSize = it, titleFontSize = it + 10f)) }
        )

        Spacer(Modifier.height(16.dp))

        SettingsSlider(
            icon = Icons.Default.FormatLineSpacing,
            label = "Межстрочный интервал",
            value = settings.lineHeightMultiplier,
            valueLabel = "×${"%.1f".format(settings.lineHeightMultiplier)}",
            min = ReaderSettings.MIN_LINE_HEIGHT,
            max = ReaderSettings.MAX_LINE_HEIGHT,
            onValueChange = { onSettingsChange(settings.copy(lineHeightMultiplier = it)) }
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SettingsSlider(
    icon: ImageVector,
    label: String,
    value: Float,
    valueLabel: String,
    min: Float,
    max: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                valueLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
