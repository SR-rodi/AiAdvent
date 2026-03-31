package ru.sr.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.sr.data.dto.Message
import ru.sr.data.dto.TokenUsage

data class AiResult(val content: String, val usage: TokenUsage?)

interface AiRepository {
    suspend fun askAi(messages: List<Message>, settings: ChatSettings): AiResult
    fun askAiStream(
        messages: List<Message>,
        settings: ChatSettings,
        onUsage: ((TokenUsage) -> Unit)? = null,
    ): Flow<String> = flow { error("Стриминг не поддерживается") }
}
