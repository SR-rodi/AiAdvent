package ru.sr

import kotlinx.coroutines.runBlocking
import ru.sr.day1.Day1SimpleAPIRequest
import ru.sr.day1.Model

fun main() = runBlocking {
    System.setOut(java.io.PrintStream(System.out, true, "UTF-8"))
    Day1SimpleAPIRequest().doWork(Model.DeepSeek)
}