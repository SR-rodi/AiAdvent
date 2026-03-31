package ru.sr.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.sr.data.dto.Message

interface AiRepository {
    suspend fun askAi(messages: List<Message>, settings: ChatSettings): String
    fun askAiStream(messages: List<Message>, settings: ChatSettings): Flow<String> =
        flow { error("Стриминг не поддерживается") }
}
