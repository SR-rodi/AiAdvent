package ru.sr.data

import ru.sr.data.dto.Message
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class SqliteChatHistory : ChatHistoryPort {

    private val connection: Connection

    init {
        val dbDir = File(System.getProperty("user.home"), ".aiAdvent")
        dbDir.mkdirs()
        val dbFile = File(dbDir, "history.db")
        connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
        createSchema()
        Runtime.getRuntime().addShutdownHook(Thread { connection.close() })
    }

    private fun createSchema() {
        connection.createStatement().use { stmt ->
            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS messages (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    agent_name TEXT NOT NULL,
                    role       TEXT NOT NULL,
                    content    TEXT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT (datetime('now'))
                )
                """.trimIndent()
            )
            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_messages_agent ON messages(agent_name)"
            )
        }
    }

    @Synchronized
    override fun listAgentNames(): List<String> {
        val sql = "SELECT agent_name FROM messages GROUP BY agent_name ORDER BY MIN(id)"
        connection.prepareStatement(sql).use { stmt ->
            val rs = stmt.executeQuery()
            val result = mutableListOf<String>()
            while (rs.next()) result.add(rs.getString("agent_name"))
            return result
        }
    }

    @Synchronized
    override fun loadHistory(agentName: String): List<Message> {
        val sql = "SELECT role, content FROM messages WHERE agent_name = ? ORDER BY id ASC"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, agentName)
            val rs = stmt.executeQuery()
            val result = mutableListOf<Message>()
            while (rs.next()) result.add(Message(rs.getString("role"), rs.getString("content")))
            return result
        }
    }

    @Synchronized
    override fun appendMessage(agentName: String, message: Message) {
        val sql = "INSERT INTO messages (agent_name, role, content) VALUES (?, ?, ?)"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, agentName)
            stmt.setString(2, message.role)
            stmt.setString(3, message.content)
            stmt.executeUpdate()
        }
    }

    @Synchronized
    override fun clearHistory(agentName: String) {
        val sql = "DELETE FROM messages WHERE agent_name = ?"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, agentName)
            stmt.executeUpdate()
        }
    }
}
