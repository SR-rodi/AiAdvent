package ru.sr.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import ru.sr.data.dto.ChatRequest
import ru.sr.data.dto.ChatResponse
import ru.sr.data.dto.Message
import ru.sr.data.dto.StreamChunk

class DeepSeekRepository(private val client: HttpClient) : AiRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun askAi(messages: List<Message>, settings: ChatSettings): String {
        return try {
            val response: HttpResponse = client.post(URL) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $API_KEY")
                setBody(buildRequestBody(messages, settings))
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

    override fun askAiStream(messages: List<Message>, settings: ChatSettings): Flow<String> = flow {
        client.preparePost(URL) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $API_KEY")
            setBody(buildRequestBody(messages, settings, stream = true))
        }.execute { response ->
            if (!response.status.isSuccess()) {
                throw Exception(response.bodyAsText())
            }
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (!line.startsWith("data: ")) continue
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") break
                val content = json.decodeFromString<StreamChunk>(data)
                    .choices.firstOrNull()?.delta?.content
                if (content != null) emit(content)
            }
        }
    }

    private fun buildRequestBody(
        messages: List<Message>,
        settings: ChatSettings,
        stream: Boolean? = null,
    ) = ChatRequest(
        model = "deepseek-chat",
        messages = messages,
        maxTokens = settings.maxTokens,
        temperature = settings.temperature,
        topP = settings.topP,
        stop = settings.stop,
        frequencyPenalty = settings.frequencyPenalty,
        presencePenalty = settings.presencePenalty,
        stream = stream,
    )

    companion object {
        private val API_KEY: String = System.getenv("DEEP_SEEK_API_KEY")
        private const val URL = "https://api.deepseek.com/v1/chat/completions"
    }
}
