package com.lzacademy.app.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Loads a remote image (academy Supabase storage) as a [Painter] for [LZAvatar].
 *
 * Free-tier friendly: every photo is downloaded from the server AT MOST ONCE per
 * device — the decoded, downscaled bitmap is cached on disk ([Context.cacheDir]/
 * avatars) and reused across sessions, in addition to the in-memory cache. Returns
 * null while downloading and when the student has no photo; the initials avatar
 * stays the graceful offline fallback.
 */
@Composable
fun rememberRemoteAvatar(url: String?): Painter? {
    if (url == null) {
        android.util.Log.d("LZSync", "RemoteAvatar: url nulle (pas de photo)")
        return null
    }
    val context = LocalContext.current
    val bitmapState: State<ImageBitmap?> = produceState<ImageBitmap?>(initialValue = null, url) {
        android.util.Log.d("LZSync", "RemoteAvatar: chargement $url")
        value = memoryCache[url] ?: withContext(Dispatchers.IO) {
            loadOrDownload(context, url)?.also { memoryCache[url] = it }
        }
    }
    val bitmap = bitmapState.value
    if (bitmap != null) android.util.Log.d("LZSync", "RemoteAvatar: prêt $url")
    return remember(bitmap) { bitmap?.let { BitmapPainter(it) } }
}

private val memoryCache = java.util.concurrent.ConcurrentHashMap<String, ImageBitmap>()

/** 512 px max side — enough for an 84 dp avatar, ~30x lighter than the originals. */
private const val MAX_SIDE = 512

private fun loadOrDownload(context: Context, url: String): ImageBitmap? {
    val file = cacheFile(context, url)
    // 1) disk cache hit — zero server contact
    file?.takeIf { it.isFile && it.length() > 0 }?.let { file ->
        decodeScaled(file)?.let { return it.asImageBitmap() }
        file.delete()
    }
    // 2) one single download, then written to disk for every future session
    return runCatching {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            if (connection.responseCode !in 200..299) {
                android.util.Log.w("LZSync", "avatar http=${connection.responseCode} $url")
                return null
            }
            val bytes = connection.inputStream.use { it.readBytes() }
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val scaled = scaled(decoded)
            file?.parentFile?.mkdirs()
            file?.outputStream()?.use { scaled.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            scaled.asImageBitmap()
        } finally {
            connection.disconnect()
        }
    }.onFailure { android.util.Log.e("LZSync", "avatar échec: $url", it) }.getOrNull()
}

/**
 * Local-file photo (e.g. the admin profile photo stored in filesDir) as a [Painter].
 * Re-decodes only when the path or the file stamp changes; returns null without a file.
 */
@Composable
fun rememberLocalPhotoPainter(path: String?): Painter? {
    if (path == null) return null
    // Stamped on every composition (not remembered): when the file is overwritten in place
    // (same path, e.g. the admin photo), lastModified changes and the bitmap is re-decoded.
    val stamp = File(path).takeIf { it.isFile }?.lastModified() ?: 0L
    val bitmapState = produceState<ImageBitmap?>(initialValue = null, path, stamp) {
        value = withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(path) }.getOrNull()?.asImageBitmap()
        }
    }
    val bitmap = bitmapState.value
    return remember(bitmap) { bitmap?.let { BitmapPainter(it) } }
}

private fun cacheFile(context: Context, url: String): File? = runCatching {
    val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
    val name = digest.joinToString("") { "%02x".format(it) }.take(32) + ".jpg"
    File(File(context.cacheDir, "avatars"), name)
}.getOrNull()

private fun decodeScaled(file: File): Bitmap? = runCatching {
    BitmapFactory.decodeFile(file.absolutePath)
}.getOrNull()?.let { scaled(it) }

private fun scaled(source: Bitmap): Bitmap {
    val ratio = maxOf(source.width, source.height).toFloat() / MAX_SIDE
    if (ratio <= 1f) return source
    return Bitmap.createScaledBitmap(
        source,
        (source.width / ratio).toInt().coerceAtLeast(1),
        (source.height / ratio).toInt().coerceAtLeast(1),
        true,
    )
}
