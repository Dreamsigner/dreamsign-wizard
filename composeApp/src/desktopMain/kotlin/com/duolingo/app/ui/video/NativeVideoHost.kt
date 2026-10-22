package com.duolingo.app.ui.video

import ca.weblite.webview.swing.WebViewComponent
import com.duolingo.app.data.CourseResource
import com.duolingo.app.ui.youtubeVideoId
import kotlinx.serialization.json.*
import java.awt.BorderLayout
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.*

internal fun playerAddress(resource: CourseResource): String {
    require(resource.kind == "video")
    youtubeVideoId(resource.url)?.let { return resource.url }
    val uri = URI(resource.url)
    val host = uri.host.orEmpty().lowercase()
    require(uri.scheme == "https" && uri.userInfo == null && uri.port == -1 && uri.path.endsWith(".mp4", true))
    require(host == "archive.org" || host.endsWith(".archive.org") || host == "ocw.mit.edu" || host.endsWith(".mit.edu"))
    return resource.url
}

internal data class VideoPageState(val url: String, val ready: Boolean, val hasVideo: Boolean, val mediaReady: Int, val fullscreen: Boolean, val mediaTime: Double = 0.0, val duration: Double = 0.0, val controls: Boolean = false)
internal fun parseVideoPageState(raw: String): VideoPageState {
    require(raw.length <= 16384)
    val outer = Json.parseToJsonElement(raw)
    val obj = (if (outer is JsonPrimitive && outer.isString) Json.parseToJsonElement(outer.content) else outer).jsonObject
    return VideoPageState(obj["url"]!!.jsonPrimitive.content, obj["ready"]!!.jsonPrimitive.boolean,
        obj["video"]!!.jsonPrimitive.boolean, obj["mediaReady"]!!.jsonPrimitive.int, obj["fullscreen"]!!.jsonPrimitive.boolean,
        obj["mediaTime"]?.jsonPrimitive?.doubleOrNull ?: 0.0, obj["duration"]?.jsonPrimitive?.doubleOrNull ?: 0.0, obj["controls"]?.jsonPrimitive?.booleanOrNull ?: false)
}
internal const val VIDEO_PAGE_PROBE = """JSON.stringify((function(){var v=document.querySelector('video');return {url:location.href,ready:document.readyState==='complete',video:!!v,mediaReady:v?v.readyState:0,fullscreen:!!document.fullscreenElement,mediaTime:v?v.currentTime:0,duration:v&&isFinite(v.duration)?v.duration:0,controls:v?!!v.controls:false};})())"""

internal interface NativeVideoAdapter {
    val component: JComponent
    fun navigate(url: String)
    fun evaluate(script: String): CompletableFuture<String>
    fun dispose()
}
private class WebViewAdapter : NativeVideoAdapter {
    private val native = WebViewComponent.create(WebViewComponent.Mode.HEAVYWEIGHT).apply { setDefaultContextMenuEnabled(true) }
    override val component: JComponent get() = native
    override fun navigate(url: String) { native.setUrl(url) }
    override fun evaluate(script: String): CompletableFuture<String> = native.evalAsync("return ($script);")
    override fun dispose() { native.dispose() }
}

/** One native WebView per window, released when its owner closes. No app bridge is exposed to pages. */
internal class NativeVideoHost(private val onState: (VideoPageState) -> Unit, private val onFailure: (String) -> Unit, private val factory: () -> NativeVideoAdapter = { WebViewAdapter() }) {
    var view: NativeVideoAdapter? = null
        private set
    private var timer: Timer? = null
    private val closed = AtomicBoolean(false)
    private val probing = AtomicBoolean(false)
    fun create(url: String): JPanel = JPanel(BorderLayout()).apply {
        try {
            check(System.getProperty("os.name").startsWith("Windows"))
            val native = factory()
            view = native
            native.navigate(url)
            add(native.component, BorderLayout.CENTER)
            var attempts = 0
            timer = Timer(500) {
                if (!closed.get() && probing.compareAndSet(false, true)) {
                    val query = try { native.evaluate(VIDEO_PAGE_PROBE) } catch (_: Exception) {
                        probing.set(false)
                        if (++attempts == 20) onFailure("Le lecteur natif ne répond pas. Ouvre la source dans le navigateur.")
                        return@Timer
                    }
                    query.orTimeout(2, TimeUnit.SECONDS).whenComplete { raw, error ->
                        SwingUtilities.invokeLater {
                            probing.set(false)
                            if (!closed.get()) {
                                if (error == null) runCatching { parseVideoPageState(raw) }.onSuccess { state ->
                                    attempts = 0
                                    if (state.url.startsWith("chrome-error:")) onFailure("Le lecteur n’a pas pu charger cette vidéo. Vérifie la connexion ou ouvre la source dans le navigateur.")
                                    else onState(state)
                                }.onFailure {
                                    if (++attempts == 20) onFailure("Le lecteur ne fournit pas son état. Ouvre la source dans le navigateur.")
                                }
                                else if (++attempts == 20) onFailure("Le lecteur reste indisponible. Vérifie la présence du runtime Microsoft Edge WebView2 et la connexion.")
                            }
                        }
                    }
                }
            }.also { it.start() }
        } catch (_: LinkageError) { onFailure("Le lecteur natif ne peut pas démarrer. Le runtime Microsoft Edge WebView2 est requis.") }
        catch (_: Exception) { onFailure("Le lecteur n’est pas disponible. Tu peux ouvrir la vidéo dans le navigateur.") }
    }
    fun close() {
        if (closed.compareAndSet(false, true)) SwingUtilities.invokeLater {
            timer?.stop(); timer = null
            view?.dispose(); view = null
        }
    }
}
