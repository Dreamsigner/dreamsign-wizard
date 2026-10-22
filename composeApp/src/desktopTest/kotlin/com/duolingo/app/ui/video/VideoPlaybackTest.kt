@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package com.duolingo.app.ui.video

import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asSkiaBitmap
import com.duolingo.app.LearningAppTheme
import com.duolingo.app.data.CourseResources
import com.duolingo.app.ui.*
import java.io.File
import java.util.concurrent.CompletableFuture
import javax.swing.*
import org.jetbrains.skia.Image
import kotlin.test.*

class VideoPlaybackTest {
    private val video = CourseResources.forSession("mit_18_06sc_2011", 1).first { it.kind == "video" && it.url.contains("youtube.com/watch") }
    @Test fun sourceUrlValidationPreservesVideoAndRejectsLocalPagesAndUntrustedFiles() {
        val videoId = com.duolingo.app.ui.youtubeVideoId(video.url)!!
        assertEquals("https://www.youtube.com/embed/$videoId?controls=1&fs=1", playerAddress(video))
        val mp4 = video.copy(url = "https://archive.org/download/mit/lecture.mp4")
        assertEquals(mp4.url, playerAddress(mp4))
        listOf("http://archive.org/lecture.mp4", "file:///C:/secret.mp4", "https://evil.example/lecture.mp4", "https://archive.org.evil/x.mp4", "https://user@archive.org/x.mp4", "https://archive.org/x.exe").forEach { url -> assertFails { playerAddress(video.copy(url = url)) } }
        assertFails { playerAddress(video.copy(kind = "pdf")) }
    }
    @Test fun allAuthoredMathVideoResourcesHaveAPlayableSourceAddress() {
        val resources = com.duolingo.app.data.SampleData.sampleSubjects
            .filter { it.category == com.duolingo.app.model.SubjectCategory.MATHEMATICS }
            .flatMap { it.units }.flatMap { it.lessons }.flatMap { it.exercises.take(1) }
            .flatMap { CourseResources.forExercise(it.id) }.filter { it.kind == "video" }.distinctBy { it.url }
        assertTrue(resources.isNotEmpty())
        resources.forEach {
            val address = playerAddress(it)
            if (it.url.contains("youtube.com/watch")) assertTrue(address.startsWith("https://www.youtube.com/embed/"), it.id)
            else assertEquals(it.url, address, it.id)
        }
    }
    @Test fun wrapperUsesOfficialYoutubeEmbedOrNativeMp4ControlsAndKeepsFitContain() {
        val youtube = videoWrapperHtml(playerAddress(video), "http://127.0.0.1:43125")
        assertTrue(youtube.contains("https://www.youtube.com/embed/"))
        assertTrue(youtube.contains("enablejsapi=1"))
        assertTrue(youtube.contains("referrerpolicy=\"strict-origin-when-cross-origin\""))
        assertTrue(youtube.contains("allowfullscreen"))
        assertFalse(youtube.contains("youtube.com/watch"))
        assertFalse(youtube.contains("display:none"))
        val mp4 = videoWrapperHtml("https://archive.org/files/lecture.mp4?a=1&b=2")
        assertTrue(mp4.contains("<video id=\"player\" controls playsinline"))
        assertTrue(mp4.contains("object-fit:contain"))
        assertTrue(mp4.contains("background:#000"))
    }
    @Test fun parsesWebViewJsonIncludingDoubleEncodedResultsAndMediaState() {
        val raw = """{"url":"https://www.youtube.com/watch?v=abc","ready":true,"video":true,"mediaReady":4,"fullscreen":true,"mediaTime":0.8,"duration":2,"controls":true}"""
        val state = parseVideoPageState(raw)
        assertTrue(state.fullscreen && state.controls && state.hasVideo)
        assertEquals(0.8, state.mediaTime)
        val encoded = kotlinx.serialization.json.JsonPrimitive(raw).toString()
        assertEquals(state, parseVideoPageState(encoded))
        assertFails { parseVideoPageState("null") }
    }
    @Test fun nativeHostDisposesExactlyOnceAndDoesNotExposeAnAppBridge() {
        var disposed = 0
        var navigated = ""
        val adapter = object : NativeVideoAdapter {
            override val component = JPanel()
            override fun navigate(url: String) { navigated = url }
            override fun evaluate(script: String) = CompletableFuture.completedFuture("{}")
            override fun dispose() { disposed++ }
        }
        val host = NativeVideoHost({}, {}, { adapter })
        SwingUtilities.invokeAndWait { host.create(video); host.close(); host.close() }
        SwingUtilities.invokeAndWait { }
        assertTrue(navigated.startsWith("http://127.0.0.1:"))
        assertEquals(1, disposed)
        assertFalse(VIDEO_PAGE_PROBE.contains("addJavascriptCallback"))
    }
    @Test fun playActionUsesNativeProviderAndRemainsSeparateFromBrowserLink() = runDesktopComposeUiTest(width = 1000, height = 700) {
        var opened = ""
        val player = object : VideoPlayer {
            @Composable override fun Player(resource: com.duolingo.app.data.CourseResource, onClose: () -> Unit) {
                opened = resource.url
                Text("Lecteur intégré ouvert")
            }
        }
        setContent { LearningAppTheme { Surface { VideoPlaybackProvider(player) { ReferenceLinks(listOf(video), showVideoPreviews = true) } } } }
        onNodeWithText("Lire dans l’app").performClick()
        onNodeWithText("Lecteur intégré ouvert").assertExists()
        runOnIdle { assertEquals(video.url, opened) }
        onNodeWithText("Ouvrir la vidéo").assertExists()
    }
    @Test fun playerHeaderControlsRemainAccessibleAtBothWidths() {
        listOf(500, 1100).forEach { width -> runDesktopComposeUiTest(width = width, height = 400) {
            var fullscreen = 0; var browser = 0; var closed = 0
            setContent { LearningAppTheme { VideoPlayerLayout("The Geometry of Linear Equations", "Commandes du lecteur source", false, { fullscreen++ }, { browser++ }, { closed++ }) { Text("Surface vidéo") } } }
            onNodeWithText("Plein écran").performClick()
            onNodeWithText("Ouvrir dans le navigateur").performClick()
            onNodeWithText("Fermer la vidéo").performClick()
            runOnIdle { assertEquals(1, fullscreen); assertEquals(1, browser); assertEquals(1, closed) }
            val file = File("build/reports/ui-snapshots/video-player-header-$width.png"); file.parentFile.mkdirs()
            Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image -> image.encodeToData()!!.use { file.writeBytes(it.bytes) } }
        } }
    }
    @Test fun fullscreenLayoutCompletelyRemovesApplicationHeader() = runDesktopComposeUiTest(width = 900, height = 600) {
        setContent { LearningAppTheme { VideoPlayerLayout("Titre secret header", "État", true, {}, {}, {}) { Text("Surface vidéo") } } }
        onNodeWithText("Titre secret header").assertDoesNotExist()
        onNodeWithText("Ouvrir dans le navigateur").assertDoesNotExist()
        onNodeWithText("Surface vidéo").assertExists()
    }
    @Test fun fullscreenStatePreservesPlacementAcrossAppAndSourceEventsAndF11() {
        val maximized = FullscreenSession(placement = androidx.compose.ui.window.WindowPlacement.Maximized,
            previousPlacement = androidx.compose.ui.window.WindowPlacement.Maximized)
        val appStarted = maximized.enterWindowFullscreen()
        assertEquals(androidx.compose.ui.window.WindowPlacement.Maximized, appStarted.previousPlacement)
        val appDomEntered = appStarted.onSourceFullscreen(true)
        assertEquals(androidx.compose.ui.window.WindowPlacement.Maximized, appDomEntered.previousPlacement)
        assertEquals(appDomEntered, appDomEntered.onShortcut()) // caller exits DOM; DOM event restores prior placement
        assertEquals(maximized, appDomEntered.onSourceFullscreen(false))

        val sourceStarted = maximized.onSourceFullscreen(true)
        assertEquals(maximized, sourceStarted.onSourceFullscreen(false))
        val restoredMaximized = FullscreenSession().synchronizeWindowPlacement(androidx.compose.ui.window.WindowPlacement.Maximized)
            .onSourceFullscreen(true).onSourceFullscreen(false)
        assertEquals(androidx.compose.ui.window.WindowPlacement.Maximized, restoredMaximized.placement)
        val f11Entered = maximized.onShortcut()
        assertEquals(androidx.compose.ui.window.WindowPlacement.Fullscreen, f11Entered.placement)
        assertEquals(maximized, f11Entered.onShortcut())
    }
}
