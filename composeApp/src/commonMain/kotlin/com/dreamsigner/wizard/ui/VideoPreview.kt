package com.dreamsigner.wizard.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamsigner.wizard.data.CourseResource
import kotlinx.coroutines.CancellationException

/** Only known YouTube video URL forms may produce a remote thumbnail request. */
internal fun youtubeVideoId(url: String): String? {
    val match = Regex("""https://(?:www\.)?youtube\.com/watch\?v=([A-Za-z0-9_-]{11})(?:&[A-Za-z0-9_=&%-]*)?""").matchEntire(url)
        ?: Regex("""https://youtu\.be/([A-Za-z0-9_-]{11})(?:\?[A-Za-z0-9_=&%-]*)?""").matchEntire(url)
    return match?.groupValues?.get(1)
}

fun interface VideoThumbnailLoader {
    suspend fun load(videoId: String): ImageBitmap?
}

private val LocalVideoThumbnailLoader = staticCompositionLocalOf<VideoThumbnailLoader> { VideoThumbnailLoader { null } }

@Composable
fun VideoThumbnailProvider(loader: VideoThumbnailLoader, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalVideoThumbnailLoader provides loader, content = content)
}

@Composable
internal fun VideoPreview(resource: CourseResource, onPlay: (() -> Unit)? = null, onOpen: () -> Unit) {
    val loader = LocalVideoThumbnailLoader.current
    val videoId = remember(resource.url) { youtubeVideoId(resource.url) }
    var loading by remember(resource.url, loader) { mutableStateOf(videoId != null) }
    var image by remember(resource.url, loader) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(resource.url, loader) {
        image = try { videoId?.let { loader.load(it) } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { null }
        loading = false
    }
    Surface(shape = LearningShape, color = Paper, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(16.dp)) {
            val compact = maxWidth < 540.dp
            val poster: @Composable () -> Unit = {
                Box(Modifier.then(if (compact) Modifier.fillMaxWidth() else Modifier.width(224.dp))
                    .aspectRatio(16f / 9f).background(Ink).testTag("video-poster"), contentAlignment = Alignment.Center) {
                    image?.let { Image(it, contentDescription = "Aperçu de la vidéo : ${resource.title.substringBeforeLast(" — ", resource.title)}",
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
                    if (image == null) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = White, modifier = Modifier.size(40.dp))
                        Text(if (loading) "Chargement de l’aperçu…" else "Aperçu non disponible", color = White, fontSize = 13.sp)
                    }
                }
            }
            val details: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (videoId != null) "Vidéo · YouTube" else "Vidéo · fichier MP4", color = ActionRed, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(resource.title.substringBeforeLast(" — ", resource.title), color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 25.sp)
                    Text("${if (resource.language == "fr") "Français" else "Anglais"} · Disponible online", color = Graphite, fontSize = 13.sp)
                    if (resource.notes.isNotBlank()) Text(resource.notes, color = Graphite)
                    if (onPlay != null) Button(onPlay, modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = ActionRed, contentColor = White)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp)); Text("Lire dans l’app")
                    }
                    Text("Ouverture dans le navigateur", color = Graphite, fontSize = 13.sp)
                    Button(onClick = onOpen, modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = ActionRed, contentColor = White)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ouvrir la vidéo")
                    }
                }
            }
            if (compact) Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { poster(); details() }
            else Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                poster(); Box(Modifier.weight(1f)) { details() }
            }
        }
    }
}
