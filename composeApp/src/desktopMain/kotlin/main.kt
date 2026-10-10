import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.duolingo.app.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Duolingo Multiplatform - Maths, Algo C++ & TCF Canada"
    ) {
        App()
    }
}
