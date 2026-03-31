package ru.sr.data

class ChatSettings {
    var maxTokens: Int? = null
    var temperature: Double? = 1.0
    var topP: Double? = null
    var stop: List<String>? = null
    var frequencyPenalty: Double? = null
    var presencePenalty: Double? = null
    var contextWindowSize: Int = 20
    var summarizeEvery: Int = 10

    fun reset() {
        maxTokens = null
        temperature = null
        topP = null
        stop = null
        frequencyPenalty = null
        presencePenalty = null
        contextWindowSize = 20
        summarizeEvery = 10
    }

    fun format(): String = buildString {
        appendLine("maxTokens         = $maxTokens")
        appendLine("temperature       = $temperature")
        appendLine("topP              = $topP")
        appendLine("stop              = $stop")
        appendLine("frequencyPenalty  = $frequencyPenalty")
        appendLine("presencePenalty   = $presencePenalty")
        appendLine("contextWindowSize = $contextWindowSize")
        append(    "summarizeEvery    = $summarizeEvery")
    }
}
