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
import ru.sr.data.dto.StreamOptions
import ru.sr.data.dto.TokenUsage

class DeepSeekRepository(private val client: HttpClient) : AiRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun askAi(messages: List<Message>, settings: ChatSettings): AiResult {
        return try {
            val response: HttpResponse = client.post(URL) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $API_KEY")
                setBody(buildRequestBody(messages, settings))
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

    override fun askAiStream(
        messages: List<Message>,
        settings: ChatSettings,
        onUsage: ((TokenUsage) -> Unit)?,
    ): Flow<String> = flow {
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
                val chunk = json.decodeFromString<StreamChunk>(data)
                val content = chunk.choices.firstOrNull()?.delta?.content
                if (content != null) emit(content)
                if (chunk.usage != null) onUsage?.invoke(chunk.usage)
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
        streamOptions = if (stream == true) StreamOptions() else null,
    )

    companion object {
        private val API_KEY: String = getEnv("DEEP_SEEK_API_KEY")
        private const val URL = "https://api.deepseek.com/v1/chat/completions"
    }
}
