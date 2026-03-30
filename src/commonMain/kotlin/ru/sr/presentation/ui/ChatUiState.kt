package ru.sr.presentation.ui

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isStreaming: Boolean = false,
    val currentAgentName: String = "Agent-1",
)

sealed class ChatMessage {
    abstract val id: Long

    data class User(
        override val id: Long,
        val text: String,
    ) : ChatMessage()

    data class Ai(
        override val id: Long,
        val text: String,
        val isStreaming: Boolean = false,
    ) : ChatMessage()

    data class System(
        override val id: Long,
        val text: String,
    ) : ChatMessage()
}
