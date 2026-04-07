/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package moe.koiverse.archivetune.utils

import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
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
import android.view.PixelCopy
import androidx.core.view.drawToBitmap
import moe.koiverse.archivetune.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

object ComposeToImage {

    private tailrec fun Context.findActivity(): Activity? {
        return when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivity()
            else -> null
        }
    }

    private fun ensureSoftwareBitmap(bitmap: Bitmap): Bitmap {
        val config = bitmap.config
        if (config != Bitmap.Config.HARDWARE && config != null) return bitmap
        return runCatching { bitmap.copy(Bitmap.Config.ARGB_8888, false) }.getOrNull() ?: bitmap
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    private suspend fun pixelCopyViewBitmap(view: View): Bitmap? {
        if (!view.isAttachedToWindow || view.width <= 0 || view.height <= 0) return null
        val activity = view.context.findActivity() ?: return null
        val window = activity.window ?: return null

        val location = IntArray(2)
        view.getLocationInWindow(location)
        val rect = Rect(
            location[0],
            location[1],
            location[0] + view.width,
            location[1] + view.height
        )

        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val copyResult =
            suspendCancellableCoroutine { cont ->
                PixelCopy.request(
                    window,
                    rect,
                    bitmap,
                    { result -> cont.resume(result) },
                    Handler(Looper.getMainLooper()),
                )
            }
        return if (copyResult == PixelCopy.SUCCESS) bitmap else null
    }

    suspend fun captureViewBitmap(
        view: View,
        targetWidth: Int? = null,
        targetHeight: Int? = null,
        backgroundColor: Int? = null,
    ): Bitmap {
        val fallbackBitmap = runCatching {
            view.drawToBitmap()
        }.getOrElse {
            val w = view.width.coerceAtLeast(1)
            val h = view.height.coerceAtLeast(1)
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bmp ->
                backgroundColor?.let { Canvas(bmp).drawColor(it) }
            }
        }

        val original =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                pixelCopyViewBitmap(view) ?: fallbackBitmap
            } else {
                fallbackBitmap
            }
        val needsScale =
            (targetWidth != null && targetWidth > 0 && targetWidth != original.width) ||
            (targetHeight != null && targetHeight > 0 && targetHeight != original.height)
        val base = if (needsScale) {
            val safeOriginal = ensureSoftwareBitmap(original)
            val tw = targetWidth ?: original.width
            val th = targetHeight ?: (original.height * tw / original.width)
            ensureSoftwareBitmap(Bitmap.createScaledBitmap(safeOriginal, tw, th, true))
        } else {
            ensureSoftwareBitmap(original)
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
        secondaryTextColor: Int? = null,
        glassStyle: moe.koiverse.archivetune.ui.component.LyricsGlassStyle? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        val style = glassStyle ?: moe.koiverse.archivetune.ui.component.LyricsGlassStyle.FrostedDark
        val cardSize = minOf(width, height) - 32
        val bitmap = createBitmap(cardSize, cardSize)
        val canvas = Canvas(bitmap)

        val mainTextColor = textColor
            ?: style.textColor.let {
                ((it.alpha * 255).toInt() shl 24) or
                ((it.red * 255).toInt() shl 16) or
                ((it.green * 255).toInt() shl 8) or
                (it.blue * 255).toInt()
            }
        val secondaryTxtColor = secondaryTextColor
            ?: style.secondaryTextColor.let {
                ((it.alpha * 255).toInt() shl 24) or
                ((it.red * 255).toInt() shl 16) or
                ((it.green * 255).toInt() shl 8) or
                (it.blue * 255).toInt()
            }
        val bgColor = backgroundColor ?: 0xFF121212.toInt()

        var coverArtBitmap: Bitmap? = null
        if (coverArtUrl != null) {
            try {
                val imageLoader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(coverArtUrl)
                    .size(512)
                    .allowHardware(false)
                    .build()
                val result = imageLoader.execute(request)
                coverArtBitmap = result.image?.toBitmap()
            } catch (_: Exception) {}
        }

        val outerCornerRadius = cardSize * 0.06f

        val scaledArt = if (coverArtBitmap != null) {
            ensureSoftwareBitmap(
                Bitmap.createScaledBitmap(coverArtBitmap, cardSize, cardSize, true)
            )
        } else null

        val artPath = Path().apply {
            addRoundRect(
                RectF(0f, 0f, cardSize.toFloat(), cardSize.toFloat()),
                outerCornerRadius, outerCornerRadius,
                Path.Direction.CW
            )
        }

        if (scaledArt != null) {
            canvas.withClip(artPath) {
                drawBitmap(scaledArt, 0f, 0f, null)
            }
        } else {
            val bgPaint = Paint().apply { color = bgColor; isAntiAlias = true }
            canvas.drawRoundRect(
                RectF(0f, 0f, cardSize.toFloat(), cardSize.toFloat()),
                outerCornerRadius, outerCornerRadius, bgPaint
            )
        }

        val dimPaint = Paint().apply {
            color = android.graphics.Color.argb(
                (style.backgroundDimAlpha * 255).toInt(), 0, 0, 0
            )
            isAntiAlias = true
        }
        canvas.drawRoundRect(
            RectF(0f, 0f, cardSize.toFloat(), cardSize.toFloat()),
            outerCornerRadius, outerCornerRadius, dimPaint
        )

        val glassMargin = cardSize * 0.04f
        val glassLeft = glassMargin
        val glassTop = glassMargin
        val glassRight = cardSize - glassMargin
        val glassBottom = cardSize - glassMargin
        val glassWidth = glassRight - glassLeft
        val glassHeight = glassBottom - glassTop
        val glassCornerRadius = cardSize * 0.05f

        val glassRect = RectF(glassLeft, glassTop, glassRight, glassBottom)
        val glassPath = Path().apply {
            addRoundRect(glassRect, glassCornerRadius, glassCornerRadius, Path.Direction.CW)
        }

        if (scaledArt != null) {
            val cropLeft = glassLeft.toInt().coerceIn(0, cardSize - 1)
            val cropTop = glassTop.toInt().coerceIn(0, cardSize - 1)
            val cropWidth = glassWidth.toInt().coerceIn(1, cardSize - cropLeft)
            val cropHeight = glassHeight.toInt().coerceIn(1, cardSize - cropTop)
            val glassCrop = Bitmap.createBitmap(scaledArt, cropLeft, cropTop, cropWidth, cropHeight)
            val frostedCrop = scaleBlur(glassCrop, 12)
            canvas.withClip(glassPath) {
                drawBitmap(frostedCrop, glassLeft, glassTop, null)
            }
        }

        val glassBgPaint = Paint().apply {
            color = style.surfaceTint.let {
                android.graphics.Color.argb(
                    (style.surfaceAlpha * 255).toInt(),
                    (it.red * 255).toInt(),
                    (it.green * 255).toInt(),
                    (it.blue * 255).toInt()
                )
            }
            isAntiAlias = true
        }
        canvas.drawRoundRect(glassRect, glassCornerRadius, glassCornerRadius, glassBgPaint)

        val overlayPaint = Paint().apply {
            color = style.overlayColor.let {
                android.graphics.Color.argb(
                    (style.overlayAlpha * 255).toInt(),
                    (it.red * 255).toInt(),
                    (it.green * 255).toInt(),
                    (it.blue * 255).toInt()
                )
            }
            isAntiAlias = true
        }
        canvas.drawRoundRect(glassRect, glassCornerRadius, glassCornerRadius, overlayPaint)

        val borderPaint = Paint().apply {
            this.style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = android.graphics.Color.argb(25, 255, 255, 255)
            isAntiAlias = true
        }
        canvas.drawRoundRect(glassRect, glassCornerRadius, glassCornerRadius, borderPaint)

        val contentPadding = glassWidth * 0.08f
        val contentLeft = glassLeft + contentPadding
        val contentTop = glassTop + contentPadding
        val contentRight = glassRight - contentPadding

        val imageCornerRadius = cardSize * 0.035f
        val coverSize = glassWidth * 0.16f

        coverArtBitmap?.let {
            val rect = RectF(contentLeft, contentTop, contentLeft + coverSize, contentTop + coverSize)
            val path = Path().apply {
                addRoundRect(rect, imageCornerRadius, imageCornerRadius, Path.Direction.CW)
            }
            canvas.withClip(path) {
                drawBitmap(it, null, rect, null)
            }
            val artBorderPaint = Paint().apply {
                this.style = Paint.Style.STROKE
                strokeWidth = 1f
                color = android.graphics.Color.argb(38, 255, 255, 255)
                isAntiAlias = true
            }
            canvas.drawRoundRect(rect, imageCornerRadius, imageCornerRadius, artBorderPaint)
        }

        val titlePaint = TextPaint().apply {
            color = mainTextColor
            textSize = cardSize * 0.038f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            letterSpacing = -0.02f
        }
        val artistPaint = TextPaint().apply {
            color = secondaryTxtColor
            textSize = cardSize * 0.028f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val textMaxWidth = (contentRight - contentLeft - coverSize - cardSize * 0.04f).toInt()
        val textStartX = contentLeft + coverSize + cardSize * 0.04f

        val titleLayout = StaticLayout.Builder.obtain(songTitle, 0, songTitle.length, titlePaint, textMaxWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(1)
            .build()
        val artistLayout = StaticLayout.Builder.obtain(artistName, 0, artistName.length, artistPaint, textMaxWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(1)
            .build()

        val imageCenter = contentTop + coverSize / 2f
        val textBlockHeight = titleLayout.height + artistLayout.height + 6f
        val textBlockY = imageCenter - textBlockHeight / 2f

        canvas.withTranslation(textStartX, textBlockY) {
            titleLayout.draw(this)
            translate(0f, titleLayout.height.toFloat() + 6f)
            artistLayout.draw(this)
        }

        val lyricsPaint = TextPaint().apply {
            color = mainTextColor
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            letterSpacing = -0.01f
        }

        val lyricsMaxWidth = (glassWidth * 0.85f).toInt()
        val logoBlockHeight = (glassHeight * 0.08f).toInt()
        val lyricsTop = glassTop + glassHeight * 0.22f
        val lyricsBottom = glassBottom - (logoBlockHeight + contentPadding)
        val availableLyricsHeight = lyricsBottom - lyricsTop

        var lyricsTextSize = cardSize * 0.055f
        var lyricsLayout: StaticLayout
        do {
            lyricsPaint.textSize = lyricsTextSize
            lyricsLayout = StaticLayout.Builder.obtain(
                lyrics, 0, lyrics.length, lyricsPaint, lyricsMaxWidth
            )
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setIncludePad(false)
                .setLineSpacing(8f, 1.35f)
                .setMaxLines(10)
                .build()
            if (lyricsLayout.height > availableLyricsHeight) {
                lyricsTextSize -= 2f
            } else {
                break
            }
        } while (lyricsTextSize > 22f)

        val lyricsYOffset = lyricsTop + (availableLyricsHeight - lyricsLayout.height) / 2f
        canvas.withTranslation(glassLeft + (glassWidth - lyricsMaxWidth) / 2f, lyricsYOffset) {
            lyricsLayout.draw(this)
        }

        AppLogo(
            context = context,
            canvas = canvas,
            canvasWidth = cardSize,
            canvasHeight = cardSize,
            padding = glassLeft + contentPadding,
            bottomPadding = glassBottom - contentPadding,
            circleColor = secondaryTxtColor,
            logoTint = if (style.isDark) 0xDD000000.toInt() else 0xE6FFFFFF.toInt(),
            textColor = secondaryTxtColor,
        )

        return@withContext bitmap
    }

    private fun scaleBlur(source: Bitmap, strength: Int): Bitmap {
        val safe = ensureSoftwareBitmap(source)
        val factor = (1f / strength.coerceAtLeast(1)).coerceAtLeast(0.02f)
        val smallW = (safe.width * factor).toInt().coerceAtLeast(1)
        val smallH = (safe.height * factor).toInt().coerceAtLeast(1)
        val downscaled = Bitmap.createScaledBitmap(safe, smallW, smallH, true)
        return Bitmap.createScaledBitmap(downscaled, safe.width, safe.height, true)
    }

    private fun AppLogo(
        context: Context,
        canvas: Canvas,
        canvasWidth: Int,
        canvasHeight: Int,
        padding: Float,
        bottomPadding: Float = canvasHeight - padding,
        circleColor: Int,
        logoTint: Int,
        textColor: Int,
    ) {
        val baseSize = minOf(canvasWidth, canvasHeight).toFloat()
        val logoSize = (baseSize * 0.045f).toInt()

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
            textSize = baseSize * 0.028f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            isAntiAlias = true
            letterSpacing = 0.02f
        }

        val circleRadius = logoSize * 0.55f
        val circleX = padding + circleRadius
        val circleY = bottomPadding - circleRadius
        val logoX = circleX - logoSize / 2f
        val logoY = circleY - logoSize / 2f
        val textX = padding + circleRadius * 2 + 10f
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
        val safeBitmap = ensureSoftwareBitmap(bitmap)
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
                safeBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            uri
        } else {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val imageFile = File(cachePath, "$fileName.png")
            FileOutputStream(imageFile).use { outputStream ->
                safeBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.FileProvider",
                imageFile
            )
        }
    }
}
