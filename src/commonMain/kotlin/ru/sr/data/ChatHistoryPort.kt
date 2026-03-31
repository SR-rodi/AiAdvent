package ru.sr.data

import ru.sr.data.dto.Message

interface ChatHistoryPort {
    fun listAgentNames(): List<String>
    fun loadHistory(agentName: String): List<Message>
    fun appendMessage(agentName: String, message: Message)
    fun clearHistory(agentName: String)
}
