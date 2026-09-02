package app.dulliesanddungeons.android

import app.dulliesanddungeons.domain.PortraitCrop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PortraitImageProcessorTest {
    @Test
    fun sanitizeCropNormalizesRotationZoomAndBounds() {
        val result = PortraitImageProcessor.sanitizeCrop(
            PortraitCrop(
                rotationQuarterTurns = -1,
                centerXFraction = -2f,
                centerYFraction = 3f,
                sizeFractionOfShortEdge = 0.01f,
            ),
            rotatedWidth = 1600,
            rotatedHeight = 900,
        )

        assertEquals(3, result.rotationQuarterTurns)
        assertEquals(PortraitImageProcessor.MIN_CROP_FRACTION, result.sizeFractionOfShortEdge)
        assertTrue(result.centerXFraction > 0f)
        assertTrue(result.centerYFraction < 1f)
    }

    @Test
    fun fourClockwiseTurnsRestoreTheCrop() {
        val original = PortraitCrop(
            rotationQuarterTurns = 0,
            centerXFraction = 0.35f,
            centerYFraction = 0.6f,
            sizeFractionOfShortEdge = 0.5f,
        )

        val restored = (1..4).fold(original) { crop, _ ->
            PortraitImageProcessor.rotateCropClockwise(crop, sourceWidth = 1600, sourceHeight = 900)
        }

        assertEquals(original.rotationQuarterTurns, restored.rotationQuarterTurns)
        assertEquals(original.centerXFraction, restored.centerXFraction, absoluteTolerance = 0.0001f)
        assertEquals(original.centerYFraction, restored.centerYFraction, absoluteTolerance = 0.0001f)
        assertEquals(original.sizeFractionOfShortEdge, restored.sizeFractionOfShortEdge, absoluteTolerance = 0.0001f)
    }

    @Test
    fun zoomOutCannotExposeSpaceOutsideTheSource() {
        val result = PortraitImageProcessor.sanitizeCrop(
            PortraitCrop(
                centerXFraction = 0.9f,
                centerYFraction = 0.1f,
                sizeFractionOfShortEdge = 5f,
            ),
            rotatedWidth = 800,
            rotatedHeight = 1200,
        )

        assertEquals(1f, result.sizeFractionOfShortEdge)
        assertEquals(0.5f, result.centerXFraction)
        assertTrue(result.centerYFraction in (1f / 3f)..(2f / 3f))
    }
}
