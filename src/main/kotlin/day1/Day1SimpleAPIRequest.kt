package ru.sr.day1

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ru.sr.day1.dto.ChatResponse
import kotlin.system.exitProcess

class Day1SimpleAPIRequest {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun doWork(model: Model) {

        println("Консольный чат-бот (${model.javaClass.simpleName}). Для выхода введите 'выход' или пустую строку")

        while (true) {
            print("Вы: ")
            val question = readlnOrNull()?.ifBlank { break } ?: break
            if (question.equals("выход", ignoreCase = true)) break


            val answer = askAiApi(client, question, model)
            println("Бот: $answer")
        }

        client.close()
        exitProcess(0)
    }


    private suspend fun askAiApi(client: HttpClient, question: String, model: Model): String = coroutineScope {
        val loadingJob = launch { showLoadingIndicator() }
        return@coroutineScope try {
            val response: HttpResponse = client.post(model.url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer ${model.apiKey}")
                setBody(model.getRequestBody(question))
            }

            if (response.status.isSuccess()) {
                loadingJob.cancel()
                clearLine()
            } else {
                throw Exception(response.bodyAsText())
            }

            response.body<ChatResponse>().choices.firstOrNull()?.message?.content ?: "Пустой ответ"
        } catch (e: Exception) {
            loadingJob.cancel()
            clearLine()
            "Ошибка при запросе: ${e.message}"
        }
    }


    private suspend fun showLoadingIndicator() {
        print(LOADING)
        repeat(8) {
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
