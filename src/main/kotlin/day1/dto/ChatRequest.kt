package ru.sr.day1.dto

import kotlinx.serialization.Serializable
import ru.sr.day1.dto.Message
import ru.sr.day1.dto.Reasoning

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val reasoning: Reasoning? = null,
    val temperature: Double = 0.2
)