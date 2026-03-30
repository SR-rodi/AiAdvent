package ru.sr.domain.agent

import kotlinx.coroutines.flow.Flow

interface Agent {
    val name: String
    suspend fun ask(question: String): String
    fun askStream(question: String): Flow<String>
}
