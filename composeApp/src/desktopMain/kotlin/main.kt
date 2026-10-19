import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import java.awt.Dimension
import com.duolingo.app.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Atelier d’apprentissage — Maths, Informatique et TCF",
        state = rememberWindowState(width = 1280.dp, height = 840.dp)
    ) {
        window.minimumSize = Dimension(360, 480)
        App()
    }
}
