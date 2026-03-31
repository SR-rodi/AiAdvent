package ru.sr.presentation.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.sr.data.ChatSettings
import ru.sr.data.FileResponseWriterPort
import ru.sr.data.dto.Message
import ru.sr.domain.agent.AgentManager
import ru.sr.domain.usecase.SendMessageUseCase
import ru.sr.presentation.CommandHandler

class ChatViewModel(
    private val sendMessageUseCase: SendMessageUseCase,
    private val commandHandler: CommandHandler,
    private val agentManager: AgentManager,
    private val fileWriter: FileResponseWriterPort,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var nextId = 0L

    // Отдельная история сообщений для каждого агента
    private val agentMessages = LinkedHashMap<String, List<ChatMessage>>().apply {
        agentManager.listNames().forEach { name ->
            put(name, agentManager.historyOf(name).map { it.toChatMessage() })
        }
    }

    private val _state = MutableStateFlow(
        ChatUiState(
            currentAgentName = agentManager.currentName(),
            agentNames = agentManager.listNames(),
            messages = getMessages(agentManager.currentName()),
            settings = snapshotSettings(),
        )
    )
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    // --- Вспомогательные функции для работы с per-agent историей ---

    private fun getMessages(agentName: String): List<ChatMessage> =
        synchronized(agentMessages) { agentMessages[agentName] ?: emptyList() }

    private fun setMessages(agentName: String, msgs: List<ChatMessage>) =
        synchronized(agentMessages) { agentMessages[agentName] = msgs }

    private fun appendMessage(agentName: String, msg: ChatMessage) =
        setMessages(agentName, getMessages(agentName) + msg)

    private fun updateAiMessage(agentName: String, id: Long, transform: (ChatMessage.Ai) -> ChatMessage.Ai) =
        setMessages(agentName, getMessages(agentName).map { msg ->
            if (msg is ChatMessage.Ai && msg.id == id) transform(msg) else msg
        })

    // --- Публичный API ---

    fun onInputChanged(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun sendInput() {
        val input = _state.value.inputText.trim()
        if (input.isBlank()) return
        _state.update { it.copy(inputText = "") }

        if (input.startsWith("/")) {
            handleCommand(input)
        } else {
            sendMessage(input)
        }
    }

    fun switchAgent(name: String) {
        agentManager.switchTo(name)
        val newName = agentManager.currentName()
        _state.update {
            it.copy(
                currentAgentName = newName,
                agentNames = agentManager.listNames(),
                messages = getMessages(newName),
                settings = snapshotSettings(),
                tokenStats = agentManager.tokenStatsOf(newName),
            )
        }
    }

    fun createAgent(name: String, settings: ChatSettings = ChatSettings()) {
        val result = agentManager.createAgent(name, settings)
        val newName = agentManager.currentName()
        synchronized(agentMessages) { agentMessages.putIfAbsent(newName, emptyList()) }
        val systemMsg = ChatMessage.System(id = nextId++, text = result)
        appendMessage(newName, systemMsg)
        _state.update {
            it.copy(
                currentAgentName = newName,
                agentNames = agentManager.listNames(),
                messages = getMessages(newName),
                showNewAgentDialog = false,
                settings = snapshotSettings(),
            )
        }
    }

    fun showNewAgentDialog() = _state.update { it.copy(showNewAgentDialog = true) }
    fun dismissNewAgentDialog() = _state.update { it.copy(showNewAgentDialog = false) }
    fun toggleSettingsPanel() = _state.update { it.copy(isSettingsPanelVisible = !it.isSettingsPanelVisible) }

    private fun snapshotSettings(): AgentSettingsUiState {
        val s = agentManager.currentAgent.settings
        return AgentSettingsUiState(
            temperature = s.temperature?.toString() ?: "",
            maxTokens = s.maxTokens?.toString() ?: "",
            topP = s.topP?.toString() ?: "",
            frequencyPenalty = s.frequencyPenalty?.toString() ?: "",
            presencePenalty = s.presencePenalty?.toString() ?: "",
            stop = s.stop?.joinToString(", ") ?: "",
        )
    }

    fun onTemperatureChanged(v: String) {
        agentManager.currentAgent.settings.temperature = v.toDoubleOrNull()
        _state.update { it.copy(settings = it.settings.copy(temperature = v)) }
    }

    fun onMaxTokensChanged(v: String) {
        agentManager.currentAgent.settings.maxTokens = v.trim().toIntOrNull()
        _state.update { it.copy(settings = it.settings.copy(maxTokens = v)) }
    }

    fun onTopPChanged(v: String) {
        agentManager.currentAgent.settings.topP = v.toDoubleOrNull()
        _state.update { it.copy(settings = it.settings.copy(topP = v)) }
    }

    fun onFrequencyPenaltyChanged(v: String) {
        agentManager.currentAgent.settings.frequencyPenalty = v.toDoubleOrNull()
        _state.update { it.copy(settings = it.settings.copy(frequencyPenalty = v)) }
    }

    fun onPresencePenaltyChanged(v: String) {
        agentManager.currentAgent.settings.presencePenalty = v.toDoubleOrNull()
        _state.update { it.copy(settings = it.settings.copy(presencePenalty = v)) }
    }

    fun onStopChanged(v: String) {
        agentManager.currentAgent.settings.stop = v.trim()
            .takeIf { it.isNotEmpty() }
            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
        _state.update { it.copy(settings = it.settings.copy(stop = v)) }
    }

    fun onSettingsReset() {
        agentManager.currentAgent.settings.reset()
        _state.update { it.copy(settings = snapshotSettings()) }
    }

    private fun handleCommand(input: String) {
        val result = commandHandler.handle(input)
        val currentName = agentManager.currentName()
        val systemMsg = ChatMessage.System(id = nextId++, text = result)
        appendMessage(currentName, systemMsg)

        // Если команда /new-agent создала нового агента, зарегистрировать его историю
        agentManager.listNames().forEach { name ->
            synchronized(agentMessages) { agentMessages.putIfAbsent(name, emptyList()) }
        }

        _state.update { state ->
            state.copy(
                messages = getMessages(currentName),
                currentAgentName = currentName,
                agentNames = agentManager.listNames(),
                tokenStats = agentManager.currentTokenStats(),
            )
        }
    }

    private fun sendMessage(text: String) {
        val currentName = agentManager.currentName()
        val userMsg = ChatMessage.User(id = nextId++, text = text)
        val aiPlaceholder = ChatMessage.Ai(id = nextId++, text = "", isStreaming = true)

        appendMessage(currentName, userMsg)
        appendMessage(currentName, aiPlaceholder)
        _state.update {
            it.copy(
                messages = getMessages(currentName),
                isStreaming = true,
            )
        }

        val aiId = aiPlaceholder.id
        scope.launch {
            try {
                val buffer = StringBuilder()
                sendMessageUseCase.executeStream(text).collect { chunk ->
                    buffer.append(chunk)
                    updateAiMessage(currentName, aiId) { it.copy(text = buffer.toString(), isStreaming = true) }
                    _state.update { state ->
                        state.copy(messages = getMessages(currentName))
                    }
                }
                updateAiMessage(currentName, aiId) { it.copy(isStreaming = false) }
                _state.update { state ->
                    state.copy(
                        isStreaming = false,
                        messages = getMessages(currentName),
                        tokenStats = agentManager.currentTokenStats(),
                    )
                }
                fileWriter.writeIfPending(text, buffer.toString())?.let { filename ->
                    val notice = ChatMessage.System(id = nextId++, text = "Ответ сохранён в $filename")
                    appendMessage(currentName, notice)
                    _state.update { it.copy(messages = getMessages(currentName)) }
                }
            } catch (e: Exception) {
                fileWriter.cancel()
                updateAiMessage(currentName, aiId) { it.copy(text = "Ошибка: ${e.message}", isStreaming = false) }
                _state.update { state ->
                    state.copy(
                        isStreaming = false,
                        messages = getMessages(currentName),
                    )
                }
            }
        }
    }

    private fun Message.toChatMessage(): ChatMessage = when (role) {
        "user"      -> ChatMessage.User(id = nextId++, text = content)
        "assistant" -> ChatMessage.Ai(id = nextId++, text = content)
        else        -> ChatMessage.System(id = nextId++, text = content)
    }

    fun clear() {
        scope.cancel()
    }
}
