package com.duolingo.app.ui.video

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.duolingo.app.LearningAppTheme
import com.duolingo.app.data.CourseResource
import com.duolingo.app.ui.*

internal data class FullscreenSession(
    val placement: WindowPlacement = WindowPlacement.Floating,
    val previousPlacement: WindowPlacement = WindowPlacement.Floating,
    val sourceFullscreen: Boolean = false,
) {
    fun onSourceFullscreen(active: Boolean): FullscreenSession = when {
        active && !sourceFullscreen -> copy(
            placement = WindowPlacement.Fullscreen,
            previousPlacement = if (placement == WindowPlacement.Fullscreen) previousPlacement else placement,
            sourceFullscreen = true,
        )
        !active && sourceFullscreen -> copy(placement = previousPlacement, sourceFullscreen = false)
        else -> this
    }

    fun enterWindowFullscreen(): FullscreenSession = if (placement == WindowPlacement.Fullscreen) this else
        copy(placement = WindowPlacement.Fullscreen, previousPlacement = placement)

    fun exitWindowFullscreen(): FullscreenSession = if (placement != WindowPlacement.Fullscreen) this else copy(placement = previousPlacement)

    /** F11/Escape leave source-controlled DOM fullscreen through its real fullscreenchange event. */
    fun onShortcut(): FullscreenSession = if (placement == WindowPlacement.Fullscreen && !sourceFullscreen) exitWindowFullscreen()
        else if (placement != WindowPlacement.Fullscreen) enterWindowFullscreen() else this

    fun synchronizeWindowPlacement(current: WindowPlacement): FullscreenSession = if (sourceFullscreen || current == WindowPlacement.Fullscreen) this
        else copy(placement = current, previousPlacement = current)
}

object DesktopVideoPlayer : VideoPlayer {
    @Composable override fun Player(resource: CourseResource, onClose: () -> Unit) {
        val uri = LocalUriHandler.current
        val supported = remember(resource.url) { runCatching { playerAddress(resource); true }.getOrDefault(false) }
        val state = rememberWindowState(width = 1100.dp, height = 760.dp)
        var message by remember(resource.url) { mutableStateOf("Chargement du lecteur source…") }
        var failure by remember(resource.url) { mutableStateOf(false) }
        var fullscreenSession by remember(resource.url) { mutableStateOf(FullscreenSession()) }
        lateinit var host: NativeVideoHost
        host = remember(resource.url) { NativeVideoHost(onState = { page ->
            fullscreenSession = fullscreenSession.synchronizeWindowPlacement(state.placement)
            message = when {
                page.youtubeError != 0 -> "YouTube ne peut pas lire cette vidéo dans l’application (erreur ${page.youtubeError}). Ouvre la source dans le navigateur."
                page.youtubeReady && page.youtubeState == 1 -> "Lecture YouTube en cours. Les commandes officielles restent disponibles."
                resource.url.contains("youtube.com/watch") && !page.youtubeReady -> "Le lecteur YouTube se charge. Vérifie la connexion ou ouvre la source dans le navigateur."
                resource.url.contains("youtube.com/watch") && page.youtubeReady -> "Le lecteur YouTube est prêt. Les commandes officielles restent disponibles."
                page.ready && page.hasVideo && page.mediaReady >= 1 -> "Utilise les commandes du lecteur source. Les fonctions dépendent de la vidéo et du réseau."
                page.ready -> "Le lecteur source est prêt."
                else -> "Le lecteur source se charge."
            }
            if (page.keyAction == "F11") {
                if (fullscreenSession.sourceFullscreen) host.view?.evaluate("(function(){if(document.fullscreenElement)document.exitFullscreen();return true;})()")
                else fullscreenSession = fullscreenSession.onShortcut()
            } else if (page.keyAction == "Escape" && state.placement == WindowPlacement.Fullscreen && !page.fullscreen) {
                fullscreenSession = fullscreenSession.exitWindowFullscreen()
            }
            fullscreenSession = fullscreenSession.onSourceFullscreen(page.fullscreen)
            state.placement = fullscreenSession.placement
        }, onFailure = { message = it; failure = true }) }
        DisposableEffect(host) { onDispose { host.close() } }
        Window(onCloseRequest = onClose, title = "Vidéo · ${resource.title.substringBeforeLast(" — ", resource.title)}", state = state) {
            val isFullscreen = state.placement == WindowPlacement.Fullscreen
            fun leaveFullscreen() {
                if (fullscreenSession.sourceFullscreen) host.view?.evaluate("(function(){if(document.fullscreenElement)document.exitFullscreen();return true;})()")
                else { fullscreenSession = fullscreenSession.exitWindowFullscreen(); state.placement = fullscreenSession.placement }
            }
            fun enterFullscreen() {
                fullscreenSession = fullscreenSession.enterWindowFullscreen()
                state.placement = fullscreenSession.placement
                host.view?.evaluate("(function(){var e=document.querySelector('#player');if(e&&e.requestFullscreen)e.requestFullscreen().catch(function(){});return true;})()")
            }
            Column(Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.F11) {
                    if (isFullscreen) leaveFullscreen() else enterFullscreen()
                    true
                } else if (event.type == KeyEventType.KeyDown && event.key == Key.Escape && isFullscreen) {
                    leaveFullscreen(); true
                } else false
            }) {
                VideoPlayerLayout(
                    title = resource.title.substringBeforeLast(" — ", resource.title),
                    message = message,
                    fullscreen = isFullscreen,
                    onFullscreen = { if (isFullscreen) leaveFullscreen() else enterFullscreen() },
                    onBrowser = { runCatching { uri.openUri(resource.url) } },
                    onClose = onClose,
                    content = {
                        if (supported) SwingPanel(factory = { host.create(resource) }, modifier = Modifier.fillMaxSize())
                        else Text("Cette source vidéo n’est pas prise en charge dans le lecteur intégré.", Modifier.padding(24.dp), color = ActionRed)
                    }
                )
                if (failure && !isFullscreen) TextButton({ runCatching { uri.openUri("https://developer.microsoft.com/microsoft-edge/webview2/") } }, Modifier.heightIn(min = 48.dp)) { Text("Informations sur WebView2") }
            }
        }
    }
}

@Composable
internal fun VideoPlayerLayout(title: String, message: String, fullscreen: Boolean,
    onFullscreen: () -> Unit, onBrowser: () -> Unit, onClose: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        if (!fullscreen) VideoPlayerHeader(title, message, fullscreen, onFullscreen, onBrowser, onClose)
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
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
