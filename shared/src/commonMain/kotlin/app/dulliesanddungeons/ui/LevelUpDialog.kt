package app.dulliesanddungeons.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

@Composable
internal fun LevelUpDialog(state: DndAppState) {
    val draft = state.levelUpDraft ?: return
    val character = state.selectedCharacter ?: return
    Dialog(
        onDismissRequest = state::cancelLevelUp,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 720.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text(state.t("Level up", "Stufenaufstieg"), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        state.t("Level ${draft.fromLevel} → ${draft.toLevel}", "Stufe ${draft.fromLevel} → ${draft.toLevel}"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        when (draft.step) {
                            0 -> ClassStep(state, draft, character)
                            1 -> HitPointStep(state, draft)
                            2 -> FeatStep(state, draft)
                            3 -> GuidedChoicesStep(state, draft)
                            else -> LevelReview(state, draft)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { if (draft.step == 0) state.cancelLevelUp() else draft.step-- },
                        modifier = Modifier.weight(1f),
                    ) { Text(if (draft.step == 0) state.t("Cancel", "Abbrechen") else state.t("Back", "Zurück")) }
                    Button(
                        onClick = {
                            val needsRoll = draft.step == 1 && draft.hpMethod == HpMethod.Rolled && draft.rolledHitDie == null
                            if (needsRoll) state.rollLevelUpHitDie()
                            else if (draft.step < 4) draft.step++
                            else state.applyLevelUp()
                        },
                        enabled = when (draft.step) {
                            1 -> true
                            2 -> state.levelUpFeatSelectionValid(draft)
                            3 -> state.levelUpGuidedChoicesValid(draft)
                            else -> true
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        val needsRoll = draft.step == 1 && draft.hpMethod == HpMethod.Rolled && draft.rolledHitDie == null
                        if (draft.step == 4) Icon(Icons.Rounded.Check, contentDescription = null)
                        else if (needsRoll) Icon(Icons.Rounded.Casino, contentDescription = null)
                        if (draft.step == 4) {
                            Text(state.t("Apply", "Anwenden"))
                        } else if (needsRoll) {
                            Text(state.t("Roll", "Würfeln"))
                        } else {
                            Text(state.t("Continue", "Weiter"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassStep(state: DndAppState, draft: LevelUpDraft, character: CharacterUi) {
    val available = state.levelUpClassOptions(character)
    val currentClasses = character.progression.map { it.className }.distinct()
        .ifEmpty { listOf(character.className) }
        .filter { it in available }
        .sortedForPicker(state.language, { it }, { it })
    val otherClasses = available.filterNot { it in currentClasses }
        .sortedForPicker(state.language, { it }, { it })
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(state.t("Which class gains the level?", "Welche Klasse erhält die Stufe?"), style = MaterialTheme.typography.titleMedium)
        currentClasses.forEach { className ->
            val classLevel = character.progression.count { it.className == className }.coerceAtLeast(1)
            OutlinedCard(
                onClick = { state.selectLevelUpClass(className) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (draft.className == className) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f)
                    else MaterialTheme.colorScheme.surface,
                ),
            ) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(className, style = MaterialTheme.typography.titleLarge)
                        Text(
                            state.t("Current class · level $classLevel", "Aktuelle Klasse · Stufe $classLevel"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    RadioButton(selected = draft.className == className, onClick = { state.selectLevelUpClass(className) })
                }
            }
        }
        if (otherClasses.isNotEmpty()) {
            Text(state.t("Other classes", "Andere Klassen"), style = MaterialTheme.typography.titleSmall)
            otherClasses.chunked(2).forEach { rowClasses ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowClasses.forEach { className ->
                        OutlinedCard(
                            onClick = { state.selectLevelUpClass(className) },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (draft.className == className) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f)
                                else MaterialTheme.colorScheme.surface,
                            ),
                        ) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(className, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                RadioButton(selected = draft.className == className, onClick = { state.selectLevelUpClass(className) })
                            }
                        }
                    }
                    if (rowClasses.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HitPointStep(state: DndAppState, draft: LevelUpDraft) {
    var hitDieMenuOpen by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(state.t("Additional Hit Points", "Zusätzliche Trefferpunkte"), style = MaterialTheme.typography.titleMedium)
        SimpleHpMethodCard(state, draft, HpMethod.Fixed, state.t("Use the fixed class value", "Festen Klassenwert verwenden"))
        OutlinedCard(
            onClick = { draft.hpMethod = HpMethod.Rolled },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(
                containerColor = if (draft.hpMethod == HpMethod.Rolled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .4f)
                else MaterialTheme.colorScheme.surface,
            ),
        ) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = draft.hpMethod == HpMethod.Rolled, onClick = { draft.hpMethod = HpMethod.Rolled })
                Column(Modifier.weight(1f)) {
                    Text(state.t("Roll the class Hit Die", "Klassentrefferwürfel würfeln"))
                    Box {
                        TextButton(onClick = { draft.hpMethod = HpMethod.Rolled; hitDieMenuOpen = true }) {
                            Text("1d${draft.hitDieSides}", fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = hitDieMenuOpen, onDismissRequest = { hitDieMenuOpen = false }) {
                            supportedClassHitDice.forEach { sides ->
                                DropdownMenuItem(
                                    text = { Text("1d$sides") },
                                    onClick = {
                                        state.selectLevelUpHitDie(sides)
                                        draft.hpMethod = HpMethod.Rolled
                                        hitDieMenuOpen = false
                                    },
                                )
                            }
                        }
                    }
                }
                Surface(
                    onClick = { draft.hpMethod = HpMethod.Rolled; state.rollLevelUpHitDie() },
                    color = Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp).semantics { role = Role.Button },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        DieFace(draft.hitDieSides, draft.rolledHitDie, Modifier.size(58.dp))
                        Text(state.t("Roll", "Würfeln"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        SimpleHpMethodCard(state, draft, HpMethod.Manual, state.t("Enter the total gain manually", "Gesamten Zuwachs manuell eingeben"))
        when (draft.hpMethod) {
            HpMethod.Rolled -> Unit
            HpMethod.Manual -> {
                Text(state.t("HP gain: ${draft.manualHitDie}", "TP-Zuwachs: ${draft.manualHitDie}"), fontWeight = FontWeight.Bold)
                Slider(
                    value = draft.manualHitDie.toFloat(),
                    onValueChange = { draft.manualHitDie = it.roundToInt() },
                    valueRange = 1f..30f,
                    steps = 28,
                )
            }
            HpMethod.Fixed -> Unit
        }
        Row(
            Modifier.fillMaxWidth().toggleable(
                value = draft.healByIncrease,
                role = Role.Checkbox,
                onValueChange = { draft.healByIncrease = it },
            ).padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = draft.healByIncrease, onCheckedChange = null)
            Spacer(Modifier.width(8.dp))
            Text(state.t("Also heal by the HP increase", "Zusätzlich um den TP-Zuwachs heilen"))
        }
    }
}

@Composable
private fun SimpleHpMethodCard(state: DndAppState, draft: LevelUpDraft, method: HpMethod, label: String) {
    OutlinedCard(
        onClick = { draft.hpMethod = method },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (draft.hpMethod == method) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .4f)
            else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = draft.hpMethod == method, onClick = { draft.hpMethod = method })
            Text(label)
        }
    }
}

@Composable
private fun FeatStep(state: DndAppState, draft: LevelUpDraft) {
    var featListOpen by remember(draft.toLevel) { mutableStateOf(false) }
    val featOptions = state.levelUpFeatOptions().sortedForPicker(state.language, FeatOptionUi::name, FeatOptionUi::id)
    val selectedFeat = featOptions.firstOrNull { it.id == draft.selectedFeatId }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(state.t("Feat or improvement", "Talent oder Verbesserung"), style = MaterialTheme.typography.titleMedium)
        if (!state.levelUpFeatAvailable(draft)) {
            Text(state.t("This level has no feat choice.", "Diese Stufe enthält keine Talentwahl."))
        } else {
            OutlinedCard(onClick = { featListOpen = true }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(selectedFeat?.name ?: state.t("Search and choose a feat", "Talent suchen und wählen"), style = MaterialTheme.typography.titleSmall)
                        selectedFeat?.category?.takeIf(String::isNotBlank)?.let { category ->
                            Text(category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                }
            }
            if (draft.selectedFeatId == "ability-score-improvement") {
                Text(
                    state.t("Choose two +1 increases. Tap one ability twice for +2.", "Wähle zwei Erhöhungen um +1. Tippe für +2 zweimal auf denselben Wert."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                listOf("STR", "DEX", "CON", "INT", "WIS", "CHA").forEach { ability ->
                    val increase = draft.abilityIncreases[ability] ?: 0
                    val score = state.selectedCharacter?.abilities?.get(ability) ?: 10
                    FilterChip(
                        selected = increase > 0,
                        onClick = { state.cycleLevelUpAbilityIncrease(ability) },
                        label = { Text("$ability  $score${if (increase > 0) " → ${score + increase}" else ""}") },
                    )
                }
            }
        }
    }
    if (featListOpen) {
        FeatPickerDialog(
            state = state,
            options = featOptions,
            selectedIds = setOfNotNull(draft.selectedFeatId),
            selectionLimit = 1,
            singleSelection = true,
            onToggle = { feat ->
                state.selectLevelUpFeat(feat.id)
                featListOpen = false
            },
            onDismiss = { featListOpen = false },
        )
    }
}

@Composable
private fun GuidedChoicesStep(state: DndAppState, draft: LevelUpDraft) {
    val choices = state.levelUpGuidedChoices(draft)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(state.t("Class choices", "Klassenwahlen"), style = MaterialTheme.typography.titleMedium)
        if (choices.isEmpty()) {
            Text(state.t("No choices are required at this level.", "Auf dieser Stufe ist keine Auswahl erforderlich."))
        }
        choices.forEach { choice ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val selected = draft.guidedSelections[choice.id].orEmpty()
                Text(choice.title, style = MaterialTheme.typography.titleSmall)
                if (choice.chooseCount > 1) {
                    Text(
                        "${selected.size}/${choice.chooseCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                choice.options.sortedForPicker(state.language, GuidedLevelOptionUi::name, GuidedLevelOptionUi::id).forEach { option ->
                    OutlinedCard(
                        onClick = { state.toggleLevelUpGuidedOption(choice.id, option.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (choice.chooseCount == 1) {
                                RadioButton(
                                    selected = option.id in selected,
                                    onClick = { state.toggleLevelUpGuidedOption(choice.id, option.id) },
                                )
                            } else {
                                Checkbox(
                                    checked = option.id in selected,
                                    onCheckedChange = { state.toggleLevelUpGuidedOption(choice.id, option.id) },
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Text(option.name, style = MaterialTheme.typography.titleSmall)
                                    val tokens = option.feature?.toCostTokens() ?: option.spell?.activationCost?.toCostTokens().orEmpty()
                                    if (tokens.isNotEmpty()) CostChipRow(state, tokens)
                                }
                                Text(option.summary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelReview(state: DndAppState, draft: LevelUpDraft) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(state.t("Review level ${draft.toLevel}", "Stufe ${draft.toLevel} prüfen"), style = MaterialTheme.typography.titleMedium)
        Text(state.t("Class: ${draft.className}", "Klasse: ${draft.className}"))
        Text(
            state.t(
                "HP: ${draft.hpMethod.name} · 1d${draft.hitDieSides}${draft.rolledHitDie?.let { " · rolled $it" }.orEmpty()}",
                "TP: ${draft.hpMethod.name} · 1d${draft.hitDieSides}${draft.rolledHitDie?.let { " · $it gewürfelt" }.orEmpty()}",
            ),
        )
        draft.selectedFeatId?.let { selected ->
            if (selected == "ability-score-improvement") {
                val increases = draft.abilityIncreases.entries.joinToString { "${it.key} +${it.value}" }
                Text(state.t("Abilities: $increases", "Attribute: $increases"))
            } else {
                Text(state.t("Feat: $selected", "Talent: $selected"))
            }
        }
        val guided = state.levelUpGuidedChoices(draft)
        val selectedNames = guided.flatMap { choice ->
            val ids = draft.guidedSelections[choice.id].orEmpty()
            choice.options.filter { it.id in ids }.map { it.name }
        }
        if (selectedNames.isNotEmpty()) Text(state.t("Choices: ${selectedNames.joinToString()}", "Auswahl: ${selectedNames.joinToString()}"))
        Text(
            state.t("Applying this step records it in the character's progression ledger.", "Dieser Schritt wird im Stufenverlauf des Charakters gespeichert."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
