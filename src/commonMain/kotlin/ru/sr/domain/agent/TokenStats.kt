package ru.sr.domain.agent

data class TokenStats(
    val lastPromptTokens: Int = 0,
    val lastCompletionTokens: Int = 0,
    val sessionTotalTokens: Int = 0,
)
