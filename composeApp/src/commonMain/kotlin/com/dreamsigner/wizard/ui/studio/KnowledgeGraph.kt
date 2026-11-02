package com.dreamsigner.wizard.ui.studio

import kotlinx.serialization.Serializable

/** A reference into the current ordered conversation; indices are zero-based. */
@Serializable data class KnowledgeSource(val sessionId: String, val messageIndex: Int, val documentName: String? = null)
@Serializable data class KnowledgeProposition(val id: String, val statement: String, val sources: List<KnowledgeSource>)
@Serializable data class KnowledgeReasoning(val id: String, val premises: List<String>, val conclusion: String, val sources: List<KnowledgeSource>)
@Serializable data class KnowledgeGraph(val version: Int = 1, val propositions: List<KnowledgeProposition> = emptyList(), val reasonings: List<KnowledgeReasoning> = emptyList()) {
    /** Reject malformed or unsupported snapshots before displaying or saving them. */
    fun validate(): KnowledgeGraph {
        require(version == 1) { "Version de graphe non prise en charge" }
        require(propositions.map { it.id }.distinct().size == propositions.size)
        require(reasonings.map { it.id }.distinct().size == reasonings.size)
        val ids = propositions.map { it.id }.toSet()
        fun validSources(sources: List<KnowledgeSource>) = sources.isNotEmpty() && sources.distinct().size == sources.size && sources.all { it.sessionId.isNotBlank() && it.messageIndex >= 0 }
        propositions.forEach { require(it.id.isNotBlank() && it.statement.isNotBlank() && validSources(it.sources)) }
        reasonings.forEach {
            require(it.id.isNotBlank() && it.premises.isNotEmpty() && it.premises.distinct().size == it.premises.size)
            require(it.conclusion in ids && it.conclusion !in it.premises && it.premises.all { id -> id in ids } && validSources(it.sources))
        }
        return this
    }

    fun putProposition(value: KnowledgeProposition): KnowledgeGraph = copy(propositions = propositions.filterNot { it.id == value.id } + value).validate()
    fun putReasoning(value: KnowledgeReasoning): KnowledgeGraph = copy(reasonings = reasonings.filterNot { it.id == value.id } + value).validate()
    fun deleteProposition(id: String): KnowledgeGraph = copy(
        propositions = propositions.filterNot { it.id == id },
        reasonings = reasonings.filterNot { it.conclusion == id || id in it.premises },
    ).validate()
    fun deleteReasoning(id: String): KnowledgeGraph = copy(reasonings = reasonings.filterNot { it.id == id }).validate()

    /** Remove one session's evidence; shared assertions survive with their remaining sources. */
    fun removeSessionSources(sessionId: String): KnowledgeGraph {
        require(sessionId.isNotBlank())
        val remaining = propositions.map { it.copy(sources = it.sources.filterNot { source -> source.sessionId == sessionId }) }.filter { it.sources.isNotEmpty() }
        val ids = remaining.map { it.id }.toSet()
        val arguments = reasonings.map { it.copy(sources = it.sources.filterNot { source -> source.sessionId == sessionId }) }
            .filter { it.sources.isNotEmpty() && it.conclusion in ids && it.premises.all { id -> id in ids } }
        return copy(propositions = remaining, reasonings = arguments).validate()
    }
}
