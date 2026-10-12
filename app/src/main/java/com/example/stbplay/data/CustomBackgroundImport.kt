package com.example.stbplay.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Local app-owned copy: the source photo and its permissions are never modified. */
internal suspend fun importCustomBackground(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Choose a readable image." }
    val options = BitmapFactory.Options().apply {
        inSampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / inSampleSize > 1920) inSampleSize *= 2
    }
    val bitmap = requireNotNull(context.contentResolver.openInputStream(uri).use {
        BitmapFactory.decodeStream(it, null, options)
    }) { "Could not read this image." }
    val orientation = runCatching {
        context.contentResolver.openInputStream(uri).use { stream ->
            android.media.ExifInterface(requireNotNull(stream)).getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)
        }
    }.getOrDefault(android.media.ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix().apply {
        when (orientation) {
            2 -> setScale(-1f, 1f)
            3 -> setRotate(180f)
            4 -> setScale(1f, -1f)
            5 -> { setRotate(90f); postScale(-1f, 1f) }
            6 -> setRotate(90f)
            7 -> { setRotate(270f); postScale(-1f, 1f) }
            8 -> setRotate(270f)
        }
    }
    val rotated = if (matrix.isIdentity) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    val directory = File(context.filesDir, "backgrounds").apply { check(mkdirs() || isDirectory) }
    val output = File(directory, "background-${UUID.randomUUID()}.jpg")
    try {
        output.outputStream().use { check(rotated.compress(Bitmap.CompressFormat.JPEG, 85, it)) }
        output
    } catch (error: Exception) {
        output.delete()
        throw error
    } finally {
        if (rotated !== bitmap) rotated.recycle()
        bitmap.recycle()
    }
}
