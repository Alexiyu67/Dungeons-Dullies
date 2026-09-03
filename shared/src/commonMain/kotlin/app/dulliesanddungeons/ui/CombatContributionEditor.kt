package app.dulliesanddungeons.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.dulliesanddungeons.domain.CombatContribution
import app.dulliesanddungeons.domain.CombatContributionTiming
import app.dulliesanddungeons.domain.CombatContributionType

@Composable
internal fun CombatContributionEditor(
    state: DndAppState,
    contributions: List<CombatContribution>,
    onChange: (List<CombatContribution>) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
        Text(
            state.t(
                "Turn potential${contributions.takeIf { it.isNotEmpty() }?.let { " (${it.size})" }.orEmpty()}",
                "Zugpotenzial${contributions.takeIf { it.isNotEmpty() }?.let { " (${it.size})" }.orEmpty()}",
            ),
            modifier = Modifier.weight(1f),
        )
        Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
    }
    if (!expanded) return

    Text(
        state.t(
            "Only reviewed structured rules affect the headline; descriptive text is never interpreted automatically.",
            "Nur geprüfte strukturierte Regeln beeinflussen die Anzeige; Beschreibungstext wird nie automatisch interpretiert.",
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    contributions.forEachIndexed { index, contribution ->
        if (index > 0) HorizontalDivider()
        ContributionFields(
            state = state,
            contribution = contribution,
            onChange = { replacement -> onChange(contributions.toMutableList().also { it[index] = replacement }) },
            onRemove = { onChange(contributions.filterIndexed { itemIndex, _ -> itemIndex != index }) },
        )
    }
    if (contributions.size < 3) {
        TextButton(
            onClick = { onChange(contributions + CombatContribution(CombatContributionType.EXTRA_ATTACKS)) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(state.t("Add potential rule", "Potenzialregel hinzufügen"))
        }
    }
}

@Composable
private fun ContributionFields(
    state: DndAppState,
    contribution: CombatContribution,
    onChange: (CombatContribution) -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(state.t("Rule ${contribution.type.displayName(state)}", "Regel ${contribution.type.displayName(state)}"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            IconButton(onClick = onRemove) { Icon(Icons.Rounded.Delete, contentDescription = state.t("Remove rule", "Regel entfernen")) }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(CombatContributionType.entries) { type ->
                FilterChip(
                    selected = contribution.type == type,
                    onClick = { onChange(contribution.copy(type = type)) },
                    label = { Text(type.displayName(state)) },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = contribution.count.toString(),
                onValueChange = { value -> onChange(contribution.copy(count = (value.filter(Char::isDigit).take(2).toIntOrNull() ?: 0).coerceIn(0, 99))) },
                label = { Text(if (contribution.type == CombatContributionType.ATTACK_ACTION_COUNT) state.t("Total", "Gesamt") else state.t("Added", "Zusätzlich")) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = contribution.resourceCost.takeIf { it > 0 }?.toString().orEmpty(),
                onValueChange = { value -> onChange(contribution.copy(resourceCost = value.filter(Char::isDigit).take(2).toIntOrNull() ?: 0)) },
                label = { Text(state.t("Resource cost", "Ressourcenkosten")) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(CombatContributionTiming.entries) { timing ->
                FilterChip(
                    selected = contribution.timing == timing,
                    onClick = { onChange(contribution.copy(timing = timing)) },
                    label = { Text(timing.displayName(state)) },
                )
            }
        }
        OutlinedTextField(
            contribution.resourceName,
            { onChange(contribution.copy(resourceName = it.take(60))) },
            label = { Text(state.t("Resource (optional)", "Ressource (optional)")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            contribution.requiredWeaponProperties.joinToString(", "),
            { value -> onChange(contribution.copy(requiredWeaponProperties = value.split(',').map(String::trim).filter(String::isNotBlank).toSet())) },
            label = { Text(state.t("Required weapon properties", "Erforderliche Waffeneigenschaften")) },
            supportingText = { Text(state.t("Comma-separated; leave empty for any weapon.", "Kommagetrennt; leer lassen für jede Waffe.")) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            contribution.note,
            { onChange(contribution.copy(note = it.take(160))) },
            label = { Text(state.t("Condition or note", "Bedingung oder Hinweis")) },
            modifier = Modifier.fillMaxWidth(),
        )
        ContributionCheck(state.t("Requires Attack action", "Benötigt Angriffsaktion"), contribution.requiresAttackAction) { onChange(contribution.copy(requiresAttackAction = it)) }
        ContributionCheck(state.t("Other casts must be action cantrips", "Andere Zauber müssen Aktions-Zaubertricks sein"), contribution.requiresActionCantripForAnotherCast) { onChange(contribution.copy(requiresActionCantripForAnotherCast = it)) }
        ContributionCheck(state.t("Setup required", "Aufbau erforderlich"), contribution.requiresSetup) { onChange(contribution.copy(requiresSetup = it)) }
        ContributionCheck(state.t("Setup uses Concentration", "Aufbau benötigt Konzentration"), contribution.setupUsesConcentration) { onChange(contribution.copy(setupUsesConcentration = it)) }
        ContributionCheck(state.t("Casts a spell this turn", "Wirkt in diesem Zug einen Zauber"), contribution.castsSpellThisTurn) { onChange(contribution.copy(castsSpellThisTurn = it)) }
        ContributionCheck(state.t("Requires a hit", "Treffer erforderlich"), contribution.requiresHit) { onChange(contribution.copy(requiresHit = it)) }
        ContributionCheck(state.t("Requires another target", "Benötigt weiteres Ziel"), contribution.requiresAdditionalTarget) { onChange(contribution.copy(requiresAdditionalTarget = it)) }
        ContributionCheck(state.t("Variable count / depends on targets", "Variable Anzahl / abhängig von Zielen"), contribution.variable) { onChange(contribution.copy(variable = it)) }
    }
}

@Composable
private fun ContributionCheck(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, onCheckedChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun CombatContributionType.displayName(state: DndAppState): String = when (this) {
    CombatContributionType.ATTACK_ACTION_COUNT -> state.t("Attack-action total", "Angriffsaktions-Gesamt")
    CombatContributionType.EXTRA_ATTACKS -> state.t("Extra attacks", "Zusätzliche Angriffe")
    CombatContributionType.EXTRA_CASTS -> state.t("Extra casts", "Zusätzliche Zauber")
}

private fun CombatContributionTiming.displayName(state: DndAppState): String = when (this) {
    CombatContributionTiming.ATTACK_ACTION -> state.t("Attack action", "Angriffsaktion")
    CombatContributionTiming.ACTION -> state.t("Action", "Aktion")
    CombatContributionTiming.BONUS_ACTION -> state.t("Bonus Action", "Bonusaktion")
    CombatContributionTiming.EXTRA_ACTION -> state.t("Extra action", "Zusatzaktion")
    CombatContributionTiming.TRIGGERED -> state.t("Triggered", "Ausgelöst")
}
