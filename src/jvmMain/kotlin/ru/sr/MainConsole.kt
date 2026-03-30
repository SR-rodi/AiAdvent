package ru.sr

import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform
import ru.sr.di.commonModule
import ru.sr.di.jvmModule
import ru.sr.presentation.ConsoleChat

@Deprecated("Console mode; use MainDesktop.kt for Compose UI")
fun mainConsole() = runBlocking {
    System.setOut(java.io.PrintStream(System.out, true, "UTF-8"))
    startKoin { modules(commonModule, jvmModule) }
    KoinPlatform.getKoin().get<ConsoleChat>().start()
}
