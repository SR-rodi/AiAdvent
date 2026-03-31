package ru.sr.domain.agent

import ru.sr.data.AiRepository
import ru.sr.data.ChatHistoryPort
import ru.sr.data.ChatSettings
import ru.sr.data.SummaryPort
import ru.sr.data.dto.Message

class AgentManager(
    private val repository: AiRepository,
    private val chatHistory: ChatHistoryPort,
    private val summaryPort: SummaryPort,
) {

    private val agents = linkedMapOf<String, ChatAgent>()
    private var current: ChatAgent

    init {
        val savedNames = chatHistory.listAgentNames()
        if (savedNames.isEmpty()) {
            current = createAndStore("Agent-1")
        } else {
            savedNames.forEach { createAndStore(it) }
            current = agents[savedNames.first()]!!
        }
    }

    val currentAgent: ChatAgent get() = current

    fun createAgent(name: String, settings: ChatSettings = ChatSettings()): String {
        if (agents.containsKey(name)) return "Агент с именем \"$name\" уже существует"
        current = createAndStore(name, settings)
        return "Создан агент \"$name\", переключились на него"
    }

    fun switchTo(name: String): String {
        val agent = agents[name] ?: return "Агент \"$name\" не найден. Введите /agents для списка"
        current = agent
        return "Переключились на агента \"$name\""
    }

    fun currentName(): String = current.name

    fun listNames(): List<String> = agents.keys.toList()

    fun historyOf(name: String): List<Message> = agents[name]?.historySnapshot() ?: emptyList()

    fun tokenStatsOf(name: String): TokenStats = agents[name]?.tokenStats ?: TokenStats()
    fun currentTokenStats(): TokenStats = current.tokenStats

    private fun createAndStore(name: String, settings: ChatSettings = ChatSettings()): ChatAgent {
        val agent = ChatAgent(name = name, repository = repository, settings = settings, chatHistory = chatHistory, summaryPort = summaryPort)
        agents[name] = agent
        return agent
    }
}
