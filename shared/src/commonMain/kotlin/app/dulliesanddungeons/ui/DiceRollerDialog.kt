package app.dulliesanddungeons.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.dulliesanddungeons.rules.DicePoolRoll
import kotlinx.coroutines.delay
import kotlin.coroutines.coroutineContext

private const val MaxDicePoolSize = 100
private val DicePoolSides = listOf(4, 6, 8, 10, 12, 20)

private sealed interface DiceRollerPage {
    data object Selection : DiceRollerPage
    data class Result(val roll: DicePoolRoll, val animationKey: Int) : DiceRollerPage
}

@Composable
internal fun DiceRollerDialog(
    state: DndAppState,
    onDismiss: () -> Unit,
) {
    val counts = remember {
        mutableStateMapOf<Int, Int>().apply {
            DicePoolSides.forEach { sides -> put(sides, 0) }
        }
    }
    var page by remember { mutableStateOf<DiceRollerPage>(DiceRollerPage.Selection) }
    var rollId by remember { mutableIntStateOf(0) }
    val selectedCount = counts.values.sum()

    fun rollSelectedDice() {
        val currentCount = counts.values.sum()
        if (currentCount !in 1..MaxDicePoolSize) return
        rollId += 1
        page = DiceRollerPage.Result(
            roll = state.rollDicePool(counts.toMap()),
            animationKey = rollId,
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp).heightIn(max = 680.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DiceRollerHeader(
                        state = state,
                        notation = (page as? DiceRollerPage.Result)?.roll?.notation,
                        onDismiss = onDismiss,
                    )
                    AnimatedContent(
                        targetState = page,
                        contentKey = { it::class },
                        transitionSpec = {
                            if (targetState is DiceRollerPage.Result) {
                                (fadeIn(tween(240)) + slideInHorizontally(tween(280)) { it / 7 }) togetherWith
                                    (fadeOut(tween(150)) + slideOutHorizontally(tween(180)) { -it / 8 })
                            } else {
                                (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { -it / 7 }) togetherWith
                                    (fadeOut(tween(140)) + slideOutHorizontally(tween(170)) { it / 8 })
                            }
                        },
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                        label = "Dice roller page",
                    ) { targetPage ->
                        when (targetPage) {
                            DiceRollerPage.Selection -> DicePoolSelection(
                                state = state,
                                counts = counts,
                                selectedCount = selectedCount,
                                onRoll = ::rollSelectedDice,
                            )

                            is DiceRollerPage.Result -> DicePoolResult(
                                state = state,
                                page = targetPage,
                                onAdjust = { page = DiceRollerPage.Selection },
                                onRollAgain = ::rollSelectedDice,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiceRollerHeader(
    state: DndAppState,
    notation: String?,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                state.t("Dice roller", "Würfelroller"),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            if (notation != null) {
                Text(
                    notation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onDismiss) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = state.t("Close dice roller", "Würfelroller schließen"),
            )
        }
    }
}

@Composable
private fun DicePoolSelection(
    state: DndAppState,
    counts: MutableMap<Int, Int>,
    selectedCount: Int,
    onRoll: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DicePoolSides.chunked(2).forEach { rowSides ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowSides.forEach { sides ->
                    val count = counts[sides] ?: 0
                    DiceCountTile(
                        state = state,
                        sides = sides,
                        count = count,
                        canAdd = selectedCount < MaxDicePoolSize,
                        onDecrease = {
                            counts[sides] = ((counts[sides] ?: 0) - 1).coerceAtLeast(0)
                        },
                        onIncrease = {
                            if (counts.values.sum() < MaxDicePoolSize) {
                                counts[sides] = (counts[sides] ?: 0) + 1
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Button(
            onClick = onRoll,
            enabled = selectedCount > 0,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(
                when (selectedCount) {
                    0 -> state.t("Select dice", "Würfel auswählen")
                    1 -> state.t("Roll 1 die", "1 Würfel werfen")
                    else -> state.t("Roll $selectedCount dice", "$selectedCount Würfel werfen")
                },
            )
        }
    }
}

@Composable
private fun DiceCountTile(
    state: DndAppState,
    sides: Int,
    count: Int,
    canAdd: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = count > 0
    Surface(
        modifier = modifier.height(132.dp).animateContentSize(tween(180)),
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DieFace(sides = sides, value = null, modifier = Modifier.size(38.dp))
                Text("d$sides", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDecrease,
                    enabled = count > 0,
                    modifier = Modifier.semantics {
                        contentDescription = state.t("Remove one d$sides", "Einen d$sides entfernen")
                    },
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = null)
                }
                Text(
                    count.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.widthIn(min = 24.dp).clearAndSetSemantics {
                        contentDescription = state.t("$count d$sides selected", "$count d$sides ausgewählt")
                    },
                )
                IconButton(
                    onClick = onIncrease,
                    enabled = canAdd,
                    modifier = Modifier.semantics {
                        contentDescription = state.t("Add one d$sides", "Einen d$sides hinzufügen")
                    },
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun DicePoolResult(
    state: DndAppState,
    page: DiceRollerPage.Result,
    onAdjust: () -> Unit,
    onRollAgain: () -> Unit,
) {
    var settled by remember(page.animationKey) { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).animateContentSize(tween(240)),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(76.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = settled,
                enter = fadeIn(tween(260)) + scaleIn(tween(300), initialScale = .88f),
                exit = fadeOut(tween(100)) + scaleOut(tween(100), targetScale = .96f),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = state.t("Total ${page.roll.total}", "Summe ${page.roll.total}")
                    },
                ) {
                    Text(
                        state.t("Total", "Summe"),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        page.roll.total.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        AnimatedDicePool(
            state = state,
            roll = page.roll,
            animationKey = page.animationKey,
            onAnimationFinished = { settled = true },
        )
        AnimatedVisibility(visible = settled, enter = fadeIn(tween(240)), exit = fadeOut(tween(100))) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                OutlinedButton(
                    onClick = onAdjust,
                    modifier = Modifier.weight(1f).height(50.dp),
                ) {
                    Text(state.t("Adjust", "Anpassen"))
                }
                Button(
                    onClick = onRollAgain,
                    modifier = Modifier.weight(1f).height(50.dp),
                ) {
                    Text(state.t("Roll again", "Erneut würfeln"))
                }
            }
        }
    }
}

@Composable
private fun AnimatedDicePool(
    state: DndAppState,
    roll: DicePoolRoll,
    animationKey: Int,
    onAnimationFinished: () -> Unit,
) {
    var frame by remember(animationKey) { mutableIntStateOf(0) }
    var settledCount by remember(animationKey) { mutableIntStateOf(0) }
    val dieSize = when {
        roll.diceCount <= 6 -> 56.dp
        roll.diceCount <= 12 -> 48.dp
        else -> 40.dp
    }

    LaunchedEffect(animationKey) {
        val scale = coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
        if (scale > 0f) {
            repeat(7) {
                frame += 1
                delay((58L * scale).toLong().coerceAtLeast(1L))
            }
            val batchSize = ((roll.diceCount + 7) / 8).coerceAtLeast(1)
            while (settledCount < roll.diceCount) {
                frame += 1
                settledCount = (settledCount + batchSize).coerceAtMost(roll.diceCount)
                delay((34L * scale).toLong().coerceAtLeast(1L))
            }
        } else {
            settledCount = roll.diceCount
        }
        onAnimationFinished()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        var groupStartIndex = 0
        roll.groups.forEach { group ->
            val startIndex = groupStartIndex
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (group.values.size == 1) "d${group.sides}" else "${group.values.size}d${group.sides}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    group.values.forEachIndexed { index, finalValue ->
                        val globalIndex = startIndex + index
                        AnimatedPoolDie(
                            state = state,
                            sides = group.sides,
                            finalValue = finalValue,
                            globalIndex = globalIndex,
                            frame = frame,
                            settled = globalIndex < settledCount,
                            size = dieSize,
                        )
                    }
                }
            }
            groupStartIndex += group.values.size
        }
    }
}

@Composable
private fun AnimatedPoolDie(
    state: DndAppState,
    sides: Int,
    finalValue: Int,
    globalIndex: Int,
    frame: Int,
    settled: Boolean,
    size: Dp,
) {
    val shownValue = if (settled) finalValue else 1 + ((finalValue + frame * 7 + globalIndex * 3) % sides)
    val targetRotation = if (settled) 0f else (((frame * 47 + globalIndex * 31) % 76) - 38).toFloat()
    val targetScale = if (settled) 1f else .86f + ((frame + globalIndex) % 3) * .05f
    val targetY = if (settled) 0f else if ((frame + globalIndex) % 2 == 0) -8f else 8f
    val duration = if (settled) 210 else 54
    val rotation by animateFloatAsState(targetRotation, tween(duration), label = "Die rotation")
    val scale by animateFloatAsState(targetScale, tween(duration), label = "Die scale")
    val translationY by animateFloatAsState(targetY, tween(duration), label = "Die bounce")

    DieFace(
        sides = sides,
        value = shownValue,
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
                this.translationY = translationY
            }
            .clearAndSetSemantics {
                contentDescription = state.t("d$sides rolled $shownValue", "d$sides zeigt $shownValue")
            },
    )
}
