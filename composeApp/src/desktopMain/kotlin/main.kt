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

fun main(args: Array<String>) {
    if (args.firstOrNull() == "--self-check") {
        val status = try {
            require(args.size == 3 && args[1] == "--report") { "Usage: --self-check --report NEW_REPORT_PATH" }
            runBlocking {
                val report = checkInstallation()
                checkDesktopDocumentEngines()
                writeInstallationCheckReport(Path.of(args[2]), report.copy(mathRenderingVerified = true, pdfRenderingVerified = true))
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
    Window(
        onCloseRequest = ::exitApplication,
        title = "Atelier d’apprentissage — Maths, Informatique et TCF",
        state = rememberWindowState(width = 1280.dp, height = 840.dp)
    ) {
        window.minimumSize = Dimension(360, 480)
        PdfReaderProvider(
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
