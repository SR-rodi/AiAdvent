package ru.sr.data

class ChatSettings {
    var maxTokens: Int? = null
    var temperature: Double? = 1.0
    var topP: Double? = null
    var stop: List<String>? = null
    var frequencyPenalty: Double? = null
    var presencePenalty: Double? = null

    fun reset() {
        maxTokens = null
        temperature = null
        topP = null
        stop = null
        frequencyPenalty = null
        presencePenalty = null
    }

    fun format(): String = buildString {
        appendLine("maxTokens        = $maxTokens")
        appendLine("temperature      = $temperature")
        appendLine("topP             = $topP")
        appendLine("stop             = $stop")
        appendLine("frequencyPenalty = $frequencyPenalty")
        append(    "presencePenalty  = $presencePenalty")
    }
}
