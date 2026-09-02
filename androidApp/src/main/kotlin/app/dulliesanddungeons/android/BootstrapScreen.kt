package app.dulliesanddungeons.android

import android.animation.ValueAnimator
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private const val LOOP_MILLIS = 1_600
private val TableColor = Color(0xFFF8F7F2)
private val InkColor = Color(0xFF26342D)

@Composable
internal fun BootstrapLoadingScreen() {
    val context = LocalContext.current
    val reducedMotion = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            !ValueAnimator.areAnimatorsEnabled()
        } else {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }
    }
    val logo = ImageBitmap.imageResource(context.resources, R.drawable.loading_d20)
    if (reducedMotion) {
        Box(
            modifier = Modifier.fillMaxSize().background(TableColor).semantics {
                contentDescription = "Loading Dullies & Dungeons"
            },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Image(bitmap = logo, contentDescription = null, modifier = Modifier.size(144.dp))
                Text("Loading…", style = MaterialTheme.typography.titleMedium, color = InkColor)
            }
        }
        return
    }
    val progress = run {
        val transition = rememberInfiniteTransition(label = "D20 bootstrap")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(LOOP_MILLIS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "D20 roll progress",
        )
        animated
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TableColor)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                contentDescription = "Loading Dullies & Dungeons"
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val dieSize = minOf(size.width * 0.34f, 180.dp.toPx())
            val tableLine = size.height * 0.56f
            val travelProgress = (progress / 0.64f).coerceIn(0f, 1f)
            val settleProgress = ((progress - 0.64f) / 0.10f).coerceIn(0f, 1f)
            val openingProgress = ((progress - 0.70f) / 0.12f).coerceIn(0f, 1f)
            val sinkingProgress = ((progress - 0.78f) / 0.15f).coerceIn(0f, 1f)
            val closingProgress = ((progress - 0.91f) / 0.09f).coerceIn(0f, 1f)

            val startX = -dieSize * 0.65f
            val centerX = size.width / 2f
            val dieCenterX = startX + (centerX - startX) * easeOutCubic(travelProgress)
            val bounceEnvelope = 1f - travelProgress
            val bounce = abs(sin(travelProgress * 2.4f * PI.toFloat())) * dieSize * 0.30f * bounceEnvelope
            val settleSquash = sin(settleProgress * PI.toFloat()) * dieSize * 0.025f
            val dieCenterY = tableLine - dieSize / 2f - bounce + sinkingProgress * dieSize * 1.08f + settleSquash

            val shadowAlpha = ((1f - bounce / (dieSize * 0.31f)).coerceIn(0f, 1f) * (1f - sinkingProgress)).coerceIn(0f, 1f)
            val shadowWidth = dieSize * (0.40f + shadowAlpha * 0.42f)
            drawOval(
                color = InkColor.copy(alpha = 0.08f + shadowAlpha * 0.17f),
                topLeft = Offset(dieCenterX - shadowWidth / 2f, tableLine - dieSize * 0.035f),
                size = Size(shadowWidth, dieSize * (0.06f + shadowAlpha * 0.035f)),
            )

            val slitHalfWidth = dieSize * 0.62f * openingProgress * (1f - closingProgress)
            if (slitHalfWidth > 0f) {
                drawLine(
                    color = InkColor,
                    start = Offset(centerX - slitHalfWidth, tableLine),
                    end = Offset(centerX + slitHalfWidth, tableLine),
                    strokeWidth = 2.dp.toPx(),
                )
            }

            clipRect(left = 0f, top = 0f, right = size.width, bottom = tableLine + 1.dp.toPx()) {
                translate(left = dieCenterX - dieSize / 2f, top = dieCenterY - dieSize / 2f) {
                    rotate(
                        degrees = 590f * travelProgress + 10f * (1f - settleProgress),
                        pivot = Offset(dieSize / 2f, dieSize / 2f),
                    ) {
                        drawImage(
                            image = logo,
                            dstOffset = IntOffset.Zero,
                            dstSize = IntSize(dieSize.toInt(), dieSize.toInt()),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun BootstrapErrorScreen(error: Throwable, onRetry: () -> Unit) {
    var detailsOpen by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().background(TableColor).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("The app could not load its local data.", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(18.dp))
        Button(onClick = onRetry, modifier = Modifier.size(width = 180.dp, height = 48.dp)) { Text("Retry") }
        TextButton(onClick = { detailsOpen = true }) { Text("Details") }
    }
    if (detailsOpen) {
        AlertDialog(
            onDismissRequest = { detailsOpen = false },
            title = { Text("Loading error") },
            text = { Text(error.message ?: error::class.java.simpleName) },
            confirmButton = { TextButton(onClick = { detailsOpen = false }) { Text("Close") } },
        )
    }
}

private fun easeOutCubic(value: Float): Float {
    val inverse = 1f - value
    return 1f - inverse * inverse * inverse
}
