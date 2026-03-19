package ru.sr.presentation

import ru.sr.data.ChatSettings
import ru.sr.data.FileResponseWriter

class CommandHandler(
    private val settings: ChatSettings,
    private val fileWriter: FileResponseWriter,
) {

    private data class CommandDef(
        val description: String,
        val usage: String? = null,
        val execute: (String?) -> String,
    )

    private val commands: Map<String, CommandDef> = buildCommands()

    fun handle(input: String): String {
        val (name, rawValue) = parseInput(input)
        return commands[name]?.execute(rawValue)
            ?: "Неизвестная команда: /$name. Введите /help для справки"
    }

    private fun buildCommands(): Map<String, CommandDef> = linkedMapOf(
        "help" to CommandDef("эта справка") { _ ->
            generateHelp()
        },
        "settings" to CommandDef("показать текущие значения параметров") { _ ->
            settings.format()
        },
        "reset" to CommandDef("сбросить все параметры к null") { _ ->
            settings.reset()
            "Параметры сброшены"
        },
        "maxTokens" to CommandDef("лимит токенов в ответе (> 0)", "= <int>") { v ->
            setNullableInt("maxTokens", v, min = 1) { settings.maxTokens = it }
        },
        "temperature" to CommandDef("случайность ответа", "= <0.0..2.0>") { v ->
            setNullableDouble("temperature", v, 0.0, 2.0) { settings.temperature = it }
        },
        "topP" to CommandDef("nucleus sampling", "= <0.0..1.0>") { v ->
            setNullableDouble("topP", v, 0.0, 1.0) { settings.topP = it }
        },
        "stop" to CommandDef("стоп-последовательности через запятую (до 16)", "= <s1, s2, ...>") { v ->
            setStop(v)
        },
        "frequencyPenalty" to CommandDef("штраф за повтор токенов", "= <-2.0..2.0>") { v ->
            setNullableDouble("frequencyPenalty", v, -2.0, 2.0) { settings.frequencyPenalty = it }
        },
        "presencePenalty" to CommandDef("штраф за уже упомянутые токены", "= <-2.0..2.0>") { v ->
            setNullableDouble("presencePenalty", v, -2.0, 2.0) { settings.presencePenalty = it }
        },
        "write" to CommandDef("записать следующий ответ в файл", "= <filename.md>") { v ->
            if (v.isNullOrBlank()) return@CommandDef "Укажите имя файла: /write = name.md"
            if (v.trim() == "null") {
                fileWriter.pendingFile = null
                return@CommandDef "Запись отменена"
            }
            fileWriter.pendingFile = v.trim()
            "Следующий ответ будет записан в: ${v.trim()}"
        },
    )

    private fun parseInput(input: String): Pair<String, String?> {
        val s = input.removePrefix("/")
        val i = s.indexOf('=')
        return if (i == -1) s.trim() to null
        else s.substring(0, i).trim() to s.substring(i + 1).trim()
    }

    private fun generateHelp(): String = buildString {
        appendLine("Доступные команды:")
        commands.forEach { (name, def) ->
            val usage = def.usage?.let { " $it" } ?: ""
            appendLine("  /$name$usage — ${def.description}")
        }
        append("Для сброса параметра используйте: /paramName = null")
    }

    private fun setNullableInt(name: String, raw: String?, min: Int, setter: (Int?) -> Unit): String {
        if (raw == null) return "Укажите значение: /$name = <число>"
        if (raw == "null") {
            setter(null)
            return "$name сброшен"
        }
        val v = raw.toIntOrNull()
            ?: return "Ошибка: $name ожидает целое число (>= $min), получено \"$raw\""
        if (v < min) return "Ошибка: $name должен быть >= $min"
        setter(v)
        return "$name = $v"
    }

    private fun setNullableDouble(
        name: String,
        raw: String?,
        min: Double,
        max: Double,
        setter: (Double?) -> Unit,
    ): String {
        if (raw == null) return "Укажите значение: /$name = <число>"
        if (raw == "null") {
            setter(null)
            return "$name сброшен"
        }
        val v = raw.toDoubleOrNull()
            ?: return "Ошибка: $name ожидает число ($min..$max), получено \"$raw\""
        if (v < min || v > max) return "Ошибка: $name должен быть в диапазоне $min..$max"
        setter(v)
        return "$name = $v"
    }

    private fun setStop(raw: String?): String {
        if (raw == null) return "Укажите значение: /stop = <s1, s2, ...>"
        if (raw == "null") {
            settings.stop = null
            return "stop сброшен"
        }
        val list = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (list.size > 16) return "Ошибка: stop принимает до 16 последовательностей, получено ${list.size}"
        settings.stop = list
        return "stop = $list"
    }
}
