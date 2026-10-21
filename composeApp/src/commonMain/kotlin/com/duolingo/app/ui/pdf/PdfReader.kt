package com.duolingo.app.ui.pdf

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.duolingo.app.data.CourseResource
import com.duolingo.app.data.CourseResources
import com.duolingo.app.ui.Paper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** A platform reader owns one in-memory document and renders only the requested page. */
interface PdfDocumentReader {
    suspend fun open(resource: CourseResource): PdfDocumentSession
}

interface PdfDocumentSession {
    val pageCount: Int
    suspend fun renderPage(pageIndex: Int, targetWidthPx: Int): ImageBitmap
    fun close()
}

class PdfReaderException(message: String, cause: Throwable? = null) : Exception(message, cause)

private val fallbackReader = object : PdfDocumentReader {
    override suspend fun open(resource: CourseResource): PdfDocumentSession =
        throw PdfReaderException("La lecture PDF intégrée n’est pas disponible sur cette plateforme.")
}

private val LocalPdfDocumentReader = staticCompositionLocalOf<PdfDocumentReader> { fallbackReader }

/** Install a platform PDF implementation around the app subtree. */
@Composable
fun PdfReaderProvider(reader: PdfDocumentReader, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPdfDocumentReader provides reader, content = content)
}

/**
 * Built-in reader for catalogued, official PDF resources. Unknown URLs and non-PDF catalog
 * entries are rejected before the platform reader can perform network I/O.
 */
@Composable
fun PdfDocumentDialog(resource: CourseResource, onClose: () -> Unit) {
    val reader = LocalPdfDocumentReader.current
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.92f)
                .widthIn(max = 1200.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colors.surface,
            elevation = 12.dp,
        ) {
            PdfDocumentContent(resource, reader, onClose)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun PdfDocumentContent(resource: CourseResource, reader: PdfDocumentReader, onClose: () -> Unit) {
    var retryCount by remember(resource.id) { mutableIntStateOf(0) }
    var session by remember(resource.id) { mutableStateOf<PdfDocumentSession?>(null) }
    var error by remember(resource.id) { mutableStateOf<String?>(null) }
    var opening by remember(resource.id) { mutableStateOf(true) }
    var pageIndex by remember(resource.id) { mutableIntStateOf(0) }
    var zoom by remember(resource.id) { mutableStateOf(1f) }
    var pageBitmap by remember(resource.id) { mutableStateOf<ImageBitmap?>(null) }
    var pageLoading by remember(resource.id) { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(resource.id, resource.url, retryCount, reader) {
        session = null
        error = null
        opening = true
        pageIndex = 0
        pageBitmap = null
        try {
            if (!isApprovedPdfResource(resource)) {
                throw PdfReaderException("Ce PDF ne fait pas partie des supports officiels du cours.")
            }
            val opened = reader.open(resource)
            try {
                currentCoroutineContext().ensureActive()
                if (opened.pageCount !in 1..MAX_PDF_PAGE_COUNT) {
                    throw PdfReaderException("Ce document dépasse la limite de pages prise en charge.")
                }
                session = opened
            } catch (failure: Throwable) {
                runCatching { opened.close() }
                throw failure
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = failure.message ?: "Impossible de lire ce document PDF."
        } finally {
            opening = false
        }
    }

    val sessionToClose = session
    DisposableEffect(sessionToClose) {
        onDispose { runCatching { sessionToClose?.close() } }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(resource.title, style = MaterialTheme.typography.h6, fontWeight = FontWeight.SemiBold)
                Text("Lecture du PDF original · connexion Internet", style = MaterialTheme.typography.caption)
            }
            TextButton(onClick = onClose, modifier = Modifier.semantics { contentDescription = "Fermer le lecteur PDF" }) {
                Text("Fermer")
            }
        }

        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            TextButton(
                onClick = { pageIndex--; pageBitmap = null },
                enabled = session != null && pageIndex > 0,
                modifier = Modifier.semantics { contentDescription = "Page précédente" },
            ) {
                Text("‹ Précédente")
            }
            Text(
                if (session == null) "Page — / —" else "Page ${pageIndex + 1} / ${session!!.pageCount}",
                modifier = Modifier.semantics { contentDescription = "Page ${pageIndex + 1} sur ${session?.pageCount ?: 0}" },
                style = MaterialTheme.typography.subtitle2,
            )
            TextButton(
                onClick = { pageIndex++; pageBitmap = null },
                enabled = session != null && pageIndex < session!!.pageCount - 1,
                modifier = Modifier.semantics { contentDescription = "Page suivante" },
            ) {
                Text("Suivante ›")
            }
            TextButton(
                onClick = { zoom = (zoom - ZOOM_STEP).coerceAtLeast(MIN_ZOOM); pageBitmap = null },
                enabled = zoom > MIN_ZOOM,
                modifier = Modifier.semantics { contentDescription = "Réduire le zoom" },
            ) {
                Text("−")
            }
            Text("${(zoom * 100).toInt()} %")
            TextButton(
                onClick = { zoom = (zoom + ZOOM_STEP).coerceAtMost(MAX_ZOOM); pageBitmap = null },
                enabled = zoom < MAX_ZOOM,
                modifier = Modifier.semantics { contentDescription = "Augmenter le zoom" },
            ) {
                Text("+")
            }
        }

        error?.let { message ->
            Column(
                Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(message, color = MaterialTheme.colors.error)
                Spacer(Modifier.size(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { retryCount++ }) { Text("Réessayer") }
                    TextButton(onClick = { runCatching { uriHandler.openUri(resource.url) } }) {
                        Text("Ouvrir dans le navigateur")
                    }
                }
            }
        } ?: BoxWithConstraints(
            Modifier.fillMaxWidth().weight(1f).background(Paper),
            contentAlignment = Alignment.TopCenter,
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { (maxWidth * zoom).roundToPx().coerceIn(320, MAX_RENDER_WIDTH_PX) }
            val page = session
            LaunchedEffect(page, pageIndex, widthPx) {
                pageBitmap = null
                if (page != null) {
                    pageLoading = true
                    try {
                        pageBitmap = page.renderPage(pageIndex, widthPx)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        error = "Cette page ne peut pas être affichée. Réessaie ou ouvre le PDF dans le navigateur."
                    } finally {
                        pageLoading = false
                    }
                }
            }
            val bitmap = pageBitmap
            if (opening || pageLoading || (page != null && bitmap == null)) {
                CircularProgressIndicator(Modifier.align(Alignment.Center).size(34.dp))
            }
            if (bitmap != null) {
                val imageWidth = maxWidth * zoom
                Column(
                    Modifier.fillMaxSize().horizontalScroll(rememberScrollState()).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Page ${pageIndex + 1} du PDF ${resource.title}",
                        modifier = Modifier.width(imageWidth).aspectRatio(bitmap.width.toFloat() / bitmap.height),
                    )
                }
            }
        }
    }
}

private const val MAX_PDF_PAGE_COUNT = 2000
private const val MAX_RENDER_WIDTH_PX = 3200
private const val MIN_ZOOM = 0.75f
private const val MAX_ZOOM = 2f
private const val ZOOM_STEP = 0.25f

/** Match the actual catalog entry so callers cannot use this reader as an arbitrary URL fetcher. */
fun isApprovedPdfResource(resource: CourseResource): Boolean {
    if (!resource.kind.contains("pdf", ignoreCase = true)) return false
    if (!resource.url.startsWith("https://", ignoreCase = true)) return false
    val officialResources = CourseResources.byCourseId.values.flatten() + CourseResources.bySession.values.flatten()
    return officialResources.any { candidate ->
        candidate.id == resource.id && candidate.url == resource.url && candidate.kind == resource.kind
    }
}
