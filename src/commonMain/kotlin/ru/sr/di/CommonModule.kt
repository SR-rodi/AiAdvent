package ru.sr.di

import org.koin.dsl.module
import ru.sr.data.AiRepository
import ru.sr.data.DeepSeekRepository
import ru.sr.domain.agent.AgentManager
import ru.sr.domain.usecase.SendMessageUseCase
import ru.sr.presentation.CommandHandler
import ru.sr.presentation.ui.ChatViewModel

val commonModule = module {
    single<AiRepository> { DeepSeekRepository(get()) }
    single { AgentManager(get()) }
    single { SendMessageUseCase(get()) }
    single { CommandHandler(get(), get()) }
    single { ChatViewModel(get(), get(), get(), get()) }
}
