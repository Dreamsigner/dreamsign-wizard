package com.duolingo.app.model

/** Space-separated count for training; not a certified FEI tokenizer or score. */
fun practiceWordCount(text: String): Int = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }

fun Exercise.WrittenProduction.canFinishPractice(text: String, reviewed: Set<Int>): Boolean =
    minWords > 0 && maxWords >= minWords && practiceWordCount(text) in minWords..maxWords &&
        reviewCriteria.isNotEmpty() && reviewCriteria.indices.all { it in reviewed }
