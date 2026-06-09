package moe.rukamori.archivetune.ui.component

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import kotlin.math.min

object BitmapBlur {

    fun blur(bitmap: Bitmap, radius: Int): Bitmap {
        val r = radius.coerceIn(1, 100)
        val scale = 0.25f
        val sw = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val sh = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, sw, sh, true)

        val pixels = IntArray(sw * sh)
        scaled.getPixels(pixels, 0, sw, 0, 0, sw, sh)

        val scaledRadius = (r * scale).toInt().coerceIn(1, 50)
        val blurred = boxBlur(pixels, sw, sh, scaledRadius)
        val blurred2 = boxBlur(blurred, sw, sh, scaledRadius)
        val blurred3 = boxBlur(blurred2, sw, sh, scaledRadius)

        val result = Bitmap.createBitmap(sw, sh, Config.ARGB_8888)
        result.setPixels(blurred3, 0, sw, 0, 0, sw, sh)
        return result
    }

    private fun boxBlur(pixels: IntArray, w: Int, h: Int, radius: Int): IntArray {
        if (radius <= 0) return pixels
        val out = pixels.copyOf()
        val div = radius * 2 + 1

        for (y in 0 until h) {
            for (x in 0 until w) {
                var ar = 0; var ag = 0; var ab = 0; var aa = 0
                for (dx in -radius..radius) {
                    val sx = (x + dx).coerceIn(0, w - 1)
                    val p = out[y * w + sx]
                    aa += (p shr 24) and 0xFF
                    ar += (p shr 16) and 0xFF
                    ag += (p shr 8) and 0xFF
                    ab += p and 0xFF
                }
                val idx = y * w + x
                out[idx] = (aa / div) shl 24 or (ar / div) shl 16 or (ag / div) shl 8 or (ab / div)
            }
        }

        for (y in 0 until h) {
            for (x in 0 until w) {
                var ar = 0; var ag = 0; var ab = 0; var aa = 0
                for (dy in -radius..radius) {
                    val sy = (y + dy).coerceIn(0, h - 1)
                    val p = out[sy * w + x]
                    aa += (p shr 24) and 0xFF
                    ar += (p shr 16) and 0xFF
                    ag += (p shr 8) and 0xFF
                    ab += p and 0xFF
                }
                val idx = y * w + x
                out[idx] = (aa / div) shl 24 or (ar / div) shl 16 or (ag / div) shl 8 or (ab / div)
            }
        }

        return out
    }
}
