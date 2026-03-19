package ru.sr.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.sr.data.AiRepository

class SendMessageUseCase(private val repository: AiRepository) {
    suspend fun execute(question: String): String = repository.askAi(question)
    fun executeStream(question: String): Flow<String> = repository.askAiStream(question)
}
