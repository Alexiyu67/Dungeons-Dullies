package app.dulliesanddungeons.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.dulliesanddungeons.domain.ActionCost
import app.dulliesanddungeons.domain.Recovery
import app.dulliesanddungeons.domain.RollMode

private enum class CustomActionKind { NONE, ACTION, BONUS_ACTION, REACTION }

private data class CustomFeatureDraft(
    val name: String,
    val summary: String,
    val minimumLevel: Int,
    val actionKind: CustomActionKind,
    val maximumUses: Int,
    val recovery: Recovery,
    val suggestInTurnGuide: Boolean,
)

private data class CustomSpellDraft(
    val name: String,
    val spellLevel: Int,
    val minimumLevel: Int,
)

@Composable
internal fun SubclassPickerDialog(state: DndAppState, onDismiss: () -> Unit) {
    var customMode by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 720.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            if (customMode) {
                CustomSubclassForm(state, onBack = { customMode = false }, onDone = onDismiss)
            } else {
                KnownSubclassList(state, onCustom = { customMode = true }, onDone = onDismiss)
            }
        }
    }
}

@Composable
private fun KnownSubclassList(state: DndAppState, onCustom: () -> Unit, onDone: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val options = state.creationSubclassOptions().filter { option ->
        query.isBlank() || option.name.contains(query, true) || option.summary(state.language).contains(query, true)
    }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(state.t("Choose subclass", "Unterklasse w\u00e4hlen"), style = MaterialTheme.typography.headlineSmall)
        Text(
            "${state.creation.className} \u00b7 ${state.creation.ruleset.shortLabel}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it.take(80) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(state.t("Search", "Suchen")) },
            singleLine = true,
        )
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                OutlinedCard(
                    onClick = {
                        state.selectCreationSubclass(option.id)
                        onDone()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (state.creation.subclassId == option.id) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else MaterialTheme.colorScheme.surface
                    ),
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
                        RadioButton(selected = state.creation.subclassId == option.id, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(option.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                if (option.local) Text(state.t("Custom", "Eigen"), style = MaterialTheme.typography.labelSmall)
                            }
                            Text(option.summary(state.language), style = MaterialTheme.typography.bodySmall)
                            Text(
                                state.t(
                                    "Mechanics start at class level ${option.mechanics.selectionLevel}",
                                    "Regeln ab Klassenstufe ${option.mechanics.selectionLevel}",
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (options.isEmpty()) {
                Text(state.t("No matching subclasses", "Keine passenden Unterklassen"))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (!state.creationSubclassRequired()) {
                TextButton(onClick = {
                    state.selectCreationSubclass(null)
                    onDone()
                }) { Text(state.t("Choose later", "Sp\u00e4ter w\u00e4hlen")) }
            } else Spacer(Modifier.width(1.dp))
            TextButton(onClick = onCustom) { Text(state.t("Custom subclass", "Eigene Unterklasse")) }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun CustomSubclassForm(state: DndAppState, onBack: () -> Unit, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var selectionLevel by remember { mutableStateOf("3") }
    var armorFormula by remember { mutableStateOf(SubclassArmorFormulaUi.NONE) }
    var armorClassBonus by remember { mutableStateOf("0") }
    var hitPointsPerLevel by remember { mutableStateOf("0") }
    var initiativeBonus by remember { mutableStateOf("0") }
    var initiativeHalfProficiency by remember { mutableStateOf(false) }
    var initiativeRollMode by remember { mutableStateOf(RollMode.NORMAL) }
    var speedBonus by remember { mutableStateOf("0") }
    var saveBonus by remember { mutableStateOf("0") }
    var attackBonus by remember { mutableStateOf("0") }
    var criticalThreshold by remember { mutableStateOf("20") }

    val features = remember { mutableStateListOf<CustomFeatureDraft>() }
    var featureName by remember { mutableStateOf("") }
    var featureSummary by remember { mutableStateOf("") }
    var featureLevel by remember { mutableStateOf("3") }
    var featureAction by remember { mutableStateOf(CustomActionKind.NONE) }
    var featureUses by remember { mutableStateOf("0") }
    var featureRecovery by remember { mutableStateOf(Recovery.MANUAL) }
    var featureSuggested by remember { mutableStateOf(false) }

    val spells = remember { mutableStateListOf<CustomSpellDraft>() }
    var spellName by remember { mutableStateOf("") }
    var spellLevel by remember { mutableStateOf("1") }
    var spellMinimumLevel by remember { mutableStateOf("3") }

    val safeSelectionLevel = selectionLevel.toIntOrNull()?.coerceIn(1, 20) ?: 3
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(state.t("Custom subclass", "Eigene Unterklasse"), style = MaterialTheme.typography.headlineSmall)
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(name, { name = it.take(60) }, Modifier.fillMaxWidth(), label = { Text(state.t("Name", "Name")) }, singleLine = true)
            OutlinedTextField(summary, { summary = it.take(240) }, Modifier.fillMaxWidth(), label = { Text(state.t("Summary", "Kurzbeschreibung")) }, minLines = 2)
            NumericField(state.t("Selection level", "Wahlstufe"), selectionLevel) { selectionLevel = it }

            Text(state.t("Automatic stats", "Automatische Werte"), style = MaterialTheme.typography.titleMedium)
            Text(state.t("Unarmored Armor Class", "R\u00fcstungsklasse ohne R\u00fcstung"), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    SubclassArmorFormulaUi.NONE to state.t("Normal", "Normal"),
                    SubclassArmorFormulaUi.DEXTERITY_13 to "13 + DEX",
                    SubclassArmorFormulaUi.DEXTERITY_AND_CHARISMA_10 to "10 + DEX + CHA",
                ).forEach { (formula, label) ->
                    FilterChip(selected = armorFormula == formula, onClick = { armorFormula = formula }, label = { Text(label) })
                }
            }
            NumericField(state.t("Armor Class bonus", "RK-Bonus"), armorClassBonus) { armorClassBonus = it }
            NumericField(state.t("Hit Points per class level", "TP pro Klassenstufe"), hitPointsPerLevel) { hitPointsPerLevel = it }
            NumericField(state.t("Initiative bonus", "Initiativebonus"), initiativeBonus) { initiativeBonus = it }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(initiativeHalfProficiency, { initiativeHalfProficiency = it })
                Text(state.t("Add half Proficiency Bonus to initiative", "Halben \u00dcbungsbonus zur Initiative addieren"))
            }
            Text(state.t("Initiative roll", "Initiativewurf"), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    RollMode.NORMAL to state.t("Normal", "Normal"),
                    RollMode.ADVANTAGE to state.t("Advantage", "Vorteil"),
                    RollMode.DISADVANTAGE to state.t("Disadvantage", "Nachteil"),
                ).forEach { (mode, label) ->
                    FilterChip(selected = initiativeRollMode == mode, onClick = { initiativeRollMode = mode }, label = { Text(label) })
                }
            }
            NumericField(state.t("Speed bonus (feet)", "Bewegungsbonus (Fu\u00df)"), speedBonus) { speedBonus = it }
            NumericField(state.t("Saving throw bonus", "Rettungswurfbonus"), saveBonus) { saveBonus = it }
            NumericField(state.t("Attack bonus", "Angriffsbonus"), attackBonus) { attackBonus = it }
            NumericField(state.t("Critical threshold", "Kritischer Schwellenwert"), criticalThreshold) { criticalThreshold = it }

            Text(state.t("Features", "Merkmale"), style = MaterialTheme.typography.titleMedium)
            features.forEachIndexed { index, feature ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(feature.name, fontWeight = FontWeight.SemiBold)
                            Text(state.t("Level ${feature.minimumLevel}", "Stufe ${feature.minimumLevel}"), style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { features.removeAt(index) }) { Text(state.t("Remove", "Entfernen")) }
                    }
                }
            }
            OutlinedTextField(featureName, { featureName = it.take(60) }, Modifier.fillMaxWidth(), label = { Text(state.t("Feature name", "Merkmalsname")) }, singleLine = true)
            OutlinedTextField(featureSummary, { featureSummary = it.take(240) }, Modifier.fillMaxWidth(), label = { Text(state.t("Feature summary", "Merkmalsbeschreibung")) }, minLines = 2)
            NumericField(state.t("Feature level", "Merkmalsstufe"), featureLevel) { featureLevel = it }
            Text(state.t("Action cost", "Aktionskosten"), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CustomActionKind.entries.forEach { kind ->
                    FilterChip(selected = featureAction == kind, onClick = { featureAction = kind }, label = { Text(actionLabel(state, kind)) })
                }
            }
            NumericField(state.t("Maximum uses (0 = unlimited)", "Maximale Nutzungen (0 = unbegrenzt)"), featureUses) { featureUses = it }
            Text(state.t("Recovery", "Erholung"), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(Recovery.MANUAL, Recovery.SHORT_REST, Recovery.LONG_REST).forEach { recovery ->
                    FilterChip(selected = featureRecovery == recovery, onClick = { featureRecovery = recovery }, label = { Text(recoveryLabel(state, recovery)) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(featureSuggested, { featureSuggested = it })
                Text(state.t("Suggest in Turn Guide", "Im Zugleitfaden vorschlagen"))
            }
            TextButton(
                enabled = featureName.isNotBlank(),
                onClick = {
                    features += CustomFeatureDraft(
                        featureName.trim(), featureSummary.trim(), featureLevel.toIntOrNull()?.coerceIn(1, 20) ?: safeSelectionLevel,
                        featureAction, featureUses.toIntOrNull()?.coerceIn(0, 99) ?: 0, featureRecovery, featureSuggested,
                    )
                    featureName = ""
                    featureSummary = ""
                },
            ) { Text(state.t("Add feature", "Merkmal hinzuf\u00fcgen")) }

            Text(state.t("Granted spells", "Gew\u00e4hrte Zauber"), style = MaterialTheme.typography.titleMedium)
            spells.forEachIndexed { index, spell ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${spell.name} \u00b7 ${state.t("spell level", "Zaubergrad")} ${spell.spellLevel}", Modifier.weight(1f))
                    TextButton(onClick = { spells.removeAt(index) }) { Text(state.t("Remove", "Entfernen")) }
                }
            }
            OutlinedTextField(spellName, { spellName = it.take(60) }, Modifier.fillMaxWidth(), label = { Text(state.t("Spell name", "Zaubername")) }, singleLine = true)
            NumericField(state.t("Spell level", "Zaubergrad"), spellLevel) { spellLevel = it }
            NumericField(state.t("Granted at class level", "Gew\u00e4hrt auf Klassenstufe"), spellMinimumLevel) { spellMinimumLevel = it }
            TextButton(
                enabled = spellName.isNotBlank(),
                onClick = {
                    spells += CustomSpellDraft(
                        spellName.trim(), spellLevel.toIntOrNull()?.coerceIn(0, 9) ?: 0,
                        spellMinimumLevel.toIntOrNull()?.coerceIn(1, 20) ?: safeSelectionLevel,
                    )
                    spellName = ""
                },
            ) { Text(state.t("Add spell", "Zauber hinzuf\u00fcgen")) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBack) { Text(state.t("Back", "Zur\u00fcck")) }
            Button(
                enabled = name.isNotBlank(),
                modifier = Modifier.weight(1f),
                onClick = {
                    val subclassSlug = name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "local" }
                    val subclassId = "custom-$subclassSlug"
                    val mechanics = SubclassMechanicsUi(
                        parentClassName = state.creation.className,
                        ruleset = state.creation.ruleset,
                        selectionLevel = safeSelectionLevel,
                        statRules = listOf(
                            SubclassStatRulesUi(
                                minimumClassLevel = safeSelectionLevel,
                                armorClassBonus = armorClassBonus.toIntOrNull()?.coerceIn(-10, 20) ?: 0,
                                armorFormula = armorFormula,
                                hitPointsPerClassLevel = hitPointsPerLevel.toIntOrNull()?.coerceIn(-10, 20) ?: 0,
                                initiativeBonus = initiativeBonus.toIntOrNull()?.coerceIn(-20, 20) ?: 0,
                                initiativeHalfProficiencyBonus = initiativeHalfProficiency,
                                initiativeRollMode = initiativeRollMode,
                                speedBonusFeet = speedBonus.toIntOrNull()?.coerceIn(-100, 100) ?: 0,
                                savingThrowBonus = saveBonus.toIntOrNull()?.coerceIn(-20, 20) ?: 0,
                                attackBonus = attackBonus.toIntOrNull()?.coerceIn(-20, 20) ?: 0,
                                criticalThreshold = criticalThreshold.toIntOrNull()?.coerceIn(1, 20),
                            )
                        ),
                        features = features.mapIndexed { index, feature ->
                            val maximum = feature.maximumUses.takeIf { it > 0 }
                            SubclassFeatureGrantUi(
                                minimumClassLevel = feature.minimumLevel,
                                feature = FeatureUi(
                                    id = "subclass-grant-$subclassId-$index",
                                    name = feature.name,
                                    summary = feature.summary,
                                    recovery = feature.recovery,
                                    actionCost = actionCost(feature.actionKind),
                                    custom = true,
                                    turnGuideEligible = feature.suggestInTurnGuide,
                                ),
                                useScaling = if (maximum == null) SubclassUseScalingUi.NONE else SubclassUseScalingUi.FIXED,
                                fixedUses = maximum ?: 0,
                            )
                        },
                        spells = spells.mapIndexed { index, spell ->
                            SubclassSpellGrantUi(
                                spell.minimumLevel,
                                SpellUi(
                                    id = "subclass-spell-$subclassId-$index",
                                    name = spell.name,
                                    level = spell.spellLevel,
                                    summary = state.t("Granted by $name", "Durch $name gew\u00e4hrt"),
                                    sourceKind = SpellSourceKind.FEATURE,
                                    sourceName = name.trim(),
                                ),
                            )
                        },
                    )
                    state.addCustomSubclass(name, summary, mechanics)
                    onDone()
                },
            ) { Text(state.t("Create and select", "Erstellen und w\u00e4hlen")) }
        }
    }
}

@Composable
private fun NumericField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { candidate ->
            if (candidate.length <= 4 && candidate.all { it.isDigit() || it == '-' }) onValueChange(candidate)
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
    )
}

private fun actionCost(kind: CustomActionKind): ActionCost = when (kind) {
    CustomActionKind.NONE -> ActionCost()
    CustomActionKind.ACTION -> ActionCost(actions = 1)
    CustomActionKind.BONUS_ACTION -> ActionCost(bonusActions = 1)
    CustomActionKind.REACTION -> ActionCost(reactions = 1)
}

private fun actionLabel(state: DndAppState, kind: CustomActionKind): String = when (kind) {
    CustomActionKind.NONE -> state.t("None", "Keine")
    CustomActionKind.ACTION -> state.t("Action", "Aktion")
    CustomActionKind.BONUS_ACTION -> state.t("Bonus", "Bonus")
    CustomActionKind.REACTION -> state.t("Reaction", "Reaktion")
}

private fun recoveryLabel(state: DndAppState, recovery: Recovery): String = when (recovery) {
    Recovery.SHORT_REST -> state.t("Short Rest", "Kurze Rast")
    Recovery.LONG_REST -> state.t("Long Rest", "Lange Rast")
    else -> state.t("Manual", "Manuell")
}
