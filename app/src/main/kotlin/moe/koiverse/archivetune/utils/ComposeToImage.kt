/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import android.view.View
import androidx.core.view.drawToBitmap
import moe.koiverse.archivetune.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ComposeToImage {

    private fun ensureSoftwareBitmap(bitmap: Bitmap): Bitmap {
        return if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
    }

    fun captureViewBitmap(
        view: View,
        targetWidth: Int? = null,
        targetHeight: Int? = null,
        backgroundColor: Int? = null,
    ): Bitmap {
        val original = ensureSoftwareBitmap(view.drawToBitmap())
        val needsScale =
            (targetWidth != null && targetWidth > 0 && targetWidth != original.width) ||
            (targetHeight != null && targetHeight > 0 && targetHeight != original.height)
        val base = if (needsScale) {
            val tw = targetWidth ?: original.width
            val th = targetHeight ?: (original.height * tw / original.width)
            ensureSoftwareBitmap(Bitmap.createScaledBitmap(original, tw, th, true))
        } else {
            original
        }
        if (backgroundColor != null) {
            val out = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
            val c = Canvas(out)
            c.drawColor(backgroundColor)
            c.drawBitmap(base, 0f, 0f, null)
            return out
        }
        return base
    }

    fun cropBitmap(source: Bitmap, left: Int, top: Int, width: Int, height: Int): Bitmap {
        val safeSource = ensureSoftwareBitmap(source)
        val safeLeft = left.coerceIn(0, safeSource.width.coerceAtLeast(1) - 1)
        val safeTop = top.coerceIn(0, safeSource.height.coerceAtLeast(1) - 1)
        val safeWidth = width.coerceIn(1, safeSource.width - safeLeft)
        val safeHeight = height.coerceIn(1, safeSource.height - safeTop)
        return ensureSoftwareBitmap(Bitmap.createBitmap(safeSource, safeLeft, safeTop, safeWidth, safeHeight))
    }

    fun fitBitmap(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        backgroundColor: Int,
    ): Bitmap {
        val safeSource = ensureSoftwareBitmap(source)
        val out = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(backgroundColor)

        val scale = minOf(
            targetWidth.toFloat() / safeSource.width.coerceAtLeast(1),
            targetHeight.toFloat() / safeSource.height.coerceAtLeast(1),
        )
        val scaledW = (safeSource.width * scale).toInt().coerceAtLeast(1)
        val scaledH = (safeSource.height * scale).toInt().coerceAtLeast(1)
        val scaled = if (scaledW != safeSource.width || scaledH != safeSource.height) {
            ensureSoftwareBitmap(Bitmap.createScaledBitmap(safeSource, scaledW, scaledH, true))
        } else {
            safeSource
        }

        val dx = ((targetWidth - scaled.width) / 2f)
        val dy = ((targetHeight - scaled.height) / 2f)
        canvas.drawBitmap(scaled, dx, dy, null)
        return out
    }

    @RequiresApi(Build.VERSION_CODES.M)
    suspend fun createLyricsImage(
        context: Context,
        coverArtUrl: String?,
        songTitle: String,
        artistName: String,
        lyrics: String,
        width: Int,
        height: Int,
        backgroundColor: Int? = null,
        textColor: Int? = null,
        secondaryTextColor: Int? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        val cardSize = minOf(width, height) - 32
        val bitmap = createBitmap(cardSize, cardSize)
        val canvas = Canvas(bitmap)

        val defaultBackgroundColor = 0xFF121212.toInt()
        val defaultTextColor = 0xFFFFFFFF.toInt()
        val defaultSecondaryTextColor = 0xB3FFFFFF.toInt()

        val bgColor = backgroundColor ?: defaultBackgroundColor
        val mainTextColor = textColor ?: defaultTextColor
        val secondaryTxtColor = secondaryTextColor ?: defaultSecondaryTextColor

        val backgroundPaint = Paint().apply {
            color = bgColor
            isAntiAlias = true
        }
        val cornerRadius = 20f
        val backgroundRect = RectF(0f, 0f, cardSize.toFloat(), cardSize.toFloat())
        canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, backgroundPaint)

        var coverArtBitmap: Bitmap? = null
        if (coverArtUrl != null) {
            try {
                val imageLoader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(coverArtUrl)
                    .size(256)
                    .allowHardware(false)
                    .build()
                val result = imageLoader.execute(request)
                coverArtBitmap = result.image?.toBitmap()
            } catch (_: Exception) {}
        }

        val padding = 32f
        val imageCornerRadius = 12f

        val coverArtSize = cardSize * 0.15f
        coverArtBitmap?.let {
            val rect = RectF(padding, padding, padding + coverArtSize, padding + coverArtSize)
            val path = Path().apply {
                addRoundRect(rect, imageCornerRadius, imageCornerRadius, Path.Direction.CW)
            }
            canvas.withClip(path) {
                drawBitmap(it, null, rect, null)
            }
        }

        val titlePaint = TextPaint().apply {
            color = mainTextColor
            textSize = cardSize * 0.040f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val artistPaint = TextPaint().apply {
            color = secondaryTxtColor
            textSize = cardSize * 0.030f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val textMaxWidth = cardSize - (padding * 2 + coverArtSize + 16f)
        val textStartX = padding + coverArtSize + 16f

        val titleLayout = StaticLayout.Builder.obtain(songTitle, 0, songTitle.length, titlePaint, textMaxWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(1)
            .build()
        val artistLayout = StaticLayout.Builder.obtain(artistName, 0, artistName.length, artistPaint, textMaxWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(1)
            .build()

        val imageCenter = padding + coverArtSize / 2f
        val textBlockHeight = titleLayout.height + artistLayout.height + 8f
        val textBlockY = imageCenter - textBlockHeight / 2f

        canvas.withTranslation(textStartX, textBlockY) {
            titleLayout.draw(this)
            translate(0f, titleLayout.height.toFloat() + 8f)
            artistLayout.draw(this)
        }

        val lyricsPaint = TextPaint().apply {
            color = mainTextColor
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            letterSpacing = 0.02f
        }

        val lyricsMaxWidth = (cardSize * 0.85f).toInt()
        val logoBlockHeight = (cardSize * 0.08f).toInt()
        val lyricsTop = cardSize * 0.18f
        val lyricsBottom = cardSize - (logoBlockHeight + 32)
        val availableLyricsHeight = lyricsBottom - lyricsTop

        var lyricsTextSize = cardSize * 0.06f
        var lyricsLayout: StaticLayout
        do {
            lyricsPaint.textSize = lyricsTextSize
            lyricsLayout = StaticLayout.Builder.obtain(
                lyrics, 0, lyrics.length, lyricsPaint, lyricsMaxWidth
            )
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setIncludePad(false)
                .setLineSpacing(10f, 1.3f)
                .setMaxLines(10)
                .build()
            if (lyricsLayout.height > availableLyricsHeight) {
                lyricsTextSize -= 2f
            } else {
                break
            }
        } while (lyricsTextSize > 26f)
        val lyricsYOffset = lyricsTop + (availableLyricsHeight - lyricsLayout.height) / 2f

        canvas.withTranslation((cardSize - lyricsMaxWidth) / 2f, lyricsYOffset) {
            lyricsLayout.draw(this)
        }

        AppLogo(
            context = context,
            canvas = canvas,
            canvasWidth = cardSize,
            canvasHeight = cardSize,
            padding = padding,
            circleColor = secondaryTxtColor,
            logoTint = bgColor,
            textColor = secondaryTxtColor,
        )

        return@withContext bitmap
    }

    private fun AppLogo(
        context: Context,
        canvas: Canvas,
        canvasWidth: Int,
        canvasHeight: Int,
        padding: Float,
        circleColor: Int,
        logoTint: Int,
        textColor: Int,
    ) {
        val baseSize = minOf(canvasWidth, canvasHeight).toFloat()
        val logoSize = (baseSize * 0.05f).toInt()

        val rawLogo = context.getDrawable(R.drawable.small_icon)?.toBitmap(logoSize, logoSize)
        val logo = rawLogo?.let { source ->
            val colored = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val canvasLogo = Canvas(colored)
            val paint = Paint().apply {
                colorFilter = PorterDuffColorFilter(logoTint, PorterDuff.Mode.SRC_IN)
                isAntiAlias = true
            }
            canvasLogo.drawBitmap(source, 0f, 0f, paint)
            colored
        }

        val appName = context.getString(R.string.app_name)
        val appNamePaint = TextPaint().apply {
            color = textColor
            textSize = baseSize * 0.030f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            isAntiAlias = true
            letterSpacing = 0.01f
        }

        val circleRadius = logoSize * 0.55f
        val logoX = padding + circleRadius - logoSize / 2f
        val logoY = canvasHeight - padding - circleRadius - logoSize / 2f
        val circleX = padding + circleRadius
        val circleY = canvasHeight - padding - circleRadius
        val textX = padding + circleRadius * 2 + 12f
        val textY = circleY + appNamePaint.textSize * 0.3f

        val circlePaint = Paint().apply {
            color = circleColor
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawCircle(circleX, circleY, circleRadius, circlePaint)

        logo?.let {
            canvas.drawBitmap(it, logoX, logoY, null)
        }

        canvas.drawText(appName, textX, textY, appNamePaint)
    }

    fun saveBitmapAsFile(context: Context, bitmap: Bitmap, fileName: String): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.png")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ArchiveTune")
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw IllegalStateException("Failed to create new MediaStore record")

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            uri
        } else {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val imageFile = File(cachePath, "$fileName.png")
            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.FileProvider",
                imageFile
            )
        }
    }
}
