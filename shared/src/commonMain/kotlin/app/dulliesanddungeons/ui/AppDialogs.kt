package app.dulliesanddungeons.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AirlineSeatFlat
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HearingDisabled
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun DeleteCharacterConfirmationDialog(
    state: DndAppState,
    character: CharacterUi,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(state.t("Delete ${character.name}?", "${character.name} löschen?")) },
        text = {
            Text(
                state.t(
                    "This permanently deletes the character, including notes, sessions, and turn progress. This cannot be undone.",
                    "Dadurch wird der Charakter einschließlich Notizen, Sitzungen und Zugfortschritt dauerhaft gelöscht. Dies kann nicht rückgängig gemacht werden.",
                )
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(state.t("Cancel", "Abbrechen")) }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(state.t("Delete", "Löschen"), color = MaterialTheme.colorScheme.error)
            }
        },
    )
}

@Composable
internal fun SearchDialog(state: DndAppState) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    val results = state.search(query.text)
    Dialog(
        onDismissRequest = { state.searchOpen = false },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { state.searchOpen = false }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = state.t("Close search", "Suche schließen"))
                    }
                    Text(state.t("Find anything", "Alles finden"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    RulesetBadge(state.selectedCharacter?.ruleset ?: Ruleset.Fifth2024)
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    placeholder = { Text(state.t("Try “golem”, “conjuration”, “dwarf”…", "Versuche „Golem“, „Beschwörung“, „Zwerg“ …")) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = if (query.text.isNotEmpty()) {{
                        IconButton(onClick = { query = TextFieldValue("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = state.t("Clear search", "Suche leeren"))
                        }
                    }} else null,
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                )
                Spacer(Modifier.height(10.dp))
                if (query.text.isBlank()) {
                    Text(
                        state.t("PLAYER WIKI & QUICK ACTIONS", "SPIELER-WIKI & SCHNELLAKTIONEN"),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        state.t("${results.size} results on this character and its rules", "${results.size} Ergebnisse für diesen Charakter und seine Regeln"),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (results.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.outline)
                            Text(state.t("No match yet", "Noch kein Treffer"), style = MaterialTheme.typography.titleMedium)
                            Text(state.t("Try a shorter term or a common phrase.", "Versuche einen kürzeren Begriff oder eine geläufige Formulierung."), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(results, key = { it.id }) { result ->
                            SearchResultCard(state, result)
                        }
                        item {
                            ExplanationCard(
                                state.t("Only the selected rules apply", "Nur die gewählten Regeln gelten"),
                                state.t("Knowledge results are tagged to ${state.selectedCharacter?.ruleset?.longLabel}. Nothing is fetched from the internet.", "Wissenseinträge sind ${state.selectedCharacter?.ruleset?.longLabel} zugeordnet. Nichts wird aus dem Internet geladen."),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(state: DndAppState, result: SearchResultUi) {
    Card(
        onClick = { state.handleSearchResult(result) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    ) {
        ListItem(
            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            leadingContent = {
                Icon(
                    when (result.kind) {
                        SearchResultKind.Roll -> Icons.Rounded.Casino
                        SearchResultKind.Action -> Icons.Rounded.PlayArrow
                        SearchResultKind.Rule -> Icons.Rounded.Info
                        SearchResultKind.Note -> Icons.Rounded.NoteAlt
                        SearchResultKind.Navigate -> Icons.Rounded.Search
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            headlineContent = {
                Column {
                    Text(result.title, fontWeight = FontWeight.SemiBold)
                    val tokens = result.cost.toCostTokens() + result.resourceLabel?.let { listOf(CostTokenUi(CostTokenKind.Resource, labelOverride = it)) }.orEmpty()
                    if (tokens.isNotEmpty()) CostChipRow(state, tokens)
                }
            },
            supportingContent = { Text(result.subtitle) },
            trailingContent = {
                Text(result.actionLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            },
        )
    }
}

@Composable
internal fun ConditionsDialog(state: DndAppState) {
    val options = if (state.selectedCharacter?.ruleset == Ruleset.Pf2eRemaster) {
        listOf(
            "Blinded", "Clumsy", "Confused", "Controlled", "Deafened", "Drained", "Enfeebled",
            "Frightened", "Grabbed", "Immobilized", "Invisible", "Off-Guard", "Paralyzed",
            "Persistent Damage", "Prone", "Quickened", "Restrained", "Sickened", "Slowed",
            "Stunned", "Unconscious", "Custom effect",
        )
    } else {
        listOf(
            "Blinded", "Charmed", "Deafened", "Exhaustion", "Frightened", "Grappled", "Incapacitated", "Inspiration",
            "Invisible", "Paralyzed", "Petrified", "Poisoned", "Prone", "Restrained", "Stunned",
            "Unconscious", "Concentrating", "Custom effect",
        )
    }
    val quickOptions = options.filter { option ->
        option == "Exhaustion" || option == "Custom effect" || state.selectedConditions.none { it.name.startsWith(option, ignoreCase = true) }
    }.sortedForPicker(
        language = state.language,
        displayName = { option ->
            if (option == "Custom effect") state.t("Custom effect…", "Eigener Effekt …") else option
        },
    ).sortedBy { it == "Custom effect" }
    var exhaustionLevel by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = { state.conditionsOpen = false },
        icon = { Icon(Icons.Rounded.MedicalServices, contentDescription = null) },
        title = { Text(state.t("Conditions & effects", "Zustände & Effekte")) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (state.selectedConditions.isNotEmpty()) {
                    item { Text(state.t("ACTIVE", "AKTIV"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                    items(state.selectedConditions, key = { it.id.ifBlank { "condition-${it.name}-${it.source}-${it.characterId}" } }) { condition ->
                        val inspiration = condition.isInspiration()
                        val infoBody = if (condition.name.equals("Custom effect", ignoreCase = true)) {
                            null
                        } else {
                            state.conditionInfo(condition.name, condition.level)
                                ?: condition.explanation.takeIf(String::isNotBlank)
                        }
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (inspiration) {
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
                                } else {
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                },
                            ),
                        ) {
                            Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    conditionIcon(condition.name),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (inspiration) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                )
                                Spacer(Modifier.width(9.dp))
                                Text(condition.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                if (infoBody != null) {
                                    IconButton(onClick = { state.showInfo(condition.name, infoBody) }) {
                                        Icon(
                                            Icons.Rounded.Info,
                                            contentDescription = state.t("Condition info for ${condition.name}", "Zustandsinfo für ${condition.name}"),
                                            modifier = Modifier.size(19.dp),
                                        )
                                    }
                                }
                                if (condition.removable) TextButton(onClick = { state.removeCondition(condition) }) { Text(state.t("Remove", "Entfernen")) }
                            }
                        }
                    }
                    item { HorizontalDivider(Modifier.padding(vertical = 5.dp)) }
                }
                item { Text(state.t("QUICK ADD", "SCHNELL HINZUFÜGEN"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                items(quickOptions) { condition ->
                    val addCondition = {
                        if (condition == "Exhaustion") exhaustionLevel = state.selectedCharacter?.exhaustionLevel?.coerceAtLeast(1) ?: 1
                        else state.addCondition(condition)
                    }
                    val infoLevel = if (condition == "Exhaustion") {
                        state.selectedCharacter?.exhaustionLevel?.coerceAtLeast(1) ?: 1
                    } else 1
                    ConditionQuickAddRow(
                        state = state,
                        condition = condition,
                        infoBody = state.conditionInfo(condition, infoLevel),
                        onAdd = addCondition,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { state.conditionsOpen = false }) { Text(state.t("Done", "Fertig")) }
        },
    )

    exhaustionLevel?.let { selectedLevel ->
        AlertDialog(
            onDismissRequest = { exhaustionLevel = null },
            icon = { Icon(Icons.Rounded.BatteryAlert, contentDescription = null) },
            title = { Text(state.t("Exhaustion level", "Erschöpfungsgrad")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(state.t("Choose the character's current level.", "Wähle den aktuellen Grad des Charakters."), style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..3).forEach { level ->
                            FilterChip(selected = selectedLevel == level, onClick = { exhaustionLevel = level }, label = { Text(level.toString()) }, modifier = Modifier.weight(1f))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (4..6).forEach { level ->
                            FilterChip(selected = selectedLevel == level, onClick = { exhaustionLevel = level }, label = { Text(level.toString()) }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            dismissButton = { TextButton(onClick = { exhaustionLevel = null }) { Text(state.t("Cancel", "Abbrechen")) } },
            confirmButton = {
                Button(onClick = {
                    state.setExhaustion(selectedLevel)
                    state.conditionsOpen = false
                    exhaustionLevel = null
                }) { Text(state.t("Apply", "Anwenden")) }
            },
        )
    }
}

@Composable
private fun ConditionQuickAddRow(
    state: DndAppState,
    condition: String,
    infoBody: String?,
    onAdd: () -> Unit,
) {
    val addLabel = state.t("Add $condition", "$condition hinzufügen")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(onClickLabel = addLabel, role = Role.Button, onClick = onAdd)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(conditionIcon(condition), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    if (condition == "Custom effect") state.t("Custom effect…", "Eigener Effekt …") else condition,
                    modifier = Modifier.weight(1f),
                )
            }
            if (infoBody != null) {
                IconButton(onClick = { state.showInfo(condition, infoBody) }) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = state.t("Condition info for $condition", "Zustandsinfo für $condition"),
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Rounded.Add, contentDescription = addLabel, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun conditionIcon(name: String): ImageVector = when {
    name.equals("Inspiration", true) -> Icons.Rounded.AutoAwesome
    name.startsWith("Blinded", true) -> Icons.Rounded.VisibilityOff
    name.startsWith("Charmed", true) -> Icons.Rounded.Favorite
    name.startsWith("Clumsy", true) -> Icons.Rounded.AirlineSeatFlat
    name.startsWith("Confused", true) -> Icons.Rounded.Psychology
    name.startsWith("Controlled", true) -> Icons.Rounded.Lock
    name.startsWith("Deafened", true) -> Icons.Rounded.HearingDisabled
    name.startsWith("Drained", true) -> Icons.Rounded.BatteryAlert
    name.startsWith("Enfeebled", true) -> Icons.Rounded.Science
    name.startsWith("Exhaust", true) -> Icons.Rounded.BatteryAlert
    name.startsWith("Frightened", true) -> Icons.Rounded.Warning
    name.startsWith("Grab", true) || name.startsWith("Grappled", true) -> Icons.Rounded.Link
    name.startsWith("Immobilized", true) -> Icons.Rounded.Block
    name.startsWith("Incapacitated", true) -> Icons.Rounded.Block
    name.startsWith("Invisible", true) -> Icons.Rounded.Visibility
    name.startsWith("Off-Guard", true) -> Icons.Rounded.Warning
    name.startsWith("Paralyzed", true) -> Icons.Rounded.PauseCircle
    name.startsWith("Persistent Damage", true) -> Icons.Rounded.ElectricBolt
    name.startsWith("Petrified", true) -> Icons.Rounded.Landscape
    name.startsWith("Poisoned", true) -> Icons.Rounded.Science
    name.startsWith("Prone", true) -> Icons.Rounded.AirlineSeatFlat
    name.startsWith("Quickened", true) -> Icons.Rounded.AutoAwesome
    name.startsWith("Restrained", true) -> Icons.Rounded.Lock
    name.startsWith("Sickened", true) -> Icons.Rounded.MedicalServices
    name.startsWith("Slowed", true) -> Icons.Rounded.PauseCircle
    name.startsWith("Stunned", true) -> Icons.Rounded.ElectricBolt
    name.startsWith("Unconscious", true) -> Icons.Rounded.VisibilityOff
    name.startsWith("Concentrating", true) -> Icons.Rounded.Psychology
    else -> Icons.Rounded.Tune
}

@Composable
internal fun ConversionDialog(state: DndAppState) {
    val source = state.selectedCharacter ?: return
    var target by remember(source.id) { mutableStateOf(Ruleset.entries.first { it != source.ruleset }) }
    AlertDialog(
        onDismissRequest = { state.conversionOpen = false },
        icon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = null) },
        title = { Text(state.t("Create a converted copy", "Konvertierte Kopie erstellen")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Text(state.t("${source.name} stays exactly as it is. Choose the rules for the new character:", "${source.name} bleibt genau wie bisher. Wähle die Regeln für den neuen Charakter:"))
                Ruleset.entries.filter { it != source.ruleset }.forEach { ruleset ->
                    FilterChip(
                        selected = target == ruleset,
                        onClick = { target = ruleset },
                        label = { Text(ruleset.longLabel) },
                        leadingIcon = if (target == ruleset) {{ Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp)) }} else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                ExplanationCard(
                    if (source.ruleset == Ruleset.Pf2eRemaster || target == Ruleset.Pf2eRemaster) state.t("Guided rebuild", "Geführter Neuaufbau") else state.t("Guided conversion", "Geführte Konvertierung"),
                    if (source.ruleset == Ruleset.Pf2eRemaster || target == Ruleset.Pf2eRemaster)
                        state.t("We keep the name, portrait, concept and level, then help you reselect every mechanical choice.", "Wir behalten Name, Porträt, Konzept und Stufe und helfen dann bei jeder neuen Regelwahl.")
                    else state.t("Clear mappings are applied. Changed or ambiguous choices wait for your review.", "Eindeutige Zuordnungen werden angewendet. Geänderte oder unklare Wahlen warten auf deine Prüfung."),
                )
            }
        },
        dismissButton = { TextButton(onClick = { state.conversionOpen = false }) { Text(state.t("Cancel", "Abbrechen")) } },
        confirmButton = {
            Button(onClick = { state.convert(target) }) { Text(state.t("Create copy", "Kopie erstellen")) }
        },
    )
}
