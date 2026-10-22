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
import java.nio.file.Path
import kotlin.test.*

class WindowsUpdateTest {
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
            assertEquals("learner", Files.readString(progress))
        } finally { dir.toFile().deleteRecursively() }
    }
    @Test fun failedIntegrityAndPartialDownloadsDoNotBecomeInstallable() {
        val dir = Files.createTempDirectory("updater-integrity-")
        try {
            val transport = object : UpdateTransport {
                override fun metadata() = this@WindowsUpdateTest.metadata()
                override fun checksum(url: String) = "${"0".repeat(64)}  $name"
                override fun download(release: UpdateRelease, target: Path, progress: (Long) -> Unit) { Files.write(target, bytes) }
            }
            assertFails { UpdateRepository(dir, transport).prepare(old) { _, _ -> } }
            assertFalse(Files.exists(dir.resolve(name)))
            Files.list(dir).use { assertEquals(0, it.count()) }
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
        listOf("Wait-Process", "Get-FileHash", "SHA256", "'/quiet','/norestart'", "3010").forEach { assertTrue(it in script) }
        assertFalse("Invoke-Expression" in script)
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
