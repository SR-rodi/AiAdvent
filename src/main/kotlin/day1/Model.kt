package ru.sr.day1

import ru.sr.day1.dto.ChatRequest
import ru.sr.day1.dto.Message
import ru.sr.day1.dto.Reasoning

sealed interface Model {

    val apiKey: String
    val url: String
    fun getRequestBody(question: String): ChatRequest

    object DeepSeek : Model {
        override val apiKey: String =  System.getenv("DEEP_SEEK_API_KEY")
        override val url = "https://api.deepseek.com/v1/chat/completions"

        override fun getRequestBody(question: String): ChatRequest {
            return ChatRequest(
                model = "deepseek-chat",
                messages = listOf(Message("user", question))
            )
        }
    }

    object OpenRouter : Model {
        override val apiKey: String = System.getenv("OPEN_ROUTER_API_KEY")
        override val url = "https://openrouter.ai/api/v1/chat/completions"

        override fun getRequestBody(question: String): ChatRequest {
            return ChatRequest(
                model = "z-ai/glm-5-turbo",
                messages = listOf(Message("user", question)),
                reasoning = Reasoning(enabled = true)
            )

        }
    }
}