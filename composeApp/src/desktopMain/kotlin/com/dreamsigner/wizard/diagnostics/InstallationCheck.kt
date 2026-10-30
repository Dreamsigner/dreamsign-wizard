package com.dreamsigner.wizard.diagnostics

import com.dreamsigner.wizard.data.SampleData
import com.dreamsigner.wizard.model.Subject
import com.dreamsigner.wizard.model.Exercise
import com.dreamsigner.wizard.media.recordingDurationMillis
import dreamsign_wizard.composeapp.generated.resources.Res
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

@Serializable
internal data class InstallationCheckReport(
    val courses: Int,
    val lessons: Int,
    val exercises: Int,
    val audioSha256: Map<String, String>,
    val mathRenderingVerified: Boolean = false,
    val pdfRenderingVerified: Boolean = false,
    val uiStarted: Boolean = false,
    val userDataAccessed: Boolean = false,
    val updaterInstalledEdition: Boolean = false,
    val updaterVersion: String = ""
)

@OptIn(ExperimentalResourceApi::class)
internal suspend fun checkInstallation(
    catalogue: List<Subject> = SampleData.sampleSubjects,
    readResource: suspend (String) -> ByteArray = { Res.readBytes(it) }
): InstallationCheckReport {
    check(catalogue.isNotEmpty()) { "No published subjects" }
    val ids = mutableSetOf<String>()
    fun unique(id: String) {
        check(id.matches(Regex("[A-Za-z0-9][A-Za-z0-9_-]{0,127}")) && ids.add(id)) { "Invalid or duplicate catalogue ID: $id" }
    }
    val units = catalogue.flatMap { it.units }
    check(units.isNotEmpty()) { "No published courses" }
    val lessons = units.flatMap { it.lessons }
    val exercises = lessons.flatMap { it.exercises }
    catalogue.forEach { unique(it.id) }
    units.forEach { unit ->
        unique(unit.id)
        check(unit.lessons.isNotEmpty() && unit.expectedLessonCount == unit.lessons.size) { "Incomplete course: ${unit.id}" }
    }
    lessons.forEach { lesson ->
        unique(lesson.id)
        check(lesson.learningNotes.isNotBlank() && lesson.sourceUrls.isNotEmpty() && lesson.exercises.isNotEmpty()) { "Incomplete lesson: ${lesson.id}" }
    }
    exercises.forEach { exercise ->
        unique(exercise.id)
        check(exercise.prompt.isNotBlank() && exercise.explanation.isNotBlank() && exercise.hint.isNotBlank()) { "Incomplete exercise: ${exercise.id}" }
        check(exercise !is Exercise.MatchingPairs) { "Unsupported exercise: ${exercise.id}" }
    }
    val paths = exercises.filterIsInstance<Exercise.ListeningChoice>().map { it.audioResourcePath }.distinct().sorted()
    val hashes = paths.associateWith { path ->
        // This is the same generated resource reader used by the real listening screen.
        // https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-usage.html#raw-files
        val bytes = readResource(path)
        check(recordingDurationMillis(bytes) > 0) { "Empty bundled document: $path" }
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
    }
    return InstallationCheckReport(units.size, lessons.size, exercises.size, hashes)
}

internal fun writeInstallationCheckReport(path: Path, report: InstallationCheckReport) {
    // CREATE_NEW refuses existing reports, symlinks and accidental user-file replacement.
    // https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/file/StandardOpenOption.html#CREATE_NEW
    Files.writeString(path, Json { encodeDefaults = true }.encodeToString(report), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
}
