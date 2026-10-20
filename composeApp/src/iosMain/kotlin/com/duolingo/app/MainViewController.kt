package com.duolingo.app

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.platform.AccessibilitySyncOptions
import platform.Foundation.NSProcessInfo

/** Native entry point exported as MainViewControllerKt.MainViewController() in Swift. */
@OptIn(ExperimentalComposeApi::class)
fun MainViewController() = ComposeUIViewController(configure = {
    // Compose 1.6.11 otherwise synchronizes only when accessibility services run.
    // XCTest opts in explicitly; normal application launches retain the default.
    if (NSProcessInfo.processInfo.environment["COMPOSE_UI_TESTING"] == "1") {
        accessibilitySyncOptions = AccessibilitySyncOptions.Always(debugLogger = null)
    }
}) { App() }
