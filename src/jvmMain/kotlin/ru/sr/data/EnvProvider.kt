package ru.sr.data

actual fun getEnv(key: String): String =
    System.getenv(key) ?: System.getProperty(key) ?: ""
