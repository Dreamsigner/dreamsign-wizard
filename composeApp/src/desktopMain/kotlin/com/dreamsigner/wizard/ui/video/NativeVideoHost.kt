package com.dreamsigner.wizard.ui.video

import ca.weblite.webview.swing.WebViewComponent
import com.dreamsigner.wizard.data.CourseResource
import com.dreamsigner.wizard.ui.youtubeVideoId
import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.json.*
import java.awt.BorderLayout
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.*

internal fun playerAddress(resource: CourseResource): String {
    require(resource.kind == "video")
    youtubeVideoId(resource.url)?.let { return "https://www.youtube.com/embed/$it?controls=1&fs=1" }
    val uri = URI(resource.url)
    val host = uri.host.orEmpty().lowercase()
    require(uri.scheme == "https" && uri.userInfo == null && uri.port == -1 && uri.path.endsWith(".mp4", true))
    require(host == "archive.org" || host.endsWith(".archive.org") || host == "ocw.mit.edu" || host.endsWith(".mit.edu"))
    return resource.url
}

internal data class VideoPageState(val url: String, val ready: Boolean, val hasVideo: Boolean, val mediaReady: Int, val fullscreen: Boolean, val mediaTime: Double = 0.0, val duration: Double = 0.0, val controls: Boolean = false,
    val youtubeReady: Boolean = false, val youtubeState: Int = -1, val youtubeError: Int = 0, val elementWidth: Int = 0, val elementHeight: Int = 0, val videoWidth: Int = 0, val videoHeight: Int = 0, val viewportWidth: Int = 0, val viewportHeight: Int = 0, val keyAction: String = "", val ended: Boolean = false)
internal fun parseVideoPageState(raw: String): VideoPageState {
    require(raw.length <= 16384)
    val outer = Json.parseToJsonElement(raw)
    val obj = (if (outer is JsonPrimitive && outer.isString) Json.parseToJsonElement(outer.content) else outer).jsonObject
    return VideoPageState(obj["url"]!!.jsonPrimitive.content, obj["ready"]!!.jsonPrimitive.boolean,
        obj["video"]!!.jsonPrimitive.boolean, obj["mediaReady"]!!.jsonPrimitive.int, obj["fullscreen"]!!.jsonPrimitive.boolean,
        obj["mediaTime"]?.jsonPrimitive?.doubleOrNull ?: 0.0, obj["duration"]?.jsonPrimitive?.doubleOrNull ?: 0.0, obj["controls"]?.jsonPrimitive?.booleanOrNull ?: false,
        obj["youtubeReady"]?.jsonPrimitive?.booleanOrNull ?: false, obj["youtubeState"]?.jsonPrimitive?.intOrNull ?: -1,
        obj["youtubeError"]?.jsonPrimitive?.intOrNull ?: 0, obj["elementWidth"]?.jsonPrimitive?.intOrNull ?: 0,
        obj["elementHeight"]?.jsonPrimitive?.intOrNull ?: 0, obj["videoWidth"]?.jsonPrimitive?.intOrNull ?: 0,
        obj["videoHeight"]?.jsonPrimitive?.intOrNull ?: 0, obj["viewportWidth"]?.jsonPrimitive?.intOrNull ?: 0,
        obj["viewportHeight"]?.jsonPrimitive?.intOrNull ?: 0, obj["keyAction"]?.jsonPrimitive?.contentOrNull ?: "", obj["ended"]?.jsonPrimitive?.booleanOrNull ?: false)
}
internal const val VIDEO_PAGE_PROBE = """JSON.stringify((function(){var v=document.querySelector('video'),p=document.querySelector('iframe')||v,r=(p||document.documentElement).getBoundingClientRect(),k=window.__nativeKeyAction||'';window.__nativeKeyAction='';return {url:location.href,ready:document.readyState==='complete',video:!!v,mediaReady:v?v.readyState:0,fullscreen:!!document.fullscreenElement,mediaTime:v?v.currentTime:(window.__ytTime||0),duration:v&&isFinite(v.duration)?v.duration:(window.__ytDuration||0),controls:v?!!v.controls:!!p,youtubeReady:!!window.__ytReady,youtubeState:window.__ytState===undefined?-1:window.__ytState,youtubeError:window.__ytError||0,ended:v?!!v.ended:window.__ytState===0,elementWidth:Math.round(r.width),elementHeight:Math.round(r.height),videoWidth:v?v.videoWidth:0,videoHeight:v?v.videoHeight:0,viewportWidth:window.innerWidth,viewportHeight:window.innerHeight,keyAction:k};})())"""

internal fun videoWrapperHtml(source: String, origin: String? = null): String {
    val isYouTube = source.startsWith("https://www.youtube.com/embed/")
    val playerSource = if (isYouTube && origin != null) "$source&enablejsapi=1&origin=${URLEncoder.encode(origin, StandardCharsets.UTF_8)}" else source
    val encoded = htmlAttribute(playerSource)
    val media = if (isYouTube) """<iframe id="player" src="$encoded" title="Lecteur YouTube" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe>"""
        else """<video id="player" controls playsinline src="$encoded"></video>"""
    val api = if (isYouTube) """<script>window.__ytReady=false;window.__ytState=-1;window.__ytError=0;window.__ytTime=0;window.__ytDuration=0;window.onYouTubeIframeAPIReady=function(){window.__ytPlayer=new YT.Player('player',{events:{onReady:function(){window.__ytReady=true;setInterval(function(){try{window.__ytTime=window.__ytPlayer.getCurrentTime()||0;window.__ytDuration=window.__ytPlayer.getDuration()||0}catch(e){}},250)},onStateChange:function(e){window.__ytState=e.data},onError:function(e){window.__ytError=e.data}}})};var s=document.createElement('script');s.src='https://www.youtube.com/iframe_api';document.head.appendChild(s);</script>""" else ""
    return """<!doctype html><html><head><meta charset="utf-8"><meta name="referrer" content="strict-origin-when-cross-origin"><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no"><style>*{box-sizing:border-box}html,body,#player{width:100%;height:100%;margin:0;background:#000}body{overflow:hidden}video,iframe{display:block;border:0}video{object-fit:contain}</style></head><body>$media<script>window.__nativeKeyAction='';document.addEventListener('fullscreenchange',function(){});document.addEventListener('keydown',function(e){if(e.key==='F11'){e.preventDefault();window.__nativeKeyAction='F11'}else if(e.key==='Escape'&&!document.fullscreenElement){window.__nativeKeyAction='Escape'}},true);</script>$api</body></html>"""
}

private fun htmlAttribute(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")

internal interface NativeVideoAdapter {
    val component: JComponent
    fun navigate(url: String)
    fun evaluate(script: String): CompletableFuture<String>
    fun dispose()
    fun disposeWhenIdle(): CompletableFuture<Unit> {
        dispose()
        return CompletableFuture.completedFuture(Unit)
    }
}
private class WebViewAdapter : NativeVideoAdapter {
    private val native = WebViewComponent.create(WebViewComponent.Mode.HEAVYWEIGHT).apply { setDefaultContextMenuEnabled(true) }
    private val pending = mutableSetOf<CompletableFuture<String>>()
    private var closing = false
    private val disposed = CompletableFuture<Unit>()
    override val component: JComponent get() = native
    private fun onEdt(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) action() else SwingUtilities.invokeLater(action)
    }
    override fun navigate(url: String) { onEdt { if (!closing) native.setUrl(url) } }
    override fun evaluate(script: String): CompletableFuture<String> {
        val result = CompletableFuture<String>()
        onEdt {
            if (closing) result.completeExceptionally(IllegalStateException("Video player closed"))
            else try {
                val evaluation = native.evalAsync("return ($script);")
                pending += evaluation
                evaluation.whenComplete { value, error -> onEdt {
                    pending -= evaluation
                    if (error == null) result.complete(value) else result.completeExceptionally(error)
                    finishDisposal()
                } }
            } catch (error: Exception) { result.completeExceptionally(error) }
        }
        return result
    }
    private fun finishDisposal() {
        if (closing && pending.isEmpty() && !disposed.isDone) {
            try {
                // Let the embedded Canvas remove its native peer before its parent
                // window is destroyed; explicit dispose remains safe and idempotent.
                native.parent?.remove(native)
                native.dispose()
                disposed.complete(Unit)
            } catch (error: Exception) { disposed.completeExceptionally(error) }
        }
    }
    override fun dispose() { disposeWhenIdle() }
    override fun disposeWhenIdle(): CompletableFuture<Unit> {
        onEdt { closing = true; finishDisposal() }
        return disposed
    }
}

/** Serves only a constant player shell on loopback, with one unguessable route per host. */
internal class VideoWrapperServer(source: String) : AutoCloseable {
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val token = UUID.randomUUID().toString()
    private lateinit var html: ByteArray
    val address: String
    init {
        server.createContext("/$token/") { exchange ->
            if (exchange.requestURI.path != "/$token/" || exchange.requestMethod != "GET") {
                exchange.sendResponseHeaders(404, -1); exchange.close()
            } else {
                exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
                exchange.responseHeaders.add("Cache-Control", "no-store")
                exchange.sendResponseHeaders(200, html.size.toLong())
                exchange.responseBody.use { it.write(html) }
            }
        }
        address = "http://127.0.0.1:${server.address.port}/$token/"
        html = videoWrapperHtml(source, "http://127.0.0.1:${server.address.port}").toByteArray(StandardCharsets.UTF_8)
        server.start()
    }
    override fun close() = server.stop(0)
}

/** One native WebView per window, released with its local shell. No app bridge is exposed. */
internal class NativeVideoHost(private val onState: (VideoPageState) -> Unit, private val onFailure: (String) -> Unit, private val factory: () -> NativeVideoAdapter = { WebViewAdapter() }, private val resumeStore: FileVideoResumeStore? = productionVideoResumeStore(), private val onPersistenceWarning: (String) -> Unit = {}) {
    internal val disposalComplete = CompletableFuture<Unit>()
    var view: NativeVideoAdapter? = null
        private set
    var lastProbeError: String? = null
        private set
    private var timer: Timer? = null
    private var wrapper: VideoWrapperServer? = null
    private val closed = AtomicBoolean(false)
    private val registered = AtomicBoolean(false)
    private val probing = AtomicBoolean(false)
    private var resumeKey: String? = null
    @Volatile private var resumeEntry: VideoResumeEntry? = null
    @Volatile private var resumeLoaded = true
    @Volatile private var resumeReadFailed = false
    private var restorationStarted = false
    private var seekIssued = false
    @Volatile private var restored = false
    private var restoreTarget = 0.0
    private var restoreDuration = 0.0
    private var persistenceDisabled = false
    private var lastSavedAt = 0L
    private var previousEnded = false
    @Volatile private var lastPersistableState: VideoPageState? = null
    fun create(resource: CourseResource): JPanel = create(playerAddress(resource), videoResumeKey(resource.url))
    fun create(sourceAddress: String): JPanel = create(sourceAddress, null)
    internal fun create(sourceAddress: String, stableResumeKey: String?): JPanel = JPanel(BorderLayout()).apply {
        try {
            check(System.getProperty("os.name").startsWith("Windows"))
            resumeKey = stableResumeKey
            if (stableResumeKey != null && resumeStore != null) {
                if (registered.compareAndSet(false, true)) VideoPlaybackFlushRegistry.register(this@NativeVideoHost)
                resumeLoaded = false
                VIDEO_IO.execute {
                    try { resumeEntry = resumeStore.read(stableResumeKey) }
                    catch (error: Exception) {
                        resumeReadFailed = true
                        SwingUtilities.invokeLater { if (!closed.get()) onPersistenceWarning("La reprise vidéo n’a pas pu être lue. La lecture reste disponible, mais cette position ne sera pas remplacée.") }
                    } finally { resumeLoaded = true }
                }
            }
            wrapper = VideoWrapperServer(sourceAddress)
            val native = factory()
            view = native
            native.navigate(wrapper!!.address)
            add(native.component, BorderLayout.CENTER)
            var attempts = 0
            timer = Timer(500) {
                if (!closed.get() && probing.compareAndSet(false, true)) {
                    val query = try { native.evaluate(VIDEO_PAGE_PROBE) } catch (error: Exception) {
                        lastProbeError = "${error.javaClass.simpleName}: ${error.message}"
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
                                    else handlePlaybackState(state, native)
                                }.onFailure {
                                    lastProbeError = "${it.javaClass.simpleName}: ${it.message}"
                                    if (++attempts == 20) onFailure("Le lecteur ne fournit pas son état. Ouvre la source dans le navigateur.")
                                }
                                else {
                                    lastProbeError = "${error.javaClass.simpleName}: ${error.message}"
                                    if (++attempts == 20) onFailure("Le lecteur reste indisponible. Vérifie WebView2 et la connexion.")
                                }
                            }
                        }
                    }
                }
            }.also { it.start() }
        } catch (_: LinkageError) { onFailure("Le lecteur natif ne peut pas démarrer. Le runtime Microsoft Edge WebView2 est requis.") }
        catch (_: Exception) { onFailure("Le lecteur n’est pas disponible. Tu peux ouvrir la vidéo dans le navigateur.") }
    }
    private fun handlePlaybackState(state: VideoPageState, native: NativeVideoAdapter) {
        val key = resumeKey
        val store = resumeStore
        if (key == null || store == null) {
            onState(state)
            return
        }
        if (!resumeLoaded || resumeReadFailed || persistenceDisabled) {
            onState(state)
            return
        }
        if (!restored) {
            val mediaLoaded = if (state.youtubeReady) state.duration > 0.0 else state.hasVideo && state.mediaReady >= 1 && state.duration > 0.0
            if (mediaLoaded && !restorationStarted) {
                restorationStarted = true
                restoreDuration = state.duration
                restoreTarget = restoredVideoPosition(resumeEntry, state.duration)
                if (restoreTarget <= 0.0) {
                    restored = true
                    previousEnded = state.ended
                } else {
                    try {
                        native.evaluate(videoResumeSeekScript(state.youtubeReady, restoreTarget,
                            resumeKey?.takeIf { it.startsWith("youtube:") }?.removePrefix("youtube:"))).orTimeout(4, TimeUnit.SECONDS).whenComplete { result, error ->
                            SwingUtilities.invokeLater {
                                if (!closed.get()) {
                                    if (videoSeekWasDispatched(result, error)) seekIssued = true
                                    else {
                                        persistenceDisabled = true
                                        onPersistenceWarning("La position vidéo n’a pas pu être restaurée. L’ancienne reprise est conservée.")
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {
                        persistenceDisabled = true
                        onPersistenceWarning("La position vidéo n’a pas pu être restaurée. L’ancienne reprise est conservée.")
                    }
                }
            }
            if (!restored && seekIssued) {
                if ((state.youtubeReady && state.youtubeState == 5) ||
                    videoRestorePositionConfirmed(restoreTarget, state.mediaTime, state.duration, state.youtubeReady)) {
                    restored = true
                    previousEnded = state.ended
                    lastSavedAt = System.nanoTime()
                } else if (lastSavedAt != 0L && videoRestoreTimedOut(System.nanoTime(), lastSavedAt)) {
                    persistenceDisabled = true
                    onPersistenceWarning("La position vidéo n’a pas été confirmée. L’ancienne reprise est conservée.")
                } else if (lastSavedAt == 0L) lastSavedAt = System.nanoTime()
            }
        }
        if (restored && (state.mediaReady >= 1 || (state.youtubeReady && state.youtubeState in listOf(0, 1, 2, 5)))) {
            val reliableState = if (state.youtubeReady && state.youtubeState == 5 && restoreTarget > 0.0)
                state.copy(mediaTime = restoreTarget, duration = restoreDuration) else state
            lastPersistableState = reliableState
            val now = System.nanoTime()
            val endedTransition = state.ended && !previousEnded
            previousEnded = state.ended
            if (endedTransition || now - lastSavedAt >= TimeUnit.SECONDS.toNanos(5)) {
                val entry = videoPositionToPersist(reliableState)
                if (entry != null) {
                    lastSavedAt = now
                    VIDEO_IO.execute {
                        try { store.write(key, entry) }
                        catch (_: Exception) { SwingUtilities.invokeLater { if (!closed.get()) { persistenceDisabled = true; onPersistenceWarning("Impossible d’enregistrer la reprise vidéo. Vérifie l’espace disque et les droits d’accès.") } } }
                    }
                }
            }
        }
        onState(state)
    }
    internal fun queueFlushForShutdown(): java.util.concurrent.CompletableFuture<Boolean>? {
        val state = lastPersistableState ?: return null
        val key = resumeKey ?: return null
        val store = resumeStore ?: return null
        if (!restored || resumeReadFailed) return null
        if (persistenceDisabled) return java.util.concurrent.CompletableFuture.completedFuture(false)
        val entry = videoPositionToPersist(state) ?: return null
        return java.util.concurrent.CompletableFuture.supplyAsync({
            try {
                store.write(key, entry)
                true
            } catch (_: Exception) {
                SwingUtilities.invokeLater { if (!closed.get()) onPersistenceWarning("Impossible d’enregistrer la reprise vidéo à la fermeture. L’ancienne position est conservée.") }
                false
            }
        }, VIDEO_IO)
    }
    fun close() {
        if (closed.compareAndSet(false, true)) {
            if (registered.compareAndSet(true, false)) VideoPlaybackFlushRegistry.unregister(this)
            queueFlushForShutdown()?.let { flush ->
                Thread({ runCatching { flush.get(5, TimeUnit.SECONDS) } }, "video-resume-final-flush").apply { isDaemon = false; start() }
            }
            SwingUtilities.invokeLater {
                timer?.stop(); timer = null
                val native = view
                view = null
                (native?.disposeWhenIdle() ?: CompletableFuture.completedFuture(Unit)).whenComplete { _, error ->
                    wrapper?.close(); wrapper = null
                    if (error == null) disposalComplete.complete(Unit) else disposalComplete.completeExceptionally(error)
                }
            }
        }
    }

    companion object {
        private val VIDEO_IO = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "video-resume-io").apply { isDaemon = true } }
        internal fun persistenceBarrier(): CompletableFuture<Boolean> = CompletableFuture.supplyAsync({ true }, VIDEO_IO)
    }
}
