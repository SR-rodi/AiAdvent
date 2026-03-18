# Рефакторинг AiAdvent: Data / Domain / Presentation + Koin DI

## Контекст

Проект `AiAdvent` — консольный Kotlin/JVM-чатбот, использующий API DeepSeek/OpenRouter через Ktor.
Весь код находился в `day1/Day1SimpleAPIRequest.kt`, где были смешаны HTTP-запросы, бизнес-логика и UI.
Код выносится напрямую под `ru.sr` без папки `day1`, разбивается на три слоя с добавлением Koin DI.

---

## Целевая структура пакетов

```
src/main/kotlin/
├── Main.kt                          (ru.sr)
├── di/
│   └── AppModule.kt                 (ru.sr.di)
├── data/
│   ├── dto/
│   │   ├── ChatRequest.kt
│   │   ├── ChatResponse.kt
│   │   ├── Choice.kt
│   │   ├── Message.kt
│   │   └── Reasoning.kt             (ru.sr.data.dto)
│   ├── AiRepository.kt              (интерфейс)
│   ├── DeepSeekRepository.kt        (реализация DeepSeek)
│   └── OpenRouterRepository.kt      (реализация OpenRouter)
├── domain/
│   └── usecase/
│       └── SendMessageUseCase.kt    (ru.sr.domain.usecase)
├── presentation/
│   └── ConsoleChat.kt               (ru.sr.presentation)
└── model/
    └── Model.kt                     (ru.sr.model)
```

---

## Слои

### Data (`ru.sr.data`)
- **`AiRepository`** — интерфейс с методом `suspend fun askAi(question: String): String`
- **`DeepSeekRepository`** — реализация через DeepSeek API (`Model.DeepSeek`)
- **`OpenRouterRepository`** — реализация через OpenRouter API (`Model.OpenRouter`)
- **`dto/`** — DTO-классы для сериализации запросов/ответов API

### Domain (`ru.sr.domain`)
- **`SendMessageUseCase`** — принимает `AiRepository`, делегирует вызов `askAi(question)`

### Presentation (`ru.sr.presentation`)
- **`ConsoleChat`** — REPL-цикл, loading-индикатор, принимает `SendMessageUseCase`

### Model (`ru.sr.model`)
- **`Model`** — sealed interface с конфигурацией провайдеров `DeepSeek` и `OpenRouter`

### DI (`ru.sr.di`)
- **`AppModule`** — Koin-модуль, связывает все слои

---

## Зависимости (добавлены)

```kotlin
implementation("io.insert-koin:koin-core:3.5.3")
```

---

## Точка входа (`Main.kt`)

```kotlin
fun main() = runBlocking {
    System.setOut(PrintStream(System.out, true, "UTF-8"))
    startKoin { modules(appModule) }
    KoinPlatform.getKoin().get<ConsoleChat>().start()
}
```
