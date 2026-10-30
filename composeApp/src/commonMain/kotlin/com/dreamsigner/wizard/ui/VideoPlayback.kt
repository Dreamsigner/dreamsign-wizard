package com.dreamsigner.wizard.ui

import androidx.compose.runtime.*
import com.dreamsigner.wizard.data.CourseResource

interface VideoPlayer {
    @Composable fun Player(resource: CourseResource, onClose: () -> Unit)
}
internal val LocalVideoPlayer = staticCompositionLocalOf<VideoPlayer?> { null }

@Composable
fun VideoPlaybackProvider(player: VideoPlayer, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalVideoPlayer provides player, content = content)
}
