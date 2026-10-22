package com.duolingo.app.update

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Properties

internal data class UpdateUiState(val version: String, val automatic: Boolean, val installed: Boolean,
    val message: String = "", val busy: Boolean = false, val ready: Boolean = false)

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
    private val state = MutableStateFlow(UpdateUiState(version?.toString() ?: "développement", installed && readAutomatic(), installed,
        if (installed) readLastFailure() else "Mise à jour automatique : installation Windows standard requise"))
    val ui = state.asStateFlow()
    private fun readLastFailure(): String = try {
        val result = folder.resolve("last-result.json")
        if (Files.exists(result) && Files.size(result) <= 4096 && "false" in Files.readString(result))
            "La dernière installation a échoué. Réessaie ou utilise l’installateur GitHub." else ""
    } catch (_: Exception) { "" }
    private fun readAutomatic(): Boolean = try {
        !Files.exists(preference) || Properties().apply { Files.newInputStream(preference).use(::load) }.getProperty("automatic") == "true"
    } catch (_: Exception) { false }
    fun start() { if (installed) scope.launch { delay(5000); while (isActive) { if (state.value.automatic) check(); delay(6 * 60 * 60 * 1000L) } } }
    private suspend fun saveAutomatic() = preferenceMutex.withLock {
        try {
            Files.createDirectories(folder)
            val temp = Files.createTempFile(folder, "preferences-", ".tmp")
            try {
                Files.newOutputStream(temp).use { Properties().apply { setProperty("automatic", state.value.automatic.toString()) }.store(it, null) }
                Files.move(temp, preference, StandardCopyOption.REPLACE_EXISTING)
            } finally { Files.deleteIfExists(temp) }
        } catch (_: Exception) { state.value = state.value.copy(automatic = false, message = "Impossible d’enregistrer le réglage.") }
    }
    fun setAutomatic(enabled: Boolean) {
        if (!installed) return
        state.value = state.value.copy(automatic = enabled)
        scope.launch { saveAutomatic() }
        if (enabled) checkNow()
    }
    fun checkNow() { if (installed && !state.value.busy) scope.launch { check() } }
    private suspend fun check() = mutex.withLock {
        state.value = state.value.copy(busy = true, message = "Recherche de mise à jour…")
        try {
            pending = repository.prepare(requireNotNull(version)) { done, total ->
                state.value = state.value.copy(message = "Téléchargement : ${done * 100 / total} %")
            }
            state.value = state.value.copy(busy = false, ready = pending != null,
                message = pending?.let { "Version ${it.release.version} prête · installation à la fermeture si Auto est activé" } ?: "Application à jour")
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { pending = null; state.value = state.value.copy(busy = false, ready = false, message = "Mise à jour indisponible. Réessaie plus tard; tes cours restent accessibles.") }
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
                    state.value = state.value.copy(message = "Impossible de préparer l’installation. Ferme à nouveau pour quitter sans mise à jour.", ready = false)
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
