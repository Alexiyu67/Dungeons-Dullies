package app.dulliesanddungeons.ui

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs

internal data class AlignmentChoice(
    val persistedValue: String,
    val englishLabel: String,
    val germanLabel: String,
) {
    fun label(language: UiLanguage): String =
        if (language == UiLanguage.English) englishLabel else germanLabel
}

private val unsetAlignment = AlignmentChoice("", "Not specified", "Nicht angegeben")

internal val standardAlignmentChoices = listOf(
    AlignmentChoice("Lawful Good", "Lawful Good", "Rechtschaffen Gut"),
    AlignmentChoice("Neutral Good", "Neutral Good", "Neutral Gut"),
    AlignmentChoice("Chaotic Good", "Chaotic Good", "Chaotisch Gut"),
    AlignmentChoice("Lawful Neutral", "Lawful Neutral", "Rechtschaffen Neutral"),
    AlignmentChoice("Neutral", "Neutral", "Neutral"),
    AlignmentChoice("Chaotic Neutral", "Chaotic Neutral", "Chaotisch Neutral"),
    AlignmentChoice("Lawful Evil", "Lawful Evil", "Rechtschaffen Böse"),
    AlignmentChoice("Neutral Evil", "Neutral Evil", "Neutral Böse"),
    AlignmentChoice("Chaotic Evil", "Chaotic Evil", "Chaotisch Böse"),
)

internal fun alignmentChoiceFor(value: String): AlignmentChoice? {
    if (value.isBlank()) return unsetAlignment
    return standardAlignmentChoices.firstOrNull { choice ->
        choice.persistedValue.equals(value.trim(), ignoreCase = true) ||
            choice.englishLabel.equals(value.trim(), ignoreCase = true) ||
            choice.germanLabel.equals(value.trim(), ignoreCase = true)
    }
}

internal fun alignmentChoicesFor(currentValue: String): List<AlignmentChoice> {
    val knownChoice = alignmentChoiceFor(currentValue)
    return buildList {
        add(unsetAlignment)
        addAll(standardAlignmentChoices)
        if (currentValue.isNotBlank() && knownChoice == null) {
            val customValue = currentValue.trim()
            add(AlignmentChoice(customValue, customValue, customValue))
        }
    }
}

internal fun alignmentDisplayName(value: String, language: UiLanguage): String =
    alignmentChoiceFor(value)?.label(language) ?: value.trim()

internal fun alignmentsEquivalent(left: String, right: String): Boolean {
    val leftChoice = alignmentChoiceFor(left)
    val rightChoice = alignmentChoiceFor(right)
    return if (leftChoice != null && rightChoice != null) {
        leftChoice.persistedValue == rightChoice.persistedValue
    } else {
        left.trim() == right.trim()
    }
}

@Composable
internal fun AlignmentWheel(
    state: DndAppState,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val choices = remember(value) { alignmentChoicesFor(value) }
    val selectedIndex = choices.indexOfFirst { alignmentsEquivalent(it.persistedValue, value) }
        .coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val coroutineScope = rememberCoroutineScope()
    val rowHeight = 48.dp
    val selectedLabel = choices[selectedIndex].label(state.language)

    LaunchedEffect(listState, choices, value) {
        snapshotFlow {
            if (listState.isScrollInProgress) null else {
                val layoutInfo = listState.layoutInfo
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                layoutInfo.visibleItemsInfo.minByOrNull { item ->
                    abs(item.offset + item.size / 2 - viewportCenter)
                }?.index
            }
        }.distinctUntilChanged().collect { index ->
            val choice = index?.let(choices::getOrNull) ?: return@collect
            if (!alignmentsEquivalent(choice.persistedValue, value)) {
                onValueChange(choice.persistedValue)
            }
        }
    }

    Column(
        modifier = modifier.semantics { stateDescription = selectedLabel },
    ) {
        Text(
            state.t("Alignment", "Gesinnung"),
            modifier = Modifier.padding(bottom = 8.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ) {
            Box(Modifier.fillMaxWidth().height(rowHeight * 3)) {
                Surface(
                    modifier = Modifier.align(Alignment.Center).fillMaxWidth().height(rowHeight),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {}
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(rowHeight * 3),
                    state = listState,
                    flingBehavior = rememberSnapFlingBehavior(listState),
                    contentPadding = PaddingValues(vertical = rowHeight),
                ) {
                    itemsIndexed(choices, key = { _, choice -> choice.persistedValue }) { index, choice ->
                        val selected = alignmentsEquivalent(choice.persistedValue, value)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(rowHeight)
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        if (!selected) onValueChange(choice.persistedValue)
                                        coroutineScope.launch { listState.animateScrollToItem(index) }
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                choice.label(state.language),
                                modifier = Modifier.padding(horizontal = 12.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
