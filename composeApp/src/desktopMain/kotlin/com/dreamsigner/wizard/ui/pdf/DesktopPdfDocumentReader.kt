package com.dreamsigner.wizard.ui.pdf

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.dreamsigner.wizard.data.CourseResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.jetbrains.skia.Image as SkiaImage
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.imageio.ImageIO
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** Desktop PDFBox implementation; it keeps PDF bytes and rendered pages in memory only. */
class DesktopPdfDocumentReader(
    private val loadBytes: suspend (CourseResource) -> ByteArray = ::downloadOfficialPdf,
) : PdfDocumentReader {
    override suspend fun open(resource: CourseResource): PdfDocumentSession {
        if (!isApprovedPdfResource(resource)) {
            throw PdfReaderException("Ce PDF ne fait pas partie des supports officiels du cours.")
        }
        val bytes = loadBytes(resource)
        validatePdfBytes(bytes)
        val createdSession = AtomicReference<PdfBoxDocumentSession?>(null)
        try {
            val openedSession = withContext(Dispatchers.IO) {
                val document = Loader.loadPDF(bytes)
                try {
                    if (document.numberOfPages !in 1..MAX_PDF_PAGES) {
                        throw PdfReaderException("Ce document dépasse la limite de pages prise en charge.")
                    }
                    PdfBoxDocumentSession(document).also(createdSession::set)
                } catch (failure: Throwable) {
                    document.close()
                    throw failure
                }
            }
            currentCoroutineContext().ensureActive()
            return openedSession
        } catch (failure: Throwable) {
            withContext(NonCancellable + Dispatchers.IO) { runCatching { createdSession.getAndSet(null)?.close() } }
            when (failure) {
                is kotlinx.coroutines.CancellationException -> throw failure
                is PdfReaderException -> throw failure
                is Exception -> throw PdfReaderException("Le document téléchargé n’est pas un PDF lisible.", failure)
                else -> throw failure
            }
        }
    }
}

internal fun validatePdfBytes(bytes: ByteArray) {
    if (bytes.isEmpty()) throw PdfReaderException("Le serveur a renvoyé un document vide.")
    if (bytes.size > MAX_PDF_BYTES) throw PdfReaderException("Le PDF dépasse la limite de 50 Mo.")
    val headerWindow = bytes.copyOfRange(0, minOf(bytes.size, PDF_HEADER_SCAN_BYTES))
    val header = headerWindow.toString(Charsets.ISO_8859_1)
    if (!header.contains("%PDF-")) throw PdfReaderException("Le serveur n’a pas renvoyé un fichier PDF valide.")
}

internal class PdfBoxDocumentSession(
    private val document: PDDocument,
    private val renderImage: (PDDocument, Int, Float) -> BufferedImage = { source, index, dpi ->
        PDFRenderer(source).apply { isSubsamplingAllowed = true }.renderImageWithDPI(index, dpi)
    },
) : PdfDocumentSession {
    private val closed = AtomicBoolean(false)
    private val disposed = AtomicBoolean(false)
    private val renderLock = ReentrantLock()
    override val pageCount: Int = document.numberOfPages
    private val aspectRatios = FloatArray(pageCount) { index ->
        val page = document.getPage(index)
        val box = page.cropBox
        val ratio = if (page.rotation % 180 != 0) box.height / box.width else box.width / box.height
        if (ratio.isFinite() && ratio > 0f) ratio else 1f / 1.41421356f
    }
    override fun pageAspectRatio(pageIndex: Int): Float = aspectRatios[pageIndex]

    override suspend fun renderPage(pageIndex: Int, targetWidthPx: Int): ImageBitmap {
        if (closed.get()) throw PdfReaderException("Le document PDF est fermé.")
        if (pageIndex !in 0 until pageCount) throw PdfReaderException("Cette page n’existe pas.")
        return withContext(Dispatchers.IO) {
            val context = currentCoroutineContext()
            renderLock.withLock {
                try {
                    context.ensureActive()
                    if (closed.get()) throw PdfReaderException("Le document PDF est fermé.")
                    val page = document.getPage(pageIndex)
                    val mediaBox = page.cropBox
                    val unit = page.userUnit.toDouble()
                    if (!unit.isFinite() || unit !in MIN_USER_UNIT..MAX_USER_UNIT) {
                        throw PdfReaderException("L’échelle de cette page dépasse la limite de rendu.")
                    }
                    val rawWidthPoints = mediaBox.width.toDouble() * unit
                    val rawHeightPoints = mediaBox.height.toDouble() * unit
                    val quarterTurned = page.rotation % 180 != 0
                    val widthPoints = if (quarterTurned) rawHeightPoints else rawWidthPoints
                    val heightPoints = if (quarterTurned) rawWidthPoints else rawHeightPoints
                    if (!widthPoints.isFinite() || !heightPoints.isFinite() ||
                        widthPoints !in 1.0..MAX_PAGE_POINTS || heightPoints !in 1.0..MAX_PAGE_POINTS
                    ) {
                        throw PdfReaderException("Les dimensions de cette page dépassent la limite de rendu.")
                    }
                    val desiredWidth = targetWidthPx.coerceIn(320, MAX_RENDER_WIDTH_PX)
                    val pagePixelLimitWidth = kotlin.math.sqrt(MAX_RENDER_PIXELS * widthPoints / heightPoints).toInt().coerceAtLeast(1)
                    val pixelWidth = minOf(desiredWidth, pagePixelLimitWidth)
                    val dpi = (pixelWidth * 72.0 / widthPoints).toFloat().coerceAtMost(240f)
                    val pageImage = renderImage(document, pageIndex, dpi)
                    try {
                        ByteArrayOutputStream().use { output ->
                            ImageIO.write(pageImage, "png", output)
                            SkiaImage.makeFromEncoded(output.toByteArray()).toComposeImageBitmap()
                        }
                    } finally { pageImage.flush() }
                } finally {
                    if (closed.get()) disposeDocument()
                }
            }
        }
    }

    /** Closing the dialog never waits on a slow PDF renderer or font discovery. */
    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        if (renderLock.tryLock()) {
            try { disposeDocument() } finally { renderLock.unlock() }
        }
        // The active render owns disposal in its finally block when it holds the lock.
    }

    private fun disposeDocument() {
        if (disposed.compareAndSet(false, true)) runCatching { document.close() }
    }
}

private suspend fun downloadOfficialPdf(resource: CourseResource): ByteArray = withContext(Dispatchers.IO) {
    var current = URI(resource.url).toURL()
    val originalHost = current.host.lowercase()
    val originalPort = current.port
    if (current.userInfo != null) throw PdfReaderException("Adresse PDF non autorisée.")
    val deadlineNanos = System.nanoTime() + TIMEOUT_NANOS
    repeat(MAX_REDIRECTS + 1) { redirectIndex ->
        if (!current.protocol.equals("https", ignoreCase = true) || current.host.lowercase() != originalHost ||
            current.port != originalPort || current.userInfo != null
        ) {
            throw PdfReaderException("Le PDF redirige vers une adresse non autorisée.")
        }
        val connection = (current.openConnection() as? HttpURLConnection)
            ?: throw PdfReaderException("Le serveur PDF n’utilise pas HTTPS.")
        try {
            val remainingMillis = ((deadlineNanos - System.nanoTime()) / NANOS_PER_MILLISECOND).coerceAtLeast(1).toInt()
            connection.instanceFollowRedirects = false
            connection.connectTimeout = minOf(TIMEOUT_MILLIS, remainingMillis)
            connection.readTimeout = minOf(TIMEOUT_MILLIS, remainingMillis)
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/pdf, application/octet-stream;q=0.9")
            connection.setRequestProperty("User-Agent", "DreamsignWizardDesktop/1.0")
            val status = connection.responseCode
            if (status in 300..399) {
                if (redirectIndex == MAX_REDIRECTS) throw PdfReaderException("Le serveur a redirigé trop souvent.")
                val location = connection.getHeaderField("Location")
                    ?: throw PdfReaderException("Le serveur PDF a renvoyé une redirection invalide.")
                current = current.toURI().resolve(location).toURL()
                return@repeat
            }
            if (status !in 200..299) throw PdfReaderException("Téléchargement PDF impossible (HTTP $status).")
            val contentType = connection.contentType?.substringBefore(';')?.trim()?.lowercase()
            if (contentType != null && contentType !in setOf("application/pdf", "application/octet-stream")) {
                throw PdfReaderException("Le serveur n’a pas renvoyé un fichier PDF.")
            }
            if (connection.contentLengthLong > MAX_PDF_BYTES) throw PdfReaderException("Le PDF dépasse la limite de 50 Mo.")
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input -> copyLimited(input, output, deadlineNanos) }
            return@withContext output.toByteArray()
        } finally {
            connection.disconnect()
        }
    }
    throw PdfReaderException("Téléchargement PDF impossible.")
}

private fun copyLimited(input: InputStream, output: ByteArrayOutputStream, deadlineNanos: Long) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        if (System.nanoTime() >= deadlineNanos) throw PdfReaderException("Le délai de téléchargement du PDF a expiré.")
        val read = input.read(buffer)
        if (read < 0) break
        total += read
        if (total > MAX_PDF_BYTES) throw PdfReaderException("Le PDF dépasse la limite de 50 Mo.")
        output.write(buffer, 0, read)
    }
}

private const val MAX_PDF_BYTES = 50L * 1024 * 1024
private const val PDF_HEADER_SCAN_BYTES = 1024
private const val MAX_PDF_PAGES = 2000
private const val MAX_PAGE_POINTS = 20_000.0
private const val MIN_USER_UNIT = 0.01
private const val MAX_USER_UNIT = 75.0
private const val MAX_RENDER_PIXELS = 8_000_000.0
private const val MAX_RENDER_WIDTH_PX = 3200
private const val MAX_REDIRECTS = 4
private const val TIMEOUT_MILLIS = 30_000
private const val TIMEOUT_NANOS = 30_000_000_000L
private const val NANOS_PER_MILLISECOND = 1_000_000L
