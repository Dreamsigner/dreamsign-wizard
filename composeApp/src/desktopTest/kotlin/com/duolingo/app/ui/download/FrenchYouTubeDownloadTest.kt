package com.duolingo.app.ui.download

import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FrenchYouTubeDownloadTest {
    private val metadata = """{"id":"Gbtma_UQpro","formats":[
      {"format_id":"18","ext":"mp4","vcodec":"avc1","acodec":"mp4a.40.2","height":360},
      {"format_id":"137","ext":"mp4","vcodec":"avc1.640028","acodec":"none","height":1080},
      {"format_id":"140","ext":"m4a","vcodec":"none","acodec":"mp4a.40.2","language":"en-US","abr":128},
      {"format_id":"250","ext":"webm","vcodec":"none","acodec":"opus","language":"fr-FR","abr":70},
      {"format_id":"251","ext":"webm","vcodec":"none","acodec":"opus","language":"fr_CA","abr":160}
    ]}"""

    @Test fun selectsFrenchRegionalAudioAndHighestMp4VideoDespiteEnglishDefault() {
        val selected = assertIs<TrackInspection.Available>(selectFrenchTracks(metadata))
        assertEquals(AudioVideoFormats(videoId = "137", audioId = "251"), selected.formats)
    }

    @Test fun frenchAbsenceIsDistinctFromInvalidOrFailedExtraction() {
        val englishOnly = metadata.replace("\"language\":\"fr-FR\"", "\"language\":\"en-GB\"")
            .replace("\"language\":\"fr_CA\"", "\"language\":\"en-CA\"")
        assertIs<TrackInspection.FrenchAudioMissing>(selectFrenchTracks(englishOnly))
        assertFailsWith<IllegalArgumentException> { selectFrenchTracks("{not-json") }
        assertEquals(
            "Échec réseau pendant la lecture des pistes YouTube. Réessaie plus tard.",
            youtubeFailureMessage("ERROR: Unable to download API page: connection reset"),
        )
        assertTrue(youtubeFailureMessage("Sign in to confirm you’re not a bot").contains("bloqué"))
    }

    @Test fun formatIdentifiersAreValidatedBeforeTheyCanEnterProcessArguments() {
        val hostile = metadata.replace("\"format_id\":\"137\"", "\"format_id\":\"--output=/tmp/evil\"")
        val selected = assertIs<TrackInspection.Available>(selectFrenchTracks(hostile))
        assertEquals("18", selected.formats.videoId)
    }

    @Test fun inspectionUsesValidatedCanonicalUrlAndIgnoresUserYtDlpConfiguration() = runBlocking {
        val runner = FakeRunner(metadata)
        assertIs<TrackInspection.Available>(service(runner).inspect("Gbtma_UQpro"))
        val args = runner.calls.single()
        assertTrue(args.contains("--ignore-config"))
        assertTrue(args.contains("--no-playlist"))
        assertFalse(args.any { it.contains("cookies") || it.contains("browser") })
        assertEquals("https://www.youtube.com/watch?v=Gbtma_UQpro", args.last())
        assertFailsWith<IllegalArgumentException> { service(runner).inspect("../../bad") }
        Unit
    }

    @Test fun finalMp4MustContainVideoAndFrenchTaggedAudio() {
        val valid = """{"format":{"format_name":"mov,mp4,m4a"},"streams":[{"codec_type":"video"},{"codec_type":"audio","tags":{"language":"fra"}}]}"""
        assertTrue(verifiedFrenchMp4(valid))
        assertFalse(verifiedFrenchMp4(valid.replace("fra", "eng")))
        assertFalse(verifiedFrenchMp4("""{"format":{"format_name":"mp4"},"streams":[{"codec_type":"audio","tags":{"language":"fra"}}]}"""))
    }

    @Test fun downloadPinsFrenchAudioFormatAndKeepsExistingDestinationWithoutConfirmation() = runBlocking {
        val root = Files.createTempDirectory("french-video-download-")
        try {
            val target = root.resolve("video-fr.mp4")
            Files.writeString(target, "keep me")
            val runner = FakeRunner(metadata)
            assertFailsWith<java.nio.file.FileAlreadyExistsException> {
                service(runner).download("Gbtma_UQpro", availableFormats(), target, overwriteConfirmed = false) {}
            }
            assertEquals("keep me", Files.readString(target))
            assertTrue(runner.calls.isEmpty())

            Files.delete(target)
            val progress = mutableListOf<Int>()
            val result = service(runner).download("Gbtma_UQpro", availableFormats(), target, overwriteConfirmed = false) { progress += it }
            assertEquals(target, result)
            assertEquals("downloaded French video", Files.readString(target))
            assertTrue(progress.contains(50) && progress.contains(100))
            val ytDlpCalls = runner.calls.filter { it.first() == "yt-dlp.exe" }
            assertEquals(2, ytDlpCalls.size)
            assertTrue(ytDlpCalls.all { it.contains("--ignore-config") && it.contains("--no-playlist") })
            assertTrue(ytDlpCalls[0].contains("137"))
            assertTrue(ytDlpCalls[1].contains("251[language~='(?i)^fr($|[-_])']"))
            assertFalse(ytDlpCalls.any { "best" in it })
            val ffmpegArgs = runner.calls.first { it.first() == "ffmpeg.exe" }
            assertTrue(ffmpegArgs.containsAll(listOf("-map", "0:v:0", "1:a:0", "language=fra")))
            assertTrue(ffmpegArgs.last().endsWith(".mp4"))
            assertFalse(Files.list(root).use { it.anyMatch { path -> path.fileName.toString().startsWith(".atelier-video-fr-") } })
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun extractionFailureDoesNotCreateOrReportACompletedFile() = runBlocking {
        val root = Files.createTempDirectory("french-video-failed-")
        try {
            val target = root.resolve("out.mp4")
            val runner = FakeRunner(metadata, downloadFailure = CommandResult(1, "", "ERROR: timed out"))
            val failure = assertFailsWith<YouTubeCommandException> {
                service(runner).download("Gbtma_UQpro", availableFormats(), target, overwriteConfirmed = false) {}
            }
            assertTrue(failure.message!!.contains("Échec réseau"))
            assertFalse(Files.exists(target))
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun cancellationRemovesItsTemporaryFilesAndDoesNotPublishPartialOutput() = runBlocking {
        val root = Files.createTempDirectory("french-video-cancel-")
        val enteredDownload = CountDownLatch(1)
        try {
            val target = root.resolve("out.mp4")
            val runner = FakeRunner(metadata, blockOnDownload = enteredDownload)
            val job = async(kotlinx.coroutines.Dispatchers.Default) { service(runner).download("Gbtma_UQpro", availableFormats(), target, overwriteConfirmed = false) {} }
            assertTrue(enteredDownload.await(5, TimeUnit.SECONDS))
            job.cancelAndJoin()
            assertFalse(Files.exists(target))
            assertFalse(Files.list(root).use { it.anyMatch { path -> path.fileName.toString().startsWith(".atelier-video-fr-") } })
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun errorClassifierNeverShowsRawSignedUrlOrDiagnosticText() {
        val message = youtubeFailureMessage("ERROR: https://googlevideo.com/videoplayback?sig=SECRET timed out")
        assertTrue(message.contains("Échec réseau"))
        assertFalse("SECRET" in message)
        assertFalse("googlevideo" in message)
    }

    private fun service(runner: DownloadCommandRunner) = FrenchYouTubeDownloadService(
        DownloadTools(Path.of("yt-dlp.exe"), Path.of("ffmpeg.exe"), Path.of("ffprobe.exe")), runner,
    )

    private fun availableFormats() = assertIs<TrackInspection.Available>(selectFrenchTracks(metadata)).formats

    private class FakeRunner(
        private val metadata: String,
        private val blockOnDownload: CountDownLatch? = null,
        private val downloadFailure: CommandResult? = null,
    ) : DownloadCommandRunner {
        val calls = mutableListOf<List<String>>()

        override fun run(arguments: List<String>, timeoutMillis: Long, onStdout: (String) -> Unit): CommandResult {
            calls += arguments
            if ("--dump-single-json" in arguments) return CommandResult(0, metadata, "")
            when (arguments.first()) {
                "ffmpeg.exe" -> {
                    Files.writeString(Path.of(arguments.last()), "downloaded French video")
                    return CommandResult(0, "", "")
                }
                "ffprobe.exe" -> return CommandResult(0, """{"format":{"format_name":"mov,mp4,m4a"},"streams":[{"codec_type":"video"},{"codec_type":"audio","tags":{"language":"fra"}}]}""", "")
            }
            blockOnDownload?.countDown()
            if (blockOnDownload != null) {
                try { CountDownLatch(1).await() } catch (interrupted: InterruptedException) { Thread.currentThread().interrupt(); throw interrupted }
            }
            downloadFailure?.let { return it }
            val extension = if (arguments.any { "language~=" in it }) "webm" else "mp4"
            val output = Path.of(arguments[arguments.indexOf("--output") + 1]).toString().replace("%(ext)s", extension)
            Files.writeString(Path.of(output), "track")
            onStdout("__ATELIER_PROGRESS__100.0%")
            return CommandResult(0, "", "")
        }
    }
}
