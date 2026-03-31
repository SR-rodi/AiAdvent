package ru.sr.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import ru.sr.domain.agent.TokenStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    agentName: String,
    tokenStats: TokenStats,
    onToggleSettings: () -> Unit,
) {
    TopAppBar(
        title = {
            Column {
                Text("AiAdvent — $agentName")
                if (tokenStats.sessionTotalTokens > 0) {
                    Text(
                        text = "⬆ ${tokenStats.lastPromptTokens.formatTokens()}" +
                               "  ⬇ ${tokenStats.lastCompletionTokens.formatTokens()}" +
                               "  Σ ${tokenStats.sessionTotalTokens.formatTokens()}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onToggleSettings) {
                Text(
                    text = "⚙",
                    fontSize = 20.sp,
                )
            }
        },
    )
}

private fun Int.formatTokens(): String = when {
    this >= 1_000 -> "${this / 1000}.${(this % 1000) / 100}k"
    else          -> toString()
}
