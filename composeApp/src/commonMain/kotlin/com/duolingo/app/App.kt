package com.duolingo.app

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.material.Typography
import androidx.compose.ui.text.font.FontFamily
import com.duolingo.app.ui.DuolingoApp

@Composable
fun App() {
    MaterialTheme(typography = Typography(defaultFontFamily = FontFamily.SansSerif)) {
        DuolingoApp()
    }
}
