package com.dreamsigner.wizard.ui.studio

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class DesktopNotesImportTest {
    @Test fun importIsReviewedBeforeAcceptanceAndDoesNotSendLocalPath() = runBlocking {
        val directory = Files.createTempDirectory("private-notes")
        val notes = directory.resolve("secret-name.md")
        val file = directory.resolve("studio.json")
        Files.writeString(notes, "Une moitié vaut 1/2.")
        var captured: TutorRequest? = null
        val tutor = StudioTutor { request ->
            captured = request
            """{"propositions":[{"id":"a","statement":"Une moitié vaut 1/2.","messageIndices":[0]}],"reasonings":[]}"""
        }
        var controller = DesktopStudioController(file, tutor, { notes })
        try {
            assertTrue(controller.configure("http://localhost:1234/v1", "model", ""))
            assertTrue(controller.create("Fractions"))
            assertTrue(controller.importNotes())
            withTimeout(10_000) { while (controller.state.value.busy) delay(10) }
            val request = checkNotNull(captured)
            val body = buildTutorBody(request.model, request.goal, request.history)
            assertTrue(body.contains("Une moitié vaut 1/2."))
            assertTrue(body.contains("Extraction du graphe"))
            assertTrue(body.contains("sans QCM"))
            assertTrue(!body.contains(notes.toString()) && !body.contains(notes.fileName.toString()))
            val chatBody = buildChatGptBody(request.model, request.goal, request.history)
            assertTrue(chatBody.contains("Extraction du graphe") && chatBody.contains("sans QCM"))
            assertTrue(!chatBody.contains(notes.toString()) && !chatBody.contains(notes.fileName.toString()))
            assertTrue(controller.state.value.knowledgeGraph.propositions.isEmpty())
            assertEquals("secret-name.md", controller.state.value.graphProposal!!.graph.propositions.single().sources.single().documentName)
            assertTrue(controller.acceptGraphProposal())
        } finally { controller.close() }
        controller = DesktopStudioController(file, tutor, { notes })
        try {
            assertEquals("Une moitié vaut 1/2.", controller.state.value.knowledgeGraph.propositions.single().statement)
            assertEquals(null, controller.state.value.graphProposal)
            assertTrue(!Files.readString(file).contains("Une moitié vaut 1/2."))
        } finally { controller.close() }
    }

    @Test fun textAndBounds() {
        val file = Files.createTempFile("notes", ".md")
        try {
            Files.writeString(file, "# Cours\nDeux parts égales.")
            assertEquals("# Cours\nDeux parts égales.", readNotes(file))
            Files.writeString(file, "   ")
            assertFails { readNotes(file) }
            Files.writeString(file, "a".repeat(NOTES_MAX_CHARS + 1))
            assertFails { readNotes(file) }
            Files.write(file, ByteArray(NOTES_MAX_BYTES + 1))
            assertFails { readNotes(file) }
        } finally { Files.deleteIfExists(file) }
    }

    @Test fun pdfTextIsExtracted() {
        val file = Files.createTempFile("notes", ".pdf")
        try {
            PDDocument().use { doc ->
                val page = PDPage(); doc.addPage(page)
                PDPageContentStream(doc, page).use { stream ->
                    stream.beginText(); stream.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 12f)
                    stream.newLineAtOffset(50f, 700f); stream.showText("Hello notes"); stream.endText()
                }
                doc.save(file.toFile())
            }
            assertTrue(readNotes(file).contains("Hello notes"))
        } finally { Files.deleteIfExists(file) }
    }

    @Test fun invalidEmptyAndProtectedPdfAreRejected() {
        val file = Files.createTempFile("notes", ".pdf")
        try {
            Files.writeString(file, "not a pdf")
            assertFails { readNotes(file) }
            PDDocument().use { it.addPage(PDPage()); it.save(file.toFile()) }
            assertFails { readNotes(file) }
            PDDocument().use {
                it.addPage(PDPage())
                it.protect(StandardProtectionPolicy("owner-password", "user-password", AccessPermission()))
                it.save(file.toFile())
            }
            assertFails { readNotes(file) }
        } finally { Files.deleteIfExists(file) }
    }

    @Test fun documentProvenanceSurvivesGraphPersistenceAndCorrection() {
        val file = Files.createTempFile("graph", ".json")
        try {
            val extraction = KnowledgeGraphExtraction.parse("""{"propositions":[{"id":"a","statement":"Une moitié","messageIndices":[0]}],"reasonings":[]}""", setOf(0))
            val proposal = KnowledgeGraphExtraction.propose(KnowledgeGraph(), extraction, "document-id", setOf(0), "notes.md")
            Files.delete(file)
            val second = KnowledgeGraphExtraction.propose(proposal.graph, extraction, "document-other", setOf(0), "notes.md")
            assertEquals(2, second.graph.propositions.single().sources.size)
            assertEquals(2, second.graph.propositions.single().sources.map { it.sessionId }.distinct().size)
            val store = DesktopKnowledgeGraphStore(file)
            store.save(proposal.graph)
            val loaded = store.load()
            assertEquals("notes.md", loaded.propositions.single().sources.single().documentName)
            val item = loaded.propositions.single()
            store.save(loaded.putProposition(item.copy(statement = "Une moitié vaut 1/2")))
            assertTrue(store.load().propositions.single().sources.single().documentName == "notes.md")
        } finally { Files.deleteIfExists(file) }
    }
}
