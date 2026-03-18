package ru.sr

import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform
import ru.sr.di.appModule
import ru.sr.presentation.ConsoleChat

fun main() = runBlocking {
    System.setOut(java.io.PrintStream(System.out, true, "UTF-8"))
    startKoin { modules(appModule) }
    KoinPlatform.getKoin().get<ConsoleChat>().start()
}
