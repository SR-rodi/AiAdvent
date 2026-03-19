package ru.sr.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface AiRepository {
    suspend fun askAi(question: String): String
    fun askAiStream(question: String): Flow<String> = flow { error("Стриминг не поддерживается") }
}
