package com.dreamsigner.wizard.ui.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.dreamsigner.wizard.data.CourseResource
import com.dreamsigner.wizard.ui.ActionRed
import com.dreamsigner.wizard.ui.Graphite
import com.dreamsigner.wizard.ui.Ink
import com.dreamsigner.wizard.ui.VideoDownloader
import com.dreamsigner.wizard.ui.youtubeVideoId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.FileAlreadyExistsException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

internal data class AudioVideoFormats(val videoId: String, val audioId: String)
internal sealed interface TrackInspection {
    data class Available(val formats: AudioVideoFormats) : TrackInspection
    data object FrenchAudioMissing : TrackInspection
}

/** Only a metadata-confirmed French audio-only format is eligible for download. */
internal fun selectFrenchTracks(metadata: String): TrackInspection {
    val root = try { Json.parseToJsonElement(metadata).jsonObject }
    catch (e: Exception) { throw IllegalArgumentException("Les métadonnées YouTube sont illisibles.", e) }
    val formats = root["formats"] as? JsonArray
        ?: throw IllegalArgumentException("YouTube n’a pas fourni la liste des pistes.")
    val candidates = formats.mapNotNull { it as? JsonObject }
        val audio = candidates.filter { format ->
        val codec = format.string("acodec") ?: "none"
        val video = format.string("vcodec") ?: "none"
        codec != "none" && video == "none" && format.languageIsFrench() && validFormatId(format.string("format_id"))
    }.maxByOrNull { it.number("abr") ?: it.number("audio_channels") ?: 0.0 }
        ?: return TrackInspection.FrenchAudioMissing
    val video = candidates.filter { format ->
        val codec = format.string("vcodec") ?: "none"
        val ext = format.string("ext")
        codec != "none" && ext == "mp4" && validFormatId(format.string("format_id"))
    }.maxWithOrNull(compareBy<JsonObject> { it.number("height") ?: 0.0 }.thenBy { it.number("fps") ?: 0.0 })
        ?: throw IllegalArgumentException("Aucun format vidéo MP4 compatible n’est disponible.")
    return TrackInspection.Available(AudioVideoFormats(video.string("format_id")!!, audio.string("format_id")!!))
}

private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.content
private fun JsonObject.number(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNull
private fun JsonObject.languageIsFrench(): Boolean {
    return isFrenchLanguage(string("language"))
}

private fun isFrenchLanguage(value: String?): Boolean {
    val language = value?.trim()?.lowercase()?.replace('_', '-') ?: return false
    return language in setOf("fr", "fra", "fre") || language.matches(Regex("fr-[a-z0-9]{2,8}(?:-[a-z0-9]{2,8})*"))
}

private fun validFormatId(value: String?): Boolean = value != null && value.matches(Regex("[A-Za-z0-9._-]{1,64}"))

internal data class CommandResult(val exitCode: Int, val stdout: String, val stderr: String)
internal interface DownloadCommandRunner {
    fun run(arguments: List<String>, timeoutMillis: Long, onStdout: (String) -> Unit = {}): CommandResult
}

internal class ProcessDownloadCommandRunner : DownloadCommandRunner {
    override fun run(arguments: List<String>, timeoutMillis: Long, onStdout: (String) -> Unit): CommandResult {
        val process = try { ProcessBuilder(arguments).start() }
        catch (_: java.io.IOException) {
            val executable = Path.of(arguments.firstOrNull().orEmpty()).fileName?.toString() ?: "l’outil vidéo"
            throw DownloadToolException("Impossible de lancer $executable. Vérifie qu’il est accessible depuis l’application.")
        }
        val stdout = StringBuilder()
        val stderr = StringBuilder()
        val outputTooLarge = AtomicBoolean(false)
        val outThread = thread(isDaemon = true, name = "video-download-stdout") {
            drain(process.inputStream, stdout, MAX_STDOUT_CHARS, outputTooLarge, onStdout)
        }
        val errThread = thread(isDaemon = true, name = "video-download-stderr") {
            drain(process.errorStream, stderr, MAX_STDERR_CHARS, null, onStdout)
        }
        try {
            val started = System.nanoTime()
            while (!process.waitFor(200, TimeUnit.MILLISECONDS)) {
                if (Thread.currentThread().isInterrupted) throw InterruptedException("Cancelled")
                if (System.nanoTime() - started > TimeUnit.MILLISECONDS.toNanos(timeoutMillis)) {
                    throw DownloadTimeoutException()
                }
                if (outputTooLarge.get()) throw IllegalStateException("La réponse de YouTube dépasse la limite autorisée.")
            }
            outThread.join(2_000); errThread.join(2_000)
            if (outputTooLarge.get()) throw IllegalStateException("La réponse de YouTube dépasse la limite autorisée.")
            return CommandResult(process.exitValue(), synchronized(stdout) { stdout.toString() }, synchronized(stderr) { stderr.toString() })
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            killProcessTree(process)
            throw e
        } catch (e: Exception) {
            killProcessTree(process)
            throw e
        } finally {
            if (process.isAlive) killProcessTree(process)
        }
    }

    private fun killProcessTree(process: Process) {
        runCatching { process.toHandle().descendants().forEach { child -> child.destroyForcibly() } }
        process.destroyForcibly()
        runCatching { process.waitFor(2, TimeUnit.SECONDS) }
    }

    private fun drain(
        input: java.io.InputStream,
        destination: StringBuilder,
        limit: Int,
        overflow: AtomicBoolean?,
        onLine: (String) -> Unit,
    ) {
        InputStreamReader(input, Charsets.UTF_8).use { reader ->
            val chars = CharArray(8192)
            val line = StringBuilder()
            var total = 0
            while (true) {
                val count = reader.read(chars)
                if (count < 0) break
                for (index in 0 until count) {
                    val char = chars[index]
                    if (total < limit) synchronized(destination) { destination.append(char) }
                    else overflow?.set(true)
                    total++
                    if (char == '\n') {
                        onLine(line.toString())
                        line.setLength(0)
                    } else if (line.length < MAX_LINE_CHARS) line.append(char)
                }
            }
            if (line.isNotEmpty()) onLine(line.toString())
        }
    }

    private companion object {
        const val MAX_STDOUT_CHARS = 8 * 1024 * 1024
        const val MAX_STDERR_CHARS = 1024 * 1024
        const val MAX_LINE_CHARS = 8 * 1024
    }
}

internal class DownloadTimeoutException : RuntimeException()
internal class DownloadToolException(message: String) : RuntimeException(message)
internal class YouTubeCommandException(message: String) : RuntimeException(message)

internal fun youtubeFailureMessage(stderr: String): String {
    val detail = stderr.lowercase()
    val message = when {
        listOf("no supported javascript runtime", "no compatible javascript runtime", "javascript runtime could not be found").any(detail::contains) ->
            "yt-dlp a besoin d’un moteur JavaScript compatible. Vérifie que Deno 2.3 ou plus récent est accessible dans PATH."
        listOf("remote component", "challenge solver script").all(detail::contains) ->
            "yt-dlp ne trouve pas son composant JavaScript pour YouTube. Vérifie son installation et réessaie."
        listOf("not a bot", "confirm you're not a bot", "confirm you’re not a bot", "confirm you are not a bot", "sign in to confirm", "http error 429", "too many requests", "http error 403").any(detail::contains) ->
            "YouTube a bloqué la lecture de cette vidéo (vérification anti-robot ou accès restreint). Aucun fichier n’a été créé."
        listOf("timed out", "timeout", "network is unreachable", "temporary failure", "unable to download api page", "connection reset", "http error 5").any(detail::contains) ->
            "Échec réseau pendant la lecture des pistes YouTube. Réessaie plus tard."
        listOf("ffmpeg was not found", "ffmpeg not found", "ffmpeg is not installed", "ffmpeg and ffprobe not found").any(detail::contains) ->
            "ffmpeg.exe est nécessaire pour assembler la vidéo et sa piste audio française."
        listOf("unsupported url", "unable to extract", "no video formats", "video unavailable", "private video", "members-only").any(detail::contains) ->
            "Impossible d’extraire les pistes de cette vidéo YouTube. Elle peut être indisponible ou restreinte."
        else -> "YouTube n’a pas pu terminer l’opération. Aucun fichier n’a été enregistré."
    }
    return if (listOf("yt-dlp version", "older than 90 days").all(detail::contains))
        "$message yt-dlp.exe est ancien. Mets à jour yt-dlp puis réessaie."
    else message
}

internal data class DownloadTools(val ytDlp: Path, val ffmpeg: Path, val ffprobe: Path)
internal fun locateDownloadTools(
    pathValue: String?,
    ytDlpOverride: String? = null,
    ffmpegOverride: String? = null,
    ffprobeOverride: String? = null,
): DownloadTools? {
    val yt = locateExecutable(ytDlpOverride, "yt-dlp", pathValue) ?: return null
    val ff = locateExecutable(ffmpegOverride, "ffmpeg", pathValue) ?: return null
    val probe = locateExecutable(ffprobeOverride, "ffprobe", pathValue) ?: return null
    return DownloadTools(yt, ff, probe)
}

private fun locateExecutable(override: String?, name: String, pathValue: String?): Path? {
    val candidates = buildList {
        if (!override.isNullOrBlank()) add(Path.of(override))
        pathValue.orEmpty().split(java.io.File.pathSeparator).filter(String::isNotBlank).forEach { dir ->
            add(Path.of(dir, "$name.exe")); add(Path.of(dir, name))
        }
    }
    return candidates.firstOrNull { Files.isRegularFile(it) && Files.isExecutable(it) }?.toAbsolutePath()?.normalize()
}

internal class FrenchYouTubeDownloadService(
    private val tools: DownloadTools,
    private val runner: DownloadCommandRunner = ProcessDownloadCommandRunner(),
) {
    suspend fun inspect(videoId: String): TrackInspection = runInterruptible(Dispatchers.IO) {
        require(videoId.matches(Regex("[A-Za-z0-9_-]{11}"))) { "Identifiant YouTube invalide." }
        val result = runner.run(listOf(
            tools.ytDlp.toString(), "--ignore-config", "--no-playlist", "--skip-download",
            "--socket-timeout", "15", "--retries", "1", "--dump-single-json", youtubeUrl(videoId),
        ), METADATA_TIMEOUT_MS)
        if (result.exitCode != 0) throw YouTubeCommandException(youtubeFailureMessage(result.stderr))
        selectFrenchTracks(result.stdout)
    }

    suspend fun download(
        videoId: String,
        tracks: AudioVideoFormats,
        target: Path,
        overwriteConfirmed: Boolean,
        onProgress: (Int) -> Unit,
    ): Path =
        runInterruptible(Dispatchers.IO) {
            require(videoId.matches(Regex("[A-Za-z0-9_-]{11}"))) { "Identifiant YouTube invalide." }
            require(validFormatId(tracks.videoId) && validFormatId(tracks.audioId)) { "Identifiants de pistes YouTube invalides." }
            require(target.fileName.toString().endsWith(".mp4", ignoreCase = true)) { "Choisis un nom de fichier MP4." }
            require(!Files.isDirectory(target)) { "Choisis un fichier MP4, pas un dossier." }
            require(!Files.isSymbolicLink(target)) { "Le fichier de destination est un lien symbolique." }
            if (Files.exists(target) && !overwriteConfirmed) throw FileAlreadyExistsException(target.toString())
            val parent = requireNotNull(target.toAbsolutePath().normalize().parent) { "Dossier de destination invalide." }
            Files.createDirectories(parent)
            val work = Files.createTempDirectory(parent, ".wizard-video-fr-")
            try {
                val videoPrefix = work.resolve("video")
                runYtDlp(videoId, tracks.videoId, videoPrefix, isFrenchAudio = false, progressBase = 0, onProgress = onProgress)
                val videoFile = findDownloadedFile(work, "video.")
                val audioPrefix = work.resolve("audio")
                runYtDlp(videoId, tracks.audioId, audioPrefix, isFrenchAudio = true, progressBase = 50, onProgress = onProgress)
                val audioFile = findDownloadedFile(work, "audio.")

                val merged = work.resolve("video-$videoId-fr.mp4")
                val merge = runner.run(listOf(
                    tools.ffmpeg.toString(), "-hide_banner", "-loglevel", "error", "-nostdin", "-y",
                    "-i", videoFile.toString(), "-i", audioFile.toString(),
                    "-map", "0:v:0", "-map", "1:a:0", "-c:v", "copy", "-c:a", "aac", "-b:a", "192k",
                    "-metadata:s:a:0", "language=fra", "-movflags", "+faststart", merged.toString(),
                ), DOWNLOAD_TIMEOUT_MS)
                if (merge.exitCode != 0) throw YouTubeCommandException(youtubeFailureMessage(merge.stderr))
                if (!Files.isRegularFile(merged) || Files.size(merged) == 0L) {
                    throw YouTubeCommandException("ffmpeg n’a pas produit de vidéo MP4 valide.")
                }
                val probe = runner.run(listOf(
                    tools.ffprobe.toString(), "-v", "error", "-show_entries",
                    "format=format_name:stream=codec_type:stream_tags=language", "-of", "json", merged.toString(),
                ), METADATA_TIMEOUT_MS)
                if (probe.exitCode != 0 || !verifiedFrenchMp4(probe.stdout)) {
                    throw YouTubeCommandException("La vidéo assemblée ne contient pas de piste audio française vérifiable.")
                }
                if (Files.exists(target) && !overwriteConfirmed) throw FileAlreadyExistsException(target.toString())
                if (overwriteConfirmed) Files.move(merged, target, StandardCopyOption.REPLACE_EXISTING)
                else Files.move(merged, target)
                onProgress(100)
                target
            } finally {
                work.toFile().deleteRecursively()
            }
        }

    private fun runYtDlp(
        videoId: String,
        formatId: String,
        outputPrefix: Path,
        isFrenchAudio: Boolean,
        progressBase: Int,
        onProgress: (Int) -> Unit,
    ) {
        val selector = if (isFrenchAudio) "$formatId[language~='(?i)^fr($|[-_])']" else formatId
        val result = runner.run(listOf(
            tools.ytDlp.toString(), "--ignore-config", "--no-playlist", "--socket-timeout", "15", "--retries", "1",
            "--newline", "--progress", "--progress-template", "download:__WIZARD_PROGRESS__%(progress._percent_str)s",
            "--output", "$outputPrefix.%(ext)s", "-f", selector, youtubeUrl(videoId),
        ), DOWNLOAD_TIMEOUT_MS) { line ->
            PROGRESS_REGEX.find(line)?.groupValues?.get(1)?.toDoubleOrNull()?.toInt()?.coerceIn(0, 100)
                ?.let { onProgress(progressBase + it / 2) }
        }
        if (result.exitCode != 0) throw YouTubeCommandException(youtubeFailureMessage(result.stderr))
    }

    private fun findDownloadedFile(directory: Path, prefix: String): Path {
        val matches = Files.list(directory).use { files ->
            files.filter { path -> path.fileName.toString().startsWith(prefix) && Files.isRegularFile(path) && !Files.isSymbolicLink(path) }.toList()
        }
        if (matches.size != 1) throw YouTubeCommandException("yt-dlp n’a pas produit le fichier de piste attendu.")
        return matches.single()
    }

    private companion object {
        const val METADATA_TIMEOUT_MS = 30_000L
        const val DOWNLOAD_TIMEOUT_MS = 4 * 60 * 60 * 1000L
        val PROGRESS_REGEX = Regex("__WIZARD_PROGRESS__\\s*([0-9]{1,3}(?:\\.[0-9]+)?)%")
    }
}

internal fun verifiedFrenchMp4(probeJson: String): Boolean {
    return try {
        val root = Json.parseToJsonElement(probeJson).jsonObject
        val format = (root["format"] as? JsonObject)?.string("format_name").orEmpty()
        val streams = root["streams"] as? JsonArray ?: return false
        val hasVideo = streams.mapNotNull { it as? JsonObject }.any { it.string("codec_type") == "video" }
        val hasFrenchAudio = streams.mapNotNull { it as? JsonObject }.any { stream ->
            if (stream.string("codec_type") != "audio") false
            else ((stream["tags"] as? JsonObject)?.string("language"))?.let(::isFrenchLanguage) == true
        }
        "mp4" in format && hasVideo && hasFrenchAudio
    } catch (_: Exception) { false }
}

private fun youtubeUrl(videoId: String) = "https://www.youtube.com/watch?v=$videoId"

internal fun chooseVideoDestination(videoId: String): Pair<Path, Boolean>? {
    val selected = callOnEdt {
        val chooser = javax.swing.JFileChooser().apply {
            dialogTitle = "Enregistrer la vidéo avec sa piste française"
            dialogType = javax.swing.JFileChooser.SAVE_DIALOG
            selectedFile = java.io.File("video-$videoId-fr.mp4")
            isAcceptAllFileFilterUsed = false
            addChoosableFileFilter(javax.swing.filechooser.FileNameExtensionFilter("Vidéo MP4", "mp4"))
        }
        if (chooser.showSaveDialog(null) != javax.swing.JFileChooser.APPROVE_OPTION) return@callOnEdt null
        var file = chooser.selectedFile
        if (!file.name.endsWith(".mp4", true)) file = java.io.File(file.parentFile, "${file.name}.mp4")
        val overwrite = file.exists() && javax.swing.JOptionPane.showConfirmDialog(
            chooser, "Ce fichier existe déjà. Le remplacer ?", "Confirmer le remplacement",
            javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE,
        ) == javax.swing.JOptionPane.YES_OPTION
        if (file.exists() && !overwrite) null else file.toPath() to overwrite
    }
    return selected
}

private fun <T> callOnEdt(block: () -> T): T {
    if (javax.swing.SwingUtilities.isEventDispatchThread()) return block()
    var result: Result<T>? = null
    javax.swing.SwingUtilities.invokeAndWait { result = runCatching(block) }
    return requireNotNull(result).getOrThrow()
}

internal class DesktopVideoDownloader(
    private val service: FrenchYouTubeDownloadService?,
    private val toolsError: String? = null,
    private val destinationPicker: (String) -> Pair<Path, Boolean>? = ::chooseVideoDestination,
) : VideoDownloader {
    @Composable
    override fun Download(resource: CourseResource, onClose: () -> Unit) {
        val videoId = remember(resource.url) { youtubeVideoId(resource.url) }
        var availability by remember(resource.url) { mutableStateOf<TrackInspection?>(null) }
        var message by remember(resource.url) { mutableStateOf("Recherche d’une piste audio française sur YouTube…") }
        var error by remember(resource.url) { mutableStateOf<String?>(null) }
        var progress by remember(resource.url) { mutableStateOf<Int?>(null) }
        var busy by remember(resource.url) { mutableStateOf(true) }
        var job by remember(resource.url) { mutableStateOf<Job?>(null) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(videoId) {
            if (videoId == null) { busy = false; error = "Cette adresse YouTube n’est pas reconnue."; return@LaunchedEffect }
            if (service == null) { busy = false; error = toolsError ?: "yt-dlp.exe, ffmpeg.exe et ffprobe.exe doivent être accessibles dans PATH."; message = "Téléchargement indisponible."; return@LaunchedEffect }
            try {
                availability = service.inspect(videoId)
                message = if (availability is TrackInspection.Available) "Une piste audio française est accessible via YouTube." else "Aucune piste audio française n’est accessible pour cette vidéo via YouTube."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error = failure.userMessage(); message = "La vérification a échoué." }
            finally { busy = false }
        }

        AlertDialog(
            onDismissRequest = { if (!busy) onClose() },
            title = { Text("Télécharger en français", color = Ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(resource.title.substringBeforeLast(" — ", resource.title), color = Graphite, fontSize = 14.sp)
                    Text(message, color = error?.let { ActionRed } ?: Ink)
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = ActionRed)
                    progress?.let { Text("Téléchargement : $it %", color = Graphite) }
                    error?.let { Text(it, color = ActionRed, fontSize = 13.sp) }
                    Text("Le fichier utilisera une piste française déjà présente sur YouTube.", color = Graphite, fontSize = 12.sp)
                }
            },
            confirmButton = {
                when {
                    busy -> CircularProgressIndicator(Modifier.width(24.dp).height(24.dp), color = ActionRed, strokeWidth = 2.dp)
                    availability is TrackInspection.Available -> Button(onClick = {
                        val id = videoId ?: return@Button
                        val picked = destinationPicker(id) ?: return@Button
                        val downloader = service ?: return@Button
                        val tracks = (availability as? TrackInspection.Available)?.formats ?: return@Button
                        busy = true; error = null; progress = 0; message = "Téléchargement de la vidéo, de la piste française et assemblage MP4…"
                        job = scope.launch {
                            try {
                                val path = downloader.download(id, tracks, picked.first, picked.second) { value -> scope.launch { progress = value } }
                                message = "Vidéo enregistrée : ${path.fileName}"
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (failure: Exception) { error = failure.userMessage(); message = "Le téléchargement a échoué." }
                            finally { busy = false; job = null }
                        }
                    }, colors = ButtonDefaults.buttonColors(backgroundColor = ActionRed, contentColor = Color.White)) { Text("Choisir le fichier") }
                }
            },
            dismissButton = {
                if (busy) TextButton(onClick = { job?.cancel(); onClose() }) { Text("Annuler", color = ActionRed) }
                else TextButton(onClick = onClose) { Text("Fermer", color = Graphite) }
            },
            properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = !busy),
        )
    }
}

internal fun defaultFrenchVideoDownloader(): DesktopVideoDownloader {
    val path = System.getenv("PATH")
    val yt = locateExecutable(System.getenv("WIZARD_YT_DLP_PATH"), "yt-dlp", path)
    val ffmpeg = locateExecutable(System.getenv("WIZARD_FFMPEG_PATH"), "ffmpeg", path)
    val ffprobe = locateExecutable(System.getenv("WIZARD_FFPROBE_PATH"), "ffprobe", path)
    val missing = buildList {
        if (yt == null) add("yt-dlp.exe")
        if (ffmpeg == null) add("ffmpeg.exe")
        if (ffprobe == null) add("ffprobe.exe")
    }
    return if (missing.isNotEmpty()) DesktopVideoDownloader(
        service = null,
        toolsError = "Outil manquant : ${missing.joinToString()}. Installe-le ou ajoute son dossier à PATH.",
    ) else DesktopVideoDownloader(FrenchYouTubeDownloadService(DownloadTools(requireNotNull(yt), requireNotNull(ffmpeg), requireNotNull(ffprobe))))
}

private fun Exception.userMessage(): String = when (this) {
    is DownloadToolException -> message ?: "Vérifie que yt-dlp.exe, ffmpeg.exe et ffprobe.exe sont accessibles."
    is YouTubeCommandException -> message ?: "YouTube n’a pas pu terminer l’opération."
    is DownloadTimeoutException -> "L’opération YouTube a dépassé le délai autorisé."
    is java.nio.file.FileAlreadyExistsException -> "Le fichier existe déjà. Choisis un autre nom ou confirme son remplacement."
    is IllegalArgumentException -> message ?: "Les métadonnées YouTube sont incomplètes ou illisibles. La disponibilité audio n’a pas pu être vérifiée."
    is CancellationException -> "Téléchargement annulé."
    else -> "Impossible d’enregistrer cette vidéo. Vérifie le dossier de destination et réessaie."
}
