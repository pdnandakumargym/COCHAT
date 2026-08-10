package com.cochat.app.util

import android.content.Context
import android.net.Uri
import java.io.File

/** Copies a content:// picker Uri into a cache file so OkHttp can stream it as multipart. */
fun uriToCacheFile(context: Context, uri: Uri): File? {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri) ?: "application/octet-stream"
    val extension = mimeType.substringAfterLast('/', "bin")
    val displayName = queryDisplayName(context, uri) ?: "upload_${System.currentTimeMillis()}.$extension"

    val outFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}_$displayName")
    return try {
        resolver.openInputStream(uri)?.use { input ->
            outFile.outputStream().use { output -> input.copyTo(output) }
        }
        outFile
    } catch (e: Exception) {
        null
    }
}

fun mimeTypeOf(context: Context, uri: Uri): String =
    context.contentResolver.getType(uri) ?: "application/octet-stream"

private fun queryDisplayName(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
    cursor.use {
        val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && it.moveToFirst()) return it.getString(idx)
    }
    return null
}
