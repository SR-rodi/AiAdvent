package ru.sr.data

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class SqliteSummary : SummaryPort {

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
                CREATE TABLE IF NOT EXISTS summaries (
                    agent_name       TEXT PRIMARY KEY,
                    content          TEXT NOT NULL,
                    summarized_count INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    @Synchronized
    override fun loadSummary(agentName: String): AgentSummary? {
        val sql = "SELECT content, summarized_count FROM summaries WHERE agent_name = ?"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, agentName)
            val rs = stmt.executeQuery()
            if (!rs.next()) return null
            return AgentSummary(
                content = rs.getString("content"),
                summarizedCount = rs.getInt("summarized_count"),
            )
        }
    }

    @Synchronized
    override fun saveSummary(agentName: String, summary: AgentSummary) {
        val sql = """
            INSERT OR REPLACE INTO summaries (agent_name, content, summarized_count)
            VALUES (?, ?, ?)
        """.trimIndent()
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, agentName)
            stmt.setString(2, summary.content)
            stmt.setInt(3, summary.summarizedCount)
            stmt.executeUpdate()
        }
    }

    @Synchronized
    override fun clearSummary(agentName: String) {
        val sql = "DELETE FROM summaries WHERE agent_name = ?"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, agentName)
            stmt.executeUpdate()
        }
    }
}
