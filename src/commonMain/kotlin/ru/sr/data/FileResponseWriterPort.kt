package ru.sr.data

interface FileResponseWriterPort {
    val pendingFile: String?
    fun schedule(filename: String)
    fun cancel()
    fun writeIfPending(question: String, response: String): String?
}
