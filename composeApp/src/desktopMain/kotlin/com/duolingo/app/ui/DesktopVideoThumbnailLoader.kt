package com.duolingo.app.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.HttpURLConnection
import java.net.URI
import java.io.ByteArrayInputStream
import java.io.InputStream
import javax.imageio.ImageIO
import org.jetbrains.skia.Image as SkiaImage

/** Small, session-only LRU cache. No cookies, API key, disk writes or video download. */
class DesktopVideoThumbnailLoader internal constructor(
    private val fetch: (String) -> ByteArray = ::fetchVideoThumbnail,
) : VideoThumbnailLoader {
    private val mutex = Mutex()
    private val cache = LinkedHashMap<String, ImageBitmap>(8, 0.75f, true)
    override suspend fun load(videoId: String): ImageBitmap? {
        if (!Regex("[A-Za-z0-9_-]{11}").matches(videoId)) return null
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                cache[videoId] ?: runCatching {
                    val bytes = fetch("https://i.ytimg.com/vi/$videoId/hqdefault.jpg")
                    require(bytes.size <= MAX_THUMBNAIL_BYTES)
                    // Read dimensions before allocating decoded pixels.
                    ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { input ->
                        requireNotNull(input)
                        val readers = ImageIO.getImageReaders(input)
                        require(readers.hasNext())
                        val reader = readers.next()
                        try {
                            reader.input = input
                            require(reader.getWidth(0) in 1..1280 && reader.getHeight(0) in 1..720)
                        } finally { reader.dispose() }
                    }
                    SkiaImage.makeFromEncoded(bytes).use { it.toComposeImageBitmap() }.also {
                        cache[videoId] = it
                        if (cache.size > 8) cache.remove(cache.keys.first())
                    }
                }.getOrNull()
            }
        }
    }
}

internal const val MAX_THUMBNAIL_BYTES = 1_048_576
internal fun readThumbnailBytes(input: InputStream): ByteArray {
    val bytes = input.readNBytes(MAX_THUMBNAIL_BYTES + 1)
    require(bytes.size <= MAX_THUMBNAIL_BYTES) { "Thumbnail too large" }
    return bytes
}

private fun fetchVideoThumbnail(url: String): ByteArray {
    val connection = URI(url).toURL().openConnection() as HttpURLConnection
    try {
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 4000
        connection.readTimeout = 4000
        connection.setRequestProperty("Accept", "image/jpeg")
        connection.setRequestProperty("User-Agent", "AtelierApprentissage/1.2")
        require(connection.responseCode == 200)
        require(connection.contentType?.substringBefore(';') == "image/jpeg")
        require(connection.contentLengthLong <= MAX_THUMBNAIL_BYTES)
        return connection.inputStream.use(::readThumbnailBytes)
    } finally { connection.disconnect() }
}
