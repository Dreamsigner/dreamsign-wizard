package com.duolingo.app.media

import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.sound.sampled.*
import javax.swing.SwingUtilities
import kotlin.test.*

/** Every recorder receives an explicit fake line factory: these tests cannot open a mic. */
class AudioRecorderTest {
    private enum class Mode { STREAM, BLOCK_AFTER_CHUNK, BLOCK_EMPTY, FAIL_READ }

    private class FakeLine(private val mode: Mode, private val openGate: CountDownLatch? = null) : TargetDataLine {
        val openEntered = CountDownLatch(1)
        val blockedRead = CountDownLatch(1)
        val closed = CountDownLatch(1)
        val readCount = AtomicInteger()
        val openedOnUi = AtomicBoolean()
        val readOnUi = AtomicBoolean()
        private val unblockRead = CountDownLatch(1)
        private val opened = AtomicBoolean()
        private val running = AtomicBoolean()
        private var audioFormat = AudioFormat(RECORDING_RATE.toFloat(), 16, 1, true, false)

        override fun open(format: AudioFormat) {
            openedOnUi.set(SwingUtilities.isEventDispatchThread())
            openEntered.countDown()
            check(openGate?.await(3, TimeUnit.SECONDS) != false) { "Fake open barrier timed out" }
            audioFormat = format
            opened.set(true)
        }
        override fun open(format: AudioFormat, bufferSize: Int) = open(format)
        override fun open() = open(audioFormat)
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (SwingUtilities.isEventDispatchThread()) readOnUi.set(true)
            val call = readCount.incrementAndGet()
            if (mode == Mode.FAIL_READ) throw IOException("Synthetic read failure")
            if (mode == Mode.BLOCK_EMPTY || (mode == Mode.BLOCK_AFTER_CHUNK && call > 1)) {
                blockedRead.countDown()
                check(unblockRead.await(3, TimeUnit.SECONDS)) { "Fake read was not stopped" }
                return 0
            }
            repeat(length) { buffer[offset + it] = (it % 89).toByte() }
            return length
        }
        override fun start() { running.set(true) }
        override fun stop() { running.set(false); unblockRead.countDown() }
        override fun close() { opened.set(false); running.set(false); unblockRead.countDown(); closed.countDown() }
        override fun isOpen() = opened.get()
        override fun isRunning() = running.get()
        override fun isActive() = running.get()
        override fun getFormat() = audioFormat
        override fun getBufferSize() = 4096
        override fun available() = 0
        override fun getFramePosition() = 0
        override fun getLongFramePosition() = 0L
        override fun getMicrosecondPosition() = 0L
        override fun getLevel() = 0f
        override fun drain() = Unit
        override fun flush() = Unit
        override fun getLineInfo(): Line.Info = Line.Info(TargetDataLine::class.java)
        override fun getControls(): Array<Control> = emptyArray()
        override fun isControlSupported(control: Control.Type) = false
        override fun getControl(control: Control.Type): Control = throw IllegalArgumentException("No fake controls")
        override fun addLineListener(listener: LineListener) = Unit
        override fun removeLineListener(listener: LineListener) = Unit
    }

    private class Probe {
        val events = CopyOnWriteArrayList<String>()
        val started = CountDownLatch(1)
        val terminal = CountDownLatch(1)
        val bytes = AtomicReference<ByteArray?>()
        val error = AtomicReference<String?>()
        val callbacksOnUi = AtomicBoolean(true)
        fun start(recorder: PlatformAudioRecorder, seconds: Int) = ui {
            recorder.start(seconds, {
                note("started"); started.countDown()
            }, {
                bytes.set(it); note("complete"); terminal.countDown()
            }, {
                error.set(it); note("error"); terminal.countDown()
            })
        }
        private fun note(event: String) {
            if (!SwingUtilities.isEventDispatchThread()) callbacksOnUi.set(false)
            events.add(event)
        }
    }

    @Test fun realWorkerEncodesPcmAndCompletesOnceAtSampleLimit() {
        val line = FakeLine(Mode.STREAM)
        val recorder = DesktopAudioRecorder { format ->
            assertEquals(16000f, format.sampleRate)
            assertEquals(16, format.sampleSizeInBits)
            assertEquals(1, format.channels)
            assertFalse(format.isBigEndian)
            line
        }
        val probe = Probe()
        try {
            probe.start(recorder, 1)
            await(probe.terminal)
            ui { recorder.stop(); recorder.stop() }
            assertEquals(listOf("started", "complete"), probe.events.toList())
            val wav = assertNotNull(probe.bytes.get())
            assertEquals(44 + 32000, wav.size)
            assertEquals(1000L, recordingDurationMillis(wav))
            assertContentEquals(ByteArray(16) { it.toByte() }, wav.copyOfRange(44, 60))
            assertTrue(probe.callbacksOnUi.get())
            assertFalse(line.openedOnUi.get())
            assertFalse(line.readOnUi.get())
            await(line.closed)
        } finally { ui { recorder.cancel() } }
    }

    @Test fun stopUnblocksReadAndFinalizesCapturedChunkOnlyOnce() {
        val line = FakeLine(Mode.BLOCK_AFTER_CHUNK)
        val recorder = DesktopAudioRecorder { line }
        val probe = Probe()
        try {
            probe.start(recorder, 5)
            await(probe.started); await(line.blockedRead)
            ui { recorder.stop(); recorder.stop() }
            await(probe.terminal)
            assertEquals(listOf("started", "complete"), probe.events.toList())
            assertEquals(128L, recordingDurationMillis(assertNotNull(probe.bytes.get())))
            assertTrue(probe.callbacksOnUi.get())
            await(line.closed)
        } finally { ui { recorder.cancel() } }
    }

    @Test fun cancelUnblocksReadWithoutCompletingOrReportingAnError() {
        val line = FakeLine(Mode.BLOCK_AFTER_CHUNK)
        val recorder = DesktopAudioRecorder { line }
        val probe = Probe()
        try {
            probe.start(recorder, 5)
            await(probe.started); await(line.blockedRead)
            ui { recorder.cancel(); recorder.cancel() }
            await(line.closed)
            assertFalse(probe.terminal.await(250, TimeUnit.MILLISECONDS))
            assertEquals(listOf("started"), probe.events.toList())
            assertNull(probe.bytes.get()); assertNull(probe.error.get())
        } finally { ui { recorder.cancel() } }
    }

    @Test fun replacementSuppressesOldWorkerEvenWhenItsOpenReturnsLate() {
        val openGate = CountDownLatch(1)
        val oldLine = FakeLine(Mode.STREAM, openGate)
        val newLine = FakeLine(Mode.STREAM)
        val calls = AtomicInteger()
        val recorder = DesktopAudioRecorder { if (calls.getAndIncrement() == 0) oldLine else newLine }
        val oldProbe = Probe()
        val newProbe = Probe()
        try {
            oldProbe.start(recorder, 1)
            await(oldLine.openEntered)
            newProbe.start(recorder, 1)
            await(newProbe.terminal)
            openGate.countDown()
            await(oldLine.closed)
            assertFalse(oldProbe.terminal.await(250, TimeUnit.MILLISECONDS))
            assertTrue(oldProbe.events.isEmpty())
            assertEquals(listOf("started", "complete"), newProbe.events.toList())
            assertEquals(2, calls.get())
        } finally { openGate.countDown(); ui { recorder.cancel() } }
    }

    @Test fun stoppingWithoutSamplesReportsOneErrorWithoutCompletion() {
        val line = FakeLine(Mode.BLOCK_EMPTY)
        val recorder = DesktopAudioRecorder { line }
        val probe = Probe()
        try {
            probe.start(recorder, 5)
            await(probe.started); await(line.blockedRead)
            ui { recorder.stop() }
            await(probe.terminal)
            assertEquals(listOf("started", "error"), probe.events.toList())
            assertNull(probe.bytes.get())
            assertTrue(assertNotNull(probe.error.get()).contains("Aucun son"))
            assertTrue(probe.callbacksOnUi.get())
        } finally { ui { recorder.cancel() } }
    }

    @Test fun readFailureClosesLineAndReportsOneErrorOnUi() {
        val line = FakeLine(Mode.FAIL_READ)
        val recorder = DesktopAudioRecorder { line }
        val probe = Probe()
        try {
            probe.start(recorder, 1)
            await(probe.terminal)
            ui { recorder.stop(); recorder.cancel() }
            assertEquals(listOf("started", "error"), probe.events.toList())
            assertNull(probe.bytes.get())
            assertTrue(probe.callbacksOnUi.get())
            await(line.closed)
        } finally { ui { recorder.cancel() } }
    }

    @Test fun initializationFailureReportsOneErrorWithoutStartedOrCompletion() {
        val factoryOnUi = AtomicBoolean(true)
        val recorder = DesktopAudioRecorder {
            factoryOnUi.set(SwingUtilities.isEventDispatchThread())
            throw LineUnavailableException("Synthetic device unavailable")
        }
        val probe = Probe()
        try {
            probe.start(recorder, 1)
            await(probe.terminal)
            ui { recorder.stop() }
            assertEquals(listOf("error"), probe.events.toList())
            assertNull(probe.bytes.get())
            assertFalse(factoryOnUi.get())
            assertTrue(probe.callbacksOnUi.get())
        } finally { ui { recorder.cancel() } }
    }

    @Test fun invalidDurationNeverCreatesAnInputLine() {
        val calls = AtomicInteger()
        val recorder = DesktopAudioRecorder { calls.incrementAndGet(); FakeLine(Mode.STREAM) }
        val probe = Probe()
        try {
            probe.start(recorder, 0)
            await(probe.terminal)
            assertEquals(0, calls.get())
            assertEquals(listOf("error"), probe.events.toList())
        } finally { ui { recorder.cancel() } }
    }

    @Test fun durationAbove270SecondsIsClampedBeforeCapturingSamples() {
        val line = FakeLine(Mode.STREAM)
        val recorder = DesktopAudioRecorder { line }
        val probe = Probe()
        try {
            probe.start(recorder, Int.MAX_VALUE)
            await(probe.terminal)
            val wav = assertNotNull(probe.bytes.get())
            assertEquals(44 + 270 * 32000, wav.size)
            assertEquals(270000L, recordingDurationMillis(wav))
            assertEquals(listOf("started", "complete"), probe.events.toList())
        } finally { ui { recorder.cancel() } }
    }

    @Test fun deadlineStopsABlockedWorkerWithoutWaitingForSampleLimit() {
        val line = FakeLine(Mode.BLOCK_AFTER_CHUNK)
        val recorder = DesktopAudioRecorder { line }
        val probe = Probe()
        try {
            probe.start(recorder, 1)
            await(probe.started); await(line.blockedRead)
            await(probe.terminal) // Swing deadline must stop the blocked read after one second.
            assertEquals(listOf("started", "complete"), probe.events.toList())
            assertEquals(128L, recordingDurationMillis(assertNotNull(probe.bytes.get())))
            assertTrue(probe.callbacksOnUi.get())
            await(line.closed)
        } finally { ui { recorder.cancel() } }
    }

    private companion object {
        fun ui(action: () -> Unit) {
            if (SwingUtilities.isEventDispatchThread()) action() else SwingUtilities.invokeAndWait { action() }
        }
        fun await(latch: CountDownLatch) = assertTrue(latch.await(3, TimeUnit.SECONDS), "Synthetic worker callback timed out")
    }
}
