@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package com.duolingo.app.update

import androidx.compose.material.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asSkiaBitmap
import com.duolingo.app.LearningAppTheme
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import org.jetbrains.skia.Image
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.*
import kotlin.test.*

class WindowsUpdateTest {
    @Test fun badgeCountsOnlyUpdatesNeedingAction() {
        val idle = UpdateUiState("1.6.0", true, true)
        assertEquals(0, idle.actionCount)
        assertEquals(0, idle.copy(busy = true, statusMessage = "Téléchargement : 13 %").actionCount)
        assertEquals(1, idle.copy(ready = true).actionCount)
        assertEquals(1, idle.copy(checkError = "Échec du contrôle").actionCount)
        assertEquals(2, idle.copy(ready = true, checkError = "Échec du contrôle").actionCount)
        assertEquals(4, idle.copy(ready = true, preferenceError = "Préférences", checkError = "Contrôle", installationError = "Installation").actionCount)
    }

    @Test fun failedCheckNeedsAttentionUntilSuccessfulRetry() = runBlocking {
        val dir = Files.createTempDirectory("updater-attention-")
        var fail = true
        val transport = object : UpdateTransport {
            override fun metadata(): String? = if (fail) error("Offline") else null
            override fun checksum(url: String): String = error("No release")
            override fun download(release: UpdateRelease, target: Path, progress: (Long) -> Unit) = error("No release")
        }
        val controller = WindowsUpdateController(dir, dir.resolve("fixture.exe"), AppVersion(1, 6, 0), UpdateRepository(dir, transport))
        try {
            controller.checkNow()
            withTimeout(5000) { while (!controller.ui.value.requiresAttention) delay(10) }
            assertEquals(1, controller.ui.value.actionCount)
            fail = false
            controller.checkNow()
            withTimeout(5000) { while (controller.ui.value.requiresAttention || controller.ui.value.busy) delay(10) }
            assertEquals(0, controller.ui.value.actionCount)
        } finally { controller.close(); dir.toFile().deleteRecursively() }
    }

    @Test fun successfulCheckDoesNotClearPreferenceSaveErrorAndSuccessfulSaveDoes() = runBlocking {
        val root = Files.createTempDirectory("updater-preference-recovery-")
        val dir = root.resolve("blocked-folder")
        Files.writeString(dir, "not a directory")
        val transport = object : UpdateTransport {
            override fun metadata(): String? { Thread.sleep(30); return null }
            override fun checksum(url: String): String = error("No release")
            override fun download(release: UpdateRelease, target: Path, progress: (Long) -> Unit) = error("No release")
        }
        val controller = WindowsUpdateController(dir, dir.resolve("fixture.exe"), old, UpdateRepository(dir, transport))
        try {
            controller.setAutomatic(false)
            withTimeout(5000) { while (controller.ui.value.preferenceError == null) delay(10) }
            assertEquals(1, controller.ui.value.actionCount)

            controller.checkNow()
            withTimeout(5000) { while (!controller.ui.value.busy) delay(10) }
            withTimeout(5000) { while (controller.ui.value.busy) delay(10) }
            assertNull(controller.ui.value.checkError)
            assertNotNull(controller.ui.value.preferenceError)
            assertEquals(1, controller.ui.value.actionCount)
            assertFalse("Application à jour" in controller.ui.value.message)
            assertTrue("Impossible d’enregistrer" in controller.ui.value.message)

            Files.delete(dir)
            Files.createDirectories(dir)
            controller.setAutomatic(false)
            withTimeout(5000) { while (controller.ui.value.preferenceError != null) delay(10) }
            assertEquals(0, controller.ui.value.actionCount)
        } finally { controller.close(); root.toFile().deleteRecursively() }
    }

    @Test fun startupInstallationFailureSurvivesSuccessfulCheckWithNoUpdate() = runBlocking {
        val dir = Files.createTempDirectory("updater-install-failure-")
        Files.writeString(dir.resolve("last-result.json"), "{\"success\":false,\"error\":\"Installer failed\"}")
        val fixture = dir.resolve("fixture.exe")
        Files.write(fixture, bytes)
        var releaseAvailable = false
        val transport = object : UpdateTransport {
            override fun metadata(): String? {
                Thread.sleep(30)
                return if (releaseAvailable) this@WindowsUpdateTest.metadata() else null
            }
            override fun checksum(url: String): String = "${fileSha256(fixture)}  $name"
            override fun download(release: UpdateRelease, target: Path, progress: (Long) -> Unit) {
                Files.write(target, bytes)
                progress(bytes.size.toLong())
            }
        }
        var installs = 0
        val controller = WindowsUpdateController(dir, fixture, old, UpdateRepository(dir, transport)) { _, _, _ -> installs++ }
        try {
            assertNotNull(controller.ui.value.installationError)
            controller.checkNow()
            withTimeout(5000) { while (!controller.ui.value.busy) delay(10) }
            withTimeout(5000) { while (controller.ui.value.busy) delay(10) }
            assertNull(controller.ui.value.checkError)
            assertNotNull(controller.ui.value.installationError)
            assertEquals(1, controller.ui.value.actionCount)
            assertTrue("La dernière installation a échoué" in controller.ui.value.message)
            assertFalse("Application à jour" in controller.ui.value.message)

            releaseAvailable = true
            controller.checkNow()
            withTimeout(5000) { while (!controller.ui.value.ready) delay(10) }
            assertNotNull(controller.ui.value.installationError)
            assertEquals(2, controller.ui.value.actionCount)
            assertTrue("La dernière installation a échoué" in controller.ui.value.message)
            assertTrue("Version 1.4.0 prête" in controller.ui.value.message)
            assertTrue(controller.beforeClose(restart = true))
            assertEquals(1, installs)
        } finally { controller.close(); dir.toFile().deleteRecursively() }
    }

    private val old = AppVersion(1, 3, 0)
    private val bytes = "MZfixture installer".toByteArray()
    private val name = "AtelierApprentissage-1.4.0-Setup.exe"
    private fun metadata(version: String = "1.4.0") = """{"tag_name":"v$version","draft":false,"prerelease":false,"assets":[
      {"name":"AtelierApprentissage-$version-Setup.exe","size":${bytes.size},"state":"uploaded","browser_download_url":"$UPDATE_REPO/releases/download/v$version/AtelierApprentissage-$version-Setup.exe"},
      {"name":"SHA256SUMS.txt","state":"uploaded","browser_download_url":"$UPDATE_REPO/releases/download/v$version/SHA256SUMS.txt"}]}"""

    @Test fun comparesNumericVersionsAndRejectsPrereleaseOrMalformedVersions() {
        assertTrue(AppVersion(1, 10, 0) > AppVersion(1, 9, 99))
        listOf("1.0", "1.2.3-beta", "1.2.3.4", "01.2.3", "99999.0.0", "../1.0.0").forEach { assertNull(AppVersion.parse(it)) }
        assertEquals(AppVersion(1, 4, 0), AppVersion.parse("1.4.0"))
        assertNull(parseUpdateRelease(metadata("1.3.0"), old))
        assertNull(parseUpdateRelease(metadata("1.2.9"), old))
    }
    @Test fun rejectsDraftWrongHostMissingOrDuplicateAssetsAndOversize() {
        val valid = metadata()
        assertEquals(name, parseUpdateRelease(valid, old)!!.fileName)
        listOf(valid.replace("\"prerelease\":false", "\"prerelease\":true"),
            valid.replace("\"draft\":false", "\"draft\":true"),
            valid.replace("https://github.com/", "https://github.com.evil/"),
            valid.replace("SHA256SUMS.txt", "missing.txt"),
            valid.replace("\"size\":${bytes.size}", "\"size\":${MAX_INSTALLER_BYTES + 1}"),
            valid.replace("\"state\":\"uploaded\"", "\"state\":\"new\"")).forEach { text -> assertFails { parseUpdateRelease(text, old) } }
        val obj = kotlinx.serialization.json.Json.parseToJsonElement(valid).toString()
        assertTrue(obj.isNotBlank())
    }
    @Test fun redirectsOnlyAllowExactGithubHttpsHosts() {
        assertTrue(allowedUpdateLocation(URI("https://release-assets.githubusercontent.com/a?sig=sample")))
        listOf("http://github.com/a", "https://github.com.evil/a", "https://user@github.com/a", "https://github.com:8443/a", "https://127.0.0.1/a").forEach { assertFalse(allowedUpdateLocation(URI(it))) }
    }
    @Test fun checksumRequiresOneExactInstallerName() {
        val hash = "a".repeat(64)
        assertEquals(hash, installerChecksum("$hash  $name\n", name))
        listOf("$hash  other.exe", "$hash  $name\n$hash  $name", "bad  $name", "$hash  ../$name").forEach { assertFails { installerChecksum(it, name) } }
    }
    @Test fun stagingVerifiesHashCachesValidInstallerAndNeverTouchesProgress() {
        val dir = Files.createTempDirectory("updater-test-")
        try {
            val progress = dir.resolve("progress.json"); Files.writeString(progress, "learner")
            val cache = dir.resolve("updates"); Files.createDirectories(cache)
            Files.writeString(cache.resolve("AtelierApprentissage-1.9.0-Setup.exe"), "old")
            Files.writeString(cache.resolve("AtelierApprentissage-1.10.0-Setup.exe"), "old")
            val fixture = dir.resolve("fixture.exe"); Files.write(fixture, bytes)
            var downloads = 0
            val transport = object : UpdateTransport {
                override fun metadata() = this@WindowsUpdateTest.metadata()
                override fun checksum(url: String) = "${fileSha256(fixture)}  $name"
                override fun download(release: UpdateRelease, target: Path, progress: (Long) -> Unit) { downloads++; Files.write(target, bytes); progress(bytes.size.toLong()) }
            }
            val repo = UpdateRepository(dir.resolve("updates"), transport)
            assertNotNull(repo.prepare(old) { done, total -> assertEquals(done, total) })
            assertNotNull(repo.prepare(old) { _, _ -> })
            assertEquals(1, downloads)
            assertTrue(Files.exists(cache.resolve(name)))
            assertFalse(Files.exists(cache.resolve("AtelierApprentissage-1.9.0-Setup.exe")))
            assertFalse(Files.exists(cache.resolve("AtelierApprentissage-1.10.0-Setup.exe")))
            assertEquals("learner", Files.readString(progress))
        } finally { dir.toFile().deleteRecursively() }
    }

    @Test fun cleanupIsNumericOfflineAndPreservesFutureRetryAndUserFiles() = runBlocking {
        val dir = Files.createTempDirectory("updater-cache-cleanup-")
        try {
            val installed = AppVersion(1, 9, 0)
            val older = dir.resolve("AtelierApprentissage-1.8.9-Setup.exe")
            val equal = dir.resolve("AtelierApprentissage-1.9.0-Setup.exe")
            val future = dir.resolve("AtelierApprentissage-1.10.0-Setup.exe")
            val staleDownload = dir.resolve("download-stale.part")
            val freshApply = dir.resolve("apply-fresh.json")
            val stalePreference = dir.resolve("preferences-stale.tmp")
            val unknown = dir.resolve("notes.txt")
            val progress = dir.resolve("progress.json")
            Files.writeString(dir.resolve("preferences.properties"), "automatic=false\n")
            Files.writeString(dir.resolve("last-result.json"), "keep-result")
            Files.writeString(dir.resolve("updater.log"), "keep-log")
            listOf(older, equal, future, staleDownload, freshApply, stalePreference, unknown, progress).forEach { Files.writeString(it, "preserve") }
            val oldTime = FileTime.from(Instant.now().minus(Duration.ofHours(25)))
            Files.setLastModifiedTime(staleDownload, oldTime)
            Files.setLastModifiedTime(stalePreference, oldTime)
            Files.setLastModifiedTime(freshApply, FileTime.from(Instant.now()))

            // Construction performs the installed-version cleanup, including with auto-updates disabled.
            val noNetwork = object : UpdateTransport {
                override fun metadata(): String? = error("Startup cache cleanup must be offline")
                override fun checksum(url: String): String = error("Startup cache cleanup must be offline")
                override fun download(release: UpdateRelease, target: Path, progress: (Long) -> Unit) = error("Startup cache cleanup must be offline")
            }
            val controller = WindowsUpdateController(dir, dir.resolve("fixture.exe"), installed, UpdateRepository(dir, noNetwork))
            try {
                assertFalse(controller.ui.value.automatic)
                controller.start()
                withTimeout(5000) { while (Files.exists(older) || Files.exists(equal) || Files.exists(staleDownload) || Files.exists(stalePreference)) delay(10) }
            } finally { controller.close() }
            assertFalse(Files.exists(older)); assertFalse(Files.exists(equal))
            assertTrue(Files.exists(future)) // 1.10 is newer than 1.9 numerically and remains retryable.
            assertFalse(Files.exists(staleDownload)); assertFalse(Files.exists(stalePreference))
            assertTrue(Files.exists(freshApply)); assertEquals("preserve", Files.readString(unknown)); assertEquals("preserve", Files.readString(progress))
            assertEquals("keep-result", Files.readString(dir.resolve("last-result.json")))
            assertEquals("keep-log", Files.readString(dir.resolve("updater.log")))

            UpdateCacheCleanup.onStartup(dir, AppVersion(1, 10, 0))
            assertFalse(Files.exists(future))
        } finally { dir.toFile().deleteRecursively() }
    }

    @Test fun cleanupKeepsDirectoriesLinksAndIgnoresDeletionFailures() {
        val dir = Files.createTempDirectory("updater-cache-links-")
        try {
            val nested = Files.createDirectory(dir.resolve("AtelierApprentissage-1.0.0-Setup.exe"))
            val target = Files.writeString(dir.resolve("target.exe"), "target")
            val strictLink = dir.resolve("AtelierApprentissage-1.0.1-Setup.exe")
            val readonly = Files.writeString(dir.resolve("AtelierApprentissage-1.0.2-Setup.exe"), "readonly")
            // A symlink may be unavailable on a locked-down Windows runner; when available it must survive.
            runCatching { Files.createSymbolicLink(strictLink, target.fileName) }
            val readonlySupported = runCatching { Files.setAttribute(readonly, "dos:readonly", true); true }.getOrDefault(false)
            try { UpdateCacheCleanup.onStartup(dir, AppVersion(2, 0, 0)) }
            finally { if (readonlySupported) runCatching { Files.setAttribute(readonly, "dos:readonly", false) } }
            assertTrue(Files.exists(nested, LinkOption.NOFOLLOW_LINKS))
            if (Files.isSymbolicLink(strictLink)) assertTrue(Files.exists(strictLink, LinkOption.NOFOLLOW_LINKS))
            if (readonlySupported) assertTrue(Files.exists(readonly))
            assertTrue(Files.exists(target))
        } finally { dir.toFile().deleteRecursively() }
    }
    @Test fun failedIntegrityAndPartialDownloadsDoNotBecomeInstallable() {
        val dir = Files.createTempDirectory("updater-integrity-")
        try {
            val retryableFuture = dir.resolve("AtelierApprentissage-1.10.0-Setup.exe")
            Files.writeString(retryableFuture, "future")
            val transport = object : UpdateTransport {
                override fun metadata() = this@WindowsUpdateTest.metadata()
                override fun checksum(url: String) = "${"0".repeat(64)}  $name"
                override fun download(release: UpdateRelease, target: Path, progress: (Long) -> Unit) { Files.write(target, bytes) }
            }
            assertFails { UpdateRepository(dir, transport).prepare(old) { _, _ -> } }
            assertFalse(Files.exists(dir.resolve(name)))
            assertTrue(Files.exists(retryableFuture))
            Files.list(dir).use { assertEquals(1, it.count()) }
        } finally { dir.toFile().deleteRecursively() }
    }
    @Test fun portableControllerNeverContactsGithubOrInstalls() = runBlocking {
        val dir = Files.createTempDirectory("updater-portable-")
        val transport = object : UpdateTransport {
            override fun metadata(): String? = error("Must not contact GitHub")
            override fun checksum(url: String): String = error("No download")
            override fun download(release: UpdateRelease, target: Path, progress: (Long) -> Unit) = error("No download")
        }
        val controller = WindowsUpdateController(dir, null, old, UpdateRepository(dir, transport))
        try {
            controller.start(); controller.checkNow()
            assertFalse(controller.ui.value.installed); assertFalse(controller.ui.value.automatic)
            assertTrue(controller.beforeClose())
        } finally { controller.close(); dir.toFile().deleteRecursively() }
    }
    @Test fun automaticSettingPersistsAndDisabledModeRequiresExplicitInstall() = runBlocking {
        val dir = Files.createTempDirectory("updater-controller-")
        val fixture = dir.resolve("fixture.exe"); Files.write(fixture, bytes)
        val transport = object : UpdateTransport {
            override fun metadata() = this@WindowsUpdateTest.metadata()
            override fun checksum(url: String) = "${fileSha256(fixture)}  $name"
            override fun download(release: UpdateRelease, target: Path, progress: (Long) -> Unit) { Files.write(target, bytes) }
        }
        var installs = 0
        val controller = WindowsUpdateController(dir, fixture, old, UpdateRepository(dir, transport)) { _, _, _ -> installs++ }
        try {
            controller.setAutomatic(false)
            controller.checkNow()
            withTimeout(5000) { while (!controller.ui.value.ready) delay(10) }
            assertTrue(controller.beforeClose())
            assertEquals(0, installs)
            assertTrue(controller.beforeClose(restart = true))
            assertEquals(1, installs)
            val restored = WindowsUpdateController(dir, fixture, old)
            try { assertFalse(restored.ui.value.automatic) } finally { restored.close() }
        } finally { controller.close(); dir.toFile().deleteRecursively() }
    }

    @Test fun updaterHelperIsBundledAndWaitsForExitAndRechecksHash() {
        val script = updateHelperScript()
        listOf("Wait-Process", "ComputeHash", "SHA256", "'/quiet','/norestart'", "3010", "apply-", "payloadIsOwned", "ReparsePoint", "finally").forEach { assertTrue(it in script) }
        assertFalse("Invoke-Expression" in script)
    }

    @Test fun realPowerShellFailureRemovesPayloadButKeepsInstallerAndPersonalFiles() {
        if (!System.getProperty("os.name").startsWith("Windows")) return
        val dir = Files.createTempDirectory("updater-helper-fixture-")
        try {
            val installer = Files.write(dir.resolve("AtelierApprentissage-1.10.0-Setup.exe"), bytes)
            val payload = dir.resolve("apply-fixture.json")
            val script = Files.writeString(dir.resolve("apply-update.ps1"), updateHelperScript())
            val result = dir.resolve("last-result.json")
            val preferences = Files.writeString(dir.resolve("preferences.properties"), "automatic=false")
            val log = Files.writeString(dir.resolve("updater.log"), "existing log")
            Files.writeString(payload, kotlinx.serialization.json.buildJsonObject {
                put("installer", installer.toAbsolutePath().toString())
                put("sha256", "0".repeat(64))
                put("executable", dir.resolve("DuolingoKmpApp.exe").toAbsolutePath().toString())
                put("parentPid", 2147483647)
                put("restart", false)
                put("result", result.toAbsolutePath().toString())
            }.toString())
            val process = ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-File", script.toString(), "-Payload", payload.toString())
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start()
            if (!process.waitFor(15, TimeUnit.SECONDS)) { process.destroyForcibly(); fail("Updater fixture timed out") }
            assertEquals(1, process.exitValue())
            assertFalse(Files.exists(payload))
            assertTrue(Files.exists(installer))
            val resultJson = Files.readString(result).trimStart('\uFEFF')
            assertFalse(kotlinx.serialization.json.Json.parseToJsonElement(resultJson).jsonObject["success"]!!.jsonPrimitive.content.toBoolean())
            assertEquals("automatic=false", Files.readString(preferences))
            assertEquals("existing log", Files.readString(log))
        } finally { dir.toFile().deleteRecursively() }
    }
    @Test fun readyUpdateAndDisabledAutomaticControlsAreAccessibleAtTwoWidths() {
        listOf(500, 1100).forEach { width -> runDesktopComposeUiTest(width = width, height = 400) {
            var checked = 0; var applied = 0; var automatic: Boolean? = null
            setContent { LearningAppTheme { Surface { UpdatePanel(UpdateUiState("1.4.0", false, true, "Version 1.5.0 prête", ready = true), { checked++ }, { automatic = it }, { applied++ }) } } }
            onNodeWithText("Vérifier les mises à jour").performClick()
            onNodeWithText("Installer et redémarrer").performClick()
            onNode(isToggleable()).performClick()
            runOnIdle { assertEquals(1, checked); assertEquals(1, applied); assertEquals(true, automatic) }
            val file = File("build/reports/ui-snapshots/updater-$width.png"); file.parentFile.mkdirs()
            Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image -> image.encodeToData()!!.use { file.writeBytes(it.bytes) } }
        } }
    }
}
