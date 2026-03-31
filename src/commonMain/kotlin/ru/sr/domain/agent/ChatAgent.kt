package ru.sr.domain.agent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import ru.sr.data.AiRepository
import ru.sr.data.ChatHistoryPort
import ru.sr.data.ChatSettings
import ru.sr.data.dto.Message

class ChatAgent(
    override val name: String,
    private val repository: AiRepository,
    val settings: ChatSettings,
    private val chatHistory: ChatHistoryPort,
) : Agent {

    private val history = chatHistory.loadHistory(name).toMutableList()

    fun historySnapshot(): List<Message> = history.toList()

    override suspend fun ask(question: String): String {
        val userMsg = Message("user", question)
        history.add(userMsg)
        chatHistory.appendMessage(name, userMsg)
        val response = repository.askAi(history.toList(), settings)
        val assistantMsg = Message("assistant", response)
        history.add(assistantMsg)
        chatHistory.appendMessage(name, assistantMsg)
        return response
    }

    override fun askStream(question: String): Flow<String> {
        val userMsg = Message("user", question)
        history.add(userMsg)
        chatHistory.appendMessage(name, userMsg)
        val buffer = StringBuilder()
        return repository.askAiStream(history.toList(), settings)
            .onEach { chunk -> buffer.append(chunk) }
            .onCompletion { error ->
                if (error == null && buffer.isNotEmpty()) {
                    val assistantMsg = Message("assistant", buffer.toString())
                    history.add(assistantMsg)
                    chatHistory.appendMessage(name, assistantMsg)
                } else if (error != null) {
                    history.removeLastOrNull()
                }
            }
    }

    fun clearHistory() {
        history.clear()
        chatHistory.clearHistory(name)
    }
}
