package com.duolingo.app.media

/**
 * Stop [player], then invoke [onBackground] on the platform UI loop when the app loses
 * foreground activity. Bind/unbind on the UI loop. The owner can cancel resource loading
 * and reset its UI. The returned unbind closure is idempotent; it does not stop the player.
 */
expect fun bindAudioLifecycle(player: PlatformAudioPlayer, onBackground: () -> Unit): () -> Unit
