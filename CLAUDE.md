# AiAdvent — подсказки для Claude

## Что за проект
Чат-клиент на Kotlin Multiplatform, который общается с LLM через API (DeepSeek, OpenRouter).
Поддерживает два режима запуска:
- **Desktop UI** — Compose Desktop (основной режим, `MainDesktop.kt`)
- **Консоль** — REPL в терминале (`MainConsole.kt`, `@Deprecated`)

Функции: стриминг ответов, несколько агентов с раздельной историей, настройки на агента, запись ответов в файл.

## Стек
- Kotlin 2.2.20, KMP (target: JVM 24)
- Compose Multiplatform 1.8.2 — Desktop UI
- Ktor 2.3.5 — HTTP-клиент (CIO engine, SSE стриминг)
- Koin 3.5.3 + koin-compose 1.1.2 — dependency injection
- Kotlinx Coroutines 1.7.3
- Kotlinx Serialization — JSON
- API-ключи задаются в `gradle.properties` и передаются как JVM-свойства через `build.gradle.kts`:
  `-DDEEP_SEEK_API_KEY`, `-DOPEN_ROUTER_API_KEY`

## Структура исходников

```
src/commonMain/kotlin/ru/sr/
  data/
    AiRepository.kt           — интерфейс репозитория
    ChatSettings.kt           — настройки на агента (mutable класс)
    DeepSeekRepository.kt     — реализация DeepSeek API (SSE стриминг)
    OpenRouterRepository.kt   — реализация OpenRouter API
    FileResponseWriterPort.kt — интерфейс записи ответов в файл (expect/actual)
    EnvProvider.kt            — expect-интерфейс чтения системных свойств
    dto/                      — ChatRequest, ChatResponse, Message, StreamChunk, Choice, Reasoning
  di/
    CommonModule.kt           — Koin: AiRepository, AgentManager, SendMessageUseCase,
                                       CommandHandler, ChatViewModel
  domain/
    agent/
      Agent.kt                — интерфейс агента
      ChatAgent.kt            — агент с историей переписки List<Message>
      AgentManager.kt         — менеджер нескольких агентов (LinkedHashMap)
    usecase/
      SendMessageUseCase.kt   — тонкая обёртка над AgentManager
  presentation/
    CommandHandler.kt         — обработка /команд
    ui/
      App.kt                  — Compose root (KoinContext + MaterialTheme)
      ChatScreen.kt           — основной экран: Row(AgentSidebar | чат | SettingsSidebar)
      ChatUiState.kt          — ChatUiState, ChatMessage, AgentSettingsUiState
      ChatViewModel.kt        — ViewModel: per-agent история, стриминг, live-настройки
      components/
        TopBar.kt             — TopAppBar + кнопка ⚙ (скрыть/показать SettingsSidebar)
        AgentSidebar.kt       — левая панель: список агентов + диалог создания нового
        SettingsSidebar.kt    — правая панель: редактирование ChatSettings в реальном времени
        InputBar.kt           — поле ввода + кнопка отправки
        MessageBubble.kt      — пузырь сообщения (User/Ai/System), SelectionContainer для копирования

src/jvmMain/kotlin/ru/sr/
  MainDesktop.kt              — точка входа Desktop UI (Compose application window)
  MainConsole.kt              — точка входа консоли (@Deprecated)
  data/
    EnvProvider.kt            — actual: System.getProperty()
    FileResponseWriter.kt     — actual: запись ответа в .md файл
  di/
    JvmModule.kt              — Koin: HttpClient (CIO, 3 мин таймаут), FileResponseWriter, ConsoleChat
  presentation/
    ConsoleChat.kt            — REPL с анимацией загрузки (@Deprecated)
```

## Ключевые концепции

### Агент (ChatAgent)
- Хранит историю `List<Message>` — передаётся в каждый запрос к API
- Имеет свои `ChatSettings` (не глобальные — у каждого агента свои)
- Стрим: история обновляется через `onCompletion` после завершения Flow

### AgentManager
- Хранит `LinkedHashMap<String, ChatAgent>`, стартует с `Agent-1`
- `createAgent(name, settings)` — создаёт агента с настройками, переключается на него
- `switchTo(name)` — переключиться, старый агент сохраняется
- `currentAgent` — текущий агент

### ChatViewModel
- Хранит `LinkedHashMap<String, List<ChatMessage>>` — отдельная UI-история для каждого агента
- При переключении агента: загружает его историю + `snapshotSettings()` → обновляет state
- `onTemperatureChanged / onMaxTokensChanged / ...` — пишут в `currentAgent.settings` и в state одновременно
- `toggleSettingsPanel()` — скрыть/показать правую панель (хранится в `ChatUiState.isSettingsPanelVisible`)

### AiRepository
- `askAi(messages, settings): String` — блокирующий запрос
- `askAiStream(messages, settings): Flow<String>` — SSE стриминг
- Настройки передаются явно — репозиторий не держит состояние

### CommandHandler
- Команды `/что-то`, парсинг: `/command = value` или `/command`
- Настройки читаются через `agentManager.currentAgent.settings`

## DI граф

**CommonModule** (commonMain):
```
AiRepository (DeepSeekRepository)
  → AgentManager
      → SendMessageUseCase
      → CommandHandler
      → ChatViewModel
```

**JvmModule** (jvmMain):
```
HttpClient (CIO, 3 мин таймаут, explicitNulls=false)
FileResponseWriter (FileResponseWriterPort)
ConsoleChat (@Deprecated)
```

`ChatSettings` создаётся внутри `AgentManager.createAndStore()` — не регистрируется в Koin.

## Команды пользователя
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

## Важные детали
- `explicitNulls = false` в JSON — null-поля не сериализуются (важно для опциональных параметров API)
- HTTP таймаут: 3 минуты (`60_000 * 3`)
- `ConsoleChat` помечен `@Deprecated` — не трогать без необходимости
- `MessageBubble` оборачивает текст в `SelectionContainer` — ответ AI можно выделить мышью
- `AgentSettingsUiState` хранит все поля как `String` — чтобы `TextField` не терял фокус при вводе
- Запуск: `JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-21-amd64-windows.2 ./gradlew run`
  (системный `JAVA_HOME` может указывать на несуществующий JDK 17)
