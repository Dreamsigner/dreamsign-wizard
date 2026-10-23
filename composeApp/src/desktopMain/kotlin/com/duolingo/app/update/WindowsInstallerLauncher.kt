package com.duolingo.app.update

import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path

internal fun updateHelperScript(): String = requireNotNull(WindowsUpdateController::class.java.getResourceAsStream("/updater/apply-update.ps1")).bufferedReader().use { it.readText() }

/** Static helper code; paths and installer metadata never become executable expressions. */
internal fun launchUpdateHelper(folder: Path, update: PreparedUpdate, executable: Path, parentPid: Long, restart: Boolean) {
    require(fileSha256(update.installer) == update.sha256)
    val payload = Files.createTempFile(folder, "apply-", ".json")
    try {
        Files.writeString(payload, buildJsonObject {
            put("installer", update.installer.toAbsolutePath().toString()); put("sha256", update.sha256)
            put("executable", executable.toAbsolutePath().toString()); put("parentPid", parentPid); put("restart", restart)
            put("result", folder.resolve("last-result.json").toAbsolutePath().toString())
        }.toString())
        val command = "& {\n" + updateHelperScript() + "\n} -Payload ${'$'}env:ATELIER_UPDATE_PAYLOAD"
        val shell = Path.of(System.getenv("SystemRoot"), "System32", "WindowsPowerShell", "v1.0", "powershell.exe")
        val builder = ProcessBuilder(shell.toString(), "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-Command", command)
            .redirectOutput(folder.resolve("updater.log").toFile()).redirectErrorStream(true)
        builder.environment()["ATELIER_UPDATE_PAYLOAD"] = payload.toAbsolutePath().toString()
        builder.start()
    } catch (failure: Exception) {
        runCatching { Files.deleteIfExists(payload) }
        throw failure
    }
}
