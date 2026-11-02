package com.dreamsigner.wizard.ui.studio

/** Renders only learner-visible session data; never serializes exercise objects or settings. */
internal fun renderStudioMarkdown(session: StudioSession): String = buildString {
    fun section(title: String, value: String) {
        if (value.isNotBlank()) append("### ").append(title).append("\n\n").append(value.trim()).append("\n\n")
    }
    append("# Séance Studio IA\n\n")
    section("Objectif", session.goal)
    session.plan?.takeIf { it.decision == "accepted" }?.let { plan ->
        append("## Plan accepté\n\n")
        plan.steps.forEachIndexed { index, step ->
            append("### ").append(index + 1).append(". ").append(step.title).append("\n\n")
            section("Critère de réussite", step.evidence)
            if (step.prerequisites.isNotEmpty()) append("Prérequis : ").append(step.prerequisites.joinToString(", ") { (it + 1).toString() }).append("\n\n")
        }
    }
    append("## Conversation\n\n")
    session.messages.forEach { message ->
        append("### ").append(if (message.role == "assistant") "Tuteur" else "Apprenant").append("\n\n")
        val visible = when (message.text) {
            STUDIO_QUIZ_REQUEST -> "QCM demandé"
            STUDIO_PREDICT_REQUEST -> "Prédiction demandée"
            STUDIO_CHECK_REQUEST -> "Vérification de compréhension demandée"
            STUDIO_PLAN_REQUEST -> "Plan pédagogique demandé"
            STUDIO_PROBLEM_REQUEST -> "Problème de pratique demandé"
            STUDIO_DIAGNOSIS_REQUEST -> "Conclusion du diagnostic demandée"
            STUDIO_GRAPH_REQUEST -> "Extraction de connaissances demandée"
            else -> if (message.text.startsWith(STUDIO_PRACTICE_ATTEMPT_PREFIX)) "Tentative envoyée" else message.text
        }
        if (visible.isNotBlank()) append(visible.trim()).append("\n\n")
        message.quiz?.let { quiz ->
            section("Question", quiz.question)
            quiz.options.forEach { append("- ").append(it.label).append("\n") }
            append("\n")
            message.quizAnswer?.let { answer ->
                section("Réponse", if (answer.dontKnow) "Je ne sais pas" else quiz.options.filter { it.id in answer.selectedIds }.joinToString(", ") { it.label })
                section("Raisonnement", answer.thinking)
                answer.confidence?.let { append("Confiance : ").append(it).append(" %\n\n") }
                section("Correction", quiz.options.filter { it.id in quiz.correctIds }.joinToString(", ") { it.label })
                section("Explication", quiz.explanation)
            }
        }
        message.problem?.let { problem ->
            section("Problème", problem.statement)
            problem.hints.take(problem.shownHints).forEachIndexed { index, hint -> section("Indice ${index + 1}", hint) }
            problem.attempt?.let { section("Réponse", it.answer); section("Raisonnement", it.thinking) }
            problem.result?.let { result -> section("Retour", result.verdict); section("Solution", problem.modelSolution) }
        }
    }
}.trimEnd().replace(Regex("(?i)(?:[a-z]:\\\\|file:///)[^\\s<>]*"), "[chemin local masqué]") + "\n"
