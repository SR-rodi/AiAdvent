package ru.sr

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.koin.core.context.startKoin
import ru.sr.di.commonModule
import ru.sr.di.jvmModule
import ru.sr.presentation.ui.App

fun main() = application {
    startKoin {
        modules(commonModule, jvmModule)
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = "AiAdvent",
    ) {
        App()
    }
}
