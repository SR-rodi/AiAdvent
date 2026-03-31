package ru.sr.di

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import ru.sr.data.ChatHistoryPort
import ru.sr.data.FileResponseWriter
import ru.sr.data.FileResponseWriterPort
import ru.sr.data.SqliteChatHistory
import ru.sr.data.SqliteSummary
import ru.sr.data.SummaryPort
import ru.sr.presentation.ConsoleChat

val jvmModule = module {
    single<ChatHistoryPort> { SqliteChatHistory() }
    single<SummaryPort> { SqliteSummary() }
    single {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000 * 3
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                })
            }
        }
    }
    single<FileResponseWriterPort> { FileResponseWriter() }
    single { ConsoleChat(get(), get(), get()) }
}
