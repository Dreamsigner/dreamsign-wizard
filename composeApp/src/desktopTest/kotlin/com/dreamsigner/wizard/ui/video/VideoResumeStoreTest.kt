package com.dreamsigner.wizard.ui.video

import java.nio.file.Files
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.test.*

class VideoResumeStoreTest {
    @Test fun versionedFileRoundTripsAfterStoreRecreationAndSeparatesSources() {
        val dir = Files.createTempDirectory("video-resume-roundtrip")
        val path = dir.resolve("video-playback.json")
        try {
            val first = FileVideoResumeStore(path)
            val youtube = videoResumeKey("https://www.youtube.com/watch?v=abcdefghijk&t=20")
            val shortLink = videoResumeKey("https://youtu.be/abcdefghijk")
            val mp4 = videoResumeKey("https://archive.org/download/course/lecture.mp4")
            assertEquals(youtube, shortLink)
            assertNotEquals(youtube, mp4)
            first.write(youtube, VideoResumeEntry(127.5, 900.0))
            first.write(mp4, VideoResumeEntry(5.0, 600.0))
            val afterRestart = FileVideoResumeStore(path)
            assertEquals(VideoResumeEntry(127.5, 900.0), afterRestart.read(shortLink))
            assertEquals(VideoResumeEntry(5.0, 600.0), afterRestart.read(mp4))
            assertTrue(Files.readString(path).contains("\"version\":1"))
        } finally { Files.deleteIfExists(path); Files.deleteIfExists(dir) }
    }

    @Test fun malformedFileAndAtomicWriteFailurePreserveExistingData() {
        val dir = Files.createTempDirectory("video-resume-atomic")
        val path = dir.resolve("video-playback.json")
        try {
            val good = FileVideoResumeStore(path)
            good.write("mp4:first", VideoResumeEntry(12.0, 60.0))
            val original = Files.readString(path)
            val failing = FileVideoResumeStore(path) { _, _ -> throw IOException("injected replacement failure") }
            assertFails { failing.write("mp4:second", VideoResumeEntry(25.0, 60.0)) }
            assertEquals(original, Files.readString(path))
            Files.writeString(path, "{broken")
            assertFails { good.read("mp4:first") }
            assertFails { good.write("mp4:second", VideoResumeEntry(25.0, 60.0)) }
            assertEquals("{broken", Files.readString(path))
        } finally { Files.deleteIfExists(path); Files.deleteIfExists(dir) }
    }

    @Test fun malformedUnrelatedEntryAlsoBlocksReadsAndWritesWithoutReplacingTheFile() {
        val dir = Files.createTempDirectory("video-resume-unrelated-corruption")
        val path = dir.resolve("video-playback.json")
        val invalid = """{"version":1,"entries":{"mp4:other":{"positionSeconds":-1,"durationSeconds":10}}}"""
        try {
            Files.writeString(path, invalid)
            val store = FileVideoResumeStore(path)
            assertFails { store.read("mp4:requested") }
            assertFails { store.write("mp4:requested", VideoResumeEntry(1.0, 10.0)) }
            assertEquals(invalid, Files.readString(path))
        } finally { Files.deleteIfExists(path); Files.deleteIfExists(dir) }
    }

    @Test fun storeRejectsInvalidNumbersAndReadsThroughDistinctInstancesSerially() {
        val dir = Files.createTempDirectory("video-resume-validation")
        val path = dir.resolve("video-playback.json")
        try {
            assertFails { FileVideoResumeStore(path).write("mp4:x", VideoResumeEntry(Double.NaN, 10.0)) }
            val a = FileVideoResumeStore(path)
            val b = FileVideoResumeStore(path)
            a.write("mp4:a", VideoResumeEntry(1.0, 10.0))
            b.write("mp4:b", VideoResumeEntry(2.0, 20.0))
            assertEquals(VideoResumeEntry(1.0, 10.0), a.read("mp4:a"))
            assertEquals(VideoResumeEntry(2.0, 20.0), b.read("mp4:b"))
        } finally { Files.deleteIfExists(path); Files.deleteIfExists(dir) }
    }

    @Test fun restoreClampsChangedDurationAndPlaybackUpdatesKeepBackwardSeekAndEndedSemantics() {
        assertEquals(29.9, restoredVideoPosition(VideoResumeEntry(120.0, 300.0), 30.0))
        assertEquals(0.0, restoredVideoPosition(null, 30.0))
        val backwardSeek = VideoPageState("", true, true, 4, false, mediaTime = 3.0, duration = 50.0)
        assertEquals(VideoResumeEntry(3.0, 50.0), videoPositionToPersist(backwardSeek))
        assertEquals(VideoResumeEntry(0.0, 50.0), videoPositionToPersist(backwardSeek.copy(mediaTime = 49.9, ended = true)))
        assertNull(videoPositionToPersist(backwardSeek.copy(mediaTime = Double.POSITIVE_INFINITY)))
    }

    @Test fun sourceSpecificRestoreHooksUseMp4MetadataAndOfficialYoutubeApiWithoutPlaying() {
        val mp4 = videoResumeSeekScript(youtube = false, seconds = 12.5)
        assertTrue(mp4.contains("v.readyState<1"))
        assertTrue(mp4.contains("v.currentTime=12.5"))
        assertFalse(mp4.contains(".play()"))
        val youtube = videoResumeSeekScript(youtube = true, seconds = 12.5, youtubeId = "Gbtma_UQpro")
        assertTrue(youtube.contains("p.seekTo(12.5,true)"))
        assertTrue(youtube.contains("p.cueVideoById({videoId:'Gbtma_UQpro',startSeconds:12.5})"))
        assertFalse(youtube.contains("playVideo"))
        assertFails { videoResumeSeekScript(youtube = false, seconds = Double.NaN) }
        assertFalse(videoSeekWasDispatched("false", null))
        assertFalse(videoSeekWasDispatched("true", IllegalStateException("timeout")))
        assertTrue(videoSeekWasDispatched("\"true\"", null))
        assertFalse(videoRestorePositionConfirmed(80.0, 0.0, 100.0, youtube = false))
        assertTrue(videoRestorePositionConfirmed(80.0, 80.1, 100.0, youtube = false))
        assertFalse(videoRestorePositionConfirmed(0.2, 0.0, 2.0, youtube = false))
        assertFalse(videoRestoreTimedOut(TimeUnit.SECONDS.toNanos(10), 0L))
        assertTrue(videoRestoreTimedOut(TimeUnit.SECONDS.toNanos(11), 0L))
    }
}
