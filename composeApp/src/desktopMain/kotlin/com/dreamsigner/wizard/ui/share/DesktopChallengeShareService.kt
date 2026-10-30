package com.dreamsigner.wizard.ui.share

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException
import javax.swing.JOptionPane

class DesktopChallengeShareService : ChallengeShareService {
    override val canSaveImage = true

    override suspend fun saveImage(image: ImageBitmap): Boolean = withContext(Dispatchers.IO) {
        val pngBytes = Image.makeFromBitmap(image.asSkiaBitmap()).use { skiaImage ->
            skiaImage.encodeToData(EncodedImageFormat.PNG)?.use { it.bytes.copyOf() }
                ?: error("Skia did not encode the challenge card as PNG")
        }
        val target = chooseDestination() ?: return@withContext false
        currentCoroutineContext().ensureActive()
        val parent = target.parent ?: error("The selected PNG destination has no parent directory")
        val temporary = Files.createTempFile(parent, ".atelier-defi-", ".png.tmp")
        try {
            Files.write(temporary, pngBytes)
            currentCoroutineContext().ensureActive()
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
            }
            true
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun chooseDestination(): Path? {
        var selected: Path? = null
        var failure: Throwable? = null
        java.awt.EventQueue.invokeAndWait {
            try {
                val owner = Frame.getFrames().firstOrNull { it.isDisplayable && it.isVisible }
                val dialog = FileDialog(owner, "Enregistrer le défi en PNG", FileDialog.SAVE)
                val name: String?
                val directory: String?
                try {
                    dialog.file = "defi-atelier.png"
                    dialog.isVisible = true
                    name = dialog.file
                    directory = dialog.directory
                } finally { dialog.dispose() }
                if (name == null) return@invokeAndWait
                val candidate = Path.of(directory ?: "", if (name.endsWith(".png", true)) name else "$name.png").toAbsolutePath().normalize()
                if (Files.exists(candidate)) {
                    val answer = JOptionPane.showConfirmDialog(
                        owner,
                        "Le fichier existe déjà. Le remplacer ?",
                        "Confirmer le remplacement",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                    )
                    if (answer != JOptionPane.YES_OPTION) return@invokeAndWait
                }
                selected = candidate
            } catch (error: Throwable) { failure = error }
        }
        failure?.let { throw it }
        return selected
    }
}
