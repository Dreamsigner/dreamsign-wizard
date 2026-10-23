package com.duolingo.app.update

import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration

internal interface UpdateTransport {
    fun metadata(): String?
    fun checksum(url: String): String
    fun download(release: UpdateRelease, target: Path, progress: (Long) -> Unit)
}

internal fun allowedUpdateLocation(uri: URI): Boolean = uri.scheme == "https" && uri.userInfo == null &&
    (uri.port == -1 || uri.port == 443) && uri.host in setOf("api.github.com", "github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com")

internal class GithubUpdateTransport : UpdateTransport {
    private fun connection(url: String): HttpURLConnection {
        var next = URI(url)
        repeat(6) {
            require(allowedUpdateLocation(next))
            val c = next.toURL().openConnection() as HttpURLConnection
            c.instanceFollowRedirects = false; c.connectTimeout = 8000; c.readTimeout = 15000
            c.setRequestProperty("User-Agent", "AtelierApprentissage-Updater")
            c.setRequestProperty("Accept", if (next.host == "api.github.com") "application/vnd.github+json" else "application/octet-stream")
            val status = c.responseCode
            if (status in listOf(301, 302, 303, 307, 308)) {
                val redirect = c.getHeaderField("Location"); c.disconnect()
                requireNotNull(redirect); next = next.resolve(redirect)
            } else return c
        }
        error("Too many redirects")
    }
    private fun text(url: String, max: Int, missingAllowed: Boolean = false): String? {
        val c = connection(url)
        try {
            if (missingAllowed && c.responseCode == 404) return null
            require(c.responseCode == 200 && c.contentLengthLong <= max)
            val bytes = c.inputStream.use { it.readNBytes(max + 1) }
            require(bytes.size <= max)
            return bytes.toString(Charsets.UTF_8)
        } finally { c.disconnect() }
    }
    override fun metadata() = text(LATEST_RELEASE, 524288, true)
    override fun checksum(url: String) = requireNotNull(text(url, 4096))
    override fun download(release: UpdateRelease, target: Path, progress: (Long) -> Unit) {
        val c = connection(release.installerUrl)
        try {
            require(c.responseCode == 200 && c.contentLengthLong <= MAX_INSTALLER_BYTES)
            val started = System.nanoTime()
            c.inputStream.use { input -> Files.newOutputStream(target).use { output ->
                var total = 0L; val buffer = ByteArray(65536)
                while (true) {
                    check(!Thread.currentThread().isInterrupted)
                    require(System.nanoTime() - started < Duration.ofMinutes(10).toNanos())
                    val count = input.read(buffer); if (count < 0) break
                    total += count; require(total <= release.size && total <= MAX_INSTALLER_BYTES)
                    output.write(buffer, 0, count); progress(total)
                }
                require(total == release.size)
            } }
        } finally { c.disconnect() }
    }
}

internal data class PreparedUpdate(val release: UpdateRelease, val installer: Path, val sha256: String)
internal class UpdateRepository(private val folder: Path, private val transport: UpdateTransport) {
    fun prepare(current: AppVersion, progress: (Long, Long) -> Unit): PreparedUpdate? {
        UpdateCacheCleanup.beforeCheck(folder, current)
        val release = parseUpdateRelease(transport.metadata() ?: return null, current) ?: return null
        Files.createDirectories(folder)
        val expected = installerChecksum(transport.checksum(release.checksumUrl), release.fileName)
        val target = folder.resolve(release.fileName)
        require(!Files.isSymbolicLink(target))
        if (Files.isRegularFile(target) && Files.size(target) == release.size && fileSha256(target) == expected) {
            UpdateCacheCleanup.keepOnlyPrepared(folder, target)
            return PreparedUpdate(release, target, expected)
        }
        val partial = Files.createTempFile(folder, "download-", ".part")
        try {
            transport.download(release, partial) { progress(it, release.size) }
            require(Files.size(partial) == release.size && fileSha256(partial) == expected) { "Installer integrity check failed" }
            Files.newInputStream(partial).use { require(it.read() == 'M'.code && it.read() == 'Z'.code) }
            Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING)
            UpdateCacheCleanup.keepOnlyPrepared(folder, target)
            return PreparedUpdate(release, target, expected)
        } finally { Files.deleteIfExists(partial) }
    }
}
