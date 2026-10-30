import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import java.awt.Dimension
import com.dreamsigner.wizard.App
import com.dreamsigner.wizard.diagnostics.checkInstallation
import com.dreamsigner.wizard.diagnostics.writeInstallationCheckReport
import com.dreamsigner.wizard.diagnostics.checkDesktopDocumentEngines
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.system.exitProcess
import com.dreamsigner.wizard.ui.math.MathRenderingProvider
import com.dreamsigner.wizard.ui.math.DesktopMathRenderer
import com.dreamsigner.wizard.ui.pdf.PdfReaderProvider
import com.dreamsigner.wizard.ui.pdf.DesktopPdfDocumentReader
import androidx.compose.runtime.*
import com.dreamsigner.wizard.update.*
import kotlinx.coroutines.launch
import com.dreamsigner.wizard.ui.share.LocalChallengeShareService
import com.dreamsigner.wizard.ui.share.DesktopChallengeShareService
import com.dreamsigner.wizard.ui.studio.DesktopStudioController
import com.dreamsigner.wizard.ui.studio.LocalStudioController

fun main(args: Array<String>) {
    if (args.firstOrNull() == "--self-check-updater") {
        // Only disposable Windows CI may exercise a real updater installation.
        require(System.getenv("GITHUB_ACTIONS") == "true" && System.getenv("RUNNER_OS") == "Windows")
        require(args.size == 5 && args[1] == "--installer" && args[3] == "--report-dir")
        val workspace = Path.of(requireNotNull(System.getenv("GITHUB_WORKSPACE"))).toAbsolutePath().normalize()
        val installer = Path.of(args[2]).toAbsolutePath().normalize()
        val reports = Path.of(args[4]).toAbsolutePath().normalize()
        require(installer.startsWith(workspace.resolve("composeApp/build")) && reports.startsWith(workspace.resolve("composeApp/build/reports")))
        java.nio.file.Files.createDirectories(reports)
        val release = UpdateRelease(AppVersion(1, 5, 1), installer.fileName.toString(), "", "", java.nio.file.Files.size(installer))
        launchUpdateHelper(reports, PreparedUpdate(release, installer, fileSha256(installer)), requireNotNull(installedApplicationPath()), ProcessHandle.current().pid(), false)
        return
    }
    if (args.firstOrNull() == "--self-check") {
        val status = try {
            require(args.size == 3 && args[1] == "--report") { "Usage: --self-check --report NEW_REPORT_PATH" }
            runBlocking {
                val report = checkInstallation()
                checkDesktopDocumentEngines()
                // Verify Studio IA's HTTP module in the reduced packaged runtime, without a request.
                java.net.http.HttpClient.newHttpClient()
                writeInstallationCheckReport(Path.of(args[2]), report.copy(mathRenderingVerified = true, pdfRenderingVerified = true,
                    updaterInstalledEdition = installedApplicationPath() != null, updaterVersion = System.getProperty("wizard.version", "")))
            }
            0
        } catch (failure: Exception) {
            System.err.println("Installation check failed: ${failure::class.simpleName}")
            1
        }
        exitProcess(status)
    }
    desktopApplication()
}

private fun desktopApplication() = application {
    val updater = remember {
        WindowsUpdateController(
            Path.of(System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"), "DreamsignWizard", "updates"),
            installedApplicationPath(), AppVersion.parse(System.getProperty("wizard.version", "")),
        )
    }
    val updateState by updater.ui.collectAsState()
    val scope = rememberCoroutineScope()
    var unsavedVideoCloseRequest by remember { mutableStateOf<Boolean?>(null) }
    val closeSafely: (Boolean) -> Unit = { restart -> scope.launch {
        val flushed = com.dreamsigner.wizard.ui.video.flushVideoPlaybackPositions()
        if (flushed.failedPlayers > 0) unsavedVideoCloseRequest = restart
        else if (updater.beforeClose(restart)) {
            com.dreamsigner.wizard.ui.video.closeVideoPlayersBeforeExit()
            exitApplication()
        }
    }; Unit }
    LaunchedEffect(updater) { updater.start() }
    DisposableEffect(updater) { onDispose { updater.close() } }
    Window(
        onCloseRequest = { closeSafely(false) },
        title = "Dreamsign Wizard — Tuteur IA et cours",
        state = rememberWindowState(width = 1280.dp, height = 840.dp)
    ) {
        window.minimumSize = Dimension(360, 480)
        unsavedVideoCloseRequest?.let { restart ->
            androidx.compose.material.AlertDialog(
                onDismissRequest = { unsavedVideoCloseRequest = null },
                title = { androidx.compose.material.Text("Position vidéo non enregistrée") },
                text = { androidx.compose.material.Text("Impossible d’enregistrer une ou plusieurs positions vidéo. Les positions précédentes sont conservées.") },
                confirmButton = { androidx.compose.material.TextButton(onClick = {
                    unsavedVideoCloseRequest = null
                    scope.launch { if (updater.beforeClose(restart)) {
                        com.dreamsigner.wizard.ui.video.closeVideoPlayersBeforeExit()
                        exitApplication()
                    } }
                }) { androidx.compose.material.Text("Quitter quand même") } },
                dismissButton = { androidx.compose.material.TextButton(onClick = { unsavedVideoCloseRequest = null }) {
                    androidx.compose.material.Text("Rester dans l’application")
                } },
            )
        }
        PdfReaderProvider(
            androidx.compose.runtime.remember { DesktopPdfDocumentReader() },
            scrollbar = { state, modifier -> androidx.compose.foundation.VerticalScrollbar(
                adapter = androidx.compose.foundation.rememberScrollbarAdapter(state), modifier = modifier,
            ) },
        ) {
            com.dreamsigner.wizard.ui.VideoThumbnailProvider(androidx.compose.runtime.remember { com.dreamsigner.wizard.ui.DesktopVideoThumbnailLoader() }) {
                com.dreamsigner.wizard.ui.VideoPlaybackProvider(com.dreamsigner.wizard.ui.video.DesktopVideoPlayer) {
                    com.dreamsigner.wizard.ui.VideoDownloadProvider(remember { com.dreamsigner.wizard.ui.download.defaultFrenchVideoDownloader() }) {
                        MathRenderingProvider(DesktopMathRenderer) {
                            CompositionLocalProvider(LocalChallengeShareService provides remember { DesktopChallengeShareService() }) {
                                val studio = remember { DesktopStudioController() }
                                DisposableEffect(studio) { onDispose { studio.close() } }
                                CompositionLocalProvider(LocalStudioController provides studio) {
                                    App(settingsActionCount = updateState.actionCount) {
                                        UpdatePanel(updateState, updater::checkNow, updater::setAutomatic, { closeSafely(true) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
