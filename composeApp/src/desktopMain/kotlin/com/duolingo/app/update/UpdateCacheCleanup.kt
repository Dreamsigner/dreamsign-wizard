package com.duolingo.app.update

import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

/** Best-effort cleanup of updater-owned files in its dedicated, flat cache directory. */
internal object UpdateCacheCleanup {
    private val installerName = Regex("AtelierApprentissage-(0|[1-9][0-9]{0,3})\\.(0|[1-9][0-9]{0,3})\\.(0|[1-9][0-9]{0,3})-Setup\\.exe")
    private val staleNames = listOf(
        Regex("download-[A-Za-z0-9._-]+\\.part"),
        Regex("preferences-[A-Za-z0-9._-]+\\.tmp"),
        Regex("apply-[A-Za-z0-9._-]+\\.json"),
    )
    private val staleAfter = Duration.ofHours(24)

    fun onStartup(folder: Path, installedVersion: AppVersion) {
        removeInstalledAndOlder(folder, installedVersion)
        removeStaleTemporaryFiles(folder)
    }

    fun beforeCheck(folder: Path, installedVersion: AppVersion) = onStartup(folder, installedVersion)

    fun keepOnlyPrepared(folder: Path, installer: Path) {
        val keep = installer.toAbsolutePath().normalize()
        visit(folder) { path ->
            installerVersion(path) ?: return@visit
            if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) && path.toAbsolutePath().normalize() != keep) {
                // Once a replacement has been fully verified, only that verified installer remains useful.
                runCatching { Files.deleteIfExists(path) }
            }
        }
    }

    private fun removeInstalledAndOlder(folder: Path, installedVersion: AppVersion) = visit(folder) { path ->
        val cachedVersion = installerVersion(path) ?: return@visit
        if (cachedVersion <= installedVersion && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            runCatching { Files.deleteIfExists(path) }
        }
    }

    private fun removeStaleTemporaryFiles(folder: Path) = visit(folder) { path ->
        if (!staleNames.any { it.matches(path.fileName.toString()) } || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) return@visit
        runCatching {
            val modified = Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toInstant()
            if (modified <= Instant.now().minus(staleAfter)) Files.deleteIfExists(path)
        }
    }

    private fun installerVersion(path: Path): AppVersion? {
        val match = installerName.matchEntire(path.fileName.toString()) ?: return null
        return AppVersion.parse("${match.groupValues[1]}.${match.groupValues[2]}.${match.groupValues[3]}")
    }

    private inline fun visit(folder: Path, action: (Path) -> Unit) {
        // Do not traverse a replaced cache directory, links, subdirectories, or any other location.
        if (!Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS)) return
        try {
            Files.newDirectoryStream(folder).use { entries ->
                for (path in entries) runCatching { action(path) }
            }
        } catch (_: Exception) {
            // Cache cleanup must never prevent update checking or installation.
        }
    }
}
