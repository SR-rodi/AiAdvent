package ru.sr.presentation

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.sr.domain.usecase.SendMessageUseCase
import kotlin.system.exitProcess

class ConsoleChat(private val sendMessageUseCase: SendMessageUseCase) {

    suspend fun start() {
        println("Консольный чат-бот. Для выхода введите 'выход' или пустую строку")

        while (true) {
            print("Вы: ")
            val question = readlnOrNull()?.ifBlank { break } ?: break
            if (question.equals("выход", ignoreCase = true)) break

            val answer = askWithLoading(question)
            println("Бот: $answer")
        }

        exitProcess(0)
    }

    private suspend fun askWithLoading(question: String): String = coroutineScope {
        val loadingJob = launch { showLoadingIndicator() }
        val answer = sendMessageUseCase.execute(question)
        loadingJob.cancel()
        clearLine()
        answer
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
