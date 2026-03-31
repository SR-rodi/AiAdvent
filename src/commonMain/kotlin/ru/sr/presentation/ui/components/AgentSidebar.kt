package ru.sr.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.sr.data.ChatSettings

@Composable
fun AgentSidebar(
    agentNames: List<String>,
    currentAgent: String,
    onAgentClick: (String) -> Unit,
    onNewAgentClick: () -> Unit,
    showDialog: Boolean,
    onCreateAgent: (String, ChatSettings) -> Unit,
    onDismissDialog: () -> Unit,
) {
    Column(
        modifier = Modifier.width(180.dp).fillMaxHeight().padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(agentNames) { name ->
                val isActive = name == currentAgent
                TextButton(
                    onClick = { if (!isActive) onAgentClick(name) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = name,
                        color = if (isActive)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface,
                        style = if (isActive)
                            MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        else
                            MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Button(
            onClick = onNewAgentClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Новый агент")
        }
    }

    if (showDialog) {
        NewAgentDialog(
            onCreateAgent = onCreateAgent,
            onDismiss = onDismissDialog,
        )
    }
}

@Composable
private fun NewAgentDialog(
    onCreateAgent: (String, ChatSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("1.0") }
    var maxTokens by remember { mutableStateOf("") }
    var topP by remember { mutableStateOf("") }
    var frequencyPenalty by remember { mutableStateOf("") }
    var presencePenalty by remember { mutableStateOf("") }
    var stop by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый агент") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя агента") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = temperature,
                    onValueChange = { temperature = it },
                    label = { Text("Temperature (0.0–2.0)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = maxTokens,
                    onValueChange = { maxTokens = it },
                    label = { Text("Max Tokens (пусто = авто)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = topP,
                    onValueChange = { topP = it },
                    label = { Text("Top P (0.0–1.0, пусто = авто)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = frequencyPenalty,
                    onValueChange = { frequencyPenalty = it },
                    label = { Text("Frequency Penalty (-2.0–2.0)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = presencePenalty,
                    onValueChange = { presencePenalty = it },
                    label = { Text("Presence Penalty (-2.0–2.0)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = stop,
                    onValueChange = { stop = it },
                    label = { Text("Stop-последовательности (через ,)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val settings = ChatSettings().apply {
                        this.temperature = temperature.toDoubleOrNull() ?: 1.0
                        this.maxTokens = maxTokens.trim().toIntOrNull()
                        this.topP = topP.trim().toDoubleOrNull()
                        this.frequencyPenalty = frequencyPenalty.trim().toDoubleOrNull()
                        this.presencePenalty = presencePenalty.trim().toDoubleOrNull()
                        this.stop = stop.trim()
                            .takeIf { it.isNotEmpty() }
                            ?.split(",")
                            ?.map { it.trim() }
                            ?.filter { it.isNotEmpty() }
                    }
                    onCreateAgent(name.trim(), settings)
                },
                enabled = name.isNotBlank(),
            ) { Text("Создать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}
