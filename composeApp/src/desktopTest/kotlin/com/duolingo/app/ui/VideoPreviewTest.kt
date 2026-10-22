@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package com.duolingo.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.SemanticsActions
import com.duolingo.app.LearningAppTheme
import com.duolingo.app.data.CourseResources
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

class VideoPreviewTest {
    private val video = CourseResources.forSession("mit_18_06sc_2011", 1).first { it.kind == "video" && it.url.startsWith("https://www.youtube.com/watch?v=") }

    @Test fun recognizesOnlyExactYouTubeHostsAndValidVideoIds() {
        assertEquals("abcDEf_12-3", youtubeVideoId("https://www.youtube.com/watch?v=abcDEf_12-3&t=30"))
        assertEquals("abcDEf_12-3", youtubeVideoId("https://youtu.be/abcDEf_12-3"))
        listOf("http://www.youtube.com/watch?v=abcDEf_12-3", "https://youtube.com.evil/watch?v=abcDEf_12-3",
            "https://www.youtube.com/watch?v=../secret", "https://www.youtube.com/watch?v=abcDEf_12-34", video.sourceUrl).forEach { assertNull(youtubeVideoId(it), it) }
    }

    @Test fun loaderBoundsInputDimensionsAndCacheAndRejectsUnsafeIds() = runBlocking {
        val bytes = ByteArrayOutputStream().also { ImageIO.write(BufferedImage(480, 360, BufferedImage.TYPE_INT_RGB), "jpg", it) }.toByteArray()
        var calls = 0
        val loader = DesktopVideoThumbnailLoader { url ->
            assertTrue(url.startsWith("https://i.ytimg.com/vi/")); calls++; bytes
        }
        assertNull(loader.load("../bad")); assertEquals(0, calls)
        assertNotNull(loader.load("abcDEf_12-3")); assertNotNull(loader.load("abcDEf_12-3")); assertEquals(1, calls)
        (0..8).forEach { loader.load("abcdefghij$it") }
        loader.load("abcDEf_12-3"); assertEquals(11, calls)
        assertNull(DesktopVideoThumbnailLoader { ByteArray(MAX_THUMBNAIL_BYTES + 1) }.load("abcDEf_12-3"))
        assertNull(DesktopVideoThumbnailLoader { byteArrayOf(1, 2, 3) }.load("abcDEf_12-3"))
        val huge = ByteArrayOutputStream().also { ImageIO.write(BufferedImage(1300, 10, BufferedImage.TYPE_INT_RGB), "jpg", it) }.toByteArray()
        assertNull(DesktopVideoThumbnailLoader { huge }.load("abcDEf_12-3"))
        assertFailsWith<IllegalArgumentException> { readThumbnailBytes(ByteArrayInputStream(ByteArray(MAX_THUMBNAIL_BYTES + 1))) }
        assertContentEquals(byteArrayOf(1, 2), readThumbnailBytes(ByteArrayInputStream(byteArrayOf(1, 2))))
    }

    @Test fun previewOpensExactVideoAndRemainsUsableWithoutThumbnailAtBothWidths() {
        listOf(1100, 500).forEach { width ->
            runDesktopComposeUiTest(width = width, height = 600) {
                var opened: String? = null
                val handler = object : UriHandler { override fun openUri(uri: String) { opened = uri } }
                setContent { LearningAppTheme { Surface(color = White) {
                    CompositionLocalProvider(LocalUriHandler provides handler) {
                        VideoThumbnailProvider(VideoThumbnailLoader { null }) {
                            Column(Modifier.fillMaxWidth().padding(24.dp)) { ReferenceLinks(listOf(video), showVideoPreviews = true) }
                        }
                    }
                } } }
                onNodeWithText("Aperçu non disponible").assertIsDisplayed()
                onNodeWithText("Ouverture dans le navigateur").assertIsDisplayed()
                onNodeWithText("Ouvrir la vidéo").performClick()
                runOnIdle { assertEquals(video.url, opened) }
                save("video-preview-fallback-$width")
            }
        }
    }

    @Test fun loadedThumbnailIsDisplayedAndMp4UsesHonestFallback() = runDesktopComposeUiTest(width = 1100, height = 850) {
        val bytes = ByteArrayOutputStream().also { ImageIO.write(BufferedImage(480, 360, BufferedImage.TYPE_INT_RGB).apply {
            createGraphics().also { it.color = java.awt.Color(74, 74, 74); it.fillRect(0, 0, width, height); it.dispose() }
        }, "jpg", it) }.toByteArray()
        val loader = DesktopVideoThumbnailLoader { bytes }
        setContent { LearningAppTheme { Surface(color = White) {
            VideoThumbnailProvider(loader) { Column(Modifier.fillMaxWidth().padding(24.dp)) {
                ReferenceLinks(listOf(video, video.copy(id = "mp4", url = "https://archive.org/example.mp4", title = "Cours vidéo MP4")), showVideoPreviews = true)
            } }
        } } }
        waitUntil(timeoutMillis = 5000) { onAllNodesWithContentDescription("Aperçu de la vidéo", substring = true).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Vidéo · fichier MP4").assertIsDisplayed()
        onNodeWithText("Aperçu non disponible").assertIsDisplayed()
        save("video-preview-loaded-and-mp4")
    }

    @OptIn(androidx.compose.ui.InternalComposeUiApi::class)
    @Test fun keyboardActivationReportsBrowserFailureWithoutClosingTheLesson() = runDesktopComposeUiTest(width = 700, height = 500) {
        val handler = object : UriHandler { override fun openUri(uri: String) { error("No browser") } }
        setContent { LearningAppTheme { CompositionLocalProvider(LocalUriHandler provides handler) {
            ReferenceLinks(listOf(video), showVideoPreviews = true)
        } } }
        val button = onNodeWithText("Ouvrir la vidéo")
        button.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        button.assertIsFocused()
        button.performKeyPress(KeyEvent(Key.Enter, KeyEventType.KeyDown))
        button.performKeyPress(KeyEvent(Key.Enter, KeyEventType.KeyUp))
        onNodeWithText("Impossible d’ouvrir ce support.", substring = true).assertExists()
        button.assertIsEnabled()
    }

    @Test fun existingReferencePresentationDoesNotLoadPreviewsByDefault() = runDesktopComposeUiTest(width = 700, height = 400) {
        var calls = 0
        setContent { LearningAppTheme { VideoThumbnailProvider(VideoThumbnailLoader { calls++; null }) { ReferenceLinks(listOf(video)) } } }
        onNodeWithTag("video-poster").assertDoesNotExist()
        runOnIdle { assertEquals(0, calls) }
    }

    private fun ComposeUiTest.save(name: String) {
        val file = File("build/reports/ui-snapshots/$name.png"); file.parentFile.mkdirs()
        Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image -> image.encodeToData()!!.use { file.writeBytes(it.bytes) } }
    }
}
