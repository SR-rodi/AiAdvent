package ru.sr.domain.agent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import ru.sr.data.AiRepository
import ru.sr.data.ChatSettings
import ru.sr.data.dto.Message

class ChatAgent(
    override val name: String,
    private val repository: AiRepository,
    val settings: ChatSettings,
) : Agent {

    private val history = mutableListOf<Message>()

    override suspend fun ask(question: String): String {
        history.add(Message("user", question))
        val response = repository.askAi(history.toList(), settings)
        history.add(Message("assistant", response))
        return response
    }

    override fun askStream(question: String): Flow<String> {
        history.add(Message("user", question))
        val buffer = StringBuilder()
        return repository.askAiStream(history.toList(), settings)
            .onEach { chunk -> buffer.append(chunk) }
            .onCompletion { error ->
                if (error == null && buffer.isNotEmpty()) {
                    history.add(Message("assistant", buffer.toString()))
                } else if (error != null) {
                    history.removeLastOrNull()
                }
            }
    }

    fun clearHistory() {
        history.clear()
    }
}
