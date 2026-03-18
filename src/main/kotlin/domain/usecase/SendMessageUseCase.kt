package ru.sr.domain.usecase

import ru.sr.data.AiRepository

class SendMessageUseCase(private val repository: AiRepository) {
    suspend fun execute(question: String): String = repository.askAi(question)
}
