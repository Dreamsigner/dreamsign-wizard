package com.duolingo.app.media

import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.WindowEvent

/** JVM-local window events; does not observe other processes. */
actual fun bindAudioLifecycle(player: PlatformAudioPlayer, onBackground: () -> Unit): () -> Unit {
    val toolkit = Toolkit.getDefaultToolkit()
    var bound = true
    val listener = AWTEventListener { event ->
        // AWT delivers window events on the Swing EDT, matching the player's UI loop.
        if (bound && event is WindowEvent && event.id == WindowEvent.WINDOW_DEACTIVATED) {
            player.stop()
            onBackground()
        }
    }
    toolkit.addAWTEventListener(listener, AWTEvent.WINDOW_EVENT_MASK)
    return {
        if (bound) {
            bound = false
            toolkit.removeAWTEventListener(listener)
        }
    }
}
