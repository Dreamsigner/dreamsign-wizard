import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import java.awt.Dimension
import com.duolingo.app.App
import com.duolingo.app.diagnostics.checkInstallation
import com.duolingo.app.diagnostics.writeInstallationCheckReport
import com.duolingo.app.diagnostics.checkDesktopDocumentEngines
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.system.exitProcess
import com.duolingo.app.ui.math.MathRenderingProvider
import com.duolingo.app.ui.math.DesktopMathRenderer
import com.duolingo.app.ui.pdf.PdfReaderProvider
import com.duolingo.app.ui.pdf.DesktopPdfDocumentReader
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import com.duolingo.app.update.*
import com.duolingo.app.LearningAppTheme
import kotlinx.coroutines.launch

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
        val release = UpdateRelease(AppVersion(1, 4, 1), installer.fileName.toString(), "", "", java.nio.file.Files.size(installer))
        launchUpdateHelper(reports, PreparedUpdate(release, installer, fileSha256(installer)), requireNotNull(installedApplicationPath()), ProcessHandle.current().pid(), false)
        return
    }
    if (args.firstOrNull() == "--self-check") {
        val status = try {
            require(args.size == 3 && args[1] == "--report") { "Usage: --self-check --report NEW_REPORT_PATH" }
            runBlocking {
                val report = checkInstallation()
                checkDesktopDocumentEngines()
                writeInstallationCheckReport(Path.of(args[2]), report.copy(mathRenderingVerified = true, pdfRenderingVerified = true,
                    updaterInstalledEdition = installedApplicationPath() != null, updaterVersion = System.getProperty("atelier.version", "")))
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
            Path.of(System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"), "AtelierApprentissage", "updates"),
            installedApplicationPath(), AppVersion.parse(System.getProperty("atelier.version", "")),
        )
    }
    val updateState by updater.ui.collectAsState()
    val scope = rememberCoroutineScope()
    val closeSafely: (Boolean) -> Unit = { restart -> scope.launch { if (updater.beforeClose(restart)) exitApplication() }; Unit }
    LaunchedEffect(updater) { updater.start() }
    DisposableEffect(updater) { onDispose { updater.close() } }
    Window(
        onCloseRequest = { closeSafely(false) },
        title = "Atelier d’apprentissage — Maths, Informatique et TCF",
        state = rememberWindowState(width = 1280.dp, height = 840.dp)
    ) {
        window.minimumSize = Dimension(360, 480)
        Column {
        LearningAppTheme { UpdatePanel(updateState, updater::checkNow, updater::setAutomatic, { closeSafely(true) }) }
        Box(Modifier.weight(1f)) { PdfReaderProvider(
            androidx.compose.runtime.remember { DesktopPdfDocumentReader() },
            scrollbar = { state, modifier -> androidx.compose.foundation.VerticalScrollbar(
                adapter = androidx.compose.foundation.rememberScrollbarAdapter(state), modifier = modifier,
            ) },
        ) {
            com.duolingo.app.ui.VideoThumbnailProvider(androidx.compose.runtime.remember { com.duolingo.app.ui.DesktopVideoThumbnailLoader() }) {
                MathRenderingProvider(DesktopMathRenderer) { App() }
            }
        }
        }
        }
    }
}
