@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package com.duolingo.app.ui.download

import androidx.compose.material.Surface
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.material.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asSkiaBitmap
import com.duolingo.app.LearningAppTheme
import com.duolingo.app.data.CourseResource
import com.duolingo.app.data.CourseResources
import com.duolingo.app.ui.*
import java.io.File
import org.jetbrains.skia.Image
import kotlin.test.Test
import kotlin.test.assertEquals

class FrenchDownloadUiTest {
    private val video = CourseResources.forSession("mit_18_06sc_2011", 1).first { it.url.contains("youtube.com/watch") }

    @Test fun youtubeDownloadActionOpensItsProviderAndLeavesMp4WithoutThisAction() {
        listOf(500, 1100).forEach { width -> runDesktopComposeUiTest(width = width, height = 700) {
            var opened = ""
            val downloader = object : VideoDownloader {
                @Composable override fun Download(resource: CourseResource, onClose: () -> Unit) {
                    opened = resource.url
                    Text("Téléchargement français ouvert")
                }
            }
            setContent { LearningAppTheme { Surface { VideoDownloadProvider(downloader) {
                Column { ReferenceLinks(listOf(video, video.copy(id = "mp4", url = "https://archive.org/video.mp4")), true) }
            } } } }
            onAllNodesWithText("Télécharger avec la piste audio française").assertCountEquals(1)
            onNodeWithText("Télécharger avec la piste audio française").performClick()
            onNodeWithText("Téléchargement français ouvert").assertExists()
            runOnIdle { assertEquals(video.url, opened) }
        } }
    }

    @Test fun missingToolsDialogHasAnExitAndRemainsReadableAtBothWidths() {
        listOf(500, 1100).forEach { width -> runDesktopComposeUiTest(width = width, height = 700) {
            setContent { LearningAppTheme {
                DesktopVideoDownloader(null, "Outil manquant : yt-dlp.exe.").Download(video) {}
            } }
            onNodeWithText("Outil manquant : yt-dlp.exe.").assertExists()
            onNodeWithText("Fermer").assertExists()
            val root = onNode(isRoot() and hasAnyDescendant(hasText("Télécharger en français")))
            val file = File("build/reports/ui-snapshots/french-download-$width.png")
            file.parentFile.mkdirs()
            Image.makeFromBitmap(root.captureToImage().asSkiaBitmap()).use { image ->
                image.encodeToData()!!.use { file.writeBytes(it.bytes) }
            }
        } }
    }
}
