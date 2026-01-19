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
import moe.koiverse.archivetune.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ComposeToImage {

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

        AppLogo(context, canvas, cardSize, padding, secondaryTxtColor, bgColor)

        return@withContext bitmap
    }

    @RequiresApi(Build.VERSION_CODES.M)
    suspend fun createYearInMusicImage(
        context: Context,
        year: Int,
        totalListeningTime: Long,
        topSongs: List<moe.koiverse.archivetune.db.entities.SongWithStats>,
        topArtists: List<moe.koiverse.archivetune.db.entities.Artist>
    ): Bitmap = withContext(Dispatchers.Default) {
        val cardWidth = 1080
        val cardHeight = 1920
        val bitmap = createBitmap(cardWidth, cardHeight)
        val canvas = Canvas(bitmap)

        val backgroundColor = 0xFF0D1117.toInt()
        val primaryColor = 0xFF58A6FF.toInt()
        val secondaryColor = 0xFF8B949E.toInt()
        val textColor = 0xFFFFFFFF.toInt()
        val accentColor = 0xFF238636.toInt()

        val backgroundPaint = Paint().apply {
            color = backgroundColor
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(), backgroundPaint)

        val gradientPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, cardWidth.toFloat(), cardHeight * 0.3f,
                intArrayOf(0xFF1F6FEB.toInt(), 0xFF238636.toInt(), 0xFF0D1117.toInt()),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            alpha = 100
        }
        canvas.drawRect(0f, 0f, cardWidth.toFloat(), cardHeight * 0.35f, gradientPaint)

        val padding = 60f
        var yOffset = padding + 40f

        val yearPaint = TextPaint().apply {
            color = textColor
            textSize = 72f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("$year", padding, yOffset + 72f, yearPaint)
        yOffset += 100f

        val subtitlePaint = TextPaint().apply {
            color = primaryColor
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(context.getString(moe.koiverse.archivetune.R.string.year_in_music), padding, yOffset + 48f, subtitlePaint)
        yOffset += 100f

        val timeString = makeTimeStringForImage(totalListeningTime)
        val timePaint = TextPaint().apply {
            color = textColor
            textSize = 96f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(timeString, padding, yOffset + 96f, timePaint)
        yOffset += 120f

        val timeLabelPaint = TextPaint().apply {
            color = secondaryColor
            textSize = 36f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        canvas.drawText(context.getString(moe.koiverse.archivetune.R.string.total_listening_time), padding, yOffset + 36f, timeLabelPaint)
        yOffset += 100f

        if (topSongs.isNotEmpty()) {
            val sectionPaint = TextPaint().apply {
                color = primaryColor
                textSize = 42f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(context.getString(moe.koiverse.archivetune.R.string.top_songs), padding, yOffset + 42f, sectionPaint)
            yOffset += 80f

            val songTitlePaint = TextPaint().apply {
                color = textColor
                textSize = 36f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val songSubtitlePaint = TextPaint().apply {
                color = secondaryColor
                textSize = 28f
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }

            val imageLoader = ImageLoader(context)
            val thumbnailSize = 100

            topSongs.take(5).forEachIndexed { index, song ->
                var songBitmap: Bitmap? = null
                if (song.thumbnailUrl != null) {
                    try {
                        val request = ImageRequest.Builder(context)
                            .data(song.thumbnailUrl)
                            .size(thumbnailSize)
                            .allowHardware(false)
                            .build()
                        val result = imageLoader.execute(request)
                        songBitmap = result.image?.toBitmap()
                    } catch (_: Exception) {}
                }

                songBitmap?.let {
                    val rect = RectF(padding, yOffset, padding + thumbnailSize, yOffset + thumbnailSize)
                    val path = Path().apply {
                        addRoundRect(rect, 12f, 12f, Path.Direction.CW)
                    }
                    canvas.withClip(path) {
                        drawBitmap(it, null, rect, null)
                    }
                }

                val textX = padding + thumbnailSize + 20f
                val titleText = "${index + 1}. ${song.title}"
                val maxWidth = cardWidth - textX - padding
                val truncatedTitle = truncateText(titleText, songTitlePaint, maxWidth)
                canvas.drawText(truncatedTitle, textX, yOffset + 40f, songTitlePaint)

                val playCount = "${song.songCountListened} plays • ${makeTimeStringForImage(song.timeListened ?: 0L)}"
                canvas.drawText(playCount, textX, yOffset + 80f, songSubtitlePaint)

                yOffset += thumbnailSize + 20f
            }
            yOffset += 40f
        }

        if (topArtists.isNotEmpty()) {
            val sectionPaint = TextPaint().apply {
                color = primaryColor
                textSize = 42f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(context.getString(moe.koiverse.archivetune.R.string.top_artists), padding, yOffset + 42f, sectionPaint)
            yOffset += 80f

            val artistPaint = TextPaint().apply {
                color = textColor
                textSize = 36f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val artistSubPaint = TextPaint().apply {
                color = secondaryColor
                textSize = 28f
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }

            topArtists.take(5).forEachIndexed { index, artist ->
                val artistText = "${index + 1}. ${artist.artist.name}"
                val maxWidth = cardWidth - padding * 2
                val truncatedArtist = truncateText(artistText, artistPaint, maxWidth)
                canvas.drawText(truncatedArtist, padding, yOffset + 36f, artistPaint)

                val timeListened = artist.timeListened?.toLong() ?: 0L
                val statsText = "${artist.songCount} plays • ${makeTimeStringForImage(timeListened)}"
                canvas.drawText(statsText, padding, yOffset + 72f, artistSubPaint)

                yOffset += 100f
            }
        }

        AppLogo(context, canvas, cardWidth, padding, secondaryColor, backgroundColor)

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

    private fun AppLogo(
        context: Context,
        canvas: Canvas,
        cardSize: Int,
        padding: Float,
        secondaryTxtColor: Int,
        backgroundColor: Int
    ) {
        val logoSize = (cardSize * 0.05f).toInt()

        val rawLogo = context.getDrawable(R.drawable.small_icon)?.toBitmap(logoSize, logoSize)
        val logo = rawLogo?.let { source ->
            val colored = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val canvasLogo = Canvas(colored)
            val paint = Paint().apply {
                colorFilter = PorterDuffColorFilter(backgroundColor, PorterDuff.Mode.SRC_IN)
                isAntiAlias = true
            }
            canvasLogo.drawBitmap(source, 0f, 0f, paint)
            colored
        }

        val appName = context.getString(R.string.app_name)
        val appNamePaint = TextPaint().apply {
            color = secondaryTxtColor
            textSize = cardSize * 0.030f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            isAntiAlias = true
            letterSpacing = 0.01f
        }

        val circleRadius = logoSize * 0.55f
        val logoX = padding + circleRadius - logoSize / 2f
        val logoY = cardSize - padding - circleRadius - logoSize / 2f
        val circleX = padding + circleRadius
        val circleY = cardSize - padding - circleRadius
        val textX = padding + circleRadius * 2 + 12f
        val textY = circleY + appNamePaint.textSize * 0.3f

        val circlePaint = Paint().apply {
            color = secondaryTxtColor
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
