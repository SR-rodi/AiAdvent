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
import ru.sr.data.FileResponseWriterPort
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

    private val _state = MutableStateFlow(
        ChatUiState(currentAgentName = agentManager.currentName())
    )
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

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

    private fun handleCommand(input: String) {
        val result = commandHandler.handle(input)
        val systemMsg = ChatMessage.System(id = nextId++, text = result)
        _state.update { state ->
            state.copy(
                messages = state.messages + systemMsg,
                currentAgentName = agentManager.currentName(),
            )
        }
    }

    private fun sendMessage(text: String) {
        val userMsg = ChatMessage.User(id = nextId++, text = text)
        val aiPlaceholder = ChatMessage.Ai(id = nextId++, text = "", isStreaming = true)

        _state.update {
            it.copy(
                messages = it.messages + userMsg + aiPlaceholder,
                isStreaming = true,
            )
        }

        val aiId = aiPlaceholder.id
        scope.launch {
            try {
                val buffer = StringBuilder()
                sendMessageUseCase.executeStream(text).collect { chunk ->
                    buffer.append(chunk)
                    val snapshot = buffer.toString()
                    _state.update { state ->
                        state.copy(messages = state.messages.map { msg ->
                            if (msg is ChatMessage.Ai && msg.id == aiId)
                                msg.copy(text = snapshot, isStreaming = true)
                            else msg
                        })
                    }
                }
                _state.update { state ->
                    state.copy(
                        isStreaming = false,
                        messages = state.messages.map { msg ->
                            if (msg is ChatMessage.Ai && msg.id == aiId)
                                msg.copy(isStreaming = false)
                            else msg
                        }
                    )
                }
                fileWriter.writeIfPending(text, buffer.toString())?.let { filename ->
                    val notice = ChatMessage.System(id = nextId++, text = "Ответ сохранён в $filename")
                    _state.update { it.copy(messages = it.messages + notice) }
                }
            } catch (e: Exception) {
                fileWriter.cancel()
                _state.update { state ->
                    state.copy(
                        isStreaming = false,
                        messages = state.messages.map { msg ->
                            if (msg is ChatMessage.Ai && msg.id == aiId)
                                msg.copy(text = "Ошибка: ${e.message}", isStreaming = false)
                            else msg
                        }
                    )
                }
            }
        }
    }

    fun clear() {
        scope.cancel()
    }
}
