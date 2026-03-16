package ru.sr.day1.dto

import kotlinx.serialization.Serializable

@Serializable
data class Message(val role: String, val content: String)