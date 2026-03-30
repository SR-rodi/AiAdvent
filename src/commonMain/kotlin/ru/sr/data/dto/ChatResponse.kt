package ru.sr.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatResponse(val choices: List<Choice>)
