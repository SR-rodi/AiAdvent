package ru.sr.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.sr.presentation.ui.AgentSettingsUiState

@Composable
fun SettingsSidebar(
    settings: AgentSettingsUiState,
    onTemperatureChanged: (String) -> Unit,
    onMaxTokensChanged: (String) -> Unit,
    onTopPChanged: (String) -> Unit,
    onFrequencyPenaltyChanged: (String) -> Unit,
    onPresencePenaltyChanged: (String) -> Unit,
    onStopChanged: (String) -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier.width(200.dp).fillMaxHeight().padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Настройки", style = MaterialTheme.typography.titleSmall)

            SettingField(
                value = settings.temperature,
                onValueChange = onTemperatureChanged,
                label = "Temperature",
                placeholder = "0.0–2.0",
                isError = settings.temperature.isNotEmpty() &&
                        settings.temperature.toDoubleOrNull() == null,
            )
            SettingField(
                value = settings.maxTokens,
                onValueChange = onMaxTokensChanged,
                label = "Max Tokens",
                placeholder = "пусто = авто",
                isError = settings.maxTokens.isNotEmpty() &&
                        settings.maxTokens.trim().toIntOrNull() == null,
            )
            SettingField(
                value = settings.topP,
                onValueChange = onTopPChanged,
                label = "Top P",
                placeholder = "0.0–1.0",
                isError = settings.topP.isNotEmpty() &&
                        settings.topP.toDoubleOrNull() == null,
            )
            SettingField(
                value = settings.frequencyPenalty,
                onValueChange = onFrequencyPenaltyChanged,
                label = "Frequency Penalty",
                placeholder = "-2.0–2.0",
                isError = settings.frequencyPenalty.isNotEmpty() &&
                        settings.frequencyPenalty.toDoubleOrNull() == null,
            )
            SettingField(
                value = settings.presencePenalty,
                onValueChange = onPresencePenaltyChanged,
                label = "Presence Penalty",
                placeholder = "-2.0–2.0",
                isError = settings.presencePenalty.isNotEmpty() &&
                        settings.presencePenalty.toDoubleOrNull() == null,
            )
            SettingField(
                value = settings.stop,
                onValueChange = onStopChanged,
                label = "Stop",
                placeholder = "через запятую",
                isError = false,
            )
        }

        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text("Сбросить")
        }
    }
}

@Composable
private fun SettingField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isError: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
        isError = isError,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodySmall,
    )
}
