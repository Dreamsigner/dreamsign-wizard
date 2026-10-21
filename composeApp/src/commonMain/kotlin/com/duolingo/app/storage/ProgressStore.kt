package com.duolingo.app.storage

/** Platform-local persistence; implementations throw when a read/write fails. */
interface ProgressStore {
    fun read(): String?
    fun write(value: String)
}

// Platform factory pattern: https://kotlinlang.org/docs/multiplatform/multiplatform-expect-actual.html
expect fun createProgressStore(): ProgressStore
expect fun todayLocalDate(): String
