package com.duolingo.app.ui.video

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.swing.*
import kotlin.test.*

/** Opt-in only: a real WebView2 window, controlled through its own Java API, no OS input automation. */
class NativeVideoSmokeTest {
    @Test fun systemWebViewPlaysControlledFixtureWithSourceControlsAndReleases() {
        check(System.getProperty("atelier.native.video.test") == "true")
        val ci = System.getenv("GITHUB_ACTIONS") == "true" && System.getenv("RUNNER_OS") == "Windows"
        check(ci || System.getProperty("atelier.native.video.local.optin") == "true")
        if (!ci) check(!System.getenv("WEBVIEW2_USER_DATA_FOLDER").isNullOrBlank())
        val clip = requireNotNull(javaClass.getResourceAsStream("/media/test-motion.webm")).use { it.readBytes() }
        val mp4 = requireNotNull(javaClass.getResourceAsStream("/media/test-motion.mp4")).use { it.readBytes() }
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        fun serveMedia(exchange: com.sun.net.httpserver.HttpExchange, bytes: ByteArray, mime: String) {
            exchange.responseHeaders.add("Content-Type", mime)
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
        server.createContext("/clip.webm") { serveMedia(it, clip, "video/webm") }
        server.createContext("/clip.mp4") { serveMedia(it, mp4, "video/mp4") }
        val html = """<!doctype html><html><body style="margin:0;background:#141414"><video controls muted autoplay loop playsinline src="/clip.webm" style="width:100%;height:45vh"></video><video id="mp4" controls muted autoplay loop playsinline src="/clip.mp4" style="width:100%;height:45vh"></video></body></html>""".toByteArray()
        server.createContext("/") { exchange ->
            exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            exchange.sendResponseHeaders(200, html.size.toLong()); exchange.responseBody.use { it.write(html) }
        }
        server.start()
        val playing = CountDownLatch(1)
        val observed = AtomicReference<VideoPageState>()
        val failure = AtomicReference<String>()
        val host = NativeVideoHost(onState = { state ->
            observed.set(state)
            if (state.mediaReady >= 2 && state.mediaTime > 0.1 && state.controls) playing.countDown()
        }, onFailure = { failure.set(it); playing.countDown() })
        var frame: JFrame? = null
        try {
            SwingUtilities.invokeAndWait {
                frame = JFrame("Atelier — native video test").apply {
                    defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
                    type = java.awt.Window.Type.UTILITY
                    isAutoRequestFocus = false
                    focusableWindowState = false
                    setLocation(-12000, -12000)
                    add(host.create("http://127.0.0.1:${server.address.port}/")); setSize(800, 500); isVisible = true
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
            val advanced = playing.await(45, TimeUnit.SECONDS)
            val initialReports = Path.of("build/reports/native-video"); Files.createDirectories(initialReports)
            Files.writeString(initialReports.resolve("initial-state.txt"), "observed=${observed.get()} failure=${failure.get()} nativeError=${host.lastProbeError}")
            assertTrue(advanced, "WebView2 did not decode/play the controlled fixture; observed=${observed.get()} failure=${failure.get()}")
            assertNull(failure.get(), host.lastProbeError)
            val state = requireNotNull(observed.get())
            assertTrue(state.mediaTime > 0.1 && state.duration > 0.0 && state.controls)
            var mp4Decoded = false
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
            while (!mp4Decoded && System.nanoTime() < deadline) {
                val result = requireNotNull(host.view).evaluate("(function(){var v=document.getElementById('mp4');return v.readyState>=2&&v.currentTime>0.1;})()").get(5, TimeUnit.SECONDS)
                mp4Decoded = result == "true"
                if (!mp4Decoded) Thread.sleep(100)
            }
            assertTrue(mp4Decoded, "WebView2 did not decode/play the MP4 fixture")
            val controls = requireNotNull(host.view).evaluate("""(function(){var v=document.querySelector('video');v.pause();v.playbackRate=1.5;v.volume=0.4;return new Promise(function(resolve){var done=function(){resolve({paused:v.paused,time:v.currentTime,rate:v.playbackRate,volume:v.volume});};v.addEventListener('seeked',done,{once:true});v.currentTime=0.75;setTimeout(done,3000);});})()""").get(5, TimeUnit.SECONDS)
            val values = kotlinx.serialization.json.Json.parseToJsonElement(controls) as kotlinx.serialization.json.JsonObject
            assertEquals("true", values["paused"].toString(), controls)
            assertTrue(kotlin.math.abs(values["time"].toString().toDouble() - 0.75) < 0.01, controls)
            assertEquals(1.5, values["rate"].toString().toDouble(), controls)
            assertTrue(kotlin.math.abs(values["volume"].toString().toDouble() - 0.4) < 0.00001, controls)
            val raw = requireNotNull(host.view).evaluate("""(function(){var v=document.querySelector('video'),c=document.createElement('canvas');c.width=v.videoWidth;c.height=v.videoHeight;c.getContext('2d').drawImage(v,0,0);return c.toDataURL('image/png');})()""").get(10, TimeUnit.SECONDS)
            val image = kotlinx.serialization.json.Json.parseToJsonElement(raw).let { (it as kotlinx.serialization.json.JsonPrimitive).content }
            val reports = Path.of("build/reports/native-video"); Files.createDirectories(reports)
            Files.write(reports.resolve("decoded-fixture-frame.png"), Base64.getDecoder().decode(image.substringAfter(',')))
            val remote = mutableListOf<String>()
            if (System.getenv("ATELIER_VIDEO_REMOTE_SMOKE") == "true") {
                val resources = com.duolingo.app.data.CourseResources.forSession("mit_18_06sc_2011", 1).filter { it.kind == "video" }
                resources.filter { it.url.contains("youtube.com/watch") || it.url.substringBefore('?').endsWith(".mp4") }.take(2).forEach { resource ->
                    val adapter = requireNotNull(host.view)
                    SwingUtilities.invokeAndWait { adapter.navigate(playerAddress(resource)) }
                    val until = System.nanoTime() + TimeUnit.SECONDS.toNanos(25)
                    var verified = false; var ready = 0; var found = false
                    while (!verified && System.nanoTime() < until) {
                        try {
                            adapter.evaluate("(function(){var v=document.querySelector('video');if(v){v.muted=true;v.play().catch(function(){});}return true;})()").get(3, TimeUnit.SECONDS)
                            val page = parseVideoPageState(adapter.evaluate(VIDEO_PAGE_PROBE).get(3, TimeUnit.SECONDS))
                            if (!page.url.contains("127.0.0.1")) {
                                ready = page.mediaReady; found = page.hasVideo
                                verified = page.hasVideo && page.mediaReady >= 2 && page.mediaTime > 0.1
                            }
                        } catch (_: Exception) { }
                        if (!verified) Thread.sleep(250)
                    }
                    remote.add("""{"source":"${if (resource.url.contains("youtube")) "youtube" else "mit_mp4"}","playerFound":$found,"mediaReady":$ready,"playbackVerified":$verified}""")
                }
            }
            Files.writeString(reports.resolve("summary.json"), """{"webView2Created":true,"videoDecoded":true,"mp4Decoded":true,"sourceControls":true,"pauseSeekSpeedVolumeVerified":true,"duration":${state.duration},"currentTime":${state.mediaTime},"fixture":"original-ffmpeg-testsrc","remoteChecks":[${remote.joinToString(",")}]}""")
        } finally {
            host.close()
            SwingUtilities.invokeAndWait { frame?.dispose() }
            server.stop(0)
        }
    }
}
