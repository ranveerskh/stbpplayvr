package com.example.stbplay.ui.theme

import android.graphics.Bitmap
import coil.size.Size
import coil.transform.Transformation

/** Bounded, two-pass bitmap blur. Coil caches the result; no live rendering effects. */
internal class StaticBackgroundBlur(private val percent: Int) : Transformation {
    override val cacheKey = "stb-background-blur-v1:$percent"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        if (percent == 0) return input
        val scale = minOf(1f, 512f / maxOf(input.width, input.height))
        val width = maxOf(1, (input.width * scale).toInt())
        val height = maxOf(1, (input.height * scale).toInt())
        val bounded = Bitmap.createScaledBitmap(input, width, height, true)
        val pixels = IntArray(width * height)
        bounded.getPixels(pixels, 0, width, 0, 0, width, height)
        val horizontal = IntArray(pixels.size)
        val output = IntArray(pixels.size)
        val radius = if (percent == 50) 8 else 16
        fun pass(source: IntArray, target: IntArray, columns: Int, rows: Int, vertical: Boolean) {
            fun index(x: Int, y: Int) = if (vertical) x * width + y else y * width + x
            val count = radius * 2 + 1
            for (y in 0 until rows) {
                var a = 0; var r = 0; var g = 0; var b = 0
                fun add(x: Int, sign: Int) {
                    val color = source[index(x.coerceIn(0, columns - 1), y)]
                    a += (color ushr 24) * sign; r += ((color ushr 16) and 255) * sign
                    g += ((color ushr 8) and 255) * sign; b += (color and 255) * sign
                }
                for (x in -radius..radius) add(x, 1)
                for (x in 0 until columns) {
                    target[index(x, y)] = ((a / count) shl 24) or ((r / count) shl 16) or
                        ((g / count) shl 8) or (b / count)
                    add(x - radius, -1); add(x + radius + 1, 1)
                }
            }
        }
        pass(pixels, horizontal, width, height, false)
        pass(horizontal, output, height, width, true)
        if (bounded !== input) bounded.recycle()
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }
}
