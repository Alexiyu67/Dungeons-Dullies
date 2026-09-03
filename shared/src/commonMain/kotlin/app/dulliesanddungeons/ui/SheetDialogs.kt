package app.dulliesanddungeons.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsMma
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

private enum class HpAdjustmentMode { CURRENT, TEMPORARY, MAXIMUM_REDUCTION }

@Composable
internal fun HpAdjustDialog(state: DndAppState) {
    val character = state.selectedCharacter ?: return
    var mode by remember(character.id) { mutableStateOf(HpAdjustmentMode.CURRENT) }
    val originalValue = when (mode) {
        HpAdjustmentMode.CURRENT -> character.hp.coerceAtMost(character.effectiveMaxHp)
        HpAdjustmentMode.TEMPORARY -> character.temporaryHp
        HpAdjustmentMode.MAXIMUM_REDUCTION -> character.maxHpReduction
    }
    val maximumValue = when (mode) {
        HpAdjustmentMode.CURRENT -> character.effectiveMaxHp
        HpAdjustmentMode.TEMPORARY -> 9_999
        HpAdjustmentMode.MAXIMUM_REDUCTION -> character.maxHp
    }
    var targetValue by remember(character.id, mode, originalValue, maximumValue) {
        mutableStateOf(originalValue.coerceIn(0, maximumValue))
    }
    var targetText by remember(character.id, mode, originalValue, maximumValue) {
        mutableStateOf(originalValue.coerceIn(0, maximumValue).toString())
    }
    val delta = targetValue - originalValue
    val deltaLabel = when {
        mode == HpAdjustmentMode.CURRENT && delta < 0 -> state.t("${abs(delta)} HP lost", "${abs(delta)} TP verloren")
        mode == HpAdjustmentMode.CURRENT && delta > 0 -> state.t("$delta HP gained", "$delta TP erhalten")
        mode == HpAdjustmentMode.CURRENT -> state.t("No HP change", "Keine TP-Änderung")
        mode == HpAdjustmentMode.TEMPORARY && delta != 0 -> state.t(
            "Replaces ${character.temporaryHp} temporary HP",
            "Ersetzt ${character.temporaryHp} temporäre TP",
        )
        mode == HpAdjustmentMode.TEMPORARY -> state.t("No temporary HP change", "Keine Änderung der temporären TP")
        else -> state.t(
            "Effective maximum: ${character.effectiveMaxHp(targetValue)} HP",
            "Effektives Maximum: ${character.effectiveMaxHp(targetValue)} TP",
        )
    }
    val fieldLabel = when (mode) {
        HpAdjustmentMode.CURRENT -> state.t("Final HP", "Endgültige TP")
        HpAdjustmentMode.TEMPORARY -> state.t("Temporary HP", "Temporäre TP")
        HpAdjustmentMode.MAXIMUM_REDUCTION -> state.t("Max HP reduction", "Max.-TP-Senkung")
    }
    val valueSuffix = when (mode) {
        HpAdjustmentMode.CURRENT -> "/ ${character.effectiveMaxHp} HP"
        HpAdjustmentMode.TEMPORARY -> state.t("temporary HP", "temporäre TP")
        HpAdjustmentMode.MAXIMUM_REDUCTION -> state.t("HP reduction", "TP-Senkung")
    }
    AlertDialog(
        onDismissRequest = { state.hpAdjustOpen = false },
        icon = { Icon(Icons.Rounded.Favorite, contentDescription = null) },
        title = { Text(state.t("Adjust HP", "TP anpassen")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = mode == HpAdjustmentMode.CURRENT,
                            onClick = { mode = HpAdjustmentMode.CURRENT },
                            label = { Text("HP") },
                        )
                    }
                    item {
                        FilterChip(
                            selected = mode == HpAdjustmentMode.TEMPORARY,
                            onClick = { mode = HpAdjustmentMode.TEMPORARY },
                            label = { Text(state.t("Temp HP", "Temp. TP")) },
                        )
                    }
                    item {
                        FilterChip(
                            selected = mode == HpAdjustmentMode.MAXIMUM_REDUCTION,
                            onClick = { mode = HpAdjustmentMode.MAXIMUM_REDUCTION },
                            label = { Text(state.t("Max reduction", "Max.-Senkung")) },
                        )
                    }
                }
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.fillMaxWidth().padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(targetValue.toString(), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(valueSuffix, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 5.dp))
                        }
                        Text(
                            deltaLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (mode == HpAdjustmentMode.CURRENT && delta < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (mode == HpAdjustmentMode.CURRENT) {
                    Slider(
                        value = targetValue.toFloat(),
                        onValueChange = { value ->
                            targetValue = value.roundToInt().coerceIn(0, maximumValue)
                            targetText = targetValue.toString()
                        },
                        valueRange = 0f..maximumValue.coerceAtLeast(1).toFloat(),
                        steps = (maximumValue - 1).coerceAtLeast(0),
                    )
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { targetValue = (targetValue - 1).coerceAtLeast(0); targetText = targetValue.toString() },
                        enabled = targetValue > 0,
                    ) {
                        Icon(
                            Icons.Rounded.Remove,
                            contentDescription = state.t("Decrease $fieldLabel by 1", "$fieldLabel um 1 verringern"),
                        )
                    }
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { text ->
                            targetText = text.filter(Char::isDigit).take(4)
                            targetText.toIntOrNull()?.let { targetValue = it.coerceIn(0, maximumValue) }
                        },
                        label = { Text(fieldLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { targetValue = (targetValue + 1).coerceAtMost(maximumValue); targetText = targetValue.toString() },
                        enabled = targetValue < maximumValue,
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = state.t("Increase $fieldLabel by 1", "$fieldLabel um 1 erhöhen"),
                        )
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = { state.hpAdjustOpen = false }) { Text(state.t("Cancel", "Abbrechen")) } },
        confirmButton = {
            Button(onClick = {
                when (mode) {
                    HpAdjustmentMode.CURRENT -> state.setHitPoints(targetValue)
                    HpAdjustmentMode.TEMPORARY -> state.setTemporaryHitPoints(targetValue)
                    HpAdjustmentMode.MAXIMUM_REDUCTION -> state.setMaximumHitPointReduction(targetValue)
                }
                state.hpAdjustOpen = false
            }, enabled = delta != 0 && targetText.toIntOrNull() != null) {
                Text(state.t("Apply", "Anwenden"))
            }
        },
    )
}

@Composable
internal fun QuickRollEditorDialog(state: DndAppState) {
    val character = state.selectedCharacter ?: return
    var selected by remember(character.id, character.quickRolls) { mutableStateOf(character.resolvedQuickRolls.take(12)) }
    val available = state.availableQuickRolls(character).sortedForPicker(
        language = state.language,
        displayName = QuickRollUi::label,
        stableId = { "${it.kind}-${it.id}" },
    )
    AlertDialog(
        onDismissRequest = { state.quickRollEditorOpen = false },
        icon = { Icon(Icons.Rounded.Casino, contentDescription = null) },
        title = { Text(state.t("Customize quick rolls", "Schnelle Würfe anpassen")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(state.t("Choose up to twelve. Use the arrows to set their order.", "Wähle bis zu zwölf. Ordne sie mit den Pfeilen."), style = MaterialTheme.typography.bodySmall)
                selected.forEachIndexed { index, quickRoll ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(15.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(start = 13.dp, end = 4.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}. ${quickRoll.label}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                            IconButton(onClick = {
                                if (index > 0) selected = selected.toMutableList().also { list -> val value = list.removeAt(index); list.add(index - 1, value) }
                            }, enabled = index > 0, modifier = Modifier.size(38.dp)) { Icon(Icons.Rounded.ArrowUpward, contentDescription = state.t("Move up", "Nach oben"), Modifier.size(18.dp)) }
                            IconButton(onClick = {
                                if (index < selected.lastIndex) selected = selected.toMutableList().also { list -> val value = list.removeAt(index); list.add(index + 1, value) }
                            }, enabled = index < selected.lastIndex, modifier = Modifier.size(38.dp)) { Icon(Icons.Rounded.ArrowDownward, contentDescription = state.t("Move down", "Nach unten"), Modifier.size(18.dp)) }
                        }
                    }
                }
                HorizontalDivider()
                LazyColumn(Modifier.height(300.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(available, key = { "${it.kind}-${it.id}" }) { option ->
                        val checked = selected.any { it.kind == option.kind && it.id == option.id }
                        Surface(
                            onClick = {
                                selected = if (checked) selected.filterNot { it.kind == option.kind && it.id == option.id }
                                else if (selected.size < 12) selected + option else selected
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (checked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = checked, onCheckedChange = null)
                                Text(option.label, modifier = Modifier.weight(1f))
                                Text(option.kind.name.lowercase().replace('_', ' '), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = { state.quickRollEditorOpen = false }) { Text(state.t("Cancel", "Abbrechen")) } },
        confirmButton = {
            Button(onClick = { state.updateQuickRolls(selected); state.quickRollEditorOpen = false }, enabled = selected.isNotEmpty()) {
                Text(state.t("Save", "Speichern"))
            }
        },
    )
}

private enum class AddMode { Standard, Equipment, Weapon }

@Composable
internal fun EquipmentAddDialog(state: DndAppState) {
    val character = state.selectedCharacter ?: return
    var mode by remember { mutableStateOf(AddMode.Standard) }
    var search by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { state.equipmentAddOpen = false },
        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
        title = { Text(state.t("Add equipment", "Ausrüstung hinzufügen")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    item { FilterChip(selected = mode == AddMode.Standard, onClick = { mode = AddMode.Standard }, label = { Text(state.t("Standard list", "Standardliste")) }) }
                    item { FilterChip(selected = mode == AddMode.Equipment, onClick = { mode = AddMode.Equipment }, label = { Text(state.t("Custom gear", "Eigene Ausrüstung")) }) }
                    item { FilterChip(selected = mode == AddMode.Weapon, onClick = { mode = AddMode.Weapon }, label = { Text(state.t("Custom weapon", "Eigene Waffe")) }) }
                }
                when (mode) {
                    AddMode.Standard -> StandardCatalogPicker(state, search, { search = it })
                    AddMode.Equipment -> CustomEquipmentForm(state)
                    AddMode.Weapon -> CustomWeaponForm(state, character)
                }
            }
        },
        confirmButton = { TextButton(onClick = { state.equipmentAddOpen = false }) { Text(state.t("Close", "Schließen")) } },
    )
}

@Composable
private fun StandardCatalogPicker(state: DndAppState, search: String, onSearch: (String) -> Unit) {
    var pendingArmor by remember { mutableStateOf<EquipmentUi?>(null) }
    val dexterityModifier = state.selectedCharacter?.abilities?.get("DEX")?.let { kotlin.math.floor((it - 10) / 2.0).toInt() } ?: 0
    OutlinedTextField(
        value = search,
        onValueChange = onSearch,
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        placeholder = { Text(state.t("Search weapons and gear", "Waffen und Ausrüstung suchen")) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    val needle = search.trim()
    val weapons = standardWeaponCatalog.filter { needle.isBlank() || it.name.contains(needle, true) }
        .sortedForPicker(state.language, StandardWeaponTemplate::name, StandardWeaponTemplate::id)
    val equipment = standardEquipmentCatalog.map { item ->
        val armorClass = when (item.id) {
            "leather-armor" -> 11 + dexterityModifier
            "studded-leather" -> 12 + dexterityModifier
            "chain-shirt" -> 13 + dexterityModifier.coerceAtMost(2)
            "scale-mail", "breastplate" -> 14 + dexterityModifier.coerceAtMost(2)
            "half-plate" -> 15 + dexterityModifier.coerceAtMost(2)
            "ring-mail" -> 14
            "chain-mail" -> 16
            "splint-armor" -> 17
            "plate-armor" -> 18
            else -> item.armorClass
        }
        item.copy(armorClass = armorClass)
    }.filter { needle.isBlank() || it.name.contains(needle, true) }
        .sortedForPicker(state.language, EquipmentUi::name, EquipmentUi::id)
    LazyColumn(Modifier.height(390.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (weapons.isNotEmpty()) item { Text(state.t("WEAPONS", "WAFFEN"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
        items(weapons, key = { "weapon-${it.id}" }) { weapon ->
            CatalogRow(Icons.Rounded.SportsMma, weapon.name, "${weapon.damage} ${weapon.damageType}") { state.addStandardWeapon(weapon) }
        }
        if (equipment.isNotEmpty()) item { Text(state.t("EQUIPMENT", "AUSRÜSTUNG"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp)) }
        items(equipment, key = { "gear-${it.id}" }) { item ->
            CatalogRow(if (item.kind == EquipmentKind.ARMOR) Icons.Rounded.Shield else Icons.Rounded.Backpack, item.name, item.details) {
                if (item.kind == EquipmentKind.ARMOR) pendingArmor = item else state.addEquipment(item)
            }
        }
    }
    pendingArmor?.let { armor ->
        AlertDialog(
            onDismissRequest = { pendingArmor = null },
            icon = { Icon(Icons.Rounded.Shield, contentDescription = null) },
            title = { Text(state.t("Wear ${armor.name}?", "${armor.name} tragen?")) },
            text = {
                Text(
                    if (armor.needsAttunement) state.t("Wearing and attunement are tracked separately.", "Tragen und Einstimmung werden getrennt erfasst.")
                    else state.t("You can change worn armor later from the Equipment section.", "Du kannst getragene Rüstung später im Ausrüstungsbereich ändern."),
                )
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    pendingArmor = null
                    addArmorFromDialog(state, armor, wear = false, attune = false)
                }) { Text(state.t("Add to pack", "Ins Gepäck")) }
            },
            confirmButton = {
                Button(onClick = {
                    pendingArmor = null
                    addArmorFromDialog(state, armor, wear = true, attune = armor.needsAttunement)
                }) { Text(if (armor.needsAttunement) state.t("Wear & attune", "Tragen & einstimmen") else state.t("Wear now", "Jetzt tragen")) }
            },
        )
    }
}

@Composable
private fun CatalogRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))) {
        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, Modifier.size(21.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge)
                if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Rounded.Add, contentDescription = null, Modifier.size(19.dp))
        }
    }
}

@Composable
private fun CustomEquipmentForm(state: DndAppState) {
    var name by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var attunement by remember { mutableStateOf(false) }
    var kind by remember { mutableStateOf(EquipmentKind.GEAR) }
    var armorClass by remember { mutableStateOf("") }
    var shieldBonus by remember { mutableStateOf("") }
    var pendingArmor by remember { mutableStateOf<EquipmentUi?>(null) }
    Column(Modifier.height(390.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(EquipmentKind.entries) { option ->
                val label = when (option) {
                    EquipmentKind.GEAR -> state.t("Gear", "Ausrüstung")
                    EquipmentKind.ARMOR -> state.t("Armor", "Rüstung")
                    EquipmentKind.TOOL -> state.t("Tool", "Werkzeug")
                    EquipmentKind.CONSUMABLE -> state.t("Consumable", "Verbrauch")
                    EquipmentKind.RATIONS -> state.t("Rations", "Rationen")
                }
                FilterChip(
                    selected = kind == option,
                    onClick = { kind = option },
                    leadingIcon = if (option == EquipmentKind.ARMOR) {{ Icon(Icons.Rounded.Shield, contentDescription = null, Modifier.size(17.dp)) }} else null,
                    label = { Text(label) },
                )
            }
        }
        OutlinedTextField(name, { name = it.take(80) }, label = { Text(state.t("Name", "Name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(details, { details = it.take(180) }, label = { Text(state.t("Useful details", "Nützliche Details")) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(quantity, { quantity = it.filter(Char::isDigit).take(3) }, label = { Text(state.t("Quantity", "Menge")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (kind == EquipmentKind.ARMOR) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(armorClass, { armorClass = it.filter(Char::isDigit).take(2) }, label = { Text("AC") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(shieldBonus, { shieldBonus = it.filter(Char::isDigit).take(1) }, label = { Text(state.t("Shield bonus", "Schildbonus")) }, singleLine = true, modifier = Modifier.weight(1f))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(attunement, { attunement = it })
            Text(state.t("Needs attunement", "Benötigt Einstimmung"))
        }
        Button(
            onClick = {
                val item = EquipmentUi(
                    id = "",
                    name = name.trim(),
                    kind = kind,
                    quantity = quantity.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                    details = details.trim(),
                    needsAttunement = attunement,
                    armorClass = armorClass.toIntOrNull()?.coerceIn(1, 30),
                    shieldBonus = shieldBonus.toIntOrNull()?.coerceIn(0, 9) ?: 0,
                )
                if (kind == EquipmentKind.ARMOR) pendingArmor = item else state.addEquipment(item)
            },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(state.t("Add equipment", "Ausrüstung hinzufügen")) }
    }
    pendingArmor?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingArmor = null },
            icon = { Icon(Icons.Rounded.Shield, contentDescription = null) },
            title = { Text(state.t("Wear ${item.name}?", "${item.name} tragen?")) },
            text = { Text(if (item.needsAttunement) state.t("This armor must be attuned before its magic benefits apply.", "Diese Rüstung muss eingestimmt sein, bevor ihre magischen Vorteile gelten.") else state.t("You can change this later in Equipment.", "Du kannst dies später unter Ausrüstung ändern.")) },
            dismissButton = {
                OutlinedButton(onClick = { pendingArmor = null; addArmorFromDialog(state, item, wear = false, attune = false) }) {
                    Text(state.t("Add to pack", "Ins Gepäck"))
                }
            },
            confirmButton = {
                Button(onClick = { pendingArmor = null; addArmorFromDialog(state, item, wear = true, attune = item.needsAttunement) }) {
                    Text(if (item.needsAttunement) state.t("Wear & attune", "Tragen & einstimmen") else state.t("Wear now", "Jetzt tragen"))
                }
            },
        )
    }
}

@Composable
private fun CustomWeaponForm(state: DndAppState, character: CharacterUi) {
    var name by remember { mutableStateOf("") }
    var ability by remember { mutableStateOf("STR") }
    var damage by remember { mutableStateOf("1d8") }
    var damageType by remember { mutableStateOf("Slashing") }
    var range by remember { mutableStateOf("") }
    var properties by remember { mutableStateOf("") }
    var mastery by remember { mutableStateOf("") }
    var magicBonusText by remember { mutableStateOf("0") }
    var attunement by remember { mutableStateOf(false) }
    var overrideOpen by remember { mutableStateOf(false) }
    var overrideText by remember { mutableStateOf("") }
    val abilityModifier = kotlin.math.floor(((character.abilities[ability] ?: 10) - 10) / 2.0).toInt()
    val itemBonus = magicBonusText.toIntOrNull()?.coerceIn(-5, 5) ?: 0
    val derivedBonus = abilityModifier + character.proficiency + itemBonus
    val attackBonus = overrideText.toIntOrNull()?.takeIf { overrideOpen } ?: derivedBonus
    val formulaValid = Regex("\\d+d\\d+(?:\\s*[+-]\\s*\\d+)?|\\d+").matches(damage.trim())
    val finalDamage = if (Regex("[+-]\\s*\\d+").containsMatchIn(damage)) damage.trim() else when {
        abilityModifier > 0 -> "${damage.trim()} + $abilityModifier"
        abilityModifier < 0 -> "${damage.trim()} - ${abs(abilityModifier)}"
        else -> damage.trim()
    }
    Column(Modifier.height(430.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        OutlinedTextField(name, { name = it.take(80) }, label = { Text(state.t("Weapon name", "Waffenname")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("STR", "DEX").forEach { option -> FilterChip(selected = ability == option, onClick = { ability = option }, label = { Text(option) }) }
        }
        OutlinedTextField(damage, { damage = it.take(24) }, label = { Text(state.t("Damage formula", "Schadensformel")) }, supportingText = { Text(if (formulaValid) finalDamage else state.t("Use a formula such as 1d8", "Nutze eine Formel wie 1d8")) }, isError = !formulaValid, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(damageType, { damageType = it.take(40) }, label = { Text(state.t("Damage type", "Schadensart")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(range, { range = it.take(40) }, label = { Text(state.t("Range (optional)", "Reichweite (optional)")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(properties, { properties = it.take(120) }, label = { Text(state.t("Properties", "Eigenschaften")) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(mastery, { mastery = it.take(40) }, label = { Text(state.t("Mastery", "Meisterschaft")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(magicBonusText, { magicBonusText = it.filter { ch -> ch.isDigit() || ch == '-' }.take(2) }, label = { Text(state.t("Magic/item bonus", "Magie-/Gegenstandsbonus")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(13.dp)) {
            Text("d20 ${signed(abilityModifier)} ${signed(character.proficiency)} ${signed(itemBonus)} = ${signed(derivedBonus)}", Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.labelLarge)
        }
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(attunement, { attunement = it }); Text(state.t("Needs attunement", "Benötigt Einstimmung")) }
        TextButton(onClick = { overrideOpen = !overrideOpen }) { Text(state.t("Advanced manual override", "Erweiterte manuelle Anpassung")) }
        if (overrideOpen) OutlinedTextField(overrideText, { overrideText = it.filter { ch -> ch.isDigit() || ch == '-' }.take(3) }, label = { Text(state.t("Final attack bonus", "Endgültiger Angriffsbonus")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                state.addCustomWeapon(WeaponUi(
                    id = "", name = name.trim(), attackBonus = attackBonus, damage = finalDamage, damageType = damageType.trim(), properties = properties.trim(),
                    ability = ability, itemBonus = itemBonus, attackBonusOverride = overrideText.toIntOrNull()?.takeIf { overrideOpen }, range = range.trim(), mastery = mastery.trim(), needsAttunement = attunement, custom = true,
                ))
            },
            enabled = name.isNotBlank() && damageType.isNotBlank() && formulaValid,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(state.t("Add weapon", "Waffe hinzufügen")) }
    }
}

@Composable
internal fun PrivateContentDialog(state: DndAppState, onImportPrivateContent: () -> Unit) {
    var kind by remember { mutableStateOf("Item") }
    var name by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var formula by remember { mutableStateOf("") }
    var combatContributions by remember { mutableStateOf(emptyList<app.dulliesanddungeons.domain.CombatContribution>()) }
    var expandedPackId by remember { mutableStateOf<String?>(null) }
    val kinds = listOf("Class", "Subclass", "Species", "Background", "Feat", "Feature", "Spell", "Creature", "Language", "Item", "Weapon", "Condition", "Action", "Resource", "Rule")
    AlertDialog(
        onDismissRequest = { state.privateContentOpen = false },
        icon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
        title = { Text(state.t("Local content", "Lokale Inhalte")) },
        text = {
            Column(Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onImportPrivateContent, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Rounded.FileOpen, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text(state.t("Import file", "Datei importieren"))
                }
                Text(
                    state.t("PDF, TXT, MD, JSON and .dndpack stay on this phone.", "PDF, TXT, MD, JSON und .dndpack bleiben auf diesem Smartphone."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.pendingImports.isNotEmpty()) {
                    Text(state.t("REVIEW IMPORTS", "IMPORTE PRÜFEN"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    state.pendingImports.forEach { pending ->
                        val expanded = expandedPackId == pending.packId
                        Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Surface(onClick = { expandedPackId = if (expanded) null else pending.packId }, color = androidx.compose.ui.graphics.Color.Transparent) {
                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.FileOpen, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(9.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(pending.sourcePath.substringAfterLast('/').substringAfterLast('\\').ifBlank { pending.containerKind }, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            pending.error ?: state.t("${pending.candidates.size} candidates", "${pending.candidates.size} Kandidaten"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (pending.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = if (expanded) state.t("Hide candidates", "Kandidaten einklappen") else state.t("Show candidates", "Kandidaten anzeigen"))
                                }
                            }
                            if (expanded) {
                                HorizontalDivider()
                                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (pending.candidates.isEmpty() && pending.error == null) {
                                        Text(state.t("No entries found.", "Keine Einträge gefunden."), style = MaterialTheme.typography.bodySmall)
                                    }
                                    pending.candidates.forEach { candidate ->
                                        Row(verticalAlignment = Alignment.Top) {
                                            Icon(Icons.Rounded.Check, contentDescription = null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(7.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(candidate.name, style = MaterialTheme.typography.labelLarge)
                                                Text(candidate.kind, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                if (candidate.summary.isNotBlank()) Text(candidate.summary, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { state.discardPendingImport(pending.packId) }, modifier = Modifier.weight(1f)) { Text(state.t("Discard", "Verwerfen")) }
                                        Button(
                                            onClick = { state.approvePendingImport(pending.packId) },
                                            enabled = pending.error == null && pending.candidates.isNotEmpty(),
                                            modifier = Modifier.weight(1f),
                                        ) { Text(state.t("Approve", "Freigeben")) }
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
                Text(state.t("OR ADD MANUALLY", "ODER MANUELL HINZUFÜGEN"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(kinds) { option -> FilterChip(selected = kind == option, onClick = { kind = option }, label = { Text(option) }) }
                }
                OutlinedTextField(name, { name = it.take(100) }, label = { Text(state.t("Name", "Name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(summary, { summary = it.take(500) }, label = { Text(state.t("Summary", "Zusammenfassung")) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(formula, { formula = it.take(80) }, label = { Text(state.t("Formula / automation (optional)", "Formel / Automatisierung (optional)")) }, modifier = Modifier.fillMaxWidth())
                if (kind in setOf("Class", "Subclass", "Feat", "Feature", "Spell", "Item", "Weapon", "Action", "Rule")) {
                    CombatContributionEditor(state, combatContributions) { combatContributions = it }
                }
                Button(
                    onClick = {
                        state.addPrivateEntry(PrivateEntryUi("", kind, name.trim(), summary.trim(), formula.trim(), combatContributions = combatContributions))
                        name = ""
                        summary = ""
                        formula = ""
                        combatContributions = emptyList()
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Icon(Icons.Rounded.Add, contentDescription = null); Spacer(Modifier.width(6.dp)); Text(state.t("Add local entry", "Lokalen Eintrag hinzufügen")) }
                if (state.privateEntries.isNotEmpty()) {
                    HorizontalDivider()
                    state.privateEntries.takeLast(8).forEach { entry ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(entry.name, style = MaterialTheme.typography.labelLarge); Text(entry.kind, style = MaterialTheme.typography.labelSmall) }
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { state.privateContentOpen = false }) { Text(state.t("Done", "Fertig")) } },
    )
}

private fun addArmorFromDialog(state: DndAppState, armor: EquipmentUi, wear: Boolean, attune: Boolean) {
    val beforeIds = state.selectedCharacter?.resolvedEquipment?.map { it.id }?.toSet().orEmpty()
    state.addEquipment(armor.copy(worn = false, attuned = false))
    val added = state.selectedCharacter?.resolvedEquipment?.firstOrNull { it.id !in beforeIds && it.name == armor.name }
        ?: state.selectedCharacter?.resolvedEquipment?.firstOrNull { it.id == armor.id }
        ?: state.selectedCharacter?.resolvedEquipment?.lastOrNull { it.name == armor.name }
    if (attune && added?.needsAttunement == true && !added.attuned) state.toggleEquipmentAttunement(added.id)
    val readyItem = state.selectedCharacter?.resolvedEquipment?.firstOrNull { it.id == added?.id }
    val canWear = readyItem != null && (!readyItem.needsAttunement || readyItem.attuned)
    if (wear && canWear && !readyItem.worn) state.toggleEquipmentWorn(readyItem.id)
}

private fun signed(value: Int): String = if (value >= 0) "+$value" else value.toString()
