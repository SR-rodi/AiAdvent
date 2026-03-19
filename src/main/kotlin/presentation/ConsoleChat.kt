package ru.sr.presentation

import kotlinx.coroutines.flow.collect
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

            print("Бот: ")
            try {
                sendMessageUseCase.executeStream(input).collect { chunk -> print(chunk) }
            } catch (e: Exception) {
                print("Ошибка: ${e.message}")
            }
            println()
        }

        exitProcess(0)
    }
}
