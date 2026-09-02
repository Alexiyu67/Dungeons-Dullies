package app.dulliesanddungeons.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsMma
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs
import kotlin.math.floor

private enum class BrowserPage { Catalog, CustomEquipment, CustomWeapon }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ItemBrowserDialog(state: DndAppState) {
    val target = state.itemBrowserTarget
    val ruleset = if (target == ItemBrowserTarget.StartingArmor) state.creation.ruleset else state.selectedCharacter?.ruleset ?: return
    var page by remember(target) { mutableStateOf(BrowserPage.Catalog) }
    var prefill by remember(target) { mutableStateOf<KnownItemUi?>(null) }
    var query by remember(target) { mutableStateOf("") }
    var filterOpen by remember(target) { mutableStateOf(false) }
    var customChoiceOpen by remember(target) { mutableStateOf(false) }
    var selectedTypes by remember(target) {
        mutableStateOf(if (target == ItemBrowserTarget.StartingArmor) setOf(KnownItemType.Armor) else emptySet())
    }
    var selectedRarities by remember(target) { mutableStateOf(emptySet<ItemRarity>()) }
    var selectedSources by remember(target) { mutableStateOf(emptySet<KnownItemSource>()) }
    var includeIncompatible by remember(target) { mutableStateOf(false) }
    var sort by remember(target) { mutableStateOf(KnownItemSort.Name) }
    var ascending by remember(target) { mutableStateOf(true) }

    Dialog(
        onDismissRequest = state::closeItemBrowser,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            if (page == BrowserPage.Catalog) state.closeItemBrowser() else {
                                page = BrowserPage.Catalog
                                prefill = null
                            }
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            if (page == BrowserPage.Catalog) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = state.t("Back", "Zurück"),
                        )
                    }
                    Text(
                        when {
                            page == BrowserPage.CustomWeapon -> state.t("Custom weapon", "Eigene Waffe")
                            page == BrowserPage.CustomEquipment && target == ItemBrowserTarget.StartingArmor -> state.t("Custom armor", "Eigene Rüstung")
                            page == BrowserPage.CustomEquipment -> state.t("Custom item", "Eigener Gegenstand")
                            target == ItemBrowserTarget.StartingArmor -> state.t("Choose starting armor", "Startrüstung wählen")
                            else -> state.t("Add items", "Gegenstände hinzufügen")
                        },
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (page == BrowserPage.Catalog) {
                        TextButton(onClick = {
                            if (target == ItemBrowserTarget.StartingArmor) page = BrowserPage.CustomEquipment
                            else customChoiceOpen = true
                        }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, Modifier.size(18.dp))
                            Text(state.t("Custom", "Eigene"))
                        }
                    }
                }

                when (page) {
                    BrowserPage.Catalog -> {
                        Box(Modifier.fillMaxSize()) {
                            ItemCatalogPage(
                                state = state,
                                target = target,
                                ruleset = ruleset,
                                query = query,
                                onQueryChange = { query = it },
                                selectedTypes = selectedTypes,
                                selectedRarities = selectedRarities,
                                selectedSources = selectedSources,
                                includeIncompatible = includeIncompatible,
                                sort = sort,
                                ascending = ascending,
                                onIncomplete = { item ->
                                    prefill = item
                                    page = if (item.type == KnownItemType.Weapon) BrowserPage.CustomWeapon else BrowserPage.CustomEquipment
                                },
                            )
                            FloatingActionButton(
                                onClick = { filterOpen = true },
                                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                            ) {
                                Icon(Icons.Rounded.Tune, contentDescription = state.t("Filter and sort items", "Gegenstände filtern und sortieren"))
                            }
                        }
                    }
                    BrowserPage.CustomEquipment -> CustomEquipmentPage(
                        state = state,
                        target = target,
                        initial = prefill,
                        onAdded = {
                            page = BrowserPage.Catalog
                            prefill = null
                        },
                    )
                    BrowserPage.CustomWeapon -> CustomWeaponPage(
                        state = state,
                        initial = prefill,
                        onAdded = {
                            page = BrowserPage.Catalog
                            prefill = null
                        },
                    )
                }
            }
        }
    }

    if (filterOpen) {
        ItemFiltersSheet(
            state = state,
            target = target,
            selectedTypes = selectedTypes,
            onTypesChange = { selectedTypes = it },
            selectedRarities = selectedRarities,
            onRaritiesChange = { selectedRarities = it },
            selectedSources = selectedSources,
            onSourcesChange = { selectedSources = it },
            includeIncompatible = includeIncompatible,
            onIncludeIncompatibleChange = { includeIncompatible = it },
            sort = sort,
            onSortChange = { sort = it },
            ascending = ascending,
            onAscendingChange = { ascending = it },
            onDismiss = { filterOpen = false },
            onClear = {
                selectedTypes = if (target == ItemBrowserTarget.StartingArmor) setOf(KnownItemType.Armor) else emptySet()
                selectedRarities = emptySet()
                selectedSources = emptySet()
                includeIncompatible = false
                sort = KnownItemSort.Name
                ascending = true
            },
        )
    }

    if (customChoiceOpen) {
        ModalBottomSheet(onDismissRequest = { customChoiceOpen = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(state.t("Create custom", "Eigenen Eintrag erstellen"), style = MaterialTheme.typography.titleLarge)
                Card(onClick = { customChoiceOpen = false; page = BrowserPage.CustomEquipment }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Backpack, contentDescription = null)
                        Spacer(Modifier.size(10.dp))
                        Text(state.t("Equipment or armor", "Ausrüstung oder Rüstung"), style = MaterialTheme.typography.titleSmall)
                    }
                }
                Card(onClick = { customChoiceOpen = false; page = BrowserPage.CustomWeapon }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SportsMma, contentDescription = null)
                        Spacer(Modifier.size(10.dp))
                        Text(state.t("Weapon", "Waffe"), style = MaterialTheme.typography.titleSmall)
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun ItemCatalogPage(
    state: DndAppState,
    target: ItemBrowserTarget,
    ruleset: Ruleset,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedTypes: Set<KnownItemType>,
    selectedRarities: Set<ItemRarity>,
    selectedSources: Set<KnownItemSource>,
    includeIncompatible: Boolean,
    sort: KnownItemSort,
    ascending: Boolean,
    onIncomplete: (KnownItemUi) -> Unit,
) {
    val catalog = state.knownItemCatalog()
    val visible = filterKnownItems(
        items = catalog,
        ruleset = ruleset,
        language = state.language,
        query = query,
        types = selectedTypes,
        rarities = selectedRarities,
        sources = selectedSources,
        includeIncompatible = includeIncompatible,
        sort = sort,
        ascending = ascending,
    )
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {{
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = state.t("Clear search", "Suche leeren"))
                }
            }} else null,
            placeholder = { Text(state.t("Search all known items", "Alle bekannten Gegenstände durchsuchen")) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
        )
        state.itemBrowserFeedback?.let { feedback ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(feedback, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (target == ItemBrowserTarget.StartingArmor && (query.isBlank() || state.t("Unarmored", "Ungerüstet").contains(query, true))) {
                item("unarmored") {
                    ItemResultCard(
                        state = state,
                        name = state.t("Unarmored", "Ungerüstet"),
                        metadata = state.t("Use the character's unarmored calculation", "Rüstungslosen Wert des Charakters verwenden"),
                        details = "",
                        warning = null,
                        complete = true,
                        onClick = state::selectCreationUnarmored,
                    )
                }
            }
            if (visible.isEmpty()) {
                item("empty") {
                    Text(
                        state.t("No items match this search and filter.", "Keine Gegenstände passen zu Suche und Filter."),
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(visible, key = KnownItemUi::id) { item ->
                val warning = state.itemCompatibilityHint(item, ruleset)
                ItemResultCard(
                    state = state,
                    name = item.name,
                    metadata = "${itemTypeLabel(state, item.type)} · ${rarityLabel(state, item.rarity)} · ${sourceLabel(state, item.source)}",
                    details = item.details,
                    warning = warning,
                    complete = item.complete,
                    onClick = {
                        when {
                            !item.complete -> onIncomplete(item)
                            target == ItemBrowserTarget.StartingArmor -> state.selectCreationArmor(item)
                            else -> state.addKnownItem(item)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ItemResultCard(
    state: DndAppState,
    name: String,
    metadata: String,
    details: String,
    warning: String?,
    complete: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (warning != null) Icons.Rounded.Warning else if (complete) Icons.Rounded.Backpack else Icons.Rounded.Edit,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (warning != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall)
                Text(metadata, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (warning != null) Text(warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!complete) Text(state.t("Complete the missing details before adding.", "Ergänze vor dem Hinzufügen die fehlenden Angaben."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Icon(if (complete) Icons.Rounded.Add else Icons.Rounded.Edit, contentDescription = null, Modifier.size(20.dp))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ItemFiltersSheet(
    state: DndAppState,
    target: ItemBrowserTarget,
    selectedTypes: Set<KnownItemType>,
    onTypesChange: (Set<KnownItemType>) -> Unit,
    selectedRarities: Set<ItemRarity>,
    onRaritiesChange: (Set<ItemRarity>) -> Unit,
    selectedSources: Set<KnownItemSource>,
    onSourcesChange: (Set<KnownItemSource>) -> Unit,
    includeIncompatible: Boolean,
    onIncludeIncompatibleChange: (Boolean) -> Unit,
    sort: KnownItemSort,
    onSortChange: (KnownItemSort) -> Unit,
    ascending: Boolean,
    onAscendingChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(state.t("Filter and sort", "Filtern und sortieren"), style = MaterialTheme.typography.titleLarge)
            if (target != ItemBrowserTarget.StartingArmor) {
                Text(state.t("Type", "Typ"), style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(KnownItemType.entries) { type ->
                        FilterChip(
                            selected = type in selectedTypes,
                            onClick = { onTypesChange(selectedTypes.toggle(type)) },
                            label = { Text(itemTypeLabel(state, type)) },
                        )
                    }
                }
            }
            Text(state.t("Rarity", "Seltenheit"), style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(ItemRarity.entries) { rarity ->
                    FilterChip(
                        selected = rarity in selectedRarities,
                        onClick = { onRaritiesChange(selectedRarities.toggle(rarity)) },
                        label = { Text(rarityLabel(state, rarity)) },
                    )
                }
            }
            Text(state.t("Source", "Quelle"), style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(KnownItemSource.entries) { source ->
                    FilterChip(
                        selected = source in selectedSources,
                        onClick = { onSourcesChange(selectedSources.toggle(source)) },
                        label = { Text(sourceLabel(state, source)) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(includeIncompatible, onIncludeIncompatibleChange)
                Column {
                    Text(state.t("Include incompatible", "Inkompatible einblenden"), style = MaterialTheme.typography.titleSmall)
                    Text(state.t("They remain selectable and show why they may not fit.", "Sie bleiben wählbar und zeigen, warum sie eventuell nicht passen."), style = MaterialTheme.typography.bodySmall)
                }
            }
            HorizontalDivider()
            Text(state.t("Sort by", "Sortieren nach"), style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(KnownItemSort.entries) { option ->
                    FilterChip(
                        selected = sort == option,
                        onClick = { onSortChange(option) },
                        label = { Text(sortLabel(state, option)) },
                    )
                }
            }
            OutlinedButton(onClick = { onAscendingChange(!ascending) }, modifier = Modifier.fillMaxWidth()) {
                Icon(if (ascending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward, contentDescription = null)
                Spacer(Modifier.size(7.dp))
                Text(if (ascending) state.t("Ascending", "Aufsteigend") else state.t("Descending", "Absteigend"))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text(state.t("Clear", "Zurücksetzen")) }
                Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(state.t("Apply", "Anwenden")) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CustomEquipmentPage(
    state: DndAppState,
    target: ItemBrowserTarget,
    initial: KnownItemUi?,
    onAdded: () -> Unit,
) {
    val initialItem = initial?.equipment
    var name by remember(initial?.id, target) { mutableStateOf(initialItem?.name.orEmpty()) }
    var details by remember(initial?.id, target) { mutableStateOf(initialItem?.details.orEmpty()) }
    var quantity by remember(initial?.id, target) { mutableStateOf((initialItem?.quantity ?: 1).toString()) }
    var kind by remember(initial?.id, target) { mutableStateOf(if (target == ItemBrowserTarget.StartingArmor) EquipmentKind.ARMOR else initialItem?.kind ?: EquipmentKind.GEAR) }
    var armorClass by remember(initial?.id, target) { mutableStateOf(initialItem?.armorClass?.toString().orEmpty()) }
    var shieldBonus by remember(initial?.id, target) { mutableStateOf(initialItem?.shieldBonus?.takeIf { it > 0 }?.toString().orEmpty()) }
    var attunement by remember(initial?.id, target) { mutableStateOf(initialItem?.needsAttunement == true) }
    val armorValid = kind != EquipmentKind.ARMOR || armorClass.toIntOrNull() in 1..30 || shieldBonus.toIntOrNull() in 1..9

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        if (target != ItemBrowserTarget.StartingArmor) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(EquipmentKind.entries) { option ->
                    FilterChip(selected = kind == option, onClick = { kind = option }, label = { Text(equipmentKindLabel(state, option)) })
                }
            }
        }
        OutlinedTextField(name, { name = it.take(80) }, label = { Text(state.t("Name", "Name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(details, { details = it.take(500) }, label = { Text(state.t("Useful details", "Nützliche Details")) }, modifier = Modifier.fillMaxWidth())
        if (target != ItemBrowserTarget.StartingArmor) {
            OutlinedTextField(quantity, { quantity = it.filter(Char::isDigit).take(3) }, label = { Text(state.t("Quantity", "Menge")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        if (kind == EquipmentKind.ARMOR) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(armorClass, { armorClass = it.filter(Char::isDigit).take(2) }, label = { Text(state.t("Final AC", "Endgültige RK")) }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(shieldBonus, { shieldBonus = it.filter(Char::isDigit).take(1) }, label = { Text(state.t("Shield bonus", "Schildbonus")) }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Text(state.t("Enter final AC for body armor, or a shield bonus for a shield.", "Gib für Körperrüstung die endgültige RK oder für einen Schild den Schildbonus ein."), style = MaterialTheme.typography.bodySmall)
        }
        if (target != ItemBrowserTarget.StartingArmor) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(attunement, { attunement = it })
                Text(state.t("Needs attunement", "Benötigt Einstimmung"))
            }
        }
        Button(
            onClick = {
                val item = EquipmentUi(
                    id = initialItem?.id.orEmpty(),
                    name = name.trim(),
                    kind = kind,
                    quantity = quantity.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                    details = details.trim(),
                    needsAttunement = target != ItemBrowserTarget.StartingArmor && attunement,
                    armorClass = armorClass.toIntOrNull()?.coerceIn(1, 30),
                    shieldBonus = shieldBonus.toIntOrNull()?.coerceIn(0, 9) ?: 0,
                )
                if (target == ItemBrowserTarget.StartingArmor) {
                    state.setCustomCreationArmor(item)
                } else {
                    state.addEquipment(item)
                    state.itemBrowserFeedback = state.t("Added ${item.name}", "${item.name} hinzugefügt")
                    onAdded()
                }
            },
            enabled = name.isNotBlank() && armorValid,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(if (target == ItemBrowserTarget.StartingArmor) state.t("Use this armor", "Diese Rüstung verwenden") else state.t("Add item", "Gegenstand hinzufügen"))
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun CustomWeaponPage(state: DndAppState, initial: KnownItemUi?, onAdded: () -> Unit) {
    val character = state.selectedCharacter ?: return
    val template = initial?.weapon
    val entry = initial?.privateEntry
    val importedDamage = template?.damage ?: entry?.formula?.let { Regex("\\b\\d+d\\d+(?:\\s*[+-]\\s*\\d+)?\\b").find(it)?.value }
    var name by remember(initial?.id) { mutableStateOf(template?.name ?: entry?.name.orEmpty()) }
    var ability by remember(initial?.id) { mutableStateOf(template?.ability ?: entry?.formulaValue("ability")?.uppercase()?.takeIf { it in setOf("STR", "DEX") } ?: "STR") }
    var damage by remember(initial?.id) { mutableStateOf(importedDamage ?: "1d8") }
    var damageType by remember(initial?.id) { mutableStateOf(template?.damageType ?: entry?.formulaValue("damage[ _-]?type").orEmpty()) }
    var range by remember(initial?.id) { mutableStateOf(template?.range ?: entry?.formulaValue("range").orEmpty()) }
    var properties by remember(initial?.id) { mutableStateOf(template?.properties ?: entry?.formulaValue("properties").orEmpty()) }
    var mastery by remember(initial?.id) { mutableStateOf(template?.mastery ?: entry?.formulaValue("mastery").orEmpty()) }
    var itemBonusText by remember(initial?.id) { mutableStateOf((template?.itemBonus ?: 0).toString()) }
    var attunement by remember(initial?.id) { mutableStateOf(template?.needsAttunement == true) }
    val abilityModifier = floor(((character.abilities[ability] ?: 10) - 10) / 2.0).toInt()
    val itemBonus = itemBonusText.toIntOrNull()?.coerceIn(-5, 5) ?: 0
    val attackBonus = abilityModifier + character.proficiency + itemBonus
    val formulaValid = Regex("\\d+d\\d+(?:\\s*[+-]\\s*\\d+)?|\\d+").matches(damage.trim())
    val finalDamage = if (Regex("[+-]\\s*\\d+").containsMatchIn(damage)) damage.trim() else when {
        abilityModifier > 0 -> "${damage.trim()} + $abilityModifier"
        abilityModifier < 0 -> "${damage.trim()} - ${abs(abilityModifier)}"
        else -> damage.trim()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(name, { name = it.take(80) }, label = { Text(state.t("Weapon name", "Waffenname")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(listOf("STR", "DEX")) { option -> FilterChip(selected = ability == option, onClick = { ability = option }, label = { Text(option) }) }
        }
        OutlinedTextField(damage, { damage = it.take(24) }, label = { Text(state.t("Damage formula", "Schadensformel")) }, supportingText = { Text(if (formulaValid) finalDamage else state.t("Use a formula such as 1d8", "Nutze eine Formel wie 1d8")) }, isError = !formulaValid, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(damageType, { damageType = it.take(40) }, label = { Text(state.t("Damage type", "Schadensart")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(range, { range = it.take(40) }, label = { Text(state.t("Range (optional)", "Reichweite (optional)")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(properties, { properties = it.take(120) }, label = { Text(state.t("Properties", "Eigenschaften")) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(mastery, { mastery = it.take(40) }, label = { Text(state.t("Mastery", "Meisterschaft")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(itemBonusText, { itemBonusText = it.filter { character -> character.isDigit() || character == '-' }.take(2) }, label = { Text(state.t("Magic/item bonus", "Magie-/Gegenstandsbonus")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(13.dp)) {
            Text(state.t("Attack bonus ${browserSigned(attackBonus)}", "Angriffsbonus ${browserSigned(attackBonus)}"), Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.labelLarge)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(attunement, { attunement = it })
            Text(state.t("Needs attunement", "Benötigt Einstimmung"))
        }
        Button(
            onClick = {
                state.addCustomWeapon(
                    WeaponUi(
                        id = template?.id.orEmpty(),
                        name = name.trim(),
                        attackBonus = attackBonus,
                        damage = finalDamage,
                        damageType = damageType.trim(),
                        properties = properties.trim(),
                        ability = ability,
                        itemBonus = itemBonus,
                        range = range.trim(),
                        mastery = mastery.trim(),
                        needsAttunement = attunement,
                        custom = true,
                        damageAbility = ability,
                    ),
                )
                state.itemBrowserFeedback = state.t("Added ${name.trim()}", "${name.trim()} hinzugefügt")
                onAdded()
            },
            enabled = name.isNotBlank() && damageType.isNotBlank() && formulaValid,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(state.t("Add weapon", "Waffe hinzufügen")) }
        Spacer(Modifier.height(20.dp))
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

private fun itemTypeLabel(state: DndAppState, type: KnownItemType): String = when (type) {
    KnownItemType.Weapon -> state.t("Weapon", "Waffe")
    KnownItemType.Armor -> state.t("Armor", "Rüstung")
    KnownItemType.Gear -> state.t("Gear", "Ausrüstung")
    KnownItemType.Tool -> state.t("Tool", "Werkzeug")
    KnownItemType.Consumable -> state.t("Consumable", "Verbrauch")
    KnownItemType.Rations -> state.t("Rations", "Rationen")
}

private fun equipmentKindLabel(state: DndAppState, kind: EquipmentKind): String = itemTypeLabel(state, kind.toKnownItemType())

private fun rarityLabel(state: DndAppState, rarity: ItemRarity): String = when (rarity) {
    ItemRarity.Mundane -> state.t("Mundane", "Gewöhnlich")
    ItemRarity.Common -> state.t("Common", "Häufig")
    ItemRarity.Uncommon -> state.t("Uncommon", "Ungewöhnlich")
    ItemRarity.Rare -> state.t("Rare", "Selten")
    ItemRarity.VeryRare -> state.t("Very rare", "Sehr selten")
    ItemRarity.Legendary -> state.t("Legendary", "Legendär")
    ItemRarity.Artifact -> state.t("Artifact", "Artefakt")
    ItemRarity.Unique -> state.t("Unique", "Einzigartig")
    ItemRarity.Unspecified -> state.t("Unspecified", "Nicht angegeben")
}

private fun sourceLabel(state: DndAppState, source: KnownItemSource): String = when (source) {
    KnownItemSource.BuiltIn -> state.t("Built-in", "Integriert")
    KnownItemSource.Local -> state.t("Local", "Lokal")
}

private fun sortLabel(state: DndAppState, sort: KnownItemSort): String = when (sort) {
    KnownItemSort.Name -> state.t("Name", "Name")
    KnownItemSort.Type -> state.t("Type", "Typ")
    KnownItemSort.Rarity -> state.t("Rarity", "Seltenheit")
}

private fun browserSigned(value: Int): String = if (value >= 0) "+$value" else value.toString()
