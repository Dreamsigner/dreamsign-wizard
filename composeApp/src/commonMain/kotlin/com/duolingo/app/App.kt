package com.duolingo.app

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.material.Typography
import androidx.compose.material.lightColors
import androidx.compose.ui.text.font.FontFamily
import com.duolingo.app.ui.ActionRed
import com.duolingo.app.ui.BrandRed
import com.duolingo.app.ui.DuolingoApp
import com.duolingo.app.ui.Ink
import com.duolingo.app.ui.Paper
import com.duolingo.app.ui.White

@Composable
fun App(settingsActionCount: Int = 0, settingsContent: @Composable () -> Unit = {}) {
    LearningAppTheme { DuolingoApp(settingsActionCount = settingsActionCount, settingsContent = settingsContent) }
}

/** Shared by native entry points and offscreen UI tests, preserving the production theme. */
@Composable
fun LearningAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = lightColors(
            primary = ActionRed,
            primaryVariant = BrandRed,
            secondary = Ink,
            background = Paper,
            surface = White,
            error = ActionRed,
            onPrimary = White,
            onSecondary = White,
            onBackground = Ink,
            onSurface = Ink,
            onError = White
        ),
        typography = Typography(defaultFontFamily = FontFamily.SansSerif)
    ) {
        content()
    }
}
