package ru.sr.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import ru.sr.data.dto.ChatRequest
import ru.sr.data.dto.ChatResponse
import ru.sr.data.dto.Message
import ru.sr.data.dto.Reasoning

class OpenRouterRepository(private val client: HttpClient) : AiRepository {

    override suspend fun askAi(messages: List<Message>, settings: ChatSettings): AiResult {
        return try {
            val response: HttpResponse = client.post(URL) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $API_KEY")
                setBody(buildRequestBody(messages))
            }
            if (response.status.isSuccess()) {
                val body = response.body<ChatResponse>()
                val content = body.choices.firstOrNull()?.message?.content ?: "Пустой ответ"
                AiResult(content, body.usage)
            } else {
                throw Exception(response.bodyAsText())
            }
        } catch (e: Exception) {
            AiResult("Ошибка при запросе: ${e.message}", null)
        }
    }

    private fun buildRequestBody(messages: List<Message>) = ChatRequest(
        model = "z-ai/glm-5-turbo",
        messages = messages,
        reasoning = Reasoning(enabled = true)
    )

    companion object {
        private val API_KEY: String = getEnv("OPEN_ROUTER_API_KEY")
        private const val URL = "https://openrouter.ai/api/v1/chat/completions"
    }
}
