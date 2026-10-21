package com.duolingo.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.duolingo.app.data.CourseResource
import com.duolingo.app.ui.pdf.PdfDocumentDialog

@Composable
internal fun ReferenceLinks(resources: List<CourseResource>) {
    val uri = LocalUriHandler.current
    var failure by remember(resources) { mutableStateOf<String?>(null) }
    var openedPdf by remember(resources) { mutableStateOf<CourseResource?>(null) }
    resources.distinctBy { it.url }.sortedBy { if (it.kind.contains("pdf")) 0 else 1 }.forEach { resource ->
        TextButton({
            failure = null
            if (resource.kind.contains("pdf")) openedPdf = resource
            else try { uri.openUri(resource.url) } catch (_: Exception) { failure = "Impossible d’ouvrir ce support. Vérifie la connexion ou le navigateur par défaut." }
        }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val format = when {
                    resource.kind == "textbook_pdf" -> "Manuel PDF complet"
                    resource.coverage == "partial_textbook" -> "Notes PDF du cours · couverture partielle"
                    resource.kind.contains("pdf") -> "Support PDF de séance"
                    resource.kind == "video" -> "Vidéo"
                    resource.kind == "textbook_html" -> "Manuel en ligne"
                    resource.kind == "resource_collection" -> "Collection de supports"
                    else -> "Référence du cours"
                }
                Text("$format · ${resource.title}", fontWeight = FontWeight.Medium)
                Text("${if (resource.language == "fr") "Français" else "Anglais"} · connexion Internet", color = Graphite)
                if (resource.notes.isNotBlank()) Text(resource.notes, color = Graphite)
            }
        }
    }
    failure?.let { Text(it, color = ActionRed) }
    openedPdf?.let { PdfDocumentDialog(it, onClose = { openedPdf = null }) }
}
