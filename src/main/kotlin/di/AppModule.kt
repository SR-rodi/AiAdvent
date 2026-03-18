package ru.sr.di

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import ru.sr.data.AiRepository
import ru.sr.data.DeepSeekRepository
import ru.sr.domain.usecase.SendMessageUseCase
import ru.sr.presentation.ConsoleChat

val appModule = module {
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
    single<AiRepository> { DeepSeekRepository(get()) }
    single { SendMessageUseCase(get()) }
    single { ConsoleChat(get()) }
}
