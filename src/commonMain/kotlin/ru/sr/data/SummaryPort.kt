package ru.sr.data

interface SummaryPort {
    fun loadSummary(agentName: String): AgentSummary?
    fun saveSummary(agentName: String, summary: AgentSummary)
    fun clearSummary(agentName: String)
}
