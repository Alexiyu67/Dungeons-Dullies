package app.dulliesanddungeons.android

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import app.dulliesanddungeons.domain.PortraitCrop
import app.dulliesanddungeons.ui.PortraitEditResult
import app.dulliesanddungeons.ui.PortraitEditSource
import app.dulliesanddungeons.ui.PortraitPickTarget
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal data class PortraitEditorSession(
    val target: PortraitPickTarget,
    val sourceBitmap: Bitmap,
    val sourceBytes: ByteArray,
    val initialCrop: PortraitCrop,
    val usesRenderedFallback: Boolean,
)

internal object PortraitImageProcessor {
    const val SOURCE_MAX_EDGE = 2048
    const val OUTPUT_SIZE = 512
    const val MIN_CROP_FRACTION = 0.2f
    private const val SOURCE_JPEG_QUALITY = 92
    private const val OUTPUT_JPEG_QUALITY = 86

    fun fromUri(
        resolver: ContentResolver,
        uri: Uri,
        target: PortraitPickTarget,
    ): PortraitEditorSession? = runCatching {
        val exif = runCatching { resolver.openInputStream(uri)?.use(::ExifInterface) }.getOrNull()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        val longest = max(bounds.outWidth, bounds.outHeight)
        while (longest / (sampleSize * 2) >= SOURCE_MAX_EDGE) sampleSize *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null
        val oriented = applyExif(decoded, exif)
        if (oriented !== decoded) decoded.recycle()
        val bounded = scaleToMaxEdge(oriented, SOURCE_MAX_EDGE)
        if (bounded !== oriented) oriented.recycle()
        val sourceBytes = encodeJpeg(bounded, SOURCE_JPEG_QUALITY) ?: run {
            bounded.recycle()
            return null
        }
        PortraitEditorSession(
            target = target,
            sourceBitmap = bounded,
            sourceBytes = sourceBytes,
            initialCrop = PortraitCrop(),
            usesRenderedFallback = false,
        )
    }.getOrNull()

    fun fromSource(
        source: PortraitEditSource,
        target: PortraitPickTarget,
    ): PortraitEditorSession? = runCatching {
        val decoded = BitmapFactory.decodeByteArray(source.sourceBytes, 0, source.sourceBytes.size)
            ?: return null
        val bounded = scaleToMaxEdge(decoded, SOURCE_MAX_EDGE)
        if (bounded !== decoded) decoded.recycle()
        val sourceBytes = if (bounded === decoded) source.sourceBytes else {
            encodeJpeg(bounded, SOURCE_JPEG_QUALITY) ?: run {
                bounded.recycle()
                return null
            }
        }
        PortraitEditorSession(
            target = target,
            sourceBitmap = bounded,
            sourceBytes = sourceBytes,
            initialCrop = source.crop,
            usesRenderedFallback = source.usesRenderedFallback,
        )
    }.getOrNull()

    fun render(session: PortraitEditorSession, crop: PortraitCrop): PortraitEditResult? = runCatching {
        val rotated = rotate(session.sourceBitmap, crop.rotationQuarterTurns)
        val safeCrop = sanitizeCrop(crop, rotated.width, rotated.height)
        val shortEdge = min(rotated.width, rotated.height)
        val side = (shortEdge * safeCrop.sizeFractionOfShortEdge)
            .roundToInt()
            .coerceIn(1, shortEdge)
        val centerX = safeCrop.centerXFraction * rotated.width
        val centerY = safeCrop.centerYFraction * rotated.height
        val left = (centerX - side / 2f).roundToInt().coerceIn(0, rotated.width - side)
        val top = (centerY - side / 2f).roundToInt().coerceIn(0, rotated.height - side)
        val cropped = Bitmap.createBitmap(rotated, left, top, side, side)
        val output = if (side == OUTPUT_SIZE) cropped else {
            cropped.scale(OUTPUT_SIZE, OUTPUT_SIZE)
        }
        try {
            encodeJpeg(output, OUTPUT_JPEG_QUALITY)?.let { portraitBytes ->
                PortraitEditResult(
                    sourceBytes = session.sourceBytes,
                    portraitBytes = portraitBytes,
                    crop = safeCrop,
                )
            }
        } finally {
            if (output !== cropped && output !== rotated && output !== session.sourceBitmap) output.recycle()
            if (cropped !== rotated && cropped !== session.sourceBitmap) cropped.recycle()
            if (rotated !== session.sourceBitmap) rotated.recycle()
        }
    }.getOrNull()

    fun rotate(source: Bitmap, quarterTurns: Int): Bitmap {
        val turns = normalizedTurns(quarterTurns)
        if (turns == 0) return source
        val matrix = Matrix().apply { postRotate(turns * 90f) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun sanitizeCrop(crop: PortraitCrop, rotatedWidth: Int, rotatedHeight: Int): PortraitCrop {
        if (rotatedWidth <= 0 || rotatedHeight <= 0) return PortraitCrop()
        val size = crop.sizeFractionOfShortEdge
            .takeIf(Float::isFinite)
            ?.coerceIn(MIN_CROP_FRACTION, 1f)
            ?: 1f
        val side = min(rotatedWidth, rotatedHeight) * size
        val halfWidthFraction = side / (2f * rotatedWidth)
        val halfHeightFraction = side / (2f * rotatedHeight)
        val centerX = crop.centerXFraction.takeIf(Float::isFinite)?.coerceIn(
            halfWidthFraction,
            1f - halfWidthFraction,
        ) ?: 0.5f
        val centerY = crop.centerYFraction.takeIf(Float::isFinite)?.coerceIn(
            halfHeightFraction,
            1f - halfHeightFraction,
        ) ?: 0.5f
        return PortraitCrop(normalizedTurns(crop.rotationQuarterTurns), centerX, centerY, size)
    }

    fun rotateCropClockwise(crop: PortraitCrop, sourceWidth: Int, sourceHeight: Int): PortraitCrop {
        val currentTurns = normalizedTurns(crop.rotationQuarterTurns)
        val currentWidth = if (currentTurns % 2 == 0) sourceWidth else sourceHeight
        val currentHeight = if (currentTurns % 2 == 0) sourceHeight else sourceWidth
        val current = sanitizeCrop(crop, currentWidth, currentHeight)
        val nextTurns = normalizedTurns(currentTurns + 1)
        val nextWidth = currentHeight
        val nextHeight = currentWidth
        return sanitizeCrop(
            PortraitCrop(
                rotationQuarterTurns = nextTurns,
                centerXFraction = 1f - current.centerYFraction,
                centerYFraction = current.centerXFraction,
                sizeFractionOfShortEdge = current.sizeFractionOfShortEdge,
            ),
            nextWidth,
            nextHeight,
        )
    }

    private fun normalizedTurns(value: Int): Int = ((value % 4) + 4) % 4

    private fun applyExif(source: Bitmap, exif: ExifInterface?): Bitmap {
        val rotation = exif?.rotationDegrees ?: 0
        val flipped = exif?.isFlipped == true
        if (rotation == 0 && !flipped) return source
        val matrix = Matrix().apply {
            if (flipped) postScale(-1f, 1f)
            if (rotation != 0) postRotate(rotation.toFloat())
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun scaleToMaxEdge(source: Bitmap, maximum: Int): Bitmap {
        val longest = max(source.width, source.height)
        if (longest <= maximum) return source
        val factor = maximum.toFloat() / longest
        return source.scale(
            max(1, (source.width * factor).roundToInt()),
            max(1, (source.height * factor).roundToInt()),
        )
    }

    private fun encodeJpeg(bitmap: Bitmap, quality: Int): ByteArray? {
        val opaque = if (bitmap.hasAlpha()) {
            createBitmap(bitmap.width, bitmap.height).also { output ->
                Canvas(output).apply {
                    drawColor(Color.WHITE)
                    drawBitmap(bitmap, 0f, 0f, null)
                }
            }
        } else bitmap
        return try {
            ByteArrayOutputStream().use { output ->
                if (!opaque.compress(Bitmap.CompressFormat.JPEG, quality, output)) return null
                output.toByteArray()
            }
        } finally {
            if (opaque !== bitmap) opaque.recycle()
        }
    }
}
