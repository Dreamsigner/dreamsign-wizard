package com.duolingo.app.ui.video

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.duolingo.app.LearningAppTheme
import com.duolingo.app.data.CourseResource
import com.duolingo.app.ui.*

object DesktopVideoPlayer : VideoPlayer {
    @Composable override fun Player(resource: CourseResource, onClose: () -> Unit) {
        val uri = LocalUriHandler.current
        val address = remember(resource.url) { runCatching { playerAddress(resource) }.getOrNull() }
        val state = rememberWindowState(width = 1100.dp, height = 760.dp)
        var message by remember(resource.url) { mutableStateOf("Chargement du lecteur source…") }
        var failure by remember(resource.url) { mutableStateOf(false) }
        var sourceFullscreen by remember { mutableStateOf(false) }
        var priorPlacement by remember { mutableStateOf(WindowPlacement.Floating) }
        val host = remember(resource.url) { NativeVideoHost(onState = { page ->
            if (page.ready) message = if (page.hasVideo && page.mediaReady >= 1) "Utilise les commandes du lecteur source : son, vitesse, sous-titres et qualité selon la vidéo."
                else "Le lecteur source se charge. Effectue les choix de confidentialité demandés par le site si nécessaire."
            if (page.fullscreen && !sourceFullscreen) {
                priorPlacement = state.placement; sourceFullscreen = true; state.placement = WindowPlacement.Fullscreen
            } else if (!page.fullscreen && sourceFullscreen) {
                sourceFullscreen = false; state.placement = priorPlacement
            }
        }, onFailure = { message = it; failure = true }) }
        DisposableEffect(host) { onDispose { host.close() } }
        Window(onCloseRequest = onClose, title = "Vidéo · ${resource.title.substringBeforeLast(" — ", resource.title)}", state = state) {
            LearningAppTheme {
                Column(Modifier.fillMaxSize()) {
                    VideoPlayerHeader(resource.title.substringBeforeLast(" — ", resource.title), message,
                        fullscreen = state.placement == WindowPlacement.Fullscreen,
                        onFullscreen = { state.placement = if (state.placement == WindowPlacement.Fullscreen) WindowPlacement.Floating else WindowPlacement.Fullscreen },
                        onBrowser = { runCatching { uri.openUri(resource.url) } }, onClose = onClose)
                    if (address != null) key(address) {
                        SwingPanel(factory = { host.create(address) }, modifier = Modifier.weight(1f).fillMaxWidth())
                    }
                    else Text("Cette source vidéo n’est pas prise en charge dans le lecteur intégré.", Modifier.padding(24.dp), color = ActionRed)
                    if (failure) TextButton({ runCatching { uri.openUri("https://developer.microsoft.com/microsoft-edge/webview2/") } }, Modifier.heightIn(min = 48.dp)) { Text("Informations sur WebView2") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VideoPlayerHeader(title: String, message: String, fullscreen: Boolean,
    onFullscreen: () -> Unit, onBrowser: () -> Unit, onClose: () -> Unit) {
    Surface(color = White, elevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.h6)
            Text(message, color = Graphite)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onFullscreen, Modifier.heightIn(min = 48.dp)) { Text(if (fullscreen) "Quitter le plein écran" else "Plein écran") }
                TextButton(onBrowser, Modifier.heightIn(min = 48.dp)) { Text("Ouvrir dans le navigateur") }
                TextButton(onClose, Modifier.heightIn(min = 48.dp)) { Text("Fermer la vidéo") }
            }
        }
    }
}
