package com.dreamsigner.wizard.ui.studio

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A model suggestion, never a statement about the learner's mastery. */
@Serializable internal data class ExtractedProposition(val id: String, val statement: String, val messageIndices: List<Int>)
@Serializable internal data class ExtractedReasoning(val id: String, val premises: List<String>, val conclusion: String, val messageIndices: List<Int>)
@Serializable internal data class KnowledgeExtraction(val propositions: List<ExtractedProposition>, val reasonings: List<ExtractedReasoning>)

/** The base must still be current when the user accepts this proposal. */
internal data class KnowledgeGraphProposal(val base: KnowledgeGraph, val graph: KnowledgeGraph)

internal object KnowledgeGraphExtraction {
    private val json = Json { ignoreUnknownKeys = false; isLenient = false }
    private val localId = Regex("[A-Za-z0-9_-]{1,64}")

    /** Only indices actually included in the request may be cited. */
    fun parse(reply: String, allowedMessageIndices: Set<Int>): KnowledgeExtraction {
        require(reply.length <= 100_000 && allowedMessageIndices.isNotEmpty() && allowedMessageIndices.all { it >= 0 })
        val extraction = json.decodeFromString<KnowledgeExtraction>(reply)
        require(extraction.propositions.size <= 100 && extraction.reasonings.size <= 100)
        require(extraction.propositions.map { it.id }.distinct().size == extraction.propositions.size)
        require(extraction.reasonings.map { it.id }.distinct().size == extraction.reasonings.size)
        fun sources(indices: List<Int>) = indices.isNotEmpty() && indices.size <= 20 &&
            indices.distinct().size == indices.size && indices.all { it in allowedMessageIndices }
        extraction.propositions.forEach {
            require(localId.matches(it.id) && it.statement.isNotBlank() && it.statement.length <= 2000 && sources(it.messageIndices))
        }
        val ids = extraction.propositions.map { it.id }.toSet()
        extraction.reasonings.forEach {
            require(localId.matches(it.id) && it.premises.isNotEmpty() && it.premises.size <= 20)
            require(it.premises.distinct().size == it.premises.size && it.premises.all { id -> id in ids })
            require(it.conclusion in ids && it.conclusion !in it.premises && sources(it.messageIndices))
        }
        return extraction
    }

    /** Merge exact matches only; ambiguous semantic equivalence is left to the user. */
    fun propose(graph: KnowledgeGraph, extraction: KnowledgeExtraction, sessionId: String, allowedMessageIndices: Set<Int>): KnowledgeGraphProposal {
        require(sessionId.isNotBlank())
        graph.validate()
        // Revalidate even if callers construct the value directly instead of parsing it.
        val checked = parse(json.encodeToString(KnowledgeExtraction.serializer(), extraction), allowedMessageIndices)
        val propositions = graph.propositions.toMutableList()
        val reasonings = graph.reasonings.toMutableList()
        val resolved = mutableMapOf<String, String>()
        fun nextId(prefix: String, used: Set<String>): String {
            var n = 1
            while ("$prefix$n" in used) n++
            return "$prefix$n"
        }
        fun provenance(indices: List<Int>) = indices.map { KnowledgeSource(sessionId, it) }
        checked.propositions.forEach { item ->
            val match = propositions.indexOfFirst { it.statement == item.statement }
            val id = if (match >= 0) propositions[match].id else nextId("extracted-p-", propositions.map { it.id }.toSet())
            if (match >= 0) {
                val old = propositions[match]
                propositions[match] = old.copy(sources = (old.sources + provenance(item.messageIndices)).distinct())
            } else propositions += KnowledgeProposition(id, item.statement, provenance(item.messageIndices))
            resolved[item.id] = id
        }
        checked.reasonings.forEach { item ->
            val premises = item.premises.map { resolved.getValue(it) }
            val conclusion = resolved.getValue(item.conclusion)
            require(premises.distinct().size == premises.size && conclusion !in premises) { "Propositions identiques dans un raisonnement" }
            val match = reasonings.indexOfFirst { it.conclusion == conclusion && it.premises.toSet() == premises.toSet() }
            if (match >= 0) {
                val old = reasonings[match]
                reasonings[match] = old.copy(sources = (old.sources + provenance(item.messageIndices)).distinct())
            } else {
                val id = nextId("extracted-r-", reasonings.map { it.id }.toSet())
                reasonings += KnowledgeReasoning(id, premises, conclusion, provenance(item.messageIndices))
            }
        }
        return KnowledgeGraphProposal(graph, graph.copy(propositions = propositions, reasonings = reasonings).validate())
    }
}
