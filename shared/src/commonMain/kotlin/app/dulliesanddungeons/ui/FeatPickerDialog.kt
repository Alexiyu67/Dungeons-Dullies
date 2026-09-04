package app.dulliesanddungeons.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

internal fun filterFeatOptions(options: List<FeatOptionUi>, query: String): List<FeatOptionUi> {
    val needle = query.trim()
    if (needle.isBlank()) return options
    return options.filter { feat ->
        buildList {
            add(feat.name)
            add(feat.summary)
            feat.category?.let(::add)
            feat.recommendedReason?.let(::add)
            addAll(feat.searchTerms)
        }.any { it.contains(needle, ignoreCase = true) }
    }
}

internal fun showFeatInfo(state: DndAppState, feat: FeatOptionUi) {
    val body = buildString {
        append(feat.summary.ifBlank { state.t("No additional details supplied.", "Keine weiteren Details angegeben.") })
        feat.recommendedReason?.let { reason ->
            append("\n\n")
            append(state.t("Why it may fit: $reason", "Warum es passen könnte: $reason"))
        }
    }
    state.showInfo(feat.name, body)
}

@Composable
internal fun FeatPickerDialog(
    state: DndAppState,
    options: List<FeatOptionUi>,
    selectedIds: Set<String>,
    selectionLimit: Int?,
    singleSelection: Boolean,
    onToggle: (FeatOptionUi) -> Unit,
    onDismiss: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(search, options) { filterFeatOptions(options, search) }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(.9f),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(Modifier.fillMaxSize()) {
                Surface(shadowElevation = 5.dp, tonalElevation = 2.dp) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(state.t("Choose feat", "Talent wählen"), style = MaterialTheme.typography.titleLarge)
                                selectionLimit?.takeUnless { singleSelection }?.let { limit ->
                                    Text(
                                        "${selectedIds.size}/$limit",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            TextButton(onClick = onDismiss) { Text(state.t("Done", "Fertig")) }
                        }
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            label = { Text(state.t("Search feats", "Talente suchen")) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    if (filtered.isEmpty()) item {
                        Text(
                            state.t("No matching feats.", "Keine passenden Talente."),
                            modifier = Modifier.padding(vertical = 20.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(filtered, key = FeatOptionUi::id) { feat ->
                        val selected = feat.id in selectedIds
                        val atLimit = !singleSelection && selectionLimit != null && selectedIds.size >= selectionLimit
                        OutlinedCard(
                            onClick = { onToggle(feat) },
                            enabled = selected || !atLimit,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(15.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .5f) else MaterialTheme.colorScheme.surface,
                            ),
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(start = 10.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (singleSelection) {
                                    RadioButton(selected = selected, onClick = null)
                                } else {
                                    Checkbox(checked = selected, onCheckedChange = null)
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(feat.name, style = MaterialTheme.typography.titleSmall)
                                    feat.category?.takeIf(String::isNotBlank)?.let { category ->
                                        Text(
                                            category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    Text(
                                        feat.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    feat.recommendedReason?.let { reason ->
                                        Text(
                                            state.t("Recommended · $reason", "Empfohlen · $reason"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                if (selected) {
                                    Icon(Icons.Rounded.Check, contentDescription = state.t("Selected", "Ausgewählt"), tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { showFeatInfo(state, feat) }) {
                                    Icon(Icons.Rounded.Info, contentDescription = state.t("About ${feat.name}", "Info zu ${feat.name}"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
