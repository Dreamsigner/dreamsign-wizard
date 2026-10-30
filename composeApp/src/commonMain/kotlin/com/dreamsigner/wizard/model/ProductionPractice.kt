package com.dreamsigner.wizard.model

/** Space-separated count for training; not a certified FEI tokenizer or score. */
fun practiceWordCount(text: String): Int = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }

fun Exercise.WrittenProduction.canFinishPractice(text: String, reviewed: Set<Int>): Boolean =
    minWords > 0 && maxWords >= minWords && practiceWordCount(text) in minWords..maxWords &&
        reviewCriteria.isNotEmpty() && reviewCriteria.indices.all { it in reviewed }

fun Exercise.SpokenProduction.canFinishPractice(durationMillis: Long?, replayed: Boolean, busy: Boolean, reviewed: Set<Int>): Boolean =
    !busy && replayed && durationMillis != null && durationMillis in minimumPracticeSeconds * 1000L..maxDurationSeconds * 1000L &&
        reviewCriteria.isNotEmpty() && reviewCriteria.indices.all { it in reviewed }
