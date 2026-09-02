package app.dulliesanddungeons.android

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.dulliesanddungeons.domain.PortraitCrop
import app.dulliesanddungeons.ui.DndAppState
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
internal fun PortraitCropDialog(
    state: DndAppState,
    session: PortraitEditorSession,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (PortraitCrop) -> Unit,
) {
    val initialTurns = normalizedTurns(session.initialCrop.rotationQuarterTurns)
    val initialWidth = if (initialTurns % 2 == 0) session.sourceBitmap.width else session.sourceBitmap.height
    val initialHeight = if (initialTurns % 2 == 0) session.sourceBitmap.height else session.sourceBitmap.width
    var crop by remember(session) {
        mutableStateOf(PortraitImageProcessor.sanitizeCrop(session.initialCrop, initialWidth, initialHeight))
    }
    val rotatedBitmap = remember(session.sourceBitmap, crop.rotationQuarterTurns) {
        PortraitImageProcessor.rotate(session.sourceBitmap, crop.rotationQuarterTurns)
    }
    DisposableEffect(rotatedBitmap, session.sourceBitmap) {
        onDispose {
            if (rotatedBitmap !== session.sourceBitmap && !rotatedBitmap.isRecycled) rotatedBitmap.recycle()
        }
    }
    DisposableEffect(session.sourceBitmap) {
        onDispose {
            if (!session.sourceBitmap.isRecycled) session.sourceBitmap.recycle()
        }
    }
    Dialog(
        onDismissRequest = { if (!saving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !saving,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                    ) {
                        Text(state.t("Cancel", "Abbrechen"))
                    }
                    Text(
                        state.t("Adjust portrait", "Porträt anpassen"),
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Button(onClick = { onSave(crop) }, enabled = !saving) {
                        if (saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(state.t("Save", "Speichern"))
                        }
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CropCanvas(
                        bitmap = rotatedBitmap,
                        crop = crop,
                        state = state,
                        onCropChanged = { crop = it },
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(20.dp),
                    )
                }

                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        state.t("Drag to reposition · Pinch to zoom", "Ziehen zum Verschieben · Aufziehen zum Zoomen"),
                        color = Color.White.copy(alpha = 0.76f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (session.usesRenderedFallback) {
                        Text(
                            state.t(
                                "Only the saved crop is available for this older portrait.",
                                "Für dieses ältere Porträt ist nur der gespeicherte Zuschnitt verfügbar.",
                            ),
                            color = Color.White.copy(alpha = 0.64f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = {
                                crop = PortraitImageProcessor.rotateCropClockwise(
                                    crop,
                                    session.sourceBitmap.width,
                                    session.sourceBitmap.height,
                                )
                            },
                            enabled = !saving,
                            modifier = Modifier.weight(1f).height(52.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.RotateRight, contentDescription = null)
                            Spacer(Modifier.size(7.dp))
                            Text(state.t("Rotate 90°", "90° drehen"))
                        }
                        OutlinedButton(
                            onClick = { crop = PortraitCrop() },
                            enabled = !saving,
                            modifier = Modifier.weight(1f).height(52.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(Modifier.size(7.dp))
                            Text(state.t("Reset", "Zurücksetzen"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CropCanvas(
    bitmap: Bitmap,
    crop: PortraitCrop,
    state: DndAppState,
    onCropChanged: (PortraitCrop) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeCrop by rememberUpdatedState(crop)
    val activeOnCropChanged by rememberUpdatedState(onCropChanged)

    fun zoomBy(factor: Float) {
        onCropChanged(
            PortraitImageProcessor.sanitizeCrop(
                crop.copy(sizeFractionOfShortEdge = crop.sizeFractionOfShortEdge / factor),
                bitmap.width,
                bitmap.height,
            )
        )
    }

    fun moveImage(horizontal: Float, vertical: Float) {
        val shortEdge = min(bitmap.width, bitmap.height).toFloat()
        val step = shortEdge * crop.sizeFractionOfShortEdge * 0.08f
        onCropChanged(
            PortraitImageProcessor.sanitizeCrop(
                crop.copy(
                    centerXFraction = crop.centerXFraction - horizontal * step / bitmap.width,
                    centerYFraction = crop.centerYFraction - vertical * step / bitmap.height,
                ),
                bitmap.width,
                bitmap.height,
            )
        )
    }

    val image = remember(bitmap) { bitmap.asImageBitmap() }
    Canvas(
        modifier = modifier
            .semantics {
                role = Role.Image
                contentDescription = state.t("Portrait crop preview", "Vorschau des Porträtzuschnitts")
                stateDescription = state.t(
                    "Rotation ${crop.rotationQuarterTurns * 90} degrees",
                    "Drehung ${crop.rotationQuarterTurns * 90} Grad",
                )
                customActions = listOf(
                    CustomAccessibilityAction(state.t("Zoom in", "Vergrößern")) { zoomBy(1.2f); true },
                    CustomAccessibilityAction(state.t("Zoom out", "Verkleinern")) { zoomBy(1f / 1.2f); true },
                    CustomAccessibilityAction(state.t("Move picture left", "Bild nach links verschieben")) { moveImage(-1f, 0f); true },
                    CustomAccessibilityAction(state.t("Move picture right", "Bild nach rechts verschieben")) { moveImage(1f, 0f); true },
                    CustomAccessibilityAction(state.t("Move picture up", "Bild nach oben verschieben")) { moveImage(0f, -1f); true },
                    CustomAccessibilityAction(state.t("Move picture down", "Bild nach unten verschieben")) { moveImage(0f, 1f); true },
                )
            }
            .pointerInput(bitmap, crop.rotationQuarterTurns) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val currentCrop = activeCrop
                    val safeZoom = zoom.takeIf { it.isFinite() && it > 0f } ?: 1f
                    val shortEdge = min(bitmap.width, bitmap.height).toFloat()
                    val currentSide = shortEdge * currentCrop.sizeFractionOfShortEdge
                    val aperture = min(size.width, size.height) * 0.86f
                    if (aperture <= 0f) return@detectTransformGestures
                    val sourcePixelsPerCanvasPixel = currentSide / aperture
                    activeOnCropChanged(
                        PortraitImageProcessor.sanitizeCrop(
                            currentCrop.copy(
                                centerXFraction = currentCrop.centerXFraction - pan.x * sourcePixelsPerCanvasPixel / bitmap.width,
                                centerYFraction = currentCrop.centerYFraction - pan.y * sourcePixelsPerCanvasPixel / bitmap.height,
                                sizeFractionOfShortEdge = currentCrop.sizeFractionOfShortEdge / safeZoom,
                            ),
                            bitmap.width,
                            bitmap.height,
                        )
                    )
                }
            }
    ) {
        val diameter = min(size.width, size.height) * 0.86f
        val radius = diameter / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val shortEdge = min(bitmap.width, bitmap.height)
        val side = (shortEdge * crop.sizeFractionOfShortEdge)
            .roundToInt()
            .coerceIn(1, shortEdge)
        val centerX = crop.centerXFraction * bitmap.width
        val centerY = crop.centerYFraction * bitmap.height
        val left = (centerX - side / 2f).roundToInt().coerceIn(0, bitmap.width - side)
        val top = (centerY - side / 2f).roundToInt().coerceIn(0, bitmap.height - side)
        val destinationLeft = (center.x - radius).roundToInt()
        val destinationTop = (center.y - radius).roundToInt()
        val circle = Path().apply {
            addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
        }
        clipPath(circle) {
            drawImage(
                image = image,
                srcOffset = IntOffset(left, top),
                srcSize = IntSize(side, side),
                dstOffset = IntOffset(destinationLeft, destinationTop),
                dstSize = IntSize(diameter.roundToInt(), diameter.roundToInt()),
                filterQuality = FilterQuality.High,
            )
        }
        drawCircle(
            color = Color.White.copy(alpha = 0.92f),
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

private fun normalizedTurns(value: Int): Int = ((value % 4) + 4) % 4
