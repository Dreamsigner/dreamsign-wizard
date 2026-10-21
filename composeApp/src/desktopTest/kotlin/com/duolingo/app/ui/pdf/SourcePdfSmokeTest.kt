package com.duolingo.app.ui.pdf

import com.duolingo.app.data.CourseResources
import kotlinx.coroutines.runBlocking
import kotlin.test.assertTrue

/** Explicit opt-in task; the ordinary desktop suite never relies on a remote course server. */
class SourcePdfSmokeTest {
    @org.junit.Test(timeout = 60000)
    fun officialMitSessionDownloadsOpensAndRendersWithTheProductionReader() = runBlocking<Unit> {
        val pdf = CourseResources.forSession("mit_18_06sc_2011", 1).first { it.kind.contains("pdf") }
        val session = DesktopPdfDocumentReader().open(pdf)
        try {
            assertTrue(session.pageCount > 0)
            val page = session.renderPage(0, 1000)
            assertTrue(page.width > 0 && page.height > 0)
            println("Source PDF smoke passed: ${pdf.url}; ${session.pageCount} pages; rendered ${page.width}x${page.height}.")
        } finally { session.close() }
    }
}
