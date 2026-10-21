package com.duolingo.app.media

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities
import kotlin.test.*

class AudioPlayerTest {
    @Test fun emptyRecordingReportsAnErrorWithoutCompletion() {
        val failed = CountDownLatch(1)
        val completed = AtomicBoolean(false)
        val callbackOnUi = AtomicBoolean(false)
        val player = createAudioPlayer()
        SwingUtilities.invokeAndWait {
            player.play(byteArrayOf(), { completed.set(true) }, {
                callbackOnUi.set(SwingUtilities.isEventDispatchThread())
                failed.countDown()
            })
        }
        assertTrue(failed.await(2, TimeUnit.SECONDS))
        assertFalse(completed.get())
        assertTrue(callbackOnUi.get())
        SwingUtilities.invokeAndWait { player.stop() }
    }

    @Test fun stopInvalidatesAnInFlightDecoderWithoutCompletionOrErrorCallback() {
        val callback = CountDownLatch(1)
        val player = createAudioPlayer()
        SwingUtilities.invokeAndWait {
            player.play(byteArrayOf(1, 2, 3), { callback.countDown() }, { callback.countDown() })
            player.stop()
        }
        assertFalse(callback.await(300, TimeUnit.MILLISECONDS))
    }
}
