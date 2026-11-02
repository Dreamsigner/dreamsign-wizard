package com.dreamsigner.wizard.ui.studio

import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import java.io.Writer
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.file.Files
import java.nio.file.Path

internal const val NOTES_MAX_BYTES = 5 * 1024 * 1024
internal const val NOTES_MAX_CHARS = 40000
internal const val NOTES_MAX_PAGES = 100

/** Reads only an explicitly chosen file; rejects oversized input rather than silently truncating it. */
internal fun readNotes(path: Path): String {
    val extension = path.fileName.toString().substringAfterLast('.', "").lowercase()
    require(extension in setOf("txt", "md", "markdown", "pdf")) { "Format non pris en charge." }
    val bytes = Files.newInputStream(path).use { it.readNBytes(NOTES_MAX_BYTES + 1) }
    require(bytes.isNotEmpty() && bytes.size <= NOTES_MAX_BYTES) { "Fichier vide ou supérieur à 5 Mo." }
    val text = if (extension == "pdf") {
        require(bytes.take(5).toByteArray().contentEquals("%PDF-".toByteArray())) { "PDF invalide." }
        Loader.loadPDF(bytes).use { document ->
            require(!document.isEncrypted) { "PDF protégé par mot de passe." }
            require(document.numberOfPages in 1..NOTES_MAX_PAGES) { "PDF limité à 100 pages." }
            val result = StringBuilder()
            val writer = object : Writer() {
                override fun write(buffer: CharArray, offset: Int, length: Int) {
                    require(result.length + length <= NOTES_MAX_CHARS) { "Texte limité à 40 000 caractères." }
                    result.append(buffer, offset, length)
                }
                override fun flush() = Unit
                override fun close() = Unit
            }
            PDFTextStripper().writeText(document, writer)
            result.toString()
        }
    } else {
        Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
    }
    require(text.length <= NOTES_MAX_CHARS && text.isNotBlank()) { "Texte vide ou supérieur à 40 000 caractères." }
    return text
}
