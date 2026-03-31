package ru.sr.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.sr.domain.agent.AgentManager

class SendMessageUseCase(private val agentManager: AgentManager) {
    suspend fun execute(question: String): String = agentManager.currentAgent.ask(question)
    fun executeStream(question: String): Flow<String> = agentManager.currentAgent.askStream(question)
}
