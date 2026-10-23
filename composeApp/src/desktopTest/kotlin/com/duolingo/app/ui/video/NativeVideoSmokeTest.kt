package com.duolingo.app.ui.video

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import java.awt.Component
import java.awt.Container
import java.awt.Toolkit
import javax.swing.*
import kotlin.test.*

/** Opt-in only: a real WebView2 window, controlled through its own Java API, no OS input automation. */
class NativeVideoSmokeTest {
    @Test fun systemWebViewPlaysControlledFixtureWithSourceControlsAndReleases() {
        check(System.getProperty("atelier.native.video.test") == "true")
        val ci = System.getenv("GITHUB_ACTIONS") == "true" && System.getenv("RUNNER_OS") == "Windows"
        check(ci || System.getProperty("atelier.native.video.local.optin") == "true")
        if (!ci) check(!System.getenv("WEBVIEW2_USER_DATA_FOLDER").isNullOrBlank())
        val mp4 = requireNotNull(javaClass.getResourceAsStream("/media/test-motion.mp4")).use { it.readBytes() }
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        fun serveMedia(exchange: com.sun.net.httpserver.HttpExchange, bytes: ByteArray, mime: String) {
            exchange.responseHeaders.add("Content-Type", mime)
            exchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
            exchange.responseHeaders.add("Accept-Ranges", "bytes")
            val range = exchange.requestHeaders.getFirst("Range")
            val match = range?.let { Regex("bytes=(\\d+)-(\\d*)").matchEntire(it) }
            val start = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val end = match?.groupValues?.get(2)?.toIntOrNull()?.coerceAtMost(bytes.lastIndex) ?: bytes.lastIndex
            if ((range != null && match == null) || start > end || start >= bytes.size) {
                exchange.responseHeaders.add("Content-Range", "bytes */${bytes.size}")
                exchange.sendResponseHeaders(416, -1); exchange.close(); return
            }
            if (match != null) exchange.responseHeaders.add("Content-Range", "bytes $start-$end/${bytes.size}")
            exchange.sendResponseHeaders(if (match == null) 200 else 206, (end - start + 1).toLong())
            exchange.responseBody.use { it.write(bytes, start, end - start + 1) }
        }
        server.createContext("/clip.mp4") { serveMedia(it, mp4, "video/mp4") }
        server.start()
        val resumeDirectory = Files.createTempDirectory("native-video-resume")
        val resumeStore = FileVideoResumeStore(resumeDirectory.resolve("video-playback.json"))
        val resumeKey = "mp4:local-fixture:${server.address.port}"
        val playing = CountDownLatch(1)
        val observed = AtomicReference<VideoPageState>()
        val failure = AtomicReference<String>()
        var host = NativeVideoHost(onState = { state ->
            observed.set(state)
            if (state.mediaReady >= 2 && state.mediaTime > 0.1 && state.controls) playing.countDown()
        }, onFailure = { failure.set(it); playing.countDown() }, resumeStore = resumeStore)
        var frame: JFrame? = null
        var playerPanel: JPanel? = null
        try {
            SwingUtilities.invokeAndWait {
                frame = JFrame("Atelier — native video test").apply {
                    defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
                    type = java.awt.Window.Type.UTILITY
                    isAutoRequestFocus = false
                    focusableWindowState = false
                    setLocation(-12000, -12000)
                    playerPanel = host.create("http://127.0.0.1:${server.address.port}/clip.mp4", resumeKey)
                    add(requireNotNull(playerPanel)); setSize(800, 500); isVisible = true
                }
                // The library attaches its native peer on the Canvas's first paint.
                // A window outside the CI desktop may never receive an OS paint event.
                fun paintNativePeers(component: java.awt.Component) {
                    if (component is java.awt.Canvas) {
                        check(component.isDisplayable)
                        val buffer = java.awt.image.BufferedImage(800, 500, java.awt.image.BufferedImage.TYPE_INT_ARGB)
                        val graphics = buffer.createGraphics()
                        try { component.paint(graphics) } finally { graphics.dispose() }
                    } else if (component is java.awt.Container) component.components.forEach { paintNativePeers(it) }
                }
                paintNativePeers(requireNotNull(frame))
            }
            val playDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
            var playRequested = false
            while (!playRequested && System.nanoTime() < playDeadline) {
                playRequested = requireNotNull(host.view).evaluate("(function(){var v=document.querySelector('video');if(!v)return false;if(!v.dataset.smokeCorsReload){var s=v.currentSrc||v.src;v.pause();v.crossOrigin='anonymous';v.dataset.smokeCorsReload='true';v.src=s;v.load();}if(v.readyState<2)return false;v.muted=true;v.play().catch(function(){});return true;})()").get(3, TimeUnit.SECONDS) == "true"
                if (!playRequested) Thread.sleep(100)
            }
            assertTrue(playRequested, "Production MP4 player did not become available")
            val advanced = playing.await(45, TimeUnit.SECONDS)
            val initialReports = Path.of("build/reports/native-video"); Files.createDirectories(initialReports)
            Files.writeString(initialReports.resolve("initial-state.txt"), "observed=${observed.get()} failure=${failure.get()} nativeError=${host.lastProbeError}")
            assertTrue(advanced, "WebView2 did not decode/play the controlled fixture; observed=${observed.get()} failure=${failure.get()}")
            assertNull(failure.get(), host.lastProbeError)
            val state = requireNotNull(observed.get())
            assertTrue(state.mediaTime > 0.1 && state.duration > 0.0 && state.controls)
            val adapter = requireNotNull(host.view)
            val initialSize = parseVideoPageState(adapter.evaluate(VIDEO_PAGE_PROBE).get(5, TimeUnit.SECONDS))
            assertTrue(initialSize.elementWidth > 700, "Production wrapper did not fill initial width: $initialSize")
            assertTrue(initialSize.elementHeight > 400, "Production wrapper did not fill initial height: $initialSize")
            val reports = Path.of("build/reports/native-video"); Files.createDirectories(reports)
            fun awtTree(component: Component): String = buildString {
                append(component.javaClass.simpleName).append(component.bounds)
                if (component is Container) component.components.forEach { append(" {").append(awtTree(it)).append('}') }
            }
            fun awtSnapshot(): String {
                var value = ""
                SwingUtilities.invokeAndWait {
                    value = "frame=${frame?.bounds} content=${frame?.contentPane?.bounds} playerPanel=${playerPanel?.bounds} native=${host.view?.component?.bounds} tree=${frame?.contentPane?.let(::awtTree)}"
                }
                return value
            }
            fun diagnostic(label: String, page: VideoPageState): String = "$label page=$page ${awtSnapshot()}"
            val resizeDiagnostics = mutableListOf(diagnostic("before-resize", initialSize))
            var initialPanelWidth = 0; var initialPanelHeight = 0
            SwingUtilities.invokeAndWait {
                playerPanel?.let { initialPanelWidth = it.width; initialPanelHeight = it.height }
            }
            SwingUtilities.invokeAndWait {
                val window = requireNotNull(frame)
                window.setSize(1200, 800)
                // The WebView library forwards its heavyweight Canvas componentResized event to sizeNative().
                // Validate and lay out synchronously because an offscreen CI window may receive no OS paint event.
                window.invalidate()
                window.validate()
                fun layoutTree(component: Container) {
                    component.doLayout()
                    component.components.filterIsInstance<Container>().forEach(::layoutTree)
                }
                layoutTree(window)
                Toolkit.getDefaultToolkit().sync()
            }
            val resizeDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
            var resized: VideoPageState? = null
            var lastResizeState: VideoPageState? = null
            var probeIndex = 0
            while (resized == null && System.nanoTime() < resizeDeadline) {
                val measurement = parseVideoPageState(adapter.evaluate(VIDEO_PAGE_PROBE).get(5, TimeUnit.SECONDS))
                lastResizeState = measurement
                if (probeIndex % 5 == 0) {
                    resizeDiagnostics += diagnostic("after-resize-probe-$probeIndex", measurement)
                }
                var panelWidth = 0; var panelHeight = 0; var nativeWidth = 0; var nativeHeight = 0
                SwingUtilities.invokeAndWait {
                    playerPanel?.let { panelWidth = it.width; panelHeight = it.height }
                    host.view?.component?.let { nativeWidth = it.width; nativeHeight = it.height }
                }
                val matchesSwingBounds = kotlin.math.abs(measurement.viewportWidth - panelWidth) <= 4 &&
                    kotlin.math.abs(measurement.viewportHeight - panelHeight) <= 4 &&
                    kotlin.math.abs(measurement.elementWidth - nativeWidth) <= 4 &&
                    kotlin.math.abs(measurement.elementHeight - nativeHeight) <= 4
                val growsWithHost = measurement.elementWidth >= initialSize.elementWidth + 50 &&
                    measurement.elementHeight >= initialSize.elementHeight + 50 &&
                    panelWidth >= initialPanelWidth + 50 && panelHeight >= initialPanelHeight + 50
                if (matchesSwingBounds && growsWithHost) resized = measurement
                else Thread.sleep(100)
                probeIndex++
            }
            resized?.let { resizeDiagnostics += diagnostic("resize-confirmed", it) }
            Files.writeString(reports.resolve("resize-diagnostics.txt"), resizeDiagnostics.joinToString("\n", postfix = "\n"))
            Files.writeString(reports.resolve("initial-state.txt"), "observed=${observed.get()} failure=${failure.get()} nativeError=${host.lastProbeError}\n${resizeDiagnostics.joinToString("\n")}\n")
            assertNotNull(resized, "Production video element did not track actual resized Swing bounds with meaningful growth: last=$lastResizeState ${awtSnapshot()}; see ${reports.resolve("resize-diagnostics.txt")}")
            val controls = requireNotNull(host.view).evaluate("""(function(){var v=document.querySelector('video');v.pause();v.playbackRate=1.5;v.volume=0.4;return new Promise(function(resolve){var done=function(){resolve({paused:v.paused,time:v.currentTime,rate:v.playbackRate,volume:v.volume});};v.addEventListener('seeked',done,{once:true});v.currentTime=0.75;setTimeout(done,3000);});})()""").get(5, TimeUnit.SECONDS)
            val values = kotlinx.serialization.json.Json.parseToJsonElement(controls) as kotlinx.serialization.json.JsonObject
            assertEquals("true", values["paused"].toString(), controls)
            assertTrue(kotlin.math.abs(values["time"].toString().toDouble() - 0.75) < 0.01, controls)
            assertEquals(1.5, values["rate"].toString().toDouble(), controls)
            assertTrue(kotlin.math.abs(values["volume"].toString().toDouble() - 0.4) < 0.00001, controls)
            val decodedDimensions = parseVideoPageState(requireNotNull(host.view).evaluate(VIDEO_PAGE_PROBE).get(5, TimeUnit.SECONDS))
            assertTrue(decodedDimensions.videoWidth > 0 && decodedDimensions.videoHeight > 0, "$decodedDimensions")
            val raw = adapter.evaluate("""(function(){var v=document.querySelector('video'),c=document.createElement('canvas');c.width=v.videoWidth;c.height=v.videoHeight;c.getContext('2d').drawImage(v,0,0);return c.toDataURL('image/png');})()""").get(10, TimeUnit.SECONDS)
            val image = kotlinx.serialization.json.Json.parseToJsonElement(raw).let { (it as kotlinx.serialization.json.JsonPrimitive).content }
            Files.write(reports.resolve("decoded-fixture-frame.png"), Base64.getDecoder().decode(image.substringAfter(',')))
            val seekDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (kotlin.math.abs((observed.get()?.mediaTime ?: 0.0) - 0.75) > 0.05 && System.nanoTime() < seekDeadline) Thread.sleep(100)
            assertTrue(kotlin.math.abs((observed.get()?.mediaTime ?: 0.0) - 0.75) <= 0.05, "Periodic host probe did not observe the manual seek before close: ${observed.get()}")
            Files.writeString(reports.resolve("lifecycle.txt"), "Closing first player after confirmed seek\n")
            host.close()
            host.disposalComplete.get(8, TimeUnit.SECONDS)
            Files.writeString(reports.resolve("lifecycle.txt"), "First native player disposed\n", java.nio.file.StandardOpenOption.APPEND)
            var flushed: VideoResumeEntry? = null
            val flushDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(6)
            while ((flushed == null || kotlin.math.abs(flushed.positionSeconds - 0.75) > 0.05) && System.nanoTime() < flushDeadline) {
                flushed = resumeStore.read(resumeKey)
                if (flushed == null || kotlin.math.abs(flushed.positionSeconds - 0.75) > 0.05) Thread.sleep(50)
            }
            val storedResume = requireNotNull(flushed) { "Close did not flush the final video position" }
            assertTrue(kotlin.math.abs(storedResume.positionSeconds - 0.75) <= 0.05, "Close flush stored the wrong position: $storedResume")
            assertTrue(kotlin.math.abs(storedResume.durationSeconds - requireNotNull(observed.get()).duration) <= 0.01, "Close flush changed the duration: $storedResume")
            val resumed = CountDownLatch(1)
            val resumeFailure = AtomicReference<String>()
            host = NativeVideoHost(onState = { page ->
                observed.set(page)
                if (page.mediaReady >= 2 && kotlin.math.abs(page.mediaTime - 0.75) < 0.2) resumed.countDown()
            }, onFailure = { resumeFailure.set(it) }, resumeStore = resumeStore)
            Files.writeString(reports.resolve("lifecycle.txt"), "Creating second player\n", java.nio.file.StandardOpenOption.APPEND)
            SwingUtilities.invokeAndWait {
                frame?.dispose()
                frame = JFrame("Atelier — video resume test").apply {
                    defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
                    type = java.awt.Window.Type.UTILITY
                    isAutoRequestFocus = false
                    focusableWindowState = false
                    setLocation(-12000, -12000)
                    playerPanel = host.create("http://127.0.0.1:${server.address.port}/clip.mp4", resumeKey)
                    add(requireNotNull(playerPanel)); setSize(800, 500); isVisible = true
                }
                fun paintAgain(component: java.awt.Component) {
                    if (component is java.awt.Canvas) {
                        val buffer = java.awt.image.BufferedImage(800, 500, java.awt.image.BufferedImage.TYPE_INT_ARGB)
                        val graphics = buffer.createGraphics()
                        try { component.paint(graphics) } finally { graphics.dispose() }
                    } else if (component is Container) component.components.forEach(::paintAgain)
                }
                paintAgain(requireNotNull(frame))
            }
            Files.writeString(reports.resolve("lifecycle.txt"), "Second player attached; waiting for resume\n", java.nio.file.StandardOpenOption.APPEND)
            assertTrue(resumed.await(15, TimeUnit.SECONDS), "Reopened production MP4 wrapper did not seek to the stored position: ${observed.get()} failure=${resumeFailure.get()}")
            val appCloseFlush = runBlocking { flushVideoPlaybackPositions() }
            assertEquals(1, appCloseFlush.activePlayers)
            assertEquals(1, appCloseFlush.flushedPlayers)
            assertEquals(0, appCloseFlush.failedPlayers)
            val resumedControls = requireNotNull(host.view).evaluate("(function(){var v=document.querySelector('video');return {time:v.currentTime,paused:v.paused};})()" ).get(5, TimeUnit.SECONDS)
            val resumedValues = kotlinx.serialization.json.Json.parseToJsonElement(resumedControls) as kotlinx.serialization.json.JsonObject
            assertTrue(kotlin.math.abs(resumedValues["time"]!!.toString().toDouble() - 0.75) < 0.2, resumedControls)
            assertEquals("true", resumedValues["paused"].toString(), "Resume must restore the position without forcing playback: $resumedControls")
            // Model the documented IFrame API contract: seekTo starts playback
            // unless already paused, whereas cueVideoById waits for user input.
            val scriptAdapter = requireNotNull(host.view)
            listOf(-1, 1, 2, 5).forEach { initial ->
                scriptAdapter.evaluate("(function(){window.__testYtState=$initial;window.__testYtTime=0;window.__testYtCue=-1;window.__ytPlayer={getPlayerState:function(){return window.__testYtState;},seekTo:function(t){window.__testYtTime=t;if(window.__testYtState!==2)window.__testYtState=1;},cueVideoById:function(o){window.__testYtCue=o.startSeconds;window.__testYtState=5;}};return true;})()").get(5, TimeUnit.SECONDS)
                assertTrue(videoSeekWasDispatched(scriptAdapter.evaluate(videoResumeSeekScript(true, 12.5, "Gbtma_UQpro")).get(5, TimeUnit.SECONDS), null))
                val result = scriptAdapter.evaluate("({state:window.__testYtState,time:window.__testYtTime,cue:window.__testYtCue})").get(5, TimeUnit.SECONDS)
                val model = kotlinx.serialization.json.Json.parseToJsonElement(result) as kotlinx.serialization.json.JsonObject
                assertEquals(if (initial in listOf(1, 2)) initial else 5, model["state"]!!.toString().toInt(), result)
                assertEquals(12.5, model[if (initial in listOf(1, 2)) "time" else "cue"]!!.toString().toDouble(), result)
            }
            val remote = mutableListOf<String>()
            if (System.getenv("ATELIER_VIDEO_REMOTE_SMOKE") == "true") {
                val resources = com.duolingo.app.data.CourseResources.forSession("mit_18_06sc_2011", 1).filter { it.kind == "video" }
                resources.filter { it.url.contains("youtube.com/watch") || it.url.substringBefore('?').endsWith(".mp4") }.take(2).forEach { resource ->
                    val adapter = requireNotNull(host.view)
                    val remoteShell = VideoWrapperServer(playerAddress(resource))
                    SwingUtilities.invokeAndWait { adapter.navigate(remoteShell.address) }
                    val until = System.nanoTime() + TimeUnit.SECONDS.toNanos(25)
                    var verified = false; var ready = 0; var found = false
                    while (!verified && System.nanoTime() < until) {
                        try {
                            adapter.evaluate("(function(){var v=document.querySelector('video');if(v){v.muted=true;v.play().catch(function(){});}if(window.__ytPlayer)window.__ytPlayer.playVideo();return true;})()").get(3, TimeUnit.SECONDS)
                            val page = parseVideoPageState(adapter.evaluate(VIDEO_PAGE_PROBE).get(3, TimeUnit.SECONDS))
                            ready = page.mediaReady; found = page.hasVideo || page.youtubeReady
                            verified = if (resource.url.contains("youtube.com")) page.youtubeReady && page.youtubeState == 1
                                else page.hasVideo && page.mediaReady >= 2 && page.mediaTime > 0.1
                        } catch (_: Exception) { }
                        if (!verified) Thread.sleep(250)
                    }
                    remote.add("""{"source":"${if (resource.url.contains("youtube")) "youtube" else "mit_mp4"}","playerFound":$found,"mediaReady":$ready,"playbackVerified":$verified}""")
                    remoteShell.close()
                }
            }
            Files.writeString(reports.resolve("summary.json"), """{"webView2Created":true,"videoDecoded":true,"mp4Decoded":true,"sourceControls":true,"pauseSeekSpeedVolumeVerified":true,"mp4ResumeAfterReopenVerified":true,"applicationCloseFlushVerified":true,"youtubeResumeApiContractVerified":true,"duration":${state.duration},"currentTime":${state.mediaTime},"fixture":"original-ffmpeg-testsrc","remoteChecks":[${remote.joinToString(",")}]}""")
        } finally {
            host.close()
            runCatching { host.disposalComplete.get(8, TimeUnit.SECONDS) }
            runBlocking { flushVideoPlaybackPositions() }
            SwingUtilities.invokeAndWait { frame?.dispose() }
            server.stop(0)
            Files.deleteIfExists(resumeDirectory.resolve("video-playback.json"))
            Files.deleteIfExists(resumeDirectory)
        }
    }
}
