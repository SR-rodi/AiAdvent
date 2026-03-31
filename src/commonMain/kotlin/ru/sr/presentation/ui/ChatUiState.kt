package ru.sr.presentation.ui

data class AgentSettingsUiState(
    val temperature: String = "1.0",
    val maxTokens: String = "",
    val topP: String = "",
    val frequencyPenalty: String = "",
    val presencePenalty: String = "",
    val stop: String = "",
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isStreaming: Boolean = false,
    val currentAgentName: String = "Agent-1",
    val agentNames: List<String> = listOf("Agent-1"),
    val showNewAgentDialog: Boolean = false,
    val settings: AgentSettingsUiState = AgentSettingsUiState(),
    val isSettingsPanelVisible: Boolean = true,
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
