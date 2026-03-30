package ru.sr.domain.agent

import ru.sr.data.AiRepository
import ru.sr.data.ChatSettings

class AgentManager(private val repository: AiRepository) {

    private val agents = linkedMapOf<String, ChatAgent>()
    private var current: ChatAgent

    init {
        current = createAndStore("Agent-1")
    }

    val currentAgent: ChatAgent get() = current

    fun createAgent(name: String): String {
        if (agents.containsKey(name)) return "Агент с именем \"$name\" уже существует"
        current = createAndStore(name)
        return "Создан агент \"$name\", переключились на него"
    }

    fun switchTo(name: String): String {
        val agent = agents[name] ?: return "Агент \"$name\" не найден. Введите /agents для списка"
        current = agent
        return "Переключились на агента \"$name\""
    }

    fun currentName(): String = current.name

    fun listNames(): List<String> = agents.keys.toList()

    private fun createAndStore(name: String): ChatAgent {
        val agent = ChatAgent(name = name, repository = repository, settings = ChatSettings())
        agents[name] = agent
        return agent
    }
}
