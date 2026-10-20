package com.duolingo.app.media

import androidx.compose.runtime.staticCompositionLocalOf
import duolingo_kmp_learning_app.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Media boundary shared by practice UI and its isolated tests.
 *
 * Player/recorder creation and lifecycle binding run on the UI loop. Their callbacks must
 * follow PlatformAudioPlayer/PlatformAudioRecorder contracts, including no completion on
 * stop/cancel. Resource loading is suspending and must cooperate with cancellation.
 * Recording storage is blocking; the UI calls it on its existing worker dispatcher.
 * Implementations used in tests must replace all methods, including lifecycle binding,
 * so composing a practice screen cannot touch native devices, permissions or user files.
 */
interface PracticeMedia {
    fun createPlayer(): PlatformAudioPlayer
    fun createRecorder(): PlatformAudioRecorder
    suspend fun loadResource(path: String): ByteArray
    fun saveRecording(id: String, bytes: ByteArray)
    fun loadRecording(id: String): ByteArray?
    fun bindPlayerLifecycle(player: PlatformAudioPlayer, onBackground: () -> Unit): () -> Unit
    fun bindRecorderLifecycle(recorder: PlatformAudioRecorder, onBackground: () -> Unit): () -> Unit
}

/** The default has no eager native initialization; only invoked methods touch the platform. */
object NativePracticeMedia : PracticeMedia {
    override fun createPlayer(): PlatformAudioPlayer = createAudioPlayer()
    override fun createRecorder(): PlatformAudioRecorder = createAudioRecorder()

    // Bundled raw resources; no runtime network or speech service.
    // https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-usage.html#raw-files
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun loadResource(path: String): ByteArray = Res.readBytes(path)

    override fun saveRecording(id: String, bytes: ByteArray) = saveAudioRecording(id, bytes)
    override fun loadRecording(id: String): ByteArray? = loadAudioRecording(id)
    override fun bindPlayerLifecycle(player: PlatformAudioPlayer, onBackground: () -> Unit): () -> Unit =
        bindAudioLifecycle(player, onBackground)
    override fun bindRecorderLifecycle(recorder: PlatformAudioRecorder, onBackground: () -> Unit): () -> Unit =
        bindAudioRecorderLifecycle(recorder, onBackground)
}

// Stable environment dependency, replaceable for the whole practice subtree.
// https://developer.android.com/develop/ui/compose/compositionlocal#create-compositionlocal
val LocalPracticeMedia = staticCompositionLocalOf<PracticeMedia> { NativePracticeMedia }
