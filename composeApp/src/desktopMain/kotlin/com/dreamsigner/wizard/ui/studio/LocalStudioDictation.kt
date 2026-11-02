package com.dreamsigner.wizard.ui.studio

import kotlinx.coroutines.runInterruptible
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

internal fun interface StudioDictation {
    suspend fun transcribe(script: String, wav: ByteArray): String
}

internal data class DictationProcessResult(val exitCode: Int, val stdout: String)
internal fun interface DictationProcessRunner {
    fun run(arguments: List<String>): DictationProcessResult
}

internal fun parseDictationOutput(output: String): String {
    val text = Json.parseToJsonElement(output).jsonObject["text"]?.jsonPrimitive?.contentOrNull
        ?: throw IllegalArgumentException("Missing transcription")
    require(text.length <= 100_000)
    return text.trim()
}

internal class LocalStudioDictation(
    private val runner: DictationProcessRunner = WindowsDictationProcessRunner(),
) : StudioDictation {
    override suspend fun transcribe(script: String, wav: ByteArray): String = runInterruptible {
        require(Files.isRegularFile(Path.of(script)))
        val directory = Files.createTempDirectory("dreamsign-dictation-")
        val audio = directory.resolve("audio.wav")
        try {
            Files.write(audio, wav)
            val result = runner.run(listOf("powershell.exe", "-NoProfile", "-NonInteractive", "-File", script,
                "-Command", "transcribe", "-File", audio.toString()))
            check(result.exitCode == 0)
            parseDictationOutput(result.stdout)
        } finally {
            Files.deleteIfExists(audio)
            Files.deleteIfExists(directory)
        }
    }
}

private class WindowsDictationProcessRunner : DictationProcessRunner {
    override fun run(arguments: List<String>): DictationProcessResult {
        val process = ProcessBuilder(arguments).start()
        val output = ByteArrayOutputStream()
        val overflow = java.util.concurrent.atomic.AtomicBoolean(false)
        val stdout = thread(isDaemon = true) {
            process.inputStream.use { stream ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    if (output.size() + count <= 256 * 1024) output.write(buffer, 0, count) else overflow.set(true)
                }
            }
        }
        val stderr = thread(isDaemon = true) { process.errorStream.use { stream -> val buffer = ByteArray(8192); while (stream.read(buffer) >= 0) Unit } }
        try {
            check(process.waitFor(10, TimeUnit.MINUTES))
            stdout.join(5000); stderr.join(5000)
            check(!stdout.isAlive && !stderr.isAlive && !overflow.get())
            return DictationProcessResult(process.exitValue(), output.toString(Charsets.UTF_8))
        } finally {
            process.toHandle().descendants().forEach { it.destroyForcibly() }
            process.destroyForcibly()
        }
    }
}

