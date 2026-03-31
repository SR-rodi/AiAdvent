package ru.sr.data

import java.io.File

class FileResponseWriter : FileResponseWriterPort {

    override var pendingFile: String? = null
        private set

    override fun schedule(filename: String) {
        pendingFile = filename
    }

    override fun cancel() {
        pendingFile = null
    }

    override fun writeIfPending(question: String, response: String): String? {
        val filename = pendingFile ?: return null
        File(filename).writeText("# $question\n\n$response")
        pendingFile = null
        return filename
    }
}
