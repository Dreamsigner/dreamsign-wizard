@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package com.dreamsigner.wizard.ui

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import com.dreamsigner.wizard.LearningAppTheme
import com.dreamsigner.wizard.storage.ProgressStore
import com.dreamsigner.wizard.update.UpdatePanel
import com.dreamsigner.wizard.update.UpdateUiState
import com.dreamsigner.wizard.viewmodel.AppScreen
import com.dreamsigner.wizard.viewmodel.AppViewModel
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.*

class SettingsUiTest {
    @Test fun settingsNavigationBadgeAndControlsWorkAtAllNavigationWidths() {
        listOf(500, 800, 1100).forEach { width -> runDesktopComposeUiTest(width = width, height = 850) {
            val model = AppViewModel(object : ProgressStore {
                override fun read(): String? = null
                override fun write(value: String) = Unit
            }, catalogue = emptyList())
            var update by mutableStateOf(UpdateUiState("1.6.0", false, true, "Version 1.7.0 prête", ready = true))
            var checks = 0
            var installs = 0
            setContent { LearningAppTheme {
                DreamsignWizardApp(model, settingsActionCount = update.actionCount) {
                    UpdatePanel(update, { checks++ }, { update = update.copy(automatic = it) }, { installs++ })
                }
            } }
            val settings = hasText("Paramètres") and hasClickAction()
            val attention = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "1 action requise")
            onNodeWithText("Version 1.6.0").assertDoesNotExist()
            onNode(settings and attention).assertIsDisplayed()
                .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
                .performKeyInput { pressKey(Key.Enter) }
            onNodeWithText("Version 1.6.0").assertIsDisplayed()
            onNodeWithText("Données et progression").assertIsDisplayed()
            onNodeWithText("Vérifier les mises à jour").performClick()
            onNodeWithText("Installer et redémarrer").performClick()
            onNode(isToggleable()).performClick().assertIsOn()
            runOnIdle { assertEquals(1, checks); assertEquals(1, installs); assertTrue(update.automatic) }
            val file = File("build/reports/ui-snapshots/settings-$width.png"); file.parentFile.mkdirs()
            Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image ->
                image.encodeToData()!!.use { file.writeBytes(it.bytes) }
            }
            onNodeWithText("Cours").performClick()
            onNodeWithText("Version 1.6.0").assertDoesNotExist()
            onNode(settings and attention).assertExists()
            runOnIdle { update = update.copy(ready = false, statusMessage = "Application à jour") }
            onNode(settings and attention).assertDoesNotExist()
            onNode(settings).performClick()
            onNodeWithText("Application à jour").assertIsDisplayed()
            onNodeWithText("Installer et redémarrer").assertDoesNotExist()
            runOnIdle { assertEquals(AppScreen.SETTINGS, model.currentScreen.value) }
        } }
    }

    @Test fun narrowSettingsRemainScrollableAndPortableDoesNotOfferAutomaticInstallation() = runDesktopComposeUiTest(width = 360, height = 480) {
        setContent { LearningAppTheme {
            SettingsScreen("Progression enregistrée sur cet appareil.") {
                UpdatePanel(UpdateUiState("1.6.0", false, false, "Installation Windows standard requise"), {}, {}, {})
            }
        } }
        onNodeWithText("Version 1.6.0").assertIsDisplayed()
        onNodeWithText("Mises à jour auto").assertDoesNotExist()
        onNodeWithText("Installer et redémarrer").assertDoesNotExist()
        onNodeWithText("Sauvegarde sur cet appareil").performScrollTo().assertIsDisplayed()
    }
}
