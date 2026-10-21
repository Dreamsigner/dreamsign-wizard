package com.duolingo.app

import com.duolingo.app.media.*
import kotlin.test.*

class RecordingFormatTest {
    @Test fun pcmIsEncodedWithAnExactSampleDuration() {
        val wav = pcmRecordingToWav(ByteArray(32000))
        assertEquals(32044, wav.size)
        assertEquals(1000L, recordingDurationMillis(wav))
        validateRecordingBytes(wav)
    }

    @Test fun corruptChunksAndArbitraryFileNamesAreRejected() {
        assertFailsWith<IllegalArgumentException> { validateRecordingId("../../other") }
        assertFailsWith<IllegalArgumentException> { validateRecordingId("C:\\record.wav") }
        validateRecordingId("tcf_speaking_2026_e01")
        val good = pcmRecordingToWav(ByteArray(32000))
        assertFailsWith<IllegalArgumentException> { validateRecordingBytes(good.copyOf(100)) }
        val wrongRate = good.copyOf().also { it[24] = 1 }
        assertFailsWith<IllegalArgumentException> { validateRecordingBytes(wrongRate) }
        val invalidChunk = good.copyOf().also { it[16] = 127; it[19] = 127 }
        assertFailsWith<IllegalArgumentException> { validateRecordingBytes(invalidChunk) }
        val wrongAlignment = good.copyOf().also { it[32] = 4 }
        assertFailsWith<IllegalArgumentException> { validateRecordingBytes(wrongAlignment) }
        // 32000 = 0x00007d00 already has a zero low byte; change it to one.
        val wrongByteRate = good.copyOf().also { it[28] = 1 }
        assertFailsWith<IllegalArgumentException> { validateRecordingBytes(wrongByteRate) }
    }
}
