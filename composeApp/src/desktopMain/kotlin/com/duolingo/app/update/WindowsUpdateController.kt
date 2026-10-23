package com.duolingo.app.update

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Properties

internal data class UpdateUiState(val version: String, val automatic: Boolean, val installed: Boolean,
    val statusMessage: String = "", val busy: Boolean = false, val ready: Boolean = false,
    val preferenceError: String? = null, val checkError: String? = null, val installationError: String? = null) {
    val requiresAttention: Boolean get() = preferenceError != null || checkError != null || installationError != null
    val actionCount: Int get() = (if (ready) 1 else 0) + listOf(preferenceError, checkError, installationError).count { it != null }
    val message: String get() {
        val errors = listOfNotNull(preferenceError, checkError, installationError)
        val status = statusMessage.takeIf { it.isNotBlank() && (errors.isEmpty() || it != "Application à jour") }
        return (errors + listOfNotNull(status)).joinToString("\n")
    }
}

internal class WindowsUpdateController(
    private val folder: Path,
    private val executable: Path?,
    private val version: AppVersion?,
    private val repository: UpdateRepository = UpdateRepository(folder, GithubUpdateTransport()),
    private val launchInstaller: (PreparedUpdate, Path, Boolean) -> Unit = { update, app, restart -> launchUpdateHelper(folder, update, app, ProcessHandle.current().pid(), restart) },
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val preferenceMutex = Mutex()
    private val preference = folder.resolve("preferences.properties")
    private var pending: PreparedUpdate? = null
    private val installed = executable != null && version != null
    private val lastFailure = if (installed) readLastFailure() else null
    private val state = MutableStateFlow(UpdateUiState(version?.toString() ?: "développement", installed && readAutomatic(), installed,
        if (installed) "" else "Mise à jour automatique : installation Windows standard requise",
        installationError = lastFailure))
    val ui = state.asStateFlow()
    private fun readLastFailure(): String? = try {
        val result = folder.resolve("last-result.json")
        if (Files.exists(result) && Files.size(result) <= 4096 && Regex("\"success\"\\s*:\\s*false").containsMatchIn(Files.readString(result)))
            "La dernière installation a échoué. Réessaie ou utilise l’installateur GitHub." else null
    } catch (_: Exception) { null }
    private fun readAutomatic(): Boolean = try {
        !Files.exists(preference) || Properties().apply { Files.newInputStream(preference).use(::load) }.getProperty("automatic") == "true"
    } catch (_: Exception) { false }
    fun start() { if (installed) scope.launch {
        UpdateCacheCleanup.onStartup(folder, requireNotNull(version))
        delay(5000)
        while (isActive) { if (state.value.automatic) check(); delay(6 * 60 * 60 * 1000L) }
    } }
    private suspend fun saveAutomatic() = preferenceMutex.withLock {
        try {
            Files.createDirectories(folder)
            val temp = Files.createTempFile(folder, "preferences-", ".tmp")
            try {
                Files.newOutputStream(temp).use { Properties().apply { setProperty("automatic", state.value.automatic.toString()) }.store(it, null) }
                Files.move(temp, preference, StandardCopyOption.REPLACE_EXISTING)
            } finally { Files.deleteIfExists(temp) }
            state.update { current -> current.copy(preferenceError = null) }
        } catch (_: Exception) { state.update { current -> current.copy(automatic = false, preferenceError = "Impossible d’enregistrer le réglage.") } }
    }
    fun setAutomatic(enabled: Boolean) {
        if (!installed) return
        state.update { current -> current.copy(automatic = enabled) }
        scope.launch { saveAutomatic() }
        if (enabled) checkNow()
    }
    fun checkNow() { if (installed && !state.value.busy) scope.launch { check() } }
    private suspend fun check() = mutex.withLock {
        state.update { current -> current.copy(busy = true, statusMessage = "Recherche de mise à jour…") }
        try {
            pending = repository.prepare(requireNotNull(version)) { done, total ->
                state.update { current -> current.copy(statusMessage = "Téléchargement : ${done * 100 / total} %") }
            }
            state.update { current -> current.copy(busy = false, ready = pending != null, checkError = null,
                statusMessage = pending?.let { "Version ${it.release.version} prête · installation à la fermeture si Auto est activé" } ?: "Application à jour") }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { pending = null; state.update { current -> current.copy(busy = false, ready = false, checkError = "Mise à jour indisponible. Réessaie plus tard; tes cours restent accessibles.", statusMessage = "") } }
    }
    suspend fun beforeClose(restart: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        if (installed) saveAutomatic()
        // Do not wait for a download when the learner closes the app.
        if (!mutex.tryLock()) return@withContext true
        try {
            val update = pending
            if (update != null && (state.value.automatic || restart)) {
                try { launchInstaller(update, requireNotNull(executable), restart) }
                catch (_: Exception) {
                    state.update { current -> current.copy(installationError = "Impossible de préparer l’installation. Ferme à nouveau pour quitter sans mise à jour.", statusMessage = "", ready = false) }
                    pending = null; return@withContext false
                }
            }
            true
        } finally { mutex.unlock() }
    }
    fun close() { scope.cancel() }
}

internal fun installedApplicationPath(): Path? {
    if (!System.getProperty("os.name").startsWith("Windows")) return null
    val local = System.getenv("LOCALAPPDATA") ?: return null
    val actual = System.getProperty("jpackage.app-path") ?: return null
    val expected = Path.of(local, "DuolingoKmpApp", "DuolingoKmpApp.exe").toAbsolutePath().normalize()
    return Path.of(actual).toAbsolutePath().normalize().takeIf { it == expected && Files.isRegularFile(it) }
}
