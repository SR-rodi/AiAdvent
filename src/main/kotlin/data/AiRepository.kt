package ru.sr.data

interface AiRepository {
    suspend fun askAi(question: String): String
}
