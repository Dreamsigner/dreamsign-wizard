package com.dreamsigner.wizard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamsigner.wizard.data.CourseResource
import com.dreamsigner.wizard.ui.pdf.PdfDocumentDialog

@Composable
internal fun ReferenceLinks(resources: List<CourseResource>, showVideoPreviews: Boolean = false) {
    val uri = LocalUriHandler.current
    val videoPlayer = LocalVideoPlayer.current
    val videoDownloader = LocalVideoDownloader.current
    var openedVideo by remember(resources) { mutableStateOf<CourseResource?>(null) }
    var downloadingVideo by remember(resources) { mutableStateOf<CourseResource?>(null) }
    var failure by remember(resources) { mutableStateOf<String?>(null) }
    var openedPdf by remember(resources) { mutableStateOf<CourseResource?>(null) }
    resources.distinctBy { it.url }
        .sortedBy { if (it.kind.contains("pdf")) 0 else if (it.kind == "video") 1 else 2 }.forEach { resource ->
        if (showVideoPreviews && resource.kind == "video") {
            VideoPreview(resource, onPlay = if (videoPlayer != null) ({ openedVideo = resource }) else null) {
                failure = null
                try { uri.openUri(resource.url) } catch (_: Exception) {
                    failure = "Impossible d’ouvrir ce support. Vérifie la connexion ou le navigateur par défaut."
                }
            }
            if (videoDownloader != null && youtubeVideoId(resource.url) != null) {
                TextButton({ downloadingVideo = resource }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Télécharger avec la piste audio française", color = ActionRed)
                }
            }
        } else TextButton({
            failure = null
            if (resource.kind.contains("pdf")) openedPdf = resource
            else try { uri.openUri(resource.url) } catch (_: Exception) { failure = "Impossible d’ouvrir ce support. Vérifie la connexion ou le navigateur par défaut." }
        }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val format = when {
                    resource.kind == "textbook_pdf" -> "Manuel PDF complet"
                    resource.coverage == "partial_textbook" -> "Notes PDF du cours · couverture partielle"
                    resource.kind.contains("pdf") -> "Support PDF de séance"
                    resource.kind == "video" && resource.url.startsWith("https://www.youtube.com/watch?v=") -> "Vidéo · YouTube"
                    resource.kind == "video" -> "Vidéo · fichier MP4"
                    resource.kind == "textbook_html" -> "Manuel en ligne"
                    resource.kind == "resource_collection" -> "Collection de supports"
                    else -> "Référence du cours"
                }
                Text(format, style = TextStyle(color = ActionRed, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp))
                Text(resource.title.substringBeforeLast(" — ", resource.title), style = TextStyle(color = Ink, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 23.sp, letterSpacing = 0.sp))
                Text("${if (resource.language == "fr") "Français" else "Anglais"} · Disponible online", style = TextStyle(color = Graphite, fontSize = 13.sp, lineHeight = 20.sp, letterSpacing = 0.sp))
                if (resource.notes.isNotBlank()) Text(resource.notes, color = Graphite)
            }
        }
    }
    failure?.let { Text(it, color = ActionRed) }
    openedPdf?.let { PdfDocumentDialog(it, onClose = { openedPdf = null }) }
    openedVideo?.let { videoPlayer?.Player(it, onClose = { openedVideo = null }) }
    downloadingVideo?.let { videoDownloader?.Download(it, onClose = { downloadingVideo = null }) }
}
