package com.duolingo.app.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.duolingo.app.model.Exercise
import com.duolingo.app.model.Lesson
import com.duolingo.app.ui.BrandRed
import com.duolingo.app.ui.Graphite
import com.duolingo.app.ui.Ink
import com.duolingo.app.ui.LearningShape
import com.duolingo.app.ui.Paper
import com.duolingo.app.ui.White
import com.duolingo.app.ui.math.LearningFormula
import com.duolingo.app.ui.math.LearningText
import com.duolingo.app.ui.math.linearizeExplicitPhysicalUnitFractions
import com.duolingo.app.ui.math.mathAccessibilityDescription
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

/** Deliberately contains only prompt material: never pass the Exercise itself to sharing code. */
data class ChallengeShareCard(
    val lessonTitle: String,
    val prompt: String,
    val options: List<String>,
    val formula: String? = null,
    val code: String? = null,
)

fun challengeShareCard(lesson: Lesson, exercise: Exercise): ChallengeShareCard? {
    val fields = when (exercise) {
        is Exercise.MathChallenge -> Triple(exercise.options, exercise.formula, null)
        is Exercise.MultipleChoice -> Triple(exercise.options, null, null)
        is Exercise.CodeChallenge -> Triple(exercise.options, null, exercise.codeSnippet)
        else -> return null
    }
    val (options, formula, code) = fields
    if (lesson.title.isBlank() || exercise.prompt.isBlank() || options.isEmpty() || options.size > 8 ||
        lesson.title.length > 100 || exercise.prompt.length > 420 || options.any { it.isBlank() || it.length > 120 } ||
        (formula?.length ?: 0) > 180 || (code?.length ?: 0) > 500
    ) return null
    return ChallengeShareCard(lesson.title, exercise.prompt, options.toList(), formula, code)
}

fun ChallengeShareCard.asShareText(): String = buildString {
    appendLine("Atelier d’apprentissage · Défi")
    appendLine(lessonTitle)
    appendLine()
    if (formula != null) appendLine(readableMath(formula))
    if (code != null) appendLine(code)
    appendLine(prompt.toReadableMath())
    appendLine()
    options.forEachIndexed { index, option -> appendLine("${'A' + index}. ${option.toReadableMath()}") }
    append("À toi de jouer !")
}.trim()

private fun readableMath(latex: String): String {
    val normalized = linearizeExplicitPhysicalUnitFractions(latex)
    val unitExponent = Regex("\\\\mathrm\\{([A-Za-zμΩ]+/[A-Za-zμΩ]+)\\^\\{([23])}}")
        .replace(normalized) { match -> match.groupValues[1] + if (match.groupValues[2] == "2") "²" else "³" }
    return mathAccessibilityDescription(unitExponent)
}

private fun String.toReadableMath(): String = replace(Regex("\\$\\$?([^$]+)\\$\\$?")) { readableMath(it.groupValues[1]) }

interface ChallengeShareService {
    val canSaveImage: Boolean
    suspend fun saveImage(image: ImageBitmap): Boolean
}

private object NoChallengeImageService : ChallengeShareService {
    override val canSaveImage = false
    override suspend fun saveImage(image: ImageBitmap): Boolean = false
}

val LocalChallengeShareService = compositionLocalOf<ChallengeShareService> { NoChallengeImageService }

@Composable
fun ChallengeShareDialog(card: ChallengeShareCard, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val service = LocalChallengeShareService.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    var busy by remember(card) { mutableStateOf(false) }
    var status by remember(card) { mutableStateOf("") }

    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(color = White, shape = LearningShape, modifier = Modifier.testTag("challenge-share-dialog").widthIn(max = 700.dp).fillMaxWidth(0.98f).heightIn(max = 720.dp)) {
            Column(Modifier.padding(20.dp).heightIn(max = 720.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Partager un défi", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("La question et ses choix, sans correction ni indice.", color = Graphite, fontSize = 14.sp)
                    }
                    TextButton(onClick = { if (!busy) onDismiss() }, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Fermer") }
                }
                // The parent scrolls on short screens. The card itself remains unbounded vertically,
                // so the recorded layer and PNG keep every option even when the preview scrolls.
                Column(Modifier.weight(1f).heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                    ChallengeShareCardView(card, Modifier.fillMaxWidth().drawWithContent {
                        layer.record { this@drawWithContent.drawContent() }
                        drawLayer(layer)
                    })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        clipboard.setText(AnnotatedString(card.asShareText()))
                        status = "Question copiée dans le presse-papiers."
                    }, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Copier le texte") }
                    if (service.canSaveImage) Button(onClick = {
                        busy = true
                        status = "Préparation de l’image…"
                        scope.launch {
                            try {
                                val image = layer.toImageBitmap()
                                status = if (service.saveImage(image)) "Image PNG enregistrée." else "Enregistrement annulé."
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                status = "Impossible de créer ou d’enregistrer l’image."
                            } finally { busy = false }
                        }
                    }, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(if (busy) "Enregistrement…" else "Enregistrer PNG") }
                }
                if (status.isNotBlank()) Text(status, color = Graphite, fontSize = 14.sp, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
    }
}

@Composable
fun ChallengeShareCardView(card: ChallengeShareCard, modifier: Modifier = Modifier) {
    Column(modifier.testTag("challenge-share-card").fillMaxWidth().background(Paper, RoundedCornerShape(18.dp)).border(1.dp, Color(0xFFE6DFD3), RoundedCornerShape(18.dp)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(BrandRed, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Text("A", color = White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("ATELIER D’APPRENTISSAGE", color = Graphite, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                Text(card.lessonTitle, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(BrandRed))
        if (card.formula != null) Surface(color = White, shape = LearningShape) {
            LearningFormula(card.formula, Modifier.fillMaxWidth().padding(16.dp), style = androidx.compose.ui.text.TextStyle(fontSize = 22.sp))
        }
        if (card.code != null) Surface(color = Ink, shape = LearningShape) {
            Text(card.code, Modifier.fillMaxWidth().padding(16.dp), color = White, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        }
        LearningText(card.prompt, style = androidx.compose.ui.text.TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold), recognizeLegacyMath = true)
        card.options.forEachIndexed { index, option ->
            Row(Modifier.fillMaxWidth().background(White, RoundedCornerShape(12.dp)).padding(14.dp), verticalAlignment = Alignment.Top) {
                Text("${'A' + index}", color = BrandRed, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(28.dp))
                LearningText(option, Modifier.weight(1f), style = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, lineHeight = 22.sp), recognizeLegacyMath = true)
            }
        }
        Text("Relève le défi", modifier = Modifier.fillMaxWidth(), color = BrandRed, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}
