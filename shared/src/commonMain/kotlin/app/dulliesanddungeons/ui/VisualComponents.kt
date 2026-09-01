package app.dulliesanddungeons.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
internal fun Modifier.paperTexture(): Modifier {
    val fiber = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.032f)
    val grain = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.022f)
    return drawWithCache {
        val fibers = List(34) { index ->
            val y = size.height * (((index * 47) % 101) / 101f)
            val x = size.width * (((index * 29) % 83) / 83f)
            Triple(x, y, size.width * (0.035f + (index % 5) * 0.012f))
        }
        val specks = List(58) { index ->
            Offset(
                size.width * (((index * 37 + 11) % 97) / 97f),
                size.height * (((index * 61 + 7) % 103) / 103f),
            )
        }
        onDrawWithContent {
            drawContent()
            fibers.forEachIndexed { index, (x, y, length) ->
                drawLine(
                    color = if (index % 3 == 0) grain else fiber,
                    start = Offset(x, y),
                    end = Offset((x + length).coerceAtMost(size.width), y + (index % 3 - 1) * 0.7f),
                    strokeWidth = 0.65f,
                    cap = StrokeCap.Round,
                )
            }
            specks.forEach { drawCircle(fiber, radius = 0.75f, center = it) }
        }
    }
}

@Composable
internal fun AnimatedDiceRow(
    sides: Int,
    values: List<Int>,
    kept: Int? = null,
    animationKey: Any? = values,
    modifier: Modifier = Modifier,
    dieSize: Dp = 50.dp,
    onAnimationFinished: () -> Unit = {},
) {
    var shown by remember(animationKey) { mutableStateOf(values) }
    LaunchedEffect(animationKey) {
        val scale = coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
        if (scale > 0f && sides > 1 && values.isNotEmpty()) {
            repeat(7) { frame ->
                shown = values.mapIndexed { index, value -> 1 + ((value + frame * 7 + index * 3) % sides) }
                delay((60L * scale).toLong().coerceAtLeast(1L))
            }
        }
        shown = values
        onAnimationFinished()
    }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
        shown.forEachIndexed { index, value ->
            DieFace(
                sides = sides,
                value = value,
                modifier = Modifier.size(dieSize),
                emphasized = kept == null || values.getOrNull(index) == kept,
            )
        }
    }
}

@Composable
internal fun DieFace(
    sides: Int,
    value: Int?,
    modifier: Modifier = Modifier.size(50.dp),
    emphasized: Boolean = true,
) {
    val fill = if (emphasized) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val outline = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.055f
            when (sides) {
                4 -> drawPolygon(3, fill, outline, stroke, rotation = -PI / 2)
                6 -> {
                    drawRoundRect(fill, cornerRadius = CornerRadius(size.minDimension * .18f))
                    drawRoundRect(outline, cornerRadius = CornerRadius(size.minDimension * .18f), style = Stroke(stroke))
                }
                8 -> drawPolygon(4, fill, outline, stroke, rotation = -PI / 2)
                10 -> drawPolygon(5, fill, outline, stroke, rotation = -PI / 2)
                12 -> drawPolygon(6, fill, outline, stroke, rotation = 0.0)
                20 -> {
                    drawPolygon(6, fill, outline, stroke, rotation = -PI / 2)
                    drawLine(outline.copy(alpha = .55f), Offset(size.width * .18f, size.height * .66f), Offset(size.width * .5f, size.height * .18f), stroke / 1.6f)
                    drawLine(outline.copy(alpha = .55f), Offset(size.width * .82f, size.height * .66f), Offset(size.width * .5f, size.height * .18f), stroke / 1.6f)
                    drawLine(outline.copy(alpha = .55f), Offset(size.width * .18f, size.height * .66f), Offset(size.width * .82f, size.height * .66f), stroke / 1.6f)
                }
                else -> drawPolygon(6, fill, outline, stroke, rotation = -PI / 2)
            }
        }
        value?.let {
            Text(
                it.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPolygon(
    points: Int,
    fill: Color,
    outline: Color,
    stroke: Float,
    rotation: Double,
) {
    val radius = min(size.width, size.height) * .46f
    val center = Offset(size.width / 2f, size.height / 2f)
    val path = Path()
    repeat(points) { index ->
        val angle = rotation + index * 2.0 * PI / points
        val point = Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius)
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    drawPath(path, fill)
    drawPath(path, outline, style = Stroke(stroke, cap = StrokeCap.Round))
}

@Composable
internal fun DicePresentationOverlay(state: DndAppState, modifier: Modifier = Modifier) {
    val presentation = state.dicePresentation
    AnimatedVisibility(
        visible = presentation != null,
        modifier = modifier.fillMaxSize(),
        enter = fadeIn(tween(220)) + scaleIn(tween(300), initialScale = .94f) + slideInVertically(tween(320)) { it / 10 },
        exit = fadeOut(tween(160)) + scaleOut(tween(180), targetScale = .97f) + slideOutVertically(tween(180)) { it / 14 },
    ) {
        Box(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
            presentation?.let { roll ->
            var settled by remember(roll.id) { mutableStateOf(false) }
            var detailsVisible by remember(roll.id) { mutableStateOf(false) }
            val rolledSubtotal = roll.kept ?: roll.dice.sum()
            val modifierValue = roll.total - rolledSubtotal
            val diceOffset by animateDpAsState(
                targetValue = if (settled) (-92).dp else 0.dp,
                animationSpec = tween(420),
            )
            val equationAlpha by animateFloatAsState(if (settled) 1f else 0f, animationSpec = tween(280))
            val equationOffset by animateDpAsState(if (settled) 0.dp else 12.dp, animationSpec = tween(360))
                Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 10.dp,
                ) {
                    Column(
                    Modifier.fillMaxWidth().padding(18.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                    Text(roll.label, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                    Box(Modifier.fillMaxWidth().height(116.dp)) {
                        AnimatedDiceRow(
                            roll.sides,
                            roll.dice,
                            roll.kept,
                            roll.id,
                            Modifier.align(Alignment.Center).offset(x = diceOffset),
                            dieSize = 76.dp,
                            onAnimationFinished = { settled = true },
                        )
                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd)
                                .offset(y = equationOffset)
                                .graphicsLayer { alpha = equationAlpha },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                                Text(
                                    buildString {
                                        append(if (modifierValue >= 0) "+$modifierValue" else "−${kotlin.math.abs(modifierValue)}")
                                        roll.modifierLabel.takeIf { it.isNotBlank() }?.let { append(" ${it.uppercase()}") }
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text("=", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    roll.total.toString(),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E9D61),
                                )
                        }
                    }
                    if (settled) {
                        if (roll.context.isNotBlank()) {
                            TextButton(onClick = { detailsVisible = !detailsVisible }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                Text(if (detailsVisible) state.t("Hide details", "Details ausblenden") else state.t("Details", "Details"))
                            }
                        }
                        if (detailsVisible) {
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                if (roll.context.isNotBlank()) Text(roll.context, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            OutlinedButton(
                                onClick = state::rerollDicePresentation,
                                modifier = Modifier.weight(1f).height(50.dp),
                            ) { Text(state.t("Roll again", "Erneut würfeln")) }
                            Button(
                                onClick = { state.dicePresentation = null },
                                modifier = Modifier.weight(1f).height(50.dp),
                            ) { Text(state.t("Close", "Schließen")) }
                        }
                    }
                    }
                }
            }
        }
    }
}
