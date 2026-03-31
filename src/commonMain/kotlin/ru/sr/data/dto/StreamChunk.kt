package ru.sr.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class StreamChunk(
    val choices: List<StreamChoice>,
    val usage: TokenUsage? = null,
)

@Serializable
data class StreamChoice(val delta: StreamDelta)

@Serializable
data class StreamDelta(val content: String? = null)
