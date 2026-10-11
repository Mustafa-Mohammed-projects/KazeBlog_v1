package com.kaze.blog.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

object Media {
    private fun dir(ctx: Context): File = File(ctx.filesDir, "media").also { it.mkdirs() }
    fun file(ctx: Context, name: String): File = File(dir(ctx), name)

    /** Copies a picked image into app storage (max 1600px, JPEG, EXIF rotation applied). */
    fun importImage(ctx: Context, uri: Uri): String? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val maxSide = 1600
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return null

            val rotation = try {
                ctx.contentResolver.openInputStream(uri)?.use {
                    when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                } ?: 0f
            } catch (e: Exception) { 0f }

            val scale = min(1f, maxSide.toFloat() / max(bmp.width, bmp.height))
            if (scale < 1f || rotation != 0f) {
                val m = Matrix().apply { postScale(scale, scale); postRotate(rotation) }
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            }
            val name = "img_${System.currentTimeMillis()}.jpg"
            FileOutputStream(file(ctx, name)).use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            name
        } catch (e: Throwable) {
            null
        }
    }

    fun savePng(ctx: Context, bmp: Bitmap): String {
        val name = "draw_${System.currentTimeMillis()}.png"
        FileOutputStream(file(ctx, name)).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return name
    }

    /** Loads an attachment, down-sampled to roughly [targetWidth] pixels wide. */
    fun load(ctx: Context, name: String, targetWidth: Int): Bitmap? {
        return try {
            val f = file(ctx, name)
            if (!f.exists()) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.path, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= targetWidth) sample *= 2
            BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
        } catch (e: Throwable) {
            null
        }
    }

    fun delete(ctx: Context, name: String) {
        runCatching { file(ctx, name).delete() }
    }
}
