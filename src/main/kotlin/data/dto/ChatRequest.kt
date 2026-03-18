package ru.sr.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val reasoning: Reasoning? = null,
    val temperature: Double = 0.2
)
