package ru.sr.data

import java.io.File

class FileResponseWriter {

    var pendingFile: String? = null

    fun writeIfPending(question: String, response: String): String? {
        val filename = pendingFile ?: return null
        File(filename).writeText("# $question\n\n$response")
        pendingFile = null
        return filename
    }
}
