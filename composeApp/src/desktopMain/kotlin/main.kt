import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import java.awt.Dimension
import com.duolingo.app.App
import com.duolingo.app.diagnostics.checkInstallation
import com.duolingo.app.diagnostics.writeInstallationCheckReport
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.firstOrNull() == "--self-check") {
        val status = try {
            require(args.size == 3 && args[1] == "--report") { "Usage: --self-check --report NEW_REPORT_PATH" }
            runBlocking { writeInstallationCheckReport(Path.of(args[2]), checkInstallation()) }
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
        App()
    }
}
