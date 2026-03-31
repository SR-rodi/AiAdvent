package ru.sr.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.sr.presentation.ui.ChatMessage

@Composable
fun MessageBubble(message: ChatMessage) {
    val arrangement: Arrangement.Horizontal
    val containerColor: Color

    when (message) {
        is ChatMessage.User -> {
            arrangement = Arrangement.End
            containerColor = MaterialTheme.colorScheme.primaryContainer
        }
        is ChatMessage.Ai -> {
            arrangement = Arrangement.Start
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        }
        is ChatMessage.System -> {
            arrangement = Arrangement.Center
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = arrangement,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = containerColor,
            modifier = Modifier.padding(4.dp).widthIn(max = 480.dp),
        ) {
            val text = when (message) {
                is ChatMessage.User -> message.text
                is ChatMessage.Ai -> if (message.isStreaming && message.text.isEmpty()) "..." else message.text
                is ChatMessage.System -> "⚙ ${message.text}"
            }
            SelectionContainer {
                Text(
                    text = text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}
