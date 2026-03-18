package ru.sr.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import ru.sr.data.dto.ChatRequest
import ru.sr.data.dto.ChatResponse
import ru.sr.data.dto.Message

class DeepSeekRepository(
    private val client: HttpClient,
    private val settings: ChatSettings,
) : AiRepository {

    override suspend fun askAi(question: String): String {
        return try {
            val response: HttpResponse = client.post(URL) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $API_KEY")
                setBody(buildRequestBody(question))
            }
            if (response.status.isSuccess()) {
                response.body<ChatResponse>().choices.firstOrNull()?.message?.content ?: "Пустой ответ"
            } else {
                throw Exception(response.bodyAsText())
            }
        } catch (e: Exception) {
            "Ошибка при запросе: ${e.message}"
        }
    }

    private fun buildRequestBody(question: String) = ChatRequest(
        model = "deepseek-chat",
        messages = listOf(Message("user", question)),
        maxTokens = settings.maxTokens,
        temperature = settings.temperature,
        topP = settings.topP,
        stop = settings.stop,
        frequencyPenalty = settings.frequencyPenalty,
        presencePenalty = settings.presencePenalty,
    )

    companion object {
        private val API_KEY: String = System.getenv("DEEP_SEEK_API_KEY")
        private const val URL = "https://api.deepseek.com/v1/chat/completions"
    }
}
