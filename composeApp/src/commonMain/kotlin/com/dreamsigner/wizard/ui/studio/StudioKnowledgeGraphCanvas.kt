package com.dreamsigner.wizard.ui.studio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min

/** Bounded pages keep a large saved graph usable without composing thousands of nodes. */
@Composable
internal fun StudioKnowledgeGraphCanvas(graph: KnowledgeGraph, sources: (List<KnowledgeSource>) -> String) {
    var page by remember { mutableStateOf(0) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var compact by remember { mutableStateOf(false) }
    val sorted = remember(graph.propositions) { graph.propositions.sortedBy { it.id } }
    val pageCount = ((sorted.size + 23) / 24).coerceAtLeast(1)
    val safePage = page.coerceIn(0, pageCount - 1)
    val visible = sorted.drop(safePage * 24).take(24)
    val selected = graph.propositions.firstOrNull { it.id == selectedId }
    Text("Carte des connaissances", style = MaterialTheme.typography.subtitle1)
    if (sorted.isEmpty()) {
        Text("La carte est vide. Extrais des connaissances ou importe des notes, puis vérifie et accepte la proposition.", modifier = Modifier.testTag("studio-graph-canvas-empty"))
        return
    }
    Text("Sélectionne une proposition pour voir son énoncé et ses sources. Les flèches vont des prémisses vers la conclusion.", style = MaterialTheme.typography.caption)
    if (pageCount > 1) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { page = safePage - 1 }, enabled = safePage > 0) { Text("Précédent") }
            Text("Page ${safePage + 1} / $pageCount", Modifier.padding(top = 14.dp))
            TextButton(onClick = { page = safePage + 1 }, enabled = safePage + 1 < pageCount) { Text("Suivant") }
        }
        Text("La carte affiche 24 propositions par page ; seuls les liens entre les propositions de cette page sont visibles.", style = MaterialTheme.typography.caption)
    }
    val lineColor = MaterialTheme.colors.primary.copy(alpha = 0.65f)
    val background = MaterialTheme.colors.onSurface.copy(alpha = 0.035f)
    val positions = remember(visible) { visible.mapIndexed { index, p -> p.id to index }.toMap() }
    val rows = ceil(visible.size / 3.0).toInt().coerceAtLeast(1)
    val scale = if (compact) 0.7f else 1f
    TextButton(onClick = { compact = !compact }, modifier = Modifier.testTag("studio-graph-fit")) {
        Text(if (compact) "Taille normale" else "Vue compacte")
    }
    Box(Modifier.fillMaxWidth().height(minOf(260, rows * 110 + 16).dp).background(background).testTag("studio-graph-canvas")
        .horizontalScroll(rememberScrollState()).verticalScroll(rememberScrollState())) {
        Box(Modifier.width((576 * scale).dp).height((rows * 110 * scale).dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val density = density * scale
                fun center(index: Int) = Offset((index % 3 * 192 + 88) * density, (index / 3 * 110 + 44) * density)
                graph.reasonings.forEach { reasoning ->
                    val end = positions[reasoning.conclusion]?.let(::center) ?: return@forEach
                    reasoning.premises.forEach { premise ->
                        val start = positions[premise]?.let(::center) ?: return@forEach
                        val delta = end - start
                        val length = delta.getDistance()
                        if (length > 1f) {
                            val unit = delta / length
                            // Intersect the line with the card rectangle, leaving the arrow outside it.
                            val horizontal = if (abs(unit.x) < 0.001f) Float.POSITIVE_INFINITY else 88 * density / abs(unit.x)
                            val vertical = if (abs(unit.y) < 0.001f) Float.POSITIVE_INFINITY else 44 * density / abs(unit.y)
                            val inset = min(horizontal, vertical)
                            val target = end - unit * inset
                            val origin = start + unit * inset
                            val side = Offset(-unit.y, unit.x)
                            drawLine(lineColor, origin, target, 2 * density)
                            drawLine(lineColor, target, target - unit * (9 * density) + side * (5 * density), 2 * density)
                            drawLine(lineColor, target, target - unit * (9 * density) - side * (5 * density), 2 * density)
                        }
                    }
                }
            }
            visible.forEachIndexed { index, item ->
                OutlinedButton(onClick = { selectedId = item.id },
                    modifier = Modifier.offset(x = (index % 3 * 192 * scale).dp, y = (index / 3 * 110 * scale).dp)
                        .size(width = (176 * scale).dp, height = (88 * scale).dp).testTag("studio-graph-node-${item.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(backgroundColor = if (selectedId == item.id) MaterialTheme.colors.primary.copy(alpha = 0.14f) else MaterialTheme.colors.surface),
                    contentPadding = PaddingValues(8.dp)) {
                    Text("${safePage * 24 + index + 1} · ${item.statement}", maxLines = 3, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onSurface)
                }
            }
        }
    }
    Text("Fais défiler la carte horizontalement et verticalement pour explorer les liens.", style = MaterialTheme.typography.caption)
    selected?.let {
        Column(Modifier.fillMaxWidth().testTag("studio-graph-selection")) {
            Text("Proposition sélectionnée", style = MaterialTheme.typography.subtitle2)
            Text(it.statement)
            Text("Sources : ${sources(it.sources)}", style = MaterialTheme.typography.caption)
        }
    }
}
