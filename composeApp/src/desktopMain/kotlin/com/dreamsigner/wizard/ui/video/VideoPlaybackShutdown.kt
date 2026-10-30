package com.dreamsigner.wizard.ui.video

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

internal data class VideoPlaybackFlushResult(
    val activePlayers: Int,
    val flushedPlayers: Int,
    val failedPlayers: Int,
)

/** Called by the desktop application's close path before exitApplication/exitProcess. */
internal suspend fun flushVideoPlaybackPositions(): VideoPlaybackFlushResult = withContext(Dispatchers.IO) {
    val hosts = VideoPlaybackFlushRegistry.snapshot()
    val pending = hosts.mapNotNull { it.queueFlushForShutdown() }
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
    var flushed = 0
    var failed = 0
    pending.forEach { future ->
        try {
            val remaining = deadline - System.nanoTime()
            if (remaining <= 0L || !future.get(remaining, TimeUnit.NANOSECONDS)) failed++ else flushed++
        } catch (_: Exception) { failed++ }
    }
    // Closing a video window can already have queued its final write and removed
    // the host from the registry. Drain those writes before the process exits too.
    try {
        val remaining = deadline - System.nanoTime()
        if (remaining <= 0L || !NativeVideoHost.persistenceBarrier().get(remaining, TimeUnit.NANOSECONDS)) failed++
    } catch (_: Exception) { failed++ }
    VideoPlaybackFlushResult(hosts.size, flushed, failed)
}

internal object VideoPlaybackFlushRegistry {
    private val active = ConcurrentHashMap.newKeySet<NativeVideoHost>()
    fun register(host: NativeVideoHost) { active += host }
    fun unregister(host: NativeVideoHost) { active -= host }
    fun snapshot(): List<NativeVideoHost> = active.toList()
}

internal suspend fun closeVideoPlayersBeforeExit() = withContext(Dispatchers.IO) {
    val hosts = VideoPlaybackFlushRegistry.snapshot()
    hosts.forEach { it.close() }
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
    hosts.forEach { host ->
        val remaining = deadline - System.nanoTime()
        if (remaining > 0L) runCatching { host.disposalComplete.get(remaining, TimeUnit.NANOSECONDS) }
    }
}
