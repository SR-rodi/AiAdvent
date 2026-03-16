package ru.sr.day1.dto

import kotlinx.serialization.Serializable
import ru.sr.day1.dto.Message

@Serializable
data class Choice(val message: Message)