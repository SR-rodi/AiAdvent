package ru.sr.day1.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatResponse(val choices: List<Choice>)