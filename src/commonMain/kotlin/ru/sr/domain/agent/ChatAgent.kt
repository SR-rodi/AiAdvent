package ru.sr.domain.agent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import ru.sr.data.AgentSummary
import ru.sr.data.AiRepository
import ru.sr.data.ChatHistoryPort
import ru.sr.data.ChatSettings
import ru.sr.data.SummaryPort
import ru.sr.data.dto.Message
import ru.sr.data.dto.TokenUsage

class ChatAgent(
    override val name: String,
    private val repository: AiRepository,
    val settings: ChatSettings,
    private val chatHistory: ChatHistoryPort,
    private val summaryPort: SummaryPort,
) : Agent {

    private val history = chatHistory.loadHistory(name).toMutableList()

    var tokenStats: TokenStats = TokenStats()
        private set

    private var currentSummary: AgentSummary? = summaryPort.loadSummary(name)
    private var summarizedUpTo: Int = currentSummary?.summarizedCount ?: 0

    fun historySnapshot(): List<Message> = history.toList()

    override suspend fun ask(question: String): String {
        val userMsg = Message("user", question)
        history.add(userMsg)
        chatHistory.appendMessage(name, userMsg)
        val result = repository.askAi(buildContext(), settings)
        val assistantMsg = Message("assistant", result.content)
        history.add(assistantMsg)
        chatHistory.appendMessage(name, assistantMsg)
        result.usage?.let { updateTokenStats(it) }
        maybeSummarize()
        return result.content
    }

    override fun askStream(question: String): Flow<String> {
        val userMsg = Message("user", question)
        history.add(userMsg)
        chatHistory.appendMessage(name, userMsg)
        val buffer = StringBuilder()
        return repository.askAiStream(buildContext(), settings) { usage ->
            updateTokenStats(usage)
        }
            .onEach { chunk -> buffer.append(chunk) }
            .onCompletion { error ->
                if (error == null && buffer.isNotEmpty()) {
                    val assistantMsg = Message("assistant", buffer.toString())
                    history.add(assistantMsg)
                    chatHistory.appendMessage(name, assistantMsg)
                    maybeSummarize()
                } else if (error != null) {
                    history.removeLastOrNull()
                }
            }
    }

    fun clearHistory() {
        history.clear()
        chatHistory.clearHistory(name)
        summaryPort.clearSummary(name)
        currentSummary = null
        summarizedUpTo = 0
        tokenStats = TokenStats()
    }

    private fun buildContext(): List<Message> {
        val tail = if (history.size > settings.contextWindowSize)
            history.takeLast(settings.contextWindowSize)
        else
            history.toList()
        val summary = currentSummary ?: return tail
        val systemMsg = Message("system", "Краткое содержание предыдущего диалога:\n${summary.content}")
        return listOf(systemMsg) + tail
    }

    private suspend fun maybeSummarize() {
        val unsummarizedCount = history.size - settings.contextWindowSize
        if (unsummarizedCount < summarizedUpTo + settings.summarizeEvery) return

        val batchEnd = history.size - settings.contextWindowSize
        val batch = history.subList(summarizedUpTo, batchEnd).toList()

        val newContent = generateSummary(currentSummary, batch)
        val newSummary = AgentSummary(content = newContent, summarizedCount = batchEnd)
        summaryPort.saveSummary(name, newSummary)
        currentSummary = newSummary
        summarizedUpTo = batchEnd
    }

    private suspend fun generateSummary(existing: AgentSummary?, batch: List<Message>): String {
        val summarySettings = ChatSettings().apply { temperature = 0.0 }
        val systemPrompt = Message(
            role = "system",
            content = "Ты — ассистент для создания кратких резюме диалога. " +
                "Создай сжатое, информативное резюме, сохраняя ключевые факты, решения и контекст. " +
                "Отвечай только резюме, без вступлений и комментариев.",
        )
        val userContent = buildString {
            if (existing != null) {
                appendLine("Существующее резюме (охватывает первые ${existing.summarizedCount} сообщений):")
                appendLine(existing.content)
                appendLine()
                appendLine("Новые сообщения для включения в резюме:")
            } else {
                appendLine("Сообщения для резюмирования:")
            }
            batch.forEach { msg -> appendLine("[${msg.role}]: ${msg.content}") }
        }
        return repository.askAi(listOf(systemPrompt, Message("user", userContent)), summarySettings).content
    }

    private fun updateTokenStats(usage: TokenUsage) {
        tokenStats = TokenStats(
            lastPromptTokens     = usage.promptTokens,
            lastCompletionTokens = usage.completionTokens,
            sessionTotalTokens   = tokenStats.sessionTotalTokens + usage.totalTokens,
        )
    }
}
