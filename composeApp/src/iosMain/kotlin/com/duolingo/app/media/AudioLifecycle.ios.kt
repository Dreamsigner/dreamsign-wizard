@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.duolingo.app.media

import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidEnterBackgroundNotification

actual fun bindAudioLifecycle(player: PlatformAudioPlayer, onBackground: () -> Unit): () -> Unit {
    val center = NSNotificationCenter.defaultCenter
    var bound = true
    val observer = center.addObserverForName(
        UIApplicationDidEnterBackgroundNotification, null, NSOperationQueue.mainQueue
    ) {
        if (bound) {
            player.stop()
            onBackground()
        }
    }
    // Drop the token-capturing closure after unregistering, so this returned closure
    // no longer retains the native observer block and its player/UI callback.
    var removeObserver: (() -> Unit)? = { center.removeObserver(observer) }
    return {
        if (bound) {
            bound = false
            removeObserver?.invoke()
            removeObserver = null
        }
    }
}
