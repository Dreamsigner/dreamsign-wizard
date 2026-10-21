@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.duolingo.app.ui.pdf

import com.duolingo.app.LearningAppTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.semantics.SemanticsActions
import com.duolingo.app.data.CourseResource
import com.duolingo.app.data.CourseResources
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.jetbrains.skia.Image
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.awt.image.BufferedImage

class DesktopPdfDocumentReaderTest {
    @Test
    fun closingDoesNotWaitForASlowBackgroundPageRender() = runBlocking<Unit> {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val document = PDDocument().apply { addPage(PDPage()) }
        val session = PdfBoxDocumentSession(document) { _, _, _ ->
            entered.countDown()
            check(release.await(10, TimeUnit.SECONDS))
            BufferedImage(640, 840, BufferedImage.TYPE_INT_RGB)
        }
        val render = async(Dispatchers.IO) { runCatching { session.renderPage(0, 640) } }
        val closer = Executors.newSingleThreadExecutor()
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            closer.submit { session.close() }.get(2, TimeUnit.SECONDS)
        } finally {
            release.countDown()
            render.await()
            session.close()
            closer.shutdownNow()
        }
    }

    @Test
    fun injectedOfficialPdfBytesOpenAndRenderOnePage() = runBlocking {
        val resource = publishedPdfResource()
        val bytes = createOnePagePdf()
        var reads = 0
        val reader = DesktopPdfDocumentReader {
            reads++
            bytes
        }
        val session = reader.open(resource)
        try {
            assertEquals(1, reads)
            assertEquals(1, session.pageCount)
            val image = session.renderPage(0, targetWidthPx = 1000)
            assertTrue(image.width > 300)
            assertTrue(image.height > 300)
        } finally {
            session.close()
        }
    }

    @Test
    fun rejectsUncataloguedUrlBeforeCallingTheLoader() = runBlocking {
        val official = publishedPdfResource()
        val forged = official.copy(url = "https://example.invalid/private.pdf")
        var reads = 0
        val reader = DesktopPdfDocumentReader {
            reads++
            createOnePagePdf()
        }
        val failure = runCatching { reader.open(forged) }.exceptionOrNull()
        assertTrue(failure is PdfReaderException)
        assertEquals(0, reads)
    }

    @Test
    fun refusesNonPdfBytesAndDocumentsOverTheDownloadCap() {
        assertFailsWith<PdfReaderException> { validatePdfBytes("not a PDF".encodeToByteArray()) }
        val tooLarge = ByteArray(50 * 1024 * 1024 + 1)
        tooLarge[0] = '%'.code.toByte()
        assertFailsWith<PdfReaderException> { validatePdfBytes(tooLarge) }
    }

    @Test
    fun embeddedPdfReaderPageIsCapturedAsAComposeImage() = runComposeUiTest(testTimeout = 60.seconds) {
        val resource = publishedPdfResource()
        val bytes = createOnePagePdf(pageCount = 2)
        var requests = 0
        val reader = DesktopPdfDocumentReader {
            requests++
            if (requests == 1) throw IOException("Fixture network unavailable")
            bytes
        }
        setContent {
            LearningAppTheme {
                Surface {
                    PdfReaderProvider(reader, scrollbar = { state, modifier ->
                        androidx.compose.foundation.VerticalScrollbar(androidx.compose.foundation.rememberScrollbarAdapter(state), modifier)
                    }) { PdfDocumentContent(resource, reader) {} }
                }
            }
        }

        onNodeWithText(resource.title).assertExists()
        waitUntil(timeoutMillis = 10000) { onAllNodesWithText("Fixture network unavailable").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Réessayer").performClick()
        waitUntil(timeoutMillis = 10000) { onAllNodesWithContentDescription("Page 1 du PDF ${resource.title}").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Page 1 / 2").assertExists()
        onNodeWithContentDescription("Page 1 du PDF ${resource.title}").assertIsDisplayed()
        assertEquals(2, requests)
        mainClock.advanceTimeByFrame()
        waitForIdle()
        val frameBitmap = onRoot().captureToImage().asSkiaBitmap()
        val frameImage = Image.makeFromBitmap(frameBitmap)
        try {
            val frameData = checkNotNull(frameImage.encodeToData())
            try {
                val frameFile = File("build/reports/ui-snapshots/pdf-reader-controls.png")
                check(frameFile.parentFile.isDirectory || frameFile.parentFile.mkdirs())
                frameFile.writeBytes(frameData.bytes)
            } finally { frameData.close() }
        } finally { frameImage.close(); frameBitmap.close() }
        val snapshot = onNodeWithContentDescription("Page 1 du PDF ${resource.title}").captureToImage().asSkiaBitmap()
        assertTrue(snapshot.width > 100 && snapshot.height > 100)
        val output = File("build/reports/ui-snapshots/pdf-reader-page.png")
        check(output.parentFile.isDirectory || output.parentFile.mkdirs())
        val skiaImage = Image.makeFromBitmap(snapshot)
        try {
            val encoded = checkNotNull(skiaImage.encodeToData()) { "PNG encoding failed" }
            try {
                output.writeBytes(encoded.bytes)
            } finally {
                encoded.close()
            }
        } finally {
            skiaImage.close()
            snapshot.close()
        }
        onNodeWithTag("pdf-pages").performScrollToIndex(1)
        waitUntil(timeoutMillis = 10000) { onAllNodesWithContentDescription("Page 2 du PDF ${resource.title}").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Page 2 / 2").assertExists()
        onNodeWithContentDescription("Augmenter le zoom").performClick()
        onNodeWithText("125 %").assertExists()
        onNodeWithTag("pdf-pages").performScrollToIndex(0)
        waitUntil(timeoutMillis = 10000) { onAllNodesWithContentDescription("Page 1 du PDF ${resource.title}").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Page 1 / 2").assertExists()
    }

    @Test
    fun retryReopensAndClosesThePreviousDocumentSession() = runComposeUiTest(testTimeout = 60.seconds) {
        val resource = publishedPdfResource()
        var opens = 0
        var closes = 0
        var renders = 0
        val reader = object : PdfDocumentReader {
            override suspend fun open(resource: CourseResource): PdfDocumentSession {
                opens++
                val thisSession = opens
                return object : PdfDocumentSession {
                    override val pageCount = 1
                    override suspend fun renderPage(pageIndex: Int, targetWidthPx: Int): androidx.compose.ui.graphics.ImageBitmap {
                        if (thisSession == 1 && renders++ == 0) throw IOException("First render fails")
                        return androidx.compose.ui.graphics.ImageBitmap(640, 840)
                    }

                    override fun close() { closes++ }
                }
            }
        }
        var visible by mutableStateOf(true)
        setContent {
            LearningAppTheme {
                Surface {
                    PdfReaderProvider(reader, scrollbar = { state, modifier ->
                        androidx.compose.foundation.VerticalScrollbar(androidx.compose.foundation.rememberScrollbarAdapter(state), modifier)
                    }) {
                        if (visible) PdfDocumentContent(resource, reader) { visible = false }
                    }
                }
            }
        }

        waitUntil(timeoutMillis = 10000) { onAllNodesWithText("Cette page ne peut pas être affichée. Réessaie ou ouvre le PDF dans le navigateur.").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Réessayer").performClick()
        waitUntil(timeoutMillis = 10000) { onAllNodesWithContentDescription("Page 1 du PDF ${resource.title}").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(2, opens)
        assertEquals(1, closes)
        runOnIdle { visible = false }
        waitForIdle()
        assertEquals(2, closes)
    }

    @OptIn(androidx.compose.ui.InternalComposeUiApi::class)
    @Test
    fun continuousScrollingReachesLaterPagesWithoutRenderingTheWholeDocument() = runComposeUiTest(testTimeout = 60.seconds) {
        val resource = publishedPdfResource()
        val rendered = mutableSetOf<Int>()
        val reader = object : PdfDocumentReader {
            override suspend fun open(resource: CourseResource) = object : PdfDocumentSession {
                override val pageCount = 200
                override suspend fun renderPage(pageIndex: Int, targetWidthPx: Int): androidx.compose.ui.graphics.ImageBitmap {
                    rendered += pageIndex
                    return androidx.compose.ui.graphics.ImageBitmap(600, 780)
                }
                override fun close() = Unit
            }
        }
        setContent { LearningAppTheme { Surface { PdfDocumentContent(resource, reader) {} } } }
        waitUntil(timeoutMillis = 10000) { onAllNodesWithContentDescription("Page 1 du PDF ${resource.title}").fetchSemanticsNodes().isNotEmpty() }
        runOnIdle { assertTrue(rendered.size < 10, "Only viewport pages may be rasterized: $rendered") }
        onNodeWithTag("pdf-pages").performScrollToIndex(99)
        waitUntil(timeoutMillis = 10000) { onAllNodesWithContentDescription("Page 100 du PDF ${resource.title}").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithContentDescription("Page 100 du PDF ${resource.title}").assertIsDisplayed()
        onNodeWithTag("pdf-pages").performTouchInput { swipeUp() }
        waitUntil(timeoutMillis = 10000) { rendered.any { it > 99 } }
        runOnIdle { assertTrue(rendered.size < 15, "Scrolling must not eagerly render 200 pages: $rendered") }
        val pages = onNodeWithTag("pdf-pages")
        pages.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        pages.performKeyPress(KeyEvent(Key.MoveEnd, KeyEventType.KeyDown))
        waitUntil(timeoutMillis = 10000) { onAllNodesWithContentDescription("Page 200 du PDF ${resource.title}").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithContentDescription("Page 200 du PDF ${resource.title}").assertIsDisplayed()
        pages.performKeyPress(KeyEvent(Key.MoveHome, KeyEventType.KeyDown))
        waitUntil(timeoutMillis = 10000) { onAllNodesWithContentDescription("Page 1 du PDF ${resource.title}").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithContentDescription("Page 1 du PDF ${resource.title}").assertIsDisplayed()
    }

    @Test
    fun pageGeometryIncludesPageSizeAndRotationBeforeRasterizing() {
        val document = PDDocument().apply {
            addPage(PDPage(PDRectangle.LETTER))
            addPage(PDPage(PDRectangle(800f, 400f)))
            addPage(PDPage(PDRectangle(400f, 800f)).apply { rotation = 90 })
        }
        val session = PdfBoxDocumentSession(document)
        try {
            assertEquals(612f / 792f, session.pageAspectRatio(0), 0.0001f)
            assertEquals(2f, session.pageAspectRatio(1), 0.0001f)
            assertEquals(2f, session.pageAspectRatio(2), 0.0001f)
        } finally { session.close() }
    }

    @Test
    fun continuousPdfPagesShareTheViewportAtWideAndNarrowWidths() {
        listOf(1100, 500).forEach { width ->
            runDesktopComposeUiTest(width = width, height = 900, testTimeout = 60.seconds) {
                val resource = publishedPdfResource()
                val reader = DesktopPdfDocumentReader { createOnePagePdf(2) }
                setContent { LearningAppTheme { Surface {
                    PdfReaderProvider(reader, scrollbar = { state, modifier ->
                        androidx.compose.foundation.VerticalScrollbar(androidx.compose.foundation.rememberScrollbarAdapter(state), modifier)
                    }) { PdfDocumentContent(resource, reader) {} }
                } } }
                waitUntil(timeoutMillis = 10000) { onAllNodesWithContentDescription("Page 1 du PDF ${resource.title}").fetchSemanticsNodes().isNotEmpty() }
                onNodeWithTag("pdf-pages").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, (width - 48) * 792f / 612f - 280f) }
                waitUntil(timeoutMillis = 10000) { onAllNodesWithContentDescription("Page 2 du PDF ${resource.title}").fetchSemanticsNodes().isNotEmpty() }
                onNodeWithContentDescription("Page 1 du PDF ${resource.title}").assertIsDisplayed()
                onNodeWithContentDescription("Page 2 du PDF ${resource.title}").assertIsDisplayed()
                val output = File("build/reports/ui-snapshots/pdf-continuous-$width.png")
                output.parentFile.mkdirs()
                Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image ->
                    image.encodeToData()!!.use { output.writeBytes(it.bytes) }
                }
            }
        }
    }

    private fun publishedPdfResource(): CourseResource =
        CourseResources.forCourse("forallx_calgary_2025").single { it.kind == "textbook_pdf" }

    private fun createOnePagePdf(pageCount: Int = 1): ByteArray = ByteArrayOutputStream().use { output ->
        PDDocument().use { document ->
            // Embedded fixture font: never enumerate the runner's installed Windows fonts.
            val font = checkNotNull(PDDocument::class.java.getResourceAsStream("/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf"))
                .use { PDType0Font.load(document, it) }
            repeat(pageCount) { pageIndex ->
                val page = PDPage(PDRectangle.LETTER)
                document.addPage(page)
                PDPageContentStream(document, page).use { content ->
                    content.beginText()
                    content.setFont(font, 22f)
                    content.newLineAtOffset(72f, 700f)
                    content.showText("Original PDF page ${pageIndex + 1}")
                    content.endText()
                }
            }
            document.save(output)
        }
        output.toByteArray()
    }
}
