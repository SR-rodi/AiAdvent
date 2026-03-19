package ru.sr.presentation

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.sr.domain.usecase.SendMessageUseCase
import kotlin.system.exitProcess

class ConsoleChat(
    private val sendMessageUseCase: SendMessageUseCase,
    private val commandHandler: CommandHandler,
) {

    suspend fun start() {
        println("Консольный чат-бот. Для выхода введите 'выход' или пустую строку")
        println("Введите /help для списка доступных команд")

        while (true) {
            print("Вы: ")
            val input = readlnOrNull()?.ifBlank { break } ?: break
            if (input.equals("выход", ignoreCase = true)) break

            if (input.startsWith("/")) {
                println(commandHandler.handle(input))
                continue
            }

            coroutineScope {
                val loadingJob = launch { showLoadingIndicator() }
                var firstChunk = true
                try {
                    sendMessageUseCase.executeStream(input).collect { chunk ->
                        if (firstChunk) {
                            loadingJob.cancel()
                            clearLine()
                            print("Бот: ")
                            firstChunk = false
                        }
                        print(chunk)
                    }
                } catch (e: Exception) {
                    loadingJob.cancel()
                    clearLine()
                    print("Бот: Ошибка: ${e.message}")
                }
                loadingJob.cancel()
            }
            println()
        }

        exitProcess(0)
    }

    private suspend fun showLoadingIndicator() {
        print(LOADING)
        repeat(DOT_AMOUNT) {
            delay(500)
            print(".")
        }
        clearLine()
        showLoadingIndicator()
    }

    private fun clearLine() {
        print("\r" + " ".repeat(LOADING.length + DOT_AMOUNT) + "\r")
    }

    private companion object {
        const val LOADING = "Loading"
        const val DOT_AMOUNT = 8
    }
}
