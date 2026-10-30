package com.dreamsigner.wizard.ui.video

import com.dreamsigner.wizard.ui.youtubeVideoId
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.charset.StandardCharsets

internal data class VideoResumeEntry(val positionSeconds: Double, val durationSeconds: Double)

/** A separate, bounded file so video activity never changes course progress. */
internal class FileVideoResumeStore(
    private val path: Path,
    private val atomicReplace: (Path, Path) -> Unit = { source, target ->
        try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
        catch (_: AtomicMoveNotSupportedException) { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING) }
    },
) {
    fun read(key: String): VideoResumeEntry? = synchronized(FILE_LOCK) {
        if (!Files.exists(path)) return@synchronized null
        require(Files.size(path) <= MAX_FILE_BYTES) { "Fichier de reprise vidéo trop volumineux" }
        val root = Json.parseToJsonElement(Files.readString(path)).jsonObject
        require(root["version"]?.jsonPrimitive?.int == FORMAT_VERSION) { "Version de reprise vidéo inconnue" }
        val entries = root["entries"]!!.jsonObject
        require(entries.size <= MAX_ENTRIES) { "Trop de positions vidéo enregistrées" }
        entries.forEach { (storedKey, item) ->
            require(storedKey.length in 1..MAX_KEY_LENGTH)
            decodeEntry(item)
        }
        entries[key]?.let(::decodeEntry)
    }

    fun write(key: String, entry: VideoResumeEntry) = synchronized(FILE_LOCK) {
        require(key.length in 1..MAX_KEY_LENGTH)
        require(validEntry(entry))
        val existing = if (Files.exists(path)) {
            require(Files.size(path) <= MAX_FILE_BYTES) { "Fichier de reprise vidéo trop volumineux" }
            Json.parseToJsonElement(Files.readString(path)).jsonObject.also {
                require(it["version"]?.jsonPrimitive?.int == FORMAT_VERSION)
            }["entries"]!!.jsonObject.also { entries ->
                require(entries.size <= MAX_ENTRIES)
                entries.forEach { (storedKey, item) ->
                    require(storedKey.length in 1..MAX_KEY_LENGTH)
                    decodeEntry(item)
                }
            }.toMutableMap()
        } else linkedMapOf()
        existing[key] = buildJsonObject {
            put("positionSeconds", entry.positionSeconds)
            put("durationSeconds", entry.durationSeconds)
        }
        while (existing.size > MAX_ENTRIES) existing.remove(existing.keys.first())
        val document = buildJsonObject {
            put("version", FORMAT_VERSION)
            put("entries", JsonObject(existing))
        }.toString()
        require(document.toByteArray(StandardCharsets.UTF_8).size <= MAX_FILE_BYTES) { "Fichier de reprise vidéo trop volumineux" }
        Files.createDirectories(path.parent)
        val temporary = Files.createTempFile(path.parent, "video-playback-", ".tmp")
        try {
            Files.writeString(temporary, document)
            atomicReplace(temporary, path)
        } finally { Files.deleteIfExists(temporary) }
    }

    companion object {
        const val FORMAT_VERSION = 1
        const val MAX_ENTRIES = 500
        const val MAX_FILE_BYTES = 256 * 1024L
        const val MAX_MEDIA_SECONDS = 14.0 * 24 * 60 * 60
        private const val MAX_KEY_LENGTH = 4096
        private val FILE_LOCK = Any()

        private fun decodeEntry(element: JsonElement): VideoResumeEntry {
            val item = element.jsonObject
            val entry = VideoResumeEntry(item["positionSeconds"]!!.jsonPrimitive.double, item["durationSeconds"]!!.jsonPrimitive.double)
            require(validEntry(entry)) { "Position vidéo invalide" }
            return entry
        }

        private fun validEntry(entry: VideoResumeEntry): Boolean =
            entry.positionSeconds.isFinite() && entry.durationSeconds.isFinite() &&
                entry.positionSeconds in 0.0..MAX_MEDIA_SECONDS && entry.durationSeconds in 0.0..MAX_MEDIA_SECONDS
    }
}

internal fun videoResumeKey(sourceUrl: String): String {
    youtubeVideoId(sourceUrl)?.let { return "youtube:$it" }
    val uri = java.net.URI(sourceUrl)
    val host = uri.host.orEmpty().lowercase()
    require(uri.scheme == "https" && uri.userInfo == null && uri.port == -1 && uri.path.endsWith(".mp4", true))
    require(host == "archive.org" || host.endsWith(".archive.org") || host == "ocw.mit.edu" || host.endsWith(".mit.edu"))
    require(sourceUrl.length <= 4096)
    return "mp4:$sourceUrl"
}

internal fun restoredVideoPosition(saved: VideoResumeEntry?, actualDuration: Double): Double {
    if (saved == null || !actualDuration.isFinite() || actualDuration <= 0.0 || actualDuration > FileVideoResumeStore.MAX_MEDIA_SECONDS) return 0.0
    return saved.positionSeconds.coerceAtMost((actualDuration - 0.1).coerceAtLeast(0.0))
}

internal fun videoPositionToPersist(state: VideoPageState): VideoResumeEntry? {
    if (!state.mediaTime.isFinite() || !state.duration.isFinite() || state.mediaTime !in 0.0..FileVideoResumeStore.MAX_MEDIA_SECONDS || state.duration !in 0.0..FileVideoResumeStore.MAX_MEDIA_SECONDS) return null
    return VideoResumeEntry(if (state.ended) 0.0 else state.mediaTime, state.duration)
}

internal fun videoResumeSeekScript(youtube: Boolean, seconds: Double, youtubeId: String? = null): String {
    require(seconds.isFinite() && seconds >= 0.0)
    val target = seconds.toString()
    return if (youtube) {
        val id = requireNotNull(youtubeId).also { require(Regex("[A-Za-z0-9_-]{11}").matches(it)) }
        "(function(){var p=window.__ytPlayer;if(!p)return false;var s=p.getPlayerState();if(s===1||s===2){p.seekTo($target,true);}else{p.cueVideoById({videoId:'$id',startSeconds:$target});}return true;})()"
    }
    else "(function(){var v=document.querySelector('video');if(!v||v.readyState<1)return false;v.currentTime=$target;return true;})()"
}

internal fun videoSeekWasDispatched(result: String?, error: Throwable?): Boolean =
    error == null && result?.trim()?.trim('"') == "true"

internal fun videoRestorePositionConfirmed(expected: Double, actual: Double, duration: Double, youtube: Boolean): Boolean {
    if (!expected.isFinite() || !actual.isFinite() || !duration.isFinite()) return false
    if (expected > 0.0 && actual <= 0.0) return false
    val tolerance = if (youtube) minOf(3.0, maxOf(0.2, expected * 0.03)) else minOf(0.2, maxOf(0.02, expected * 0.1))
    return kotlin.math.abs(actual - expected) <= tolerance
}

internal fun videoRestoreTimedOut(nowNanos: Long, startedAtNanos: Long): Boolean =
    nowNanos - startedAtNanos > java.util.concurrent.TimeUnit.SECONDS.toNanos(10)

internal fun productionVideoResumeStore(): FileVideoResumeStore {
    val root = System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() } ?: System.getProperty("user.home")
    return FileVideoResumeStore(Path.of(root, "DreamsignWizard", "video-playback.json"))
}
