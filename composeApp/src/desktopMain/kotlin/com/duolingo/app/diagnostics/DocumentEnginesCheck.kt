package com.duolingo.app.diagnostics

import androidx.compose.ui.graphics.Color
import com.duolingo.app.ui.math.renderMathBitmap
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.rendering.PDFRenderer
import java.io.ByteArrayOutputStream

/** Exercise the distributed engines with original in-memory material, never learner data. */
internal fun checkDesktopDocumentEngines() {
    val math = checkNotNull(renderMathBitmap("\\frac{x^2}{2}+\\sqrt{y}", 24f, Color.Black, true))
    check(math.width > 0 && math.height > 0)
    val bytes = ByteArrayOutputStream().use { output ->
        PDDocument().use { document ->
            document.addPage(PDPage())
            document.save(output)
        }
        output.toByteArray()
    }
    Loader.loadPDF(bytes).use { document ->
        check(document.numberOfPages == 1)
        val page = PDFRenderer(document).renderImage(0, 0.25f)
        try { check(page.width > 0 && page.height > 0) } finally { page.flush() }
    }
}
