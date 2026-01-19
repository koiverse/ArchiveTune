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
import kotlin.random.Random

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
        val outerCornerRadius = 54f
        val innerCornerRadius = 40f
        val footerReserve = 160f

        val backgroundPaint = Paint().apply {
            color = c.background
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(), backgroundPaint)

        val bgLum = colorLuminance(c.background)
        val glowAlphaBoost = if (bgLum >= 0.55) 1.15f else 1f
        drawRadialGlow(
            canvas = canvas,
            color = c.primary,
            cx = cardWidth * 0.16f,
            cy = cardHeight * 0.12f,
            radius = cardWidth * 1.12f,
            alpha = (120 * glowAlphaBoost).toInt().coerceIn(0, 255),
        )
        drawRadialGlow(
            canvas = canvas,
            color = c.secondary,
            cx = cardWidth * 1.04f,
            cy = cardHeight * 0.22f,
            radius = cardWidth * 0.92f,
            alpha = (105 * glowAlphaBoost).toInt().coerceIn(0, 255),
        )
        drawRadialGlow(
            canvas = canvas,
            color = c.tertiary,
            cx = cardWidth * 0.54f,
            cy = cardHeight * 0.56f,
            radius = cardWidth * 1.25f,
            alpha = (80 * glowAlphaBoost).toInt().coerceIn(0, 255),
        )
        drawRadialGlow(
            canvas = canvas,
            color = c.primary,
            cx = cardWidth * 0.92f,
            cy = cardHeight * 0.94f,
            radius = cardWidth * 1.35f,
            alpha = (55 * glowAlphaBoost).toInt().coerceIn(0, 255),
        )

        val baseOverlay = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                0f,
                cardHeight.toFloat(),
                intArrayOf(
                    setAlpha(c.surfaceVariant, if (bgLum >= 0.55) 38 else 30),
                    setAlpha(c.surfaceVariant, if (bgLum >= 0.55) 18 else 22),
                    setAlpha(c.background, 0),
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(), baseOverlay)

        val noise = createNoiseBitmap(240, year)
        val noisePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = if (bgLum >= 0.55) 24 else 16 }
        var nx = 0f
        while (nx < cardWidth) {
            var ny = 0f
            while (ny < cardHeight) {
                canvas.drawBitmap(noise, nx, ny, noisePaint)
                ny += noise.height
            }
            nx += noise.width
        }

        val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cardWidth * 0.55f,
                cardHeight * 0.35f,
                cardHeight * 0.95f,
                intArrayOf(setAlpha(c.background, 0), setAlpha(c.background, if (bgLum >= 0.55) 185 else 210)),
                floatArrayOf(0.52f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(), vignettePaint)

        var yOffset = 76f
        val contentWidth = (cardWidth - contentPadding * 2).toInt()
        val safeBottom = cardHeight - footerReserve

        val heroHeight = 410f
        val heroRect = RectF(
            contentPadding,
            yOffset,
            cardWidth - contentPadding,
            (yOffset + heroHeight).coerceAtMost(safeBottom - 20f),
        )
        drawCard(
            canvas = canvas,
            rect = heroRect,
            cornerRadius = outerCornerRadius,
            fillColor = setAlpha(c.surface, if (bgLum >= 0.55) 225 else 210),
            borderColor = setAlpha(c.outline, if (bgLum >= 0.55) 140 else 170),
            shadowBlur = 52f,
            shadowDy = 24f,
            shadowAlpha = if (bgLum >= 0.55) 65 else 85,
            highlightAlpha = 95,
        )

        val heroOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                heroRect.left,
                heroRect.top,
                heroRect.right,
                heroRect.bottom,
                intArrayOf(setAlpha(c.primary, 36), setAlpha(c.secondary, 22), setAlpha(c.tertiary, 30)),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(heroRect, outerCornerRadius, outerCornerRadius, heroOverlayPaint)

        val heroX = heroRect.left + 44f
        val heroTop = heroRect.top + 40f

        val appBadgeText = context.getString(R.string.app_name).uppercase()
        val appBadgePaint = TextPaint().apply {
            color = setAlpha(c.onSurfaceVariant, 210)
            textSize = 22f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            isAntiAlias = true
            letterSpacing = 0.12f
        }
        val appBadgeH = 44f
        val appBadgeW = appBadgePaint.measureText(appBadgeText) + 34f
        val appBadgeRect = RectF(heroX, heroTop, heroX + appBadgeW, heroTop + appBadgeH)
        drawCard(
            canvas = canvas,
            rect = appBadgeRect,
            cornerRadius = appBadgeH * 0.5f,
            fillColor = setAlpha(c.surfaceVariant, if (bgLum >= 0.55) 210 else 200),
            borderColor = setAlpha(c.outline, if (bgLum >= 0.55) 110 else 150),
            shadowBlur = 18f,
            shadowDy = 10f,
            shadowAlpha = 50,
            highlightAlpha = 70,
        )
        canvas.drawText(
            appBadgeText,
            appBadgeRect.left + 17f,
            appBadgeRect.centerY() + appBadgePaint.textSize * 0.36f,
            appBadgePaint,
        )

        val yearBadgePaint = TextPaint().apply {
            color = setAlpha(c.onSurface, 240)
            textSize = 28f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            isAntiAlias = true
        }
        val yearBadgeText = "$year"
        val yearBadgeW = yearBadgePaint.measureText(yearBadgeText) + 44f
        val yearBadgeRect = RectF(appBadgeRect.right + 14f, heroTop, appBadgeRect.right + 14f + yearBadgeW, heroTop + appBadgeH)
        val yearBadgeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                yearBadgeRect.left,
                yearBadgeRect.top,
                yearBadgeRect.right,
                yearBadgeRect.bottom,
                intArrayOf(setAlpha(c.primary, 200), setAlpha(c.secondary, 180)),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(yearBadgeRect, appBadgeH * 0.5f, appBadgeH * 0.5f, yearBadgeFill)
        val yearBadgeBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = setAlpha(c.outline, if (bgLum >= 0.55) 90 else 140)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(yearBadgeRect, appBadgeH * 0.5f, appBadgeH * 0.5f, yearBadgeBorder)
        canvas.drawText(
            yearBadgeText,
            yearBadgeRect.left + (yearBadgeW - yearBadgePaint.measureText(yearBadgeText)) / 2f,
            yearBadgeRect.centerY() + yearBadgePaint.textSize * 0.34f,
            yearBadgePaint,
        )

        val titleText = runCatching { context.getString(R.string.your_year_in_music, year) }.getOrElse {
            "${context.getString(R.string.year_in_music)} $year"
        }
        val titlePaint = TextPaint().apply {
            color = setAlpha(c.onSurface, 248)
            textSize = 64f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            isAntiAlias = true
            letterSpacing = -0.02f
        }
        val titleY = appBadgeRect.bottom + 26f
        val titleHeight = drawTextLayout(
            canvas = canvas,
            text = titleText,
            paint = titlePaint,
            width = (heroRect.right - heroX - 44f).toInt(),
            x = heroX,
            y = titleY,
            maxLines = 2,
        )

        val timeText = makeTimeStringForImage(totalListeningTime)
        val timePaint = TextPaint().apply {
            textSize = 112f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(20f, 0f, 10f, setAlpha(Color.BLACK, if (bgLum >= 0.55) 35 else 70))
            shader = LinearGradient(
                heroX,
                0f,
                heroRect.right - 44f,
                0f,
                intArrayOf(c.primary, c.tertiary, c.secondary),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        val timeY = titleY + titleHeight + 26f
        canvas.drawText(timeText, heroX, timeY + timePaint.textSize, timePaint)

        val timeLabelPaint = TextPaint().apply {
            color = setAlpha(c.onSurfaceVariant, 235)
            textSize = 30f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText(
            context.getString(R.string.total_listening_time),
            heroX,
            timeY + timePaint.textSize + 44f,
            timeLabelPaint,
        )

        val chipTextPaint = TextPaint().apply {
            color = setAlpha(c.onSurface, 245)
            textSize = 26f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            isAntiAlias = true
        }
        val chipHeight = 48f
        val chipGap = 14f
        val chipsY = (heroRect.bottom - 40f - chipHeight)
        val chipTexts = listOf(
            context.getString(R.string.top_songs) + "  " + topSongs.size,
            context.getString(R.string.top_artists) + "  " + topArtists.size,
        )
        var chipX = heroX
        chipTexts.forEach { chipText ->
            val chipWidth = chipTextPaint.measureText(chipText) + 34f
            val chipRect = RectF(chipX, chipsY, chipX + chipWidth, chipsY + chipHeight)
            drawCard(
                canvas = canvas,
                rect = chipRect,
                cornerRadius = chipHeight * 0.5f,
                fillColor = setAlpha(c.surfaceVariant, if (bgLum >= 0.55) 220 else 205),
                borderColor = setAlpha(c.outline, if (bgLum >= 0.55) 105 else 150),
                shadowBlur = 18f,
                shadowDy = 10f,
                shadowAlpha = 52,
                highlightAlpha = 70,
            )
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = setAlpha(c.primary, 220) }
            canvas.drawCircle(chipRect.left + 18f, chipRect.centerY(), 5.5f, dotPaint)
            canvas.drawText(
                chipText,
                chipRect.left + 30f,
                chipRect.centerY() + chipTextPaint.textSize * 0.36f,
                chipTextPaint,
            )
            chipX = chipRect.right + chipGap
        }

        yOffset = heroRect.bottom + 26f

        val imageLoader = ImageLoader(context)

        if (topSongs.isNotEmpty()) {
            val songsToRender = topSongs.take(5)
            val itemHeight = 96f
            val itemGap = 10f
            val cardInnerPadding = 28f
            val headerHeight = 54f
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
                cornerRadius = outerCornerRadius,
                fillColor = setAlpha(c.surface, if (bgLum >= 0.55) 225 else 215),
                borderColor = setAlpha(c.outline, if (bgLum >= 0.55) 135 else 175),
                shadowBlur = 44f,
                shadowDy = 20f,
                shadowAlpha = if (bgLum >= 0.55) 60 else 85,
                highlightAlpha = 92,
            )

            val sectionPaint = TextPaint().apply {
                color = setAlpha(c.onSurface, 245)
                textSize = 38f
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
            val accentRect = RectF(
                cardRect.left + cardInnerPadding,
                cardRect.top + cardInnerPadding + 18f,
                cardRect.left + cardInnerPadding + 96f,
                cardRect.top + cardInnerPadding + 28f,
            )
            canvas.drawRoundRect(accentRect, 10f, 10f, sectionAccentPaint)
            canvas.drawText(
                context.getString(R.string.top_songs),
                cardRect.left + cardInnerPadding,
                cardRect.top + cardInnerPadding + 52f,
                sectionPaint,
            )

            val titlePaintRow = TextPaint().apply {
                color = setAlpha(c.onSurface, 245)
                textSize = 32f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                isAntiAlias = true
            }
            val subPaintRow = TextPaint().apply {
                color = setAlpha(c.onSurfaceVariant, 230)
                textSize = 24f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                isAntiAlias = true
            }
            val badgeNumPaint = TextPaint().apply {
                color = c.onPrimary
                textSize = 20f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                isAntiAlias = true
            }
            val rowBgBorder = setAlpha(c.outline, if (bgLum >= 0.55) 95 else 135)
            val rowBgFill = setAlpha(c.surfaceVariant, if (bgLum >= 0.55) 215 else 205)
            val rowBgFill2 = setAlpha(c.surfaceVariant, if (bgLum >= 0.55) 200 else 190)
            val timeBadgePaint = TextPaint().apply {
                color = setAlpha(c.onSurface, 245)
                textSize = 22f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                isAntiAlias = true
            }

            var rowY = cardRect.top + cardInnerPadding + headerHeight
            val thumbSize = 76f
            val thumbCorner = 22f
            songsToRender.forEachIndexed { index, song ->
                val rowRect = RectF(
                    cardRect.left + cardInnerPadding,
                    rowY,
                    cardRect.right - cardInnerPadding,
                    rowY + itemHeight,
                )
                val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        rowRect.left,
                        rowRect.top,
                        rowRect.right,
                        rowRect.bottom,
                        intArrayOf(rowBgFill, rowBgFill2),
                        floatArrayOf(0f, 1f),
                        Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawRoundRect(rowRect, innerCornerRadius, innerCornerRadius, rowPaint)
                val rowBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = rowBgBorder
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                canvas.drawRoundRect(rowRect, innerCornerRadius, innerCornerRadius, rowBorderPaint)

                val thumbRect = RectF(rowRect.left + 14f, rowRect.top + (itemHeight - thumbSize) / 2f, rowRect.left + 14f + thumbSize, rowRect.top + (itemHeight - thumbSize) / 2f + thumbSize)
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

                val textX = thumbRect.right + 18f
                val timeBadgeText = makeTimeStringForImage(song.timeListened ?: 0L)
                val timeBadgeW = timeBadgePaint.measureText(timeBadgeText) + 26f
                val timeBadgeH = 42f
                val timeBadgeRect = RectF(
                    rowRect.right - 14f - timeBadgeW,
                    rowRect.centerY() - timeBadgeH / 2f,
                    rowRect.right - 14f,
                    rowRect.centerY() + timeBadgeH / 2f,
                )
                val timeBadgeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        timeBadgeRect.left,
                        timeBadgeRect.top,
                        timeBadgeRect.right,
                        timeBadgeRect.bottom,
                        intArrayOf(setAlpha(c.primary, 75), setAlpha(c.secondary, 55)),
                        floatArrayOf(0f, 1f),
                        Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawRoundRect(timeBadgeRect, timeBadgeH * 0.5f, timeBadgeH * 0.5f, timeBadgeFill)
                val timeBadgeBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = setAlpha(c.outline, if (bgLum >= 0.55) 90 else 130)
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                canvas.drawRoundRect(timeBadgeRect, timeBadgeH * 0.5f, timeBadgeH * 0.5f, timeBadgeBorder)
                canvas.drawText(
                    timeBadgeText,
                    timeBadgeRect.left + (timeBadgeW - timeBadgePaint.measureText(timeBadgeText)) / 2f,
                    timeBadgeRect.centerY() + timeBadgePaint.textSize * 0.36f,
                    timeBadgePaint,
                )

                val maxTextWidth = (timeBadgeRect.left - 14f - textX).toInt().coerceAtLeast(1)
                val truncatedTitle = truncateText(song.title, titlePaintRow, maxTextWidth.toFloat())
                canvas.drawText(truncatedTitle, textX, rowRect.top + 42f, titlePaintRow)
                canvas.drawText("${song.songCountListened} plays", textX, rowRect.top + 76f, subPaintRow)

                rowY += itemHeight + itemGap
            }
            yOffset = cardRect.bottom + 22f
        }

        if (topArtists.isNotEmpty()) {
            val artistsToRender = topArtists.take(5)
            val itemHeight = 88f
            val itemGap = 10f
            val cardInnerPadding = 28f
            val headerHeight = 54f
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
                cornerRadius = outerCornerRadius,
                fillColor = setAlpha(c.surface, if (bgLum >= 0.55) 225 else 212),
                borderColor = setAlpha(c.outline, if (bgLum >= 0.55) 135 else 175),
                shadowBlur = 44f,
                shadowDy = 20f,
                shadowAlpha = if (bgLum >= 0.55) 60 else 85,
                highlightAlpha = 92,
            )

            val sectionPaint = TextPaint().apply {
                color = setAlpha(c.onSurface, 245)
                textSize = 38f
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
            val accentRect = RectF(
                cardRect.left + cardInnerPadding,
                cardRect.top + cardInnerPadding + 18f,
                cardRect.left + cardInnerPadding + 96f,
                cardRect.top + cardInnerPadding + 28f,
            )
            canvas.drawRoundRect(accentRect, 10f, 10f, sectionAccentPaint)
            canvas.drawText(
                context.getString(R.string.top_artists),
                cardRect.left + cardInnerPadding,
                cardRect.top + cardInnerPadding + 52f,
                sectionPaint,
            )

            val artistPaint = TextPaint().apply {
                color = setAlpha(c.onSurface, 245)
                textSize = 32f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                isAntiAlias = true
            }
            val artistSubPaint = TextPaint().apply {
                color = setAlpha(c.onSurfaceVariant, 230)
                textSize = 24f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                isAntiAlias = true
            }
            val badgeNumPaint = TextPaint().apply {
                color = c.onPrimary
                textSize = 20f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                isAntiAlias = true
            }
            val rowBgBorder = setAlpha(c.outline, if (bgLum >= 0.55) 95 else 135)
            val rowBgFill = setAlpha(c.surfaceVariant, if (bgLum >= 0.55) 215 else 205)
            val rowBgFill2 = setAlpha(c.surfaceVariant, if (bgLum >= 0.55) 200 else 190)
            val timeBadgePaint = TextPaint().apply {
                color = setAlpha(c.onSurface, 245)
                textSize = 22f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                isAntiAlias = true
            }

            var rowY = cardRect.top + cardInnerPadding + headerHeight
            val avatarSize = 64f
            val avatarRadius = avatarSize / 2f
            artistsToRender.forEachIndexed { index, artist ->
                val rowRect = RectF(
                    cardRect.left + cardInnerPadding,
                    rowY,
                    cardRect.right - cardInnerPadding,
                    rowY + itemHeight,
                )
                val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        rowRect.left,
                        rowRect.top,
                        rowRect.right,
                        rowRect.bottom,
                        intArrayOf(rowBgFill, rowBgFill2),
                        floatArrayOf(0f, 1f),
                        Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawRoundRect(rowRect, innerCornerRadius, innerCornerRadius, rowPaint)
                val rowBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = rowBgBorder
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                canvas.drawRoundRect(rowRect, innerCornerRadius, innerCornerRadius, rowBorderPaint)

                val avatarCx = rowRect.left + 14f + avatarRadius
                val avatarCy = rowRect.centerY()

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
                val badgeCx = avatarCx - avatarRadius + 8f
                val badgeCy = avatarCy - avatarRadius + 8f
                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = setAlpha(c.secondary, 235) }
                canvas.drawCircle(badgeCx, badgeCy, badgeRadius, badgePaint)
                val badgeText = "${index + 1}"
                val badgeTextWidth = badgeNumPaint.measureText(badgeText)
                canvas.drawText(
                    badgeText,
                    badgeCx - badgeTextWidth / 2f,
                    badgeCy + badgeNumPaint.textSize * 0.36f,
                    badgeNumPaint,
                )

                val timeListened = artist.timeListened?.toLong() ?: 0L
                val timeBadgeText = makeTimeStringForImage(timeListened)
                val timeBadgeW = timeBadgePaint.measureText(timeBadgeText) + 26f
                val timeBadgeH = 42f
                val timeBadgeRect = RectF(
                    rowRect.right - 14f - timeBadgeW,
                    rowRect.centerY() - timeBadgeH / 2f,
                    rowRect.right - 14f,
                    rowRect.centerY() + timeBadgeH / 2f,
                )
                val timeBadgeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        timeBadgeRect.left,
                        timeBadgeRect.top,
                        timeBadgeRect.right,
                        timeBadgeRect.bottom,
                        intArrayOf(setAlpha(c.secondary, 75), setAlpha(c.primary, 55)),
                        floatArrayOf(0f, 1f),
                        Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawRoundRect(timeBadgeRect, timeBadgeH * 0.5f, timeBadgeH * 0.5f, timeBadgeFill)
                val timeBadgeBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = setAlpha(c.outline, if (bgLum >= 0.55) 90 else 130)
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                canvas.drawRoundRect(timeBadgeRect, timeBadgeH * 0.5f, timeBadgeH * 0.5f, timeBadgeBorder)
                canvas.drawText(
                    timeBadgeText,
                    timeBadgeRect.left + (timeBadgeW - timeBadgePaint.measureText(timeBadgeText)) / 2f,
                    timeBadgeRect.centerY() + timeBadgePaint.textSize * 0.36f,
                    timeBadgePaint,
                )

                val textX = (avatarCx + avatarRadius + 16f)
                val maxTextWidth = (timeBadgeRect.left - 14f - textX).toInt().coerceAtLeast(1)
                val name = truncateText(artist.artist.name, artistPaint, maxTextWidth.toFloat())
                canvas.drawText(name, textX, rowRect.top + 42f, artistPaint)
                canvas.drawText("${artist.songCount} plays", textX, rowRect.top + 76f, artistSubPaint)

                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 6f
                    shader = SweepGradient(
                        avatarCx,
                        avatarCy,
                        intArrayOf(c.primary, c.secondary, c.tertiary, c.primary),
                        floatArrayOf(0f, 0.45f, 0.78f, 1f),
                    )
                    alpha = 190
                }
                canvas.drawCircle(avatarCx, avatarCy, avatarRadius + 5f, ringPaint)

                rowY += itemHeight + itemGap
            }
            yOffset = cardRect.bottom + 18f
        }

        AppLogo(
            context = context,
            canvas = canvas,
            canvasWidth = cardWidth,
            canvasHeight = cardHeight,
            padding = contentPadding,
            circleColor = setAlpha(c.primary, 215),
            logoTint = setAlpha(c.onSurface, 245),
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

    private fun colorLuminance(color: Int): Double {
        fun channel(v: Int): Double {
            val c = v / 255.0
            return if (c <= 0.04045) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        val r = channel(Color.red(color))
        val g = channel(Color.green(color))
        val b = channel(Color.blue(color))
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun createNoiseBitmap(size: Int, seed: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val rnd = Random(seed)
        val pixels = IntArray(size * size)
        var i = 0
        while (i < pixels.size) {
            val v = rnd.nextInt(0, 256)
            val a = rnd.nextInt(12, 26)
            pixels[i] = Color.argb(a, v, v, v)
            i++
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        return bmp
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
        shadowBlur: Float = 28f,
        shadowDy: Float = 14f,
        shadowAlpha: Int = 70,
        highlightAlpha: Int = 80,
    ) {
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillColor
            setShadowLayer(shadowBlur, 0f, shadowDy, setAlpha(Color.BLACK, shadowAlpha))
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, shadowPaint)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint)

        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                rect.left,
                rect.top,
                rect.left,
                rect.bottom,
                intArrayOf(setAlpha(Color.WHITE, highlightAlpha), setAlpha(Color.WHITE, 0)),
                floatArrayOf(0f, 0.65f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, highlightPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
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
