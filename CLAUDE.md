# AiAdvent — подсказки для Claude

## Что за проект
Чат-клиент на Kotlin Multiplatform, который общается с LLM через API (DeepSeek, OpenRouter).
Поддерживает два режима запуска:
- **Desktop UI** — Compose Desktop (основной режим, `MainDesktop.kt`)
- **Консоль** — REPL в терминале (`MainConsole.kt`, `@Deprecated`)

Функции: стриминг ответов, несколько агентов с раздельной историей, настройки на агента, запись ответов в файл, персистентность истории в SQLite, счётчик токенов в TopBar, управление контекстом (скользящее окно + инкрементальное суммари).

## Стек
- Kotlin 2.2.20, KMP (target: JVM 24)
- Compose Multiplatform 1.8.2 — Desktop UI
- Ktor 2.3.5 — HTTP-клиент (CIO engine, SSE стриминг)
- Koin 3.5.3 + koin-compose 1.1.2 — dependency injection
- Kotlinx Coroutines 1.7.3
- Kotlinx Serialization — JSON
- SQLite (org.xerial:sqlite-jdbc:3.45.3.0) — персистентность истории диалогов
- API-ключи задаются в `gradle.properties` и передаются как JVM-свойства через `build.gradle.kts`:
  `-DDEEP_SEEK_API_KEY`, `-DOPEN_ROUTER_API_KEY`

## Структура исходников

```
src/commonMain/kotlin/ru/sr/
  data/
    AiRepository.kt           — интерфейс репозитория; askAi возвращает AiResult(content, usage)
    AiResult.kt               — data class AiResult(content: String, usage: TokenUsage?)
    ChatSettings.kt           — настройки на агента (mutable класс)
    ChatHistoryPort.kt        — интерфейс персистентности истории (listAgentNames, load, append, clear)
    AgentSummary.kt           — data class AgentSummary(content, summarizedCount)
    SummaryPort.kt            — интерфейс персистентности суммари (load, save, clear)
    DeepSeekRepository.kt     — реализация DeepSeek API (SSE стриминг + stream_options для usage)
    OpenRouterRepository.kt   — реализация OpenRouter API
    FileResponseWriterPort.kt — интерфейс записи ответов в файл (expect/actual)
    EnvProvider.kt            — expect-интерфейс чтения системных свойств
    dto/
      ChatRequest.kt          — запрос к API; включает StreamOptions для включения usage в стриминге
      ChatResponse.kt         — ответ API; поле usage: TokenUsage?
      StreamChunk.kt          — стриминговый чанк; поле usage: TokenUsage? (в последнем чанке)
      TokenUsage.kt           — prompt_tokens, completion_tokens, total_tokens
      Message.kt, Choice.kt, Reasoning.kt
  di/
    CommonModule.kt           — Koin: AiRepository, AgentManager, SendMessageUseCase,
                                       CommandHandler, ChatViewModel
  domain/
    agent/
      Agent.kt                — интерфейс агента
      ChatAgent.kt            — агент: история List<Message>, tokenStats, ChatHistoryPort
      AgentManager.kt         — менеджер агентов; восстанавливает агентов из БД при старте
      TokenStats.kt           — lastPromptTokens, lastCompletionTokens, sessionTotalTokens
    usecase/
      SendMessageUseCase.kt   — тонкая обёртка над AgentManager
  presentation/
    CommandHandler.kt         — обработка /команд
    ui/
      App.kt                  — Compose root (KoinContext + MaterialTheme)
      ChatScreen.kt           — основной экран: Row(AgentSidebar | чат | SettingsSidebar)
      ChatUiState.kt          — ChatUiState (+ tokenStats: TokenStats), ChatMessage, AgentSettingsUiState
      ChatViewModel.kt        — ViewModel: per-agent история, стриминг, live-настройки, tokenStats
      components/
        TopBar.kt             — TopAppBar + счётчик токенов (⬆ запрос  ⬇ ответ  Σ сессия) + кнопка ⚙
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
    SqliteChatHistory.kt      — реализация ChatHistoryPort; БД в ~/.aiAdvent/history.db
    SqliteSummary.kt          — реализация SummaryPort; таблица summaries в том же history.db
  di/
    JvmModule.kt              — Koin: HttpClient, FileResponseWriter, SqliteChatHistory, ConsoleChat
  presentation/
    ConsoleChat.kt            — REPL с анимацией загрузки (@Deprecated)
```

## Ключевые концепции

### Агент (ChatAgent)
- Хранит историю `List<Message>` — загружается из SQLite при создании агента
- Имеет свои `ChatSettings` (не глобальные — у каждого агента свои)
- Стрим: история обновляется через `onCompletion` после завершения Flow
- Хранит `tokenStats: TokenStats` — обновляется после каждого ответа API
- `clearHistory()` очищает историю и в памяти, и в SQLite, и сбрасывает tokenStats и суммари
- `buildContext()` — строит контекст для API: суммари (system) + последние `contextWindowSize` сообщений
- `maybeSummarize()` — вызывается после каждого ответа; если за пределами окна накопилось ≥ `summarizeEvery` новых сообщений, генерирует обновлённое суммари через `repository.askAi`
- Суммари хранится в `SummaryPort`; восстанавливается при перезапуске

### AgentManager
- Хранит `LinkedHashMap<String, ChatAgent>`, при старте восстанавливает всех агентов из БД
- Если БД пуста — создаёт `Agent-1`
- `createAgent(name, settings)` — создаёт агента с настройками, переключается на него
- `switchTo(name)` — переключиться, старый агент сохраняется
- `currentAgent` — текущий агент
- `tokenStatsOf(name)` / `currentTokenStats()` — доступ к статистике токенов

### ChatViewModel
- Хранит `LinkedHashMap<String, List<ChatMessage>>` — отдельная UI-история для каждого агента
- При инициализации загружает UI-историю из доменной истории всех агентов
- При переключении агента: загружает его историю + `snapshotSettings()` + `tokenStats` → обновляет state
- `onTemperatureChanged / onMaxTokensChanged / ...` — пишут в `currentAgent.settings` и в state одновременно
- `toggleSettingsPanel()` — скрыть/показать правую панель (хранится в `ChatUiState.isSettingsPanelVisible`)

### AiRepository
- `askAi(messages, settings): AiResult` — блокирующий запрос; возвращает контент + usage
- `askAiStream(messages, settings, onUsage): Flow<String>` — SSE стриминг; `onUsage` вызывается из последнего чанка
- Настройки передаются явно — репозиторий не держит состояние

### ChatHistoryPort / SqliteChatHistory
- Таблица `messages(id, agent_name, role, content, created_at)` в `~/.aiAdvent/history.db`
- `listAgentNames()` — порядок агентов по MIN(id) (порядок создания)
- Все методы `@Synchronized` — единственный `Connection` на весь процесс

### SummaryPort / SqliteSummary
- Таблица `summaries(agent_name PK, content, summarized_count)` в том же `~/.aiAdvent/history.db`
- `summarized_count` — сколько сообщений с начала истории уже вошло в суммари
- `saveSummary` использует `INSERT OR REPLACE`
- Все методы `@Synchronized`

### CommandHandler
- Команды `/что-то`, парсинг: `/command = value` или `/command`
- Настройки читаются через `agentManager.currentAgent.settings`

## DI граф

**CommonModule** (commonMain):
```
AiRepository (DeepSeekRepository)
  → AgentManager (+ ChatHistoryPort)
      → SendMessageUseCase
      → CommandHandler
      → ChatViewModel
```

**JvmModule** (jvmMain):
```
HttpClient (CIO, 3 мин таймаут, explicitNulls=false)
SqliteChatHistory (ChatHistoryPort)
SqliteSummary (SummaryPort)
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
| `/reset` | очистить историю, настройки и счётчик токенов текущего агента |
| `/settings` | показать настройки текущего агента |
| `/maxTokens = N` | лимит токенов (> 0) |
| `/temperature = N` | случайность (0.0..2.0) |
| `/topP = N` | nucleus sampling (0.0..1.0) |
| `/stop = s1, s2` | стоп-последовательности (до 16) |
| `/frequencyPenalty = N` | штраф за повтор (-2.0..2.0) |
| `/presencePenalty = N` | штраф за упомянутые (-2.0..2.0) |
| `/contextSize = N` | размер контекстного окна (> 0, по умолчанию 20) |
| `/summarizeEvery = N` | суммаризировать каждые N сообщений (> 0, по умолчанию 10) |
| `/write = file.md` | записать следующий ответ в файл |
| `/help` | справка |

## Важные детали
- `explicitNulls = false` в JSON — null-поля не сериализуются (важно для опциональных параметров API)
- HTTP таймаут: 3 минуты (`60_000 * 3`)
- `ConsoleChat` помечен `@Deprecated` — не трогать без необходимости
- `MessageBubble` оборачивает текст в `SelectionContainer` — ответ AI можно выделить мышью
- `AgentSettingsUiState` хранит все поля как `String` — чтобы `TextField` не терял фокус при вводе; включает `contextWindowSize` и `summarizeEvery`
- `stream_options: {include_usage: true}` передаётся только при стриминге — DeepSeek шлёт usage в последнем чанке (`choices: []`)
- Счётчик токенов в TopBar скрыт, пока `sessionTotalTokens == 0`
- Запуск: `JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-21-amd64-windows.2 ./gradlew run`
  (системный `JAVA_HOME` может указывать на несуществующий JDK 17)
