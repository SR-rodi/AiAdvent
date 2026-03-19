package ru.sr.di

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import ru.sr.data.AiRepository
import ru.sr.data.ChatSettings
import ru.sr.data.DeepSeekRepository
import ru.sr.data.FileResponseWriter
import ru.sr.domain.usecase.SendMessageUseCase
import ru.sr.presentation.CommandHandler
import ru.sr.presentation.ConsoleChat

val appModule = module {
    single {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                })
            }
        }
    }
    single { ChatSettings() }
    single { FileResponseWriter() }
    single<AiRepository> { DeepSeekRepository(get(), get()) }
    single { SendMessageUseCase(get()) }
    single { CommandHandler(get(), get()) }
    single { ConsoleChat(get(), get(), get()) }
}
