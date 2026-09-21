package com.honlnk.chat_opener.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.honlnk.chat_opener.app.R
import com.honlnk.chat_opener.app.ui.components.CompactTopAppBar
import kotlin.math.roundToInt

private const val VERSION = "1.0.0"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: Int,
    fontSizeSp: Int,
    showTimestamps: Boolean,
    showSystemMessages: Boolean,
    onThemeChange: (Int) -> Unit,
    onFontChange: (Int) -> Unit,
    onTimestampsChange: (Boolean) -> Unit,
    onSystemMessagesChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CompactTopAppBar(
                title = { Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = themeMode == 0, onClick = { onThemeChange(0) }, label = { Text(stringResource(R.string.theme_system)) })
                FilterChip(selected = themeMode == 1, onClick = { onThemeChange(1) }, label = { Text(stringResource(R.string.theme_light)) })
                FilterChip(selected = themeMode == 2, onClick = { onThemeChange(2) }, label = { Text(stringResource(R.string.theme_dark)) })
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.font_size), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text("$fontSizeSp sp", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Slider(
                value = fontSizeSp.toFloat(),
                onValueChange = { onFontChange(it.roundToInt()) },
                valueRange = 12f..28f,
                steps = 15
            )

            Spacer(Modifier.height(12.dp))
            SwitchRow(
                title = stringResource(R.string.show_timestamps),
                checked = showTimestamps,
                onChange = onTimestampsChange
            )
            SwitchRow(
                title = stringResource(R.string.show_system_messages),
                checked = showSystemMessages,
                onChange = onSystemMessagesChange
            )

            Spacer(Modifier.height(24.dp))
            Text(
                "${stringResource(R.string.current_version)} $VERSION",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
