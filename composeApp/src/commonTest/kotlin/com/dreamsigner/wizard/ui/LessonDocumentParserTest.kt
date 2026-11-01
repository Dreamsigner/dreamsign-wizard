package com.dreamsigner.wizard.ui

import com.dreamsigner.wizard.ui.math.LearningTextSegment
import com.dreamsigner.wizard.ui.math.parseLearningText
import com.dreamsigner.wizard.ui.math.mathAccessibilityDescription
import kotlin.test.Test
import kotlin.test.assertEquals

class LessonDocumentParserTest {
    @Test fun modelResponseKeepsDisplayMathAndCodeAndParsesNumberedSteps() {
        val source = "# Calcul\n\n1. Écrire \\(F=ma\\)\n2. Calculer\n\n\\[\nF = ma\n\n= 12\\,\\mathrm{N}\n\\]\n\n```python\nprice = '$5'\n```"
        val blocks = parseLessonDocument(source)
        assertEquals(listOf(LessonBlockKind.HEADING, LessonBlockKind.ORDERED, LessonBlockKind.ORDERED,
            LessonBlockKind.PARAGRAPH, LessonBlockKind.CODE), blocks.map { it.kind })
        assertEquals("1.", blocks[1].language)
        assertEquals(listOf(LearningTextSegment.Math("\nF = ma\n\n= 12\\,\\mathrm{N}\n", true)), parseLearningText(blocks[3].text))
        assertEquals("price = '$5'", blocks[4].text)
    }
    @Test fun explicitBlocksPreserveCodeOperatorsAndMath() {
        val blocks = parseLessonDocument("## Flux Unix\n\nLe **shell** prépare les arguments.\n\n```bash\nprintf '%s\\n' bleu | sort > liste.txt\n```\n\n> Attention aux redirections.\n\n- Lire le résultat.\n\n\$\$x_1=b_1\$\$")
        assertEquals(listOf(LessonBlockKind.HEADING, LessonBlockKind.PARAGRAPH, LessonBlockKind.CODE,
            LessonBlockKind.CALLOUT, LessonBlockKind.BULLET, LessonBlockKind.PARAGRAPH), blocks.map { it.kind })
        assertEquals("bash", blocks[2].language)
        assertEquals("printf '%s\\n' bleu | sort > liste.txt", blocks[2].text)
        assertEquals("\$\$x_1=b_1\$\$", blocks.last().text)
    }

    @Test fun unfinishedCodeFenceIsNotSilentlyRemoved() {
        assertEquals(listOf(LessonBlock(LessonBlockKind.PARAGRAPH, "```bash\npwd")), parseLessonDocument("```bash\npwd"))
    }

    @Test fun inlineCodeIsNeverInterpretedAsMath() {
        assertEquals(listOf(LearningTextSegment.Code("x_1^2 > 0; \$HOME")), parseLearningText("`x_1^2 > 0; \$HOME`", true))
        assertEquals(listOf(LearningTextSegment.Strong("Bash")), parseLearningText("**Bash**"))
        assertEquals(listOf(LearningTextSegment.Text("x**2 + y**2")), parseLearningText("x**2 + y**2", true))
    }

    @Test fun subscriptsAndSuperscriptsRetainTheirDifferentMeaning() {
        assertEquals(listOf(LearningTextSegment.Math("x_{1}", false), LearningTextSegment.Text(" et "),
            LearningTextSegment.Math("x^{2}", false)), parseLearningText("x₁ et x²", true))
    }

    @Test fun datesAndPathsAreNotInferredAsFractions() {
        listOf("2026/01/09", "/tmp/3/4", "3/4.txt", "2026-02-31").forEach { value ->
            assertEquals(listOf(LearningTextSegment.Text(value)), parseLearningText(value, true))
        }
        assertEquals(listOf(LearningTextSegment.Math("\\frac{3}{4}", false)), parseLearningText("3/4", true))
    }

    @Test fun newVectorAndRowOperationCommandsHaveReadableDescriptions() {
        assertEquals("x", mathAccessibilityDescription("\\mathbf{x}"))
        assertEquals("L indice 2 reçoit L indice 2 -2L indice 1", mathAccessibilityDescription("L_2\\leftarrow L_2-2L_1"))
    }
}
