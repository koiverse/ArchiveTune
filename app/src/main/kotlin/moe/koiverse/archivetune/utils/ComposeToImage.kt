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
import android.text.TextUtils
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
import moe.koiverse.archivetune.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ComposeToImage {

    data class YearInMusicImageColors(
        val background: Int,
        val surface: Int,
        val surfaceVariant: Int,
        val onSurface: Int,
        val onSurfaceVariant: Int,
        val primary: Int,
        val secondary: Int,
        val tertiary: Int,
        val outline: Int,
        val onPrimary: Int,
    ) {
        companion object {
            fun default(): YearInMusicImageColors {
                val background = 0xFF0D1117.toInt()
                return YearInMusicImageColors(
                    background = background,
                    surface = 0xFF141B24.toInt(),
                    surfaceVariant = 0xFF1B2432.toInt(),
                    onSurface = 0xFFFFFFFF.toInt(),
                    onSurfaceVariant = 0xFFB6C2D1.toInt(),
                    primary = 0xFF58A6FF.toInt(),
                    secondary = 0xFF8B949E.toInt(),
                    tertiary = 0xFF238636.toInt(),
                    outline = 0x33FFFFFF.toInt(),
                    onPrimary = background,
                )
            }
        }
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

    @RequiresApi(Build.VERSION_CODES.M)
    suspend fun createYearInMusicImage(
        context: Context,
        year: Int,
        totalListeningTime: Long,
        topSongs: List<moe.koiverse.archivetune.db.entities.SongWithStats>,
        topArtists: List<moe.koiverse.archivetune.db.entities.Artist>,
        colors: YearInMusicImageColors? = null,
    ): Bitmap = withContext(Dispatchers.Default) {
        val cardWidth = 1080
        val cardHeight = 1920
        val bitmap = createBitmap(cardWidth, cardHeight)
        val canvas = Canvas(bitmap)

        val c = colors ?: YearInMusicImageColors.default()
        val contentPadding = 72f
        val cornerRadius = 44f

        val backgroundPaint = Paint().apply {
            color = c.background
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(), backgroundPaint)

        drawRadialGlow(
            canvas = canvas,
            color = c.primary,
            cx = cardWidth * 0.18f,
            cy = cardHeight * 0.14f,
            radius = cardWidth * 1.05f,
            alpha = 110,
        )
        drawRadialGlow(
            canvas = canvas,
            color = c.tertiary,
            cx = cardWidth * 1.02f,
            cy = cardHeight * 0.22f,
            radius = cardWidth * 0.95f,
            alpha = 95,
        )
        drawRadialGlow(
            canvas = canvas,
            color = c.secondary,
            cx = cardWidth * 0.92f,
            cy = cardHeight * 0.92f,
            radius = cardWidth * 1.25f,
            alpha = 60,
        )

        val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cardWidth * 0.5f,
                cardHeight * 0.45f,
                cardHeight * 0.85f,
                intArrayOf(setAlpha(c.background, 0), setAlpha(c.background, 210)),
                floatArrayOf(0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(), vignettePaint)

        var yOffset = 96f
        val contentWidth = (cardWidth - contentPadding * 2).toInt()

        val badgeText = context.getString(R.string.app_name)
        val badgeTextPaint = TextPaint().apply {
            color = setAlpha(c.onSurfaceVariant, 220)
            textSize = 26f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            isAntiAlias = true
            letterSpacing = 0.08f
        }
        val badgeHeight = 48f
        val badgeWidth = badgeTextPaint.measureText(badgeText) + 38f
        val badgeRect = RectF(
            contentPadding,
            yOffset,
            contentPadding + badgeWidth,
            yOffset + badgeHeight,
        )
        drawCard(
            canvas = canvas,
            rect = badgeRect,
            cornerRadius = badgeHeight * 0.55f,
            fillColor = setAlpha(c.surface, 215),
            borderColor = setAlpha(c.outline, 160),
        )
        canvas.drawText(
            badgeText,
            badgeRect.left + 19f,
            badgeRect.centerY() + badgeTextPaint.textSize * 0.35f,
            badgeTextPaint,
        )
        yOffset = badgeRect.bottom + 32f

        val titlePaint = TextPaint().apply {
            color = c.onSurface
            textSize = 72f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            isAntiAlias = true
            letterSpacing = -0.015f
        }
        val titleText = runCatching {
            context.getString(R.string.your_year_in_music, year)
        }.getOrElse {
            "$year ${context.getString(R.string.year_in_music)}"
        }
        val titleHeight = drawTextLayout(
            canvas = canvas,
            text = titleText,
            paint = titlePaint,
            width = contentWidth,
            x = contentPadding,
            y = yOffset,
            maxLines = 2,
        )
        yOffset += titleHeight + 22f

        val timeText = makeTimeStringForImage(totalListeningTime)
        val timePaint = TextPaint().apply {
            textSize = 104f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            isAntiAlias = true
            shader = LinearGradient(
                contentPadding,
                yOffset,
                contentPadding + cardWidth * 0.7f,
                yOffset + textSize,
                intArrayOf(c.primary, c.tertiary, c.secondary),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawText(timeText, contentPadding, yOffset + timePaint.textSize, timePaint)
        yOffset += timePaint.textSize + 14f

        val labelPaint = TextPaint().apply {
            color = setAlpha(c.onSurfaceVariant, 230)
            textSize = 34f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText(
            context.getString(R.string.total_listening_time),
            contentPadding,
            yOffset + labelPaint.textSize,
            labelPaint,
        )
        yOffset += labelPaint.textSize + 34f

        val chipTextPaint = TextPaint().apply {
            color = setAlpha(c.onSurface, 235)
            textSize = 28f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            isAntiAlias = true
        }
        val chipHeight = 50f
        val chipGap = 16f
        val chipTexts = listOf(
            context.getString(R.string.top_songs) + "  " + topSongs.size,
            context.getString(R.string.top_artists) + "  " + topArtists.size,
        )
        var chipX = contentPadding
        chipTexts.forEach { chipText ->
            val chipWidth = chipTextPaint.measureText(chipText) + 30f
            val chipRect = RectF(chipX, yOffset, chipX + chipWidth, yOffset + chipHeight)
            drawCard(
                canvas = canvas,
                rect = chipRect,
                cornerRadius = chipHeight * 0.5f,
                fillColor = setAlpha(c.surface, 205),
                borderColor = setAlpha(c.outline, 140),
            )
            canvas.drawText(
                chipText,
                chipRect.left + 15f,
                chipRect.centerY() + chipTextPaint.textSize * 0.36f,
                chipTextPaint,
            )
            chipX = chipRect.right + chipGap
        }
        yOffset += chipHeight + 34f

        val imageLoader = ImageLoader(context)

        if (topSongs.isNotEmpty()) {
            val songsToRender = topSongs.take(5)
            val itemHeight = 128f
            val itemGap = 16f
            val cardInnerPadding = 32f
            val headerHeight = 66f
            val cardHeight = cardInnerPadding * 2 + headerHeight + songsToRender.size * itemHeight + (songsToRender.size - 1).coerceAtLeast(0) * itemGap

            val cardRect = RectF(
                contentPadding,
                yOffset,
                cardWidth - contentPadding,
                yOffset + cardHeight,
            )
            drawCard(
                canvas = canvas,
                rect = cardRect,
                cornerRadius = cornerRadius,
                fillColor = setAlpha(c.surface, 220),
                borderColor = setAlpha(c.outline, 170),
            )

            val sectionPaint = TextPaint().apply {
                color = setAlpha(c.onSurface, 245)
                textSize = 40f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                isAntiAlias = true
            }
            val sectionAccentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    cardRect.left,
                    cardRect.top,
                    cardRect.left + 220f,
                    cardRect.top,
                    intArrayOf(setAlpha(c.primary, 230), setAlpha(c.tertiary, 230)),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP,
                )
            }
            val accentRect = RectF(cardRect.left + cardInnerPadding, cardRect.top + cardInnerPadding + 20f, cardRect.left + cardInnerPadding + 80f, cardRect.top + cardInnerPadding + 28f)
            canvas.drawRoundRect(accentRect, 8f, 8f, sectionAccentPaint)
            canvas.drawText(
                context.getString(R.string.top_songs),
                cardRect.left + cardInnerPadding,
                cardRect.top + cardInnerPadding + 52f,
                sectionPaint,
            )

            val titlePaintRow = TextPaint().apply {
                color = setAlpha(c.onSurface, 245)
                textSize = 34f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                isAntiAlias = true
            }
            val subPaintRow = TextPaint().apply {
                color = setAlpha(c.onSurfaceVariant, 230)
                textSize = 26f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                isAntiAlias = true
            }
            val badgeNumPaint = TextPaint().apply {
                color = c.onPrimary
                textSize = 20f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                isAntiAlias = true
            }
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = setAlpha(c.outline, 120)
                strokeWidth = 2f
            }

            var rowY = cardRect.top + cardInnerPadding + headerHeight
            val thumbSize = 92f
            val thumbCorner = 22f
            songsToRender.forEachIndexed { index, song ->
                val thumbRect = RectF(cardRect.left + cardInnerPadding, rowY, cardRect.left + cardInnerPadding + thumbSize, rowY + thumbSize)
                val thumbPath = Path().apply { addRoundRect(thumbRect, thumbCorner, thumbCorner, Path.Direction.CW) }

                var songBitmap: Bitmap? = null
                song.thumbnailUrl?.let { url ->
                    runCatching {
                        val request = ImageRequest.Builder(context)
                            .data(url)
                            .size(thumbSize.toInt())
                            .allowHardware(false)
                            .build()
                        imageLoader.execute(request).image?.toBitmap()
                    }.getOrNull()?.let { songBitmap = it }
                }

                if (songBitmap != null) {
                    canvas.withClip(thumbPath) {
                        drawBitmap(songBitmap!!, null, thumbRect, null)
                    }
                } else {
                    val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = setAlpha(c.surfaceVariant, 240) }
                    canvas.drawRoundRect(thumbRect, thumbCorner, thumbCorner, placeholderPaint)
                }

                val badgeRadius = 16f
                val badgeCx = thumbRect.left + 18f
                val badgeCy = thumbRect.top + 18f
                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = setAlpha(c.primary, 235) }
                canvas.drawCircle(badgeCx, badgeCy, badgeRadius, badgePaint)
                val badgeText = "${index + 1}"
                val badgeTextWidth = badgeNumPaint.measureText(badgeText)
                canvas.drawText(
                    badgeText,
                    badgeCx - badgeTextWidth / 2f,
                    badgeCy + badgeNumPaint.textSize * 0.36f,
                    badgeNumPaint,
                )

                val textX = thumbRect.right + 22f
                val maxTextWidth = (cardRect.right - cardInnerPadding - textX).toInt()
                val titleText = song.title
                val truncatedTitle = truncateText(titleText, titlePaintRow, maxTextWidth.toFloat())
                canvas.drawText(truncatedTitle, textX, rowY + 38f, titlePaintRow)

                val playCount = "${song.songCountListened} plays • ${makeTimeStringForImage(song.timeListened ?: 0L)}"
                canvas.drawText(playCount, textX, rowY + 76f, subPaintRow)

                if (index != songsToRender.lastIndex) {
                    val dividerY = rowY + itemHeight + 4f
                    canvas.drawLine(
                        cardRect.left + cardInnerPadding,
                        dividerY,
                        cardRect.right - cardInnerPadding,
                        dividerY,
                        dividerPaint,
                    )
                }

                rowY += itemHeight + itemGap
            }
            yOffset = cardRect.bottom + 26f
        }

        if (topArtists.isNotEmpty()) {
            val artistsToRender = topArtists.take(5)
            val itemHeight = 120f
            val itemGap = 16f
            val cardInnerPadding = 32f
            val headerHeight = 66f
            val cardHeight = cardInnerPadding * 2 + headerHeight + artistsToRender.size * itemHeight + (artistsToRender.size - 1).coerceAtLeast(0) * itemGap

            val cardRect = RectF(
                contentPadding,
                yOffset,
                cardWidth - contentPadding,
                yOffset + cardHeight,
            )
            drawCard(
                canvas = canvas,
                rect = cardRect,
                cornerRadius = cornerRadius,
                fillColor = setAlpha(c.surface, 215),
                borderColor = setAlpha(c.outline, 170),
            )

            val sectionPaint = TextPaint().apply {
                color = setAlpha(c.onSurface, 245)
                textSize = 40f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                isAntiAlias = true
            }
            val sectionAccentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    cardRect.left,
                    cardRect.top,
                    cardRect.left + 240f,
                    cardRect.top,
                    intArrayOf(setAlpha(c.secondary, 230), setAlpha(c.primary, 230)),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP,
                )
            }
            val accentRect = RectF(cardRect.left + cardInnerPadding, cardRect.top + cardInnerPadding + 20f, cardRect.left + cardInnerPadding + 80f, cardRect.top + cardInnerPadding + 28f)
            canvas.drawRoundRect(accentRect, 8f, 8f, sectionAccentPaint)
            canvas.drawText(
                context.getString(R.string.top_artists),
                cardRect.left + cardInnerPadding,
                cardRect.top + cardInnerPadding + 52f,
                sectionPaint,
            )

            val artistPaint = TextPaint().apply {
                color = setAlpha(c.onSurface, 245)
                textSize = 34f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                isAntiAlias = true
            }
            val artistSubPaint = TextPaint().apply {
                color = setAlpha(c.onSurfaceVariant, 230)
                textSize = 26f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                isAntiAlias = true
            }
            val badgeNumPaint = TextPaint().apply {
                color = c.onPrimary
                textSize = 20f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                isAntiAlias = true
            }
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = setAlpha(c.outline, 120)
                strokeWidth = 2f
            }

            var rowY = cardRect.top + cardInnerPadding + headerHeight
            val avatarSize = 84f
            val avatarRadius = avatarSize / 2f
            artistsToRender.forEachIndexed { index, artist ->
                val avatarCx = cardRect.left + cardInnerPadding + avatarRadius
                val avatarCy = rowY + avatarRadius

                var artistBitmap: Bitmap? = null
                artist.artist.thumbnailUrl?.let { url ->
                    runCatching {
                        val request = ImageRequest.Builder(context)
                            .data(url)
                            .size(avatarSize.toInt())
                            .allowHardware(false)
                            .build()
                        imageLoader.execute(request).image?.toBitmap()
                    }.getOrNull()?.let { artistBitmap = it }
                }

                if (artistBitmap != null) {
                    val shader = BitmapShader(artistBitmap!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                    val matrix = Matrix().apply {
                        val scale = avatarSize / artistBitmap!!.width.coerceAtLeast(1).toFloat()
                        setScale(scale, scale)
                    }
                    shader.setLocalMatrix(matrix)
                    val avatarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
                    canvas.drawCircle(avatarCx, avatarCy, avatarRadius, avatarPaint)
                } else {
                    val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = setAlpha(c.surfaceVariant, 240) }
                    canvas.drawCircle(avatarCx, avatarCy, avatarRadius, placeholderPaint)
                }

                val badgeRadius = 15f
                val badgeCx = avatarCx - avatarRadius + 10f
                val badgeCy = avatarCy - avatarRadius + 10f
                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = setAlpha(c.secondary, 230) }
                canvas.drawCircle(badgeCx, badgeCy, badgeRadius, badgePaint)
                val badgeText = "${index + 1}"
                val badgeTextWidth = badgeNumPaint.measureText(badgeText)
                canvas.drawText(
                    badgeText,
                    badgeCx - badgeTextWidth / 2f,
                    badgeCy + badgeNumPaint.textSize * 0.36f,
                    badgeNumPaint,
                )

                val textX = cardRect.left + cardInnerPadding + avatarSize + 22f
                val maxTextWidth = (cardRect.right - cardInnerPadding - textX).toInt()
                val name = truncateText(artist.artist.name, artistPaint, maxTextWidth.toFloat())
                canvas.drawText(name, textX, rowY + 38f, artistPaint)

                val timeListened = artist.timeListened?.toLong() ?: 0L
                val statsText = "${artist.songCount} plays • ${makeTimeStringForImage(timeListened)}"
                canvas.drawText(statsText, textX, rowY + 74f, artistSubPaint)

                if (index != artistsToRender.lastIndex) {
                    val dividerY = rowY + itemHeight + 4f
                    canvas.drawLine(
                        cardRect.left + cardInnerPadding,
                        dividerY,
                        cardRect.right - cardInnerPadding,
                        dividerY,
                        dividerPaint,
                    )
                }

                rowY += itemHeight + itemGap
            }
            yOffset = cardRect.bottom + 22f
        }

        AppLogo(
            context = context,
            canvas = canvas,
            canvasWidth = cardWidth,
            canvasHeight = cardHeight,
            padding = contentPadding,
            circleColor = setAlpha(c.primary, 230),
            logoTint = c.onPrimary,
            textColor = setAlpha(c.onSurfaceVariant, 235),
        )

        return@withContext bitmap
    }

    private fun truncateText(text: String, paint: TextPaint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var truncated = text
        while (truncated.isNotEmpty() && paint.measureText("$truncated...") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        return "$truncated..."
    }

    private fun makeTimeStringForImage(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }

    private fun setAlpha(color: Int, alpha: Int): Int {
        return Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )
    }

    private fun drawRadialGlow(
        canvas: Canvas,
        color: Int,
        cx: Float,
        cy: Float,
        radius: Float,
        alpha: Int,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx,
                cy,
                radius,
                intArrayOf(setAlpha(color, alpha), setAlpha(color, 0)),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(cx, cy, radius, paint)
    }

    private fun drawCard(
        canvas: Canvas,
        rect: RectF,
        cornerRadius: Float,
        fillColor: Int,
        borderColor: Int,
    ) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun drawTextLayout(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        width: Int,
        x: Float,
        y: Float,
        maxLines: Int,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    ): Int {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(alignment)
            .setIncludePad(false)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setMaxLines(maxLines)
            .build()
        canvas.withTranslation(x, y) { layout.draw(this) }
        return layout.height
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
