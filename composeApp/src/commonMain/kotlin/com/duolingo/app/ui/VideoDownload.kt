package com.duolingo.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.duolingo.app.data.CourseResource

/** Desktop implementation for downloading a YouTube video with an existing French audio track. */
interface VideoDownloader {
    @Composable fun Download(resource: CourseResource, onClose: () -> Unit)
}

internal val LocalVideoDownloader = staticCompositionLocalOf<VideoDownloader?> { null }

@Composable
fun VideoDownloadProvider(downloader: VideoDownloader, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalVideoDownloader provides downloader, content = content)
}
