package com.duolingo.app

import androidx.compose.ui.window.ComposeUIViewController

/** Native entry point exported as MainViewControllerKt.MainViewController() in Swift. */
// Compose 1.8 removed AccessibilitySyncOptions; accessibility now synchronizes on demand.
// https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.8.0
fun MainViewController() = ComposeUIViewController { App() }
