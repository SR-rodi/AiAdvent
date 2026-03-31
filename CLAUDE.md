# AiAdvent — подсказки для Claude

## Что за проект
Чат-клиент на Kotlin Multiplatform, который общается с LLM через API (DeepSeek, OpenRouter).
Поддерживает два режима запуска:
- **Desktop UI** — Compose Desktop (основной режим, `MainDesktop.kt`)
- **Консоль** — REPL в терминале (`MainConsole.kt`, помечен `@Deprecated`)

Функции: стриминг ответов, несколько агентов с раздельной историей, настройки на агента, запись ответов в файл.

## Стек
- Kotlin 2.2.20, KMP (target: JVM 24)
- Compose Multiplatform 1.8.2 — Desktop UI
- Ktor 2.3.5 — HTTP-клиент (CIO engine, SSE стриминг)
- Koin 3.5.3 + koin-compose 1.1.2 — dependency injection
- Kotlinx Coroutines 1.7.3
- Kotlinx Serialization — JSON
- API-ключи передаются через JVM-свойства: `-DDEEP_SEEK_API_KEY`, `-DOPEN_ROUTER_API_KEY`
  (задаются в `build.gradle.kts` через `findProperty` из `gradle.properties`)

## Структура исходников

```
src/commonMain/kotlin/ru/sr/
  data/
    AiRepository.kt          — интерфейс репозитория
    ChatSettings.kt          — настройки на агента (mutable)
    DeepSeekRepository.kt    — реализация для DeepSeek API
    OpenRouterRepository.kt  — реализация для OpenRouter API
    FileResponseWriterPort.kt — интерфейс записи ответов в файл
    EnvProvider.kt           — expect-интерфейс для чтения env/sys-свойств
    dto/                     — ChatRequest, ChatResponse, Message, StreamChunk, Choice, Reasoning
  di/
    CommonModule.kt          — Koin: общие зависимости (репозиторий, агенты, UseCase, CommandHandler)
  domain/
    agent/
      Agent.kt               — интерфейс агента
      ChatAgent.kt           — агент с историей переписки
      AgentManager.kt        — менеджер нескольких агентов
    usecase/
      SendMessageUseCase.kt  — делегирует AgentManager
  presentation/
    CommandHandler.kt        — обработка /команд
    ui/
      App.kt                 — Compose root (KoinContext + MaterialTheme)
      ChatScreen.kt          — основной экран (Row: AgentSidebar | чат | SettingsSidebar)
      ChatUiState.kt         — состояние UI + ChatMessage + AgentSettingsUiState
      ChatViewModel.kt       — ViewModel (per-agent история, стриминг, настройки)
      components/
        TopBar.kt            — TopAppBar с кнопкой ⚙ (скрыть/показать SettingsSidebar)
        AgentSidebar.kt      — левая панель: список агентов + кнопка создания нового
        SettingsSidebar.kt   — правая панель: редактирование ChatSettings в реальном времени
        InputBar.kt          — поле ввода + кнопка отправки
        MessageBubble.kt     — пузырь сообщения (User / Ai / System), текст выделяем мышью

src/jvmMain/kotlin/ru/sr/
  MainDesktop.kt             — точка входа Desktop (Compose window)
  MainConsole.kt             — точка входа Console (@Deprecated)
  data/
    EnvProvider.kt           — actual: читает System.getProperty()
    FileResponseWriter.kt    — actual: запись ответа в .md файл
  di/
    JvmModule.kt             — Koin: JVM-специфичные зависимости (FileResponseWriter, ConsoleChat)
  presentation/
    ConsoleChat.kt           — REPL (@Deprecated)
```

## Ключевые концепции

### Агент (ChatAgent)
- Хранит историю `List<Message>` — передаётся в каждый запрос к API
- Имеет свои `ChatSettings` (не глобальные)
- Стрим: история обновляется через `onCompletion` после завершения Flow
- `clearHistory()` — сброс истории

### AgentManager
- Хранит `LinkedHashMap<String, ChatAgent>`, стартует с `Agent-1`
- `createAgent(name, settings)` — создаёт агента с заданными настройками, переключается на него
- `switchTo(name)` — переключиться, старый агент сохраняется
- `currentAgent` — текущий агент

### ChatViewModel
- Хранит `LinkedHashMap<String, List<ChatMessage>>` — отдельная UI-история для каждого агента
- При переключении агента загружает его историю и снапшот настроек (`snapshotSettings()`)
- Методы `onTemperatureChanged`, `onMaxTokensChanged` и т.д. — пишут в `currentAgent.settings` и обновляют state
- `toggleSettingsPanel()` — скрыть/показать правую панель

### AiRepository
Интерфейс: `askAi(messages, settings): String` и `askAiStream(messages, settings): Flow<String>`
Настройки передаются явно — репозиторий не держит состояние.

### CommandHandler
Все команды `/что-то`. Настройки читаются через `agentManager.currentAgent.settings`.
Парсинг: `/command = value` или просто `/command`.

## Команды (консоль и UI)
| Команда | Описание |
|---|---|
| `/agent` | имя текущего агента |
| `/agents` | список всех агентов |
| `/new-agent = Name` | создать нового агента |
| `/switch = Name` | переключиться на агента |
| `/reset` | очистить историю и настройки текущего агента |
| `/settings` | показать настройки текущего агента |
| `/maxTokens = N` | лимит токенов (> 0) |
| `/temperature = N` | случайность (0.0..2.0) |
| `/topP = N` | nucleus sampling (0.0..1.0) |
| `/stop = s1, s2` | стоп-последовательности (до 16) |
| `/frequencyPenalty = N` | штраф за повтор (-2.0..2.0) |
| `/presencePenalty = N` | штраф за упомянутые (-2.0..2.0) |
| `/write = file.md` | записать следующий ответ в файл |
| `/help` | справка |

## DI граф

**CommonModule** (commonMain):
```
HttpClient → DeepSeekRepository (AiRepository)
  → AgentManager → SendMessageUseCase
  → CommandHandler
```

**JvmModule** (jvmMain):
```
FileResponseWriter (FileResponseWriterPort)
ChatViewModel
ConsoleChat (@Deprecated)
```

`ChatSettings` — создаётся внутри `AgentManager` при создании каждого агента (не в Koin).

## Важные детали
- `explicitNulls = false` в JSON — null-поля не сериализуются (важно для опциональных параметров API)
- HTTP таймаут: 3 минуты (`60_000 * 3`)
- `ConsoleChat` помечен `@Deprecated` — не трогать без необходимости
- `MessageBubble` оборачивает текст в `SelectionContainer` — ответ AI можно выделить мышью
- `AgentSettingsUiState` хранит поля как строки — чтобы TextField не терял фокус при вводе
- Запуск: `JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-21-amd64-windows.2 ./gradlew run`
  (JAVA_HOME по умолчанию может указывать на несуществующий JDK)
